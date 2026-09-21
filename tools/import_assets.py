"""Offline conversion only. No mcfunction, scoreboard or command interpreter is shipped.

Retains artist geometry and sampled bone transforms as declarative animation data.
Run from this project with: python tools/import_assets.py
"""
from pathlib import Path
import gzip
import hashlib
import json
import re
import shutil
import struct

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / '_nyctocosm_source'
PACK = SOURCE / 'datapack' / 'data'
ART = SOURCE / 'resources' / 'assets'
OUT = ROOT / 'src/main/resources'

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')

def model(item, cmd):
    data = json.loads((ART / 'minecraft/models/item' / (item + '.json')).read_text(encoding='utf-8-sig'))
    return next(o['model'] for o in data['overrides'] if o['predicate'].get('custom_model_data') == cmd)

ITEMS = {
    'nyctocosmic_ash': 'minecraft:item/sugar',
    'seelic_essence': model('black_dye', 1), 'null_splinter': model('black_dye', 2),
    'void_shard': model('black_dye', 3), 'nightmare_scale': model('black_dye', 4),
    'soul_bottle': model('carrot_on_a_stick', 1), 'planar_coil': model('carrot_on_a_stick', 2),
    'seelic_scythe': model('warped_fungus_on_a_stick', 1),
    'wyrmsliver': model('warped_fungus_on_a_stick', 2),
    'nullfissure': model('warped_fungus_on_a_stick', 10),
    'semblanceofdoom': model('warped_fungus_on_a_stick', 20),
}
ARTIFACTS = ['transposition','amelioration','levitation','absorption','accumulation',
             'protection_dream','protection_radiance','protection_resonance',
             'protection_nightmare','protection_void','protection_dissonance','expulsion','extraction']
for index, name in enumerate(ARTIFACTS, 1):
    ITEMS['artifact_' + name] = model('red_dye', index)
for name, cmd in [('ancient_journals',1),('catechism',2),('dreams',4),('axiotopology',5),
                  ('adherent_serpent',3),('adherent_sorcerer',3),('adherent_emissary',3)]:
    ITEMS[name] = model('written_book', cmd)

def import_art():
    copied = 0
    for src in ART.rglob('*'):
        if not src.is_file():
            continue
        rel = src.relative_to(ART)
        # Unreferenced legacy geometry rejected by vanilla's [-16,32] element bounds.
        if rel.as_posix()=='nyctocosm/models/mobs/death_walker/rig/thigh.json':
            continue
        # Native items do not change the appearance or behavior of vanilla items.
        if rel.parts[:3] == ('minecraft','models','item') and src.suffix == '.json':
            if 'overrides' in json.loads(src.read_text(encoding='utf-8-sig')):
                continue
        if rel.parts[:2] == ('minecraft','lang'):
            continue
        dst = OUT / 'assets' / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        copied += 1
    for name, parent in ITEMS.items():
        obj = {'parent': parent}
        if name in ('wyrmsliver', 'nullfissure', 'semblanceofdoom', 'planar_coil'):
            vanilla, cmd = ('carrot_on_a_stick',3) if name == 'planar_coil' else ('warped_fungus_on_a_stick', {'wyrmsliver':3,'nullfissure':11,'semblanceofdoom':21}[name])
            obj['overrides'] = [{'predicate':{'nyctocosm:casting':1},'model':model(vanilla,cmd)}]
        write_json(OUT / 'assets/nyctocosm/models/item' / (name+'.json'), obj)
    for langfile in (ART / 'minecraft/lang').glob('en_us.json'):
        data = json.loads(langfile.read_text(encoding='utf-8-sig'))
        data = {k:v for k,v in data.items() if not any(k.startswith(prefix) for prefix in ('item.minecraft.','block.minecraft.','entity.minecraft.'))}
        for name in ITEMS:
            data.setdefault('item.nyctocosm.'+name, name.replace('_',' ').title())
        data['item.nyctocosm.planar_coil']='Planar Coil'
        data['item.nyctocosm.semblanceofdoom']='Semblance of Doom'
        for name in ARTIFACTS:
            data['item.nyctocosm.artifact_'+name]=data.get('artifact.'+name,name.replace('_',' ').title())
        data.update({'itemGroup.nyctocosm':'Nyctocosm Native','entity.nyctocosm.serpent':'Nightmare Serpent',
                     'entity.nyctocosm.sorcerer':'The Sorcerer','entity.nyctocosm.emissary':'The Emissary',
                     'entity.nyctocosm.death_walker':'Death Walker','entity.nyctocosm.nightmare_apparition':'Nightmare Apparition'})
        write_json(OUT / 'assets/nyctocosm/lang' / langfile.name, data)
    import_sounds()
    from localization import export
    export()
    return copied

