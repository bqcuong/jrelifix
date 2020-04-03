package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData

abstract class Collector(projectData: ProjectData) {
  def collect(): ProjectData
}
