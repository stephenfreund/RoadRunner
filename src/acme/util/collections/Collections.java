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

package acme.util.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper methods for converting between various collection types.
 */
public class Collections {

	/**
	 * A method to convert a variable number of arguments into a List object
	 */
	public static <T> List<T> listify(T... ts) {
		ArrayList<T> v = new ArrayList<T>();
		for (T t : ts) {
			v.add(t);
		}
		return v;
	}

	/**
	 * A method to convert a variable number of arguments into a Set object
	 */
	public static <T> Set<T> setify(T... ts) {
		Set<T> v = new BetterSet<T>();
		for (T t : ts) {
			v.add(t);
		}
		return v;
	}

	/**
	 * A method to convert a variable number of arguments into a List object
	 */
	public static <T> List<T> listify(Iterable<T> ts) {
		ArrayList<T> v = new ArrayList<T>();
		for (T t : ts) {
			v.add(t);
		}
		return v;
	}

	/**
	 * A method to convert a variable number of arguments into a Set object
	 */
	public static <T> Set<T> setify(Iterable<T> ts) {
		Set<T> v = new BetterSet<T>();
		for (T t : ts) {
			v.add(t);
		}
		return v;
	}

	public static <T> String listToString(Iterable<T> list, String sep) {
		boolean first = true;
		String result = "";
		for (T t : list) {
			if (!first) {
				result += sep;
			}
			result += t;
			first = false;
		}
		return result;
	}
	
	public static <T> String listToString(Iterable<T> list) {
		return listToString(list, ",");
	}

	public static <T extends Comparable<T>, U> String mapToOrderedString(Map<T,U> map, String sep) {
		return mapToOrderedString(map, sep, -1);
	}

	public static <T extends Comparable<T>, U> String mapToOrderedString(Map<T,U> map, String sep, int limit) {
		String result = "";
		Set<T> a = map.keySet();
		ArrayList<T> s = new ArrayList<T>(a);
		java.util.Collections.sort(s);
		int count = 0;
		for (T p :s) {
			if (count > 0) {
				result += sep;
			} 
			if (count == limit) {
				result += "...";
				break;
			}
			count++;
			result += p + "->" + map.get(p);
		}
		return result;
	}
	
}
