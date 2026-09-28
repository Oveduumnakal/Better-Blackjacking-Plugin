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

import net.runelite.api.coords.WorldPoint;

/**
 * The area the plugin works in: the town of Pollnivneach.
 *
 * <p>The town is taken to be map regions {@value #NORTH_REGION_ID} (x 3328–3391, y 2944–3007) and
 * {@value #SOUTH_REGION_ID} (x 3328–3391, y 2880–2943, which holds the town's southern edge). Every
 * Pollnivneach NPC spawn on the OSRS Wiki falls in one of them. The plane doesn't matter.
 */
final class Pollnivneach
{
	/** The region holding most of the town, including the bandits and thugs. */
	static final int NORTH_REGION_ID = 13358;

	/** The region just south of {@link #NORTH_REGION_ID}, holding the town's southern edge. */
	static final int SOUTH_REGION_ID = 13357;

	private Pollnivneach()
	{
	}

	/**
	 * Whether a map region is part of Pollnivneach.
	 *
	 * @param regionId the region ID, as from {@link WorldPoint#getRegionID()}
	 * @return {@code true} for regions {@value #NORTH_REGION_ID} and {@value #SOUTH_REGION_ID}
	 */
	static boolean containsRegion(int regionId)
	{
		return regionId == NORTH_REGION_ID || regionId == SOUTH_REGION_ID;
	}

	/**
	 * Whether a world point is in Pollnivneach.
	 *
	 * @param point the point, e.g. the local player's world location; may be {@code null}
	 * @return {@code true} if the point is in one of the town's regions; {@code false} for {@code null}
	 */
	static boolean contains(WorldPoint point)
	{
		return point != null && containsRegion(point.getRegionID());
	}
}
