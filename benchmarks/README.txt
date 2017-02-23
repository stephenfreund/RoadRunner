Copyright (c) 2016, Cormac Flanagan (University of California, Santa Cruz)
                    and Stephen Freund (Williams College) 

All rights reserved.  

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.

   * Redistributions in binary form must reproduce the above
     copyright notice, this list of conditions and the following
     disclaimer in the documentation and/or other materials provided
     with the distribution.

   * Neither the names of the University of California, Santa Cruz
     and Williams College nor the names of its contributors may be
     used to endorse or promote products derived from this software
     without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


This directory contains the benchmark programs from the Dacapo and
JavaGrande suites.  The eclipse and tradebeans programs are excluded
because of incompatibilities with RoadRunner.  See the
LICENSE-DaCapo.txt and README-JavaGrande.txt for the copyright/license
on those files.

Benchmarks
----------

Each subdirectory contains at least the following:

  - TEST: Runs the benchmark a single time in RoadRunner, with the
          command line arguments provided.  For JavaGrande, TEST runs
          the smallest data size.  (There may also be TESTB and TESTC to
          run it with the B/C data sizes.)  For DaCapo, TEST runs the 
	  default size.

  - TEST_BENCH: Runs the benchmark for multiple iterations, with the
          command line arguments provided.  Be sure to include
          -benchmarks=N to run it for N iterations.  This uses the
          largest sizes for JavaGrande, and the default sizes for
          DaCapo.  

	  Look for the "RRBench: Average" XML entry in the output for
          the average running time of the benchmark iterations.

  - RRBench.java: The main entry points used when running TEST_BENCH

  - original.jar: The compiled code for the benchmark, as well as
          RRBench.class

Environment Variables
---------------------

PROGRAM_ARGS: The number of worker threads for JavaGrande

AVAIL_PROCS: The number of processor cores available to the benchmark
program.  This should really be the number of cores on the machine
unless you are doing something unusual.

JVM_ARGS: Arguments to pass directory to the JVM when running
RoadRunner.

The last two should be set by RoadRunner's msetup script.

Example
------- 

cd crypt

./TESTC -quiet -noinst

./TESTC -quiet -maxWarn=1 -tool=FT2

./TEST_BENCH -quiet -benchmark=5 -noinst

./TEST_BENCH -quiet -maxWarn=1 -benchmark=5 -tool=FT2


Running all Benchmarks
----------------------

You can run all benchmarks and gather average running times with a
command like the following.  

./TEST_BMS all FastTrack2 -tool=FT2 -quiet -maxWarn=1

It will generate the summary file FastTrack2.csv.  Individual log
files file be in the log subdirectory with obvious names.

See that script for more details.

