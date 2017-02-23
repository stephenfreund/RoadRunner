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

package rr.barrier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import rr.instrument.hooks.SpecialMethodCallBack;
import rr.instrument.hooks.SpecialMethodListener;
import rr.instrument.hooks.SpecialMethods;
import rr.meta.InstrumentationFilter;
import rr.state.ShadowThread;
import acme.util.WeakResourceManager;
import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * By default, the barrier classes used in the montecarlo and spec benchmarks are
 * handled specially, since they include races that we'd often like to ignore.  To 
 * handle barrier events, have your tool implement BarrierMonitor and
 * create a new monitor in your Tool's constructor:
 * <pre>
 * 		new BarrierMonitor<BarrierState>(this, new DefaultValue<Object,BarrierState>() {
 *			public BarrierState get(Object k) {
 *				return new BarrierState(ShadowLock.get(k));
 *			}
 *		});
 *	</pre>
 *  This will cause the barrier event handlers to be called whenever a thread enters/exits
 *  a barrier, and each barrier will have an associated BarrierState that the tool can
 *  use to store whatever info it needs.
 */
public class BarrierMonitor<T> implements SpecialMethodListener {

	protected final DefaultValue<Object,T> defaultValue;
	protected final BarrierListener<T> listener;

	/**
	 * Option to turn off special handling of barriers.
	 */
	public static final CommandLineOption<Boolean> noBarrier = 
		CommandLine.makeBoolean("nobarrier", false, CommandLineOption.Kind.STABLE, "Do not monitor barrier calls, even if monitor is installed.");


	private static int count;
	private final Decoration<ShadowThread, BarrierEvent<T>> events = 
		ShadowThread.makeDecoration("barrier event ", DecorationFactory.Type.MULTIPLE, new DefaultValue<ShadowThread, BarrierEvent<T>>() {

			public BarrierEvent<T> get(ShadowThread t) {
				return new BarrierEvent<T>(t, null, true);
			}

		});

	private final WeakResourceManager<Object, T> barriers = new WeakResourceManager<Object, T>() {
		@Override
		protected T make(Object k) {
			return defaultValue.get(k);
		}
	};

	private class BarrierState {
		int parties;
		int count;
	}

	private final WeakResourceManager<Object, BarrierState> barrierState = new WeakResourceManager<Object, BarrierState>() {
		@Override
		protected BarrierState make(Object k) {
			BarrierState b = new BarrierState();
			b.count = 0;
			b.parties = getParties(k);
			Util.logf("New Barrier: %s.  Class=%s, parties=%d", Util.objectToIdentityString(k), k.getClass(), b.parties);
			return b;
		}
	};

	private int getParties(Object o) {
		final Class<?> barrierClass = o.getClass();
		try {
			Field f = barrierClass.getField("numThreads");
			return f.getInt(o);
		} catch (Exception e) {
			try {
				Method m = barrierClass.getMethod("getParties", new Class[0]);
				return (Integer) m.invoke(o);
			} catch (Exception e1) {
				return -1;
			}
		}
	}


	public BarrierMonitor(BarrierListener<T> listener, DefaultValue<Object,T> defaultValue) {
		this.defaultValue = defaultValue;
		this.listener = listener;

		if (!noBarrier.get()) {
			SpecialMethods.addHook(".*TournamentBarrier", "void DoBarrier(int)", this);
			SpecialMethods.addHook(".*Barrier", "void DoBarrier(int)", this);
			SpecialMethods.addHook("java.util.concurrent.CyclicBarrier", "int await()", this);
			SpecialMethods.addHook("java.util.concurrent.CyclicBarrier", "int await(long, java.util.concurrent.TimeUnit)", this);
			SpecialMethods.addHook(".*Barrier", "void await()", this);
			Util.log("Turning off Instrumentation for all classes match .*Barrier.*: see -nobarrier");
			InstrumentationFilter.classesToWatch.get().addFirst("-.*Barrier.*");
		}
	}

	private  void preBarrier(final Object barrierObj, ShadowThread td) {
		BarrierEvent<T> e;
		synchronized(this) {
			T data = barriers.get(barrierObj);
			e = events.get(td);
			e.setBarrier(data);
			e.setEntering(true);
			BarrierState b = barrierState.get(barrierObj);
			b.count++;
			e.setParties(b.parties);
			e.setCount(b.count);
		}
		this.listener.preDoBarrier(e);
	}



	private void postBarrier(final Object barrierObj, ShadowThread td) {
		BarrierEvent<T> e;
		synchronized(this) {
			T data = barriers.get(barrierObj);
			e = events.get(td);
			e.setBarrier(data);
			e.setEntering(false);
			BarrierState b = barrierState.get(barrierObj);
			b.count--;
			e.setParties(b.parties);
			e.setCount(b.count);
		}
		this.listener.postDoBarrier(e);
	}


	/**
	 * RoadRunner internal method.
	 */
	public void invoked(SpecialMethodCallBack hook, boolean isPre, Object[] args, ShadowThread td) {
		if (isPre) {
			preBarrier(args[0], td);
		} else {
			postBarrier(args[0], td);
		}
	}
}
