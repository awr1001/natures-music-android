#!/usr/bin/env python3
"""Normalize Nature's Music mixable sounds to ~-16 LUFS, TP <= -1 dBTP.
Writes the SAME bytes to Android res/raw and iOS Sound Generator 2.
"""
from __future__ import annotations

import argparse
import csv
import math
import os
import shutil
import subprocess
import sys
import traceback
from pathlib import Path

import lameenc
import numpy as np
import pyloudnorm as pyln
import soundfile as sf
from scipy.signal import resample_poly

ANDROID_RAW = Path("/Users/aaronroberts/Desktop/Android Apps/Nature's Music/app/src/main/res/raw")
ANDROID_ROOT = Path("/Users/aaronroberts/Desktop/Android Apps/Nature's Music")
IOS_DIR = Path("/Users/aaronroberts/Desktop/iOS Apps/Nature's Music/Nature's Music/Sound Generator 2")
IOS_BACKUP = Path("/Users/aaronroberts/Desktop/iOS Apps/Nature's Music/Nature's Music/.loudness-backup-ios")
ANDROID_BACKUP = Path("/Users/aaronroberts/Desktop/Android Apps/Nature's Music/.loudness-backup-android")
WORK = Path("/Users/aaronroberts/Desktop/Android Apps/Nature's Music/.loudness-work")
DECODE = WORK / "decode"
NORMALIZED = WORK / "normalized"
LOG_PATH = WORK / "loudness_log.csv"

TARGET_LUFS = -16.0
TARGET_TP_DB = -1.0
TARGET_RMS_DB = -20.0
SKIP_STEMS = {"click", "tap", "tap2", "tictoc"}
AUDIO_EXTS = {".mp3", ".wav"}


def db_to_lin(db: float) -> float:
    return 10.0 ** (db / 20.0)


def lin_to_db(x: float) -> float:
    if x <= 0:
        return -120.0
    return 20.0 * math.log10(x)


def true_peak_lin(audio: np.ndarray, over: int = 4) -> float:
    """4x oversampled true-peak (linear). audio is (n, ch) float."""
    if audio.size == 0:
        return 0.0
    peak = float(np.max(np.abs(audio)))
    n = audio.shape[0]
    ch = audio.shape[1]
    # Process in ~2s chunks with 32-sample overlap to catch boundary peaks.
    hop = 88200
    pad = 32
    for start in range(0, n, hop):
        a = max(0, start - pad)
        b = min(n, start + hop + pad)
        chunk = audio[a:b]
        for c in range(ch):
            y = resample_poly(chunk[:, c], over, 1)
            peak = max(peak, float(np.max(np.abs(y))))
    return peak


def rms_db(audio: np.ndarray) -> float:
    m = float(np.mean(np.square(audio, dtype=np.float64)))
    if m <= 0:
        return -120.0
    return 10.0 * math.log10(m)


def load_audio(path: Path, tmp_wav: Path) -> tuple[np.ndarray, int]:
    """Return (n, ch) float64 in -1..1 and sample rate."""
    if path.suffix.lower() == ".mp3":
        tmp_wav.parent.mkdir(parents=True, exist_ok=True)
        if tmp_wav.exists():
            tmp_wav.unlink()
        subprocess.run(
            ["afconvert", "-f", "WAVE", "-d", "LEF32", str(path), str(tmp_wav)],
            check=True,
            capture_output=True,
        )
        data, rate = sf.read(str(tmp_wav), always_2d=True, dtype="float64")
        try:
            tmp_wav.unlink()
        except OSError:
            pass
        return data, int(rate)
    data, rate = sf.read(str(path), always_2d=True, dtype="float64")
    return data, int(rate)


def measure_lufs(audio: np.ndarray, rate: int) -> float | None:
    n = audio.shape[0]
    if n < int(rate * 0.4):
        # Pad with silence so the BS.1770 gate has enough blocks.
        pad_n = int(rate * 0.5) - n
        if pad_n > 0:
            audio = np.concatenate([audio, np.zeros((pad_n, audio.shape[1]), dtype=audio.dtype)], axis=0)
    try:
        meter = pyln.Meter(rate)
        loud = float(meter.integrated_loudness(audio))
        if not math.isfinite(loud) or loud < -70.0:
            return None
        return loud
    except Exception:
        return None


