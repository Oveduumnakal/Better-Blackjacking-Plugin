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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Covers the click-box outline: its colour per state, its stroke and fill, and when it draws nothing. */
public class OutlineOverlayTest
{
	private static final Map<TargetState, Color> COLORS = new EnumMap<>(TargetState.class);

	static
	{
		COLORS.put(TargetState.SAFE, BetterBlackjackingColors.SAFE);
		COLORS.put(TargetState.WAKING, BetterBlackjackingColors.WAKING);
		COLORS.put(TargetState.KNOCK_OUT, BetterBlackjackingColors.KNOCK_OUT);
		COLORS.put(TargetState.ATTACKING, BetterBlackjackingColors.ATTACKING);
	}

	private TargetStateView view;
	private BetterBlackjackingConfig config;
	private OutlineOverlay overlay;
	private Graphics2D graphics;

	@Before
	public void setUp()
	{
		view = mock(TargetStateView.class);
		config = mock(BetterBlackjackingConfig.class, Answers.CALLS_REAL_METHODS);
		graphics = mock(Graphics2D.class);
		overlay = new OutlineOverlay(view, config);
		when(view.isActive()).thenReturn(true);
		for (Map.Entry<TargetState, Color> entry : COLORS.entrySet())
			when(view.colorOf(entry.getKey())).thenReturn(entry.getValue());
	}

	@Test
	public void isADynamicOverlayAboveTheScene()
	{
		assertEquals(OverlayPosition.DYNAMIC, overlay.getPosition());
		assertEquals(OverlayLayer.ABOVE_SCENE, overlay.getLayer());
	}

	@Test
	public void eachStateIsOutlinedAndFilledInItsColour()
	{
		for (TargetState state : TargetState.values())
		{
			Shape hull = new Rectangle(state.ordinal() * 10, 0, 5, 5);
			NPC npc = npc(hull);
			when(view.stateOf(npc)).thenReturn(state);
			when(view.eligibleTargets()).thenReturn(Collections.singletonList(npc));
			Graphics2D g = mock(Graphics2D.class);

			assertNull(overlay.render(g));

			Color color = COLORS.get(state);
			InOrder order = inOrder(g);
			order.verify(g).setColor(color);
			order.verify(g).draw(hull);
			order.verify(g).setColor(withAlpha(color, 20));
			order.verify(g).fill(hull);
		}
	}

	@Test
	public void everyEligibleTargetIsDrawnInItsOwnStateColour()
	{
		NPC safe = npc(new Rectangle(0, 0, 5, 5));
		NPC attacking = npc(new Rectangle(10, 0, 5, 5));
		when(view.stateOf(safe)).thenReturn(TargetState.SAFE);
		when(view.stateOf(attacking)).thenReturn(TargetState.ATTACKING);
		when(view.eligibleTargets()).thenReturn(Arrays.asList(safe, attacking));

		overlay.render(graphics);

		InOrder order = inOrder(graphics);
		order.verify(graphics).setColor(BetterBlackjackingColors.SAFE);
		order.verify(graphics).draw(safe.getConvexHull());
		order.verify(graphics).setColor(BetterBlackjackingColors.ATTACKING);
		order.verify(graphics).draw(attacking.getConvexHull());
	}

	@Test
	public void strokeWidthAndFillOpacityComeFromConfig()
	{
		when(config.outlineWidth()).thenReturn(5);
		when(config.fillOpacity()).thenReturn(77);
		NPC npc = npc(new Rectangle(0, 0, 5, 5));
		when(view.stateOf(npc)).thenReturn(TargetState.WAKING);
		when(view.eligibleTargets()).thenReturn(Collections.singletonList(npc));

		overlay.render(graphics);

		ArgumentCaptor<Stroke> strokes = ArgumentCaptor.forClass(Stroke.class);
		verify(graphics, times(2)).setStroke(strokes.capture());
		assertEquals(5f, firstLineWidth(strokes), 0f);

		ArgumentCaptor<Color> colors = ArgumentCaptor.forClass(Color.class);
		verify(graphics, times(2)).setColor(colors.capture());
		List<Color> used = colors.getAllValues();
		assertEquals(BetterBlackjackingColors.WAKING, used.get(0));
		assertEquals(withAlpha(BetterBlackjackingColors.WAKING, 77), used.get(1));
	}

	@Test
	public void defaultsAreAWidthOfTwoAndAFillAlphaOfTwenty()
	{
		NPC npc = npc(new Rectangle(0, 0, 5, 5));
		when(view.stateOf(npc)).thenReturn(TargetState.KNOCK_OUT);
		when(view.eligibleTargets()).thenReturn(Collections.singletonList(npc));

		overlay.render(graphics);

		ArgumentCaptor<Stroke> strokes = ArgumentCaptor.forClass(Stroke.class);
		verify(graphics, times(2)).setStroke(strokes.capture());
		assertEquals(2f, firstLineWidth(strokes), 0f);
		verify(graphics).setColor(withAlpha(BetterBlackjackingColors.KNOCK_OUT, 20));
	}

	@Test
	public void drawsNothingWhileInactive()
	{
		when(view.isActive()).thenReturn(false);
		NPC npc = npc(new Rectangle(0, 0, 5, 5));
		when(view.eligibleTargets()).thenReturn(Collections.singletonList(npc));

		assertNull(overlay.render(graphics));

		verifyNoInteractions(graphics);
		verify(view, never()).eligibleTargets();
	}

	@Test
	public void skipsATargetWithoutAHull()
	{
		NPC hidden = npc(null);
		NPC shown = npc(new Rectangle(0, 0, 5, 5));
		when(view.stateOf(any(NPC.class))).thenReturn(TargetState.SAFE);
		when(view.eligibleTargets()).thenReturn(Arrays.asList(hidden, shown));

		assertNull(overlay.render(graphics));

		verify(graphics, times(1)).draw(any(Shape.class));
		verify(graphics, times(1)).fill(any(Shape.class));
		verify(graphics).draw(shown.getConvexHull());
	}

	@Test
	public void drawsNothingWhenNoTargetHasAHull()
	{
		NPC hidden = npc(null);
		when(view.stateOf(hidden)).thenReturn(TargetState.SAFE);
		when(view.eligibleTargets()).thenReturn(Collections.singletonList(hidden));

		assertNull(overlay.render(graphics));

		verifyNoInteractions(graphics);
	}

	@Test
	public void rendersTheOutlineAndTranslucentFillIntoAnImage()
	{
		when(config.outlineWidth()).thenReturn(1);
		when(config.fillOpacity()).thenReturn(128);
		NPC npc = npc(new Rectangle(2, 2, 10, 10));
		when(view.stateOf(npc)).thenReturn(TargetState.ATTACKING);
		when(view.eligibleTargets()).thenReturn(Collections.singletonList(npc));
		BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();

		try
		{
			assertNull(overlay.render(g));
		}
		finally
		{
			g.dispose();
		}

		assertEquals(0xFFFF0000, image.getRGB(12, 6));
		assertEquals(0x80FF0000, image.getRGB(6, 6));
		assertEquals(0, image.getRGB(16, 16));
	}

	private static NPC npc(Shape hull)
	{
		NPC npc = mock(NPC.class);
		when(npc.getConvexHull()).thenReturn(hull);
		return npc;
	}

	private static float firstLineWidth(ArgumentCaptor<Stroke> strokes)
	{
		BasicStroke stroke = (BasicStroke) strokes.getAllValues().get(0);
		return stroke.getLineWidth();
	}

	private static Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}