def import_sounds():
    sounds=json.loads((ART/'nyctocosm/sounds.json').read_text(encoding='utf-8-sig'))
    repaired=[];missing=[]
    for event,definition in sounds.items():
        available=[]
        for sound in definition['sounds']:
            if isinstance(sound,dict) and sound.get('type')=='event':
                available.append(sound);continue
            name=sound if isinstance(sound,str) else sound['name']
            namespace,path=name.split(':',1) if ':' in name else ('minecraft',name)
            if namespace=='minecraft' or (ART/namespace/'sounds'/(path+'.ogg')).is_file():
                available.append(sound);continue
            corrected=re.sub(r'rumble_-(\d)$',r'rumble_-0\1',path)
            if corrected!=path and (ART/namespace/'sounds'/(corrected+'.ogg')).is_file():
                replacement=namespace+':'+corrected
                available.append(replacement if isinstance(sound,str) else {**sound,'name':replacement})
                repaired.append({'event':event,'from':name,'to':replacement})
            else:
                # The source pack itself has no recording. Preserve the event as silent,
                # rather than inventing an unrelated sound or shipping a broken resource path.
                missing.append({'event':event,'missing_source_file':name})
        definition['sounds']=available
    write_json(OUT/'assets/nyctocosm/sounds.json',sounds)
    write_json(ROOT/'docs/sound-import.json',{'repaired_paths':repaired,'silent_missing_source_recordings':missing})

def matrices(raw):
    return [round(float(x.rstrip('fFdD')),7) for x in raw.split(',')]

def import_rigs():
    result = {}
    pattern = re.compile(r'"aj\.([^.]+)\.bone\.([^" ]+)"\],transformation:\[([^]]+)\].*?item:\{id:"minecraft:([^" ]+)".*?CustomModelData:(\d+)', re.S)
    pose_pattern = re.compile(r'tag=aj\.[^.]+\.bone\.([^],]+).*?transformation:\[([^]]+)\]')
    for rig in (PACK / 'animated_java/functions').iterdir():
        summon = rig / 'summon.mcfunction'
        if not summon.exists():
            continue
        bones = []
        for _, name, mat, item, cmd in pattern.findall(summon.read_text(encoding='utf-8-sig')):
            bones.append({'name':name,'model':model(item,int(cmd)),'source_item':item,'transform':matrices(mat)})
        if not bones:
            continue
        animations = {}
        tree = rig / 'zzzzzzzz/animations'
        for folder in sorted(tree.iterdir()) if tree.exists() else []:
            frames = []
            for leaf in folder.rglob('leaf_*_as_bone.mcfunction'):
                frame = int(re.search(r'leaf_(\d+)',leaf.name)[1])
                poses = {name:matrices(mat) for name, mat in pose_pattern.findall(leaf.read_text(encoding='utf-8-sig'))}
                if poses:
                    frames.append({'tick':frame,'bones':poses})
            if frames:
                animations[folder.name] = sorted(frames,key=lambda f:f['tick'])
        variants={}
        for variant in (rig/'zzzzzzzz/apply_variant').glob('*/as_bone.mcfunction'):
            changes={}
            for name, cmd in re.findall(r'tag=aj\.[^.]+\.bone\.([^],]+).*?item.tag.CustomModelData set value (\d+)',variant.read_text(encoding='utf-8-sig')):
                bone=next((b for b in bones if b['name']==name),None)
                if bone:changes[name]=model(bone['source_item'],int(cmd))
            if changes:variants[variant.parent.name]=changes
        payload = {'bones':bones,'animations':animations,'variants':variants}
        dst = OUT / 'assets/nyctocosm/native_rigs' / (rig.name + '.json.gz')
        dst.parent.mkdir(parents=True,exist_ok=True)
        with gzip.GzipFile(filename=str(dst), mode='wb', mtime=0) as output:
            output.write(json.dumps(payload,separators=(',',':')).encode())
        result[rig.name] = {'bones':len(bones),'animations':{k:len(v) for k,v in animations.items()},'variants':list(variants)}
    write_json(OUT / 'assets/nyctocosm/native_rigs/index.json',list(result))
    return result

