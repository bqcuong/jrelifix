package net.bqc.lyfix.context

import net.bqc.lyfix.context.compiler.ICompiler
import net.bqc.lyfix.context.diff.DiffCollector
import net.bqc.lyfix.context.mutation.MutationGenerator
import net.bqc.lyfix.context.parser.JavaParser
import net.bqc.lyfix.context.validation.TestCaseValidator

class EngineContext(
                     val parser: JavaParser,
                     val differ: DiffCollector,
                     val compiler: ICompiler,
                     val testValidator: TestCaseValidator,
                     val mutationGenerator: MutationGenerator) {
}
