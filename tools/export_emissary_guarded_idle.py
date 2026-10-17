"""Player-requested guarded idle: source entrance arms75 and idle's shield wings0.

Retain the existing imported clips and light source idle breathing. No game commands.
"""
from pathlib import Path
import gzip,json,hashlib,re

ROOT=Path(__file__).resolve().parents[1]
rig=ROOT/'src/main/resources/assets/nyctocosm/native_rigs/ajemissary.json.gz'
data=json.loads(gzip.decompress(rig.read_bytes()))
tree=ROOT.parent/'_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations'
sources={}
for clip,frame in [('spawn',75),('phase1_idle',0)]:
    p=tree/clip/'tree'/f'leaf_{frame}_as_bone.mcfunction'
    sources[str(p.relative_to(ROOT.parent))]=hashlib.sha256(p.read_bytes()).hexdigest()
    imported=data['animations'][clip][frame]['bones']
    for name,raw in re.findall(r'tag=aj\.[^.]+\.bone\.([^],]+).*?transformation:\[([^]]+)\]',p.read_text(encoding='utf-8-sig')):
        actual=[round(float(v.rstrip('fFdD')),7) for v in raw.split(',')]
        assert actual==imported[name],(clip,frame,name)

def multiply(a,b):
    return [sum(a[r*4+k]*b[k*4+c] for k in range(4)) for r in range(4) for c in range(4)]
def inverse(a):
    rows=[a[r*4:r*4+4]+[float(r==c) for c in range(4)] for r in range(4)]
    for c in range(4):
        pivot=max(range(c,4),key=lambda r:abs(rows[r][c]));rows[c],rows[pivot]=rows[pivot],rows[c]
        factor=rows[c][c];rows[c]=[v/factor for v in rows[c]]
        for r in range(4):
            if r!=c:
                factor=rows[r][c];rows[r]=[x-factor*y for x,y in zip(rows[r],rows[c])]
    return [v for row in rows for v in row[4:]]

fold=data['animations']['spawn'][75]['bones']
idle=data['animations']['phase1_idle']
anchor=multiply(idle[0]['bones']['torso'],inverse(fold['torso']))
closed={bone:multiply(anchor,pose) for bone,pose in fold.items()}
frames=[]
arms={'rightarm','rightforearm','righthand','leftarm','leftforearm','lefthand'}
carried={}
for frame in idle:
    bob=frame['bones']['torso'][7]-idle[0]['bones']['torso'][7]
    carried.update(frame['bones']);poses={bone:pose.copy() for bone,pose in carried.items()}
    for bone in poses:
        if bone in arms or 'wing' in bone:
            pose=(closed[bone] if bone in arms else idle[0]['bones'][bone]).copy()
            pose[7]+=bob;poses[bone]=[round(v,7) for v in pose]
    frames.append({'tick':frame['tick'],'bones':poses})
for name in list(data['animations']):
    if name.startswith('feedback_guard_'):del data['animations'][name]
data['animations']['phase1_idle_guarded']=frames
with gzip.GzipFile(filename=str(rig),mode='wb',mtime=0) as stream:
    stream.write(json.dumps(data,separators=(',',':')).encode())
out=ROOT/'docs/validation/feedback-alpha19-20261004/guarded-idle-import.json'
out.write_text(json.dumps({'source_sha256':sources,'source_arm_pose':'spawn:75','source_wing_pose':'phase1_idle:0','anchor':'phase1_idle:0 torso','frames':len(frames),'bones':len(poses),'presentation_exception_requested_by_user':True,'asset_sha256':hashlib.sha256(rig.read_bytes()).hexdigest()},indent=2)+'\n')
print('Imported guarded idle',len(frames),'frames',len(closed),'bones')
