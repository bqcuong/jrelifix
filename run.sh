#!/bin/bash

# Build JRelifix
#mvn package

# Run repair with samples project
PROJECT_DIR=samples
JACOCO_JAR=libs/jacocoagent.jar
JAGUAR_JAR=libs/br.usp.each.saeg.jaguar.core-1.0.0-jar-with-dependencies.jar
JRELIFIX_JAR=target/jrelifix-0.1-jar-with-dependencies.jar

SRC_PATH=src/main/java
TEST_PATH=src/test/java
SRC_CLASS=target/classes
TEST_CLASS=target/test-classes

if [ "$1" ]; then PROJECT_DIR=$1; fi

java -javaagent:$JACOCO_JAR=output=tcpserver -cp $JAGUAR_JAR:$JRELIFIX_JAR \
        net.bqc.jrelifix.JRelifixMain \
                --projectFolder "$PROJECT_DIR" \
                --sourceFolder $SRC_PATH \
                --testFolder $TEST_PATH \
                --sourceClassFolder $SRC_CLASS \
                --testClassFolder $TEST_CLASS \
                --topNFaults 10 \
                --reducedTests \
                "net.bqc.sampleapr.MainTest#test5,net.bqc.sampleapr.MainTest#test6,net.bqc.sampleapr.MainTest#test7"