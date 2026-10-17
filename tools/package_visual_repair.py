"""Record the actual alpha.14 release, tests, client captures and read-only source evidence."""
from pathlib import Path
import hashlib, json, re, zipfile

root=Path(__file__).resolve().parents[1]
evidence=root/'docs/validation/visual-repair-20261002'
evidence.mkdir(exist_ok=True)
version=re.search(r"^version\s*=\s*'([^']+)'",(root/'build.gradle').read_text(),re.M)[1]
assert version=='0.3.0-native-alpha.14','Use the current release packaging script; preserve the historical alpha.14 manifest.'
jar=root/f'build/libs/nyctocosm-native-{version}.jar'
digest=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
tests=[]
for name in ('visual-repair-gametest-green3-20261002','d31-context-green-20261002','visual-repair-alpha14-soul-final-20261002'):
    path=root/f'docs/validation/{name}.log'
    log=path.read_text(encoding='utf-8',errors='replace')
    passed=re.search(r'All (\d+) required tests passed',log)
    assert passed and 'BUILD SUCCESSFUL' in log and ' failed!' not in log,path
    tests.append({'log':str(path),'required_passed':int(passed[1]),'sha256':digest(path)})
client=root/'docs/validation/visual-repair-alpha14-soul-client-20261002.log'
assert 'VISUAL_REPAIR_CAPTURE_COMPLETE' in client.read_text(encoding='utf-8',errors='replace')
audit=json.loads((root/'docs/native-audit.json').read_text(encoding='utf-8'))
assert audit['errors']==[] and audit['checks']['jar_sha256']==digest(jar)
source=root.parent/'_nyctocosm_source/datapack/data'
source_hashes={}
for filename,base in (('sorcerer-particle-import-20261002.json',source),('rig-variant-import-20261002.json',source/'animated_java/functions')):
    report=json.loads((root/'docs/validation'/filename).read_text())
    groups=[report] if 'source_sha256' in report else list(report.values())
    for group in groups:
        for relative,expected in group['source_sha256'].items():
            path=base/relative
            assert path.is_file() and digest(path)==expected,(filename,relative)
            source_hashes[str(path)]=expected
for path in (source/'sorcerer/functions/attacks/singularitycharge').rglob('*.mcfunction'):
    source_hashes[str(path)]=digest(path)
for relative in ('sorcerer/functions/load.mcfunction','animated_java/functions/ajsorcererm1/zzzzzzzz/animations/singularitycharge/tree/leaf_44.mcfunction'):
    path=source/relative;source_hashes[str(path)]=digest(path)
with zipfile.ZipFile(jar) as archive:
    entries=archive.namelist()
    forbidden=[name for name in entries if 'NativeGameTests' in name or 'ClientSmoke' in name or name.endswith('.mcfunction')]
    assert not forbidden,forbidden
    classes=[int.from_bytes(archive.read(name)[6:8],'big') for name in entries if name.endswith('.class')]
    assert max(classes)<=61,sorted(set(classes))
previous=root/'build/libs/nyctocosm-native-0.3.0-native-alpha.13.jar'
assert digest(previous)=='1fd0e3caaac1cdcc3b7647bc9e93f92b88c0f3e4875af58024cfabfe124c604c'
manifest={'version':version,'jar':str(jar),'bytes':jar.stat().st_size,'sha256':digest(jar),'entries':len(entries),
    'java_class_major_versions':sorted(set(classes)),'tests':tests,'client_log':str(client),'client_log_sha256':digest(client),
    'screenshots':str(root/'run-visual-repair-alpha14-soul-client-20261002/screenshots'),'source_sha256':source_hashes,
    'implementation_sha256':{str(path.relative_to(root)):digest(path) for path in (root/'src/main/java').rglob('*.java')},
    'previous_alpha13_sha256':digest(previous),'audit':audit,
    'pending':['D28 cancellation-status choice','portal shader/settings reproduction','D29/D30/D32/D33 independent combat parity',
               'existing world-fire/party/altar decisions','real multiplayer, chunk unload/restart, shader and FPS acceptance']}
(evidence/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({key:manifest[key] for key in ('version','jar','bytes','sha256','entries')},ensure_ascii=False,indent=2))
