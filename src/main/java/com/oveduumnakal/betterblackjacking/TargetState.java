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

/**
 * What the player should do next with a blackjack target, which decides the colour it is drawn in.
 *
 * <p>Targets that aren't being tracked (neither the player's current knock-out target nor in combat
 * with the player) are {@link #KNOCK_OUT}.
 */
public enum TargetState
{
	/** Pickpocket now: the target is knocked out and at least one more pickpocket will land. */
	SAFE,
	/** Stop pickpocketing and get ready: the target is knocked out, but no more pickpockets will land. */
	WAKING,
	/** Knock the target out: it is awake and not in combat with the player. */
	KNOCK_OUT,
	/**
	 * The target is in combat with the player, so it can't be knocked out. The player has to break
	 * combat by any means first, e.g. swap weapon, then knock out.
	 */
	ATTACKING;

	/**
	 * Whether the target is knocked out, which is when the timer and pickpocket pips are drawn.
	 *
	 * @return {@code true} for {@link #SAFE} and {@link #WAKING}
	 */
	public boolean isKnockedOut()
	{
		return this == SAFE || this == WAKING;
	}
}
