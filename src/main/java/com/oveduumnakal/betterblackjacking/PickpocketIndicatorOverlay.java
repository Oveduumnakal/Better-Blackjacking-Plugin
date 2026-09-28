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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the pickpocket pips under the knocked-out target's timer (spec §5.4).
 *
 * <p>Two pips sit side by side just below where the pie timer is drawn. The first
 * {@link TargetStateView#pickpocketsLeft()} are filled in the {@link TargetState#SAFE} colour and the
 * rest are hollow outlines in the same colour. When no pickpockets are left both pips are hollow and
 * the word {@code STOP} is drawn below them in the {@link TargetState#WAKING} colour. Everything is
 * sized from the target's tile through {@link TileScale}, and nothing is drawn for a target whose
 * tile isn't on screen.
 */
public class PickpocketIndicatorOverlay extends Overlay
{
	/** How many pips are drawn: the most pickpockets a single knock-out allows. */
	static final int PIP_COUNT = 2;

	/** The word drawn below the pips when no pickpockets are left. */
	static final String STOP_TEXT = "STOP";

	/** The {@code STOP} font size as a fraction of {@link TileScale#fontSize(int)}. */
	static final double STOP_FONT_SCALE = 0.8;

	/** The smallest {@code STOP} font size, in pixels. */
	static final int STOP_FONT_MIN = 8;

	/** The largest {@code STOP} font size, in pixels. */
	static final int STOP_FONT_MAX = 26;

	private final Function<NPC, Polygon> tilePolygon;
	private final TargetStateView view;
	private final BetterBlackjackingConfig config;

	/**
	 * Creates the overlay, reading each target's tile polygon from the client's perspective.
	 *
	 * @param client the client, used to project the target's tile onto the canvas
	 * @param view   the plugin's state
	 * @param config the plugin's config
	 */
	@Inject
	PickpocketIndicatorOverlay(Client client, TargetStateView view, BetterBlackjackingConfig config)
	{
		this(npc -> canvasTilePoly(client, npc), view, config);
	}

	/**
	 * Creates the overlay with a custom tile polygon source, so tests can supply fixed polygons.
	 *
	 * @param tilePolygon gives a target's tile polygon on the canvas, or {@code null} when it's off-screen
	 * @param view        the plugin's state
	 * @param config      the plugin's config
	 */
	PickpocketIndicatorOverlay(Function<NPC, Polygon> tilePolygon, TargetStateView view,
		BetterBlackjackingConfig config)
	{
		this.tilePolygon = tilePolygon;
		this.view = view;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/**
	 * Draws the pips (and {@code STOP} at zero) for each knocked-out eligible target, while the plugin
	 * is active and the pips are enabled.
	 *
	 * @param graphics the canvas graphics
	 * @return {@code null}, as a dynamic overlay has no fixed size
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!view.isActive() || !config.pickpocketPips())
			return null;

		for (NPC npc : view.eligibleTargets())
		{
			if (view.stateOf(npc).isKnockedOut())
				renderTarget(graphics, npc);
		}

		return null;
	}

	private void renderTarget(Graphics2D graphics, NPC npc)
	{
		Polygon poly = tilePolygon.apply(npc);
		int u = TileScale.unit(poly);
		Optional<Point> anchor = TileScale.anchor(poly);
		if (u == 0 || !anchor.isPresent())
			return;

		Rectangle[] pips = pipBounds(anchor.get(), u);
		int left = Math.max(0, Math.min(PIP_COUNT, view.pickpocketsLeft()));
		Color safe = view.colorOf(TargetState.SAFE);

		Object antialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(safe);
		for (int i = 0; i < pips.length; i++)
		{
			Rectangle pip = pips[i];
			if (i < left)
				graphics.fillOval(pip.x, pip.y, pip.width, pip.height);
			else
				graphics.drawOval(pip.x, pip.y, pip.width - 1, pip.height - 1);
		}

		if (antialiasing != null)
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);

		if (left == 0)
			renderStop(graphics, anchor.get(), pips[0], u);
	}

	private void renderStop(Graphics2D graphics, Point anchor, Rectangle firstPip, int u)
	{
		Font font = FontManager.getRunescapeBoldFont().deriveFont((float) stopFontSize(u));
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		int x = anchor.getX() - metrics.stringWidth(STOP_TEXT) / 2;
		int y = firstPip.y + firstPip.height + verticalGap(firstPip.height) + metrics.getAscent();
		OverlayUtil.renderTextLocation(graphics, new Point(x, y), STOP_TEXT, view.colorOf(TargetState.WAKING));
	}

	/**
	 * Where the two pips go for a tile anchored at {@code anchor} with unit {@code u}: side by side
	 * with a gap of half a pip, centred horizontally on the anchor, just below where the pie timer's
	 * bottom edge would be.
	 *
	 * @param anchor the tile's centre on the canvas
	 * @param u      the tile unit, greater than 0
	 * @return the bounds of each pip, left to right
	 */
	static Rectangle[] pipBounds(Point anchor, int u)
	{
		int diameter = TileScale.pipDiameter(u);
		int gap = Math.max(1, Math.round(diameter / 2f));
		int totalWidth = PIP_COUNT * diameter + (PIP_COUNT - 1) * gap;
		int x = anchor.getX() - totalWidth / 2;
		int y = anchor.getY() + TileScale.pieRadius(u) + verticalGap(diameter);
		Rectangle[] pips = new Rectangle[PIP_COUNT];
		for (int i = 0; i < PIP_COUNT; i++)
			pips[i] = new Rectangle(x + i * (diameter + gap), y, diameter, diameter);

		return pips;
	}

	/**
	 * The {@code STOP} font size: {@link TileScale#fontSize(int)} times {@link #STOP_FONT_SCALE},
	 * clamped to [{@link #STOP_FONT_MIN}, {@link #STOP_FONT_MAX}] px.
	 *
	 * @param u the tile unit, greater than 0
	 * @return the font size in pixels
	 */
	static int stopFontSize(int u)
	{
		return TileScale.clamp(TileScale.fontSize(u) * STOP_FONT_SCALE, STOP_FONT_MIN, STOP_FONT_MAX);
	}

	private static int verticalGap(int diameter)
	{
		return Math.max(1, diameter / 4);
	}

	private static Polygon canvasTilePoly(Client client, NPC npc)
	{
		LocalPoint location = npc.getLocalLocation();
		if (location == null)
			return null;

		return Perspective.getCanvasTilePoly(client, location);
	}
}
