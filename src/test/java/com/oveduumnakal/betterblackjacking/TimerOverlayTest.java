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
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the timer's pie fraction and seconds text, and which targets the overlay draws on, by
 * rendering into a real image with fake tile polygons.
 */
public class TimerOverlayTest
{
	private static final Color TIMER_COLOR = new Color(0, 255, 0);

	private static final long DURATION = 3000;

	private final TargetStateView view = mock(TargetStateView.class);

	private final BetterBlackjackingConfig config = mock(BetterBlackjackingConfig.class);

	private final Map<NPC, Polygon> tiles = new HashMap<>();

	private final Map<NPC, TargetState> states = new HashMap<>();

	private TimerOverlay overlay;

	private BufferedImage image;

	@Before
	public void setUp()
	{
		when(view.isActive()).thenReturn(true);
		when(view.remainingMillis()).thenReturn(2400L);
		when(view.durationMillis()).thenReturn(DURATION);
		when(view.colorOf(any())).thenReturn(TIMER_COLOR);
		when(view.stateOf(any())).thenAnswer(invocation -> states.get(invocation.<NPC>getArgument(0)));
		when(config.timerStyle()).thenReturn(TimerStyle.PIE);
		overlay = new TimerOverlay(view, config, tiles::get);
		image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
	}

	private NPC target(TargetState state, int left)
	{
		NPC npc = mock(NPC.class);
		Polygon tile = new Polygon();
		tile.addPoint(left, 40);
		tile.addPoint(left + 80, 40);
		tile.addPoint(left + 80, 120);
		tile.addPoint(left, 120);
		tiles.put(npc, tile);
		states.put(npc, state);
		return npc;
	}

	private void targets(NPC... npcs)
	{
		when(view.eligibleTargets()).thenReturn(Arrays.asList(npcs));
	}

	private void render()
	{
		Graphics2D graphics = image.createGraphics();
		try
		{
			assertNull(overlay.render(graphics));
		}
		finally
		{
			graphics.dispose();
		}
	}

	private int paintedPixels(Rectangle area)
	{
		int painted = 0;
		for (int x = area.x; x < area.x + area.width; x++)
		{
			for (int y = area.y; y < area.y + area.height; y++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
					painted++;
			}
		}

		return painted;
	}

