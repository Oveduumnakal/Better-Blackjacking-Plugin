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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.runelite.api.ChatMessageType;

/**
 * Parses the debug-log fixtures in {@code src/test/resources/fixtures} and replays them into a
 * {@link KnockoutTracker}, one tick at a time.
 *
 * <p>Each fixture line is {@code tick=<n> ko+<ticks since knockout> <event>}. Chat, overhead,
 * animation, player-hitsplat, NPC-interacting and despawn lines are replayed. The player's own
 * {@code interacting player -> X} lines are tracked to supply the interacting NPC for chat messages.
 * {@code KNOCKOUT} lines are the prototype's own record of a knock-out and are only reported to the
 * listener, never replayed. Everything else (graphics, poses, the prototype's countdown notes) is
 * parsed as {@link Kind#OTHER}.
 */
final class FixtureReplay
{
	/** The actor index the fixtures use for the local player. */
	static final int PLAYER = -2;

	private static final Pattern PREFIX = Pattern.compile("^tick=(\\d+) ko\\+\\S+ (.*)$");
	private static final Pattern CHAT = Pattern.compile("^chat type=(\\w+) matched=\\w+ msg=\"(.*)\"$");
	private static final Pattern OVERHEAD = Pattern.compile("^overhead (\\S+) text=\"(.*)\"$");
	private static final Pattern ANIMATION = Pattern.compile("^animation (\\S+) anim=(-?\\d+) pose=-?\\d+$");
	private static final Pattern HITSPLAT = Pattern.compile("^hitsplat (\\S+) amount=\\d+$");
	private static final Pattern INTERACTING = Pattern.compile("^interacting (\\S+) -> (\\S+)$");
	private static final Pattern DESPAWN = Pattern.compile("^despawn (\\S+)$");
	private static final Pattern KNOCKOUT = Pattern.compile("^KNOCKOUT target=(\\S+) .*$");
	private static final Pattern NPC = Pattern.compile("^[^#]+#(\\d+)\\(id=\\d+\\)$");

	/** What a fixture line records. */
	enum Kind
	{
		/** A chat message. */
		CHAT,
		/** An actor's overhead text. */
		OVERHEAD,
		/** An actor's animation change. */
		ANIMATION,
		/** A hitsplat on an actor. */
		HITSPLAT,
		/** An actor's interacting target changed. */
		INTERACTING,
		/** An NPC despawned. */
		DESPAWN,
		/** The prototype recorded a knock-out. */
		KNOCKOUT,
		/** Anything the tracker doesn't use. */
		OTHER
	}

	/** One parsed fixture line. */
	static final class Line
	{
		final int tick;
		final Kind kind;
		final String raw;
		int actor = KnockoutTracker.NO_NPC;
		int target = KnockoutTracker.NO_NPC;
		ChatMessageType chatType;
		String text;
		int animationId;

		Line(int tick, Kind kind, String raw)
		{
			this.tick = tick;
			this.kind = kind;
			this.raw = raw;
		}

		@Override
		public String toString()
		{
			return raw;
		}
	}

	/** Receives callbacks while a fixture replays, so tests can assert on the tracker as it goes. */
	interface Listener
	{
		/**
		 * Called after {@link KnockoutTracker#onTick(int)}, before the tick's lines.
		 *
		 * @param tick the tick
		 */
		default void afterTick(int tick)
		{
		}

		/**
		 * Called after a line has been replayed (or, for {@code KNOCKOUT} lines, read).
		 *
		 * @param line the line
		 */
		default void afterLine(Line line)
		{
		}

		/**
		 * Called after the tick's last line.
		 *
		 * @param tick the tick
		 */
		default void endOfTick(int tick)
		{
		}
	}

	private final List<Line> lines;
	private int playerTarget = KnockoutTracker.NO_NPC;

	private FixtureReplay(List<Line> lines)
	{
		this.lines = lines;
	}

	/**
	 * Loads a fixture from the test resources.
	 *
	 * @param name the file name in {@code fixtures/}
	 * @return the parsed fixture
	 * @throws IOException if it can't be read
	 */
	static FixtureReplay load(String name) throws IOException
	{
		List<Line> lines = new ArrayList<>();
		try (InputStream in = FixtureReplay.class.getResourceAsStream("/fixtures/" + name))
		{
			if (in == null)
				throw new IOException("Missing fixture " + name);

			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String raw;
			while ((raw = reader.readLine()) != null)
			{
				if (!raw.trim().isEmpty())
					lines.add(parse(raw.trim()));
			}
		}

		return new FixtureReplay(lines);
	}

