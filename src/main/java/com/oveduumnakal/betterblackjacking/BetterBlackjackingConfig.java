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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

/**
 * RuneLite configuration for the Better Blackjacking plugin.
 *
 * <p>Every user-facing setting is a defaulted {@code @ConfigItem} accessor, grouped into
 * {@code @ConfigSection}s in this order: outline, colours, timer, pickpockets, animation, and a
 * closed-by-default advanced section. The {@code KEY_*} constants are the persisted setting keys
 * and {@link #GROUP} names the config group.
 *
 * <p>RuneLite saves a setting's default into the user's profile the first time it is read, so
 * changing a default later never reaches existing users. Changing a default therefore means
 * renaming its key, and the rename gets a changelog note.
 */
@ConfigGroup(BetterBlackjackingConfig.GROUP)
public interface BetterBlackjackingConfig extends Config
{
	/** RuneLite config group name ({@code "betterblackjacking"}). */
	String GROUP = "betterblackjacking";

	/** Persisted config key {@code "outlineWidth"}. */
	String KEY_OUTLINE_WIDTH = "outlineWidth";
	/** Persisted config key {@code "fillOpacity"}. */
	String KEY_FILL_OPACITY = "fillOpacity";
	/** Persisted config key {@code "safeColor"}. */
	String KEY_SAFE_COLOR = "safeColor";
	/** Persisted config key {@code "wakingColor"}. */
	String KEY_WAKING_COLOR = "wakingColor";
	/** Persisted config key {@code "knockOutColor"}. */
	String KEY_KNOCK_OUT_COLOR = "knockOutColor";
	/** Persisted config key {@code "attackingColor"}. */
	String KEY_ATTACKING_COLOR = "attackingColor";
	/** Persisted config key {@code "timerStyle"}. */
	String KEY_TIMER_STYLE = "timerStyle";
	/** Persisted config key {@code "pickpocketPips"}. */
	String KEY_PICKPOCKET_PIPS = "pickpocketPips";
	/** Persisted config key {@code "wakeAnimation"}. */
	String KEY_WAKE_ANIMATION = "wakeAnimation";
	/** Persisted config key {@code "knockOutTicks"}. */
	String KEY_KNOCK_OUT_TICKS = "knockOutTicks";
	/** Persisted config key {@code "debugLogging"}. */
	String KEY_DEBUG_LOGGING = "debugLogging";

	/** The click-box outline's stroke and fill. */
	@ConfigSection(
			name = "Outline",
			description = "How the target's click box is outlined",
			position = 0
	)
	String outlineSection = "outline";

	/** The colour for each target state, shared by the outline, timer and pips. */
	@ConfigSection(
			name = "Colours",
			description = "The colour for each target state",
			position = 1
	)
	String coloursSection = "colours";

	/** The knock-out timer drawn on the target's tile. */
	@ConfigSection(
			name = "Timer",
			description = "The countdown to the next knock-out",
			position = 2
	)
	String timerSection = "timer";

	/** The pips that time the two guaranteed pickpockets. */
	@ConfigSection(
			name = "Pickpockets",
			description = "The pips that time the guaranteed pickpockets",
			position = 3
	)
	String pickpocketsSection = "pickpockets";

	/** The get-up animation played when the target wakes. */
	@ConfigSection(
			name = "Animation",
			description = "The animation the target plays when it wakes up",
			position = 4
	)
	String animationSection = "animation";

	/** Timing overrides and diagnostics most players never need; closed by default. */
	@ConfigSection(
			name = "Advanced",
			description = "Timing overrides and diagnostics",
			position = 5,
			closedByDefault = true
	)
	String advancedSection = "advanced";

	/**
	 * Stroke width of the click-box outline, from 1 to 6 pixels.
	 *
	 * @return the outline stroke width in pixels (default 2)
	 */
	@Range(min = 1, max = 6)
	@ConfigItem(
			keyName = KEY_OUTLINE_WIDTH,
			name = "Outline width",
			description = "Width of the outline around the target's click box",
			section = outlineSection,
			position = 0
	)
	default int outlineWidth()
	{
		return 2;
	}

	/**
	 * Opacity of the click-box fill, from 0 (no fill) to 255 (solid).
	 *
	 * @return the fill alpha, 0 to 255 (default 20)
	 */
	@Range(min = 0, max = 255)
	@ConfigItem(
			keyName = KEY_FILL_OPACITY,
			name = "Fill opacity",
			description = "Opacity of the fill inside the click box, from 0 (none) to 255 (solid)",
			section = outlineSection,
			position = 1
	)
	default int fillOpacity()
	{
		return 20;
	}

