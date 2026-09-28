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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import static com.oveduumnakal.betterblackjacking.BlackjackTarget.BEARDED_BANDIT;
import static com.oveduumnakal.betterblackjacking.BlackjackTarget.CLEAN_SHAVEN_BANDIT;
import static com.oveduumnakal.betterblackjacking.BlackjackTarget.MENAPHITE_THUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Table-driven cases for the activation gate: area, weapon and each target's level requirement. */
public class ActivationGateTest
{
	private static final int NORTH = 13358;

	private static final int SOUTH = 13357;

	private static final int OUTSIDE = 13359;

	private static final int OAK = ItemID.BLACKJACK_OAK;

	private static final int NONE = ActivationGate.NO_WEAPON;

	private static final Case[] CASES = {
		new Case("below every requirement", NORTH, OAK, 44, true),
		new Case("at the bearded bandit's requirement", NORTH, OAK, 45, true, BEARDED_BANDIT),
		new Case("above the bearded bandit's requirement", NORTH, OAK, 46, true, BEARDED_BANDIT),
		new Case("below the clean-shaven bandit's requirement", NORTH, OAK, 54, true, BEARDED_BANDIT),
		new Case("at the clean-shaven bandit's requirement", NORTH, OAK, 55, true, BEARDED_BANDIT,
				CLEAN_SHAVEN_BANDIT),
		new Case("above the clean-shaven bandit's requirement", NORTH, OAK, 56, true, BEARDED_BANDIT,
				CLEAN_SHAVEN_BANDIT),
		new Case("below the thug's requirement", SOUTH, OAK, 64, true, BEARDED_BANDIT, CLEAN_SHAVEN_BANDIT),
		new Case("at the thug's requirement", SOUTH, OAK, 65, true, BEARDED_BANDIT, CLEAN_SHAVEN_BANDIT,
				MENAPHITE_THUG),
		new Case("above the thug's requirement", SOUTH, OAK, 66, true, BEARDED_BANDIT, CLEAN_SHAVEN_BANDIT,
				MENAPHITE_THUG),
		new Case("at level 99", NORTH, ItemID.ROGUETRADER_BJ_DEFEND_MAPLE, 99, true, BEARDED_BANDIT,
				CLEAN_SHAVEN_BANDIT, MENAPHITE_THUG),
		new Case("at level 1", NORTH, OAK, 1, true),
		new Case("with no blackjack", NORTH, NONE, 99, false),
		new Case("with the makeshift blackjack", NORTH, ItemID.VMQ4_JANUS_SLAP, 99, false),
		new Case("with another weapon", NORTH, ItemID.BRONZE_SWORD, 99, false),
		new Case("outside the town to the north", OUTSIDE, OAK, 99, false),
		new Case("outside the town to the south", 13356, OAK, 99, false),
		new Case("outside the town to the west", 13102, OAK, 99, false),
		new Case("outside the town to the east", 13614, OAK, 99, false),
		new Case("with no region", -1, OAK, 99, false),
		new Case("outside the town with no blackjack", OUTSIDE, NONE, 99, false),
	};

	@Test
	public void everyCaseMatchesTheTable()
	{
		for (Case c : CASES)
		{
			ActivationGate gate = ActivationGate.of(c.regionId, c.weaponItemId, c.level);
			assertEquals(c.name + ": active", c.active, gate.isActive());
			for (BlackjackTarget target : BlackjackTarget.values())
			{
				boolean eligible = c.eligible.contains(target);
				assertEquals(c.name + ": " + target, eligible, gate.isEligible(target));
				for (int npcId : target.npcIds())
					assertEquals(c.name + ": NPC " + npcId, eligible, gate.isEligibleNpc(npcId));
			}
		}
	}

	@Test
	public void tableCoversEveryRequirementEdge()
	{
		List<Integer> levels = new ArrayList<>();
		for (Case c : CASES)
			levels.add(c.level);

		for (BlackjackTarget target : BlackjackTarget.values())
		{
			int requirement = target.thievingLevel();
			assertTrue(target + " below", levels.contains(requirement - 1));
			assertTrue(target + " at", levels.contains(requirement));
			assertTrue(target + " above", levels.contains(requirement + 1));
		}
	}

	@Test
	public void everyBlackjackOpensTheGate()
	{
		int[] blackjacks = {4599, 6408, 6410, 4600, 6412, 6414, 6416, 6418, 6420};
		for (int itemId : blackjacks)
			assertTrue("blackjack " + itemId, ActivationGate.of(NORTH, itemId, 45).isActive());
	}

