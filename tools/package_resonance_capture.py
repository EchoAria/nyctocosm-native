"""Package the real client frames plus an explicitly reconstructed, event-timed soundtrack."""
from pathlib import Path
import csv
import hashlib
import json
import shutil
import subprocess

root = Path(__file__).resolve().parents[1]
capture = root / 'run/screenshots/resonance'
output = root / 'docs/validation'
console = (root / 'docs/resonance-client.log').read_text(encoding='utf-8', errors='replace')
assert 'RESONANCE_SMOKE_COMPLETE frames=341 sounds=14' in console
assert 'RESONANCE_SHAKE_COMPLETE windows=6' in console
frames = sorted(capture.glob('frame-*.png'))
assert [p.name for p in frames] == [f'frame-{i:05d}.png' for i in range(341)]
with (capture / 'trace.csv').open(newline='') as stream:
    trace = list(csv.DictReader(stream))
assert len(trace) == 720 and trace[-1]['x_portal'] == trace[-1]['z_portal'] == 'true'
with (capture / 'sound-events.csv').open(newline='') as stream:
    sounds = list(csv.DictReader(stream))
assert len(sounds) == 14
for sequence in (sounds[:7], sounds[7:]):
    start = int(sequence[1]['client_tick'])
    for event, offset in zip(sequence[1:], [0, 16, 32, 90, 106, 122]):
        assert abs(int(event['client_tick'])-start-offset) <= 2
ffmpeg = shutil.which('ffmpeg')
assert ffmpeg, 'Put ffmpeg on PATH.'
output.mkdir(parents=True, exist_ok=True)
catalog = json.loads((root / 'src/main/resources/assets/nyctocosm/sounds.json').read_text())
args = [ffmpeg, '-hide_banner', '-loglevel', 'error', '-y', '-framerate', '10', '-i', str(capture / 'frame-%05d.png')]
filters = []
for i, event in enumerate(sounds):
    key = event['sound'].split(':', 1)[1]
    sound = catalog[key]['sounds'][0]
    asset = sound if isinstance(sound, str) else sound['name']
    namespace, path = asset.split(':', 1)
    args += ['-i', str(root / f'src/main/resources/assets/{namespace}/sounds/{path}.ogg')]
    delay = max(0, (int(event['client_tick'])-20)*50)
    filters.append(f'[{i+1}:a]adelay={delay}:all=1[a{i}]')
filters.append(''.join(f'[a{i}]' for i in range(len(sounds))) + f'amix=inputs={len(sounds)}:normalize=0,volume=0.45,alimiter=limit=0.95[audio]')
video = output / 'resonance-portal.mp4'
subprocess.run(args + ['-filter_complex', ';'.join(filters), '-map', '0:v', '-map', '[audio]', '-t', '34.1', '-c:v', 'libx264', '-crf', '20', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-movflags', '+faststart', str(video)], check=True)
samples = [15, 40, 50, 75, 105, 145, 195, 225, 255, 335]
selection = '+'.join(f'eq(n,{n})' for n in samples)
subprocess.run([ffmpeg, '-hide_banner', '-loglevel', 'error', '-y', '-framerate', '10', '-i', str(capture / 'frame-%05d.png'), '-vf', f"select='{selection}',scale=320:180,tile=5x2", '-frames:v', '1', str(output / 'resonance-contact-sheet.png')], check=True)
for name in ['trace.csv', 'sound-events.csv']:
    shutil.copy2(capture / name, output / ('resonance-' + name))
report = {'version': '0.3.0-native-alpha.11', 'frames': 341, 'fps': 10, 'trace_ticks': 720,
          'verified_client_sound_events': 14, 'verified_shake_windows': 6, 'frame_axes': ['X', 'Z'],
          'audio': 'Reconstructed from the original OGG assets at recorded client sound-event times; not system loopback audio.',
          'checks': ['Actual Soul Bottle + owned Echo Shard casting', 'Both sculk-vein-covered frame interiors activate', 'Portal remains closed during animation', 'Each of seven sound events plays once per opening'],
          'video_sha256': hashlib.sha256(video.read_bytes()).hexdigest()}
(output / 'resonance-capture.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
