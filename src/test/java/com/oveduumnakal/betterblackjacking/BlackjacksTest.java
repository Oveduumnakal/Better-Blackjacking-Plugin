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

import net.runelite.api.gameval.ItemID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the blackjack table: all nine equippable blackjacks, and nothing else. */
public class BlackjacksTest
{
	private static final int[] BLACKJACK_IDS = {4599, 6408, 6410, 4600, 6412, 6414, 6416, 6418, 6420};

	@Test
	public void everyEquippableBlackjackCounts()
	{
		for (int itemId : BLACKJACK_IDS)
			assertTrue("blackjack " + itemId, Blackjacks.isBlackjack(itemId));
	}

	@Test
	public void theItemIdConstantsMatchTheWikiIds()
	{
		int[] constants = {
			ItemID.BLACKJACK_OAK,
			ItemID.ROGUETRADER_BJ_ASSAULT_OAK,
			ItemID.ROGUETRADER_BJ_DEFEND_OAK,
			ItemID.BLACKJACK_WILLOW,
			ItemID.ROGUETRADER_BJ_ASSAULT_WILLOW,
			ItemID.ROGUETRADER_BJ_DEFEND_WILLOW,
			ItemID.ROGUETRADER_BJ_MAPLE,
			ItemID.ROGUETRADER_BJ_ASSAULT_MAPLE,
			ItemID.ROGUETRADER_BJ_DEFEND_MAPLE,
		};
		assertArrayEquals(BLACKJACK_IDS, constants);
	}

	@Test
	public void makeshiftBlackjackIsExcluded()
	{
		assertFalse(Blackjacks.isBlackjack(30944));
		assertFalse(Blackjacks.isBlackjack(ItemID.VMQ4_JANUS_SLAP));
	}

	@Test
	public void emptySlotAndOtherItemsAreNotBlackjacks()
	{
		assertFalse(Blackjacks.isBlackjack(-1));
		assertFalse(Blackjacks.isBlackjack(0));
		assertFalse(Blackjacks.isBlackjack(ItemID.BRONZE_SWORD));
		assertFalse(Blackjacks.isBlackjack(4598));
		assertFalse(Blackjacks.isBlackjack(4601));
		assertFalse(Blackjacks.isBlackjack(6409));
		assertFalse(Blackjacks.isBlackjack(6421));
	}
}
