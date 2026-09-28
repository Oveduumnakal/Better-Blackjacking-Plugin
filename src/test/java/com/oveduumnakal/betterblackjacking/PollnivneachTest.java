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

import org.junit.Test;

import net.runelite.api.coords.WorldPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the Pollnivneach area: its two regions, their corners and the tiles just outside them. */
public class PollnivneachTest
{
	@Test
	public void townIsRegions13357And13358()
	{
		assertEquals(13358, Pollnivneach.NORTH_REGION_ID);
		assertEquals(13357, Pollnivneach.SOUTH_REGION_ID);
		assertTrue(Pollnivneach.containsRegion(13357));
		assertTrue(Pollnivneach.containsRegion(13358));
	}

	@Test
	public void neighbouringRegionsAreOutside()
	{
		assertFalse(Pollnivneach.containsRegion(13356));
		assertFalse(Pollnivneach.containsRegion(13359));
		assertFalse(Pollnivneach.containsRegion(13101));
		assertFalse(Pollnivneach.containsRegion(13102));
		assertFalse(Pollnivneach.containsRegion(13613));
		assertFalse(Pollnivneach.containsRegion(13614));
		assertFalse(Pollnivneach.containsRegion(-1));
	}

	@Test
	public void cornersOfBothRegionsAreInside()
	{
		assertInside(3328, 2880);
		assertInside(3391, 2880);
		assertInside(3328, 2943);
		assertInside(3391, 2943);
		assertInside(3328, 2944);
		assertInside(3391, 2944);
		assertInside(3328, 3007);
		assertInside(3391, 3007);
	}

	@Test
	public void tilesJustOutsideTheCornersAreOutside()
	{
		assertOutside(3327, 2880);
		assertOutside(3328, 2879);
		assertOutside(3392, 2880);
		assertOutside(3391, 2879);
		assertOutside(3327, 3007);
		assertOutside(3328, 3008);
		assertOutside(3392, 3007);
		assertOutside(3391, 3008);
		assertOutside(3327, 2943);
		assertOutside(3392, 2944);
	}

	@Test
	public void wikiSpawnAreasAreInside()
	{
		assertInside(3354, 2984);
		assertInside(3371, 3003);
		assertInside(3333, 2945);
		assertInside(3352, 2961);
		assertInside(3350, 2942);
	}

	@Test
	public void planeDoesNotMatter()
	{
		assertTrue(Pollnivneach.contains(new WorldPoint(3360, 2990, 1)));
		assertTrue(Pollnivneach.contains(new WorldPoint(3360, 2990, 3)));
		assertFalse(Pollnivneach.contains(new WorldPoint(3392, 2990, 1)));
	}

	@Test
	public void nullIsOutside()
	{
		assertFalse(Pollnivneach.contains(null));
	}

	private static void assertInside(int x, int y)
	{
		assertTrue("(" + x + ", " + y + ")", Pollnivneach.contains(new WorldPoint(x, y, 0)));
	}

	private static void assertOutside(int x, int y)
	{
		assertFalse("(" + x + ", " + y + ")", Pollnivneach.contains(new WorldPoint(x, y, 0)));
	}
}
