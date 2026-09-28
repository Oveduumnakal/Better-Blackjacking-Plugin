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
package com.oveduumnakal.betterblackjacking.dev;

import java.util.function.IntUnaryOperator;

import net.runelite.api.gameval.AnimationID;

/**
 * The cycle-by-cycle script of one preview clip: which animation plays from which client cycle.
 *
 * <p>A clip is a run of segments played back to back. A get-up clip lies unconscious for
 * {@link #LYING_CYCLES}, plays each get-up animation in turn for its full length, then stands idle
 * for {@link #STANDING_CYCLES}. A client cycle is 20 ms.
 */
final class PreviewTimeline
{
	/** The animation ID that clears the actor's animation, leaving it standing idle. */
	static final int IDLE = -1;

	/** How long a get-up clip lies unconscious before the get-up starts: 0.8 s. */
	static final int LYING_CYCLES = 40;

	/** How long a get-up clip stands after the get-up ends: 0.8 s. */
	static final int STANDING_CYCLES = 40;

	private final int[] animationIds;

	private final int[] startCycles;

	private final int length;

	/**
	 * Creates a timeline that plays each animation for the matching number of cycles, in order.
	 *
	 * @param animationIds the animation of each segment, or {@link #IDLE} to stand idle
	 * @param cycles the length of each segment in client cycles; each must be positive
	 * @throws IllegalArgumentException if the arrays differ in length or a segment isn't positive
	 */
	PreviewTimeline(int[] animationIds, int[] cycles)
	{
		if (animationIds.length != cycles.length)
			throw new IllegalArgumentException("Every animation needs a length");

		this.animationIds = animationIds.clone();
		startCycles = new int[cycles.length];
		int at = 0;
		for (int i = 0; i < cycles.length; i++)
		{
			if (cycles[i] <= 0)
				throw new IllegalArgumentException("Segment " + i + " has no length: " + cycles[i]);

			startCycles[i] = at;
			at += cycles[i];
		}

		length = at;
	}

	/**
	 * Creates a get-up clip: lying unconscious, then each get-up animation back to back, then standing.
	 *
	 * @param getUpIds the get-up animations, in play order
	 * @param cyclesOf the length of an animation in client cycles, given its ID
	 * @return the clip's timeline
	 */
	static PreviewTimeline getUp(int[] getUpIds, IntUnaryOperator cyclesOf)
	{
		final int[] ids = new int[getUpIds.length + 2];
		final int[] cycles = new int[ids.length];
		ids[0] = AnimationID.HUMAN_UNCONSCIOUS;
		cycles[0] = LYING_CYCLES;
		for (int i = 0; i < getUpIds.length; i++)
		{
			ids[i + 1] = getUpIds[i];
			cycles[i + 1] = cyclesOf.applyAsInt(getUpIds[i]);
		}

		ids[ids.length - 1] = IDLE;
		cycles[cycles.length - 1] = STANDING_CYCLES;
		return new PreviewTimeline(ids, cycles);
	}

	/**
	 * Returns an animation's length in client cycles from its frame lengths.
	 *
	 * @param frameLengths the length of each frame in client cycles, or {@code null} if unknown
	 * @return the sum of the frame lengths; 0 if they are unknown
	 */
	static int cyclesOf(int[] frameLengths)
	{
		if (frameLengths == null)
			return 0;

		int total = 0;
		for (int frameLength : frameLengths)
			total += frameLength;

		return total;
	}

	/**
	 * Returns the clip's length.
	 *
	 * @return the total number of client cycles across every segment
	 */
	int length()
	{
		return length;
	}

	/**
	 * Returns whether a segment starts on a cycle, so its animation must be set then.
	 *
	 * @param cycle the client cycle since the clip began
	 * @return {@code true} if a segment starts on that cycle
	 */
	boolean startsAt(int cycle)
	{
		for (int start : startCycles)
		{
			if (start == cycle)
				return true;
		}

		return false;
	}

	/**
	 * Returns the animation in play on a cycle.
	 *
	 * @param cycle the client cycle since the clip began
	 * @return the animation of the segment covering the cycle; {@link #IDLE} before the start or after the end
	 */
	int animationAt(int cycle)
	{
		if (cycle < 0 || cycle >= length)
			return IDLE;

		int segment = 0;
		while (segment + 1 < startCycles.length && startCycles[segment + 1] <= cycle)
			segment++;

		return animationIds[segment];
	}
}
