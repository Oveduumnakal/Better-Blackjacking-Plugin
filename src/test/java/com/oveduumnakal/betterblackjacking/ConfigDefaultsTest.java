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
import java.lang.reflect.Method;

import org.junit.Test;
import org.mockito.Answers;

import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Pins every setting in {@link BetterBlackjackingConfig} to the plan's config table: its default, its
 * persisted key, its section and position, and its range and units.
 *
 * <p>The config is a Mockito mock that calls the interface's real default methods, so each accessor
 * returns exactly the default a fresh profile would get. RuneLite saves defaults into profiles, so a
 * failure here on a shipped key means the key must be renamed rather than the default changed.
 */
public class ConfigDefaultsTest
{
	private final BetterBlackjackingConfig config = mock(BetterBlackjackingConfig.class, Answers.CALLS_REAL_METHODS);

	@Test
	public void outlineDefaults()
	{
		assertEquals(2, config.outlineWidth());
		assertEquals(20, config.fillOpacity());
	}

	@Test
	public void colourDefaults()
	{
		assertEquals(new Color(0x00FF00), config.safeColor());
		assertEquals(new Color(0xFFFF00), config.wakingColor());
		assertEquals(new Color(0xFFA500), config.knockOutColor());
		assertEquals(new Color(0xFF0000), config.attackingColor());
	}

	@Test
	public void timerPickpocketAndAnimationDefaults()
	{
		assertEquals(TimerStyle.PIE, config.timerStyle());
		assertTrue(config.pickpocketPips());
		assertEquals(WakeAnimation.OFF, config.wakeAnimation());
	}

	@Test
	public void advancedDefaults()
	{
		assertEquals(5, config.knockOutTicks());
		assertFalse(config.debugLogging());
	}

	@Test
	public void keysSectionsAndPositionsMatchThePlan() throws NoSuchMethodException
	{
		assertEquals("betterblackjacking", BetterBlackjackingConfig.GROUP);

		assertItem("outlineWidth", "Outline width", BetterBlackjackingConfig.outlineSection, 0);
		assertItem("fillOpacity", "Fill opacity", BetterBlackjackingConfig.outlineSection, 1);
		assertItem("safeColor", "Safe to pickpocket", BetterBlackjackingConfig.coloursSection, 0);
		assertItem("wakingColor", "Waking up", BetterBlackjackingConfig.coloursSection, 1);
		assertItem("knockOutColor", "Knock out", BetterBlackjackingConfig.coloursSection, 2);
		assertItem("attackingColor", "Attacking", BetterBlackjackingConfig.coloursSection, 3);
		assertItem("timerStyle", "Timer style", BetterBlackjackingConfig.timerSection, 0);
		assertItem("pickpocketPips", "Show pickpocket pips", BetterBlackjackingConfig.pickpocketsSection, 0);
		assertItem("wakeAnimation", "Wake-up animation", BetterBlackjackingConfig.animationSection, 0);
		assertItem("knockOutTicks", "Knock-out duration", BetterBlackjackingConfig.advancedSection, 0);
		assertItem("debugLogging", "Debug logging", BetterBlackjackingConfig.advancedSection, 1);
	}

	@Test
	public void rangesAndUnitsMatchThePlan() throws NoSuchMethodException
	{
		assertRange("outlineWidth", 1, 6);
		assertRange("fillOpacity", 0, 255);
		assertRange("knockOutTicks", 3, 8);

		Method knockOutTicks = BetterBlackjackingConfig.class.getMethod("knockOutTicks");
		Units units = knockOutTicks.getAnnotation(Units.class);
		assertNotNull("knockOutTicks has no @Units", units);
		assertEquals(Units.TICKS, units.value());
	}

	@Test
	public void sectionsAreInOrderAndOnlyAdvancedStartsClosed() throws NoSuchFieldException
	{
		assertSection("outlineSection", "Outline", 0, false);
		assertSection("coloursSection", "Colours", 1, false);
		assertSection("timerSection", "Timer", 2, false);
		assertSection("pickpocketsSection", "Pickpockets", 3, false);
		assertSection("animationSection", "Animation", 4, false);
		assertSection("advancedSection", "Advanced", 5, true);
	}

	private static void assertItem(String key, String name, String section, int position)
			throws NoSuchMethodException
	{
		Method method = BetterBlackjackingConfig.class.getMethod(key);
		ConfigItem item = method.getAnnotation(ConfigItem.class);
		assertNotNull(key + " has no @ConfigItem", item);
		assertEquals(key, item.keyName());
		assertEquals(key + " name", name, item.name());
		assertEquals(key + " section", section, item.section());
		assertEquals(key + " position", position, item.position());
		assertFalse(key + " has no description", item.description().isEmpty());
	}

	private static void assertRange(String key, int min, int max) throws NoSuchMethodException
	{
		Method method = BetterBlackjackingConfig.class.getMethod(key);
		Range range = method.getAnnotation(Range.class);
		assertNotNull(key + " has no @Range", range);
		assertEquals(key + " min", min, range.min());
		assertEquals(key + " max", max, range.max());
	}

	private static void assertSection(String field, String name, int position, boolean closed)
			throws NoSuchFieldException
	{
		ConfigSection section = BetterBlackjackingConfig.class.getField(field).getAnnotation(ConfigSection.class);
		assertNotNull(field + " has no @ConfigSection", section);
		assertEquals(field + " name", name, section.name());
		assertEquals(field + " position", position, section.position());
		assertEquals(field + " closedByDefault", closed, section.closedByDefault());
	}
}
