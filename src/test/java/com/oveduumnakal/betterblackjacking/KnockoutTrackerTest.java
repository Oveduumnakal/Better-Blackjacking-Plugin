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

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.ChatMessageType;
import net.runelite.api.gameval.AnimationID;

import static org.junit.Assert.assertEquals;

/** Covers each transition in PLAN §4.2, driven by plain values. */
public class KnockoutTrackerTest
{
	private static final int T = 100;
	private static final int BANDIT = 28415;
	private static final int OTHER = 28420;
	private static final String KNOCK_OUT = "You smack the bandit over the head and render them unconscious.";
	private static final String GLANCE = "Your blow only glances off the bandit's head.";
	private static final String PICKPOCKET = "You attempt to pick the bandit's pocket.";
	private static final int NONE = KnockoutTracker.NO_NPC;

	private KnockoutTracker tracker;

	@Before
	public void setUp()
	{
		tracker = new KnockoutTracker();
	}

	@Test
	public void untrackedNpcsAreKnockOut()
	{
		tracker.onTick(T);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		assertEquals(KnockoutTracker.NO_NPC, tracker.knockedOutNpcIndex());
		assertEquals(KnockoutTracker.NO_TICK, tracker.knockOutTick());
		assertEquals(KnockoutTracker.NO_TICK, tracker.wakeTick());
		assertEquals(0, tracker.pickpocketsLeft());
	}

