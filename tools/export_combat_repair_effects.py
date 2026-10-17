"""Compile selected source combat cues. Source files are read only; no runtime functions."""
import hashlib,json,re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT.parent/'_nyctocosm_source/datapack/data'
COORD=r'[~^](?:-?\d+(?:\.\d+)?)?'
PARTICLE=re.compile(r'particle (.+?) ('+COORD+r') ('+COORD+r') ('+COORD+r') ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) (\d+)(?: (force|normal))?(?: @a)?$')
HASHES={}

def read(relative):
    file=SOURCE/relative;HASHES[relative]=hashlib.sha256(file.read_bytes()).hexdigest()
    return file.read_text(encoding='utf-8-sig').splitlines()

def vector(parts):return [float(p[1:] or 0) if p.startswith(('~','^')) else float(p) for p in parts]

def anchor(line):
    match=re.search(r'tag=aj\.ajemissary\.locator\.([a-z0-9]+)',line)
    return match[1] if match else 'center' if 'tag=center' in line else ''

def row(line):
    m=PARTICLE.search(line);assert m,line
    spec,x,y,z,dx,dy,dz,speed,count,mode=m.groups()
    local,world=[0,0,0],[0,0,0]
    prefix=line[:m.start()]
    for position in re.finditer(r'positioned ('+COORD+r') ('+COORD+r') ('+COORD+r')',prefix):
        dest=local if position[1].startswith('^') else world
        for i,v in enumerate(vector(position.groups())):dest[i]+=v
    dest=local if x.startswith('^') else world
    for i,v in enumerate(vector((x,y,z))):dest[i]+=v
    data={'particle':spec,'local':local,'world':world,'delta':vector((dx,dy,dz)),'delta_local':dx.startswith('^'),'speed':float(speed),'count':int(count),'force':mode=='force'}
    if anchor(prefix):data['locator']=anchor(prefix)
    rotated=re.search(r'rotated ('+COORD+r'|[-\d.]+) ('+COORD+r'|[-\d.]+)',prefix)
    if rotated:
        for axis,value in zip(('yaw','pitch'),rotated.groups()):
            data[axis+'_offset' if value.startswith('~') else axis+'_absolute']=float(value[1:] or 0) if value.startswith('~') else float(value)
    return data

def expand(relative):
    result=[]
    for line in read(relative):
        ref=re.search(r'function animated_java:(.+/leaf_\d+_effects_\d+)',line)
        if ref:result.extend(expand('animated_java/functions/'+ref[1]+'.mcfunction'))
        else:result.append(line)
    return result

animations={};events={}
for clip in ('phase1_transition','phase2_transition','phase3_transition','die'):
    frames={};timeline={}
    folder=SOURCE/'animated_java/functions/ajemissary/zzzzzzzz/animations'/clip/'tree'
    for leaf in sorted(folder.glob('leaf_*.mcfunction')):
        m=re.fullmatch(r'leaf_(\d+)\.mcfunction',leaf.name)
        if not m:continue
        frame=m[1];particles=[];sounds=[];helpers=[]
        for line in expand(leaf.relative_to(SOURCE).as_posix()):
            if 'particle ' in line:particles.append(row(line))
            if 'playsound ' in line:
                sound=re.search(r'playsound nyctocosm:([^ ]+) master @a\[distance=\.\.([\d.]+)\] ~ ~ ~ ([\d.]+)',line)
                assert sound,line
                sounds.append({'id':sound[1],'radius':float(sound[2]),'volume':float(sound[3]),'locator':anchor(line)})
            if 'function effects:emissary/death_swirl' in line:
                helpers.append({'clip':'repair_death_swirl','locator':'center','pitch':0})
            if 'function effects:emissary/crucify' in line:
                helpers.append({'clip':'repair_crucify','locator':anchor(line),'pitch':0})
                sounds.append({'id':'emissary.death.crucify','radius':300,'volume':50,'locator':anchor(line)})
        if particles:frames[frame]=particles
        if sounds or helpers:timeline[frame]={'sounds':sounds,'helpers':helpers}
    if frames:animations[clip]=frames
    events[clip]=timeline

animations['phase3.attack.ground_slam']={'7':[row(l) for l in read('emissary/functions/attacks/phase3/ground_slam/particles.mcfunction') if 'particle ' in l]}
helpers={
    'repair_seeker_idle':{'0':[row(l) for l in read('emissary/functions/attacks/phase3/seeking_missiles/__generated__/block/605.mcfunction') if 'particle ' in l]},
    'repair_crucify':{'0':[row(l) for rel in ('effects/functions/emissary/crucify.mcfunction','effects/functions/emissary/__generated__/block/82.mcfunction') for l in read(rel) if 'particle ' in l]},
    'repair_emissary_final':{'0':[row(l) for l in read('emissary/functions/__generated__/block/34.mcfunction') if 'particle ' in l]},
    'repair_voidplane':{str(age):[row(l) for l in read(f'effects/functions/shockwave/__generated__/block/{block}.mcfunction') if 'particle ' in l] for age,block in ((0,5),(3,6),(5,7))},
    'repair_transmittance':{str(age):[row(l) for l in read(f'effects/functions/shockwave/__generated__/block/{block}.mcfunction') if 'particle ' in l] for age,block in ((0,40),(3,41),(5,42))},
}
for rel in ('effects/functions/emissary/death_swirl.mcfunction','effects/functions/emissary/__generated__/block/83.mcfunction','effects/functions/emissary/__generated__/block/75.mcfunction','effects/functions/emissary/__generated__/block/76.mcfunction'):
    read(rel)
assets=ROOT/'src/main/resources/assets/nyctocosm/effects'
(assets/'combat_repair_keyframes.json').write_text(json.dumps({'ajemissary':animations,'combat_repair':helpers},separators=(',',':'))+'\n',encoding='utf-8')
(assets/'combat_repair_events.json').write_text(json.dumps(events,separators=(',',':'))+'\n',encoding='utf-8')
report={'source_sha256':HASHES,'particles':{name:sum(len(rows) for rows in frames.values()) for name,frames in animations.items()},'helpers':{name:sum(len(rows) for rows in frames.values()) for name,frames in helpers.items()},'sounds':{name:sum(len(e['sounds']) for e in frames.values()) for name,frames in events.items()},'death_swirl_calls':sum(len(e['helpers']) for e in events['die'].values())}
(ROOT/'docs/validation/boss-repair-alpha18-20261003/particle-import.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print({k:v for k,v in report.items() if k!='source_sha256'})
