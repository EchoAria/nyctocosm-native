"""Audit this feedback release against its before.json, without changing old evidence."""
from pathlib import Path
import hashlib,json,re,struct,zipfile

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT.parent
OUT=ROOT/'docs/validation/feedback-alpha19-20261004'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
aggregate=lambda d:hashlib.sha256(json.dumps(d,sort_keys=True,separators=(',',':')).encode()).hexdigest()
baseline=json.loads((OUT/'before.json').read_text(encoding='utf-8'))
version=re.search(r"^version\s*=\s*'([^']+)'",(ROOT/'build.gradle').read_text(),re.M)[1]
jar=ROOT/'build/libs'/f'nyctocosm-native-{version}.jar'
final=(OUT/'flight-final2.log').read_text(encoding='utf-8',errors='replace')
assert re.search(r'All 288 required tests passed',final) and 'BUILD SUCCESSFUL' in final
assert 'failed!' not in final and 'BUILD FAILED' not in final
client=(OUT/'client-flight-fixed.log').read_text(encoding='utf-8',errors='replace')
assert 'FEEDBACK19_CAPTURE_COMPLETE' in client and 'BUILD SUCCESSFUL' in client
for error in ('Exception in thread','Rendering entity in world','Error executing task on Server','Error executing task on Client'):
    assert error not in client,error
current={p.relative_to(WORK).as_posix():sha(p) for p in (ROOT/'src/main').rglob('*') if p.is_file() and '__pycache__' not in p.parts}
current['nyctocosm-native/build.gradle']=sha(ROOT/'build.gradle')
before=baseline['production_sha256']
source={p.relative_to(WORK).as_posix():sha(p) for p in (WORK/'_nyctocosm_source').rglob('*') if p.is_file()}
assert len(source)==baseline['readonly_source']['files'] and aggregate(source)==baseline['readonly_source']['aggregate_sha256'],'Original source changed'
old={name:sha(ROOT/'build/libs'/name) for name in baseline['old_jars']}
assert old==baseline['old_jars'],'Old release jar changed'
imported=json.loads((OUT/'guarded-idle-import.json').read_text())
for name,digest in imported['source_sha256'].items():assert sha(WORK/name)==digest,name
with zipfile.ZipFile(jar) as archive:
    names=archive.namelist()
    assert not any('NativeGameTests' in n or 'ClientSmoke' in n or n.endswith('.mcfunction') for n in names)
    majors={struct.unpack('>H',archive.read(n)[6:8])[0] for n in names if n.endswith('.class')}
    assert majors=={61}
    for name in ('assets/nyctocosm/native_rigs/ajemissary.json.gz','assets/nyctocosm/effects/combat_repair_keyframes.json','assets/nyctocosm/effects/combat_repair_events.json'):
        assert archive.read(name)==(ROOT/'src/main/resources'/name).read_bytes(),name
audit=json.loads((ROOT/'docs/native-audit.json').read_text())
assert not audit['errors'] and audit['checks']['jar_sha256']==sha(jar)
screens=sorted((ROOT/'run-feedback19-flight-fixed-client-20261004/screenshots').glob('feedback19-*.png'))
assert screens,'No final client captures'
flight_events=[]
for line in client.splitlines():
    if 'FEEDBACK19_FLIGHT_ATTACK ' in line or 'FEEDBACK19_HIGH_RAIN_FIXTURE' in line or 'FEEDBACK19_RAIN_FIRE ' in line:flight_events.append(line)
(OUT/'flight-events.txt').write_text('\n'.join(flight_events)+'\n',encoding='utf-8')
source_paths=[
    'emissary/functions/ai/subtick.mcfunction',
    'emissary/functions/ai/__generated__/block/736.mcfunction',
    'emissary/functions/ai/__generated__/block/737.mcfunction',
    'emissary/functions/ai/phase/4/flytarget.mcfunction',
    'emissary/functions/ai/phase/4/fly.mcfunction',
    'emissary/functions/ai/phase/4/movement.mcfunction',
    'emissary/functions/attacks/phase4/choose.mcfunction',
    'emissary/functions/attacks/phase4/__generated__/block/661.mcfunction',
    'emissary/functions/attacks/phase4/__generated__/block/662.mcfunction',
    'emissary/functions/attacks/phase4/apocalypse_rain/__generated__/block/688.mcfunction',
    'emissary/functions/attacks/phase4/apocalypse_rain/fire.mcfunction',
    'emissary/functions/__generated__/block/10.mcfunction',
    'emissary/functions/attacks/phase4/decelerate.mcfunction',
]
animation_root=WORK/'_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations'
evidence={}
for clip in ('phase4_idle_transition_flying','phase4_flying_transition_idle','phase4.attack.apocalypse_rain'):
    for p in (animation_root/clip/'tree').glob('leaf_*.mcfunction'):
        if '_as_' not in p.name:evidence[p.relative_to(WORK).as_posix()]={'sha256':sha(p),'lines':p.read_text(encoding='utf-8-sig').splitlines()}
for path in source_paths:
    p=WORK/'_nyctocosm_source/datapack/data'/path
    evidence[p.relative_to(WORK).as_posix()]={'sha256':sha(p),'lines':p.read_text(encoding='utf-8-sig').splitlines()}
(OUT/'source-flight-evidence.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
manifest={
    'version':version,'protocol':6,'jar':{'path':str(jar),'bytes':jar.stat().st_size,'sha256':sha(jar),'entries':len(names),'class_major':sorted(majors)},
    'required_tests':288,'final_log_sha256':sha(OUT/'flight-final2.log'),'client_log_sha256':sha(OUT/'client-flight-fixed.log'),
    'production_changed':[k for k in sorted(set(current)&set(before)) if current[k]!=before[k]],
    'production_added':sorted(set(current)-set(before)),'production_removed':sorted(set(before)-set(current)),
    'production_sha256':current,'readonly_source':{'files':len(source),'aggregate_sha256':aggregate(source),'unchanged':True},
    'old_jars':old,'old_jars_unchanged':True,
    'guarded_idle_import_sha256':sha(OUT/'guarded-idle-import.json'),'source_flight_evidence_sha256':sha(OUT/'source-flight-evidence.json'),
    'screenshots':{str(p.relative_to(ROOT)):sha(p) for p in screens},
    'logs':{p.name:sha(p) for p in OUT.glob('*.log') if p.name!='package.log'},
    'development_fixture_sha256':{name:sha(ROOT/'src/main/java/com/flubberschnub/nyctocosm'/name) for name in ('NativeGameTestsFeedback19.java','ClientSmokeFeedback19.java')},
    'limits':['Isolated natural AI uses an immune flying player and a separate following camera; final high-altitude Rain is a scripted fixture','Guarded idle is a player-requested presentation adjustment derived from source poses','No original-pack/mod side-by-side full battle recording','No multiplayer/shader/FPS benchmark','No real dedicated-server restart or chunk unload integration','All 37 attacks have not reached full source parity'],
}
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(OUT/'jar-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
trace=ROOT/'build/validation/serpent-body-source-trace.csv'
if trace.is_file():
    (OUT/'serpent-body-source-trace.csv').write_bytes(trace.read_bytes())
print(json.dumps({'jar':manifest['jar'],'required_tests':288,'screenshots':len(screens),'source_unchanged':True,'old_jars_unchanged':True,'changed':manifest['production_changed'],'added':manifest['production_added']},ensure_ascii=False,indent=2))