	private int paintedPixels()
	{
		return paintedPixels(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
	}

	private static Rectangle tileArea(int left)
	{
		return new Rectangle(left, 40, 80, 80);
	}

	@Test
	public void progressIsTheShareOfTheKnockOutLeft()
	{
		assertEquals(1.0, TimerOverlay.progress(3000, 3000), 0);
		assertEquals(0.8, TimerOverlay.progress(2400, 3000), 1e-9);
		assertEquals(0.5, TimerOverlay.progress(1500, 3000), 1e-9);
		assertEquals(0.0, TimerOverlay.progress(0, 3000), 0);
	}

	@Test
	public void progressIsClampedAndSafeForABadDuration()
	{
		assertEquals(1.0, TimerOverlay.progress(4000, 3000), 0);
		assertEquals(0.0, TimerOverlay.progress(-50, 3000), 0);
		assertEquals(0.0, TimerOverlay.progress(1000, 0), 0);
	}

	@Test
	public void secondsTextHasOneDecimal()
	{
		assertEquals("2.4", TimerOverlay.secondsText(2400));
		assertEquals("3.0", TimerOverlay.secondsText(3000));
		assertEquals("0.0", TimerOverlay.secondsText(0));
		assertEquals("12.0", TimerOverlay.secondsText(12000));
	}

	@Test
	public void secondsTextRoundsUpSoItNeverShowsZeroWhileTimeIsLeft()
	{
		assertEquals("0.1", TimerOverlay.secondsText(50));
		assertEquals("0.1", TimerOverlay.secondsText(1));
		assertEquals("0.1", TimerOverlay.secondsText(100));
		assertEquals("2.5", TimerOverlay.secondsText(2401));
		assertEquals("2.4", TimerOverlay.secondsText(2399));
		assertEquals("0.0", TimerOverlay.secondsText(-20));
	}

	@Test
	public void subTickClockDrivesASmoothCountdown()
	{
		long[] nanos = {0};
		SubTickClock clock = new SubTickClock(() -> nanos[0]);
		clock.onTick();
		long duration = 5 * SubTickClock.TICK_MILLIS;

		assertEquals("3.0", TimerOverlay.secondsText(clock.remainingMillis(105, 100)));
		assertEquals(1.0, TimerOverlay.progress(clock.remainingMillis(105, 100), duration), 0);

		nanos[0] = 250_000_000L;
		assertEquals("2.8", TimerOverlay.secondsText(clock.remainingMillis(105, 100)));
		assertEquals(2750 / 3000.0, TimerOverlay.progress(clock.remainingMillis(105, 100), duration), 1e-9);

		nanos[0] = 550_000_000L;
		assertEquals("0.1", TimerOverlay.secondsText(clock.remainingMillis(105, 104)));
		assertEquals("0.0", TimerOverlay.secondsText(clock.remainingMillis(105, 105)));
		assertEquals(0.0, TimerOverlay.progress(clock.remainingMillis(105, 105), duration), 0);
	}

	@Test
	public void isADynamicOverlayAboveTheScene()
	{
		TimerOverlay injected = new TimerOverlay(mock(Client.class), view, config);
		assertEquals(OverlayPosition.DYNAMIC, injected.getPosition());
		assertEquals(OverlayLayer.ABOVE_SCENE, injected.getLayer());
	}

	@Test
	public void offDrawsNothing()
	{
		when(config.timerStyle()).thenReturn(TimerStyle.OFF);
		targets(target(TargetState.SAFE, 20), target(TargetState.WAKING, 220));
		render();
		assertEquals(0, paintedPixels());
	}

	@Test
	public void inactiveDrawsNothing()
	{
		when(view.isActive()).thenReturn(false);
		targets(target(TargetState.SAFE, 20));
		render();
		assertEquals(0, paintedPixels());
	}

	@Test
	public void pieDrawsOnlyOnKnockedOutTargets()
	{
		targets(target(TargetState.SAFE, 20), target(TargetState.KNOCK_OUT, 120),
				target(TargetState.ATTACKING, 220), target(TargetState.WAKING, 310));
		render();
		assertTrue(paintedPixels(tileArea(20)) > 0);
		assertEquals(0, paintedPixels(tileArea(120)));
		assertEquals(0, paintedPixels(tileArea(220)));
		assertTrue(paintedPixels(tileArea(310)) > 0);
	}

	@Test
	public void pieIsDrawnInTheStateColourAroundTheTileCentre()
	{
		targets(target(TargetState.SAFE, 20));
		render();
		assertEquals(TIMER_COLOR.getRGB(), image.getRGB(70, 90));
		assertEquals(0, paintedPixels(new Rectangle(0, 0, 400, 50)));
		assertEquals(0, paintedPixels(new Rectangle(0, 110, 400, 90)));
	}

	@Test
	public void emptyPieDrawsOnlyItsBorder()
	{
		targets(target(TargetState.WAKING, 20));
		render();
		int full = paintedPixels();

		image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		when(view.remainingMillis()).thenReturn(0L);
		render();
		assertTrue(paintedPixels() < full);
	}

	@Test
	public void secondsDrawsOnlyOnKnockedOutTargets()
	{
		when(config.timerStyle()).thenReturn(TimerStyle.SECONDS);
		targets(target(TargetState.SAFE, 20), target(TargetState.KNOCK_OUT, 120),
				target(TargetState.ATTACKING, 220), target(TargetState.WAKING, 310));
		render();
		assertTrue(paintedPixels(tileArea(20)) > 0);
		assertEquals(0, paintedPixels(tileArea(120)));
		assertEquals(0, paintedPixels(tileArea(220)));
		assertTrue(paintedPixels(tileArea(310)) > 0);
	}

	@Test
	public void secondsTextIsInTheStateColourWithADarkShadow()
	{
		when(config.timerStyle()).thenReturn(TimerStyle.SECONDS);
		targets(target(TargetState.SAFE, 20));
		render();
		boolean coloured = false;
		boolean shadow = false;
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				int rgb = image.getRGB(x, y);
				coloured |= rgb == TIMER_COLOR.getRGB();
				shadow |= rgb == Color.BLACK.getRGB();
			}
		}

		assertTrue(coloured);
		assertTrue(shadow);
	}

	@Test
	public void targetWithoutATileIsSkipped()
	{
		NPC offScreen = target(TargetState.SAFE, 20);
		tiles.put(offScreen, null);
		NPC degenerate = target(TargetState.SAFE, 120);
		tiles.put(degenerate, new Polygon(new int[]{130, 130, 130}, new int[]{40, 80, 120}, 3));
		NPC shown = target(TargetState.SAFE, 220);
		targets(offScreen, degenerate, shown);

		render();
		assertEquals(0, paintedPixels(new Rectangle(0, 0, 200, 200)));
		assertTrue(paintedPixels(tileArea(220)) > 0);

		when(config.timerStyle()).thenReturn(TimerStyle.SECONDS);
		image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		render();
		assertEquals(0, paintedPixels(new Rectangle(0, 0, 200, 200)));
		assertTrue(paintedPixels(tileArea(220)) > 0);
	}
}
