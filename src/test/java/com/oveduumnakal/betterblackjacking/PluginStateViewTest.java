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

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import net.runelite.api.ChatMessageType;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link PluginStateView}: the sub-tick countdown and duration, the state colours, the gate's
 * reset on close and the cached eligible targets. Time comes from a fake nanosecond source.
 */
public class PluginStateViewTest
{
	private static final long NANOS_PER_MILLI = 1_000_000L;

	private static final String KNOCKOUT = "You smack the bandit over the head and render them unconscious.";

	private static final ActivationGate OPEN =
			ActivationGate.of(Pollnivneach.NORTH_REGION_ID, ItemID.BLACKJACK_OAK, 99);

	private final BetterBlackjackingConfig config = mock(BetterBlackjackingConfig.class, Answers.CALLS_REAL_METHODS);

	private long nanos;

	private PluginStateView view;

	private NPC bandit;

	@Before
	public void setUp()
	{
		view = new PluginStateView(config, new SubTickClock(() -> nanos));
		bandit = npc(7, NpcID.FEUD_ARABIAN_GUARD2_1);
		view.addTarget(bandit);
		view.setGate(OPEN);
	}

	@Test
	public void remainingMillisIsZeroWithNothingKnockedOut()
	{
		tick(100);

		assertEquals(0, view.remainingMillis());
		assertEquals(0, view.pickpocketsLeft());
	}

	@Test
	public void remainingMillisCountsDownBetweenTicks()
	{
		tick(100);
		view.tracker().onChat(100, ChatMessageType.SPAM, KNOCKOUT, bandit.getIndex());
		assertEquals(5 * SubTickClock.TICK_MILLIS, view.remainingMillis());

		nanos += 250 * NANOS_PER_MILLI;
		assertEquals(5 * SubTickClock.TICK_MILLIS - 250, view.remainingMillis());

		nanos += 350 * NANOS_PER_MILLI;
		tick(101);
		assertEquals(4 * SubTickClock.TICK_MILLIS, view.remainingMillis());

		for (int t = 102; t <= 105; t++)
			tick(t);

		assertEquals(0, view.remainingMillis());
	}

	@Test
	public void durationMillisFollowsTheKnockOutTicks()
	{
		assertEquals(3000, view.durationMillis());

		view.tracker().setKnockOutTicks(7);

		assertEquals(4200, view.durationMillis());
	}

	@Test
	public void stateOfReadsTheTrackerByIndex()
	{
		tick(100);
		view.tracker().onChat(100, ChatMessageType.SPAM, KNOCKOUT, bandit.getIndex());

		assertEquals(TargetState.SAFE, view.stateOf(bandit));
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(npc(8, NpcID.FEUD_ARABIAN_GUARD2_1)));
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(null));
		assertEquals(2, view.pickpocketsLeft());
	}

	@Test
	public void colorOfUsesTheConfiguredColours()
	{
		when(config.safeColor()).thenReturn(Color.BLUE);

		assertEquals(Color.BLUE, view.colorOf(TargetState.SAFE));
		assertEquals(config.wakingColor(), view.colorOf(TargetState.WAKING));
		assertEquals(config.knockOutColor(), view.colorOf(TargetState.KNOCK_OUT));
		assertEquals(config.attackingColor(), view.colorOf(TargetState.ATTACKING));
		assertEquals(config.knockOutColor(), view.colorOf(null));
	}

	@Test
	public void closingTheGateResetsTheTracker()
	{
		tick(100);
		view.tracker().onChat(100, ChatMessageType.SPAM, KNOCKOUT, bandit.getIndex());

		assertFalse(view.setGate(OPEN.withThievingLevel(50)));
		assertEquals(TargetState.SAFE, view.stateOf(bandit));

		assertTrue(view.setGate(OPEN.withWeaponItemId(ActivationGate.NO_WEAPON)));
		assertFalse(view.isActive());
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
	}

	@Test
	public void eligibleTargetsAreCachedUntilTheGateOrTargetsChange()
	{
		Collection<NPC> first = view.eligibleTargets();
		assertEquals(Collections.singletonList(bandit), first);
		assertSame(first, view.eligibleTargets());

		NPC thug = npc(9, NpcID.FEUD_EGYPTIAN_DOORMAN_2);
		assertTrue(view.addTarget(thug));
		assertFalse(view.addTarget(thug));
		Collection<NPC> second = view.eligibleTargets();
		assertNotSame(first, second);
		assertEquals(2, second.size());

		view.setGate(OPEN.withThievingLevel(60));
		assertEquals(Collections.singletonList(bandit), view.eligibleTargets());

		assertTrue(view.removeTarget(bandit));
		assertTrue(view.eligibleTargets().isEmpty());
		assertEquals(1, view.targets().size());
	}

	@Test
	public void inactiveViewHasNoEligibleTargets()
	{
		view.setGate(ActivationGate.CLOSED);

		assertFalse(view.isActive());
		assertTrue(view.eligibleTargets().isEmpty());
	}

	@Test
	public void addTargetIgnoresNpcsThatCannotBeBlackjacked()
	{
		assertFalse(view.addTarget(npc(10, NpcID.FEUD_VILLAGER_1_1)));
		assertFalse(view.addTarget(null));
		assertEquals(1, view.targets().size());
	}

	@Test
	public void targetByIndexFindsTrackedTargets()
	{
		assertSame(bandit, view.targetByIndex(7));
		assertNull(view.targetByIndex(8));
		assertNull(view.targetByIndex(KnockoutTracker.NO_NPC));
	}

	@Test
	public void clearForgetsEverythingButTheDuration()
	{
		view.tracker().setKnockOutTicks(6);
		tick(100);
		view.tracker().onChat(100, ChatMessageType.SPAM, KNOCKOUT, bandit.getIndex());

		view.clear();

		assertFalse(view.isActive());
		assertTrue(view.targets().isEmpty());
		assertEquals(0, view.remainingMillis());
		assertEquals(3600, view.durationMillis());
	}

	private void tick(int tick)
	{
		view.clock().onTick();
		view.tracker().onTick(tick);
	}

	private static NPC npc(int index, int id)
	{
		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(index);
		when(npc.getId()).thenReturn(id);
		return npc;
	}
}
