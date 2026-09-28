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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * The frames of one preview clip and the time each was drawn, collected on the client thread and
 * then written to disk on a writer thread.
 *
 * <p>A clip folder holds {@code frame_000.png} onwards, {@link #LABEL_FILE} with the caption for the
 * GIF, and {@link #TIMINGS_FILE} with each frame's time in milliseconds since the first frame, one per
 * line. The timings file is written last, so a folder that has one is complete.
 */
final class CapturedClip
{
	/** The file that marks a complete clip and holds each frame's time since the first, in milliseconds. */
	static final String TIMINGS_FILE = "timings.txt";

	/** The file that holds the clip's caption. */
	static final String LABEL_FILE = "label.txt";

	private final String label;

	private final List<BufferedImage> frames = new ArrayList<>();

	private final List<Long> frameNanos = new ArrayList<>();

	private boolean closed;

	/**
	 * Creates an empty clip.
	 *
	 * @param label the caption for the clip's GIF
	 */
	CapturedClip(String label)
	{
		this.label = label;
	}

	/**
	 * Adds a frame, unless the clip is closed. A frame requested before the clip ended can arrive
	 * after it; it is dropped rather than added to a list the writer may be reading.
	 *
	 * @param frame the frame
	 * @param nanos when the frame was drawn, from {@link System#nanoTime()}
	 */
	void add(BufferedImage frame, long nanos)
	{
		if (closed)
			return;

		frames.add(frame);
		frameNanos.add(nanos);
	}

	/** Stops the clip taking frames, before it is handed to the writer. */
	void close()
	{
		closed = true;
	}

	/**
	 * Returns whether the clip has stopped taking frames.
	 *
	 * @return {@code true} once {@link #close()} has been called
	 */
	boolean isClosed()
	{
		return closed;
	}

	/**
	 * Returns how many frames the clip holds.
	 *
	 * @return the frame count
	 */
	int size()
	{
		return frames.size();
	}

	/**
	 * Returns each frame's time since the first frame.
	 *
	 * @return the times in milliseconds, starting at 0
	 */
	List<Long> timingsMillis()
	{
		final List<Long> timings = new ArrayList<>(frameNanos.size());
		for (long nanos : frameNanos)
			timings.add(TimeUnit.NANOSECONDS.toMillis(nanos - frameNanos.get(0)));

		return timings;
	}

	/**
	 * Replaces the contents of a clip folder with this clip's frames, caption and timings.
	 *
	 * @param dir the clip folder, which is created if needed
	 * @throws IOException if a file can't be deleted or written
	 */
	void writeTo(File dir) throws IOException
	{
		Files.createDirectories(dir.toPath());
		final File[] old = dir.listFiles();
		if (old != null)
		{
			for (File file : old)
				Files.delete(file.toPath());
		}

		for (int i = 0; i < frames.size(); i++)
			ImageIO.write(frames.get(i), "png", new File(dir, String.format("frame_%03d.png", i)));

		Files.write(new File(dir, LABEL_FILE).toPath(), List.of(label), StandardCharsets.UTF_8);
		final List<String> lines = new ArrayList<>(frames.size());
		for (long millis : timingsMillis())
			lines.add(Long.toString(millis));

		Files.write(new File(dir, TIMINGS_FILE).toPath(), lines, StandardCharsets.UTF_8);
	}
}
