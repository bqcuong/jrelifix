#!/bin/bash

CURRENT_DIR=`pwd`
JACOCO_JAR=$CURRENT_DIR/libs/jacocoagent.jar
JAGUAR_JAR=$CURRENT_DIR/libs/br.usp.each.saeg.jaguar.core-1.0.0-jar-with-dependencies.jar
LYFIX_JAR=$CURRENT_DIR/target/lyfix-0.1-jar-with-dependencies.jar

java -javaagent:$JACOCO_JAR=output=tcpserver -cp $JAGUAR_JAR:$LYFIX_JAR \
        net.bqc.lyfix.Main \
            --projectFolder $CURRENT_DIR/BugsDataset \
            --depClasspath $CURRENT_DIR/BugsDataset/target/dependency \
            --sourceFolder src/main/java \
            --testFolder src/test/java \
            --sourceClassFolder target/classes \
            --testClassFolder target/test-classes \
            --bugInducingCommit 314b6b56bec4af56dba667d66a25c1613f4bc800 \
            --reducedTests "org.apache.commons.lang3.reflect.MethodUtilsTest#testGetMethodsWithAnnotationSearchSupersButNotIgnoreAccess,org.apache.commons.lang3.reflect.MethodUtilsTest#testGetMethodsWithAnnotationSearchSupersAndIgnoreAccess" \
            --faultFile SusFiles/PerfectFL/apache-commons-lang-224267191.txt \
            --externalTestCommand "mvn test"