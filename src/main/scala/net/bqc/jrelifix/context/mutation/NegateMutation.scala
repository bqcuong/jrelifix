package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier

case class NegateMutation(faultStatement: Identifier, projectData: ProjectData)

  extends Mutation(faultStatement, projectData) {

  override def mutate(): Unit = {

  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
