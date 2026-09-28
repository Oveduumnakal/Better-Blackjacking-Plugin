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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DebugLog}: it is silent and does no formatting while debug logging is off, and its lines
 * match the prototype's fixture lines (both sessions) exactly once the tag is removed.
 */
public class DebugLogTest
{
	private static final String[] FIXTURES = {
		"planning/fixtures/session1-bandit.log",
		"planning/fixtures/session2-bandit.log",
	};
	private static final String BANDIT = DebugLog.describe("Bandit", 28415, 737);
	private static final String OTHER_BANDIT = DebugLog.describe("Bandit", 28420, 737);

	private final BetterBlackjackingConfig config = mock(BetterBlackjackingConfig.class);
	private final List<String> lines = new ArrayList<>();
	private int tick;
	private int tickReads;
	private DebugLog debugLog;

	/**
	 * Builds a debug log over a mocked config, a settable tick and a sink that collects lines.
	 */
	@Before
	public void setUp()
	{
		when(config.debugLogging()).thenReturn(true);
		debugLog = new DebugLog(config, () ->
		{
			tickReads++;
			return tick;
		}, lines::add);
	}

	@Test
	public void offFormatsAndLogsNothing()
	{
		when(config.debugLogging()).thenReturn(false);
		final CountingArg arg = new CountingArg();

		debugLog.log("value %s", arg);
		debugLog.chat(arg, true, "msg");
		debugLog.overhead(BANDIT, "Zzzzzz");
		debugLog.animation(DebugLog.PLAYER, 401, 808);
		debugLog.hitsplat(DebugLog.PLAYER, 4);
		debugLog.interacting(DebugLog.PLAYER, BANDIT);
		debugLog.knockout(BANDIT, BANDIT, 838, 808, 4, 296);
		debugLog.despawn(BANDIT);

		assertFalse(debugLog.isEnabled());
		assertTrue(lines.isEmpty());
		assertEquals(0, arg.formatted);
		assertEquals(0, tickReads);
	}

	@Test
	public void offUsesTheDefaultLoggerWithoutError()
	{
		when(config.debugLogging()).thenReturn(false);
		final CountingArg arg = new CountingArg();

		new DebugLog(config, () -> 1).log("value %s", arg);

		assertEquals(0, arg.formatted);
	}

	@Test
	public void onFormatsOncePerLine()
	{
		final CountingArg arg = new CountingArg();

		debugLog.log("value %s", arg);

		assertEquals(1, arg.formatted);
		assertEquals(1, tickReads);
		assertEquals(DebugLog.TAG + " tick=0 ko+- value arg", lines.get(0));
	}

	@Test
	public void koOffsetIsDashBeforeAnyKnockout()
	{
		tick = 37;
		debugLog.chat("SPAM", false, "Your blow only glances off the bandit's head.");

		assertEquals(DebugLog.TAG + " tick=37 ko+- chat type=SPAM matched=false"
				+ " msg=\"Your blow only glances off the bandit's head.\"", lines.get(0));
	}

	@Test
	public void koOffsetCountsFromTheKnockoutAndClears()
	{
		debugLog.setKnockoutTick(292);
		tick = 297;
		debugLog.log("tick");
		debugLog.clearKnockout();
		debugLog.log("tick");

		assertEquals(DebugLog.TAG + " tick=297 ko+5 tick", lines.get(0));
		assertEquals(DebugLog.TAG + " tick=297 ko+- tick", lines.get(1));
	}

	@Test
	public void linesMatchTheFixture() throws IOException
	{
		tick = 33;
		debugLog.interacting(DebugLog.PLAYER, DebugLog.NONE);
		tick = 37;
		debugLog.animation(DebugLog.PLAYER, 401, 824);
		tick = 38;
		debugLog.hitsplat(DebugLog.PLAYER, 4);
		debugLog.interacting(OTHER_BANDIT, DebugLog.PLAYER);
		debugLog.animation(OTHER_BANDIT, 395, 819);
		tick = 292;
		debugLog.chat("SPAM", true, "You smack the bandit over the head and render them unconscious.");
		debugLog.setKnockoutTick(292);
		debugLog.knockout(BANDIT, BANDIT, 838, 808, 4, 296);
		debugLog.overhead(BANDIT, "Zzzzzz");
		tick = 296;
		debugLog.log("countdown expired, plugin now shows %s as awake", BANDIT);
		tick = 297;
		debugLog.overhead(BANDIT, "Arghh my head.");
		debugLog.setKnockoutTick(117);
		tick = 152;
		debugLog.despawn(BANDIT);

		final List<String> expected = List.of(
				"tick=33 ko+- interacting player -> none",
				"tick=37 ko+- animation player anim=401 pose=824",
				"tick=38 ko+- hitsplat player amount=4",
				"tick=38 ko+- interacting Bandit#28420(id=737) -> player",
				"tick=38 ko+- animation Bandit#28420(id=737) anim=395 pose=819",
				"tick=292 ko+- chat type=SPAM matched=true"
						+ " msg=\"You smack the bandit over the head and render them unconscious.\"",
				"tick=292 ko+0 KNOCKOUT target=Bandit#28415(id=737) (interacting=Bandit#28415(id=737))"
						+ " anim=838 pose=808 duration=4 wakeTick=296",
				"tick=292 ko+0 overhead Bandit#28415(id=737) text=\"Zzzzzz\"",
				"tick=296 ko+4 countdown expired, plugin now shows Bandit#28415(id=737) as awake",
				"tick=297 ko+5 overhead Bandit#28415(id=737) text=\"Arghh my head.\"",
				"tick=152 ko+35 despawn Bandit#28415(id=737)");
		final Set<String> fixture = new HashSet<>();
		for (String path : FIXTURES)
			fixture.addAll(Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8));

		assertEquals(expected.size(), lines.size());
		for (int i = 0; i < expected.size(); i++)
		{
			assertEquals(DebugLog.TAG + " " + expected.get(i), lines.get(i));
			assertTrue("not in fixture: " + expected.get(i), fixture.contains(expected.get(i)));
		}
	}

	@Test
	public void describeMatchesTheFixtureShape()
	{
		assertEquals("Bandit#28415(id=737)", BANDIT);
	}

	/**
	 * A format argument that counts how many times it has been turned into text.
	 */
	private static final class CountingArg
	{
		private int formatted;

		@Override
		public String toString()
		{
			formatted++;
			return "arg";
		}
	}
}
