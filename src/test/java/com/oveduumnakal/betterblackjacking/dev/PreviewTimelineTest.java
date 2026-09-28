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
package com.oveduumnakal.betterblackjacking.dev;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.runelite.api.gameval.AnimationID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the preview clip timeline and the clip's on-disk form, with no client involved. */
public class PreviewTimelineTest
{
	private static final Map<Integer, Integer> CYCLES = Map.of(
			AnimationID.DREAM_CYRISUS_SIT_UP_TRANSITION, 60,
			AnimationID.DREAM_CYRISUS_STAND_UP_TRANSITION, 100);

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void cyclesOfSumsTheFrameLengths()
	{
		assertEquals(25, PreviewTimeline.cyclesOf(new int[]{4, 6, 15}));
		assertEquals(0, PreviewTimeline.cyclesOf(new int[0]));
		assertEquals(0, PreviewTimeline.cyclesOf(null));
	}

	@Test
	public void lengthIsTheSumOfTheSegments()
	{
		assertEquals(35, new PreviewTimeline(new int[]{1, 2, 3}, new int[]{10, 20, 5}).length());
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptySegmentsAreRejected()
	{
		new PreviewTimeline(new int[]{1, 2}, new int[]{10, 0});
	}

	@Test(expected = IllegalArgumentException.class)
	public void mismatchedArraysAreRejected()
	{
		new PreviewTimeline(new int[]{1, 2}, new int[]{10});
	}

	@Test
	public void twoAnimationGetUpPlaysBackToBackBetweenLyingAndStanding()
	{
		final int[] getUp = {AnimationID.DREAM_CYRISUS_SIT_UP_TRANSITION,
				AnimationID.DREAM_CYRISUS_STAND_UP_TRANSITION};
		final PreviewTimeline timeline = PreviewTimeline.getUp(getUp, CYCLES::get);
		final int lying = PreviewTimeline.LYING_CYCLES;

		assertEquals(lying + 60 + 100 + PreviewTimeline.STANDING_CYCLES, timeline.length());
		assertEquals(AnimationID.HUMAN_UNCONSCIOUS, timeline.animationAt(0));
		assertEquals(AnimationID.HUMAN_UNCONSCIOUS, timeline.animationAt(lying - 1));
		assertEquals(AnimationID.DREAM_CYRISUS_SIT_UP_TRANSITION, timeline.animationAt(lying));
		assertEquals(AnimationID.DREAM_CYRISUS_SIT_UP_TRANSITION, timeline.animationAt(lying + 59));
		assertEquals(AnimationID.DREAM_CYRISUS_STAND_UP_TRANSITION, timeline.animationAt(lying + 60));
		assertEquals(AnimationID.DREAM_CYRISUS_STAND_UP_TRANSITION, timeline.animationAt(lying + 159));
		assertEquals(PreviewTimeline.IDLE, timeline.animationAt(lying + 160));
		assertEquals(PreviewTimeline.IDLE, timeline.animationAt(timeline.length()));
		assertEquals(PreviewTimeline.IDLE, timeline.animationAt(-1));
	}

	@Test
	public void segmentsStartOnlyOnTheirFirstCycle()
	{
		final PreviewTimeline timeline = PreviewTimeline.getUp(
				new int[]{AnimationID.DREAM_CYRISUS_SIT_UP_TRANSITION}, CYCLES::get);
		final int lying = PreviewTimeline.LYING_CYCLES;

		assertTrue(timeline.startsAt(0));
		assertTrue(timeline.startsAt(lying));
		assertTrue(timeline.startsAt(lying + 60));
		assertFalse(timeline.startsAt(1));
		assertFalse(timeline.startsAt(lying + 1));
		assertFalse(timeline.startsAt(timeline.length()));
	}

	@Test
	public void writtenClipHasFramesLabelAndTimingsFromTheFirstFrame() throws IOException
	{
		final CapturedClip clip = new CapturedClip("Max get up (7122)");
		clip.add(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), 5_000_000_000L);
		clip.add(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), 5_040_000_000L);
		clip.add(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), 5_081_500_000L);
		clip.close();
		clip.add(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), 5_120_000_000L);

		final File dir = new File(folder.getRoot(), "MAX_GET_UP");
		assertTrue(dir.mkdirs());
		assertTrue(new File(dir, "frame_009.png").createNewFile());
		clip.writeTo(dir);

		final String[] files = dir.list();
		Arrays.sort(files);
		assertEquals(List.of("frame_000.png", "frame_001.png", "frame_002.png", CapturedClip.LABEL_FILE,
				CapturedClip.TIMINGS_FILE), List.of(files));
		assertEquals(List.of("0", "40", "81"), readLines(new File(dir, CapturedClip.TIMINGS_FILE)));
		assertEquals(List.of("Max get up (7122)"), readLines(new File(dir, CapturedClip.LABEL_FILE)));
	}

	private static List<String> readLines(File file) throws IOException
	{
		return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
	}
}
