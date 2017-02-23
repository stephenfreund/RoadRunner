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

package rr.instrument.hooks;

import java.util.Vector;
import java.util.regex.Pattern;

import rr.org.objectweb.asm.commons.Method;

import rr.meta.MetaDataInfoKeys;
import rr.state.ShadowThread;

public class SpecialMethodCallBack {

	protected final Method method;
	protected final String key;
	protected Vector<SpecialMethodListener> listeners = new Vector<SpecialMethodListener>();
	protected final Pattern pattern;

	public SpecialMethodCallBack(String classPattern, String methodString) {
		method = Method.getMethod(methodString);
		key = MetaDataInfoKeys.getMethodKey(classPattern, method.getName(), method.getDescriptor()).replaceAll("\\(", "\\\\(").replaceAll("\\)","\\\\)");
		pattern = Pattern.compile(key);
	}

	public void addListener(SpecialMethodListener l) {
		this.listeners.add(l);
	}

	public void invoke(boolean isPre, Object args[], ShadowThread td) {
		for (SpecialMethodListener l : listeners) {
			l.invoked(this, isPre, args, td);
		}
	}

	public Vector<SpecialMethodListener> getListeners() {
		return listeners;
	}

	public void setListeners(Vector<SpecialMethodListener> listeners) {
		this.listeners = listeners;
	}

	public String getKey() {
		return key;
	}

	public Method getMethod() {
		return method;
	}

	public boolean matches(String other) {
		return pattern.matcher(other).matches();
	}	
	
	public String toString() {
		return key + "->" + listeners;
	}
}
