Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
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

Notes on Major Updates
========================

  * Version 0.5: 
  
  	New FastTrack implementation. See
  	tools/fasttrack/FastTrackTool and the tech report at
  	http://dept.cs.williams.edu/~freund/papers/ft2-techreport.pdf
  	for details.

	The old versions are available in tools/old, but the new one
  	is better in various ways.  There is also a version that uses
  	longs to encode epochs to enable larger clock values.  The
  	performance of that version should be essentially identical to
  	the one using ints on a 64-bit architecture.  The tools are
  	abbreviated as FT2 and FT2L.
  	
  	Two new command-line options for running benchmarks in a
  	fashion similar to, eg, the Dacapo test harness: -benchmark
  	and -warmup.  See rr.RRMain for details.
 
 	New array shadow state caching modes flag:
 	-arrayCacheType. See rr.state.AbstractArrayStateCache for
 	details.  The new default caching mechanism replaces weak
 	pointers (as a way to ensure caches don't pin down memory that
 	could be collected) with period clearing of all cached
 	information during full collections.  This offers better
 	run-time performance while still ensuring that unneeded shadow
 	memory is not retained indefinitely.  In general, you should
 	probably stick to the default mechanism and also use the
 	default Mark-Sweep collector to ensure this feature is
 	properly used.
  	
  	Various small bug fixes and improvements to infrastructure and
  	tools.

  * Version 0.4: committed on 7/27/2016.
  
         Various small bug fixes and performance improvements to
		 infrastructure and tools.

		 Eliminated reliance on deprecated sun classes.

		 Now works with Java 1.7 or 1.8.
                 
  * Version 0.3: committed on 6/11/2015.
  
                 Various small bug fixes and memory performance improvements.
                 
                 *** Now requires Java 1.7. ***
                 
  * Version 0.2: committed on 11/19/2014.
  
                 Various small bug fixes and memory performance improvements.
  
  * Version 0.1: committed on 8/30/2014.
  
		 All users should update to this version or later.
                 
                 It fixes many bugs and corner cases, has a variety of 
                 small functionality enhancements for fastpath code, and 
                 improves scalability of the shadow state data 
                 structures.  Instrumentation now uses ASM version 5.
                 
  * Version 0.0: initial release.

    	    	 See CHANGES.txt for more details.

Installation and Running
========================

See INSTALL.txt for instructions for installing and using RoadRunner.


Package Structure
=================

Javadoc for the most common API elements is provided in docs.  You
should not access any classes except those listed on those pages.
These are the most relevant classes/packages for writing new tools.

  - rr.tool.Tool:
  
          The base class of any analysis tool
          
  - rr.event.*:
  
          The Event objects that are passed to a tool's event handlers.
          You should only need to use the accessor methods for those objects.
          Note that these are reused.  You should never keep a reference to
          an Event object.  You will never create any of these objects
          yourself.
          
  - rr.meta.*:
  
          Represents the source metadata about types, classes, operations, etc.
          You should only need to use the accessors provided on these classes
          in your event handlers.  You will never create any of these objects
          yourself.
          
  - rr.simple.*:
  
          A couple simple tools. 
          
  - rr.state.ShadowThread:
  - rr.state.ShadowLock:
  - rr.state.ShadowVar:
  - rr.state.ShadowVolatile:
  
          The state attached to each Thread, object uses as a lock, and
          memory location used by a program.  Tools can attach decorations
          to ThreadStates and LockStates to store additional info on those
          structures.
          
          Tools need to create new types of VariableStates that are specific
          to them -- see the example tools.
          
  - rr.error:
  
          Error reporting classes.  See tools.* for examples of how to use
          them.
          
 The rest of the code base is primarily to instrument bytecode and provide the
 run-time support for tools.  You should not need to modify (or even look at)
 that code.
 
 
