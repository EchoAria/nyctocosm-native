"""Compile source variant keyframes and the requested phase-two palette; source stays read-only."""
import gzip, hashlib, json, re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root.parent / '_nyctocosm_source/datapack/data/animated_java/functions'
assets = root / 'src/main/resources/assets'
report = {}
for name in ('ajsorcerer', 'ajsorcererm1', 'ajsorcererm2', 'ajemissary'):
    path = assets / f'nyctocosm/native_rigs/{name}.json.gz'
    rig = json.loads(gzip.decompress(path.read_bytes()))
    events, hashes = {}, {}
    for folder in sorted((source / name / 'zzzzzzzz/animations').iterdir()):
        timeline = []
        for file in sorted((folder / 'tree').glob('leaf_*.mcfunction')):
            match = re.fullmatch(r'leaf_(\d+)\.mcfunction', file.name)
            if not match: continue
            variants = re.findall(r'function animated_java:[^/]+/zzzzzzzz/apply_variant/([^/]+)/as_root', file.read_text(encoding='utf-8-sig'))
            if variants:
                timeline.append({'tick': int(match[1]), 'variants': variants})
                hashes[str(file.relative_to(source))] = hashlib.sha256(file.read_bytes()).hexdigest()
        if timeline: events[folder.name] = sorted(timeline, key=lambda value: value['tick'])
    rig['variant_keyframes'] = events
    if name == 'ajemissary':
        palette = {}
        for bone, model in rig['variants']['wing_config_1'].items():
            namespace, model_path = model.split(':')
            textures = {}
            cursor = assets / namespace / 'models' / (model_path + '.json')
            while cursor.exists():
                data = json.loads(cursor.read_text(encoding='utf-8-sig'))
                for key, value in data.get('textures', {}).items(): textures.setdefault(key, value)
                parent = data.get('parent', '')
                if ':' not in parent: break
                ns, relative = parent.split(':')
                cursor = assets / ns / 'models' / (relative + '.json')
            # Transparent slots still hide the source's removed wings.
            override = {key: value[:-1]+'0' for key, value in textures.items() if re.fullmatch(r'nyctocosm:emissary/(upper|middle|lower)wing_1', value)}
            target = f'item/ajemissary/wing_phase2/{bone}'
            destination = assets / 'animated_java/models' / (target+'.json')
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_text(json.dumps({'parent': model, 'textures': override}, indent=2)+'\n', encoding='utf-8')
            palette[bone] = 'animated_java:'+target
        rig['variants']['wing_phase2'] = palette
    path.write_bytes(gzip.compress(json.dumps(rig, separators=(',', ':')).encode(), mtime=0))
    report[name] = {'keyframes': events, 'source_sha256': hashes}
(root / 'docs/validation/rig-variant-import-20261002.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
print({name: sum(map(len, data['keyframes'].values())) for name, data in report.items()})
