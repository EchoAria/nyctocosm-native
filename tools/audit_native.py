"""Reproducible static checks for converted assets and the optionally built release jar."""
from pathlib import Path
import gzip
import hashlib
import io
import json
import math
import re
import sys
import zipfile

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
sys.path.insert(0,str(ROOT/'tools'))
from nbt_assets import number as nbt_number, payload as nbt_payload, string as nbt_string
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
skeletal_rigs=0
damage_locator_bundles=0
damage_locator_frames=0
for path in rigs:
    with gzip.open(path,'rt',encoding='utf-8') as handle: rig=json.load(handle)
    if rig.get('schema')==1 and 'locators' in rig and 'animations' in rig:
        damage_locator_bundles+=1
        locator_names=set(rig['locators'])
        required_locators={'center','leftmiddlewingtip0','rightmiddlewingtip0','rightupperwingtip1','leftupperwingtip1','rightmiddlewingtip1','leftmiddlewingtip1','rightlowerwingtip1','leftlowerwingtip1','righthand','lefthand','rightmiddlewingtip2','rightupperwingtip2','leftmiddlewingtip2','leftupperwingtip2','rightmiddlewingtip3','leftupperwingtip3','rightupperwingtip3'}
        check(len(locator_names)==len(rig['locators']) and locator_names==required_locators,f'Invalid Emissary locator set: {path.name}')
        check(bool(re.fullmatch(r'[0-9A-F]{64}',rig.get('sourceSha256',''))),f'Invalid source digest: {path.name}')
        check(rig.get('sourceFileCount',0)>0,f'Missing source file count: {path.name}')
        check(len(rig['animations'])>=40,f'Incomplete damage locator animation set: {path.name}')
        for animation,metadata in rig['animations'].items():
            keyframes=metadata.get('frames',[])
            damage_locator_frames+=len(keyframes)
            ticks=[frame.get('tick') for frame in keyframes]
            check(ticks==sorted(set(ticks)),f'Unordered/duplicate locator frame: {path.name}/{animation}')
            for frame in keyframes:
                check(set(frame.get('locators',{}))==locator_names,f'Incomplete locator frame: {path.name}/{animation}/{frame.get("tick")}')
                for name,values in frame.get('locators',{}).items():
                    check(len(values)==5 and all(math.isfinite(value) for value in values),f'Invalid locator transform: {path.name}/{animation}/{name}')
        continue
    skeletal_rigs+=1
    if path.name=='ajemissary.json.gz':
        guarded=rig.get('animations',{}).get('phase1_idle_guarded',[])
        check(len(guarded)==61 and [frame['tick'] for frame in guarded]==list(range(61)),'Incomplete guarded idle asset')
        check(not any(name.startswith('feedback_guard_') for name in rig['animations']),'Temporary guarded idle probes shipped')
    bones={bone['name'] for bone in rig['bones']}
    for bone in rig['bones']:
        check(exists(bone['model'],'models','.json'),f'Missing bone model: {path.name}/{bone["name"]}')
        check(len(bone['transform'])==16 and all(math.isfinite(n) for n in bone['transform']),f'Invalid rest matrix: {path.name}')
    for variant,models in rig.get('variants',{}).items():
        for bone,model in models.items():
            check(bone in bones and exists(model,'models','.json'),f'Invalid variant model: {path.name}/{variant}/{bone}')
    for animation,keyframes in rig.get('variant_keyframes',{}).items():
        check(animation in rig['animations'],f'Unknown variant animation: {path.name}/{animation}')
        check([f['tick'] for f in keyframes]==sorted(set(f['tick'] for f in keyframes)),f'Unordered variant frames: {path.name}/{animation}')
        for frame in keyframes:
            check(all(name in rig.get('variants',{}) for name in frame['variants']),f'Unknown variant cue: {path.name}/{animation}/{frame["tick"]}')
    for animation,keyframes in rig['animations'].items():
        frames+=len(keyframes)
        check([f['tick'] for f in keyframes]==sorted(set(f['tick'] for f in keyframes)),f'Unordered/duplicate frame: {path.name}/{animation}')
        for frame in keyframes:
            for name,matrix in frame['bones'].items():
                check(name in bones,f'Unknown bone: {path.name}/{name}')
                check(len(matrix)==16 and all(math.isfinite(n) for n in matrix),f'Invalid animation matrix: {path.name}/{animation}')
