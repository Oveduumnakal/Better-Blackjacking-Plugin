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
import static org.junit.Assert.assertNull;

/** Covers the chat and overhead patterns from PLAN §3.4, including NPC nouns other than "bandit". */
public class ChatSignalsTest
{
	@Test
	public void classifiesEveryBanditMessage()
	{
		assertEquals(ChatSignals.Message.KNOCKOUT_SUCCESS,
				ChatSignals.classifyMessage("You smack the bandit over the head and render them unconscious."));
		assertEquals(ChatSignals.Message.KNOCKOUT_FAILED,
				ChatSignals.classifyMessage("Your blow only glances off the bandit's head."));
		assertEquals(ChatSignals.Message.KNOCKOUT_IN_COMBAT,
				ChatSignals.classifyMessage("You can't knock-out the bandit during combat."));
		assertEquals(ChatSignals.Message.PICKPOCKET_IN_COMBAT,
				ChatSignals.classifyMessage("You can't pickpocket during combat."));
		assertEquals(ChatSignals.Message.ALREADY_UNCONSCIOUS,
				ChatSignals.classifyMessage("I can't do that. They're unconscious."));
		assertEquals(ChatSignals.Message.PICKPOCKET_PROCESSED,
				ChatSignals.classifyMessage("You attempt to pick the bandit's pocket."));
		assertEquals(ChatSignals.Message.PICKPOCKET_LANDED,
				ChatSignals.classifyMessage("You pick the bandit's pocket."));
	}

	@Test
	public void capturesOtherNpcNouns()
	{
		String thug = "You smack the Menaphite Thug over the head and render them unconscious.";
		assertEquals(ChatSignals.Message.KNOCKOUT_SUCCESS, ChatSignals.classifyMessage(thug));
		assertEquals("Menaphite Thug", ChatSignals.npcNoun(thug));
		assertEquals("bandit", ChatSignals.npcNoun("Your blow only glances off the bandit's head."));
		assertEquals("menaphite thug", ChatSignals.npcNoun("You attempt to pick the menaphite thug's pocket."));
	}

	@Test
	public void theNounIsMissingWhereTheMessageNamesNoNpc()
	{
		assertNull(ChatSignals.npcNoun("You can't pickpocket during combat."));
		assertNull(ChatSignals.npcNoun("Welcome to Old School RuneScape."));
		assertNull(ChatSignals.npcNoun(null));
	}

	@Test
	public void stripsTagsBeforeMatching()
	{
		assertEquals(ChatSignals.Message.KNOCKOUT_FAILED,
				ChatSignals.classifyMessage("<col=ef1020>Your blow only glances off the bandit's head.</col>"));
		assertEquals(ChatSignals.Overhead.WAKING_UP, ChatSignals.classifyOverhead("<col=ffff00>Arghh my head.</col>"));
	}

	@Test
	public void ignoresUnrelatedAndPartialMessages()
	{
		assertNull(ChatSignals.classifyMessage(null));
		assertNull(ChatSignals.classifyMessage("You fail to pick the bandit's pocket."));
		assertNull(ChatSignals.classifyMessage("Someone said: You pick the bandit's pocket."));
		assertNull(ChatSignals.classifyMessage("<colNORMAL>[<col=c4941a>OSRS TCG</col>] Credited 100 credits!"));
	}

	@Test
	public void classifiesTheOverheads()
	{
		assertEquals(ChatSignals.Overhead.KNOCKED_OUT, ChatSignals.classifyOverhead("Zzzzzz"));
		assertEquals(ChatSignals.Overhead.WAKING_UP, ChatSignals.classifyOverhead("Arghh my head."));
		assertEquals(ChatSignals.Overhead.ANGRY, ChatSignals.classifyOverhead("I'll kill you for that!"));
		assertEquals("Zzzzzz", ChatSignals.Overhead.KNOCKED_OUT.text());
		assertNull(ChatSignals.classifyOverhead("Zzz"));
		assertNull(ChatSignals.classifyOverhead(null));
	}

	@Test
	public void exposesThePatterns()
	{
		assertEquals("You can't pickpocket during combat\\.",
				ChatSignals.Message.PICKPOCKET_IN_COMBAT.pattern().pattern());
	}
}
