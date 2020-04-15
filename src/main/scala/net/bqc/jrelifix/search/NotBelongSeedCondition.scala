package net.bqc.jrelifix.search
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.identifier.seed.Seedy

case class NotBelongSeedCondition(outerCode: String) extends ISeedCondition {
  override def satisfied(seed: Seedy): Boolean = {
    val javaNode = seed.asInstanceOf[Identifier].getJavaNode().toString.trim
    !outerCode.contains(javaNode)
  }
}
