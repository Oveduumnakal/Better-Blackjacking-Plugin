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
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Covers the pickpocket pips: how many are filled, when {@code STOP} shows, the gates and the layout. */
public class PickpocketIndicatorOverlayTest
{
	private static final Color SAFE = new Color(0x00FF00);
	private static final Color WAKING = new Color(0xFFFF00);
	private static final int SIZE = 250;
	private static final int U = 100;
	private static final Point ANCHOR = new Point(100, 100);

	private TargetStateView view;
	private BetterBlackjackingConfig config;
	private NPC npc;
	private Polygon polygon;
	private PickpocketIndicatorOverlay overlay;

	private static Polygon square(int x, int y, int size)
	{
		Polygon square = new Polygon();
		square.addPoint(x, y);
		square.addPoint(x + size, y);
		square.addPoint(x + size, y + size);
		square.addPoint(x, y + size);
		return square;
	}

	@Before
	public void setUp()
	{
		view = mock(TargetStateView.class);
		config = mock(BetterBlackjackingConfig.class, Answers.CALLS_REAL_METHODS);
		npc = mock(NPC.class);
		polygon = square(50, 50, U);
		when(view.isActive()).thenReturn(true);
		when(view.eligibleTargets()).thenReturn(Collections.singletonList(npc));
		when(view.stateOf(npc)).thenReturn(TargetState.SAFE);
		when(view.colorOf(TargetState.SAFE)).thenReturn(SAFE);
		when(view.colorOf(TargetState.WAKING)).thenReturn(WAKING);
		when(view.colorOf(TargetState.KNOCK_OUT)).thenReturn(new Color(0xFFA500));
		when(view.colorOf(TargetState.ATTACKING)).thenReturn(new Color(0xFF0000));
		overlay = new PickpocketIndicatorOverlay(n -> n == npc ? polygon : null, view, config);
	}

	private BufferedImage render()
	{
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			assertNull(overlay.render(graphics));
		}
		finally
		{
			graphics.dispose();
		}

