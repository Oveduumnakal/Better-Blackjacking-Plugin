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

import java.util.EnumSet;

import org.junit.Test;

import net.runelite.api.gameval.NpcID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/** Covers the blackjack target table: every NPC ID, every level requirement and the exclusions. */
public class BlackjackTargetTest
{
	@Test
	public void beardedBanditsAreIds736And737AtThieving45()
	{
		assertArrayEquals(new int[]{736, 737}, BlackjackTarget.BEARDED_BANDIT.npcIds());
		assertEquals(45, BlackjackTarget.BEARDED_BANDIT.thievingLevel());
		assertSame(BlackjackTarget.BEARDED_BANDIT, BlackjackTarget.forNpcId(736));
		assertSame(BlackjackTarget.BEARDED_BANDIT, BlackjackTarget.forNpcId(737));
	}

	@Test
	public void cleanShavenBanditsAreIds734And735AtThieving55()
	{
		assertArrayEquals(new int[]{734, 735}, BlackjackTarget.CLEAN_SHAVEN_BANDIT.npcIds());
		assertEquals(55, BlackjackTarget.CLEAN_SHAVEN_BANDIT.thievingLevel());
		assertSame(BlackjackTarget.CLEAN_SHAVEN_BANDIT, BlackjackTarget.forNpcId(734));
		assertSame(BlackjackTarget.CLEAN_SHAVEN_BANDIT, BlackjackTarget.forNpcId(735));
	}

	@Test
	public void menaphiteThugIsId3550AtThieving65()
	{
		assertArrayEquals(new int[]{3550}, BlackjackTarget.MENAPHITE_THUG.npcIds());
		assertEquals(65, BlackjackTarget.MENAPHITE_THUG.thievingLevel());
		assertSame(BlackjackTarget.MENAPHITE_THUG, BlackjackTarget.forNpcId(3550));
	}

	@Test
	public void thereAreExactlyThreeTargets()
	{
		assertEquals(EnumSet.of(BlackjackTarget.BEARDED_BANDIT, BlackjackTarget.CLEAN_SHAVEN_BANDIT,
				BlackjackTarget.MENAPHITE_THUG), EnumSet.allOf(BlackjackTarget.class));
	}

	@Test
	public void preQuestThugIsExcluded()
	{
		assertNull(BlackjackTarget.forNpcId(NpcID.FEUD_EGYPTIAN_DOORMAN_1));
		assertNull(BlackjackTarget.forNpcId(3549));
	}

	@Test
	public void villagersAreExcluded()
	{
		for (int npcId = 3552; npcId <= 3560; npcId++)
			assertNull("villager " + npcId, BlackjackTarget.forNpcId(npcId));

		assertNull(BlackjackTarget.forNpcId(NpcID.FEUD_VILLAGER_1_1));
		assertNull(BlackjackTarget.forNpcId(NpcID.FEUD_VILLAGER_3_3));
	}

	@Test
	public void neighbouringAndInvalidIdsAreNotTargets()
	{
		assertNull(BlackjackTarget.forNpcId(733));
		assertNull(BlackjackTarget.forNpcId(738));
		assertNull(BlackjackTarget.forNpcId(3551));
		assertNull(BlackjackTarget.forNpcId(-1));
		assertNull(BlackjackTarget.forNpcId(0));
	}

	@Test
	public void npcIdsAreACopy()
	{
		int[] ids = BlackjackTarget.MENAPHITE_THUG.npcIds();
		ids[0] = -1;
		assertArrayEquals(new int[]{3550}, BlackjackTarget.MENAPHITE_THUG.npcIds());
	}
}
