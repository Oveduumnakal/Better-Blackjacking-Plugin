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

import java.awt.Color;
import java.util.Collection;

import net.runelite.api.NPC;

/**
 * The read-only view of the plugin's state that the overlays draw from.
 *
 * <p>The plugin implements it by combining the knock-out tracker, the activation and level gates,
 * the sub-tick clock and the config, so overlays depend only on this interface and can be written
 * and tested without the plugin. At most one target is knocked out at a time; the timing methods
 * ({@link #pickpocketsLeft()}, {@link #remainingMillis()}) describe that target.
 *
 * <p>Every method is called on the client thread, usually once per rendered frame, so each one must
 * be cheap, must not block, and must never return {@code null}.
 */
public interface TargetStateView
{
	/**
	 * Whether the activation gate is open: the player is in Pollnivneach with a blackjack equipped.
	 * Overlays draw nothing while this is {@code false}.
	 *
	 * @return whether the plugin is active
	 */
	boolean isActive();

	/**
	 * The NPCs to draw: every blackjack target in the scene that the player has the Thieving level
	 * for. Empty, never {@code null}, while the plugin is inactive. Callers must not modify the
	 * returned collection.
	 *
	 * @return the eligible targets currently in the scene
	 */
	Collection<NPC> eligibleTargets();

	/**
	 * The state of {@code npc}, which decides its colour. A target that isn't tracked (not the current
	 * knock-out target and not in combat with the player) is {@link TargetState#KNOCK_OUT}.
	 *
	 * @param npc a target, usually one from {@link #eligibleTargets()}
	 * @return the target's state, never {@code null}
	 */
	TargetState stateOf(NPC npc);

	/**
	 * How many more guaranteed pickpockets will land on the knocked-out target before it wakes, if the
	 * player clicks as soon as possible. This is between 0 and 2 while a target is knocked out, and 0
	 * when none is.
	 *
	 * @return the pickpockets left, from 0 to 2
	 */
	int pickpocketsLeft();

	/**
	 * Milliseconds until the knocked-out target wakes, interpolated between game ticks so a countdown
	 * drawn every frame moves smoothly. It is never negative, and 0 when no target is knocked out.
	 *
	 * @return the time left until the target wakes, in milliseconds
	 */
	long remainingMillis();

	/**
	 * The full length of a knock-out in milliseconds (the configured knock-out duration in ticks times
	 * 600 ms). A pie timer's filled fraction is {@code remainingMillis() / (double) durationMillis()}.
	 *
	 * @return the knock-out duration in milliseconds, always positive
	 */
	long durationMillis();

	/**
	 * The colour the player has configured for {@code state}.
	 *
	 * @param state a target state
	 * @return that state's colour, never {@code null}
	 */
	Color colorOf(TargetState state);
}
