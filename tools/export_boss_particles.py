"""Compile direct Sorcerer animation particles into numeric data; no functions run in the mod."""
import hashlib, json, re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=root.parent/'_nyctocosm_source/datapack/data/animated_java/functions'
result, hashes, helpers = {}, {}, {}
coordinate=r'[~^](?:-?\d+(?:\.\d+)?)?'
pattern=re.compile(r'particle (.+?) ('+coordinate+r') ('+coordinate+r') ('+coordinate+r') ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) (\d+)(?: (force|normal))?$')
def vector(values): return [float(value[1:] or 0) for value in values]
for rig in ('ajsorcerer','ajsorcererm1','ajsorcererm2'):
    animations={}
    for folder in sorted((source/rig/'zzzzzzzz/animations').iterdir()):
        frames={}
        files=list((folder/'tree').glob('leaf_*_effects_*.mcfunction'))
        if rig=='ajsorcererm2' and folder.name=='die': files.append(folder/'tree/leaf_116.mcfunction')
        for keyframe in sorted(files):
            frame=re.fullmatch(r'leaf_(\d+)(?:_effects_\d+)?\.mcfunction',keyframe.name)[1]
            file=keyframe
            if file.name=='leaf_116.mcfunction':
                assert 'function sorcerer:deathpoof' in file.read_text()
                hashes[str(file.relative_to(source.parent.parent))]=hashlib.sha256(file.read_bytes()).hexdigest()
                file=source.parent.parent/'sorcerer/functions/deathpoof.mcfunction'
            for line in file.read_text(encoding='utf-8-sig').splitlines():
                if 'particle ' not in line:
                    if 'function effects:' in line: helpers.setdefault(rig+'/'+folder.name,[]).append(line)
                    continue
                match=pattern.search(line)
                assert match, (file,line)
                spec,x,y,z,dx,dy,dz,speed,count,force=match.groups()
                if spec=='item quartz{CustomModelData:5}': spec='item nyctocosm:sorcerer_void_particle_visual'
                position=re.search(r'positioned ('+coordinate+r') ('+coordinate+r') ('+coordinate+r')',line[:match.start()])
                local=vector(position.groups()) if position and position[1].startswith('^') else [0,0,0]
                world=vector(position.groups()) if position and position[1].startswith('~') else [0,0,0]
                offsets=vector((x,y,z));destination=local if x.startswith('^') else world
                for i in range(3): destination[i]+=offsets[i]
                row={'particle':spec,'local':local,'world':world,'delta':[float(dx),float(dy),float(dz)],'speed':float(speed),'count':int(count),'force':force=='force'}
                if folder.name=='dashstrike' and frame=='49' and 'execute at @s run particle' in line: row['post_move']=True
                frames.setdefault(frame,[]).append(row)
                hashes[str(file.relative_to(source.parent.parent))]=hashlib.sha256(file.read_bytes()).hexdigest()
        if rig=='ajsorcererm2' and folder.name=='airdash':
            helper=source.parent.parent/'effects/functions/sorcerer/dashfly.mcfunction'
            hashes[str(helper.relative_to(source.parent.parent))]=hashlib.sha256(helper.read_bytes()).hexdigest()
            for keyframe in files:
                if 'function effects:sorcerer/dashfly' not in keyframe.read_text(): continue
                frame=re.fullmatch(r'leaf_(\d+)(?:_effects_\d+)?\.mcfunction',keyframe.name)[1]
                for line in helper.read_text().splitlines():
                    if not line.startswith('particle '): continue
                    match=pattern.search(line);assert match,(helper,line)
                    spec,x,y,z,dx,dy,dz,speed,count,force=match.groups()
                    assert all(v=='0' for v in (dx,dy,dz,speed)),line
                    values=vector((x,y,z))
                    # The helper's pitch is 90; position is first offset in the boss's own frame.
                    at=[values[0],2-values[2],-2+values[1]]
                    frames.setdefault(frame,[]).append({'particle':spec,'local':at,'world':[0,0,0],'delta':[0,0,0],'speed':0,'count':int(count),'force':force=='force'})
        if rig=='ajsorcererm2' and folder.name=='airdash':
            helper=source.parent.parent/'effects/functions/sorcerer/dashfly2.mcfunction'
            hashes[str(helper.relative_to(source.parent.parent))]=hashlib.sha256(helper.read_bytes()).hexdigest()
            for z_origin in (-1,-3):
                for line in helper.read_text().splitlines():
                    if not line.startswith('particle '): continue
                    match=pattern.search(line);assert match,(helper,line)
                    spec,x,y,z,dx,dy,dz,speed,count,force=match.groups();values=vector((x,y,z))
                    assert all(v=='0' for v in (dx,dy,dz,speed)),line
                    frames.setdefault('20',[]).append({'particle':spec,'local':[values[0],2-values[2],z_origin+values[1]],'world':[0,0,0],'delta':[0,0,0],'speed':0,'count':int(count),'force':force=='force'})
            for frame in range(26,40):
                for spec,count in (('dust 0.741 0.000 1.000 1.5',15),('dust 0.0 0.000 0.000 1.5',20)):
                    frames.setdefault(str(frame),[]).append({'particle':spec,'local':[0,0,0],'world':[0,1,0],'delta':[1,1,1],'speed':0,'count':count,'force':True})
            for name in ('aim','dashtotarget','slam'):
                helper=source.parent.parent/f'sorcerer/functions/attacks/airdash/{name}.mcfunction'
                hashes[str(helper.relative_to(source.parent.parent))]=hashlib.sha256(helper.read_bytes()).hexdigest()
            # These command helpers are not direct rig particle rows.
            for spec,count,delta,force in (('flash',1,[0,0,0],True),):
                frames.setdefault('20',[]).append({'particle':spec,'local':[0,0,0],'world':[0,2,0],'delta':delta,'speed':0,'count':count,'force':force})
            for spec,count,delta in (('explosion_emitter',1,[0,0,0]),('flash',2,[.3,.3,.3])):
                frames.setdefault('40',[]).append({'particle':spec,'local':[0,0,0],'world':[0,1,0],'delta':delta,'speed':0,'count':count,'force':False})
        if rig=='ajsorcererm2' and folder.name=='die':
            # animated_java tick precedes effects tick; helper timer 1 fires in the creation frame.
            for delay,block in ((0,40),(3,41),(5,42)):
                helper=source.parent.parent/f'effects/functions/shockwave/__generated__/block/{block}.mcfunction'
                hashes[str(helper.relative_to(source.parent.parent))]=hashlib.sha256(helper.read_bytes()).hexdigest()
                for line in helper.read_text().splitlines():
                    if not line.startswith('particle '): continue
                    match=pattern.search(line);assert match,(helper,line)
                    spec,x,y,z,dx,dy,dz,speed,count,force=match.groups()
                    assert (x,y,z)==('~','~','~'),line
                    frames.setdefault(str(116+delay),[]).append({'particle':spec,'local':[0,3,-.6],'world':[0,0,0],
                        'delta':vector((dx,dy,dz)),'delta_local':True,'speed':float(speed),'count':int(count),'force':force=='force'})
        if frames: animations[folder.name]=frames
    result[rig]=animations
