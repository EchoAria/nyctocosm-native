"""Save verified build evidence before the user's requested shutdown."""
from pathlib import Path
import hashlib,json,sys,zipfile

root=Path(__file__).resolve().parents[1]
stamp=sys.argv[1]
version='0.3.0-native-alpha.16'
jar=root/f'build/libs/nyctocosm-native-{version}.jar'
log=root/f'docs/validation/shutdown-build-alpha16-{stamp}.log'
digest=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
assert 'BUILD SUCCESSFUL' in log.read_text(encoding='utf-8',errors='replace')
audit=json.loads((root/'docs/native-audit.json').read_text(encoding='utf-8'))
assert audit['errors']==[] and audit['checks']['jar_sha256']==digest(jar)
previous=root/'build/libs/nyctocosm-native-0.3.0-native-alpha.15.jar'
assert digest(previous)=='7c9a89a5790962a7032ea32d8f82faace927306f4fad98cb50a7c964b037103d'
with zipfile.ZipFile(previous) as old,zipfile.ZipFile(jar) as new:
    entries=new.namelist()
    classes={n for n in entries if n.endswith('.class')}
    assert classes=={n for n in old.namelist() if n.endswith('.class')}
    changed=[n for n in sorted(classes) if old.read(n)!=new.read(n)]
    assert not changed,changed
    majors=sorted({int.from_bytes(new.read(n)[6:8],'big') for n in classes})
    assert majors==[61]
    assert not [n for n in entries if 'NativeGameTests' in n or 'ClientSmoke' in n or n.endswith('.mcfunction')]
    assert f'version="{version}"' in new.read('META-INF/mods.toml').decode('utf-8')
receipt={'version':version,'jar':str(jar),'bytes':jar.stat().st_size,'sha256':digest(jar),'jar_entries':len(entries),'java_class_major_versions':majors,
    'build_succeeded':True,'build_log':str(log),'build_log_sha256':digest(log),'audit_errors':audit['errors'],
    'production_class_bytes_identical_to_alpha15':True,'previous_alpha15_sha256':digest(previous),
    'gameplay_regression_evidence':'sorcerer-repair-alpha15-final2-20261002.log:248/248; source behavior unchanged; no new GameTest run claimed',
    'pending':'Existing D28/gameplay decisions and real survival/multiplayer/shader/FPS acceptance remain pending.',
    'shutdown':'User requested shutdown after successful build; separate helper requests graceful Windows shutdown after20 seconds.'}
path=root/f'docs/validation/SHUTDOWN_BUILD_ALPHA16_{stamp}.json'
assert not path.exists(),path
path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:receipt[k] for k in ('version','jar','bytes','sha256','production_class_bytes_identical_to_alpha15')},ensure_ascii=False,indent=2))
