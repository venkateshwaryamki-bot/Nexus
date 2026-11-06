"""Generates NEXUS's original placeholder sound effects (procedural synthesis, no third-party samples).

    python3 tools/audio/generate_sfx.py

Outputs 16-bit mono WAV to app/src/main/res/raw/. See ASSET_PIPELINE.md for replacement specs.
"""
import numpy as np, wave, os
SR = 22050
OUT = os.path.join(os.path.dirname(__file__), '../../app/src/main/res/raw')

def save(name, x):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.85
    with wave.open(os.path.join(OUT, name), 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())

def env(n, a, r):
    e = np.ones(n); ai = int(a * SR); ri = int(r * SR)
    e[:ai] = np.linspace(0, 1, ai) ** 2; e[-ri:] = np.linspace(1, 0, ri) ** 1.5
    return e

rng = np.random.default_rng(7)

# Intro (~6.5 s): sub-bass swell, rising shimmer, energy-core ignition at ~3.6 s, airy tail.
d = 6.5; t = np.arange(int(SR * d)) / SR
drone = sum(np.sin(2 * np.pi * f * t + p) * g for f, p, g in [(55, 0, 1), (82.4, 1, .6), (110, 2, .35), (164.8, .5, .2)])
drone *= np.clip(t / 3.0, 0, 1) ** 2 * np.clip((d - t) / 1.5, 0, 1)
rise_f = 220 * 2 ** (t / 1.8)
shimmer = np.sin(2 * np.pi * np.cumsum(np.minimum(rise_f, 1760)) / SR) * 0.18 * np.clip(t / 3.6, 0, 1) ** 3 * (t < 3.7)
noise = np.convolve(rng.standard_normal(len(t)), np.ones(40) / 40, 'same')
whoosh = noise * np.exp(-((t - 3.5) / 0.6) ** 2) * 1.2
hit_t = np.clip(t - 3.6, 0, None)
hit = (np.sin(2 * np.pi * 65 * hit_t) * np.exp(-hit_t * 2.5) * 1.6 +
       sum(np.sin(2 * np.pi * f * hit_t) * np.exp(-hit_t * k) * g for f, k, g in [(523.3, 1.2, .3), (784, 1.0, .22), (1046.5, .8, .15), (1318.5, .7, .1)])) * (t >= 3.6)
save('nexus_intro.wav', drone * 0.5 + shimmer + whoosh * 0.35 + hit)

# Session complete (~1.6 s): ascending bright arpeggio with soft bell partials.
d = 1.6; t = np.arange(int(SR * d)) / SR; x = np.zeros_like(t)
for i, f in enumerate([523.3, 659.3, 784.0, 1046.5]):
    tt = np.clip(t - i * 0.09, 0, None)
    x += (np.sin(2 * np.pi * f * tt) + .3 * np.sin(2 * np.pi * f * 2.01 * tt)) * np.exp(-tt * 3.2) * (t >= i * 0.09)
save('session_complete.wav', x * env(len(t), 0.005, 0.4))

# Energy gain (~0.7 s): short upward sweep, used for the home-screen pulse.
d = 0.7; t = np.arange(int(SR * d)) / SR
f = 300 + 900 * (t / d) ** 1.5
save('energy_gain.wav', np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(t), 0.01, 0.35) * 0.8)
# Ambient focus loop (~24 s, seamless): slow detuned pads + airy filtered noise, no rhythm, very low
# contrast so it supports concentration. Loops cleanly because every partial completes whole cycles.
d = 24.0; t = np.arange(int(SR * d)) / SR
def cyc(f): return round(f * d) / d  # snap to whole cycles over the loop
pad = sum(np.sin(2 * np.pi * cyc(f) * t + ph) * g for f, ph, g in [(110, 0, .5), (110.5, 1, .45), (164.8, 2, .3), (220, .4, .18), (329.6, 1.3, .08)])
lfo = 0.75 + 0.25 * np.sin(2 * np.pi * cyc(1 / 8) * t)
air = np.convolve(rng.standard_normal(len(t)), np.ones(300) / 300, 'same')
air = air - np.convolve(air, np.ones(30) / 30, 'same') * 0  # keep soft
air *= 0.6 * (0.7 + 0.3 * np.sin(2 * np.pi * cyc(1 / 12) * t + 1))
x = pad * lfo * 0.6 + air * 3
n = int(0.05 * SR); x[:n] = x[:n] * np.linspace(0, 1, n) + x[-n:] * np.linspace(1, 0, n)  # crossfade seam
save('ambient_focus.wav', x[:-n] * 0.5)
# Ship the loop as Ogg Vorbis (~10x smaller). Needs `pip install soundfile`.
import soundfile as sf
wav = os.path.join(OUT, 'ambient_focus.wav'); data, rate = sf.read(wav)
sf.write(os.path.join(OUT, 'ambient_focus.ogg'), data, rate, format='OGG', subtype='VORBIS'); os.remove(wav)
print('ok')
