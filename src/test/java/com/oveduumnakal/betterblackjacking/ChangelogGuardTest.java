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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The changelog build guard: run as part of {@code ./gradlew build} (alongside the
 * style check), it fails the build — locally and in CI — when the bundled changelog
 * has no entry for the current {@code runelite-plugin.properties} version, or when
 * the changelog is malformed. Since bumping that version is the first step of every
 * release, the next build forces the new release's changelog entry to exist.
 */
public class ChangelogGuardTest
{
	private static final Pattern DATE = Pattern.compile("[A-Z][a-z]+ \\d{1,2} \\d{4}");

	private String pluginVersion() throws IOException
	{
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream("runelite-plugin.properties"))
		{
			props.load(in);
		}

		return props.getProperty("version");
	}

	@Test
	public void currentVersionHasATopChangelogEntry() throws IOException
	{
		String version = pluginVersion();
		assertNotNull("runelite-plugin.properties is missing a version", version);

		Changelog log = Changelog.load();
		assertFalse("changelog.md has no releases", log.releases().isEmpty());
		assertTrue("changelog.md has no entry for version " + version, log.hasVersion(version));
		assertEquals("the newest changelog entry must be the current version " + version,
				version, log.currentVersion());
	}

	@Test
	public void changelogIsWellFormed()
	{
		Changelog log = Changelog.load();
		Set<String> versions = new HashSet<>();

		for (Changelog.Release release : log.releases())
		{
			assertNotNull("a release is missing its version", release.getVersion());
			assertTrue("duplicate release version: " + release.getVersion(), versions.add(release.getVersion()));
			assertNotNull("release " + release.getVersion() + " is missing its date", release.getDate());
			assertTrue("release " + release.getVersion() + " has a malformed date: " + release.getDate(),
					DATE.matcher(release.getDate()).matches());

			assertFalse("release " + release.getVersion() + " has an empty body", release.getBody().isEmpty());
		}
	}
}
