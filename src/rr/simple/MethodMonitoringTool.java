/******************************************************************************

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

 ******************************************************************************/

package rr.simple;

import rr.meta.MetaDataInfoMaps;
import rr.meta.MetaDataInfoAdapter;
import rr.meta.MethodInfo;
import rr.tool.Tool;
import acme.util.StringMatchResult;
import acme.util.StringMatcher;
import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Super class that adds a decoration to each method indicating
 * whether or not it is "interesting".  Subclass this to get an efficient
 * way to decide which methods to process at run time.
 */

public abstract class MethodMonitoringTool extends Tool {

	protected CommandLineOption<StringMatcher> methodsToWatch;
	
	private static enum MethodStatus { WATCH, IGNORE };
	
	protected Decoration<MethodInfo,MethodStatus> shouldWatch = MetaDataInfoMaps.getMethods().makeDecoration("monitor", DecorationFactory.Type.MULTIPLE, MethodStatus.IGNORE); 

	public MethodMonitoringTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		methodsToWatch = CommandLine.makeStringMatcher(getWatchFlag(), StringMatchResult.ACCEPT, CommandLineOption.Kind.STABLE, "criteria for methods to watch", getInitialWatchCritera());
		commandLine.add(methodsToWatch);
		addMetaDataListener(new MetaDataInfoAdapter() {
			@Override
			public void visit(MethodInfo info) {
				shouldWatch.set(info, MethodStatus.IGNORE);
				if (methodsToWatch.get().test(info.toString()) == StringMatchResult.ACCEPT) {
					shouldWatch.set(info, MethodStatus.WATCH);
				}
			}			
		});
	}
	
	protected abstract String getWatchFlag();
	protected abstract String[] getInitialWatchCritera();

	
	public void forceWatch(MethodInfo info) {
		shouldWatch.set(info, MethodStatus.WATCH);
	}
	
	protected boolean watch(MethodInfo info) {
		boolean b = shouldWatch.get(info) == MethodStatus.WATCH;
		return b;
	}
}
