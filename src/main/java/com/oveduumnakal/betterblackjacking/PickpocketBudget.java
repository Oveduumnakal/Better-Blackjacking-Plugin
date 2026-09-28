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

/**
 * How many guaranteed pickpockets still fit into one knock-out (PLAN §4.3).
 *
 * <p>A knock-out lands on tick {@code T} and the target wakes on {@code T + duration}. A pickpocket
 * lands {@link KnockoutTracker#PICKPOCKET_LANDING_DELAY} tick after the game processes it and must land
 * before the wake tick, so the last tick one can be processed is {@code wakeTick - 1 - landingDelay}.
 * Pickpockets are processed at least {@link KnockoutTracker#PICKPOCKET_INTERVAL} ticks apart, the
 * first no earlier than {@code T + 1}, and a click made during the current tick is processed next
 * tick. The timing constants live in {@link KnockoutTracker}.
 */
public final class PickpocketBudget
{
	private final int knockOutTick;
	private final int wakeTick;
	private int nextPickpocketTick;

	/**
	 * Starts the budget for a knock-out.
	 *
	 * @param knockOutTick the tick {@code T} the knock-out message arrived on
	 * @param knockOutTicks how many ticks the target stays down, at least 1
	 */
	public PickpocketBudget(int knockOutTick, int knockOutTicks)
	{
		this.knockOutTick = knockOutTick;
		this.wakeTick = knockOutTick + Math.max(1, knockOutTicks);
		this.nextPickpocketTick = knockOutTick + 1;
	}

	/**
	 * The tick the knock-out landed on.
	 *
	 * @return {@code T}
	 */
	public int knockOutTick()
	{
		return knockOutTick;
	}

	/**
	 * The tick the target wakes on, which the knock-out timer counts down to (PLAN §4.4).
	 *
	 * @return {@code T + duration}
	 */
	public int wakeTick()
	{
		return wakeTick;
	}

	/**
	 * The last tick a pickpocket can be processed on and still land before the target wakes.
	 *
	 * @return {@code wakeTick - 1 - landingDelay}
	 */
	public int lastSafeTick()
	{
		return wakeTick - 1 - KnockoutTracker.PICKPOCKET_LANDING_DELAY;
	}

	/**
	 * The earliest tick the player's next pickpocket can be processed on.
	 *
	 * @return {@code T + 1} before any pickpocket, otherwise the last processed tick plus the interval
	 */
	public int nextPickpocketTick()
	{
		return nextPickpocketTick;
	}

	/**
	 * Records a pickpocket the game processed ({@code You attempt to pick the ...'s pocket.}).
	 *
	 * @param tick the tick {@code A} the message arrived on; the next one can be processed at
	 * {@code A + interval}
	 */
	public void onPickpocketProcessed(int tick)
	{
		nextPickpocketTick = tick + KnockoutTracker.PICKPOCKET_INTERVAL;
	}

	/**
	 * How many more pickpockets will land before the target wakes if the player clicks now.
	 *
	 * @param currentTick the current game tick
	 * @return 0 to {@link KnockoutTracker#MAX_PICKPOCKETS}
	 */
	public int pickpocketsLeft(int currentTick)
	{
		int lastSafeTick = lastSafeTick();
		int firstTick = Math.max(nextPickpocketTick, currentTick + 1);
		if (firstTick > lastSafeTick)
			return 0;

		int fit = (lastSafeTick - firstTick) / KnockoutTracker.PICKPOCKET_INTERVAL + 1;
		return Math.min(KnockoutTracker.MAX_PICKPOCKETS, fit);
	}
}
