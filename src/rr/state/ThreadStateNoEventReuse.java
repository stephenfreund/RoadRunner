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

import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassInitializedEvent;
import rr.event.FieldAccessEvent;
import rr.event.InterruptEvent;
import rr.event.JoinEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;

/*
 * RRExperimental.  To measure cost of Event object creation.
 */
public final class ThreadStateNoEventReuse extends ShadowThread {

	protected ThreadStateNoEventReuse(final Thread thread, final ShadowThread parent) {
		super(thread, parent);
	}

	@Override
	public FieldAccessEvent getFieldAccessEvent() {
		return new FieldAccessEvent(this);
	}

	@Override
	public VolatileAccessEvent getVolatileAccessEvent() {
		return new VolatileAccessEvent(this);
	}

	@Override
	public ArrayAccessEvent getArrayAccessEvent() {
		return new ArrayAccessEvent(this);
	}

	@Override
	public AcquireEvent getAcquireEvent() {
		return new AcquireEvent(this);
	}

	@Override
	public ReleaseEvent getReleaseEvent() {
		return new ReleaseEvent(this);
	}

	@Override
	public StartEvent getStartEvent() {
		return new StartEvent(this);
	}

	@Override
	public WaitEvent getWaitEvent() {
		return new WaitEvent(this);
	}

	@Override
	public JoinEvent getJoinEvent() {
		return new JoinEvent(this);
	}

	@Override
	public InterruptEvent getInterruptEvent() {
		return new InterruptEvent(this);
	}

	@Override
	public NotifyEvent getNotifyEvent() {
		return new NotifyEvent(this);
	}

	@Override
	public SleepEvent getSleepEvent() {
		return new SleepEvent(this);
	}

	@Override
	public ClassInitializedEvent getClassInitEvent() {
		return new ClassInitializedEvent(this);
	}
}
