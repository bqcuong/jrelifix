package net.bqc.jrelifix.context.diff.gumtree;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.gen.jdt.JdtVisitor;

@Register(id = "java-jdt", accept = "\\.java$", priority = Registry.Priority.MAXIMUM)
public class MyJdtTreeGenerator extends MyAbstractJdtTreeGenerator {

    @Override
    protected AbstractJdtVisitor createVisitor() {
        return new JdtVisitor();
    }
}