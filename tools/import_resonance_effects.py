"""Compile original local particle geometry to numeric assets. No command runtime is shipped."""
from pathlib import Path
import hashlib
import json
import re

root = Path(__file__).resolve().parents[1]
source = root.parent / '_nyctocosm_source/datapack/data/effects/functions'
groups = {
    'burst': {1: 'elements/resonance/transmutate/__generated__/block/597.mcfunction',
              2: 'elements/resonance/transmutate/__generated__/block/595.mcfunction',
              3: 'elements/resonance/transmutate/__generated__/block/596.mcfunction'},
    'shockwave': {1: 'shockwave/__generated__/block/36.mcfunction',
                  4: 'shockwave/__generated__/block/37.mcfunction',
                  6: 'shockwave/__generated__/block/38.mcfunction'},
}
pattern = re.compile(r'(?:execute positioned (\^\S*) (\^\S*) (\^\S*) run )?particle minecraft:(\w+)(?: 0)? ~ ~ ~ (\^\S*) (\^\S*) (\^\S*) (\S+) 0(?: force)?$')
result = {}; provenance = {}
def number(value):
    return float(value[1:] or 0) if value else 0
for group, files in groups.items():
    result[group] = {}
    for frame, path in files.items():
        file = source / path
        provenance[path] = hashlib.sha256(file.read_bytes()).hexdigest()
        particles = []
        for line in file.read_text().splitlines():
            if 'particle ' not in line:
                continue
            match = pattern.fullmatch(line)
            assert match, (path, line)
            x, y, z, kind, dx, dy, dz, speed = match.groups()
            particles.append([kind, *map(number, (x, y, z)), *[number(v)*float(speed) for v in (dx, dy, dz)]])
        result[group][str(frame)] = particles
output = root / 'src/main/resources/assets/nyctocosm/effects/resonance.json'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(result, separators=(',', ':')) + '\n', encoding='utf-8')
report = {'source_sha256': provenance, 'counts': {k: {f: len(v) for f, v in frames.items()} for k, frames in result.items()}}
(root / 'docs/resonance-particle-import.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
print(report['counts'])
