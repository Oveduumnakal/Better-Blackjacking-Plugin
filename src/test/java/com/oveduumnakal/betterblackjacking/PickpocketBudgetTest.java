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

import static org.junit.Assert.assertEquals;

/** Covers the pickpocket formula in PLAN §4.3, with the knock-out on tick T = 100. */
public class PickpocketBudgetTest
{
	private static final int T = 100;

	@Test
	public void theTimingConstantsMatchThePlan()
	{
		assertEquals(5, KnockoutTracker.DEFAULT_KNOCK_OUT_TICKS);
		assertEquals(1, KnockoutTracker.PICKPOCKET_LANDING_DELAY);
		assertEquals(2, KnockoutTracker.PICKPOCKET_INTERVAL);
		assertEquals(2, KnockoutTracker.MAX_PICKPOCKETS);
		assertEquals(2, KnockoutTracker.ATTACKING_EXIT_TICKS);
	}

	@Test
	public void aFreshKnockOutWakesAtTPlus5WithTheLastSafeTickAtTPlus3()
	{
		PickpocketBudget budget = new PickpocketBudget(T, 5);
		assertEquals(T, budget.knockOutTick());
		assertEquals(T + 5, budget.wakeTick());
		assertEquals(T + 3, budget.lastSafeTick());
		assertEquals(T + 1, budget.nextPickpocketTick());
	}

	@Test
	public void withoutPickpocketsTheBudgetShrinksAsTicksPass()
	{
		PickpocketBudget budget = new PickpocketBudget(T, 5);
		assertEquals(2, budget.pickpocketsLeft(T));
		assertEquals(1, budget.pickpocketsLeft(T + 1));
		assertEquals(1, budget.pickpocketsLeft(T + 2));
		assertEquals(0, budget.pickpocketsLeft(T + 3));
		assertEquals(0, budget.pickpocketsLeft(T + 4));
		assertEquals(0, budget.pickpocketsLeft(T + 5));
	}

	@Test
	public void aPickpocketAtTPlus1LeavesOneMore()
	{
		PickpocketBudget budget = new PickpocketBudget(T, 5);
		budget.onPickpocketProcessed(T + 1);
		assertEquals(T + 3, budget.nextPickpocketTick());
		assertEquals(1, budget.pickpocketsLeft(T + 1));
		assertEquals(1, budget.pickpocketsLeft(T + 2));
		budget.onPickpocketProcessed(T + 3);
		assertEquals(0, budget.pickpocketsLeft(T + 3));
	}

	@Test
	public void aPickpocketAtTPlus2LeavesNone()
	{
		PickpocketBudget budget = new PickpocketBudget(T, 5);
		budget.onPickpocketProcessed(T + 2);
		assertEquals(0, budget.pickpocketsLeft(T + 2));
	}

	@Test
	public void aLongerKnockOutStillCapsAtTwo()
	{
		PickpocketBudget budget = new PickpocketBudget(T, 8);
		assertEquals(T + 6, budget.lastSafeTick());
		assertEquals(2, budget.pickpocketsLeft(T));
		budget.onPickpocketProcessed(T + 1);
		assertEquals(2, budget.pickpocketsLeft(T + 1));
		budget.onPickpocketProcessed(T + 3);
		assertEquals(1, budget.pickpocketsLeft(T + 3));
	}

	@Test
	public void aShortKnockOutFitsFewerPickpockets()
	{
		assertEquals(1, new PickpocketBudget(T, 3).pickpocketsLeft(T));
		assertEquals(0, new PickpocketBudget(T, 2).pickpocketsLeft(T));
	}

	@Test
	public void theDurationIsAtLeastOneTick()
	{
		assertEquals(T + 1, new PickpocketBudget(T, 0).wakeTick());
	}
}
