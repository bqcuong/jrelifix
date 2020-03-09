package net.bqc.jrelifix.engine

import net.bqc.jrelifix.context.compiler.JavaJDKCompiler
import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.DiffUtils
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

case class JRelifixEngine(override val faults: ArrayBuffer[Identifier],
                          override val projectData: ProjectData,
                          override val context: EngineContext)
  extends APREngine(faults, projectData, context) {

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
        val reducedTSValidation = this.context.testValidator.validateTestCases(this.context.testValidator.predefinedNegTests, projectData.config().classpath())
        logger.debug(" ==> REDUCED TEST SUITE VALIDATION: " + (if (reducedTSValidation._1) "\u2713" else "\u00D7"))

        if (reducedTSValidation._1) {
          val wholeTSValidation = this.context.testValidator.validateAllTestCases(projectData.config().classpath())
          logger.debug("==> WHOLE TEST SUITE VALIDATION: " + (if (wholeTSValidation._1) "\u2713" else "\u00D7"))

          if (wholeTSValidation._1) {
            logger.debug("==========================================")
            logger.debug("FOUND A REPAIR (See below patch):")
            for (faultFile <- projectData.originalFaultFiles) {
              val changedDocument = projectData.sourceFileContents.get(faultFile)
              val originalSourceContent = changedDocument.document.get()
              val patchedSourceContent = changedDocument.modifiedDocument.get()
              val diff = DiffUtils.getDiff(originalSourceContent, patchedSourceContent, faultFile)
              if (!diff.trim.isEmpty) {
                logger.debug("------------------------------------------\n" + diff)
              }

            }
            logger.debug("==========================================")
            return
          }
        }
      }
    }

    logger.debug("==========================================")
    logger.debug("NOT FOUND ANY REPAIR")
    logger.debug("==========================================")
  }
}