def import_world():
    count = 0
    for namespace in PACK.iterdir():
        for folder in ('dimension','dimension_type','worldgen'):
            source = namespace / folder
            for src in source.rglob('*.json'):
                dst = OUT / 'data' / namespace.name / folder / src.relative_to(source)
                dst.parent.mkdir(parents=True,exist_ok=True)
                shutil.copy2(src,dst)
                count += 1
    from nbt_assets import sanitize
    structures={}
    for namespace in PACK.iterdir():
        for src in (namespace/'structures').rglob('*.nbt'):
            rel=src.relative_to(PACK)
            structures[str(rel)]=sanitize(src,OUT/'data'/rel)
    write_json(ROOT/'docs/structure-import.json',structures)
    for src in (PACK/'nyctocosm/tags/blocks').glob('*.json'):
        dst=OUT/'data/nyctocosm/tags/blocks'/src.name;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst)
    for namespace in PACK.iterdir():
        if namespace.name=='minecraft':continue
        for src in (namespace/'loot_tables').rglob('*.json'):
            obj=json.loads(src.read_text(encoding='utf-8-sig'))
            def convert(value):
                if isinstance(value,dict):
                    if value.get('type')=='minecraft:item':
                        nbt=' '.join(f.get('tag','') for f in value.get('functions',[]) if f.get('function')=='minecraft:set_nbt')
                        cmd=re.search(r'CustomModelData\s*:\s*(\d+)',nbt)
                        vanilla=value.get('name','').removeprefix('minecraft:')
                        if cmd:
                            try:
                                parent=model(vanilla,int(cmd[1]))
                                target=next((k for k,v in ITEMS.items() if v==parent),None)
                                if target:
                                    value['name']='nyctocosm:'+target
                                    value['functions']=[f for f in value.get('functions',[]) if f.get('function')!='minecraft:set_nbt']
                            except (FileNotFoundError,StopIteration,KeyError):pass
                    for child in value.values():convert(child)
                elif isinstance(value,list):
                    for child in value:convert(child)
            convert(obj);write_json(OUT/'data'/namespace.name/'loot_tables'/src.relative_to(namespace/'loot_tables'),obj)
    return count

