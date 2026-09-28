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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.oveduumnakal.betterblackjacking.WakeAnimation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;

/**
 * Dev-only recorder, never shipped: plays each {@link WakeAnimation} option on the local player, whose
 * human skeleton the bandits and thugs share, and saves the frames for {@code scripts/make-preview-gifs.py}
 * to turn into the README GIFs.
 *
 * <p>Each clip lies unconscious, plays the option's animations back to back, then stands (see
 * {@link PreviewTimeline}). Clips go to {@code <RuneLite dir>/better-blackjacking-previews/<OPTION>/},
 * in the form {@link CapturedClip} describes. A few ticks after logging in, it records the clips that
 * aren't on disk yet; {@code ::bbpreview} records them all again. Stand still and leave the camera alone
 * while it runs: it swings the camera low and side-on first, and puts it back afterwards.
 *
 * <p>Frames are downscaled as they are captured, since full-size frames from a 4K client exhausted the
 * heap and froze the client, and they are written to disk off the client thread.
 */
@PluginDescriptor(
		name = "Better Blackjacking Preview Recorder",
		description = "Dev-only: records the wake-up animation options as frames for the README GIFs",
		developerPlugin = true
)
public class PreviewRecorderPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(PreviewRecorderPlugin.class);

	/** The chat command that records every clip again. */
	static final String COMMAND = "bbpreview";

	private static final File OUTPUT_DIR = new File(RuneLite.RUNELITE_DIR, "better-blackjacking-previews");

	private static final int LOGIN_DELAY_TICKS = 5;

	private static final int CAMERA_SETTLE_CYCLES = 75;

	private static final int SIDE_ON_PITCH = 140;

	private static final int SIDE_ON_YAW_OFFSET = 512;

	private static final int YAW_MASK = 2047;

	private static final int CAPTURE_EVERY_CYCLES = 2;

	private static final int MAX_FRAME_SIZE = 640;

	private static final int UNKNOWN_ANIMATION_CYCLES = 60;

	private static final int MIN_TILE_SIZE = 30;

	private final Deque<WakeAnimation> queue = new ArrayDeque<>();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DrawManager drawManager;

	private ExecutorService writer;

	private boolean loginRecordingStarted;

	private int loggedInTicks;

	private int settleCycles;

	private boolean cameraMoved;

	private int savedPitch;

	private int savedYaw;

	private WakeAnimation clip;

	private PreviewTimeline timeline;

	private CapturedClip captured;

	private Rectangle crop;

	private int cycle;

	/** Starts the writer thread; the login recording runs once per start. */
	@Override
	protected void startUp()
	{
		writer = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "bb-preview-writer"));
		loginRecordingStarted = false;
		loggedInTicks = 0;
	}

	/** Abandons any recording in progress, restoring the camera, and lets queued writes finish. */
	@Override
	protected void shutDown()
	{
		clientThread.invoke(this::stop);
		writer.shutdown();
	}

	/**
	 * Records every clip again on {@code ::bbpreview}.
	 *
	 * @param event the command
	 */
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (COMMAND.equals(event.getCommand()))
			start(false);
	}

	/**
	 * Records the missing clips a few ticks after the first login.
	 *
	 * @param event the tick
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (loginRecordingStarted || ++loggedInTicks < LOGIN_DELAY_TICKS)
			return;

		loginRecordingStarted = start(true);
	}

	/**
	 * Waits for the camera to settle, then steps the current clip one client cycle: sets the animation
	 * when a segment starts, captures every other frame, and moves on when the clip ends.
	 *
	 * @param event the client cycle
	 */
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (settleCycles > 0)
		{
			if (--settleCycles == 0)
				nextClip();

			return;
		}

		if (clip == null)
			return;

		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			log.warn("[Preview] the local player went away; stopping");
			stop();
			return;
		}

		if (timeline.startsAt(cycle))
		{
			player.setAnimation(timeline.animationAt(cycle));
			player.setAnimationFrame(0);
		}

		if (cycle % CAPTURE_EVERY_CYCLES == 0)
			capture();

		if (++cycle >= timeline.length())
		{
			saveClip();
			nextClip();
		}
	}

	private boolean start(boolean onlyMissing)
	{
		if (settleCycles > 0 || clip != null)
		{
			log.info("[Preview] already recording");
			return true;
		}

		final Player player = client.getLocalPlayer();
		if (player == null)
			return false;

		queue.clear();
		for (WakeAnimation animation : WakeAnimation.values())
		{
			if (animation != WakeAnimation.OFF && !(onlyMissing && isOnDisk(animation)))
				queue.add(animation);
		}

		if (queue.isEmpty())
		{
			log.info("[Preview] every clip is already in {}; ::{} records them again", OUTPUT_DIR, COMMAND);
			return true;
		}

		log.info("[Preview] recording {} clips to {}; stand still and leave the camera alone", queue.size(),
				OUTPUT_DIR);
		savedPitch = client.getCameraPitchTarget();
		savedYaw = client.getCameraYawTarget();
		cameraMoved = true;
		client.setCameraPitchTarget(SIDE_ON_PITCH);
		client.setCameraYawTarget((player.getOrientation() + SIDE_ON_YAW_OFFSET) & YAW_MASK);
		settleCycles = CAMERA_SETTLE_CYCLES;
		return true;
	}

	private void nextClip()
	{
		final WakeAnimation next = queue.poll();
		final Player player = client.getLocalPlayer();
		if (next == null || player == null)
		{
			log.info("[Preview] done");
			stop();
			return;
		}

		clip = next;
		cycle = 0;
		timeline = PreviewTimeline.getUp(next.animationIds(), this::animationCycles);
		captured = new CapturedClip(labelOf(next));
		crop = cropAround(player);
	}

	private void stop()
	{
		final Player player = client.getLocalPlayer();
		if (clip != null && player != null)
			player.setAnimation(PreviewTimeline.IDLE);

		if (cameraMoved)
		{
			client.setCameraPitchTarget(savedPitch);
			client.setCameraYawTarget(savedYaw);
			cameraMoved = false;
		}

		queue.clear();
		settleCycles = 0;
		clip = null;
		timeline = null;
		captured = null;
	}

	private boolean isOnDisk(WakeAnimation animation)
	{
		return new File(clipDir(animation), CapturedClip.TIMINGS_FILE).exists();
	}

	private static File clipDir(WakeAnimation animation)
	{
		return new File(OUTPUT_DIR, animation.name());
	}

	private static String labelOf(WakeAnimation animation)
	{
		final String ids = Arrays.stream(animation.animationIds())
				.mapToObj(Integer::toString)
				.collect(Collectors.joining("+"));
		return animation + " (" + ids + ")";
	}

	private void capture()
	{
		final CapturedClip target = captured;
		final Rectangle area = crop;
		drawManager.requestNextFrameListener(image ->
		{
			if (target.isClosed())
				return;

			final BufferedImage frame = downscale(image, area);
			if (frame != null)
				target.add(frame, System.nanoTime());
		});
	}

	private void saveClip()
	{
		final CapturedClip done = captured;
		final File dir = clipDir(clip);
		final String name = clip.name();
		done.close();
		writer.execute(() ->
		{
			try
			{
				done.writeTo(dir);
				log.info("[Preview] saved {} frames of {} to {}", done.size(), name, dir);
			}
			catch (IOException e)
			{
				log.warn("[Preview] couldn't save {}", name, e);
			}
		});
	}

	private BufferedImage downscale(Image image, Rectangle area)
	{
		final int width = image.getWidth(null);
		final int height = image.getHeight(null);
		if (width <= 0 || height <= 0)
			return null;

		final double scaleX = (double) width / client.getCanvasWidth();
		final double scaleY = (double) height / client.getCanvasHeight();
		final int x = clamp((int) (area.x * scaleX), 0, width - 1);
		final int y = clamp((int) (area.y * scaleY), 0, height - 1);
		final int w = clamp((int) (area.width * scaleX), 1, width - x);
		final int h = clamp((int) (area.height * scaleY), 1, height - y);
		final double scale = Math.min(1.0, (double) MAX_FRAME_SIZE / Math.max(w, h));
		final int outWidth = Math.max(1, (int) (w * scale));
		final int outHeight = Math.max(1, (int) (h * scale));
		final BufferedImage frame = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = frame.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(image, 0, 0, outWidth, outHeight, x, y, x + w, y + h, null);
		graphics.dispose();
		return frame;
	}

	private Rectangle cropAround(Player player)
	{
		final Polygon tile = Perspective.getCanvasTilePoly(client, player.getLocalLocation());
		if (tile == null)
			return new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());

		final Rectangle bounds = tile.getBounds();
		final int size = Math.max(bounds.width, MIN_TILE_SIZE);
		final int centerX = bounds.x + bounds.width / 2;
		final int groundY = bounds.y + bounds.height / 2;
		return new Rectangle(centerX - size * 5 / 2, groundY - size * 4, size * 5, size * 5);
	}

	private int animationCycles(int animationId)
	{
		final Animation animation = client.loadAnimation(animationId);
		final int cycles = animation == null ? 0 : PreviewTimeline.cyclesOf(animation.getFrameLengths());
		if (cycles > 0)
			return cycles;

		log.warn("[Preview] no frame lengths for animation {}; assuming {} cycles", animationId,
				UNKNOWN_ANIMATION_CYCLES);
		return UNKNOWN_ANIMATION_CYCLES;
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}
}