	/**
	 * The parsed lines, in file order.
	 *
	 * @return the lines
	 */
	List<Line> lines()
	{
		return lines;
	}

	/**
	 * The NPC the player is interacting with, as of the lines replayed so far.
	 *
	 * @return its index, or {@link KnockoutTracker#NO_NPC}
	 */
	int playerTarget()
	{
		return playerTarget;
	}

	/**
	 * Replays every tick from the first line's tick to the last line's, in order.
	 *
	 * @param tracker the tracker to feed
	 * @param listener the callbacks
	 */
	void replay(KnockoutTracker tracker, Listener listener)
	{
		playerTarget = KnockoutTracker.NO_NPC;
		int index = 0;
		int firstTick = lines.get(0).tick;
		int lastTick = lines.get(lines.size() - 1).tick;
		for (int tick = firstTick; tick <= lastTick; tick++)
		{
			tracker.onTick(tick);
			listener.afterTick(tick);
			while (index < lines.size() && lines.get(index).tick == tick)
			{
				Line line = lines.get(index++);
				apply(tracker, line);
				listener.afterLine(line);
			}

			listener.endOfTick(tick);
		}

		if (index != lines.size())
			throw new IllegalStateException("Fixture ticks are out of order at " + lines.get(index));
	}

	private void apply(KnockoutTracker tracker, Line line)
	{
		switch (line.kind)
		{
			case CHAT:
				tracker.onChat(line.tick, line.chatType, line.text, playerTarget);
				break;
			case OVERHEAD:
				if (line.actor >= 0)
					tracker.onOverhead(line.tick, line.actor, line.text);

				break;
			case ANIMATION:
				if (line.actor >= 0)
					tracker.onNpcAnimation(line.tick, line.actor, line.animationId);

				break;
			case HITSPLAT:
				if (line.actor == PLAYER)
					tracker.onPlayerHitsplat(line.tick);

				break;
			case INTERACTING:
				if (line.actor == PLAYER)
					playerTarget = line.target;
				else if (line.actor >= 0)
					tracker.onNpcInteracting(line.tick, line.actor, line.target == PLAYER);

				break;
			case DESPAWN:
				tracker.onDespawn(line.actor);
				break;
			default:
				break;
		}
	}

	private static Line parse(String raw)
	{
		Matcher prefix = PREFIX.matcher(raw);
		if (!prefix.matches())
			throw new IllegalArgumentException("Not a fixture line: " + raw);

		int tick = Integer.parseInt(prefix.group(1));
		String event = prefix.group(2);

		Matcher m = CHAT.matcher(event);
		if (m.matches())
		{
			Line line = new Line(tick, Kind.CHAT, raw);
			line.chatType = ChatMessageType.valueOf(m.group(1));
			line.text = m.group(2);
			return line;
		}

		m = OVERHEAD.matcher(event);
		if (m.matches())
		{
			Line line = new Line(tick, Kind.OVERHEAD, raw);
			line.actor = actor(m.group(1));
			line.text = m.group(2);
			return line;
		}

		m = ANIMATION.matcher(event);
		if (m.matches())
		{
			Line line = new Line(tick, Kind.ANIMATION, raw);
			line.actor = actor(m.group(1));
			line.animationId = Integer.parseInt(m.group(2));
			return line;
		}

		m = HITSPLAT.matcher(event);
		if (m.matches())
		{
			Line line = new Line(tick, Kind.HITSPLAT, raw);
			line.actor = actor(m.group(1));
			return line;
		}

		m = INTERACTING.matcher(event);
		if (m.matches())
		{
			Line line = new Line(tick, Kind.INTERACTING, raw);
			line.actor = actor(m.group(1));
			line.target = actor(m.group(2));
			return line;
		}

		m = DESPAWN.matcher(event);
		if (m.matches())
		{
			Line line = new Line(tick, Kind.DESPAWN, raw);
			line.actor = actor(m.group(1));
			return line;
		}

		m = KNOCKOUT.matcher(event);
		if (m.matches())
		{
			Line line = new Line(tick, Kind.KNOCKOUT, raw);
			line.target = actor(m.group(1));
			return line;
		}

		return new Line(tick, Kind.OTHER, raw);
	}

	private static int actor(String token)
	{
		if ("player".equals(token))
			return PLAYER;

		if ("none".equals(token))
			return KnockoutTracker.NO_NPC;

		Matcher npc = NPC.matcher(token);
		if (!npc.matches())
			throw new IllegalArgumentException("Unknown actor " + token);

		return Integer.parseInt(npc.group(1));
	}
}
