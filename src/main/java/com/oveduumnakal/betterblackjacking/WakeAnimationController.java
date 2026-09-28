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

import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.NPC;

/**
 * Plays the configured {@link WakeAnimation} on a target that has just woken (PLAN §5.5).
 *
 * <p>The plugin calls {@link #play(NPC)} from its {@code AnimationChanged} handler when the target
 * snaps to standing ({@code HUMAN_READY}) at its wake tick; the chosen get-up animation replaces that
 * snap from its first frame. It is cosmetic and only shows on this player's screen. Setting the
 * animation fires another {@code AnimationChanged}, but the plugin only reacts to {@code HUMAN_READY},
 * so this doesn't recurse.
 */
@Singleton
class WakeAnimationController
{
	private final BetterBlackjackingConfig config;

	/**
	 * Creates a controller that reads the chosen animation from the config each time it plays.
	 *
	 * @param config the plugin config
	 */
	@Inject
	WakeAnimationController(BetterBlackjackingConfig config)
	{
		this.config = config;
	}

	/**
	 * Plays the configured wake-up animation on the NPC from its first frame, or does nothing when
	 * the option is {@link WakeAnimation#OFF}.
	 *
	 * @param npc the target that woke
	 */
	void play(NPC npc)
	{
		WakeAnimation animation = config.wakeAnimation();
		if (npc == null || animation == null || animation.animationId() == WakeAnimation.NO_ANIMATION)
			return;

		npc.setAnimation(animation.animationId());
		npc.setAnimationFrame(0);
	}
}
