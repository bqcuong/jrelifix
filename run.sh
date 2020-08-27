#!/bin/bash

CURRENT_DIR=`pwd`
JACOCO_JAR=$CURRENT_DIR/libs/jacocoagent.jar
JAGUAR_JAR=$CURRENT_DIR/libs/br.usp.each.saeg.jaguar.core-1.0.0-jar-with-dependencies.jar
LYFIX_JAR=$CURRENT_DIR/target/lyfix-0.1-jar-with-dependencies.jar

java -javaagent:$JACOCO_JAR=output=tcpserver -cp $JAGUAR_JAR:$LYFIX_JAR net.bqc.lyfix.Main "$@"