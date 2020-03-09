# JRelifix Implementation Planning
## Current
* Repair Framework with supports of:
	* Fault Localization
	* Java Parser
	* Diff Collector (Git)
	* Java In-Memory Compiler
	* Test Case Invoker
	
## Implementing
Reference tool models:
* [GitHub - qhanam/Java-RSRepair: A Java version of the automatic program repair described by RSRepair and GenProg.](https://github.com/qhanam/Java-RSRepair)
* [GitHub - kusumotolab/kGenProg: A High-performance, High-extensibility and High-portability APR System](https://github.com/kusumotolab/kGenProg)
* [GitHub - xgdsmileboy/SimFix: Automatically fix programs by leveraging existing patches from other projects and similar code snippets from the faulty project.](https://github.com/xgdsmileboy/SimFix)

### Operators
* All are derived from CoreBench
* Relifix implemented 8 over 14 proposed operators from CoreBench

![](doc/relifix_operators.png)

* More specific operators for Java can be proposed (from Defects4J, InduceBenchmark)

* Three types of regressions bugs (by relifix) and the important operators:
  * **Local**: can be fixed by *Revert to previous statement*
  * **Unmask**: can be fixed by *Add condition*
  * **Remote**


### Patch Generation & Validation Approach
* Follow the proposed algorithm of Relifix
## Packaging (Useable for end users)
* Maven/Gradle Plugin
* Eclipse/IntelliJ Plugin
* CI/CD Tool
* Standalone tool

## Evaluation
Compare performance with state-of-the-arts tools: SimFix, PraPR, CapGen, jGenProg, JAID 
* Evaluate on 91 bugs of Defects4J (provided [here](https://github.com/justinwm/InduceBenchmark/blob/master/Defects4J.csv)). These bugs are provided with bug-inducing commits but we also need filter regression bugs.
* Evaluate on 362 bugs of InduceBenchmark (provided [here](https://github.com/justinwm/InduceBenchmark)). These projects needs complex configuration to run.
