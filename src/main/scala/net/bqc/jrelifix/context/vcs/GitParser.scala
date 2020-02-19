package net.bqc.jrelifix.context.vcs

import java.io.{ByteArrayOutputStream, File}

import ch.qos.logback.classic.Level
import net.bqc.jrelifix.config.OptParser
import net.bqc.jrelifix.context.diff.ChangedFile
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.{ObjectLoader, Repository}
import org.eclipse.jgit.revwalk.DepthWalk.RevWalk
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
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
    this.repository = builder.setGitDir(new File(path + File.separator + ".git"))
      .readEnvironment()
      .findGitDir()
      .setMustExist(true)
      .build()
  }

  def getCommit(commitHash: String): RevCommit = {
    val obj = repository.resolve(commitHash)
    val walk = new RevWalk(repository, 100)
    val commit = walk.parseCommit(obj)
    commit
  }

  /**
   * Only retrieve Java modified files!!!
   * @param oldCommit
   * @param newCommit
   * @return
   */
  def listModifiedFiles(oldCommit: RevCommit, newCommit: RevCommit): mutable.Set[String] = {
    val modifiedFiles = mutable.Set[String]()

    val out = new ByteArrayOutputStream()
    val df = new DiffFormatter(out)
    df.setRepository(this.repository)
    df.setContext(0)

    val diffs = df.scan(oldCommit, newCommit)
    import scala.jdk.CollectionConverters._
    for (diff <- diffs.asScala) {
      df.format(diff)
      var filePath = diff.getOldPath
      if (filePath == null) {
        filePath = diff.getNewPath
      }
      if (filePath.endsWith(".java")) {
        modifiedFiles.add(filePath)
      }
    }
//    println(out.toString)
    modifiedFiles
  }

  def getFileContent(filePath: String, commit: RevCommit): String = {
    val tree = commit.getTree

    // now try to find a specific file
    try {
      val treeWalk: TreeWalk = new TreeWalk(repository)
      try {
        treeWalk.addTree(tree)
        treeWalk.setRecursive(true)
        treeWalk.setFilter(PathFilter.create(filePath))
        if (!treeWalk.next) {
          throw new IllegalStateException("Did not find expected file 'README.md'")
        }
        val objectId = treeWalk.getObjectId(0)
        val loader: ObjectLoader = repository.open(objectId)
        // and then one can the loader to read the file
        new String(loader.getBytes)
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          ""
        }
      }
      finally {
        if (treeWalk != null) treeWalk.close()
      }
    }
  }

  def closeRepository(): Unit = {
    repository.close()
  }

  def getModifiedFiles(commit: String): ArrayBuffer[ChangedFile] = {
    loadRepository(OptParser.params().projFolder)

    val currentCommit = getCommit(commit)
    val parentCommit = getCommit(commit + "^")
    val modifiedFilePaths = listModifiedFiles(parentCommit, currentCommit)
    val modifiedFiles = ArrayBuffer[ChangedFile]()
    for (f <- modifiedFilePaths) {
      val v1 = getFileContent(f, parentCommit)
      val v2 = getFileContent(f, currentCommit)
      modifiedFiles.append(new ChangedFile(f, v1, v2))
    }

    closeRepository()

    modifiedFiles
  }
}
