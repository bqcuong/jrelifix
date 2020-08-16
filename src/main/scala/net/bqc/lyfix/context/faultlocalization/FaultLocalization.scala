package net.bqc.lyfix.context.faultlocalization

import net.bqc.lyfix.identifier.Identifier

import scala.collection.mutable.ArrayBuffer

abstract class FaultLocalization {
  var rankedList: ArrayBuffer[Identifier] = new ArrayBuffer[Identifier]()
  def run(): Unit
}