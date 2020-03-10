package net.bqc.jrelifix.utils

import java.io._
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import net.bqc.jrelifix.context.diff.{ChangedFile, ChangedSnippet, ChangedType, SourceRange}
import net.bqc.jrelifix.identifier.Identifier
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger

import scala.collection.mutable

object DiffUtils {

  val logger: Logger = Logger.getLogger(this.getClass)

  def getChangedSnippet(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                toCheck: Identifier): ChangedSnippet = {
    getChangedSnippet(changedSourcesMap, toCheck, 0)
  }

  def getChangedSnippet(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                        toCheck: Identifier,
                        distance: Int): ChangedSnippet = {

    val fileName = toCheck.getFileName()
    val changedFile: ChangedFile = changedSourcesMap.get(fileName).orNull
    if (changedFile == null) {
      logger.debug("Not found modified file for " + fileName)
      return null
    }

    var changed = false
    val changedSnippets = changedFile.changedSnippets
    for (cs <- changedSnippets) {
      cs.changedType match {
        case ChangedType.ADDED =>
          changed = isInRange(toCheck, cs.dstRange, distance)
        case ChangedType.REMOVED =>
          changed = isInRange(toCheck, cs.srcRange, distance)
        case ChangedType.MODIFIED =>
          changed = isInRange(toCheck, cs.srcRange, distance) || isInRange(toCheck, cs.dstRange, distance)
        case ChangedType.MOVED =>
          changed = isInRange(toCheck, cs.srcRange, distance) || isInRange(toCheck, cs.dstRange, distance)
      }

      if (changed) return cs
    }
    null
  }

  def isChanged(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                toCheck: Identifier): Boolean = {
    getChangedSnippet(changedSourcesMap, toCheck) != null
  }

  def isInRange(toCheck: Identifier, range: SourceRange, lineDistance: Int) : Boolean = {
    val c1 = toCheck.getBeginLine() >= (range.beginLine - lineDistance) && toCheck.getEndLine() <= (range.endLine + lineDistance)
    val c2 = toCheck.getBeginColumn() == -1 ||
      (toCheck.getBeginColumn() >= range.beginColumn && toCheck.getEndColumn() <= range.endColumn)
    c1 && c2
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
        val command = "diff -w -b -u " + " --label=" + filePathLabel + " --label=" + filePathLabel + " " + original.getAbsolutePath + " " + current.getAbsolutePath
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
