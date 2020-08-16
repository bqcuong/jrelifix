package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.seed.Seedy

case class NotBelongSeedCondition(outerCode: String) extends ISeedCondition {
  override def satisfied(seed: Seedy): Boolean = {
    val javaNode = seed.asInstanceOf[Identifier].getJavaNode().toString.trim
    !outerCode.contains(javaNode)
  }
}
