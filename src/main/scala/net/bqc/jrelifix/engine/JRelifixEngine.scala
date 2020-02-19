package net.bqc.jrelifix.engine

import net.bqc.jrelifix.config.OptParser
import net.bqc.jrelifix.context.EngineContext
import net.bqc.jrelifix.context.compiler.JavaJDKCompiler
import net.bqc.jrelifix.context.diff.ChangedSnippet
import net.bqc.jrelifix.identifier.Identifier
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

case class JRelifixEngine(override val faults: ArrayBuffer[Identifier], override val context: EngineContext)
  extends APREngine(faults, context) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def repair(): Unit = {
    var faultIdx = 0
    while (faultIdx < faults.size) {
      val faultLine = faults(faultIdx)
      logger.debug("Try to fix on the fault point: " + faultLine)
      faultIdx = faultIdx + 1

      // Mutating
      val mutation = this.context.mutationGenerator.getRandomMutation(faultLine)
      mutation.mutate()
      logger.debug("Try mutating with operator [%s]".format(mutation.getClass.getName))

      // Try to compile
      val compileStatus = this.context.compiler.compile()
      logger.debug("Compile status: " + compileStatus)

      if (compileStatus == JavaJDKCompiler.Status.COMPILED) {
        val reducedTSValidation = this.context.testValidator.validateTestCases(this.context.testValidator.predefinedNegTests, OptParser.params().classpath())
        logger.debug("Validation result on the reduced Test Suite: " + reducedTSValidation._1)

        val wholeTSValidation = this.context.testValidator.validateAllTestCases(OptParser.params().classpath())
        logger.debug("Validation result on the whole Test Suite: " + wholeTSValidation._1)

        if (wholeTSValidation._1) {
          logger.debug("==========================================")
          logger.debug("FOUND A REPAIR")
          logger.debug("==========================================")
          return
        }
      }
    }

    logger.debug("==========================================")
    logger.debug("NOT FOUND ANY REPAIR")
    logger.debug("==========================================")
  }
}
