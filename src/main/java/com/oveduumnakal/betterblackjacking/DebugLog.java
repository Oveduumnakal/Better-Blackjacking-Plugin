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

import java.util.function.Consumer;
import java.util.function.IntSupplier;

import lombok.extern.slf4j.Slf4j;

/**
 * Writes tuning lines to the client log while the {@code debugLogging} setting is on.
 *
 * <p>Every line has the form {@code [BetterBlackjacking] tick=<n> ko+<offset> <message>}, where
 * {@code <offset>} is the number of ticks since the last knock-out, or {@code -} before the first
 * one. Without the tag, a line matches the fixtures in {@code planning/fixtures/}, so a recorded
 * session can be added as a fixture directly.
 *
 * <p>When the setting is off, every method returns after reading the setting: nothing is formatted,
 * the tick is not read and nothing is logged. Callers that build arguments with {@link #describe}
 * can check {@link #isEnabled()} first to skip that work too.
 *
 * <p>The plugin records the knock-out tick with {@link #setKnockoutTick(int)} whether or not
 * logging is on. To match the fixtures, it logs the knock-out chat line before calling it (so the
 * chat line shows the previous offset) and the {@link #knockout} line after (so it shows
 * {@code ko+0}).
 */
@Slf4j
final class DebugLog
{
	/** The tag every line starts with. */
	static final String TAG = "[BetterBlackjacking]";

	/** How the local player is described in a line. */
	static final String PLAYER = "player";

	/** How a missing actor, such as an interaction target of {@code null}, is described in a line. */
	static final String NONE = "none";

	private final BetterBlackjackingConfig config;
	private final IntSupplier tickSupplier;
	private final Consumer<String> sink;

	private boolean knockedOut;
	private int knockoutTick;

	/**
	 * Creates a debug log that writes to the plugin's SLF4J logger at INFO.
	 *
	 * @param config the plugin config, whose {@code debugLogging()} turns logging on
	 * @param tickSupplier the current game tick
	 */
	DebugLog(BetterBlackjackingConfig config, IntSupplier tickSupplier)
	{
		this(config, tickSupplier, DebugLog::writeToLog);
	}

	/**
	 * Creates a debug log that writes each finished line to {@code sink}.
	 *
	 * @param config the plugin config, whose {@code debugLogging()} turns logging on
	 * @param tickSupplier the current game tick
	 * @param sink where finished lines go, tag included
	 */
	DebugLog(BetterBlackjackingConfig config, IntSupplier tickSupplier, Consumer<String> sink)
	{
		this.config = config;
		this.tickSupplier = tickSupplier;
		this.sink = sink;
	}

	/**
	 * Describes an NPC as the fixtures do: {@code Name#index(id=N)}.
	 *
	 * @param name the NPC's name
	 * @param index the NPC's index in the client's NPC array
	 * @param id the NPC's ID
	 * @return the description, e.g. {@code Bandit#28415(id=737)}
	 */
	static String describe(String name, int index, int id)
	{
		return name + "#" + index + "(id=" + id + ")";
	}

	/**
	 * Returns whether debug logging is on.
	 *
	 * @return the current value of the {@code debugLogging} setting
	 */
	boolean isEnabled()
	{
		return config.debugLogging();
	}

	/**
	 * Records the tick of the latest knock-out, so later lines show {@code ko+} the ticks since it.
	 *
	 * @param tick the game tick the knock-out succeeded on
	 */
	void setKnockoutTick(int tick)
	{
		knockedOut = true;
		knockoutTick = tick;
	}

	/**
	 * Forgets the latest knock-out, so later lines show {@code ko+-}.
	 */
	void clearKnockout()
	{
		knockedOut = false;
	}

	/**
	 * Logs a line whose message is {@code String.format(format, args)}. Does nothing, and formats
	 * nothing, when debug logging is off.
	 *
	 * @param format the message's format string
	 * @param args the format arguments
	 */
	void log(String format, Object... args)
	{
		if (!isEnabled())
			return;

		write(String.format(format, args));
	}

