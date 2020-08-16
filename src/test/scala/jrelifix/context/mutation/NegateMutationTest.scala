package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.BaseTest
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite

class NegateMutationTest extends BaseTest {

  override def setUp(): Unit = {
    val args = Array[String](
      "--projectFolder", "/Users/cuong/APR/samples/test",
      "--sourceFolder", "src/main/java",
      "--testFolder", "src/test/java",
      "--sourceClassFolder", "target/classes",
      "--testClassFolder", "target/test-classes",
      "--topNFaults", "10",
      "--failingTests",
        "net.bqc.sampleapr.MainTest#test1," +
        "net.bqc.sampleapr.MainTest#test3," +
        "net.bqc.sampleapr.MainTest#test5," +
        "net.bqc.sampleapr.MainTest#test6," +
        "net.bqc.sampleapr.MainTest#test7",
      "--faultLines", "net.bqc.sampleapr.test.Main:15 18 9 10"
    )

    createConfig(args)
    super.setUp()
    createContext()
  }

//  def testMutate(): Unit = {
//    val negateMutation = NegateMutation(topNFaults(0), projectData)
//    negateMutation.mutate()
//    val rewriter: ASTRewrite = negateMutation.getRewriter
//  }
}