checks.update(rigs=skeletal_rigs,animation_frames=frames,damage_locator_bundles=damage_locator_bundles,damage_locator_frames=damage_locator_frames)
particle_asset=RES/'assets/nyctocosm/effects/sorcerer_keyframes.json'
particle_rows=0
for animations in json.loads(particle_asset.read_text()).values():
    for keyframes in animations.values():
        for cues in keyframes.values():
            particle_rows+=len(cues)
            for cue in cues:
                check(all(len(cue[key])==3 and all(math.isfinite(n) for n in cue[key]) for key in ('local','world','delta')),'Invalid compiled particle vector')
                check(cue['count']>=0 and math.isfinite(cue['speed']) and cue['speed']>=0,'Invalid compiled particle count/speed')
checks['sorcerer_particle_rows']=particle_rows
feedback_asset=RES/'assets/nyctocosm/effects/feedback_keyframes.json'
feedback=json.loads(feedback_asset.read_text())['feedback']
feedback_counts={clip:sum(map(len,frames.values())) for clip,frames in feedback.items()}
check(feedback_counts=={'plane':1000,'radiance':722,'resistance':1,'flight':10,'flight_step':4,'divine_ray':27,'god_ray':3,'god_signal':1,'ray_impact':3,'meteor_ring':1130,'meteor_ring_small':400,'corkscrew':72},'Incomplete focus/flight source particles')
for frames in feedback.values():
    for cues in frames.values():
        for cue in cues:
            check(all(len(cue[key])==3 and all(math.isfinite(n) for n in cue[key]) for key in ('local','world','delta')),'Invalid feedback particle vector')
            check(cue['count']>=0 and math.isfinite(cue['speed']) and cue['speed']>=0,'Invalid feedback particle count/speed')
checks['feedback_particle_rows']=sum(feedback_counts.values())
beam_particles=json.loads((RES/'assets/nyctocosm/effects/sorcerer_beam_particles.json').read_text())
check({key:len(value) for key,value in beam_particles.items()}=={'halobeaminitial':35,'halobeamfinal':70,'halobeambeam':38},'Incomplete Sorcerer beam helper geometry')
for cues in beam_particles.values():
    for cue in cues:
        check(isinstance(cue['particle'],str) and bool(cue['particle']) and len(cue['local'])==3 and all(math.isfinite(n) for n in cue['local']),'Invalid numeric beam particle')
checks['sorcerer_beam_particle_rows']=sum(map(len,beam_particles.values()))
sounds=json.loads((RES/'assets/nyctocosm/sounds.json').read_text(encoding='utf-8-sig'))
for key,value in sounds.items():
    for sound in value['sounds']:
        if isinstance(sound,dict) and sound.get('type')=='event':continue
        name=sound if isinstance(sound,str) else sound['name']
        check(exists(name,'sounds','.ogg'),f'Missing sound: {key} -> {name}')
checks['sound_events']=len(sounds)
check(not list(RES.rglob('*.mcfunction')),'Runtime mcfunctions found')
for path in (ROOT/'src/main/java').rglob('*.java'):
    # These development-only classes are independently forbidden in the release JAR below.
    if path.name.startswith(('NativeGameTests','ClientSmoke')):
        continue
    check(not re.search(r'performPrefixedCommand|performCommand|CommandFunction|\.getFunctions\(',path.read_text(encoding='utf-8')),f'Command runtime dependency: {path.name}')
for path in (RES/'data').glob('*/tags/functions/*.json'):
    check(path.stem not in ('load','tick'),f'Datapack tick/load hook: {path}')
checks['runtime_mcfunctions']=0
structure_events={
    'data/structures/structures/crypt/hallways/generic/end.nbt':'nyctocosm:crypt_slatehenge_trigger',
    'data/nyctocosm/structures/slatehenge.nbt':'nyctocosm:slatehenge_effect',
}
for relative,expected_id in structure_events.items():
    resource=RES/relative
    check(resource.is_file(),f'Missing native structure event resource: {relative}')
    if not resource.is_file():continue
    stream=io.BytesIO(gzip.decompress(resource.read_bytes()))
    root_type=nbt_number(stream,'B');nbt_string(stream);root=nbt_payload(stream,root_type)
    entities=root.get('entities',(9,(10,[])))[1][1]
    matching=[entity for entity in entities if entity.get('nbt',(10,{}))[1].get('id',(8,''))[1]==expected_id]
    check(len(matching)==1,f'Expected one {expected_id} event in {relative}')
    if matching:
        event_nbt=matching[0].get('nbt',(10,{}))[1]
        check(set(event_nbt)=={'id'},f'Unexpected executable or source tags retained by {expected_id}')