	/**
	 * Logs a chat message: {@code chat type=SPAM matched=true msg="..."}.
	 *
	 * @param type the chat message type, written with {@code toString()} (a {@code ChatMessageType}
	 *        writes its constant name)
	 * @param matched whether the message matched a signal the plugin reacts to
	 * @param message the message text, as received
	 */
	void chat(Object type, boolean matched, String message)
	{
		if (!isEnabled())
			return;

		write("chat type=" + type + " matched=" + matched + " msg=\"" + message + "\"");
	}

	/**
	 * Logs an actor's overhead text: {@code overhead Bandit#28415(id=737) text="Zzzzzz"}.
	 *
	 * @param actor the actor, from {@link #describe} or {@link #PLAYER}
	 * @param text the overhead text
	 */
	void overhead(String actor, String text)
	{
		if (!isEnabled())
			return;

		write("overhead " + actor + " text=\"" + text + "\"");
	}

	/**
	 * Logs an actor's animation change: {@code animation player anim=401 pose=808}.
	 *
	 * @param actor the actor, from {@link #describe} or {@link #PLAYER}
	 * @param animation the actor's animation ID, or -1 for none
	 * @param pose the actor's pose animation ID
	 */
	void animation(String actor, int animation, int pose)
	{
		if (!isEnabled())
			return;

		write("animation " + actor + " anim=" + animation + " pose=" + pose);
	}

	/**
	 * Logs a hitsplat: {@code hitsplat player amount=4}.
	 *
	 * @param actor the actor hit, from {@link #describe} or {@link #PLAYER}
	 * @param amount the hitsplat's amount
	 */
	void hitsplat(String actor, int amount)
	{
		if (!isEnabled())
			return;

		write("hitsplat " + actor + " amount=" + amount);
	}

	/**
	 * Logs an interaction change: {@code interacting player -> Bandit#28415(id=737)}.
	 *
	 * @param source the actor whose target changed, from {@link #describe} or {@link #PLAYER}
	 * @param target the new target, from {@link #describe}, {@link #PLAYER} or {@link #NONE}
	 */
	void interacting(String source, String target)
	{
		if (!isEnabled())
			return;

		write("interacting " + source + " -> " + target);
	}

	/**
	 * Logs a successful knock-out:
	 * {@code KNOCKOUT target=... (interacting=...) anim=838 pose=808 duration=4 wakeTick=296}.
	 *
	 * @param target the knocked-out NPC, from {@link #describe} or {@link #NONE}
	 * @param interacting what the player was interacting with, from {@link #describe} or
	 *        {@link #NONE}
	 * @param animation the target's animation ID, or -1 if there is no target
	 * @param pose the target's pose animation ID, or -1 if there is no target
	 * @param duration the knock-out duration in ticks
	 * @param wakeTick the tick the target is expected to wake on
	 */
	void knockout(String target, String interacting, int animation, int pose, int duration, int wakeTick)
	{
		if (!isEnabled())
			return;

		write("KNOCKOUT target=" + target + " (interacting=" + interacting + ") anim=" + animation
				+ " pose=" + pose + " duration=" + duration + " wakeTick=" + wakeTick);
	}

	/**
	 * Logs an NPC despawning: {@code despawn Bandit#28415(id=737)}.
	 *
	 * @param actor the NPC, from {@link #describe}
	 */
	void despawn(String actor)
	{
		if (!isEnabled())
			return;

		write("despawn " + actor);
	}

	private void write(String message)
	{
		final int tick = tickSupplier.getAsInt();
		final String offset = knockedOut ? String.valueOf(tick - knockoutTick) : "-";
		sink.accept(TAG + " tick=" + tick + " ko+" + offset + " " + message);
	}

	private static void writeToLog(String line)
	{
		log.info("{}", line);
	}
}
