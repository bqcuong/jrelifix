package net.bqc.jrelifix.context

import net.bqc.jrelifix.context.compiler.JavaJDKCompiler
import net.bqc.jrelifix.context.diff.DiffCollector
import net.bqc.jrelifix.context.mutation.MutationGenerator
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.context.validation.TestCaseValidator

class EngineContext(
                     val parser: JavaParser,
                     val differ: DiffCollector,
                     val compiler: JavaJDKCompiler,
                     val testValidator: TestCaseValidator,
                     val mutationGenerator: MutationGenerator) {
}
