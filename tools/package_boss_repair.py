"""Record alpha.18 build, source immutability and isolated test/client evidence."""
from pathlib import Path
import hashlib,json,re,struct,zipfile

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT.parent
OUT=ROOT/'docs/validation/boss-repair-alpha18-20261003'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
aggregate=lambda d:hashlib.sha256(json.dumps(d,sort_keys=True,separators=(',',':')).encode()).hexdigest()
baseline=json.loads((OUT/'before.json').read_text(encoding='utf-8'))
version=re.search(r"^version\s*=\s*'([^']+)'",(ROOT/'build.gradle').read_text(),re.M)[1]
jar=ROOT/'build/libs'/f'nyctocosm-native-{version}.jar'
final=(OUT/'final-counter.log').read_text(encoding='utf-8',errors='replace')
assert re.search(r'All 276 required tests passed',final) and 'BUILD SUCCESSFUL' in final
assert 'failed!' not in final and 'BUILD FAILED' not in final
client=(OUT/'client-verified.log').read_text(encoding='utf-8',errors='replace')
assert 'BOSS_REPAIR18_CAPTURE_COMPLETE' in client and 'BUILD SUCCESSFUL' in client
assert 'Exception in thread' not in client and 'Rendering entity in world' not in client and 'Error executing task on Server' not in client
current={p.relative_to(WORK).as_posix():sha(p) for p in (ROOT/'src/main').rglob('*') if p.is_file() and '__pycache__' not in p.parts and not p.name.startswith(('NativeGameTestsBossRepair','ClientSmokeBossRepair'))}
current['nyctocosm-native/build.gradle']=sha(ROOT/'build.gradle')
before=baseline['production_sha256']
source={p.relative_to(WORK).as_posix():sha(p) for p in (WORK/'_nyctocosm_source').rglob('*') if p.is_file()}
assert len(source)==baseline['readonly_source']['files'] and aggregate(source)==baseline['readonly_source']['aggregate_sha256'],'Original source changed'
old={name:sha(ROOT/'build/libs'/name) for name in baseline['old_jars']}
assert old==baseline['old_jars'],'Old release jar changed'
imported=json.loads((OUT/'particle-import.json').read_text())
for name,digest in imported['source_sha256'].items():assert sha(WORK/'_nyctocosm_source/datapack/data'/name)==digest,name
with zipfile.ZipFile(jar) as archive:
    names=archive.namelist()
    assert not any('NativeGameTests' in n or 'ClientSmoke' in n or n.endswith('.mcfunction') for n in names)
    majors={struct.unpack('>H',archive.read(n)[6:8])[0] for n in names if n.endswith('.class')}
    assert majors=={61}
    for name in ('combat_repair_keyframes.json','combat_repair_events.json'):
        entry='assets/nyctocosm/effects/'+name
        assert archive.read(entry)==(ROOT/'src/main/resources'/entry).read_bytes()
audit=json.loads((ROOT/'docs/native-audit.json').read_text())
assert not audit['errors'] and audit['checks']['jar_sha256']==sha(jar)
screens=sorted((ROOT/'run-boss-repair18-client-verified-20261003/screenshots').glob('boss-repair18-*.png'))
assert screens,'No client captures'
manifest={
    'version':version,'protocol':6,'jar':{'path':str(jar),'bytes':jar.stat().st_size,'sha256':sha(jar),'entries':len(names),'class_major':sorted(majors)},
    'required_tests':276,'final_log_sha256':sha(OUT/'final-counter.log'),'client_log_sha256':sha(OUT/'client-verified.log'),
    'production_changed':[k for k in sorted(set(current)&set(before)) if current[k]!=before[k]],
    'production_added':sorted(set(current)-set(before)),'production_removed':sorted(set(before)-set(current)),
    'production_sha256':current,'readonly_source':{'files':len(source),'aggregate_sha256':aggregate(source),'unchanged':True},
    'old_jars':old,'old_jars_unchanged':True,'source_import_files':len(imported['source_sha256']),
    'screenshots':{str(p.relative_to(ROOT)):sha(p) for p in screens},
    'logs':{p.name:sha(p) for p in OUT.glob('*.log') if p.name!='package.log'},
    'development_fixture_sha256':{name:sha(ROOT/'src/main/java/com/flubberschnub/nyctocosm'/name) for name in ('NativeGameTestsBossRepair.java','ClientSmokeBossRepair.java')},
    'limits':['Manually driven client scene, not natural survival combat','No multiplayer/shader/FPS benchmark','No real dedicated-server restart or chunk unload integration','All 37 attacks have not reached full source parity'],
}
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(OUT/'jar-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'jar':manifest['jar'],'required_tests':276,'screenshots':len(screens),'source_unchanged':True,'old_jars_unchanged':True,'changed':manifest['production_changed'],'added':manifest['production_added']},ensure_ascii=False,indent=2))