	@Test
	public void excludedNpcsAreNeverEligible()
	{
		ActivationGate gate = ActivationGate.of(NORTH, OAK, 99);
		assertFalse(gate.isEligibleNpc(3549));
		for (int npcId = 3552; npcId <= 3560; npcId++)
			assertFalse("villager " + npcId, gate.isEligibleNpc(npcId));

		assertFalse(gate.isEligibleNpc(-1));
		assertFalse(gate.isEligible(null));
	}

	@Test
	public void worldPointsGateOnTheirRegion()
	{
		assertTrue(ActivationGate.of(new WorldPoint(3360, 2990, 0), OAK, 45).isActive());
		assertTrue(ActivationGate.of(new WorldPoint(3328, 2880, 0), OAK, 45).isActive());
		assertTrue(ActivationGate.of(new WorldPoint(3391, 3007, 0), OAK, 45).isActive());
		assertFalse(ActivationGate.of(new WorldPoint(3327, 2990, 0), OAK, 45).isActive());
		assertFalse(ActivationGate.of(new WorldPoint(3360, 3008, 0), OAK, 45).isActive());
		assertFalse(ActivationGate.of((WorldPoint) null, OAK, 45).isActive());
	}

	@Test
	public void closedGateIsInactive()
	{
		assertFalse(ActivationGate.CLOSED.isActive());
		assertFalse(ActivationGate.CLOSED.isEligible(BEARDED_BANDIT));
		assertEquals(NONE, ActivationGate.CLOSED.weaponItemId());
	}

	@Test
	public void gateOpensAndClosesAsInputsChange()
	{
		ActivationGate gate = ActivationGate.CLOSED.withThievingLevel(50);
		assertFalse(gate.isActive());

		gate = gate.withLocation(new WorldPoint(3360, 2990, 0));
		assertFalse(gate.isActive());

		gate = gate.withWeaponItemId(OAK);
		assertTrue(gate.isActive());
		assertTrue(gate.isEligible(BEARDED_BANDIT));
		assertFalse(gate.isEligible(CLEAN_SHAVEN_BANDIT));

		gate = gate.withThievingLevel(55);
		assertTrue(gate.isEligible(CLEAN_SHAVEN_BANDIT));

		gate = gate.withThievingLevel(54);
		assertFalse(gate.isEligible(CLEAN_SHAVEN_BANDIT));

		gate = gate.withWeaponItemId(NONE);
		assertFalse(gate.isActive());
		assertFalse(gate.isEligible(BEARDED_BANDIT));

		gate = gate.withWeaponItemId(OAK).withRegionId(OUTSIDE);
		assertFalse(gate.isActive());

		gate = gate.withRegionId(SOUTH);
		assertTrue(gate.isActive());
		assertEquals(SOUTH, gate.regionId());
		assertEquals(OAK, gate.weaponItemId());
		assertEquals(54, gate.thievingLevel());
	}

	@Test
	public void unchangedInputsReturnTheSameGate()
	{
		ActivationGate gate = ActivationGate.of(NORTH, OAK, 60);
		assertSame(gate, gate.withRegionId(NORTH));
		assertSame(gate, gate.withLocation(new WorldPoint(3360, 2990, 0)));
		assertSame(gate, gate.withWeaponItemId(OAK));
		assertSame(gate, gate.withThievingLevel(60));
		assertNotSame(gate, gate.withRegionId(SOUTH));
	}

	@Test
	public void toStringDescribesInputsAndResult()
	{
		assertEquals("ActivationGate{region=13358, weapon=4599, thieving=60, active=true}",
				ActivationGate.of(NORTH, OAK, 60).toString());
	}

	/** One row of the gate table: the inputs, whether the gate is open and which targets are eligible. */
	private static final class Case
	{
		private final String name;

		private final int regionId;

		private final int weaponItemId;

		private final int level;

		private final boolean active;

		private final Set<BlackjackTarget> eligible = EnumSet.noneOf(BlackjackTarget.class);

		Case(String name, int regionId, int weaponItemId, int level, boolean active, BlackjackTarget... eligible)
		{
			this.name = name;
			this.regionId = regionId;
			this.weaponItemId = weaponItemId;
			this.level = level;
			this.active = active;
			this.eligible.addAll(Arrays.asList(eligible));
		}
	}
}
