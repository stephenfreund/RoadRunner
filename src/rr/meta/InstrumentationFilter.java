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

package rr.meta;

import acme.util.StringMatchResult;
import acme.util.StringMatcher;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Command line options to enable/disable instrumentation of
 * various syntactic elements.
 */
public class InstrumentationFilter {

	public static final Decoration<ClassInfo,Boolean> shouldInstrumentClass = 
			MetaDataInfoMaps.getClasses().makeDecoration("instrument class", DecorationFactory.Type.SINGLE, new DefaultValue<ClassInfo, Boolean>() { 
				public Boolean get(ClassInfo rrClass) { 
					return 
							classesToWatch.get().test(rrClass.getKey()) == StringMatchResult.ACCEPT; 
				}
			});

	public static final Decoration<MethodInfo,Boolean> shouldInstrumentMethod = 
			MetaDataInfoMaps.getMethods().makeDecoration("instrument method", DecorationFactory.Type.SINGLE, new DefaultValue<MethodInfo, Boolean>() { 
				public Boolean get(MethodInfo method) { 
					return shouldInstrument(method.getOwner()) &&
							methodsToWatch.get().test(method.getKey()) == StringMatchResult.ACCEPT && 
							!method.isNative() &&
							method.getOwner().isClass();
				}
			});


	public static final Decoration<FieldInfo,Boolean> shouldInstrumentField = 
			MetaDataInfoMaps.getFields().makeDecoration("instrument field", DecorationFactory.Type.SINGLE, new DefaultValue<FieldInfo, Boolean>() { 
				public Boolean get(FieldInfo field) { 
					return shouldInstrument(field.getOwner()) &&
							fieldsToWatch.get().test(field.getKey()) == StringMatchResult.ACCEPT && 
							(field.isVolatile() ||  // always track volatiles since they are sync devices...
									!field.isFinal() &&
									!field.isSynthetic());
				}
			});

	public static final Decoration<OperationInfo,Boolean> shouldInstrumentOp = 
			MetaDataInfoMaps.getOpDecorations().make("instrument op", DecorationFactory.Type.SINGLE, new DefaultValue<OperationInfo, Boolean>() {
				public Boolean get(OperationInfo op) { 
					final MethodInfo enclosing = op.getEnclosing();
					String s = op.getLoc().getKey();
					s = s.substring(0, s.lastIndexOf(":"));
					return shouldInstrument(enclosing.getOwner()) &&		
							linesToWatch.get().test(s) == StringMatchResult.ACCEPT &&
							shouldInstrument(enclosing);
				}	
			});


	public static final Decoration<MethodInfo,Boolean> supportsThreadStateParam  = 
			MetaDataInfoMaps.getMethods().makeDecoration("supportThreadStateParam", DecorationFactory.Type.SINGLE, new DefaultValue<MethodInfo, Boolean>() { 
				public Boolean get(MethodInfo method) { 
					final boolean b = !method.getName().contains("[") &&
							shouldInstrument(method) &&
							methodsSupportThreadStateParam.get().test(method.getKey())==StringMatchResult.ACCEPT && 
							!method.isNative() &&
							method.getOwner().isClass();
					return b;
				}
			});

	public static CommandLineOption<StringMatcher> methodsToWatch = 
			CommandLine.makeStringMatcher("methods", StringMatchResult.ACCEPT, CommandLineOption.Kind.STABLE, "Specifies which methods to instrument.  The default is all."
					//		,"-.*access\\$.*"
					//		,"-.*<clinit>.*"
					);

	public static CommandLineOption<StringMatcher> fieldsToWatch  = 
			CommandLine.makeStringMatcher("fields", StringMatchResult.ACCEPT, CommandLineOption.Kind.STABLE, "Specifies which fields to instrument.  The default is all.", "-.*this\\$.*", "-$.*__\\$rr.*");

