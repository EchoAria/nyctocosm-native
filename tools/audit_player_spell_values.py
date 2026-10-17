"""Read-only comparison of all 17 spells against this source datapack's loaded defaults."""
import hashlib, json, re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=root.parent/'_nyctocosm_source/datapack/data/spells/functions/load.mcfunction'
defaults={}
for name,key,value in re.findall(r'scoreboard players set (\w+) s\.(damage|duration|cast_time) (\d+)',source.read_text(encoding='utf-8-sig')):
    defaults.setdefault(name.lower(),{})[key]=int(value)
native=(root/'src/main/java/com/flubberschnub/nyctocosm/Spell.java').read_text()
rows=[]
for name,reagent,damage,duration,cast in re.findall(r'(\w+)\(Items\.(\w+),(\d+),(\d+),(\d+),',native):
    actual={'damage':int(damage),'duration':int(duration),'cast_time':int(cast)}
    expected=defaults[name.lower()]
    rows.append({'spell':name.lower(),'reagent':reagent.lower(),'source':expected,'native':actual,'matches':actual==expected})
assert len(rows)==17
report={'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'spell_count':len(rows),'rows':rows,
    'errors':[r['spell'] for r in rows if not r['matches']],
    'scope':'Loaded default values only; projectile paths, dynamic powers and visual timelines require their own tests.'}
(root/'docs/validation/player-spell-values-20261003.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print({'spells':len(rows),'errors':report['errors']})
assert not report['errors']
