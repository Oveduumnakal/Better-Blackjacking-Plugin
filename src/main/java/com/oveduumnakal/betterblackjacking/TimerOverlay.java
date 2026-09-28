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
import java.util.Locale;
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
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

/**
 * Draws the knock-out timer on the knocked-out target's tile, as a draining pie or as the seconds
 * left, in the target's state colour.
 *
 * <p>The timer is drawn only while the plugin is active, the timer style isn't
 * {@link TimerStyle#OFF}, and the target is knocked out ({@link TargetState#SAFE} or
 * {@link TargetState#WAKING}). It is centred on the tile's projected centre and sized from the tile
 * unit (see {@link TileScale}), and a target whose tile isn't on screen is skipped.
 */
public class TimerOverlay extends Overlay
{
	private static final long MILLIS_PER_TENTH = 100;

	private final TargetStateView view;

	private final BetterBlackjackingConfig config;

	private final Function<NPC, Polygon> tilePoly;

	private final ProgressPieComponent pie = new ProgressPieComponent();

	private Font font;

	private int fontSize;

	/**
	 * Creates the overlay, projecting each target's tile with {@code Perspective.getCanvasTilePoly}.
	 *
	 * @param client the client, used to project tiles onto the canvas
	 * @param view   the plugin state to draw
	 * @param config the plugin config, which gives the timer style
	 */
	@Inject
	public TimerOverlay(Client client, TargetStateView view, BetterBlackjackingConfig config)
	{
		this(view, config, npc -> canvasTilePoly(client, npc));
	}

	/**
	 * Creates the overlay with a custom tile projection, so tests can supply the polygons.
	 *
	 * @param view     the plugin state to draw
	 * @param config   the plugin config, which gives the timer style
	 * @param tilePoly the canvas polygon of a target's tile, or {@code null} when it's off-screen
	 */
	TimerOverlay(TargetStateView view, BetterBlackjackingConfig config, Function<NPC, Polygon> tilePoly)
	{
		this.view = view;
		this.config = config;
		this.tilePoly = tilePoly;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	private static Polygon canvasTilePoly(Client client, NPC npc)
	{
		LocalPoint location = npc.getLocalLocation();
		if (location == null)
			return null;

		return Perspective.getCanvasTilePoly(client, location);
	}

	/**
	 * The pie's filled fraction: the share of the knock-out still to run, which drains from 1 at the
	 * knock-out to 0 when the target wakes.
	 *
	 * @param remainingMillis the time until the target wakes, in milliseconds
	 * @param durationMillis  the full knock-out duration, in milliseconds
	 * @return {@code remainingMillis / durationMillis} clamped to [0, 1], or 0 if the duration isn't
	 * positive
	 */
	static double progress(long remainingMillis, long durationMillis)
	{
		if (durationMillis <= 0)
			return 0;

		double fraction = remainingMillis / (double) durationMillis;
		return Math.max(0, Math.min(1, fraction));
	}

	/**
	 * The seconds timer's text: the time left in seconds with one decimal, e.g. {@code 2.4}.
	 *
	 * <p>It rounds <em>up</em> to the next tenth of a second, so it never shows {@code 0.0} while any
	 * time is left: 2400 ms is {@code 2.4}, 2401 ms is {@code 2.5}, 50 ms is {@code 0.1}, and only
	 * 0 ms is {@code 0.0}. A negative time is treated as 0.
	 *
	 * @param remainingMillis the time until the target wakes, in milliseconds
	 * @return the seconds left, formatted with one decimal and a {@code .} separator
	 */
	static String secondsText(long remainingMillis)
	{
		long millis = Math.max(0, remainingMillis);
		long tenths = (millis + MILLIS_PER_TENTH - 1) / MILLIS_PER_TENTH;
		return String.format(Locale.ROOT, "%.1f", tenths / 10.0);
	}

	/**
	 * Draws the timer on every knocked-out eligible target whose tile is on screen.
	 *
	 * @param graphics the graphics to draw with
	 * @return {@code null}, since a dynamic overlay has no size of its own
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		TimerStyle style = config.timerStyle();
		if (!view.isActive() || style == null || style == TimerStyle.OFF)
			return null;

		for (NPC npc : view.eligibleTargets())
		{
			TargetState state = view.stateOf(npc);
			if (!state.isKnockedOut())
				continue;

			Polygon poly = tilePoly.apply(npc);
			int u = TileScale.unit(poly);
			Optional<Point> anchor = TileScale.anchor(poly);
			if (u == 0 || !anchor.isPresent())
				continue;

			Color color = view.colorOf(state);
			if (style == TimerStyle.PIE)
				renderPie(graphics, anchor.get(), u, color);
			else
				renderSeconds(graphics, anchor.get(), u, color);
		}

		return null;
	}

	private void renderPie(Graphics2D graphics, Point anchor, int u, Color color)
	{
		pie.setPosition(anchor);
		pie.setDiameter(2 * TileScale.pieRadius(u));
		pie.setFill(color);
		pie.setBorderColor(color);
		pie.setProgress(progress(view.remainingMillis(), view.durationMillis()));
		pie.render(graphics);
	}

	private void renderSeconds(Graphics2D graphics, Point anchor, int u, Color color)
	{
		String text = secondsText(view.remainingMillis());
		Font previous = graphics.getFont();
		graphics.setFont(fontOfSize(TileScale.fontSize(u)));
		FontMetrics metrics = graphics.getFontMetrics();
		int x = anchor.getX() - metrics.stringWidth(text) / 2;
		int y = anchor.getY() + (metrics.getAscent() - metrics.getDescent()) / 2;
		OverlayUtil.renderTextLocation(graphics, new Point(x, y), text, color);
		graphics.setFont(previous);
	}

	private Font fontOfSize(int size)
	{
		if (font == null || fontSize != size)
		{
			font = FontManager.getRunescapeBoldFont().deriveFont((float) size);
			fontSize = size;
		}

		return font;
	}
}