	public static CommandLineOption<StringMatcher> linesToWatch  = 
			CommandLine.makeStringMatcher("lines", StringMatchResult.ACCEPT, CommandLineOption.Kind.STABLE, "Specifies which lines to instrument (only affects field/array operations).  The default is all.  (Form is 'test/Test.java:48')");

	public static CommandLineOption<StringMatcher> classesToWatch  = 
			CommandLine.makeStringMatcher("classes", StringMatchResult.ACCEPT, CommandLineOption.Kind.STABLE, "Specifies classes to instrument.  The default is all but standard libs.  Uses the StringMatcher scheme.  Examples:\n" + 
					"   -classes=\"-.*cow.*\" ignores classes with cow in name.\n" +
					"   -classes=\"+.*moo.*\" -classes=\"-.*cow.*\" ignores classes with cow in name, except if they have moo in the name",
					"-java..*", "-javax..*", "-com.sun..*", "-sun..*", "-rr..*", "-tools..*", "-acme..*", "-.*__\\$rr_.*", "-org.xml..*");

	public static CommandLineOption<StringMatcher> methodsSupportThreadStateParam  = 
			CommandLine.makeStringMatcher("shadowThread", StringMatchResult.ACCEPT, CommandLineOption.Kind.DEPRECATED, "Specifies which methods can be tranformed into version that take a ShadowThread parameter.  No longer used --- JVMs have faster direct access to thread local data than before.",
					"-.*");


	//	public static CommandLineOption<StringMatcher> methodsSupportThreadStateParam  = 
	//		CommandLine.makeStringMatcher("shadowThread", StringMatchResult.ACCEPT, CommandLineOption.Kind.STABLE, "Specifies which methods can be tranformed into version that take a ShadowThread parameter.  The default is all except main, run, and constructors.",
	//				"-.*main\\(\\[Ljava/lang/String;\\)V.*", 
	//				"-.*run\\(\\)V.*", 
	//				"+.*\\$rr__Original.*",
	//				"-.*\\$rr.*",
	//				"-.*\\<init\\>.*", 
	//				"-.*\\<clinit\\>.*");

	public static CommandLineOption<StringMatcher> noOpsOption  = 
			CommandLine.makeStringMatcher("noop", StringMatchResult.ACCEPT, CommandLineOption.Kind.STABLE, "Specifies which void methods should be replaced with a no op.  Useful for ignoring methods that check access via stack inspection.");

	private static ToolSpecificInstrumentationFilter toolFilter = new ToolSpecificInstrumentationFilter() {
		public boolean shouldInstrument(ClassInfo rrClass) { return true; }
		public boolean shouldInstrument(FieldInfo field) { return true; }
		public boolean shouldInstrument(MethodInfo rrMethod) { return true; }
		public boolean shouldInstrument(OperationInfo rrOp) { return true; }
	};

	public static boolean shouldInstrument(ClassInfo rrClass) {
		return shouldInstrumentClass.get(rrClass) && toolFilter.shouldInstrument(rrClass);
	}

	public static boolean shouldInstrument(FieldInfo field) {
		return shouldInstrumentField.get(field) && toolFilter.shouldInstrument(field);
	}

	public static boolean shouldInstrument(MethodInfo rrMethod) {
		return shouldInstrumentMethod.get(rrMethod) && toolFilter.shouldInstrument(rrMethod);
	}

	public static boolean shouldInstrument(OperationInfo rrOp) {
		return shouldInstrumentOp.get(rrOp) && toolFilter.shouldInstrument(rrOp);
	}

	public static boolean supportsThreadStateParam(MethodInfo m) {
		return supportsThreadStateParam.get(m);
	}

	public static boolean isNoOp(MethodInfo m) {
		return noOpsOption.get().test(m.getKey())==StringMatchResult.REJECT;
	}

	/* 
	 * Note: Only one specific filter is permitted.
	 */
	public static void addToolSpecificInstrumentationFilter(ToolSpecificInstrumentationFilter filter) {
		toolFilter = filter;
	}


}

