#!/usr/bin/env python3
"""Generate royalty-free placeholder loops and launcher artwork."""

from __future__ import annotations

import math
import os
import struct
import wave
import zlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "app/src/main/res/raw"
PLAY = ROOT / "play"
MIPMAPS = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
ADAPTIVE = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

SR = 22050
RNG = np.random.default_rng(18)


def write_wav(path: Path, samples: np.ndarray) -> None:
    samples = np.clip(samples, -1.0, 1.0)
    pcm = (samples * 32767.0).astype(np.int16)
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "w") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SR)
        wf.writeframes(pcm.tobytes())


def fade_loop(x: np.ndarray, ms: float = 40.0) -> np.ndarray:
    n = max(1, int(SR * ms / 1000.0))
    ramp = np.linspace(0.0, 1.0, n, dtype=np.float64)
    y = x.astype(np.float64).copy()
    y[:n] *= ramp
    y[-n:] *= ramp[::-1]
    peak = np.max(np.abs(y)) or 1.0
    return (y / peak) * 0.72


def brown(n: int) -> np.ndarray:
    x = np.cumsum(RNG.normal(0, 1, n))
    x -= x.mean()
    return x / (np.max(np.abs(x)) or 1.0)


def pink(n: int) -> np.ndarray:
    white = RNG.normal(0, 1, n)
    b = np.array([0.049922035, -0.095993537, 0.050612699, -0.004408786])
    a = np.array([1.0, -2.494956002, 2.017265875, -0.522189400])
    # simple IIR via lfilter-equivalent
    y = np.zeros(n)
    x1 = x2 = x3 = y1 = y2 = y3 = 0.0
    for i, w in enumerate(white):
        y[i] = b[0] * w + b[1] * x1 + b[2] * x2 + b[3] * x3 - a[1] * y1 - a[2] * y2 - a[3] * y3
        x3, x2, x1 = x2, x1, w
        y3, y2, y1 = y2, y1, y[i]
    y -= y.mean()
    return y / (np.max(np.abs(y)) or 1.0)


def lowpass(x: np.ndarray, cutoff: float) -> np.ndarray:
    rc = 1.0 / (2 * math.pi * cutoff)
    dt = 1.0 / SR
    a = dt / (rc + dt)
    y = np.zeros_like(x)
    acc = 0.0
    for i, s in enumerate(x):
        acc += a * (s - acc)
        y[i] = acc
    return y


def highpass(x: np.ndarray, cutoff: float) -> np.ndarray:
    rc = 1.0 / (2 * math.pi * cutoff)
    dt = 1.0 / SR
    a = rc / (rc + dt)
    y = np.zeros_like(x)
    prev_x = x[0]
    prev_y = 0.0
    for i, s in enumerate(x):
        prev_y = a * (prev_y + s - prev_x)
        prev_x = s
        y[i] = prev_y
    return y


def bandpass(x: np.ndarray, lo: float, hi: float) -> np.ndarray:
    return lowpass(highpass(x, lo), hi)


def env_sine(n: int, hz: float, depth: float, base: float = 1.0) -> np.ndarray:
    t = np.arange(n) / SR
    return base + depth * np.sin(2 * math.pi * hz * t)


def ocean(n: int) -> np.ndarray:
    swell = env_sine(n, 0.18, 0.45, 0.55)
    water = lowpass(brown(n), 480) * swell
    foam = highpass(pink(n), 900) * 0.12 * swell
    return fade_loop(water * 0.9 + foam)


def ocean_surf(n: int) -> np.ndarray:
    t = np.arange(n) / SR
    crash = (np.sin(2 * math.pi * 0.28 * t) ** 8)
    body = lowpass(brown(n), 380) * (0.35 + 0.65 * crash)
    spray = highpass(RNG.normal(0, 1, n), 1800) * crash * 0.18
    return fade_loop(body + spray)


