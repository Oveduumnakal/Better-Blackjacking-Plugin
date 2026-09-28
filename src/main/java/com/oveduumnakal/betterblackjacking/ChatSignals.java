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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies the chat messages and overhead texts that drive the knock-out tracker (PLAN §3.4).
 *
 * <p>Only bandit messages have been captured so far, so every pattern that names the NPC captures
 * the noun as group 1 instead of hard-coding "bandit". Colour tags such as {@code <col=ff0000>} are
 * stripped before matching, and a message must match in full.
 */
public final class ChatSignals
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");

	private ChatSignals()
	{
	}

	/** A chat-box message from the game that the tracker reacts to. */
	public enum Message
	{
		/** The knock-out landed: the target is unconscious from this tick. */
		KNOCKOUT_SUCCESS("You smack the (.+) over the head and render them unconscious\\."),
		/** The knock-out failed, and the target is about to attack the player. */
		KNOCKOUT_FAILED("Your blow only glances off the (.+)'s head\\."),
		/** The player tried to knock out a target while in combat. */
		KNOCKOUT_IN_COMBAT("You can't knock-out the (.+) during combat\\."),
		/** The player tried to pickpocket while in combat. */
		PICKPOCKET_IN_COMBAT("You can't pickpocket during combat\\."),
		/** The player tried to knock out a target that is already unconscious. */
		ALREADY_UNCONSCIOUS("I can't do that\\. They're unconscious\\."),
		/** The game processed a pickpocket, on tick {@code A}. */
		PICKPOCKET_PROCESSED("You attempt to pick the (.+)'s pocket\\."),
		/** A pickpocket landed, on tick {@code A + 1}. */
		PICKPOCKET_LANDED("You pick the (.+)'s pocket\\.");

		private final Pattern pattern;

		Message(String regex)
		{
			this.pattern = Pattern.compile(regex);
		}

		/**
		 * The pattern a message must match in full. Group 1, where present, is the NPC's noun.
		 *
		 * @return the compiled pattern
		 */
		public Pattern pattern()
		{
			return pattern;
		}
	}

	/** An overhead text said by a blackjack target that the tracker reacts to. */
	public enum Overhead
	{
		/** Said on the knock-out tick; it confirms which NPC was knocked out. */
		KNOCKED_OUT("Zzzzzz"),
		/** Said on the tick the target wakes up. */
		WAKING_UP("Arghh my head."),
		/** Said after a failed knock-out, as the target starts attacking the player. */
		ANGRY("I'll kill you for that!");

		private final String text;

		Overhead(String text)
		{
			this.text = text;
		}

		/**
		 * The exact overhead text.
		 *
		 * @return the text, without tags
		 */
		public String text()
		{
			return text;
		}
	}

	/**
	 * Finds which tracked message a chat message is.
	 *
	 * @param message the raw chat message, tags allowed
	 * @return the matching message, or {@code null} if it isn't one the tracker uses
	 */
	public static Message classifyMessage(String message)
	{
		if (message == null)
			return null;

		String plain = clean(message);
		for (Message candidate : Message.values())
		{
			if (candidate.pattern.matcher(plain).matches())
				return candidate;
		}

		return null;
	}

	/**
	 * Finds which tracked overhead text an NPC said.
	 *
	 * @param text the raw overhead text, tags allowed
	 * @return the matching overhead, or {@code null} if it isn't one the tracker uses
	 */
	public static Overhead classifyOverhead(String text)
	{
		if (text == null)
			return null;

		String plain = clean(text);
		for (Overhead candidate : Overhead.values())
		{
			if (candidate.text.equals(plain))
				return candidate;
		}

		return null;
	}

	/**
	 * The NPC noun a tracked message names, e.g. {@code bandit}, for logging and verification.
	 *
	 * @param message the raw chat message, tags allowed
	 * @return the captured noun, or {@code null} if the message isn't tracked or names no NPC
	 */
	public static String npcNoun(String message)
	{
		Message kind = classifyMessage(message);
		if (kind == null)
			return null;

		Matcher matcher = kind.pattern.matcher(clean(message));
		if (!matcher.matches() || matcher.groupCount() < 1)
			return null;

		return matcher.group(1);
	}

	private static String clean(String text)
	{
		return TAG.matcher(text)
				.replaceAll("")
				.trim();
	}
}
