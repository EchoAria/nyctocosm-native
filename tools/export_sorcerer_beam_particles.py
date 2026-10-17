"""Compile three reachable beam particle helpers as numeric data, never runtime commands."""
import hashlib, json, re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=root.parent/'_nyctocosm_source/datapack/data'
coordinate=r'\^(?:-?\d+(?:\.\d+)?)?'
pattern=re.compile(r'particle (.+?) ('+coordinate+r') ('+coordinate+r') ('+coordinate+r') 0 0 0 0 1(?: (force|normal))?$')
hashes={}
def rows(file,offset=(0,0,0)):
    hashes[file.relative_to(source).as_posix()]=hashlib.sha256(file.read_bytes()).hexdigest()
    result=[]
    for line in file.read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'): continue
        if line.startswith('particle '):
            match=pattern.fullmatch(line);assert match,(file,line)
            particle,*values,force=match.groups()
            result.append({'particle':particle,'local':[offset[i]+float(value[1:] or 0) for i,value in enumerate(values)],'force':force=='force'})
        else:
            call=re.fullmatch(r'execute positioned \^ \^(-?\d+(?:\.\d+)?) \^ run function effects:sorcerer/__generated__/block/(\d+)',line)
            assert call,(file,line)
            result+=rows(source/f'effects/functions/sorcerer/__generated__/block/{call[2]}.mcfunction',(offset[0],offset[1]+float(call[1]),offset[2]))
    return result
result={name:rows(source/f'effects/functions/sorcerer/{name}.mcfunction') for name in ('halobeaminitial','halobeamfinal','halobeambeam')}
target=root/'src/main/resources/assets/nyctocosm/effects/sorcerer_beam_particles.json'
target.write_text(json.dumps(result,separators=(',',':'))+'\n',encoding='utf-8')
for attack in ('shardhail','frozenblades','halobeam','voidbeam'):
    for file in (source/f'sorcerer/functions/attacks/{attack}').rglob('*.mcfunction'):
        hashes[file.relative_to(source).as_posix()]=hashlib.sha256(file.read_bytes()).hexdigest()
report={'counts':{key:len(value) for key,value in result.items()},'source_sha256':hashes,'output_sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
(root/'docs/validation/sorcerer-beam-import-20261002.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(report['counts'])