def rain(n: int) -> np.ndarray:
    hiss = highpass(RNG.normal(0, 1, n), 1200) * 0.22
    drops = np.zeros(n)
    for _ in range(n // 40):
        i = int(RNG.integers(0, n - 80))
        length = int(RNG.integers(20, 70))
        burst = RNG.normal(0, 1, length) * RNG.uniform(0.15, 0.55)
        burst *= np.hanning(length)
        drops[i : i + length] += burst
    drops = highpass(drops, 800)
    return fade_loop(hiss + drops * 0.55)


def rain_window(n: int) -> np.ndarray:
    pane = lowpass(pink(n), 900) * 0.35
    ticks = np.zeros(n)
    for _ in range(n // 90):
        i = int(RNG.integers(0, n - 30))
        length = int(RNG.integers(8, 24))
        burst = RNG.normal(0, 1, length) * RNG.uniform(0.3, 0.8)
        burst *= np.hanning(length)
        ticks[i : i + length] += burst
    return fade_loop(pane + highpass(ticks, 1500) * 0.35)


def forest(n: int) -> np.ndarray:
    leaves = bandpass(pink(n), 200, 1800) * 0.45
    t = np.arange(n) / SR
    rustle = leaves * (0.7 + 0.3 * np.sin(2 * math.pi * 0.11 * t))
    return fade_loop(rustle)


def forest_stream(n: int) -> np.ndarray:
    water = highpass(lowpass(RNG.normal(0, 1, n), 2400), 300)
    gurgle = lowpass(brown(n), 220) * 0.25
    return fade_loop(water * 0.28 + gurgle)


def storm(n: int) -> np.ndarray:
    rain_layer = rain(n) * 0.55
    thunder = np.zeros(n)
    for start_s in (1.2, 4.8):
        i = int(start_s * SR)
        length = int(1.6 * SR)
        if i + length > n:
            continue
        rumble = lowpass(brown(length), 90)
        decay = np.exp(-np.linspace(0, 4.5, length))
        thunder[i : i + length] += rumble * decay * 1.3
    return fade_loop(rain_layer + thunder)


def storm_wind(n: int) -> np.ndarray:
    t = np.arange(n) / SR
    gust = 0.5 + 0.5 * np.sin(2 * math.pi * 0.15 * t + 0.4)
    whoosh = bandpass(pink(n), 180, 900) * gust
    return fade_loop(whoosh)


def city(n: int) -> np.ndarray:
    traffic = lowpass(brown(n), 160) * 0.7
    t = np.arange(n) / SR
    passby = np.exp(-((t - 3.2) ** 2) / 0.45) + 0.6 * np.exp(-((t - 6.4) ** 2) / 0.35)
    whoosh = bandpass(pink(n), 250, 1200) * passby * 0.35
    return fade_loop(traffic + whoosh)


def city_crowd(n: int) -> np.ndarray:
    murmur = bandpass(pink(n), 180, 700) * 0.4
    t = np.arange(n) / SR
    wobble = 0.75 + 0.25 * np.sin(2 * math.pi * 0.07 * t)
    return fade_loop(murmur * wobble)


def extra_birds(n: int) -> np.ndarray:
    out = np.zeros(n)
    t0 = np.arange(int(0.18 * SR)) / SR
    starts = [0.4, 1.6, 2.3, 3.8, 5.1, 6.7]
    for s, f0 in zip(starts, [3200, 2800, 3500, 2600, 3000, 3400]):
        i = int(s * SR)
        chirp = np.sin(2 * math.pi * (f0 + 900 * t0) * t0)
        chirp *= np.hanning(len(t0))
        end = min(n, i + len(t0))
        out[i:end] += chirp[: end - i] * 0.35
    bed = bandpass(pink(n), 400, 2000) * 0.04
    return fade_loop(out + bed)


def extra_seagulls(n: int) -> np.ndarray:
    out = np.zeros(n)
    for s, f0 in ((0.6, 980), (2.8, 860), (5.4, 1040)):
        length = int(0.55 * SR)
        i = int(s * SR)
        t = np.arange(length) / SR
        call = np.sin(2 * math.pi * (f0 - 220 * t) * t)
        call += 0.35 * np.sin(2 * math.pi * (f0 * 1.8 - 300 * t) * t)
        call *= np.hanning(length)
        end = min(n, i + length)
        out[i:end] += call[: end - i] * 0.32
    bed = lowpass(pink(n), 500) * 0.06
    return fade_loop(out + bed)


def extra_wind(n: int) -> np.ndarray:
    t = np.arange(n) / SR
    gust = 0.4 + 0.6 * (0.5 + 0.5 * np.sin(2 * math.pi * 0.09 * t))
    return fade_loop(bandpass(pink(n), 120, 700) * gust)


def extra_traffic(n: int) -> np.ndarray:
    engine = lowpass(brown(n), 110) * 0.8
    t = np.arange(n) / SR
    tire = bandpass(pink(n), 400, 1600) * (0.15 + 0.1 * np.sin(2 * math.pi * 0.4 * t))
    return fade_loop(engine + tire)


def extra_industrial(n: int) -> np.ndarray:
    t = np.arange(n) / SR
    hum = 0.18 * np.sin(2 * math.pi * 58 * t) + 0.08 * np.sin(2 * math.pi * 116 * t)
    clank = np.zeros(n)
    for s in (1.0, 2.5, 4.0, 6.2):
        i = int(s * SR)
        length = int(0.08 * SR)
        click = np.sin(2 * math.pi * 420 * np.arange(length) / SR)
        click *= np.exp(-np.linspace(0, 8, length))
        end = min(n, i + length)
        clank[i:end] += click[: end - i] * 0.35
    machine = lowpass(brown(n), 200) * 0.25
    return fade_loop(hum + clank + machine)


def alarm_bell(n: int) -> np.ndarray:
    t = np.arange(n) / SR
    # Two-tone looping beep, classic alarm clock.
    cycle = ((t * 4.0) % 2.0) < 1.0
    tone = np.where(cycle, np.sin(2 * math.pi * 880 * t), np.sin(2 * math.pi * 698 * t))
    gate = ((t * 4.0) % 1.0) < 0.55
    out = tone * gate * 0.55
    return fade_loop(out, ms=8.0)


GENERATORS = {
    "ocean": (8.0, ocean),
    "ocean_surf": (8.0, ocean_surf),
    "rain": (6.0, rain),
    "rain_window": (6.0, rain_window),
    "forest": (8.0, forest),
    "forest_stream": (8.0, forest_stream),
    "storm": (8.0, storm),
    "storm_wind": (7.0, storm_wind),
    "city": (8.0, city),
    "city_crowd": (8.0, city_crowd),
    "extra_birds": (8.0, extra_birds),
    "extra_seagulls": (8.0, extra_seagulls),
    "extra_wind": (8.0, extra_wind),
    "extra_traffic": (8.0, extra_traffic),
    "extra_industrial": (8.0, extra_industrial),
    "alarm_bell": (2.0, alarm_bell),
}


def draw_mark(draw: ImageDraw.ImageDraw, size: int, color: tuple[int, int, int], width_scale: float = 1.0) -> None:
    """Simple wave + leaf, no lettering."""
    m = size
    w = max(2, int(m * 0.045 * width_scale))
    # Waves
    y0 = int(m * 0.58)
    pts1 = []
    pts2 = []
    for x in range(int(m * 0.16), int(m * 0.84) + 1):
        xf = (x - m * 0.16) / (m * 0.68)
        pts1.append((x, y0 + int(math.sin(xf * math.pi * 2.1) * m * 0.055)))
        pts2.append((x, y0 + int(m * 0.11 + math.sin(xf * math.pi * 2.1 + 0.6) * m * 0.045)))
    draw.line(pts1, fill=color, width=w, joint="curve")
    draw.line(pts2, fill=color, width=max(2, w - 1), joint="curve")
    # Leaf
    cx, cy = int(m * 0.50), int(m * 0.36)
    leaf = [
        (cx, cy - int(m * 0.16)),
        (cx + int(m * 0.13), cy - int(m * 0.02)),
        (cx, cy + int(m * 0.07)),
        (cx - int(m * 0.13), cy - int(m * 0.02)),
    ]
    draw.polygon(leaf, fill=color)
    stem_w = max(2, w - 1)
    draw.line([(cx, cy - int(m * 0.02)), (cx, cy + int(m * 0.10))], fill=color, width=stem_w)


def save_png(path: Path, image: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG")


def make_icons() -> None:
    bg = (232, 236, 228, 255)
    fg = (27, 72, 58, 255)

    # Legacy / anydpi fallback
    for density, size in MIPMAPS.items():
        img = Image.new("RGBA", (size, size), bg)
        draw = ImageDraw.Draw(img)
        draw_mark(draw, size, fg[:3])
        save_png(ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher.png", img)
        save_png(ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher_round.png", img)

    # Adaptive layers (108dp)
    for density, size in ADAPTIVE.items():
        back = Image.new("RGBA", (size, size), bg)
        fore = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(fore)
        draw_mark(draw, size, fg[:3], width_scale=1.15)
        save_png(ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher_background.png", back)
        save_png(ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher_foreground.png", fore)

    # Play store 1024
    play = Image.new("RGBA", (1024, 1024), bg)
    draw = ImageDraw.Draw(play)
    draw_mark(draw, 1024, fg[:3], width_scale=1.2)
    PLAY.mkdir(parents=True, exist_ok=True)
    play.convert("RGB").save(PLAY / "icon-1024.png", "PNG")

    # White notification mark
    for density, size in {"mdpi": 24, "hdpi": 36, "xhdpi": 48, "xxhdpi": 72, "xxxhdpi": 96}.items():
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        draw_mark(draw, size, (255, 255, 255), width_scale=1.4)
        save_png(ROOT / f"app/src/main/res/drawable-{density}/ic_stat_wave.png", img)


def main() -> None:
    RAW.mkdir(parents=True, exist_ok=True)
    for name, (seconds, fn) in GENERATORS.items():
        n = int(seconds * SR)
        samples = fn(n)
        write_wav(RAW / f"{name}.wav", samples)
        print(f"wrote {name}.wav ({seconds:.0f}s)")
    make_icons()
    print("icons ready")


if __name__ == "__main__":
    main()
