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

import net.runelite.api.gameval.AnimationID;

/**
 * The get-up animation played on the knocked-out target when it wakes, in place of the game's snap
 * from lying down to standing ({@link AnimationID#HUMAN_READY}).
 *
 * <p>Each option carries the animation IDs to play in order; a two-animation option plays the second
 * once the first has finished. The animation is cosmetic and only shows on this player's screen. The
 * lengths in each option's Javadoc were measured in the cache viewer.
 */
public enum WakeAnimation
{
	/** No replacement: the game's own snap to standing. */
	OFF("Off"),
	/** Situps get up, 0.5 s. */
	SITUPS_GET_UP("Situps get up", AnimationID.SITUPS_GETUP),
	/** A sit-up followed by the situps get-up, 1.1 s. */
	SIT_UP_THEN_GET_UP("Sit-up, then get up", AnimationID.SITUPS, AnimationID.SITUPS_GETUP),
	/** Sit-up short, 1.9 s. */
	SIT_UP_SHORT("Sit-up short", AnimationID.SITUP_SHORT),
	/** Cyrisus's sit up, 1.2 s: unconscious to sitting, then a snap to standing. */
	CYRISUS_SIT_UP("Cyrisus sit up", AnimationID.DREAM_CYRISUS_SIT_UP_TRANSITION),
	/** Cyrisus's stand up, 2.0 s: crouching to standing. */
	CYRISUS_STAND_UP("Cyrisus stand up", AnimationID.DREAM_CYRISUS_STAND_UP_TRANSITION),
	/** Cyrisus's sit up followed by his stand up, 3.2 s. */
	CYRISUS_SIT_UP_THEN_STAND("Cyrisus sit up, then stand", AnimationID.DREAM_CYRISUS_SIT_UP_TRANSITION,
			AnimationID.DREAM_CYRISUS_STAND_UP_TRANSITION),
	/** Max's get up, 1.6 s. */
	MAX_GET_UP("Max get up", AnimationID.MAX_GET_UP);

	private final String label;

	private final int[] animationIds;

	WakeAnimation(String label, int... animationIds)
	{
		this.label = label;
		this.animationIds = animationIds;
	}

	/**
	 * Returns the animation IDs to play, in order. The array is a copy, so callers may keep or change it.
	 *
	 * @return the animation IDs in play order; empty for {@link #OFF}
	 */
	public int[] animationIds()
	{
		return animationIds.clone();
	}

	/**
	 * Returns the display name shown in the config panel.
	 *
	 * @return the display name
	 */
	@Override
	public String toString()
	{
		return label;
	}
}
