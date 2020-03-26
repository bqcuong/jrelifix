package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangedFile
import net.bqc.jrelifix.identifier.Identifier

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class Collector(projectData: ProjectData) {
  def collect(): ProjectData
}
