package net.bqc.jrelifix.engine

import net.bqc.jrelifix.context.EngineContext
import net.bqc.jrelifix.identifier.Identifier

import scala.collection.mutable.ArrayBuffer

abstract class APREngine(val faults: ArrayBuffer[Identifier], val context: EngineContext) {
  def repair() : Unit
}
