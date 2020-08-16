package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.seed.Seedy

case class NotEqualSeedCondition(outerCode: String) extends ISeedCondition {
  override def satisfied(seed: Seedy): Boolean = {
    val javaNode = seed.asInstanceOf[Identifier].getJavaNode().toString.trim
    !outerCode.trim.equals(javaNode)
  }
}