def import_native_data():
    # A small native GameTest structure, not an original command-bearing structure.
    def string(value):
        raw=value.encode();return struct.pack('>H',len(raw))+raw
    def tag(kind,name,payload):return bytes([kind])+string(name)+payload
    air=tag(8,'Name',string('minecraft:air'))+b'\0'
    nbt=b'\x0a\0\0'+tag(3,'DataVersion',struct.pack('>i',3465))
    nbt+=tag(9,'size',b'\x03'+struct.pack('>iiii',3,8,5,8))
    nbt+=tag(9,'palette',b'\x0a'+struct.pack('>i',1)+air)
    nbt+=tag(9,'blocks',b'\x0a'+struct.pack('>i',0))+tag(9,'entities',b'\x0a'+struct.pack('>i',0))+b'\0'
    empty=OUT/'data/nyctocosm/structures/empty.nbt';empty.parent.mkdir(parents=True,exist_ok=True)
    with gzip.GzipFile(filename=str(empty),mode='wb',mtime=0) as f:f.write(nbt)
    portal_test=empty.with_name('portal_test.nbt')
    with gzip.GzipFile(filename=str(portal_test),mode='wb',mtime=0) as f:f.write(nbt.replace(struct.pack('>iiii',3,8,5,8),struct.pack('>iiii',3,26,10,10)))
    for name in ('dream','radiance','resonance','nightmare','void','dissonance'):
        write_json(OUT / 'data/nyctocosm/damage_type' / (name+'.json'),
                   {'message_id':'nyctocosm.'+name,'scaling':'when_caused_by_living_non_player','exhaustion':0.1})
    lore=PACK / 'lore/functions/storylines'
    chapters={}
    for name,path in [('ancient_journals','journals/give'),('catechism','catechism/give'),('dreams','dreams/give'),('axiotopology','axiotopology/give'),
                      ('adherent_serpent','adherent/serpent'),('adherent_sorcerer','adherent/sorcerer'),('adherent_emissary','adherent/emissary')]:
        raw=(lore/(path+'.mcfunction')).read_text(encoding='utf-8-sig')
        nbt=re.search(r'give @s written_book(\{.*\})',raw)[1]
        dst=OUT / 'data/nyctocosm/lore' / (name+'.snbt')
        dst.parent.mkdir(parents=True,exist_ok=True)
        dst.write_text(nbt,encoding='utf-8')
        storyline=path.split('/')[0]
        entries=sorted((lore/storyline/'__generated__/block').glob('*.mcfunction'),key=lambda p:int(p.stem))
        if entries:
            chapters[name]=len(entries)
            for index,entry in enumerate(entries,1):
                content=entry.read_text(encoding='utf-8-sig')
                chapter=re.search(r'with written_book(\{.*\}) 1',content)[1]
                (dst.parent/(name+'_'+str(index)+'.snbt')).write_text(chapter,encoding='utf-8')
    write_json(OUT/'data/nyctocosm/lore/chapters.json',chapters)
    for name in ('serpent','sorcerer','emissary','death_walker','nightmare_apparition'):
        write_json(OUT/'assets/nyctocosm/models/item'/(name+'_spawn_egg.json'),{'parent':'minecraft:item/template_spawn_egg'})
    write_json(OUT/'assets/nyctocosm/blockstates/dais.json',{'variants':{'':{'model':'nyctocosm:magic/dais/dais_anvil'}}})
    write_json(OUT/'assets/nyctocosm/models/item/dais.json',{'parent':'nyctocosm:magic/dais/dais_anvil'})
    write_json(OUT/'data/nyctocosm/loot_tables/blocks/dais.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'nyctocosm:dais'}]}]})
    write_json(OUT/'assets/nyctocosm/blockstates/portal.json',{'variants':{'axis=x':{'model':'nyctocosm:block/portal'},'axis=z':{'model':'nyctocosm:block/portal','y':90}}})
    write_json(OUT/'assets/nyctocosm/models/block/portal.json',{'render_type':'minecraft:translucent','textures':{'portal':'nyctocosm:portal/portal_surface_upper','particle':'nyctocosm:portal/portal_surface_upper'},
        'elements':[{'from':[0,0,8],'to':[16,16,8],'faces':{'north':{'uv':[.25,.25,.75,.75],'texture':'#portal'},'south':{'uv':[.25,.25,.75,.75],'texture':'#portal'}}}]})
    for name,pattern,keys,result in [
        ('soul_bottle',[' G ','GSG',' G '],{'G':'minecraft:glass','S':'minecraft:soul_sand'},1),
        ('dais',[' A ','QBQ','QQQ'],{'A':'minecraft:anvil','Q':'minecraft:quartz_block','B':'minecraft:beacon'},1)]:
        # Bottle/dais convenience recipes; actual inculcation preserves source costs in DaisMenu.
        write_json(OUT/'data/nyctocosm/recipes'/(name+'.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in keys.items()},'result':{'item':'nyctocosm:'+name,'count':result}})
    zh={'itemGroup.nyctocosm':'Nyctocosm · 夜之宇宙','item.nyctocosm.soul_bottle':'灵魂瓶','item.nyctocosm.planar_coil':'位面线圈',
        'item.nyctocosm.seelic_scythe':'灵魂镰刀','item.nyctocosm.wyrmsliver':'龙鳞之刃','item.nyctocosm.nullfissure':'虚无裂隙','item.nyctocosm.semblanceofdoom':'末日之影'}
    zh['block.nyctocosm.dais']='灌注祭坛'
    for name in ITEMS:zh.setdefault('item.nyctocosm.'+name,name.replace('_',' ').title())
    for name in ('serpent','sorcerer','emissary','death_walker','nightmare_apparition'):
        zh['entity.nyctocosm.'+name]=name.replace('_',' ').title()
        zh['item.nyctocosm.'+name+'_spawn_egg']=name.replace('_',' ').title()+' 刷怪蛋'
    from localization import export
    export()

if __name__ == '__main__':
    import_native_data()
    report = {'source_sha256':hashlib.sha256((ROOT.parent/'nyctocosm.zip').read_bytes()).hexdigest(),
              'art_files':import_art(),'rigs':import_rigs(),'world_files':import_world(),
              'native_item_models':list(ITEMS),'runtime_mcfunctions':0}
    write_json(ROOT / 'docs/asset-import.json',report)
    print(json.dumps(report,indent=2))
