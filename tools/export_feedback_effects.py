"""Compile original focus, status and flight particles into values, without runtime commands."""
import hashlib, json, re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root.parent / '_nyctocosm_source/datapack/data'
coordinate = r'[~^](?:-?\d+(?:\.\d+)?)?'
pattern = re.compile(r'particle (.+?) ('+coordinate+r') ('+coordinate+r') ('+coordinate+r') ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) (\d+)(?: (force|normal))?$')
result, hashes = {}, {}
def vector(parts): return [float(p[1:] or 0) if p.startswith(('~','^')) else float(p) for p in parts]
def rows(relative):
    path = source / relative
    hashes[relative] = hashlib.sha256(path.read_bytes()).hexdigest()
    values = []
    for line in path.read_text(encoding='utf-8-sig').splitlines():
        if 'particle ' not in line: continue
        m = pattern.search(line); assert m, (relative,line)
        spec,x,y,z,dx,dy,dz,speed,count,mode = m.groups()
        local,world = [0,0,0],[0,0,0]
        position = re.search(r'positioned ('+coordinate+r') ('+coordinate+r') ('+coordinate+r')',line[:m.start()])
        if position:
            destination = local if position[1].startswith('^') else world
            destination[:] = vector(position.groups())
        destination = local if x.startswith('^') else world
        for i,value in enumerate(vector((x,y,z))): destination[i] += value
        values.append({'particle':spec,'local':local,'world':world,'delta':vector((dx,dy,dz)),
            'delta_local':dx.startswith('^'),'speed':float(speed),'count':int(count),'force':mode=='force'})
    return values
result['plane'] = {str(age):rows(f'effects/functions/shockwave/__generated__/block/{block}.mcfunction') for age,block in ((0,1),(3,2),(5,3))}
result['radiance'] = {str(age):rows('effects/functions/elements/radiance/transmutate/subtick.mcfunction') for age in range(5)}
result['radiance']['0'] += rows('effects/functions/elements/radiance/transmutate/play.mcfunction')
result['resistance'] = {'0':rows('spells/functions/resistance/__generated__/block/458.mcfunction')}
result['flight'] = {'0':rows('emissary/functions/ai/phase/4/__generated__/block/764.mcfunction')}
result['flight_step'] = {'0':rows('emissary/functions/ai/phase/4/__generated__/block/786.mcfunction')}
result['divine_ray'] = {'0':rows('emissary/functions/attacks/phase4/divine_blast/__generated__/block/717.mcfunction')}
result['god_ray'] = {'0':rows('emissary/functions/attacks/phase4/godrays/__generated__/block/676.mcfunction')}
result['god_signal'] = {'0':rows('emissary/functions/attacks/phase4/godrays/__generated__/block/681.mcfunction')}
result['ray_impact'] = {'0':rows('emissary/functions/attacks/phase4/godrays/__generated__/block/679.mcfunction')}
result['meteor_ring'] = {str(age):rows(f'effects/functions/shockwave/__generated__/block/{block}.mcfunction') for age,block in ((0,17),(3,18),(5,19))}
result['meteor_ring_small'] = {str(age):rows(f'effects/functions/shockwave/__generated__/block/{block}.mcfunction') for age,block in ((0,21),(3,22))}
# A source step advances the corkscrew score before its two directional particles.
path = source / 'emissary/functions/ai/phase/4/__generated__/block/768.mcfunction'
all_rows = rows(str(path.relative_to(source)).replace('\\','/'))
conditions = [int(re.search(r'matches (\d+) ',line)[1]) for line in path.read_text().splitlines() if 'particle ' in line]
result['corkscrew'] = {str(age):[row for row,condition in zip(all_rows,conditions) if condition==age] for age in range(36)}
target = root / 'src/main/resources/assets/nyctocosm/effects/feedback_keyframes.json'
target.write_text(json.dumps({'feedback':result},separators=(',',':'))+'\n',encoding='utf-8')
report={'source_sha256':hashes,'rows':{clip:sum(map(len,frames.values())) for clip,frames in result.items()}}
(root/'docs/validation/feedback-particle-import-20261003.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(report['rows'])
