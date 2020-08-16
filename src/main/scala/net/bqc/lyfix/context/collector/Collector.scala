package net.bqc.lyfix.context.collector

import net.bqc.lyfix.context.ProjectData

abstract class Collector(projectData: ProjectData) {
  def collect(): ProjectData
}