target=root/'src/main/resources/assets/nyctocosm/effects/sorcerer_keyframes.json'
target.write_text(json.dumps(result,separators=(',',':'))+'\n',encoding='utf-8')
for folder in ('soul_swirl','void_vein','void_swirl'):
    for file in (source.parent.parent/'effects/functions'/folder).rglob('*.mcfunction'):
        hashes[str(file.relative_to(source.parent.parent))]=hashlib.sha256(file.read_bytes()).hexdigest()
for relative in ('minecraft/tags/functions/tick.json','ajloadtick/functions/tick.mcfunction','effects/functions/tick.mcfunction',
                 'effects/predicates/soul_swirl/straight_chance.json','effects/predicates/soul_swirl/radius_raise.json'):
    file=source.parent.parent/relative
    hashes[relative]=hashlib.sha256(file.read_bytes()).hexdigest()
report={'source_sha256':hashes,'counts':{rig:{animation:sum(map(len,frames.values())) for animation,frames in animations.items()} for rig,animations in result.items()},'independent_helper_calls':helpers,
        'implemented_helpers':['void_swirl/single_xflat','sorcerer/dashfly','shockwave/transmittance_first','void_vein/explode','soul_swirl/explode'],
        'note':'Numeric keyframes plus pure client math; independent combat helpers are tracked separately, not declared complete.'}
(root/'docs/validation/sorcerer-particle-import-20261002.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(report['counts'])
