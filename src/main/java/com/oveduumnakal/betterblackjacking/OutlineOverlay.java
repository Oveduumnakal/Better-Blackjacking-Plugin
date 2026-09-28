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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import javax.inject.Inject;

import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Outlines each eligible blackjack target's click box in the colour of its state, so the player can
 * see at a glance whether to pickpocket, get ready, knock out or break combat.
 *
 * <p>The click box is the NPC's convex hull. It is stroked at the configured outline width and filled
 * with the same colour at the configured fill opacity. Nothing is drawn while the plugin is inactive,
 * and a target without a hull (e.g. off-screen) is skipped.
 */
public class OutlineOverlay extends Overlay
{
	private final TargetStateView view;
	private final BetterBlackjackingConfig config;

	/**
	 * Creates the overlay, drawn above the scene at each target's own position.
	 *
	 * @param view   the plugin state to draw
	 * @param config the plugin config, for the outline width and fill opacity
	 */
	@Inject
	public OutlineOverlay(TargetStateView view, BetterBlackjackingConfig config)
	{
		this.view = view;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/**
	 * Outlines every eligible target's click box in its state colour.
	 *
	 * @param graphics the graphics to draw with
	 * @return always {@code null}, since a dynamic overlay has no bounds of its own
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!view.isActive())
			return null;

		Stroke stroke = new BasicStroke(config.outlineWidth());
		int fillAlpha = clampAlpha(config.fillOpacity());
		for (NPC npc : view.eligibleTargets())
		{
			Shape hull = npc.getConvexHull();
			if (hull == null)
				continue;

			Color color = view.colorOf(view.stateOf(npc));
			Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha);
			OverlayUtil.renderPolygon(graphics, hull, color, fill, stroke);
		}

		return null;
	}

	private static int clampAlpha(int alpha)
	{
		return Math.max(0, Math.min(255, alpha));
	}
}
