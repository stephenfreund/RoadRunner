package rr.state;

import acme.util.Assert;
import acme.util.Util;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;


/*
 * Types of caching:
 *   - NONE: No attempt to cache shadows for arrays at each array access.
 *   - ORIG: Caches contain array/shadow pairs.  Caches are never cleared
 *           so some memory may get pinned down after target stops referencing
 *           it
 *   - WEAK: Caches contain weak pointers to array/shadow pairs, so eventually
 *           all garbage arrays are reclaimed, but this slows all caching ops.
 *   - STRONG: Keep strong pointers, but periodically reset all caches so
 *             memory will be reclaimed reasonably fast.
 */
public abstract class AbstractArrayStateCache {

	public static final AbstractArrayStateCache[] caches = new AbstractArrayStateCache[30000];
	protected static volatile int count = 0;
	protected final String tag;
	protected final int id;

	private static enum CacheType { NONE, ORIG, WEAK, STRONG }
	//	public static final CommandLineOption<Boolean> noOptimizedArrayLookupOption = 
	//		CommandLine.makeBoolean("noArrayLookupOpt", false, CommandLineOption.Kind.EXPERIMENTAL, "Turn of Array lookup optimizations.");

	public static final CommandLineOption<CacheType> cacheTypeOption = 
			CommandLine.makeEnumChoice("arrayCacheType", CacheType.STRONG, CommandLineOption.Kind.EXPERIMENTAL, "Set array shadow cache type.", CacheType.class);

	public static AbstractArrayStateCache make(String tag) {
		if (count == caches.length) {
			Assert.panic("Too many array accesses in code.  Change Constant in " + ArrayStateCache.class);
		}
		synchronized(AbstractArrayStateCache.class) {
			switch (cacheTypeOption.get()) {
			case NONE: caches[count] = new UnoptimizedArrayStateCache(tag, count); break;
			case ORIG: caches[count] = new ArrayStateCache(tag, count); break;
			case WEAK: caches[count] = new ArrayStateCacheWeak(tag, count); break;
			case STRONG: caches[count] = new ArrayStateCacheStrong(tag, count); break;
			}
			return caches[count++];
		}
	}

	/*
	 * Clear all caches for Thread tid.
	 */
	public static final void clearAll(int tid) {
		//		Util.logf("Clearing Caches for " + tid);
		int n = count;
		for (int i = 0; i < n; i++) {
			caches[i].clear(tid);
		}
	}

	abstract protected void clear(int tid);

	public static AbstractArrayState get(Object array, ShadowThread td, int cacheId) {
		return caches[cacheId].get(array, td);
	}

	public AbstractArrayState get(Object array, ShadowThread td) {
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
