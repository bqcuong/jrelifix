package net.bqc.lyfix.context.vcs

import java.io.{ByteArrayOutputStream, File, FileNotFoundException, IOException}

import ch.qos.logback.classic.Level
import net.bqc.lyfix.context.diff.ChangedFile
import net.bqc.lyfix.utils.FileFolderUtils
import org.eclipse.jgit.diff.{DiffEntry, DiffFormatter, RenameDetector}
import org.eclipse.jgit.lib.{Config, ObjectLoader, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, TreeWalk}
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class GitParser extends VCSParser {
  private var repository: Repository = _

  def loadRepository(path: String): Unit = {
    // set log level off for JGit
    LoggerFactory.getLogger("org.eclipse.jgit").asInstanceOf[ch.qos.logback.classic.Logger].setLevel(Level.OFF);

    val builder = new FileRepositoryBuilder()
    this.repository = builder.setGitDir(getGitFolder(path))
      .readEnvironment()
      .findGitDir()
      .setMustExist(true)
      .build()
  }

  def getGitFolder(projectFolder: String): File = {
    var dotGit = new File(projectFolder + File.separator + ".git")
    if (dotGit.isFile) {
      val content = FileFolderUtils.readFile(dotGit.getAbsolutePath)
      val dotGitPath = content.replace("gitdir:", "").trim
      dotGit = new File(projectFolder + File.separator + dotGitPath)
    }

    if (dotGit.isDirectory) return dotGit
    else throw new FileNotFoundException("Not found .git folder for the input project")
  }

  def getCommit(commitHash: String): RevCommit = {
    val obj = repository.resolve(commitHash)
    val walk = new RevWalk(repository)
    val commit = walk.parseCommit(obj)
    commit
  }

  /**
   * Only retrieve Java modified files!!!
   * @param oldCommit
   * @param newCommit
   * @return
   */
  def listModifiedFiles(oldCommit: RevCommit, newCommit: RevCommit): mutable.Set[(String, String)] = {
    val modifiedFiles = mutable.HashSet[(String, String)]()

    val out = new ByteArrayOutputStream()
    val df = new DiffFormatter(out)
    df.setRepository(this.repository)
    df.setContext(0)

    var diffs = df.scan(oldCommit, newCommit)

    // these three below lines are used to compute the rename file action, damn JGit API!!!
    val rd = new RenameDetector(this.repository)
    rd.addAll(diffs)
    diffs = rd.compute()

    import scala.jdk.CollectionConverters._
    for (diff <- diffs.asScala) {
      df.format(diff)
      val prevPath: String = diff.getOldPath
      val currPath: String = diff.getNewPath
      if (prevPath.endsWith(".java") || currPath.endsWith(".java")) {
        modifiedFiles.add((prevPath, currPath))
      }
    }
    modifiedFiles
  }

  def getFileContent(filePath: String, commit: RevCommit): String = {
    if (filePath == DiffEntry.DEV_NULL) return ""

    val tree = commit.getTree
    // now try to find a specific file
    val treeWalk: TreeWalk = new TreeWalk(repository)
    try {
      treeWalk.addTree(tree)
      treeWalk.setRecursive(true)
      treeWalk.setFilter(PathFilter.create(filePath))
      if (!treeWalk.next) {
        throw new IllegalStateException("Git Parser Error!")
      }
      val objectId = treeWalk.getObjectId(0)
      val loader: ObjectLoader = repository.open(objectId)
      // and then one can the loader to read the file
      new String(loader.getBytes)
    }
    catch {
      case e: Exception =>
        e.printStackTrace()
        ""
    }
    finally {
      if (treeWalk != null) treeWalk.close()
    }
  }

  def closeRepository(): Unit = {
    repository.close()
  }

  override def getModifiedFiles(projectPath: String, currentCommit: String, previousCommit: String): ArrayBuffer[ChangedFile] = {
    loadRepository(projectPath)

    val currentRevCommit = getCommit(currentCommit)
    val parentRevCommit = getCommit(if (currentCommit == previousCommit) currentCommit + "^" else previousCommit)
    val modifiedRelativeFilePaths = listModifiedFiles(parentRevCommit, currentRevCommit)
    val modifiedFiles = ArrayBuffer[ChangedFile]()
    for (f <- modifiedRelativeFilePaths) {
      val v1 = getFileContent(f._1, parentRevCommit)
      val v2 = getFileContent(f._2, currentRevCommit)
      modifiedFiles.append(ChangedFile(
        new File(projectPath + File.separator + f._2).getCanonicalPath,
        new File(projectPath + File.separator + f._1).getCanonicalPath,
        new File(projectPath + File.separator + f._2).getCanonicalPath,
        v1, v2))
    }

    closeRepository()

    modifiedFiles
  }
}
