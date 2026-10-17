"""Archive the source-command differential result; original datapack files remain read-only."""
from pathlib import Path
import csv
import hashlib
import json
import re
import shutil

root = Path(__file__).resolve().parents[1]
console = (root / 'docs/p0-gametest-console.log').read_text(encoding='utf-8', errors='replace')
assert 'SERPENT_SOURCE_BODY_MATCH samples=17220' in console
assert re.search(r'All \d+ required tests passed', console), 'Latest GameTest run did not pass.'
trace = root / 'build/validation/serpent-body-source-trace.csv'
with trace.open(encoding='utf-8', newline='') as stream:
    rows = list(csv.DictReader(stream))
assert len(rows) == 420 * 41
assert {(int(r['tick']), int(r['link'])) for r in rows} == {(t, s) for t in range(420) for s in range(1, 42)}
maximum = max(float(r['error']) for r in rows)
assert maximum < .002
source = root.parent / '_nyctocosm_source/datapack/data/serpent/functions/__generated__/block'
files = [source / f'{35 + tier * 51 + link}.mcfunction' for tier in range(3) for link in range(41)]
output = root / 'docs/validation'
output.mkdir(parents=True, exist_ok=True)
shutil.copy2(trace, output / trace.name)
report = {
    'test': 'NativeGameTests.serpent_body_matches_original_function_files',
    'ticks': 420, 'segments': 40, 'separate_tail': 1, 'compared_positions': len(rows),
    'maximum_position_error_blocks': maximum,
    'source_execution': 'Minecraft executes the original tp subcommand verbatim. The fixture evaluates the source selector minimum-distance gate directly and resolves facing-entity feet from the known predecessor coordinates, avoiding stale entity-section lookups after manually moved test markers.',
    'source_functions_sha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in files},
    'trace_sha256': hashlib.sha256(trace.read_bytes()).hexdigest(),
    'limits': ['Explicit head-to-tail traversal; not a full original datapack scheduler or multiplayer replay.'],
}
(output / 'serpent-source-comparison.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'Compared {len(rows)} positions from {len(files)} original functions; maximum error {maximum}.')
