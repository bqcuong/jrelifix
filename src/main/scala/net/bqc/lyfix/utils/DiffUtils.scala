package net.bqc.lyfix.utils

import java.io._
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import net.bqc.lyfix.context.diff.{ChangeSnippet, ChangeType, ChangedFile}
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.search.cs.{IChangeSnippetCondition, InsideSnippetCondition}
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object DiffUtils {

  val logger: Logger = Logger.getLogger(this.getClass)

  /**
   * Search changed snippets with a given condition, inside a given fileName
   * @param changedSourcesMap
   * @param fileName if null, search in the whole changed sources map
   * @param condition
   * @return
   */
  def searchChangeSnippets(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                           condition: IChangeSnippetCondition,
                           fileName: String = null) : ArrayBuffer[ChangeSnippet] = {
    val fileNames = ArrayBuffer[String]()
    val result = ArrayBuffer[ChangeSnippet]()

    if (fileName != null) fileNames.addOne(fileName)
    else fileNames.addAll(changedSourcesMap.keys)

    for (fn <- fileNames) {
      val changedFile: ChangedFile = changedSourcesMap.get(fn).orNull
      if (changedFile == null) {
        logger.debug("Not found modified file for " + fileName)
      }
      else {
        val css = changedFile.rootCS
        for (cs <- css) {
          if (condition.satisfied(cs)) result.addOne(cs)
        }
      }
    }
    result
  }

  def searchChangeSnippetOutside(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                                 toCheck: Identifier): ChangeSnippet = {
    searchChangeSnippetOutside(changedSourcesMap, toCheck, 0)
  }

  def searchChangeSnippetOutside(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                                 toCheck: Identifier,
                                 distance: Int): ChangeSnippet = {

    val fileName = toCheck.getFileName()
    val changedFile: ChangedFile = changedSourcesMap.get(fileName).orNull
    if (changedFile == null) {
      logger.debug("Not found modified file for " + fileName)
      return null
    }

    var changed = false
    val css = changedFile.rootCS
    for (cs <- css) {
      cs.changeType match {
        case ChangeType.ADDED =>
          changed = ASTUtils.isInRangeForId(toCheck, cs.dstRange, distance)
        case ChangeType.REMOVED =>
          changed = ASTUtils.isInRangeForId(toCheck, cs.srcRange, distance)
        case ChangeType.MODIFIED =>
          changed = ASTUtils.isInRangeForId(toCheck, cs.dstRange, distance)
        case ChangeType.MOVED =>
          changed = ASTUtils.isInRangeForId(toCheck, cs.dstRange, distance)
      }

      if (changed) return cs
    }
    null
  }

  def isChanged(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                toCheck: Identifier, distance: Int = 0): Boolean = {
    searchChangeSnippetOutside(changedSourcesMap, toCheck, distance) != null ||
    searchChangeSnippets(changedSourcesMap, InsideSnippetCondition(toCheck.toSourceRange()),
      toCheck.getFileName()).nonEmpty
  }

  /**
   * Borrow this method from Astor
   * @param prev
   * @param curr
   * @return
   */
  def getDiff(prev: String, curr: String, filePathLabel: String): String = {
    val original: File = File.createTempFile("jrelifix_", ".java")
    val current: File = File.createTempFile("jrelifix_", ".java")
    FileUtils.write(original, prev, Charset.forName("utf-8"))
    FileUtils.write(current, curr, Charset.forName("utf-8"))
    try {
      val builder = new ProcessBuilder("/bin/bash")
      builder.redirectErrorStream(true)
      val process = builder.start
      val p_stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream))

      try { // Set up the timezone
        val command = "diff -w -b -u " + " --label=" + filePathLabel + " --label=" + filePathLabel + " " + original.getCanonicalPath + " " + current.getCanonicalPath
//        logger.debug("diff command : " + command)
        p_stdin.write(command)
        p_stdin.newLine()
        p_stdin.flush()
        // end
        p_stdin.write("exit")
        p_stdin.newLine()
        p_stdin.flush()
      } catch {
        case e: IOException =>
          e.printStackTrace()
      }
      process.waitFor(30, TimeUnit.SECONDS)
      val stderr = process.getErrorStream
      val stdout = process.getInputStream
      val reader = new BufferedReader(new InputStreamReader(stdout))
      val readerE = new BufferedReader(new InputStreamReader(stderr))
      val out = readBuffer(reader)
      val outerror = readBuffer(readerE)
      if (!outerror.trim.isEmpty) logger.error("Error reading diff: " + outerror)
      process.destroyForcibly
      return out
    }
    catch {
      case e: Exception =>
        e.printStackTrace()
        logger.error(e)
    }
    null
  }

  @throws[IOException]
  private def readBuffer(reader: BufferedReader): String = {
    var line:String = null
    val res = new mutable.StringBuilder()
    while ({line = reader.readLine(); line != null}) {
      res.append(line).append("\n")
    }
    res.toString()
  }
}
