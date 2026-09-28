/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oveduumnakal.betterblackjacking;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Covers the sub-tick clock's interpolation and clamping with a fake time source. */
public class SubTickClockTest
{
	private static final long MS = 1_000_000L;

	private final AtomicLong now = new AtomicLong(5_000_000_000L);

	private SubTickClock clock;

	@Before
	public void setUp()
	{
		clock = new SubTickClock(now::get);
	}

	@Test
	public void nothingHasElapsedBeforeTheFirstTick()
	{
		assertEquals(0, clock.millisSinceTick());
		assertEquals(3000, clock.remainingMillis(105, 100));
	}

	@Test
	public void millisSinceTickCountsFromTheLastTick()
	{
		clock.onTick();
		now.addAndGet(250 * MS);
		assertEquals(250, clock.millisSinceTick());

		clock.onTick();
		assertEquals(0, clock.millisSinceTick());
		now.addAndGet(40 * MS);
		assertEquals(40, clock.millisSinceTick());
	}

	@Test
	public void remainingIsWholeTicksMinusTimeIntoTheTick()
	{
		clock.onTick();
		assertEquals(3000, clock.remainingMillis(105, 100));
		now.addAndGet(250 * MS);
		assertEquals(2750, clock.remainingMillis(105, 100));
		assertEquals(350, clock.remainingMillis(101, 100));
	}

	@Test
	public void interpolationIsAccurateToWithinOneMillisecond()
	{
		clock.onTick();
		long tickStart = now.get();
		for (long offset = 0; offset <= 600 * MS; offset += 7_300_123L)
		{
			now.set(tickStart + offset);
			double exact = 5 * 600 - offset / (double) MS;
			long remaining = clock.remainingMillis(105, 100);
			assertTrue("offset " + offset + "ns gave " + remaining, Math.abs(remaining - exact) < 1);
		}
	}

	@Test
	public void remainingCountsDownSmoothlyAcrossTicks()
	{
		long previous = Long.MAX_VALUE;
		for (int tick = 100; tick < 105; tick++)
		{
			clock.onTick();
			for (int ms = 0; ms < 600; ms += 50)
			{
				long remaining = clock.remainingMillis(105, tick);
				assertTrue(remaining < previous);
				assertEquals((105 - tick) * 600 - ms, remaining);
				previous = remaining;
				now.addAndGet(50 * MS);
			}
		}
	}

	@Test
	public void remainingIsClampedAtZero()
	{
		clock.onTick();
		now.addAndGet(900 * MS);
		assertEquals(0, clock.remainingMillis(101, 100));
		assertEquals(0, clock.remainingMillis(100, 100));
		assertEquals(0, clock.remainingMillis(95, 100));
	}

	@Test
	public void remainingIsClampedAtTheWholeTicks()
	{
		clock.onTick();
		now.addAndGet(-50 * MS);
		assertEquals(0, clock.millisSinceTick());
		assertEquals(1200, clock.remainingMillis(102, 100));
	}

	@Test
	public void resetForgetsTheLastTick()
	{
		clock.onTick();
		now.addAndGet(300 * MS);
		clock.reset();
		assertEquals(0, clock.millisSinceTick());
		assertEquals(600, clock.remainingMillis(101, 100));
	}

	@Test
	public void defaultClockUsesTheSystemTime()
	{
		SubTickClock system = new SubTickClock();
		system.onTick();
		long remaining = system.remainingMillis(2, 1);
		assertTrue(remaining > 0 && remaining <= 600);
	}
}
