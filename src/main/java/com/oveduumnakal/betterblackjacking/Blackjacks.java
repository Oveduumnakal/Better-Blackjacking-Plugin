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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.runelite.api.gameval.ItemID;

/**
 * The blackjacks that turn the plugin on when wielded.
 *
 * <p>These are the nine equippable blackjacks: oak, willow and maple, each plain, offensive
 * ({@code (o)}) and defensive ({@code (d)}). The makeshift blackjack ({@link ItemID#VMQ4_JANUS_SLAP})
 * is a quest item that can't be equipped, so it isn't one.
 */
final class Blackjacks
{
	private static final Set<Integer> ITEM_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ItemID.BLACKJACK_OAK,
			ItemID.ROGUETRADER_BJ_ASSAULT_OAK,
			ItemID.ROGUETRADER_BJ_DEFEND_OAK,
			ItemID.BLACKJACK_WILLOW,
			ItemID.ROGUETRADER_BJ_ASSAULT_WILLOW,
			ItemID.ROGUETRADER_BJ_DEFEND_WILLOW,
			ItemID.ROGUETRADER_BJ_MAPLE,
			ItemID.ROGUETRADER_BJ_ASSAULT_MAPLE,
			ItemID.ROGUETRADER_BJ_DEFEND_MAPLE)));

	private Blackjacks()
	{
	}

	/**
	 * Whether an item is an equippable blackjack.
	 *
	 * @param itemId the item ID, e.g. of the weapon slot; {@code -1} for an empty slot
	 * @return {@code true} if it is one of the nine equippable blackjacks
	 */
	static boolean isBlackjack(int itemId)
	{
		return ITEM_IDS.contains(itemId);
	}
}