		return image;
	}

	private static Rectangle[] pips()
	{
		return PickpocketIndicatorOverlay.pipBounds(ANCHOR, U);
	}

	private static boolean isFilled(BufferedImage image, Rectangle pip)
	{
		return image.getRGB((int) pip.getCenterX(), (int) pip.getCenterY()) == SAFE.getRGB();
	}

	private static boolean hasOutline(BufferedImage image, Rectangle pip)
	{
		int y = (int) pip.getCenterY();
		for (int x = pip.x; x < pip.x + 2; x++)
		{
			int argb = image.getRGB(x, y);
			if ((argb >>> 24) > 0 && (argb & 0xFF0000) == 0)
				return true;
		}

		return false;
	}

	private static boolean isYellowish(int argb)
	{
		int alpha = argb >>> 24;
		int red = (argb >> 16) & 0xFF;
		int green = (argb >> 8) & 0xFF;
		int blue = argb & 0xFF;
		return alpha > 200 && red > 200 && green > 200 && blue < 100;
	}

	/** The bounds of the yellow {@code STOP} pixels, or {@code null} if there are none. */
	private static Rectangle stopBounds(BufferedImage image)
	{
		Rectangle bounds = null;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if (!isYellowish(image.getRGB(x, y)))
					continue;

				if (bounds == null)
					bounds = new Rectangle(x, y, 1, 1);
				else
					bounds.add(new Rectangle(x, y, 1, 1));
			}
		}

		return bounds;
	}

	private static boolean isBlank(BufferedImage image)
	{
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
					return false;
			}
		}

		return true;
	}

	@Test
	public void twoLeftFillsBothPipsWithoutStop()
	{
		when(view.pickpocketsLeft()).thenReturn(2);
		BufferedImage image = render();
		assertTrue(isFilled(image, pips()[0]));
		assertTrue(isFilled(image, pips()[1]));
		assertNull(stopBounds(image));
	}

	@Test
	public void oneLeftFillsOnlyTheFirstPip()
	{
		when(view.pickpocketsLeft()).thenReturn(1);
		BufferedImage image = render();
		assertTrue(isFilled(image, pips()[0]));
		assertFalse(isFilled(image, pips()[1]));
		assertTrue(hasOutline(image, pips()[1]));
		assertNull(stopBounds(image));
	}

	@Test
	public void zeroLeftLeavesBothPipsHollowAndDrawsStopBelowThem()
	{
		when(view.pickpocketsLeft()).thenReturn(0);
		when(view.stateOf(npc)).thenReturn(TargetState.WAKING);
		BufferedImage image = render();
		Rectangle[] pips = pips();
		for (Rectangle pip : pips)
		{
			assertFalse(isFilled(image, pip));
			assertTrue(hasOutline(image, pip));
		}

		Rectangle stop = stopBounds(image);
		assertNotNull("STOP should be drawn", stop);
		assertTrue("STOP should be below the pips", stop.y >= pips[0].y + pips[0].height);
		assertEquals(ANCHOR.getX(), stop.getCenterX(), 3);
	}

	@Test
	public void outOfRangeCountsAreClamped()
	{
		when(view.pickpocketsLeft()).thenReturn(5);
		BufferedImage image = render();
		assertTrue(isFilled(image, pips()[0]));
		assertTrue(isFilled(image, pips()[1]));

		when(view.pickpocketsLeft()).thenReturn(-1);
		image = render();
		assertFalse(isFilled(image, pips()[0]));
		assertNotNull(stopBounds(image));
	}

	@Test
	public void drawsNothingWhenTurnedOff()
	{
		when(view.pickpocketsLeft()).thenReturn(0);
		doReturn(false)
			.when(config)
			.pickpocketPips();
		assertTrue(isBlank(render()));
	}

	@Test
	public void drawsNothingWhenInactive()
	{
		when(view.pickpocketsLeft()).thenReturn(0);
		when(view.isActive()).thenReturn(false);
		assertTrue(isBlank(render()));
	}

	@Test
	public void drawsNothingForTargetsThatAreNotKnockedOut()
	{
		when(view.pickpocketsLeft()).thenReturn(0);
		for (TargetState state : Arrays.asList(TargetState.KNOCK_OUT, TargetState.ATTACKING))
		{
			when(view.stateOf(npc)).thenReturn(state);
			assertTrue(state.name(), isBlank(render()));
		}
	}

	@Test
	public void skipsATargetWithNoTilePolygon()
	{
		when(view.pickpocketsLeft()).thenReturn(0);
		polygon = null;
		assertTrue(isBlank(render()));

		polygon = new Polygon();
		assertTrue(isBlank(render()));
	}

	@Test
	public void pipsAreCentredBelowThePieAndInsideTheTileWidth()
	{
		for (int u : new int[]{20, 60, 100, 200})
		{
			Rectangle[] pips = PickpocketIndicatorOverlay.pipBounds(ANCHOR, u);
			int diameter = TileScale.pipDiameter(u);
			assertEquals(2, pips.length);
			assertEquals(diameter, pips[0].width);
			assertEquals(diameter, pips[1].height);
			assertEquals(pips[0].y, pips[1].y);
			assertTrue(pips[0].y > ANCHOR.getY() + TileScale.pieRadius(u));

			int gap = pips[1].x - (pips[0].x + pips[0].width);
			assertEquals(diameter / 2.0, gap, 1);

			double centre = (pips[0].x + pips[1].x + pips[1].width) / 2.0;
			assertEquals(ANCHOR.getX(), centre, 1);
			assertTrue(pips[1].x + pips[1].width - pips[0].x < u);
		}
	}

	@Test
	public void stopFontIsScaledFromTheTimerFontAndClamped()
	{
		assertEquals(26, PickpocketIndicatorOverlay.stopFontSize(200));
		assertEquals(22, PickpocketIndicatorOverlay.stopFontSize(60));
		assertEquals(8, PickpocketIndicatorOverlay.stopFontSize(10));
	}

	@Test
	public void isADynamicOverlayAboveTheScene()
	{
		PickpocketIndicatorOverlay injected = new PickpocketIndicatorOverlay(mock(Client.class), view, config);
		assertEquals(OverlayPosition.DYNAMIC, injected.getPosition());
		assertEquals(OverlayLayer.ABOVE_SCENE, injected.getLayer());
	}
}
