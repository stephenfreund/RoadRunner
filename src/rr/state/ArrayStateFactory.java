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

package rr.state;


import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.BitSet;
import java.util.HashMap;

import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.meta.SourceLocation;
import rr.state.update.Updaters;
import rr.tool.RR;

import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.count.Counter;
import acme.util.count.Timer;
import acme.util.identityhash.ConcurrentIdentityHashMap;
import acme.util.identityhash.WeakIdentityHashMap;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public class ArrayStateFactory {

	public static enum ArrayMode { NONE, FINE, COARSE, SPECIAL, USER };

	protected static final ConcurrentIdentityHashMap<Object,AbstractArrayState> table = new ConcurrentIdentityHashMap<Object,AbstractArrayState>((1 << 16) - 11, (float) 0.5, 128);
	private static final WeakIdentityHashMap<Object,WeakReference<AbstractArrayState>> attic = new WeakIdentityHashMap<Object,WeakReference<AbstractArrayState>>((1 << 18) - 11);
	private static int count = 0; // since ConcurrentHashMap size() is slow, approximate here...

	public static CommandLineOption<ArrayMode> arrayOption = 
			CommandLine.makeEnumChoice("array", ArrayMode.FINE, CommandLineOption.Kind.STABLE, "Determine the granularity of array shadow memory.\n    NONE tracks no array info.\n    FINE uses one location per index.\n    COARSE uses one location per array\n    SPECIAL can change from COARSE to FINE if tool requests it.", ArrayMode.class);


	public static CommandLineOption<Constructor<? extends AbstractArrayState>> userArrayOption;

	static {
		try {
			userArrayOption =
					new CommandLineOption<Constructor<? extends AbstractArrayState>>("userArray", 
							(Constructor<? extends AbstractArrayState>)rr.state.FineArrayState.class.getConstructor(Object.class), 
							true, CommandLineOption.Kind.EXPERIMENTAL, "User defined array mode class") {
				@Override
				protected void apply(String arg) {
					try {
						Class<AbstractArrayState> c = (Class<AbstractArrayState>)RR.getToolLoader().loadClass(arg);
						Assert.assertTrue(c != null);
						Constructor<AbstractArrayState> x = c.getConstructor(Object.class);
						set(x);
					} catch (Exception e) {
						Assert.fail("Invalid option for userArray: " + arg + ".  " + e);
					}
				}
			};
		} catch(Exception e) {
			userArrayOption = null;
			Assert.panic(e);
		}
	}



	public static final NullArrayState NULL = new NullArrayState();

	private static int CACHE_SIZE = 8;

	protected final AbstractArrayState cache[] = new AbstractArrayState[CACHE_SIZE];
	protected int rotate = 0;

	protected final ArrayMode defaultMode;
	protected final boolean useCAS;


	private static int MAP_CHECK = 2000;
	private static int MAP_MAX = 100000;
	private static int MAP_INC = 500;
	private static int mapCheck = MAP_CHECK;
	private static final Counter size = new Counter("ArrayStateFactory", "Size");
	private static final Timer atticTime = new Timer("ArrayStateFactory", "Attic Move Time");
	private static final Counter atticHits = new Counter("ArrayStateFactory", "Attic Hits");


	public ArrayStateFactory(ArrayMode defaultMode, boolean useCAS) {
		this.defaultMode = defaultMode;
		this.useCAS = useCAS;
		for (int i = 0; i < cache.length; i++) {
			cache[i] = NULL;
		}
	}

	public ArrayStateFactory() {
		this(arrayOption.get(), Updaters.updateOptions.get() == Updaters.UpdateMode.CAS);
	}

	private static AbstractArrayState get0(Object array, ArrayMode mode, boolean useCAS) {
		if (array == null) {
			return NULL; 
		} else {
			//		synchronized(array) {
			int hash = Util.identityHashCode(array);
			AbstractArrayState state = table.get(array, hash);
			if (state != null) {
				return state;
			}
			synchronized(attic) {
				WeakReference<AbstractArrayState> aas = attic.get(array);
				if (aas != null && aas.get() != null) {
					atticHits.inc();
					state = aas.get();
					AbstractArrayState z = table.putIfAbsent(array, state, hash);
					if (z != null && z != state) {
						Assert.panic("Pulled wrong array state for %s from attic: %s != %s", array, state, z);
					}
					return state;
				}
			}
			switch (mode) {
			case NONE:
				Assert.panic("NO array state option....");
			case FINE:
				state = useCAS ? new CASFineArrayState(array) : new FineArrayState(array);
				break;
			case COARSE:
				state = useCAS ? new CASCoarseArrayState(array) : new CoarseArrayState(array);
				break;
			case SPECIAL:
				state = new SpecializingArrayState(array);
				break;
			case USER:
				try {
					state = userArrayOption.get().newInstance(array);
				} catch (Exception ex) {
					Assert.panic(ex);
				}
			}
			return put0(array, state, hash);
			//		}		
		}

	}

	public static AbstractArrayState make(Object array, ArrayMode mode, boolean useCAS) {
		return get0(array, mode, useCAS);
	}

	public AbstractArrayState get(Object array) {
		return get(array, defaultMode, useCAS);		
	}


	public AbstractArrayState get(Object array, ArrayMode mode, boolean useCAS) {
		AbstractArrayState state;

		for (int i = 0; i < CACHE_SIZE; i++) {
			final AbstractArrayState s = cache[i];
			if (s.getArray() == array) {
				return s; 
			}
		}

		state = get0(array, mode, useCAS);
		cache[rotate] = state;
		rotate = (rotate + 1) % cache.length;
		return state;

	}  

	private static AbstractArrayState put0(Object array, AbstractArrayState state, int hash) {
		AbstractArrayState z = table.putIfAbsent(array, state, hash);
		if (z != null) {
			Yikes.yikes("Concurrent array state creation...");
			state = z;
		}
		count++;
		size.inc();
		if (count % mapCheck == 0) {
			synchronized (attic) {
				count = 0;	
				if (mapCheck < MAP_MAX) {
					mapCheck += MAP_INC;
				}
				//	if (count > MAP_THRESHOLD) {
				long start = atticTime.start();
				int oldCount = count;
				int oldAtticSize = attic.size();
				for (AbstractArrayState aas : table.values()) {
					final Object a = aas.array;
					if (!attic.containsKey(a)) {
						attic.put(a, new WeakReference<AbstractArrayState>(aas));
					}
					final AbstractArrayState remove = table.remove(a);
				}
				//		count = table.size();
				Util.logf("ArrayStateFactory Moved Entries to Attic.  Attic size: %d->%d", oldAtticSize, attic.size());
				atticTime.stop(start);
				//}
			}
		}

		return state;
	}	

	public static AbstractArrayState make(Object array) {
		return make(array, arrayOption.get(), Updaters.updateOptions.get() == Updaters.UpdateMode.CAS);
	}
}
