"""Package the opt-in client capture as a silent video and a reproducible evidence index."""
from pathlib import Path
import csv
import hashlib
import json
import shutil
import subprocess

root = Path(__file__).resolve().parents[1]
capture = root / 'run/screenshots/p0'
output = root / 'docs/validation'
output.mkdir(parents=True, exist_ok=True)
frames = sorted(capture.glob('frame-*.png'))
assert len(frames) == 811, f'Expected 811 continuous frames, got {len(frames)}'
assert [p.name for p in frames] == [f'frame-{n:05d}.png' for n in range(811)], 'Missing capture frame'
console = (root / 'docs/p0-client-console.log').read_text(encoding='utf-8', errors='replace')
assert 'P0_PLANE_TRANSFORM_MATCH' in console and 'P0_SMOKE_COMPLETE frames=811' in console
assert 'P0_SERPENT_PITCH_TRANSFORM_MATCH' in console
with (capture / 'trace.csv').open(encoding='utf-8', newline='') as source:
    rows = list(csv.DictReader(source))
assert len(rows) == 1670 and [int(row['tick']) for row in rows] == list(range(1, 1671))
shutil.copy2(capture / 'trace.csv', output / 'p0-native-trace.csv')
ffmpeg = shutil.which('ffmpeg')
if not ffmpeg:
    raise SystemExit('Install ffmpeg or put it on PATH to encode the continuous capture.')
video = output / 'p0-continuous.mp4'
subprocess.run([ffmpeg, '-hide_banner', '-loglevel', 'error', '-y', '-framerate', '10',
                '-i', str(capture / 'frame-%05d.png'), '-c:v', 'libx264', '-crf', '20',
                '-pix_fmt', 'yuv420p', '-movflags', '+faststart', str(video)], check=True)
samples = [65, 100, 150, 240, 300, 330, 414, 418, 550, 620, 720, 800]
selection = '+'.join(f'eq(n,{n})' for n in samples)
subprocess.run([ffmpeg, '-hide_banner', '-loglevel', 'error', '-y', '-framerate', '10',
                '-i', str(capture / 'frame-%05d.png'), '-vf', f"select='{selection}',scale=427:240,tile=4x3",
                '-frames:v', '1', str(output / 'p0-contact-sheet.png')], check=True)
report = {
    'version': '0.3.0-native-alpha.9',
    'capture_frames': len(frames), 'capture_fps': 10, 'server_trace_rows': len(rows),
    'vanilla_armor_stand_matrix_comparison': 'passed; tolerance 0.00001',
    'serpent_helmet_pitch_comparison': 'passed at -80, -30, 30, 80 degrees; tolerance 0.00001',
    'scenes_seconds': {'sorcerer_and_vanilla_reference': [0, 29], 'serpent_dive_and_reload': [29, 54],
                       'serpent_laser': [54, 66.5], 'serpent_beam': [66.5, 81.1]},
    'contact_sheet_frame_indices': samples,
    'video_sha256': hashlib.sha256(video.read_bytes()).hexdigest(),
    'limitations': ['Silent client capture; not an audio comparison.',
                    'Vanilla armor stand is a rendering reference, not the complete original datapack.',
                    'No full original-pack combat capture, real multiplayer, or third-party mob validation.'],
}
(output / 'p0-capture.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
