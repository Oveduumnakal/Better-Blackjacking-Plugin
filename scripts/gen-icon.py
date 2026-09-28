#!/usr/bin/env python3
"""Render icons/source/icon.svg to the plugin's 48x48 icon.png files.

Usage, from the repository root:

    python scripts/gen-icon.py [--preview PATH]

It writes two identical files:

    icon.png                                                  (Plugin Hub icon)
    src/main/resources/com/oveduumnakal/betterblackjacking/icon.png

--preview PATH also writes a 4x nearest-neighbour upscale for checking the pixels by eye.

Rendering: when the `cairosvg` package works (it needs the Cairo DLL on Windows), the SVG is
rendered by Cairo. Otherwise a small built-in Pillow renderer draws it. Either way the SVG is drawn
at 8x (384x384) and downsampled to 48x48 with LANCZOS. The built-in renderer understands only the
subset icon.svg uses:

  - <g> with inherited fill / stroke / stroke-width and an optional `rotate(a cx cy)` transform
  - <rect x y width height rx>, <circle cx cy r>, <polygon points>
  - presentation attributes only (no CSS, <path>, <use>, gradients or filters)

Keep icon.svg inside that subset so both renderers agree. Needs Pillow (tested with 9.5).
"""

import argparse
import io
import math
import os
import re
import sys
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, 'icons', 'source', 'icon.svg')
OUTPUTS = [
    os.path.join(ROOT, 'icon.png'),
    os.path.join(ROOT, 'src', 'main', 'resources', 'com', 'oveduumnakal', 'betterblackjacking', 'icon.png'),
]
SIZE = 48
SCALE = 8
SVG_NS = '{http://www.w3.org/2000/svg}'


def render_with_cairo():
    """Return the SVG rendered at SIZE * SCALE by cairosvg, or None if it is unavailable."""
    try:
        import cairosvg
    except (ImportError, OSError):
        return None

    try:
        data = cairosvg.svg2png(url=SOURCE, output_width=SIZE * SCALE, output_height=SIZE * SCALE)
    except OSError:
        return None

    return Image.open(io.BytesIO(data)).convert('RGBA')


def parse_colour(value):
    """Parse #rgb / #rrggbb into an RGBA tuple; 'none' or missing gives None."""
    if value is None or value == 'none':
        return None

    value = value.lstrip('#')
    if len(value) == 3:
        value = ''.join(c * 2 for c in value)

    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def num(elem, name, default=0.0):
    """Read a numeric attribute, scaled to the supersampled canvas."""
    return float(elem.get(name, default)) * SCALE


def draw_round_rect(draw, box, radius, colour):
    """Draw a filled rounded rectangle, skipping degenerate boxes."""
    x0, y0, x1, y1 = box
    if x1 <= x0 or y1 <= y0:
        return

    radius = max(0.0, min(radius, (x1 - x0) / 2, (y1 - y0) / 2))
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=colour)


def draw_rect(draw, elem, fill, stroke, width):
    """Draw <rect>: the stroke is centred on the edge, so paint outer then inner shapes."""
    x, y = num(elem, 'x'), num(elem, 'y')
    w, h = num(elem, 'width'), num(elem, 'height')
    r = num(elem, 'rx', elem.get('ry', 0))
    half = width / 2 if stroke else 0.0
    if stroke:
        draw_round_rect(draw, (x - half, y - half, x + w + half, y + h + half), r + half, stroke)

    if fill:
        draw_round_rect(draw, (x + half, y + half, x + w - half, y + h - half), max(0.0, r - half), fill)


def draw_circle(draw, elem, fill, stroke, width):
    """Draw <circle> with a centred stroke."""
    cx, cy, r = num(elem, 'cx'), num(elem, 'cy'), num(elem, 'r')
    half = width / 2 if stroke else 0.0
    if stroke:
        draw.ellipse([cx - r - half, cy - r - half, cx + r + half, cy + r + half], fill=stroke)

    if fill and r - half > 0:
        draw.ellipse([cx - r + half, cy - r + half, cx + r - half, cy + r - half], fill=fill)


def draw_polygon(draw, elem, fill, stroke, width):
    """Draw <polygon> with a centred, round-joined stroke."""
    values = [float(v) * SCALE for v in re.split(r'[\s,]+', elem.get('points').strip())]
    points = list(zip(values[0::2], values[1::2]))
    if fill:
        draw.polygon(points, fill=fill)

    if stroke:
        draw.line(points + [points[0]], fill=stroke, width=int(round(width)), joint='curve')
        half = width / 2
        for px, py in points:
            draw.ellipse([px - half, py - half, px + half, py + half], fill=stroke)


SHAPES = {'rect': draw_rect, 'circle': draw_circle, 'polygon': draw_polygon}


def rotate_layer(layer, transform):
    """Apply an SVG rotate(a cx cy) transform to a layer (SVG angles are clockwise)."""
    match = re.fullmatch(r'\s*rotate\(\s*([-\d.]+)(?:[\s,]+([-\d.]+)[\s,]+([-\d.]+))?\s*\)\s*', transform)
    if not match:
        sys.exit('gen-icon: unsupported transform %r' % transform)

    angle = float(match.group(1))
    cx = float(match.group(2) or 0) * SCALE
    cy = float(match.group(3) or 0) * SCALE
    premultiplied = layer.convert('RGBa')
    rotated = premultiplied.rotate(-angle, resample=Image.BICUBIC, center=(cx, cy))
    return rotated.convert('RGBA')


def render_group(elem, inherited):
    """Render a group's children onto a new layer, then apply the group's transform."""
    style = dict(inherited)
    for key in ('fill', 'stroke', 'stroke-width'):
        if key in elem.attrib:
            style[key] = elem.get(key)

    layer = Image.new('RGBA', (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for child in elem:
        tag = child.tag.replace(SVG_NS, '')
        if tag == 'g':
            sub = render_group(child, style)
            layer.alpha_composite(sub)
            draw = ImageDraw.Draw(layer)
            continue

        if tag not in SHAPES:
            continue

        fill = parse_colour(child.get('fill', style.get('fill', '#000')))
        stroke = parse_colour(child.get('stroke', style.get('stroke')))
        width = float(child.get('stroke-width', style.get('stroke-width', 1))) * SCALE
        SHAPES[tag](draw, child, fill, stroke, width)

    if 'transform' in elem.attrib:
        layer = rotate_layer(layer, elem.get('transform'))

    return layer


def render_with_pillow():
    """Render the SVG at SIZE * SCALE with the built-in subset renderer."""
    root = ET.parse(SOURCE).getroot()
    return render_group(root, {})


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--preview', help='also write a 4x nearest-neighbour upscale here')
    args = parser.parse_args()

    big = render_with_cairo()
    renderer = 'cairosvg'
    if big is None:
        big = render_with_pillow()
        renderer = 'built-in Pillow renderer'

    icon = big.resize((SIZE, SIZE), Image.LANCZOS)
    for path in OUTPUTS:
        icon.save(path, optimize=True)
        print('gen-icon: wrote %s (%s)' % (os.path.relpath(path, ROOT), renderer))

    if args.preview:
        icon.resize((SIZE * 4, SIZE * 4), Image.NEAREST).save(args.preview)
        print('gen-icon: wrote preview %s' % args.preview)


if __name__ == '__main__':
    main()
