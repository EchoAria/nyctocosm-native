"""Reproducible static checks for converted assets and the optionally built release jar."""
from pathlib import Path
import gzip
import hashlib
import json
import math
import re
import sys
import zipfile

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
errors=[]
checks={}

def check(condition,message):
    if not condition: errors.append(message)

def exists(identifier,folder,suffix):
    namespace,path=identifier.split(':',1) if ':' in identifier else ('minecraft',identifier)
    return namespace=='minecraft' or (RES/'assets'/namespace/folder/(path+suffix)).is_file()

models=list((RES/'assets').glob('*/models/**/*.json'))
for path in models:
    obj=json.loads(path.read_text(encoding='utf-8-sig'))
    if 'parent' in obj: check(exists(obj['parent'],'models','.json'),f'Missing model parent: {path.relative_to(RES)} -> {obj["parent"]}')
    for texture in obj.get('textures',{}).values():
        if not texture.startswith('#'):check(exists(texture,'textures','.png'),f'Missing texture: {path.relative_to(RES)} -> {texture}')
checks['model_files']=len(models)
rigs=list((RES/'assets/nyctocosm/native_rigs').glob('*.json.gz'))
frames=0
for path in rigs:
    with gzip.open(path,'rt',encoding='utf-8') as handle: rig=json.load(handle)
    bones={bone['name'] for bone in rig['bones']}
    for bone in rig['bones']:
        check(exists(bone['model'],'models','.json'),f'Missing bone model: {path.name}/{bone["name"]}')
        check(len(bone['transform'])==16 and all(math.isfinite(n) for n in bone['transform']),f'Invalid rest matrix: {path.name}')
    for animation,keyframes in rig['animations'].items():
        frames+=len(keyframes)
        check([f['tick'] for f in keyframes]==sorted(set(f['tick'] for f in keyframes)),f'Unordered/duplicate frame: {path.name}/{animation}')
        for frame in keyframes:
            for name,matrix in frame['bones'].items():
                check(name in bones,f'Unknown bone: {path.name}/{name}')
                check(len(matrix)==16 and all(math.isfinite(n) for n in matrix),f'Invalid animation matrix: {path.name}/{animation}')
checks.update(rigs=len(rigs),animation_frames=frames)
sounds=json.loads((RES/'assets/nyctocosm/sounds.json').read_text(encoding='utf-8-sig'))
for key,value in sounds.items():
    for sound in value['sounds']:
        if isinstance(sound,dict) and sound.get('type')=='event':continue
        name=sound if isinstance(sound,str) else sound['name']
        check(exists(name,'sounds','.ogg'),f'Missing sound: {key} -> {name}')
checks['sound_events']=len(sounds)
check(not list(RES.rglob('*.mcfunction')),'Runtime mcfunctions found')
for path in (ROOT/'src/main/java').rglob('*.java'):
    check(not re.search(r'performPrefixedCommand|performCommand|CommandFunction|\.getFunctions\(',path.read_text(encoding='utf-8')),f'Command runtime dependency: {path.name}')
for path in (RES/'data').glob('*/tags/functions/*.json'):
    check(path.stem not in ('load','tick'),f'Datapack tick/load hook: {path}')
checks['runtime_mcfunctions']=0
if len(sys.argv)>1:
    jar=Path(sys.argv[1])
    with zipfile.ZipFile(jar) as archive:
        names=archive.namelist()
        check('META-INF/mods.toml' in names,'Missing mod manifest')
        forbidden=[name for name in names if name.endswith('.mcfunction') or 'NativeGameTests' in name or 'ClientSmoke' in name or name in ('data/nyctocosm/structures/empty.nbt','data/nyctocosm/structures/portal_test.nbt')]
        check(not forbidden,f'Development/datapack execution content shipped: {forbidden}')
        checks['jar_entries']=len(names)
        checks['jar']=str(jar.resolve())
        checks['jar_sha256']=hashlib.sha256(jar.read_bytes()).hexdigest()
        checks['jar_bytes']=jar.stat().st_size
report={'checks':checks,'errors':errors}
(ROOT/'docs/native-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(bool(errors))
