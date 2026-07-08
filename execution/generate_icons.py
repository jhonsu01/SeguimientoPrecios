#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generador de iconos para SeguimientoPrecios.

Produce, a partir de un unico diseno vectorial-programatico:
  - Android: iconos legacy (cuadrado + redondo) y foreground adaptativo
             en las 5 densidades (mdpi..xxxhdpi).
  - Windows: icon.ico multi-tamano (16..256) para electron-builder.
  - Docs:    icon-512.png de referencia.

Diseno: fondo con degradado teal->indigo, motivo de barras ascendentes
(precio en el tiempo) con una linea de tendencia esmeralda y flecha al alza.
Es 100% deterministico (mismo output cada corrida).
"""
import os
from PIL import Image, ImageDraw

SS = 4  # supersampling para bordes suaves

# --- Paleta ---
BG_TL = (13, 148, 136)    # teal-600  (#0D9488)
BG_BR = (79, 70, 229)     # indigo-600 (#4F46E5)
BAR = (245, 247, 250, 240)
BAR_EDGE = (255, 255, 255, 255)
TREND = (52, 211, 153)    # emerald-400 (#34D399)
ADAPTIVE_BG = "#0F172A"   # slate-900 para la capa de fondo adaptativa

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "android-app", "app", "src", "main", "res")
WIN_BUILD = os.path.join(ROOT, "windows-app", "build")
WIN_ASSETS = os.path.join(ROOT, "windows-app", "assets")
DOCS = os.path.join(ROOT, "docs")


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def gradient(size):
    """Degradado diagonal renderizado pequeno y escalado (rapido y suave)."""
    g = Image.new("RGB", (64, 64))
    px = g.load()
    for y in range(64):
        for x in range(64):
            t = (x + y) / 126.0
            px[x, y] = lerp(BG_TL, BG_BR, t)
    return g.resize((size, size), Image.BILINEAR).convert("RGBA")


def shape_mask(size, shape):
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    if shape == "circle":
        d.ellipse([0, 0, size - 1, size - 1], fill=255)
    elif shape == "squircle":
        r = int(size * 0.235)
        d.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=255)
    else:
        d.rectangle([0, 0, size - 1, size - 1], fill=255)
    return m


def draw_motif(size, span):
    """Dibuja barras ascendentes + linea de tendencia sobre capa transparente.
    span = fraccion del lienzo que ocupa el motivo (0..1)."""
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = size / 2.0, size / 2.0
    w = size * span

    base_y = cy + 0.30 * w          # linea base de las barras
    bar_w = 0.135 * w
    gap = 0.058 * w
    heights = [0.34, 0.52, 0.72, 0.96]
    n = len(heights)
    total = n * bar_w + (n - 1) * gap
    x0 = cx - total / 2.0
    rad = bar_w * 0.32

    tops = []
    for i, h in enumerate(heights):
        left = x0 + i * (bar_w + gap)
        top = base_y - h * w
        d.rounded_rectangle([left, top, left + bar_w, base_y],
                            radius=rad, fill=BAR)
        tops.append((left + bar_w / 2.0, top))

    # linea de tendencia (por encima de las cimas de barra)
    pts = [(x, y - 0.10 * w) for (x, y) in tops]
    lw = max(2, int(0.045 * w))
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=TREND, width=lw)
    r = 0.045 * w
    for (x, y) in pts:
        d.ellipse([x - r, y - r, x + r, y + r], fill=TREND)

    # flecha al alza al final
    ex, ey = pts[-1]
    ax, ay = ex + 0.16 * w, ey - 0.20 * w
    d.line([(ex, ey), (ax, ay)], fill=TREND, width=lw)
    ah = 0.11 * w
    d.polygon([(ax + 0.02 * w, ay - 0.02 * w),
               (ax - ah * 0.9, ay),
               (ax, ay + ah * 0.9)], fill=TREND)
    return layer


def render(size, shape="squircle", with_bg=True, span=0.62):
    S = size * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    if with_bg:
        bg = gradient(S)
        mask = shape_mask(S, shape)
        img = Image.composite(bg, img, mask)
    motif = draw_motif(S, span)
    img = Image.alpha_composite(img, motif)
    return img.resize((size, size), Image.LANCZOS)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("  ->", os.path.relpath(path, ROOT))


def main():
    # ---- Android ----
    dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    print("Android mipmaps:")
    for name, f in dens.items():
        legacy = int(48 * f)
        fg = int(108 * f)
        d = os.path.join(RES, f"mipmap-{name}")
        save(render(legacy, "squircle", True, 0.62),
             os.path.join(d, "ic_launcher.png"))
        save(render(legacy, "circle", True, 0.60),
             os.path.join(d, "ic_launcher_round.png"))
        # foreground adaptativo: transparente, motivo dentro de la zona segura
        save(render(fg, "none", False, 0.42),
             os.path.join(d, "ic_launcher_foreground.png"))

    # ---- Windows (.ico multi-tamano) ----
    print("Windows icon.ico:")
    src = render(256, "squircle", True, 0.62)
    ico_sizes = [(16, 16), (24, 24), (32, 32), (48, 48),
                 (64, 64), (128, 128), (256, 256)]
    os.makedirs(WIN_BUILD, exist_ok=True)
    ico_path = os.path.join(WIN_BUILD, "icon.ico")
    src.save(ico_path, sizes=ico_sizes)
    print("  ->", os.path.relpath(ico_path, ROOT))
    save(render(512, "squircle", True, 0.62),
         os.path.join(WIN_ASSETS, "icon.png"))

    # ---- Docs ----
    save(render(512, "squircle", True, 0.62), os.path.join(DOCS, "icon-512.png"))
    print("Listo.")


if __name__ == "__main__":
    main()
