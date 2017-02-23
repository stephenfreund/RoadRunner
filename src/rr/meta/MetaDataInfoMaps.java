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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import rr.instrument.Constants;
import acme.util.Assert;
import acme.util.Yikes;
import acme.util.Util;
import acme.util.decorations.DecorationFactory;
import acme.util.option.Option;
import acme.util.time.TimedExpr;



@SuppressWarnings("unchecked")
public class MetaDataInfoMaps {

	public static final Option<String> metaOption = new Option<String>("meta", Util.getenv("RR_META_DATA", null));

	private static final MetaDataAllocator<ClassInfo> classes;
	private static final MetaDataAllocator<FieldInfo> fields;
	private static final MetaDataAllocator<MethodInfo> methods;
	private static final MetaDataAllocator<AcquireInfo> acquires;
	private static final MetaDataAllocator<ReleaseInfo> releases;
	private static final MetaDataAllocator<StartInfo> starts;
	private static final MetaDataAllocator<WaitInfo> waits;
	private static final MetaDataAllocator<JoinInfo> joins;
	private static final MetaDataAllocator<InterruptInfo> interrupts;
	private static final MetaDataAllocator<FieldAccessInfo> fieldAccesses;
	private static final MetaDataAllocator<ArrayAccessInfo> arrayAccesses;
	private static final DecorationFactory<OperationInfo> opDecorations;
	private static final MetaDataAllocator<InvokeInfo> invokes;



	private static final GlobalMetaDataInfoDecorations globalDecorations;

	static final private <T extends MetaDataInfo> MetaDataAllocator<T> read(final String name, final ObjectInputStream in) throws Exception {
		return Util.log(new TimedExpr<MetaDataAllocator<T>>("Loading " + name + "...") {
			@Override
			public MetaDataAllocator<T> run() throws Exception {
				return (MetaDataAllocator<T>)in.readObject();
			}
		});
	}

	static { 
		String s = metaOption.get();
		if (s == null) {
			Util.logf("Creating Fresh Meta Data");
			classes = new MetaDataAllocator<ClassInfo>(new ClassInfo[0]);
			fields = new MetaDataAllocator<FieldInfo>(new FieldInfo[0]);
			methods = new MetaDataAllocator<MethodInfo>(new MethodInfo[0]);
			acquires = new MetaDataAllocator<AcquireInfo>(new AcquireInfo[0]);
			releases = new MetaDataAllocator<ReleaseInfo>(new ReleaseInfo[0]);	
			starts = new MetaDataAllocator<StartInfo>(new StartInfo[0]);
			waits  = new MetaDataAllocator<WaitInfo>(new WaitInfo[0]);
			joins = new MetaDataAllocator<JoinInfo>(new JoinInfo[0]);
			interrupts = new MetaDataAllocator<InterruptInfo>(new InterruptInfo[0]);
			fieldAccesses = new MetaDataAllocator<FieldAccessInfo>(new FieldAccessInfo[0]);
			arrayAccesses = new MetaDataAllocator<ArrayAccessInfo>(new ArrayAccessInfo[0]);
			invokes = new MetaDataAllocator<InvokeInfo>(new InvokeInfo[0]);

			opDecorations = new DecorationFactory<OperationInfo>();
			globalDecorations = new GlobalMetaDataInfoDecorations();

		} else {
			try {
				final String file = s + "/rr.meta";
				Util.logf("Loading Meta Data from %s...", file);
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
				classes = read("classes", in); 
				fields = read("fields", in);
				methods = read("methods", in);
				acquires = read("acquires", in);
				releases = read("releases", in);
				starts = read("starts", in);
				waits  = read("waits", in); 
				joins =  read("joins", in); 
				interrupts =  read("interrupts", in); 
				fieldAccesses = read("field accesses", in); 
				arrayAccesses = read("array accesses", in);
				invokes = read("invokes", in);

				Util.log("decorations...");
				opDecorations = (DecorationFactory<OperationInfo>)in.readObject();
				globalDecorations = (GlobalMetaDataInfoDecorations)in.readObject();
				in.close();
			} catch (Exception e) {
				Assert.panic(e);
				throw new RuntimeException(e);
			}
		}
	}


