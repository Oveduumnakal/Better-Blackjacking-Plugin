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

/**
 * The plugin's shared colour palette, defined once so the config defaults and every overlay stay
 * consistent and a colour change is a one-line edit.
 *
 * <p>The four state colours are only defaults: the overlays draw with the colours the player has
 * configured, read through {@link TargetStateView#colorOf(TargetState)}.
 */
final class BetterBlackjackingColors
{
	/** Default colour of {@link TargetState#SAFE}: green {@code #00FF00}. */
	static final Color SAFE = new Color(0x00FF00);

	/** Default colour of {@link TargetState#WAKING}: yellow {@code #FFFF00}. */
	static final Color WAKING = new Color(0xFFFF00);

	/** Default colour of {@link TargetState#KNOCK_OUT}: orange {@code #FFA500}. */
	static final Color KNOCK_OUT = new Color(0xFFA500);

	/** Default colour of {@link TargetState#ATTACKING}: red {@code #FF0000}. */
	static final Color ATTACKING = new Color(0xFF0000);

	private BetterBlackjackingColors()
	{
	}
}