checks['native_structure_events']=len(structure_events)
if len(sys.argv)>1:
    jar=Path(sys.argv[1])
    with zipfile.ZipFile(jar) as archive:
        names=archive.namelist()
        check('META-INF/mods.toml' in names,'Missing mod manifest')
        expected_version=re.search(r"^version\s*=\s*'([^']+)'",(ROOT/'build.gradle').read_text(encoding='utf-8'),re.M).group(1)
        manifest=archive.read('META-INF/mods.toml').decode('utf-8')
        check(f'version="{expected_version}"' in manifest,'Packaged mod version does not match build.gradle')
        checks['mod_version']=expected_version
        forbidden=[name for name in names if name.endswith('.mcfunction') or 'NativeGameTests' in name or 'ClientSmoke' in name or name in ('data/nyctocosm/structures/empty.nbt','data/nyctocosm/structures/portal_test.nbt')]
        check(not forbidden,f'Development/datapack execution content shipped: {forbidden}')
        locator_resource=RES/'assets/nyctocosm/native_rigs/emissary_damage_locators.json.gz'
        locator_entry=locator_resource.relative_to(RES).as_posix()
        check(locator_entry in names,'Emissary damage locator resource missing from release JAR')
        if locator_entry in names:
            check(archive.read(locator_entry)==locator_resource.read_bytes(),'Packaged Emissary damage locator resource differs from source resource')
        particle_entry=particle_asset.relative_to(RES).as_posix()
        check(particle_entry in names,'Compiled Sorcerer particles missing from release JAR')
        if particle_entry in names:check(archive.read(particle_entry)==particle_asset.read_bytes(),'Packaged Sorcerer particles differ from source resource')
        feedback_entry=feedback_asset.relative_to(RES).as_posix()
        check(feedback_entry in names,'Focus/flight particles missing from release JAR')
        if feedback_entry in names:check(archive.read(feedback_entry)==feedback_asset.read_bytes(),'Packaged feedback particles differ from source resource')
        for repair_name in ('combat_repair_keyframes.json','combat_repair_events.json'):
            repair_entry='assets/nyctocosm/effects/'+repair_name
            check(repair_entry in names,f'Combat repair resource missing from release JAR: {repair_name}')
            if repair_entry in names:check(archive.read(repair_entry)==(RES/repair_entry).read_bytes(),f'Packaged combat repair resource differs from source: {repair_name}')
        for carrier in ('EmissarySeekingMissile','BossEffectTimeline','EmissarySwirlEffects','SerpentAmbientEffects','EmissaryAppearance','client/FocusEffectController','NativeNetwork$SourceVisual','NativeNetwork$EmissaryRay'):
            check(f'com/flubberschnub/nyctocosm/{carrier}.class' in names,f'Missing feedback production class {carrier}')
        for carrier in ('BossVisuals','SerpentBody','client/SerpentWorldRenderer','client/RigLibrary'):
            check(f'com/flubberschnub/nyctocosm/{carrier}.class' in names,f'Missing alpha19 production class {carrier}')
        guarded_entry='assets/nyctocosm/native_rigs/ajemissary.json.gz'
        check(guarded_entry in names,'Guarded idle rig missing from release JAR')
        if guarded_entry in names:check(archive.read(guarded_entry)==(RES/guarded_entry).read_bytes(),'Packaged guarded rig differs from source resource')
        beam_entry='assets/nyctocosm/effects/sorcerer_beam_particles.json'
        check(beam_entry in names,'Sorcerer beam particles missing from release JAR')
        if beam_entry in names:check(archive.read(beam_entry)==(RES/beam_entry).read_bytes(),'Packaged beam particles differ from source resource')
        for carrier in ('SorcererHail','FrozenBlade','SorcererBeamNode','SorcererDash'):
            check(f'com/flubberschnub/nyctocosm/{carrier}.class' in names,f'Missing Sorcerer production carrier {carrier}')
        for relative in structure_events:
            check(relative in names,f'Native structure event template missing from release JAR: {relative}')
            if relative in names:
                check(archive.read(relative)==(RES/relative).read_bytes(),f'Packaged structure event template differs from source resource: {relative}')
        checks['jar_entries']=len(names)
        checks['jar']=str(jar.resolve())
        checks['jar_sha256']=hashlib.sha256(jar.read_bytes()).hexdigest()
        checks['jar_bytes']=jar.stat().st_size
report={'checks':checks,'errors':errors}
(ROOT/'docs/native-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(bool(errors))
