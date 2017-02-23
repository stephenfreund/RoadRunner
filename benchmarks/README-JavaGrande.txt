/**************************************************************************
*                                                                         *
*         Java Grande Forum Benchmark Suite - Thread Version 1.0          *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 2001.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


Compiling the benchmark suite
-----------------------------

 1. Add the top directory (threadv1.0, the directory where this README 
   file is located) to your CLASSPATH. 

 2. Change directory to jgfutil.  

 3. Compile all the  .java files in this directory (e.g. javac *.java) 

 4. Change directory to section1.  

 5. Compile all the  .java files in this directory (e.g. javac *.java) 
    The compiler should be able to follow all dependencies. 

 6. Repeat steps 4 and 5 for section2 and section3.
 
Running the benchmark suite
---------------------------

Each benchmark in each section has a separate main program for each
size of dataset. For example, section2/JGFLUFactBenchSizeA.java
contains the main program for the LU factorisation benchmark in
Section 2 with dataset size A.

Also supplied is a main program which runs all the benchmarks in a
given section for a given dataset size (e.g. section2/JGFAllSizeA.java). 
Note that for large dataset sizes using these main programs can result 
in memory problems, and it may be preferable to run each benchmark 
separately. 

Note that, because some of the benchmarks require significant amounts
of memory, the default amount of memory available to JVMs may not be
large enough, resulting in a java.lang.OutOfMemoryError message. 
If this occurs, it will be necessary to increase the amount of
memory available to the JVM. 

The number of threads is specified as a command line arguement. If no
arguement is given, the benchmarks are run on 1 thread.


We would welcome any new results - please email to epcc-javagrande@epcc.ed.ac.uk

