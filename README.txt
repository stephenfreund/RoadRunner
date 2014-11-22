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

  * Version 1.2: committed on 11/19/2014.
  
                 Various small bug fixes and memory performance improvements.
  
  * Version 1.1: committed on 8/30/2014.
  
		 All users should update to this version or later.
                 
                 It fixes many bugs and corner cases, has a variety of 
                 small functionality enhancements for fastpath code, and 
                 improves scalability of the shadow state data 
                 structures.  Instrumentation now uses ASM version 5.
                 
  * Version 1.0: initial release.

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
 
 
