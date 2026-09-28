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

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Optional;

import net.runelite.api.Point;

/**
 * Sizes and positions overlay elements from the target's on-screen tile, so they shrink and grow
 * with the tile (smaller when the camera is zoomed out) without ever reading the camera zoom.
 *
 * <p>The tile unit {@code u} is the on-screen width, in pixels, of the polygon from
 * {@code Perspective.getCanvasTilePoly}. Each size is a factor of {@code u}, rounded and clamped to a
 * pixel range so it stays readable at extreme zoom. A missing or degenerate polygon gives a unit of 0,
 * every size of 0 and no anchor, which tells the overlay to skip drawing that target.
 */
public final class TileScale
{
	/** The pie timer's radius as a fraction of the tile unit. */
	public static final double PIE_RADIUS_FACTOR = 0.35;

	/** The smallest pie timer radius, in pixels. */
	public static final int PIE_RADIUS_MIN = 6;

	/** The largest pie timer radius, in pixels. */
	public static final int PIE_RADIUS_MAX = 40;

	/** The seconds timer's font size as a fraction of the tile unit. */
	public static final double FONT_SIZE_FACTOR = 0.45;

	/** The smallest seconds timer font size, in pixels. */
	public static final int FONT_SIZE_MIN = 10;

	/** The largest seconds timer font size, in pixels. */
	public static final int FONT_SIZE_MAX = 32;

	/** The pickpocket pip's diameter as a fraction of the tile unit. */
	public static final double PIP_DIAMETER_FACTOR = 0.15;

	/** The smallest pickpocket pip diameter, in pixels. */
	public static final int PIP_DIAMETER_MIN = 4;

	/** The largest pickpocket pip diameter, in pixels. */
	public static final int PIP_DIAMETER_MAX = 16;

	private TileScale()
	{
	}

	/**
	 * The tile unit {@code u}: the on-screen width of the tile polygon's bounds, in pixels.
	 *
	 * @param tilePoly the tile's canvas polygon, from {@code Perspective.getCanvasTilePoly}; may be
	 *                 {@code null} when the tile is off-screen
	 * @return the tile width in pixels, or 0 if the polygon is {@code null} or degenerate (fewer than
	 * three points, or no width)
	 */
	public static int unit(Polygon tilePoly)
	{
		if (isDegenerate(tilePoly))
			return 0;

		return tilePoly.getBounds().width;
	}

	/**
	 * Rounds {@code value} to the nearest pixel and clamps it to {@code [min, max]}.
	 *
	 * @param value a size in pixels, usually a factor times the tile unit
	 * @param min   the smallest allowed size
	 * @param max   the largest allowed size, at least {@code min}
	 * @return the rounded size, between {@code min} and {@code max}
	 */
	public static int clamp(double value, int min, int max)
	{
		long rounded = Math.round(value);
		return (int) Math.max(min, Math.min(max, rounded));
	}

	/**
	 * A size of {@code factor × u}, rounded and clamped to {@code [min, max]}. A unit of 0 or less (no
	 * usable tile) gives 0, so the caller skips drawing rather than drawing at the minimum size.
	 *
	 * @param u      the tile unit from {@link #unit(Polygon)}
	 * @param factor the size as a fraction of the tile unit
	 * @param min    the smallest size for a usable tile
	 * @param max    the largest size
	 * @return the size in pixels, or 0 if {@code u} is 0 or less
	 */
	public static int scaled(int u, double factor, int min, int max)
	{
		if (u <= 0)
			return 0;

		return clamp(factor * u, min, max);
	}

	/**
	 * The pie timer's radius: {@code 0.35u} clamped to [6, 40] px.
	 *
	 * @param u the tile unit from {@link #unit(Polygon)}
	 * @return the radius in pixels, or 0 if {@code u} is 0 or less
	 */
	public static int pieRadius(int u)
	{
		return scaled(u, PIE_RADIUS_FACTOR, PIE_RADIUS_MIN, PIE_RADIUS_MAX);
	}

	/**
	 * The seconds timer's font size: {@code 0.45u} clamped to [10, 32] px.
	 *
	 * @param u the tile unit from {@link #unit(Polygon)}
	 * @return the font size in pixels, or 0 if {@code u} is 0 or less
	 */
	public static int fontSize(int u)
	{
		return scaled(u, FONT_SIZE_FACTOR, FONT_SIZE_MIN, FONT_SIZE_MAX);
	}

	/**
	 * A pickpocket pip's diameter: {@code 0.15u} clamped to [4, 16] px.
	 *
	 * @param u the tile unit from {@link #unit(Polygon)}
	 * @return the diameter in pixels, or 0 if {@code u} is 0 or less
	 */
	public static int pipDiameter(int u)
	{
		return scaled(u, PIP_DIAMETER_FACTOR, PIP_DIAMETER_MIN, PIP_DIAMETER_MAX);
	}

	/**
	 * The tile's projected centre on the canvas, where the timer is drawn.
	 *
	 * <p>For the usual four-corner tile polygon this is where its diagonals cross, which is exactly the
	 * projection of the tile's centre (a perspective projection keeps straight lines straight), and so
	 * matches {@code Perspective.localToCanvas} at height 0 for a flat tile. For any other polygon, or if
	 * the diagonals don't cross inside it, it falls back to the centre of the polygon's bounds.
	 *
	 * @param tilePoly the tile's canvas polygon; may be {@code null}
	 * @return the centre in canvas pixels, or empty if the polygon is {@code null} or degenerate (exactly
	 * when {@link #unit(Polygon)} is 0)
	 */
	public static Optional<Point> anchor(Polygon tilePoly)
	{
		if (isDegenerate(tilePoly))
			return Optional.empty();

		if (tilePoly.npoints == 4)
		{
			Point crossing = diagonalCrossing(tilePoly.xpoints, tilePoly.ypoints);
			if (crossing != null)
				return Optional.of(crossing);
		}

		Rectangle bounds = tilePoly.getBounds();
		return Optional.of(new Point((int) Math.round(bounds.getCenterX()), (int) Math.round(bounds.getCenterY())));
	}

	private static boolean isDegenerate(Polygon tilePoly)
	{
		return tilePoly == null || tilePoly.npoints < 3 || tilePoly.getBounds().width <= 0;
	}

	/**
	 * Where the diagonal from corner 0 to 2 crosses the diagonal from corner 1 to 3, or {@code null}
	 * if they are parallel or don't cross within both segments.
	 */
	private static Point diagonalCrossing(int[] x, int[] y)
	{
		double d1x = x[2] - x[0];
		double d1y = y[2] - y[0];
		double d2x = x[3] - x[1];
		double d2y = y[3] - y[1];
		double denominator = d1x * d2y - d1y * d2x;
		if (denominator == 0)
			return null;

		double ox = x[1] - x[0];
		double oy = y[1] - y[0];
		double t = (ox * d2y - oy * d2x) / denominator;
		double s = (ox * d1y - oy * d1x) / denominator;
		if (t < 0 || t > 1 || s < 0 || s > 1)
			return null;

		return new Point((int) Math.round(x[0] + t * d1x), (int) Math.round(y[0] + t * d1y));
	}
}
