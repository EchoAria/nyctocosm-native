"""Capture alpha.15 evidence without overwriting the previous visual repair manifest."""
from pathlib import Path
import hashlib,json,re,zipfile

root=Path(__file__).resolve().parents[1]
digest=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
version=re.search(r"^version\s*=\s*'([^']+)'",(root/'build.gradle').read_text(),re.M)[1]
assert version=='0.3.0-native-alpha.15'
jar=root/f'build/libs/nyctocosm-native-{version}.jar'
evidence=root/'docs/validation/sorcerer-repair-alpha15-20261002'
evidence.mkdir(exist_ok=True)
tests=[]
for name in ('d29-chain-green-20261002','d30-wave-green2-20261002','d32-ray-green2-20261002','sorcerer-repair-alpha15-final2-20261002'):
    path=root/f'docs/validation/{name}.log';log=path.read_text(encoding='utf-8',errors='replace')
    passed=re.search(r'All (\d+) required tests passed',log)
    assert passed and 'BUILD SUCCESSFUL' in log and ' failed!' not in log,path
    tests.append({'log':str(path),'required_passed':int(passed[1]),'sha256':digest(path)})
client=root/'docs/validation/sorcerer-repair-alpha15-client-20261002.log'
log=client.read_text(encoding='utf-8',errors='replace')
assert 'SORCERER_REPAIR_CAPTURE_COMPLETE' in log and 'BUILD SUCCESSFUL' in log
assert all('SORCERER_REPAIR_SCENE '+clip in log for clip in ('halobeam','shardhail','frozenblades','voidbeam','dashstrike','airdash'))
assert 'Exception in thread' not in log and 'Rendering entity in world' not in log
visual=root/'docs/validation/visual-repair-alpha15-client-20261002.log'
visual_log=visual.read_text(encoding='utf-8',errors='replace')
assert 'VISUAL_REPAIR_CAPTURE_COMPLETE' in visual_log and 'VISUAL_VARIANT_ASSERTIONS_PASS' in visual_log and 'BUILD SUCCESSFUL' in visual_log
audit=json.loads((root/'docs/native-audit.json').read_text(encoding='utf-8'))
assert not audit['errors'] and audit['checks']['jar_sha256']==digest(jar)
source=root.parent/'_nyctocosm_source/datapack/data'
source_hashes={}
for filename,base in (('sorcerer-particle-import-20261002.json',source),('sorcerer-beam-import-20261002.json',source),('rig-variant-import-20261002.json',source/'animated_java/functions')):
    report=json.loads((root/'docs/validation'/filename).read_text())
    for group in ([report] if 'source_sha256' in report else report.values()):
        for relative,expected in group['source_sha256'].items():
            path=base/relative;assert path.is_file() and digest(path)==expected,(filename,relative)
            source_hashes[str(path)]=expected
for folder in ('dashstrike','airdash','halobeam','voidbeam','shardhail','frozenblades','singularitycharge'):
    for path in (source/'sorcerer/functions/attacks'/folder).rglob('*.mcfunction'):source_hashes[str(path)]=digest(path)
with zipfile.ZipFile(jar) as archive:
    entries=archive.namelist()
    assert not [n for n in entries if 'NativeGameTests' in n or 'ClientSmoke' in n or n.endswith('.mcfunction')]
    classes=[int.from_bytes(archive.read(n)[6:8],'big') for n in entries if n.endswith('.class')]
    assert max(classes)<=61
    for name in ('SorcererHail','FrozenBlade','SorcererBeamNode','SorcererDash','SorcererBeamEffects'):
        assert f'com/flubberschnub/nyctocosm/{name}.class' in entries,name
    assert 'assets/nyctocosm/effects/sorcerer_beam_particles.json' in entries
    assert '0.3.0-native-alpha.15' in archive.read('META-INF/mods.toml').decode()
prior={}
for name,expected in (('13','1fd0e3caaac1cdcc3b7647bc9e93f92b88c0f3e4875af58024cfabfe124c604c'),('14','d1ac9901cfde0b4b19dda39cdfdd4e72cdc0fd0bccfba1704ca0c12b66e4c55f')):
    path=root/f'build/libs/nyctocosm-native-0.3.0-native-alpha.{name}.jar';assert digest(path)==expected
    prior[name]=digest(path)
manifest={'version':version,'jar':str(jar),'bytes':jar.stat().st_size,'sha256':digest(jar),'entries':len(entries),'java_class_major_versions':sorted(set(classes)),
    'tests':tests,'client_log':str(client),'client_log_sha256':digest(client),'screenshots':str(root/'run-sorcerer-repair-alpha15-client-20261002/screenshots'),
    'visual_client_log':str(visual),'visual_client_log_sha256':digest(visual),'visual_screenshots':str(root/'run-visual-repair-alpha15-client-20261002/screenshots'),
    'source_sha256':source_hashes,'implementation_sha256':{str(p.relative_to(root)):digest(p) for p in (root/'src/main/java').rglob('*.java')},'previous_jars_sha256':prior,'audit':audit,
    'pending':['D28 damage-cancellation status choice for unapproved spells','original portal shader/settings reproduction','real survival/multiplayer/chunk unload/restart/shader/FPS acceptance','existing fire/party/altar/scythe choices and other unclosed D items']}
(evidence/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:manifest[k] for k in ('version','jar','bytes','sha256','entries')},ensure_ascii=False,indent=2))