def normalize(audio: np.ndarray, rate: int) -> tuple[np.ndarray, dict]:
    info: dict = {}
    tp_in = true_peak_lin(audio)
    info["in_tp_db"] = round(lin_to_db(tp_in), 2)
    info["in_rms_db"] = round(rms_db(audio), 2)
    peak_lim = db_to_lin(TARGET_TP_DB)

    lufs = measure_lufs(audio, rate)
    info["in_lufs"] = round(lufs, 2) if lufs is not None else ""

    if lufs is not None:
        gain = db_to_lin(TARGET_LUFS - lufs)
        method = "LUFS"
    else:
        in_rms = rms_db(audio)
        if in_rms <= -120.0:
            info["method"] = "SILENT"
            info["gain_db"] = 0.0
            return audio, info
        gain = db_to_lin(TARGET_RMS_DB - in_rms)
        method = "RMS"

    out = audio * gain
    tp = true_peak_lin(out)
    if tp > peak_lim and tp > 0:
        extra = peak_lim / tp
        out = out * extra
        gain *= extra
        method = method + "+TPcap"

    info["method"] = method
    info["gain_db"] = round(lin_to_db(gain) if gain > 0 else -120.0, 2)
    info["out_tp_db"] = round(lin_to_db(true_peak_lin(out)), 2)
    info["out_rms_db"] = round(rms_db(out), 2)
    out_lufs = measure_lufs(out, rate)
    info["out_lufs"] = round(out_lufs, 2) if out_lufs is not None else ""
    return out, info


def encode_mp3(audio: np.ndarray, rate: int, dest: Path) -> None:
    ch = int(audio.shape[1])
    bitrate = 128 if ch == 1 else 192
    pcm = np.clip(np.round(audio * 32767.0), -32767, 32767).astype(np.int16)
    pcm = np.ascontiguousarray(pcm)
    enc = lameenc.Encoder()
    enc.set_bit_rate(bitrate)
    enc.set_in_sample_rate(int(rate))
    enc.set_channels(ch)
    enc.set_quality(2)
    try:
        enc.set_cbr(True)
    except Exception:
        pass
    mp3 = enc.encode(pcm.tobytes())
    mp3 += enc.flush()
    dest.write_bytes(bytes(mp3))


def write_wav(audio: np.ndarray, rate: int, dest: Path) -> None:
    # soundfile writes (n, ch); squeeze mono to (n,) for a proper 1ch wav.
    data = audio
    if data.shape[1] == 1:
        data = data[:, 0]
    sf.write(str(dest), data, int(rate), subtype="PCM_16")


def ios_index() -> dict[str, Path]:
    out: dict[str, Path] = {}
    for p in IOS_DIR.iterdir():
        if p.suffix.lower() in AUDIO_EXTS and not p.name.startswith("."):
            out[p.stem.lower()] = p
    return out


def android_index() -> dict[str, Path]:
    out: dict[str, Path] = {}
    for p in ANDROID_RAW.iterdir():
        if p.suffix.lower() in AUDIO_EXTS and not p.name.startswith("."):
            out[p.stem.lower()] = p
    return out


def pick_source(android: Path | None, ios: Path | None) -> Path | None:
    paths = [p for p in (android, ios) if p is not None and p.exists()]
    if not paths:
        return None
    wavs = [p for p in paths if p.suffix.lower() == ".wav"]
    if wavs:
        return max(wavs, key=lambda p: p.stat().st_size)
    return paths[0]


def backup_originals(stems: list[str], android_map: dict[str, Path], ios_map: dict[str, Path]) -> None:
    ANDROID_BACKUP.mkdir(parents=True, exist_ok=True)
    IOS_BACKUP.mkdir(parents=True, exist_ok=True)
    for stem in stems:
        a = android_map.get(stem)
        i = ios_map.get(stem)
        if a and a.exists():
            dest = ANDROID_BACKUP / a.name
            if not dest.exists():
                shutil.copy2(a, dest)
        if i and i.exists():
            dest = IOS_BACKUP / i.name
            if not dest.exists():
                shutil.copy2(i, dest)


def clear_android_duplicates(stem: str, keep: Path) -> None:
    for p in ANDROID_RAW.iterdir():
        if p.stem.lower() == stem and p.suffix.lower() in AUDIO_EXTS and p.resolve() != keep.resolve():
            p.unlink()
            print(f"  removed android duplicate {p.name}")