	/**
	 * Colour of {@link TargetState#SAFE}: the target is knocked out and another pickpocket will land.
	 *
	 * @return the safe colour (default green)
	 */
	@ConfigItem(
			keyName = KEY_SAFE_COLOR,
			name = "Safe to pickpocket",
			description = "The target is knocked out and another pickpocket will land",
			section = coloursSection,
			position = 0
	)
	default Color safeColor()
	{
		return BetterBlackjackingColors.SAFE;
	}

	/**
	 * Colour of {@link TargetState#WAKING}: the target is knocked out but no more pickpockets will land.
	 *
	 * @return the waking colour (default yellow)
	 */
	@ConfigItem(
			keyName = KEY_WAKING_COLOR,
			name = "Waking up",
			description = "Stop pickpocketing: no more pickpockets will land before the target wakes",
			section = coloursSection,
			position = 1
	)
	default Color wakingColor()
	{
		return BetterBlackjackingColors.WAKING;
	}

	/**
	 * Colour of {@link TargetState#KNOCK_OUT}: the target is awake and can be knocked out.
	 *
	 * @return the knock-out colour (default orange)
	 */
	@ConfigItem(
			keyName = KEY_KNOCK_OUT_COLOR,
			name = "Knock out",
			description = "The target is awake and can be knocked out",
			section = coloursSection,
			position = 2
	)
	default Color knockOutColor()
	{
		return BetterBlackjackingColors.KNOCK_OUT;
	}

	/**
	 * Colour of {@link TargetState#ATTACKING}: the target is in combat with the player, so it can't be
	 * knocked out until combat is broken.
	 *
	 * @return the attacking colour (default red)
	 */
	@ConfigItem(
			keyName = KEY_ATTACKING_COLOR,
			name = "Attacking",
			description = "You're being attacked and can't knock out. Break combat first, e.g. swap weapon.",
			section = coloursSection,
			position = 3
	)
	default Color attackingColor()
	{
		return BetterBlackjackingColors.ATTACKING;
	}

	/**
	 * How the countdown to the next knock-out is drawn.
	 *
	 * @return the timer style (default {@link TimerStyle#PIE})
	 */
	@ConfigItem(
			keyName = KEY_TIMER_STYLE,
			name = "Timer style",
			description = "How the countdown to the next knock-out is drawn on the target's tile",
			section = timerSection,
			position = 0
	)
	default TimerStyle timerStyle()
	{
		return TimerStyle.PIE;
	}

	/**
	 * Whether to draw the pips that show how many guaranteed pickpockets are left.
	 *
	 * @return whether the pickpocket pips are shown (default true)
	 */
	@ConfigItem(
			keyName = KEY_PICKPOCKET_PIPS,
			name = "Show pickpocket pips",
			description = "Show how many guaranteed pickpockets are left, and STOP when there are none",
			section = pickpocketsSection,
			position = 0
	)
	default boolean pickpocketPips()
	{
		return true;
	}

	/**
	 * The get-up animation the target plays, on this player's screen only, when it wakes.
	 *
	 * @return the wake-up animation (default {@link WakeAnimation#MAX_GET_UP})
	 */
	@ConfigItem(
			keyName = KEY_WAKE_ANIMATION,
			name = "Wake-up animation",
			description = "Play a get-up animation when the target wakes, instead of snapping to standing."
					+ " Only you see it.",
			section = animationSection,
			position = 0
	)
	default WakeAnimation wakeAnimation()
	{
		return WakeAnimation.MAX_GET_UP;
	}

	/**
	 * How many game ticks a knocked-out target stays down, from 3 to 8.
	 *
	 * @return the knock-out duration in ticks (default 5)
	 */
	@Range(min = 3, max = 8)
	@Units(Units.TICKS)
	@ConfigItem(
			keyName = KEY_KNOCK_OUT_TICKS,
			name = "Knock-out duration",
			description = "How many game ticks a knocked-out target stays down. Only change this if the game does.",
			section = advancedSection,
			position = 0
	)
	default int knockOutTicks()
	{
		return 5;
	}

	/**
	 * Whether to write knock-out, pickpocket and wake-up events to the client log.
	 *
	 * @return whether debug logging is on (default false)
	 */
	@ConfigItem(
			keyName = KEY_DEBUG_LOGGING,
			name = "Debug logging",
			description = "Write knock-out, pickpocket and wake-up events to the client log, for bug reports",
			section = advancedSection,
			position = 1
	)
	default boolean debugLogging()
	{
		return false;
	}
}
