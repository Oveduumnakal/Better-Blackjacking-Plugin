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
import java.util.Optional;

import org.junit.Test;

import net.runelite.api.Point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Covers the tile unit, the clamped sizes derived from it and the tile's anchor point. */
public class TileScaleTest
{
	private static Polygon poly(int... xy)
	{
		Polygon polygon = new Polygon();
		for (int i = 0; i < xy.length; i += 2)
			polygon.addPoint(xy[i], xy[i + 1]);

		return polygon;
	}

	@Test
	public void unitIsTheTileWidth()
	{
		assertEquals(100, TileScale.unit(poly(0, 0, 100, 0, 100, 80, 0, 80)));
		assertEquals(80, TileScale.unit(poly(10, 100, 90, 100, 70, 40, 30, 40)));
	}

	@Test
	public void unitIsZeroForAMissingOrDegeneratePolygon()
	{
		assertEquals(0, TileScale.unit(null));
		assertEquals(0, TileScale.unit(new Polygon()));
		assertEquals(0, TileScale.unit(poly(0, 0, 50, 50)));
		assertEquals(0, TileScale.unit(poly(20, 0, 20, 30, 20, 60, 20, 90)));
	}

	@Test
	public void clampRoundsAndBoundsBothEnds()
	{
		assertEquals(6, TileScale.clamp(5.5, 0, 10));
		assertEquals(5, TileScale.clamp(5.4, 0, 10));
		assertEquals(0, TileScale.clamp(-3, 0, 10));
		assertEquals(10, TileScale.clamp(1e12, 0, 10));
	}

	@Test
	public void pieRadiusIsAThirdOfTheTileWithinItsRange()
	{
		assertEquals(21, TileScale.pieRadius(60));
		assertEquals(6, TileScale.pieRadius(10));
		assertEquals(40, TileScale.pieRadius(200));
		assertEquals(6, TileScale.pieRadius(1));
	}

	@Test
	public void fontSizeIsNearlyHalfTheTileWithinItsRange()
	{
		assertEquals(18, TileScale.fontSize(40));
		assertEquals(10, TileScale.fontSize(10));
		assertEquals(32, TileScale.fontSize(100));
	}

	@Test
	public void pipDiameterIsAFractionOfTheTileWithinItsRange()
	{
		assertEquals(9, TileScale.pipDiameter(60));
		assertEquals(4, TileScale.pipDiameter(10));
		assertEquals(16, TileScale.pipDiameter(200));
	}

	@Test
	public void sizesAreZeroWithoutATile()
	{
		assertEquals(0, TileScale.pieRadius(0));
		assertEquals(0, TileScale.fontSize(0));
		assertEquals(0, TileScale.pipDiameter(0));
		assertEquals(0, TileScale.pieRadius(-5));
	}

	@Test
	public void anchorOfASquareIsItsCentre()
	{
		assertEquals(Optional.of(new Point(50, 40)), TileScale.anchor(poly(0, 0, 100, 0, 100, 80, 0, 80)));
	}

	@Test
	public void anchorOfAPerspectiveTileIsWhereTheDiagonalsCross()
	{
		Polygon trapezoid = poly(10, 100, 90, 100, 70, 40, 30, 40);
		assertEquals(Optional.of(new Point(50, 60)), TileScale.anchor(trapezoid));
	}

	@Test
	public void anchorFallsBackToTheBoundsCentre()
	{
		assertEquals(Optional.of(new Point(50, 40)), TileScale.anchor(poly(0, 0, 100, 80, 100, 0, 0, 80)));
		assertEquals(Optional.of(new Point(50, 30)), TileScale.anchor(poly(0, 60, 100, 60, 50, 0)));
	}

	@Test
	public void anchorIsEmptyForAMissingOrDegeneratePolygon()
	{
		assertFalse(TileScale.anchor(null).isPresent());
		assertFalse(TileScale.anchor(new Polygon()).isPresent());
		assertFalse(TileScale.anchor(poly(0, 0, 50, 50)).isPresent());
		assertFalse(TileScale.anchor(poly(20, 0, 20, 30, 20, 60, 20, 90)).isPresent());
	}
}
