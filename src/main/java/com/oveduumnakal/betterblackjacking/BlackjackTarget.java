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

import java.util.HashMap;
import java.util.Map;

import net.runelite.api.gameval.NpcID;

/**
 * The Pollnivneach NPCs that can be blackjacked, with their NPC IDs and the Thieving level needed to
 * pickpocket them.
 *
 * <p>Villagers are deliberately left out: they need only Thieving 30 but give no experience after
 * The Feud. The pre-quest Menaphite Thug ({@link NpcID#FEUD_EGYPTIAN_DOORMAN_1}) can't be blackjacked
 * and is left out too.
 */
public enum BlackjackTarget
{
	/** The bearded bandit in northern Pollnivneach: combat 41, Thieving 45. */
	BEARDED_BANDIT(45, NpcID.FEUD_ARABIAN_GUARD2_1, NpcID.FEUD_ARABIAN_GUARD2_2),
	/** The clean-shaven bandit in northern Pollnivneach: combat 56, Thieving 55. */
	CLEAN_SHAVEN_BANDIT(55, NpcID.FEUD_ARABIAN_GUARD1_1, NpcID.FEUD_ARABIAN_GUARD1_2),
	/** The Menaphite Thug in southern Pollnivneach: combat 55, Thieving 65. */
	MENAPHITE_THUG(65, NpcID.FEUD_EGYPTIAN_DOORMAN_2);

	private static final Map<Integer, BlackjackTarget> BY_NPC_ID = new HashMap<>();

	static
	{
		for (BlackjackTarget target : values())
		{
			for (int npcId : target.npcIds)
				BY_NPC_ID.put(npcId, target);
		}
	}

	private final int thievingLevel;

	private final int[] npcIds;

	BlackjackTarget(int thievingLevel, int... npcIds)
	{
		this.thievingLevel = thievingLevel;
		this.npcIds = npcIds;
	}

	/**
	 * Returns the Thieving level needed to pickpocket, and so to usefully blackjack, this target.
	 *
	 * @return the Thieving level requirement
	 */
	public int thievingLevel()
	{
		return thievingLevel;
	}

	/**
	 * Returns every NPC ID this target appears as. The array is a copy, so callers may keep or change it.
	 *
	 * @return the NPC IDs
	 */
	public int[] npcIds()
	{
		return npcIds.clone();
	}

	/**
	 * Looks up the target an NPC ID belongs to.
	 *
	 * @param npcId the NPC's ID, as from {@code NPC#getId()}
	 * @return the target, or {@code null} if the NPC can't be blackjacked
	 */
	public static BlackjackTarget forNpcId(int npcId)
	{
		return BY_NPC_ID.get(npcId);
	}
}
