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

import java.util.function.LongSupplier;

/**
 * Interpolates between game ticks so a countdown drawn every frame moves smoothly instead of jumping
 * once per 600 ms tick.
 *
 * <p>The plugin calls {@link #onTick()} on every {@code GameTick}, which records the time. The time
 * left until a later tick is then that many whole ticks minus the time already spent in the current
 * one. All methods are meant to be called on the client thread.
 */
public final class SubTickClock
{
	/** The length of one game tick, in milliseconds. */
	public static final long TICK_MILLIS = 600;

	private static final long NANOS_PER_MILLI = 1_000_000L;

	private final LongSupplier nanoTime;

	private long lastTickNanos;

	private boolean ticked;

	/** Creates a clock that reads {@link System#nanoTime()}. */
	public SubTickClock()
	{
		this(System::nanoTime);
	}

	/**
	 * Creates a clock with its own time source, so tests can control time.
	 *
	 * @param nanoTime a monotonic time source in nanoseconds, like {@link System#nanoTime()}
	 */
	public SubTickClock(LongSupplier nanoTime)
	{
		this.nanoTime = nanoTime;
	}

	/** Records that a game tick has just happened. Call it on every {@code GameTick}. */
	public void onTick()
	{
		lastTickNanos = nanoTime.getAsLong();
		ticked = true;
	}

	/** Forgets the last tick, as if none had happened yet, e.g. on logout or hop. */
	public void reset()
	{
		ticked = false;
		lastTickNanos = 0;
	}

	/**
	 * Milliseconds since the last {@link #onTick()}, rounded down. This can exceed a tick's length if
	 * the next tick is late (server lag).
	 *
	 * @return the time since the last tick, never negative, and 0 if no tick has been recorded
	 */
	public long millisSinceTick()
	{
		if (!ticked)
			return 0;

		long elapsed = nanoTime.getAsLong() - lastTickNanos;
		return Math.max(0, elapsed / NANOS_PER_MILLI);
	}

	/**
	 * Milliseconds until {@code wakeTick}: {@code (wakeTick − currentTick) × 600 − millisSinceTick()},
	 * clamped to between 0 and {@code (wakeTick − currentTick) × 600}.
	 *
	 * @param wakeTick    the tick being counted down to
	 * @param currentTick the current game tick, from {@code Client.getTickCount()}
	 * @return the time left in milliseconds, never negative, and 0 once {@code wakeTick} has been reached
	 */
	public long remainingMillis(int wakeTick, int currentTick)
	{
		long wholeTicks = (long) wakeTick - currentTick;
		if (wholeTicks <= 0)
			return 0;

		long full = wholeTicks * TICK_MILLIS;
		return Math.max(0, Math.min(full, full - millisSinceTick()));
	}
}