	public static void dump(String file) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(getClasses());
			out.writeObject(getFields());
			out.writeObject(getMethods());
			out.writeObject(getAcquires());
			out.writeObject(getReleases());
			out.writeObject(getStarts());
			out.writeObject(getWaits());
			out.writeObject(getJoins());
			out.writeObject(getInterrupts());
			out.writeObject(getFieldAccesses());
			out.writeObject(getArrayAccesses());
			out.writeObject(getInvokes());
			out.writeObject(getOpDecorations());
			out.writeObject(getGlobalDecorations());
			out.close();
		} catch (Exception e) {
			Assert.panic(e);
		}
	}


	public static void print(PrintWriter out) {
		for (ClassInfo c : getClasses()) {
			ClassInfo.State state = c.getState();
			switch (state) {
			case FRESH:
				out.println("fresh " + c.getName());
				break;
			case PRELOADED:
			case COMPLETE:
				out.print(InstrumentationFilter.shouldInstrument(c) ? "instrumented " : "");
				out.print(c.isSynthetic() ? "synthetic " : "");
				out.print(c.isClass() ? "class " : "interface ");
				out.print(c.getName());
				if (c.getSuperClass() != null) {
					out.print(" extends ");
					out.print(c.getSuperClass());
				}
				if (c.getInterfaces().size() > 0) {
					out.print(" implements ");
					out.print(c.getInterfaces());
				}
				out.println(" {");
				out.println(c.getInstanceFields().size());
				for (FieldInfo f : c.getFields()) {
					out.print("    ");
					out.print(InstrumentationFilter.shouldInstrument(f) ? "instrumented " : "");
					out.print(f.getFieldOffset() + " : ");
					out.print(f.isSynthetic() ? "synthetic " : "");
					out.print(f.isFinal() ? "final " : "");
					out.print(f.isVolatile() ? "volatile " : "");
					out.print(f.isStatic() ? "static " : "");
					out.print(f.getDescriptor() + " " + f.getName() + ";");
					out.println();
				}
				for (MethodInfo m : c.getMethods()) {
					out.print("    ");
					out.print(InstrumentationFilter.shouldInstrument(m) ? "instrumented " : "");
					out.print(InstrumentationFilter.supportsThreadStateParam(m)? "threadStateOk " : "");
					out.print(m.isSynthetic() ? "synthetic " : "");
					out.print(m.isStatic() ? "static " : "");
					out.print(m.isNative() ? "native " : "");

					out.println("    " + m.getName() + " "+ m.getDescriptor() + ":");
					for (OperationInfo o : m.getOps()) {
						out.print("            ");
						out.print(InstrumentationFilter.shouldInstrument(o) ? "instrumented " : "");
						out.println(o);
					}
				}
				out.println("}");
				break;
			}
			out.println();
		}
	}


	public static MethodInfo getMethod(ClassInfo rrType, String name, String signature) {
		Assert.assertTrue(signature != null);
		MethodInfo x = getMethods().get(MetaDataInfoKeys.getMethodKey(rrType, name, signature));
		if (x == null) {
			boolean isSynthetic = Constants.isSyntheticName(name);
			x = new MethodInfo(getMethods().size(), SourceLocation.NULL, rrType, name, signature, isSynthetic);
			getMethods().put(x);
		} 
		rrType.addMethod(x);

		return x;
	}

	public static ClassInfo getClass(String className) {		
		ClassInfo x = getClasses().get(MetaDataInfoKeys.getClassKey(className));
		if (x == null) {
//			System.err.println("NOT FOUND:" + className);
			boolean isSynthetic = Constants.isSyntheticName(className);
			x = new ClassInfo(getClasses().size(), SourceLocation.NULL, className, isSynthetic);
			getClasses().put(x);
		} 
		return x;
	}

	public static FieldInfo getField(ClassInfo rrClass, String name, String descriptor) {
		FieldInfo x = getFields().get(MetaDataInfoKeys.getFieldKey(rrClass, name, descriptor));
		if (x == null) {
			boolean isSynthetic = Constants.isSyntheticName(name);
			x = new FieldInfo(getFields().size(), SourceLocation.NULL, rrClass, name, descriptor, isSynthetic);
			getFields().put(x);
		} 
		rrClass.addField(x);
		return x;
	}

	public static FieldInfo getField(String key) {
		FieldInfo x = getFields().get(key);
		Assert.assertTrue(x != null, key);
		return x;
	}

	
	public static AcquireInfo makeAcquire(SourceLocation loc, MethodInfo enclosing) {
		AcquireInfo a;
		while (true) {
			a = getAcquires().get(MetaDataInfoKeys.getLockKey(loc, true));
			if (a == null) break;
			loc = new SourceLocation(loc.getFile(), loc.getLine(), loc.getOffset() + 1);
//			Yikes.yikes("making bogus loc");
		}
		a = new AcquireInfo(getAcquires().size(), loc, enclosing);
		getAcquires().put(a);

		return a;
	}

	public static ReleaseInfo makeRelease(SourceLocation loc, MethodInfo enclosing) {
		ReleaseInfo a;
		while (true) {
			a = getReleases().get(MetaDataInfoKeys.getLockKey(loc, false));
			if (a == null) break;
			loc = new SourceLocation(loc.getFile(), loc.getLine(), loc.getOffset() + 1);
//			Yikes.yikes("making bogus loc");
		}
		a = new ReleaseInfo(getReleases().size(), loc, enclosing);
		getReleases().put(a);
		return a;
	}

	public static ArrayAccessInfo makeArrayAccess(SourceLocation loc, MethodInfo enclosing, boolean isWrite) {
		ArrayAccessInfo a;
		loc = new SourceLocation(loc.getFile(), loc.getLine(), loc.getOffset());
		while (true) {
			a = getArrayAccesses().get(MetaDataInfoKeys.getArrayAccessKey(loc, isWrite));
			if (a == null) break;
			loc = new SourceLocation(loc.getFile(), loc.getLine(), loc.getOffset() + 1);
//			Yikes.yikes("making bogus loc");
		}
		a = new ArrayAccessInfo(getArrayAccesses().size(), loc, enclosing, isWrite);
		getArrayAccesses().put(a);
		return a;
	}

	public static FieldAccessInfo makeFieldAccess(SourceLocation loc, MethodInfo enclosing, boolean isWrite, FieldInfo field) {
		FieldAccessInfo a;
		loc = new SourceLocation(loc.getFile(), loc.getLine(), loc.getOffset());
		while (true) {
			a = getFieldAccesses().get(MetaDataInfoKeys.getFieldAccessKey(loc, enclosing, field, isWrite));
			if (a == null) break;
			loc = new SourceLocation(loc.getFile(), loc.getLine(), loc.getOffset() + 1);
//			Yikes.yikes("making bogus loc");
		}
		a = new FieldAccessInfo(getFieldAccesses().size(), loc, enclosing, isWrite, field);
		getFieldAccesses().put(a);
		return a;
	}

	public static JoinInfo makeJoin(SourceLocation loc, MethodInfo enclosing) {
		JoinInfo a = getJoins().get(MetaDataInfoKeys.getJoinKey(loc));
		if (a == null) {
			a = new JoinInfo(getJoins().size(), loc, enclosing);
			getJoins().put(a);
		}
		return a;
	}

	public static StartInfo makeStart(SourceLocation loc, MethodInfo enclosing) {
		StartInfo a = getStarts().get(MetaDataInfoKeys.getStartKey(loc));
		if (a == null) {
			a = new StartInfo(getStarts().size(), loc, enclosing);
			getStarts().put(a);
		}
		return a;
	}

	public static WaitInfo makeWait(SourceLocation loc, MethodInfo enclosing) {
		WaitInfo a = getWaits().get(MetaDataInfoKeys.getWaitKey(loc));
		if (a == null) {
			a = new WaitInfo(getWaits().size(), loc, enclosing);
			getWaits().put(a);
		}
		return a;
	}



	public static InterruptInfo makeInterrupt(SourceLocation sourceLocation, MethodInfo method) {
		InterruptInfo a = getInterrupts().get(MetaDataInfoKeys.getWaitKey(sourceLocation));
		if (a == null) {
			a = new InterruptInfo(getInterrupts().size(), sourceLocation, method);
			getInterrupts().put(a);
		}
		return a;	
	}


	public static InvokeInfo makeInvoke(SourceLocation loc, MethodInfo method, MethodInfo enclosing) {
		InvokeInfo a;
		final MetaDataAllocator<InvokeInfo> invokes2 = getInvokes();
		while (true) {
			a = invokes2.get(MetaDataInfoKeys.getInvokeKey(loc, method));
			if (a == null) break;
			loc = new SourceLocation(loc.getFile(), loc.getLine(), loc.getOffset() + 1);
		}
		a = new InvokeInfo(invokes2.size(), loc, method, enclosing);
		invokes2.put(a);

		return a;
	}


	public static MetaDataAllocator<ClassInfo> getClasses() {
		return classes;
	}


	public static MetaDataAllocator<FieldInfo> getFields() {
		return fields;
	}


	public static MetaDataAllocator<MethodInfo> getMethods() {
		return methods;
	}


	public static MetaDataAllocator<AcquireInfo> getAcquires() {
		return acquires;
	}


	public static MetaDataAllocator<ReleaseInfo> getReleases() {
		return releases;
	}


	public static MetaDataAllocator<StartInfo> getStarts() {
		return starts;
	}


	public static MetaDataAllocator<WaitInfo> getWaits() {
		return waits;
	}


	public static MetaDataAllocator<JoinInfo> getJoins() {
		return joins;
	}


	public static MetaDataAllocator<InterruptInfo> getInterrupts() {
		return interrupts;
	}


	public static MetaDataAllocator<FieldAccessInfo> getFieldAccesses() {
		return fieldAccesses;
	}


	public static MetaDataAllocator<ArrayAccessInfo> getArrayAccesses() {
		return arrayAccesses;
	}


	public static MetaDataAllocator<InvokeInfo> getInvokes() {
		return invokes;
	}


	public static DecorationFactory<OperationInfo> getOpDecorations() {
		return opDecorations;
	}


	public static GlobalMetaDataInfoDecorations getGlobalDecorations() {
		return globalDecorations;
	}


}
