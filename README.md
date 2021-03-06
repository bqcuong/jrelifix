# LyFix - Regression Error Repair for Java Program
**LyFix** is an automated bug fixing tool for Java programs that focuses on regression errors by leveraging fix ingredients and specific repair operators learned from the software development history to achieve better repair results for regression bugs.
User first provides the current version of the program, the bug-inducing commit (BIC) and reduced tests (passing before BIC and failing after BIC).
**LyFix** then searches for change actions in BIC which the tool obtains fix ingredients from.
Next, specific repair operators are used to generate patches for buggy locations (given by [Jaguar](https://github.com/saeg/jaguar)).
**LyFix** is supported by the [Google Summer of Code 2020](https://summerofcode.withgoogle.com/projects/#5961790384504832) program.

## Requirements
- **Operating System:** Linux and MacOS (Windows will be supported soon)
- **Java Runtime:** Oracle JDK 1.8
- **Build Tool:** Maven 3.6.0+

## Installation
### Clone
First, clone the source code of **LyFix**:
```
$ git clone https://github.com/bqcuong/lyfix
```
You are also supposed to obtain the source code of **Bugs Dataset** (a git submodule) if you want to run the example in Usage section:
```
$ cd lyfix
$ git submodule init
$ git submodule update --progress
```

### Build
If you intend to use the pre-built version (the bundle file named `target/lyfix-0.1-jar-with-dependencies.jar`), please skip this step.

Otherwise, please just simply run the below command to trigger the build process:
```bash
$ mvn clean package
``` 

## Usage
The below text is the instructions to run **LyFix**.
```
Usage: ./run.sh [options]

  --help                   prints this usage text
  --depClasspath <value>   Dependencies Classpath (external libs,...). Accept both folder and jar path. e.g., /libs1/:/lib2/common.jar:...
  --javaHome <value>       Specify the path to Java home used to execute test cases
  --testDriver <value>     Test Driver. e.g., JUnit, TestNG. Default: JUnit
  --sourceFolder <value>   Folder of source code, e.g., src/main/java
  --sourceClassFolder <value>
                           Folder of classes of compiled source code, e.g., target/classes
  --reducedTests <value>   List of reduced test cases to be executed first, e.g., a.b.c.TestD#methodM
  --testFolder <value>     Folder of tests, e.g., src/main/test
  --testClassFolder <value>
                           Folder of classes of tests, e.g., target/test-classes
  --projectFolder <value>  Folder of project, if there are multiple modules, please fill the absolute path to the module
  --rootProjectFolder <value>
                           Root folder of the project if your project has multiple modules, which contains the .git folder
  --testTimeout <value>    Timeout for running tests, in seconds
  --ignoredTests <value>   
  --locHeuristic <value>   Name of heuristic for fault localization, e.g., Ochiai, Tarantula, etc
  --faultLines <value>     Faulty lines with class names, begin line, end line, begin column, end column "e.g., a.b.c.XYZ:10 10 1 9"
  --faultFile <value>      The path to the file which contains faulty lines with class names, content inside: e.g., a.b.c.XYZ@123@1.00
  --topNFaults <value>     Top N Faults to be considered, default is 100
  --isDataFlow <value>     Option for Jaguar fault localization tool
  --bugInducingCommit <value>
                           The hash of the bug-inducing commit. If not being set, it'll be the current commit.
  --bgValidation <value>   Specify if use bugswarm scripts to execute and validate the whole test suite
  --bgImageTag <value>     The image tag of BugSwam artifact which you want to evaluate on
  --externalTestCommand <value>
                           The external command to test the whole test suite. e.g., "mvn test -Dtest=a.b.c.ClassD"
  --externalReducedTestCommand <value>
                           The external command to test the reduced test suite. e.g., "mvn test"
  --configFile <value>     The path to config file which contains all run options. If providing this, none of the others is needed
```

### Example
*This section demonstrates how to make **LyFix** try to fix the bug `apache-commons-lang-224267191` of the [BugSwarm](bugswarm.org) benchmark*.
1. Go into the bug repository then check it out to the correct branch:
```bash
$ cd BugsDataset
$ git checkout apache-commons-lang-224267191
```

2. Compile the tests source code and collect dependency libraries of the bug program then go out:
```bash
$ ./reproduce_bug.sh
$ cd .. 
```

3. Run **LyFix** repair process with the command:
```bash
$ ./run.sh \
    --projectFolder BugsDataset \
    --depClasspath BugsDataset/target/dependency \
    --sourceFolder src/main/java \
    --testFolder src/test/java \
    --sourceClassFolder target/classes \
    --testClassFolder target/test-classes \
    --bugInducingCommit 314b6b56bec4af56dba667d66a25c1613f4bc800 \
    --reducedTests "org.apache.commons.lang3.reflect.MethodUtilsTest#testGetMethodsWithAnnotationSearchSupersButNotIgnoreAccess,org.apache.commons.lang3.reflect.MethodUtilsTest#testGetMethodsWithAnnotationSearchSupersAndIgnoreAccess" \
    --ignoredTests org.apache.commons.lang3.builder.ToStringBuilderTest#testReflectionHierarchyArrayList \
    --faultFile SusFiles/PerfectFL/apache-commons-lang-224267191.txt \
    --externalTestCommand "mvn test"
```
