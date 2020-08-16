package net.bqc.lyfix.engine

import net.bqc.lyfix.context.{EngineContext, ProjectData}
import net.bqc.lyfix.identifier.Identifier

import scala.collection.mutable.ArrayBuffer

abstract class APREngine(val faults: ArrayBuffer[Identifier], val projectData: ProjectData, val context: EngineContext) {
  def repair() : Unit
}
