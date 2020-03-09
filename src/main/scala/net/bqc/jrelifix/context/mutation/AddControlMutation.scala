package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.DiffUtils

class AddControlMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  override def mutate(): Unit = {
    if (!DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement))
      return


  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
