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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the small enums the overlays and wiring share: their display names, IDs and helpers. */
public class SharedContractsTest
{
	@Test
	public void timerStylesShowTheirDisplayNames()
	{
		assertEquals(Arrays.asList("Pie", "Seconds", "Off"), labels(TimerStyle.values()));
	}

	@Test
	public void wakeAnimationsCarryTheCacheAnimationIds()
	{
		assertEquals(-1, WakeAnimation.NO_ANIMATION);
		assertEquals(WakeAnimation.NO_ANIMATION, WakeAnimation.OFF.animationId());
		assertEquals(7122, WakeAnimation.MAX_GET_UP.animationId());
	}

	@Test
	public void wakeAnimationsShowTheirDisplayNames()
	{
		assertEquals(Arrays.asList("Off", "Max get up"), labels(WakeAnimation.values()));
	}

	@Test
	public void onlySafeAndWakingAreKnockedOut()
	{
		EnumSet<TargetState> knockedOut = EnumSet.of(TargetState.SAFE, TargetState.WAKING);
		for (TargetState state : TargetState.values())
			assertEquals(state.name(), knockedOut.contains(state), state.isKnockedOut());

		assertTrue(TargetState.SAFE.isKnockedOut());
		assertFalse(TargetState.ATTACKING.isKnockedOut());
	}

	private static List<String> labels(Object[] values)
	{
		return Arrays.stream(values)
				.map(Object::toString)
				.collect(Collectors.toList());
	}
}
