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

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Replays the two recorded bandit sessions (PLAN §2) and checks T4's acceptance criteria as it goes:
 * SAFE at every knock-out, KNOCK_OUT exactly at T+5 or at {@code Arghh my head.} (whichever comes
 * first), ATTACKING after every failed knock-out, and two pickpockets left at T but one after a
 * pickpocket processed at T+1.
 */
public class KnockoutTrackerReplayTest
{
	@Test
	public void session1MeetsTheAcceptanceCriteria() throws IOException
	{
		Checker checker = replay("session1-bandit.log");
		assertEquals(9, checker.knockOuts);
		assertEquals(9, checker.wakes);
		assertEquals(4, checker.glances);
		assertEquals(3, checker.pickpocketsAtT1);
		assertEquals(6, checker.laterPickpockets);
	}

	@Test
	public void session2MeetsTheAcceptanceCriteria() throws IOException
	{
		Checker checker = replay("session2-bandit.log");
		assertEquals(10, checker.knockOuts);
		assertEquals(10, checker.wakes);
		assertEquals(4, checker.glances);
		assertEquals(6, checker.pickpocketsAtT1);
		assertEquals(4, checker.laterPickpockets);
	}

	private static Checker replay(String fixture) throws IOException
	{
		FixtureReplay replay = FixtureReplay.load(fixture);
		KnockoutTracker tracker = new KnockoutTracker();
		Checker checker = new Checker(tracker, replay);
		replay.replay(tracker, checker);
		return checker;
	}

	/** Asserts the acceptance criteria while a fixture replays, and counts how often each one ran. */
	private static final class Checker implements FixtureReplay.Listener
	{
		private final KnockoutTracker tracker;
		private final FixtureReplay replay;
		private int target = KnockoutTracker.NO_NPC;
		private int knockOutTick;
		private boolean awake = true;
		private int glanceTick = KnockoutTracker.NO_TICK;
		private int knockOuts;
		private int wakes;
		private int glances;
		private int pickpocketsAtT1;
		private int laterPickpockets;

		Checker(KnockoutTracker tracker, FixtureReplay replay)
		{
			this.tracker = tracker;
			this.replay = replay;
		}

		@Override
		public void afterTick(int tick)
		{
			if (awake)
				return;

			if (tick < knockOutTick + KnockoutTracker.DEFAULT_KNOCK_OUT_TICKS)
			{
				assertTrue("still down at tick " + tick, tracker.stateOf(target).isKnockedOut());
				return;
			}

			assertEquals("awake at T+5, tick " + tick, TargetState.KNOCK_OUT, tracker.stateOf(target));
			awake = true;
			wakes++;
		}

		@Override
		public void afterLine(FixtureReplay.Line line)
		{
			if (line.kind == FixtureReplay.Kind.KNOCKOUT)
			{
				checkKnockOut(line);
				return;
			}

			if (line.kind == FixtureReplay.Kind.CHAT && line.text.startsWith("Your blow only glances off"))
				glanceTick = line.tick;

			if (awake)
				return;

			if (line.kind == FixtureReplay.Kind.OVERHEAD && line.actor == target
					&& "Arghh my head.".equals(line.text))
			{
				assertEquals(line.toString(), TargetState.KNOCK_OUT, tracker.stateOf(target));
				awake = true;
				wakes++;
				return;
			}

			assertTrue(line.toString(), tracker.stateOf(target).isKnockedOut());
			if (line.kind == FixtureReplay.Kind.CHAT && line.text.startsWith("You attempt to pick"))
				checkPickpocket(line);
		}

		@Override
		public void endOfTick(int tick)
		{
			if (tick != glanceTick)
				return;

			int glanced = replay.playerTarget();
			assertNotEquals("glance at tick " + tick + " has a target", KnockoutTracker.NO_NPC, glanced);
			assertEquals("attacking after the glance at tick " + tick, TargetState.ATTACKING, tracker.stateOf(glanced));
			glances++;
		}

		private void checkKnockOut(FixtureReplay.Line line)
		{
			target = line.target;
			knockOutTick = line.tick;
			awake = false;
			knockOuts++;
			assertEquals(line.toString(), TargetState.SAFE, tracker.stateOf(target));
			assertEquals(line.toString(), target, tracker.knockedOutNpcIndex());
			assertEquals(line.toString(), line.tick, tracker.knockOutTick());
			assertEquals(line.toString(), line.tick + 5, tracker.wakeTick());
			assertEquals(line.toString(), 2, tracker.pickpocketsLeft());
		}

		private void checkPickpocket(FixtureReplay.Line line)
		{
			if (line.tick == knockOutTick + 1)
			{
				assertEquals(line.toString(), 1, tracker.pickpocketsLeft());
				assertEquals(line.toString(), TargetState.SAFE, tracker.stateOf(target));
				pickpocketsAtT1++;
			}
			else
			{
				assertEquals(line.toString(), 0, tracker.pickpocketsLeft());
				assertEquals(line.toString(), TargetState.WAKING, tracker.stateOf(target));
				laterPickpockets++;
			}
		}
	}
}
