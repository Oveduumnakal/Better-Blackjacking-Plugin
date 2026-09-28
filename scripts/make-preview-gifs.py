#!/usr/bin/env python3
"""Turns the preview recorder's frames into labelled GIFs.

The dev-only PreviewRecorderPlugin (src/test/.../dev) writes one folder per
wake-up animation option under <RuneLite dir>/better-blackjacking-previews/,
holding frame_000.png onwards, label.txt (the caption) and timings.txt (each
frame's time in milliseconds since the first frame). This script crops each
frame around its centre, scales it to a square, draws the caption on a bar
across the top, and saves <OPTION>.gif with per-frame durations taken from the
timings, so the GIF plays at the speed the client drew it.

Needs Pillow (pip install Pillow).

Usage:
  python scripts/make-preview-gifs.py OUTPUT_DIR [--input DIR] [--crop 0.45] [--size 360]
"""

import argparse
import os
import sys

from PIL import Image, ImageDraw

DEFAULT_INPUT = os.path.join(os.path.expanduser('~'), '.runelite', 'better-blackjacking-previews')
TIMINGS_FILE = 'timings.txt'
LABEL_FILE = 'label.txt'
BAR_HEIGHT = 20
BAR_COLOUR = (0, 0, 0)
TEXT_COLOUR = (255, 255, 0)
GIF_TICK_MS = 10
MIN_FRAME_MS = 20


def read_lines(path):
    with open(path, encoding='utf-8') as file:
        return [line.strip() for line in file if line.strip()]


def durations(timings):
    """Per-frame durations in ms, rounded to the GIF's 10 ms unit without drifting.

    Each frame is shown until the next one's (rounded) timestamp, so rounding
    errors don't add up over the clip. The last frame gets the previous one's
    duration, as its own end time isn't recorded.
    """
    ticks = [round(t / GIF_TICK_MS) * GIF_TICK_MS for t in timings]
    result = [max(MIN_FRAME_MS, b - a) for a, b in zip(ticks, ticks[1:])]
    result.append(result[-1] if result else MIN_FRAME_MS)
    return result


def crop_centre(image, fraction):
    width, height = image.size
    side = int(min(width, height) * fraction)
    left = (width - side) // 2
    top = (height - side) // 2
    return image.crop((left, top, left + side, top + side))


def make_gif(clip_dir, out_path, crop, size):
    timings = [int(t) for t in read_lines(os.path.join(clip_dir, TIMINGS_FILE))]
    frame_files = sorted(f for f in os.listdir(clip_dir) if f.startswith('frame_') and f.endswith('.png'))
    count = min(len(frame_files), len(timings))
    if count == 0:
        return None

    label_path = os.path.join(clip_dir, LABEL_FILE)
    label = read_lines(label_path)[0] if os.path.exists(label_path) else os.path.basename(clip_dir)
    frames = []
    for name in frame_files[:count]:
        with Image.open(os.path.join(clip_dir, name)) as source:
            frame = crop_centre(source.convert('RGB'), crop).resize((size, size), Image.LANCZOS)

        draw = ImageDraw.Draw(frame)
        draw.rectangle([0, 0, frame.width, BAR_HEIGHT], fill=BAR_COLOUR)
        draw.text((6, 4), label, fill=TEXT_COLOUR)
        frames.append(frame)

    frame_durations = durations(timings[:count])
    frames[0].save(out_path, save_all=True, append_images=frames[1:], duration=frame_durations, loop=0,
                   optimize=True)
    return count, sum(frame_durations)


def main():
    parser = argparse.ArgumentParser(description='Assemble the preview recorder frames into labelled GIFs.')
    parser.add_argument('output', help='folder to write the GIFs to')
    parser.add_argument('--input', default=DEFAULT_INPUT, help='the recorder output folder (default: %(default)s)')
    parser.add_argument('--crop', type=float, default=0.45,
                        help='side of the centred square crop, as a fraction of the frame (default: %(default)s)')
    parser.add_argument('--size', type=int, default=360, help='GIF side in pixels (default: %(default)s)')
    args = parser.parse_args()

    if not 0 < args.crop <= 1:
        parser.error('--crop must be in (0, 1]')

    if not os.path.isdir(args.input):
        sys.exit(f'No recorder output at {args.input}; run the client and let the recorder finish first.')

    os.makedirs(args.output, exist_ok=True)
    made = 0
    for name in sorted(os.listdir(args.input)):
        clip_dir = os.path.join(args.input, name)
        if not os.path.isfile(os.path.join(clip_dir, TIMINGS_FILE)):
            print(f'skipping {name}: no {TIMINGS_FILE}, so the clip is incomplete')
            continue

        out_path = os.path.join(args.output, name + '.gif')
        result = make_gif(clip_dir, out_path, args.crop, args.size)
        if result is None:
            print(f'skipping {name}: no frames')
            continue

        count, total_ms = result
        print(f'{out_path}: {count} frames, {total_ms} ms, {os.path.getsize(out_path) // 1024} KB')
        made += 1

    print(f'made {made} GIFs in {args.output}')


if __name__ == '__main__':
    main()