	@Test
	public void aKnockOutMakesTheInteractingNpcSafe()
	{
		knockOut(T, BANDIT);
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(OTHER));
		assertEquals(BANDIT, tracker.knockedOutNpcIndex());
		assertEquals(T, tracker.knockOutTick());
		assertEquals(T + 5, tracker.wakeTick());
		assertEquals(2, tracker.pickpocketsLeft());
		assertEquals(2, tracker.pickpocketsLeft(T));
		assertEquals(1, tracker.pickpocketsLeft(T + 1));
	}

	@Test
	public void onlyGameMessagesCount()
	{
		tracker.onChat(T, ChatMessageType.PUBLICCHAT, KNOCK_OUT, BANDIT);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		tracker.onChat(T, ChatMessageType.GAMEMESSAGE, KNOCK_OUT, BANDIT);
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
	}

	@Test
	public void theSleepingOverheadNamesTheTargetWhenNothingWasInteracting()
	{
		knockOut(T, NONE);
		assertEquals(KnockoutTracker.NO_NPC, tracker.knockedOutNpcIndex());
		tracker.onOverhead(T, BANDIT, "Zzzzzz");
		assertEquals(BANDIT, tracker.knockedOutNpcIndex());
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
	}

	@Test
	public void theSleepingOverheadOverridesTheInteractingNpc()
	{
		knockOut(T, OTHER);
		tracker.onOverhead(T, BANDIT, "Zzzzzz");
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(OTHER));
	}

	@Test
	public void aSleepingOverheadOnALaterTickIsIgnored()
	{
		knockOut(T, BANDIT);
		tracker.onOverhead(T + 1, OTHER, "Zzzzzz");
		assertEquals(BANDIT, tracker.knockedOutNpcIndex());
	}

	@Test
	public void safeTurnsWakingWhenNoMorePickpocketsFit()
	{
		knockOut(T, BANDIT);
		tracker.onTick(T + 2);
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
		tracker.onTick(T + 3);
		assertEquals(TargetState.WAKING, tracker.stateOf(BANDIT));
		assertEquals(0, tracker.pickpocketsLeft());
	}

	@Test
	public void pickpocketsSpendTheBudget()
	{
		knockOut(T, BANDIT);
		tracker.onChat(T + 1, ChatMessageType.SPAM, PICKPOCKET, BANDIT);
		assertEquals(1, tracker.pickpocketsLeft());
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
		tracker.onChat(T + 3, ChatMessageType.SPAM, PICKPOCKET, BANDIT);
		assertEquals(0, tracker.pickpocketsLeft());
		assertEquals(TargetState.WAKING, tracker.stateOf(BANDIT));
	}

	@Test
	public void aLatePickpocketGoesStraightToWaking()
	{
		knockOut(T, BANDIT);
		tracker.onChat(T + 2, ChatMessageType.SPAM, PICKPOCKET, BANDIT);
		assertEquals(TargetState.WAKING, tracker.stateOf(BANDIT));
	}

	@Test
	public void aPickpocketWithoutAKnockOutIsIgnored()
	{
		tracker.onChat(T, ChatMessageType.SPAM, PICKPOCKET, BANDIT);
		assertEquals(0, tracker.pickpocketsLeft());
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void theTargetWakesAtTPlus5()
	{
		knockOut(T, BANDIT);
		tracker.onTick(T + 4);
		assertEquals(TargetState.WAKING, tracker.stateOf(BANDIT));
		tracker.onTick(T + 5);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		assertEquals(KnockoutTracker.NO_NPC, tracker.knockedOutNpcIndex());
		assertEquals(KnockoutTracker.NO_TICK, tracker.wakeTick());
		assertEquals(0, tracker.pickpocketsLeft());
	}

	@Test
	public void theWakingOverheadWinsOverTheTimer()
	{
		knockOut(T, BANDIT);
		tracker.onOverhead(T + 3, BANDIT, "Arghh my head.");
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		assertEquals(KnockoutTracker.NO_NPC, tracker.knockedOutNpcIndex());
	}

	@Test
	public void anotherNpcWakingLeavesTheTargetDown()
	{
		knockOut(T, BANDIT);
		tracker.onOverhead(T + 3, OTHER, "Arghh my head.");
		tracker.onNpcAnimation(T + 3, OTHER, AnimationID.HUMAN_READY);
		assertEquals(TargetState.WAKING, tracker.stateOf(BANDIT));
	}

	@Test
	public void theStandingAnimationEndsTheKnockOut()
	{
		knockOut(T, BANDIT);
		tracker.onNpcAnimation(T + 2, BANDIT, AnimationID.HUMAN_UNCONSCIOUS);
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
		tracker.onNpcAnimation(T + 2, BANDIT, AnimationID.HUMAN_READY);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void theKnockOutDurationIsSettable()
	{
		tracker.setKnockOutTicks(7);
		assertEquals(7, tracker.knockOutTicks());
		knockOut(T, BANDIT);
		assertEquals(T + 7, tracker.wakeTick());
		tracker.onTick(T + 6);
		assertEquals(TargetState.WAKING, tracker.stateOf(BANDIT));
		tracker.onTick(T + 7);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void aChangedDurationAppliesFromTheNextKnockOut()
	{
		knockOut(T, BANDIT);
		tracker.setKnockOutTicks(3);
		assertEquals(T + 5, tracker.wakeTick());
		tracker.setKnockOutTicks(0);
		assertEquals(1, tracker.knockOutTicks());
	}

	@Test
	public void aFailedKnockOutMakesTheInteractingNpcAttack()
	{
		tracker.onChat(T, ChatMessageType.SPAM, GLANCE, BANDIT);
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
	}

	@Test
	public void theAngryOverheadMakesTheNpcAttack()
	{
		tracker.onOverhead(T, BANDIT, "I'll kill you for that!");
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
	}

	@Test
	public void eitherDuringCombatMessageMakesTheInteractingNpcAttack()
	{
		tracker.onChat(T, ChatMessageType.GAMEMESSAGE, "You can't knock-out the bandit during combat.", BANDIT);
		tracker.onChat(T, ChatMessageType.GAMEMESSAGE, "You can't pickpocket during combat.", OTHER);
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
		assertEquals(TargetState.ATTACKING, tracker.stateOf(OTHER));
	}

	@Test
	public void attackSignalsWithoutAnNpcAreIgnored()
	{
		tracker.onChat(T, ChatMessageType.SPAM, GLANCE, NONE);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(NONE));
	}

	@Test
	public void aPlayerHitsplatMakesInteractingNpcsAttack()
	{
		tracker.onNpcInteracting(T, BANDIT, true);
		tracker.onNpcInteracting(T, OTHER, false);
		tracker.onPlayerHitsplat(T + 1);
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(OTHER));
	}

	@Test
	public void aKnockOutEndsAttacking()
	{
		tracker.onChat(T, ChatMessageType.SPAM, GLANCE, BANDIT);
		tracker.onNpcInteracting(T + 1, BANDIT, true);
		knockOut(T + 4, BANDIT);
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
		tracker.onPlayerHitsplat(T + 5);
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
		tracker.onTick(T + 9);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void theKnockedOutTargetCannotBeMarkedAttacking()
	{
		knockOut(T, BANDIT);
		tracker.onChat(T + 1, ChatMessageType.GAMEMESSAGE, "You can't pickpocket during combat.", BANDIT);
		assertEquals(TargetState.SAFE, tracker.stateOf(BANDIT));
	}

	@Test
	public void attackingEndsTwoTicksAfterTheNpcStopsInteracting()
	{
		tracker.onChat(T, ChatMessageType.SPAM, GLANCE, BANDIT);
		tracker.onNpcInteracting(T + 1, BANDIT, true);
		tracker.onTick(T + 10);
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
		tracker.onNpcInteracting(T + 11, BANDIT, false);
		tracker.onTick(T + 12);
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
		tracker.onTick(T + 13);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void attackingEndsIfTheNpcNeverInteracts()
	{
		tracker.onOverhead(T, BANDIT, "I'll kill you for that!");
		tracker.onTick(T + 1);
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
		tracker.onTick(T + 2);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void interactingAgainKeepsItAttacking()
	{
		tracker.onChat(T, ChatMessageType.SPAM, GLANCE, BANDIT);
		tracker.onNpcInteracting(T + 1, BANDIT, true);
		tracker.onNpcInteracting(T + 2, BANDIT, false);
		tracker.onNpcInteracting(T + 3, BANDIT, true);
		tracker.onTick(T + 10);
		assertEquals(TargetState.ATTACKING, tracker.stateOf(BANDIT));
	}

	@Test
	public void aDespawnForgetsTheKnockedOutTarget()
	{
		knockOut(T, BANDIT);
		tracker.onDespawn(BANDIT);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		assertEquals(KnockoutTracker.NO_NPC, tracker.knockedOutNpcIndex());
		assertEquals(0, tracker.pickpocketsLeft());
	}

	@Test
	public void aDespawnForgetsAnAttackingNpc()
	{
		tracker.onNpcInteracting(T, BANDIT, true);
		tracker.onChat(T, ChatMessageType.SPAM, GLANCE, BANDIT);
		knockOut(T, OTHER);
		tracker.onDespawn(BANDIT);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		assertEquals(TargetState.SAFE, tracker.stateOf(OTHER));
		tracker.onPlayerHitsplat(T + 1);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void resetClearsEverything()
	{
		tracker.setKnockOutTicks(6);
		tracker.onNpcInteracting(T, OTHER, true);
		tracker.onChat(T, ChatMessageType.SPAM, GLANCE, OTHER);
		knockOut(T, BANDIT);
		tracker.reset();
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(OTHER));
		assertEquals(KnockoutTracker.NO_NPC, tracker.knockedOutNpcIndex());
		assertEquals(Integer.MIN_VALUE, tracker.currentTick());
		assertEquals(6, tracker.knockOutTicks());
		tracker.onPlayerHitsplat(T + 1);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(OTHER));
	}

	@Test
	public void eventsAdvanceTheTick()
	{
		knockOut(T, BANDIT);
		assertEquals(T, tracker.currentTick());
		tracker.onOverhead(T + 5, OTHER, "Zzzzzz");
		assertEquals(T + 5, tracker.currentTick());
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
	}

	@Test
	public void aNewKnockOutReplacesTheOldTarget()
	{
		knockOut(T, BANDIT);
		knockOut(T + 2, OTHER);
		assertEquals(TargetState.KNOCK_OUT, tracker.stateOf(BANDIT));
		assertEquals(TargetState.SAFE, tracker.stateOf(OTHER));
		assertEquals(T + 7, tracker.wakeTick());
	}

	private void knockOut(int tick, int interactingNpcIndex)
	{
		tracker.onChat(tick, ChatMessageType.SPAM, KNOCK_OUT, interactingNpcIndex);
	}
}
