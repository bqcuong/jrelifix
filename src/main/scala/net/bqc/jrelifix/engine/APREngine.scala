package net.bqc.jrelifix.engine

import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.identifier.Identifier

import scala.collection.mutable.ArrayBuffer

abstract class APREngine(val faults: ArrayBuffer[Identifier], val projectData: ProjectData, val context: EngineContext) {
  def repair() : Unit
}
