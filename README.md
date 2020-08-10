# LyFix - Regression Error Repair for Java Program
LyFix is an automated bug fixing tool for Java programs that focus on regression errors by leveraging fix ingredients and specific repair operators learned from the software development history to achieve better repair results for regression bugs.
User need to provide the current version of program, the bug-inducing commit (BIC) and reduced tests (passing before BIC and failing after BIC).
LyFix then searches for change actions in BIC which the tool obtains fix ingredients from.
Next, specific repair operators are used to generate patches for buggy locations (given by [Jaguar](https://github.com/saeg/jaguar)).
LyFix is supported by the [Google Summer of Code 2020](https://summerofcode.withgoogle.com/projects/#5961790384504832) program.

### Installation
#### Clone
First, clone the source code of LyFix:
```
git clone https://github.com/bqcuong/lyfix
```
You also need to obtain the source code of samples program (a git submodule) if you want to run the scripts in Usage section:
```
git submodule init
git submodule update
```
#### Build
Please make sure your machine is satisfied with below requirements:
- Java 8 installed
- Maven 3.6.0 installed

Then, just simply run:
```bash
mvn -DskipTests package
```
## Usage
*The below instructions are shown to run repair for the samples program. Running repair for other programs is in the same way.*

Compile the tests source code of examined program first:
```
mvn -f samples/pom.xml test-compile 
```
Run LyFix repair process with command:
```
./run.sh samples
```
