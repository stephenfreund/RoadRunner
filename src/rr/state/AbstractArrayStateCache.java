package rr.state;

import acme.util.Assert;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public abstract class AbstractArrayStateCache {

	public static final AbstractArrayStateCache[] caches = new AbstractArrayStateCache[30000];
	protected static int count = 0;
	protected final String tag;
	protected final int id;


	public static final CommandLineOption<Boolean> noOptimizedArrayLookupOption = 
		CommandLine.makeBoolean("noArrayLookupOpt", false, CommandLineOption.Kind.EXPERIMENTAL, "Turn of Array lookup optimizations.");


	public static AbstractArrayStateCache make(String tag) {
		if (count == caches.length) {
			Assert.panic("Too many array accesses in code.  Change Constant in " + ArrayStateCache.class);
		}
		caches[count] = noOptimizedArrayLookupOption.get() ? new UnoptimizedArrayStateCache(tag, count) : new ArrayStateCacheWeak(tag, count);
		return caches[count++];
	}

	// clear out any cached array states for thread tid
	public abstract void clear(int tid);
	
	public static void clearAll(int tid) {
		for (int i = 0; i < count; i++)  {
			caches[i].clear(tid);
		}
	}
	
	public static AbstractArrayState get(Object array, ShadowThread td, int cacheId) {
		return caches[cacheId].get(array, td);
	}

	public AbstractArrayState get(Object array, ShadowThread td) {
		// TODO Auto-generated method stub
		return null;
	}

	public AbstractArrayStateCache(String tag, int id) {
		this.id = id;
		this.tag = tag;
	}

	public String getTag() {
		return tag;
	}

	public int getId() {
		return id;
	}

}
