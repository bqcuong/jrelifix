package net.bqc.jrelifix.faultlocalization

import net.bqc.jrelifix.model.{PositionBasedIdentifier, PredefinedFaultIdentifier}

case class PredefinedFaultLocalization(faultLines: String) extends FaultLocalization {
  // faultLines's format: x.y.z.ABC:1 2 7 8,3 5 7 9;m.n.p.KHG:3 4 6 5

  override def run(): Unit = faultLines.split(";").foreach {
    case l => {
      val sp = l.trim.split(":") // sp(0) is file name, sp(1) is faulty lines
      sp(1).split(",").foreach {
        case fl => {
          val pos = fl.trim.split(" ").map(_.toInt)
          val identifier = PredefinedFaultIdentifier(pos(0), pos(1), pos(2), pos(3), sp(0))
          rankedList.append(identifier)
        }
      }
    }
  }
}
