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

import rr.RRMain;
import rr.meta.FieldAccessInfo;
import rr.meta.FieldInfo;
import rr.meta.MetaDataInfoMaps;
import acme.util.ResourceManager;
import acme.util.WeakResourceManager;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.count.Counter;
import acme.util.decorations.Decoratable;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;

/**
 * The state associated with a volatile memory location.  Tools should save information about volatiles as decorations
 * on the ShadowVolatile object.
 */
public class ShadowVolatile extends Decoratable {

	private static final DecorationFactory<ShadowVolatile> decoratorFactory = new DecorationFactory<ShadowVolatile>(); 

	/**
	 * Create a new decoration for volatile variables.
	 * @param <T>
	 * @param name
	 * @param type
	 * @param defaultValueMaker
	 */
	public static <T> Decoration<ShadowVolatile, T> makeDecoration(String name, DecorationFactory.Type type, DefaultValue<ShadowVolatile, T> defaultValueMaker) {
		return decoratorFactory.make(name, type, defaultValueMaker);
	}

	private static final Counter count = new Counter("ShadowVolatile", "objects");

	private final WeakReference<Object> target;
	private final FieldInfo fd;

	private final int hashCode;

	private ShadowVolatile(Object target, FieldInfo fd) {
		// Assert.assertTrue(target != null || fd.isStatic());
		this.target = new WeakReference<Object>(target);
		this.fd = fd;
		this.hashCode = Util.identityHashCode(target) + Util.identityHashCode(fd);
		if (RRMain.slowMode()) count.inc();
	}

	@Override
	public final int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "VOLATILE " + Util.objectToIdentityString(this.getTarget()) + "." + getField().getName();
	}

	/**
	 * This may return null in two cases:
	 *   1) if the field is a static field.
	 *   2) if the owning object has already been garbage collected.
	 */
	public Object getTarget() {
		Object l = target.get();
		if (l == null && !fd.isStatic()) Yikes.yikes("Getting target of ShadowVolatile after target has been gc'd");
		return l;
	}

	public FieldInfo getField() {
		return fd;
	}

	/*
	 * Use Weak Resource Managers to avoid pinning down objects
	 * that could be collected.
	 */
	private static class ByFieldTable extends ResourceManager<FieldInfo,WeakResourceManager<Object,ShadowVolatile>> {

		public ByFieldTable() {
			super(11);
		}

		@Override
		protected WeakResourceManager<Object, ShadowVolatile> make(final FieldInfo field) {
			return new WeakResourceManager<Object, ShadowVolatile>() {

				@Override
				protected ShadowVolatile make(Object obj) {
					return new ShadowVolatile(obj, field);
				}

			};
		}
	}

	private static ByFieldTable byField = new ByFieldTable();

	/**
	 * Get the ShadowVolatile for the fd field of the target object.
	 * @param target
	 * @param fd
	 */
	public static ShadowVolatile get(Object target, FieldInfo fd) {
		return byField.get(fd).get(target);

	}
	
	/**
	 * @RRInternal
	 * Used in instrumentor to get the ShadowVolatile, which will be the lock to ensure
	 * atomicity of access/event.
	 */
	public static ShadowVolatile get(Object target, int fieldAccessId) {
		FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(fieldAccessId);
		return ShadowVolatile.get(target, fad.getField());
	}


}
