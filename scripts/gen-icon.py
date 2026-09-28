#!/usr/bin/env python3
"""Render icons/source/icon-source.png to the plugin's 48x48 icon.png files.

Usage, from the repository root:

    python scripts/gen-icon.py [--preview PATH]

It writes two identical files:

    icon.png                                                  (Plugin Hub icon)
    src/main/resources/com/oveduumnakal/betterblackjacking/icon.png

The source is the full-size logo artwork with a transparent background. The script crops it to
the visible artwork plus a 3% margin, centres that in a square, and downsamples it to 48x48 with
LANCZOS. --preview PATH also writes a 4x nearest-neighbour upscale on dark and light backgrounds
for checking the pixels by eye. Needs Pillow (tested with 9.5).
"""

import argparse
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, 'icons', 'source', 'icon-source.png')
OUTPUTS = [
    os.path.join(ROOT, 'icon.png'),
    os.path.join(ROOT, 'src', 'main', 'resources', 'com', 'oveduumnakal', 'betterblackjacking', 'icon.png'),
]
SIZE = 48
MARGIN = 0.03
ALPHA_THRESHOLD = 8


def render():
    """Crop the source to its artwork, square it up and downsample it to SIZE x SIZE."""
    source = Image.open(SOURCE).convert('RGBA')
    visible = source.getchannel('A').point(lambda a: 255 if a > ALPHA_THRESHOLD else 0)
    left, top, right, bottom = visible.getbbox()
    side = max(right - left, bottom - top)
    side += 2 * int(side * MARGIN)
    square = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    x = (left + right) // 2 - side // 2
    y = (top + bottom) // 2 - side // 2
    square.paste(source.crop((x, y, x + side, y + side)), (0, 0))
    return square.resize((SIZE, SIZE), Image.LANCZOS)


def preview(icon, path):
    """Write a 4x upscale of the icon on a dark and a light background, side by side."""
    big = icon.resize((SIZE * 4, SIZE * 4), Image.NEAREST)
    sheet = Image.new('RGBA', (SIZE * 8 + 16, SIZE * 4), (40, 40, 40, 255))
    sheet.alpha_composite(big, (0, 0))
    light = Image.new('RGBA', big.size, (230, 230, 230, 255))
    light.alpha_composite(big)
    sheet.paste(light, (SIZE * 4 + 16, 0))
    sheet.save(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--preview', help='also write a 4x preview PNG to this path')
    args = parser.parse_args()
    icon = render()
    for output in OUTPUTS:
        icon.save(output, optimize=True)
        print('wrote', os.path.relpath(output, ROOT))

    if args.preview:
        preview(icon, args.preview)
        print('wrote', args.preview)


if __name__ == '__main__':
    main()