def process_one(stem: str, android: Path | None, ios: Path | None) -> dict:
    src = pick_source(android, ios)
    if src is None:
        return {"stem": stem, "status": "MISSING"}

    # Keep original iOS format so ViewController ofType stays valid.
    if ios is not None:
        out_ext = ios.suffix.lower()
    elif android is not None:
        out_ext = android.suffix.lower()
    else:
        out_ext = ".wav"

    row = {
        "stem": stem,
        "source": str(src),
        "out_ext": out_ext,
        "ios_name": ios.name if ios else "",
        "android_in": android.name if android else "",
    }

    tmp = DECODE / f"{stem}.f32.wav"
    audio, rate = load_audio(src, tmp)
    audio, info = normalize(audio, rate)
    row.update(info)

    out_path = NORMALIZED / f"{stem}{out_ext}"
    if out_path.exists():
        out_path.unlink()
    if out_ext == ".mp3":
        encode_mp3(audio, rate, out_path)
    else:
        write_wav(audio, rate, out_path)
    row["out_bytes"] = out_path.stat().st_size

    # Copy identical bytes to both apps.
    android_dest = ANDROID_RAW / f"{stem}{out_ext}"
    shutil.copy2(out_path, android_dest)
    os.chmod(android_dest, 0o644)
    clear_android_duplicates(stem, android_dest)
    row["android_out"] = android_dest.name

    if ios is not None:
        ios_dest = IOS_DIR / ios.name
        # If extension matches original iOS name, overwrite in place.
        # If we had to change ext (shouldn't), write new name — keep original.
        if ios.suffix.lower() != out_ext:
            # Should not happen; keep iOS filename/ext by encoding that format.
            ios_dest = IOS_DIR / ios.name
        shutil.copy2(out_path, ios_dest)
        os.chmod(ios_dest, 0o644)
        row["ios_out"] = ios_dest.name
        if ios_dest.suffix.lower() != out_ext:
            row["status"] = "EXT_MISMATCH"
        else:
            row["status"] = "OK"
    else:
        row["ios_out"] = ""
        row["status"] = "ANDROID_ONLY"

    return row


def collect_stems(android_map: dict[str, Path], ios_map: dict[str, Path]) -> list[str]:
    stems = set(android_map) | set(ios_map)
    stems -= SKIP_STEMS
    # Mixable catalog lives on Android raw. Don't add iOS-only UI files.
    # Include iOS mixable that also exist on Android, plus Android-only (alarm_bell).
    # If an iOS mixable is missing from Android, still include it so platforms share it.
    # Filter: skip if BOTH sides would be UI-only (already skipped). Keep union minus skip.
    # But iOS has only mixable + UI clicks. After skip, remaining iOS-only would be mixable.
    return sorted(stems)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--only", nargs="*", help="Process only these stems (lowercase)")
    parser.add_argument("--skip-backup", action="store_true")
    args = parser.parse_args()

    DECODE.mkdir(parents=True, exist_ok=True)
    NORMALIZED.mkdir(parents=True, exist_ok=True)

    android_map = android_index()
    ios_map = ios_index()
    stems = collect_stems(android_map, ios_map)
    if args.only:
        only = {s.lower() for s in args.only}
        stems = [s for s in stems if s in only]

    if not args.skip_backup:
        backup_originals(stems, android_map, ios_map)

    fieldnames = [
        "stem", "status", "source", "method", "in_lufs", "in_rms_db", "in_tp_db",
        "gain_db", "out_lufs", "out_rms_db", "out_tp_db", "out_ext", "out_bytes",
        "android_in", "android_out", "ios_name", "ios_out",
    ]
    rows = []
    errors = []
    for i, stem in enumerate(stems, 1):
        print(f"[{i}/{len(stems)}] {stem} ...", flush=True)
        try:
            row = process_one(stem, android_map.get(stem), ios_map.get(stem))
            rows.append(row)
            print(
                f"    {row.get('status')} {row.get('method')} "
                f"inLUFS={row.get('in_lufs')} gain={row.get('gain_db')} "
                f"outLUFS={row.get('out_lufs')} outTP={row.get('out_tp_db')} "
                f"-> {row.get('android_out')} / {row.get('ios_out')}",
                flush=True,
            )
        except Exception as e:
            err = f"{stem}: {e}"
            errors.append(err)
            print(f"    ERROR {err}", flush=True)
            traceback.print_exc()
            rows.append({"stem": stem, "status": f"ERROR:{e}"})

    with LOG_PATH.open("w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)

    ok = sum(1 for r in rows if str(r.get("status", "")).startswith("OK") or r.get("status") == "ANDROID_ONLY")
    print(f"\nDone. {ok}/{len(rows)} ok. log={LOG_PATH}", flush=True)
    if errors:
        print("ERRORS:", *errors, sep="\n  ", flush=True)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
