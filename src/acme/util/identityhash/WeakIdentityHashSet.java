package acme.util.identityhash;

import acme.util.identityhash.WeakIdentityHashMap.ValueFunction;

public class WeakIdentityHashSet<E> {

	    private transient WeakIdentityHashMap<E,Object> map;

	    // Dummy value to associate with an Object in the backing Map
	    private static final Object PRESENT = new Object();

	    /**
	     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
	     * default initial capacity (16) and load factor (0.75).
	     */
	    public WeakIdentityHashSet() {
	        map = new WeakIdentityHashMap<E,Object>();
	    }

	    /**
	     * Returns the number of elements in this set (its cardinality).
	     *
	     * @return the number of elements in this set (its cardinality)
	     */
	    public synchronized int size() {
	        return map.size();
	    }

	    /**
	     * Returns <tt>true</tt> if this set contains no elements.
	     *
	     * @return <tt>true</tt> if this set contains no elements
	     */
	    public synchronized boolean isEmpty() {
	        return map.isEmpty();
	    }

	    /**
	     * Returns <tt>true</tt> if this set contains the specified element.
	     * More formally, returns <tt>true</tt> if and only if this set
	     * contains an element <tt>e</tt> such that
	     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	     *
	     * @param o element whose presence in this set is to be tested
	     * @return <tt>true</tt> if this set contains the specified element
	     */
	    public synchronized boolean contains(Object o) {
	        return map.containsKey(o);
	    }

	    /**
	     * Adds the specified element to this set if it is not already present.
	     * More formally, adds the specified element <tt>e</tt> to this set if
	     * this set contains no element <tt>e2</tt> such that
	     * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
	     * If this set already contains the element, the call leaves the set
	     * unchanged and returns <tt>false</tt>.
	     *
	     * @param e element to be added to this set
	     * @return <tt>true</tt> if this set did not already contain the specified
	     * element
	     */
	    public synchronized boolean add(E e) {
	        return map.put(e, PRESENT)==null;
	    }

	    /**
	     * Removes the specified element from this set if it is present.
	     * More formally, removes an element <tt>e</tt> such that
	     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>,
	     * if this set contains such an element.  Returns <tt>true</tt> if
	     * this set contained the element (or equivalently, if this set
	     * changed as a result of the call).  (This set will not contain the
	     * element once the call returns.)
	     *
	     * @param o object to be removed from this set, if present
	     * @return <tt>true</tt> if the set contained the specified element
	     */
	    public synchronized boolean remove(Object o) {
	        return map.remove(o)==PRESENT;
	    }

	    /**
	     * Removes all of the elements from this set.
	     * The set will be empty after this call returns.
	     */
	    public synchronized void clear() {
	        map.clear();
	    }

	    
	    
	    /**
	     * Applies the ValueFunction to all values with non-null keys.
	     * 
	     * @param f	the ValueFunction
	     */
	    public synchronized void applyToAllActiveValues(ValueFunction<E> f) {
	    	map.applyToAllActiveKeys(f);
	    }


	   
}
