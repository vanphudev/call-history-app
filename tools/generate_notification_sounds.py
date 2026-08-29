"""Generate the four short, original MCAS notification WAV assets."""

from __future__ import annotations

import math
import struct
import wave
from pathlib import Path


RATE = 44_100
OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "raw"


def envelope(t: float, start: float, duration: float, attack: float = 0.018, release: float = 0.18) -> float:
    local = t - start
    if local < 0.0 or local >= duration:
        return 0.0
    attack_gain = min(1.0, local / attack)
    release_gain = min(1.0, (duration - local) / release)
    return attack_gain * release_gain


def tone(t: float, start: float, duration: float, frequency: float, volume: float = 1.0) -> float:
    gain = envelope(t, start, duration)
    local = max(0.0, t - start)
    fundamental = math.sin(2.0 * math.pi * frequency * local)
    harmonic = 0.24 * math.sin(2.0 * math.pi * frequency * 2.01 * local)
    return volume * gain * (fundamental + harmonic) / 1.24


def pulse(t: float) -> float:
    return tone(t, 0.02, 0.30, 659.25, 0.72) + tone(t, 0.28, 0.48, 987.77, 0.78)


def ripple(t: float) -> float:
    notes = ((0.00, 523.25), (0.13, 659.25), (0.26, 783.99), (0.39, 1046.50))
    return sum(tone(t, start, 0.48, frequency, 0.34) for start, frequency in notes)


def bamboo(t: float) -> float:
    first = tone(t, 0.02, 0.50, 392.00, 0.70)
    second = tone(t, 0.38, 0.62, 587.33, 0.68)
    breath = 0.035 * math.sin(2.0 * math.pi * 2113.0 * t) * envelope(t, 0.02, 0.98, 0.01, 0.35)
    return first + second + breath


def crystal(t: float) -> float:
    base = tone(t, 0.01, 1.24, 880.00, 0.53)
    shimmer = tone(t, 0.01, 1.10, 1318.51, 0.32)
    top = tone(t, 0.20, 0.84, 1760.00, 0.20)
    return base + shimmer + top


def write(name: str, duration: float, sampler) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    samples = []
    for index in range(round(duration * RATE)):
        value = max(-1.0, min(1.0, sampler(index / RATE)))
        samples.append(struct.pack("<h", round(value * 25_500)))
    with wave.open(str(OUT / name), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(b"".join(samples))


if __name__ == "__main__":
    write("mcas_pulse.wav", 0.90, pulse)
    write("mcas_ripple.wav", 1.05, ripple)
    write("mcas_bamboo.wav", 1.22, bamboo)
    write("mcas_crystal.wav", 1.38, crystal)
