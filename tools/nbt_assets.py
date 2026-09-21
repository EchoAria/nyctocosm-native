"""Typed NBT codec for offline structure sanitization; never used by the mod."""
import gzip
import io
import struct
FORMATS={1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}
def number(stream,fmt):return struct.unpack('>'+fmt,stream.read(struct.calcsize(fmt)))[0]
def string(stream):return stream.read(number(stream,'H')).decode('utf-8')
def payload(stream,kind):
    if kind in FORMATS:return number(stream,FORMATS[kind])
    if kind==8:return string(stream)
    if kind in (7,11,12):
        length=number(stream,'i');return (length,stream.read(length*{7:1,11:4,12:8}[kind]))
    if kind==9:
        child=number(stream,'B');length=number(stream,'i');return (child,[payload(stream,child) for _ in range(length)])
    if kind==10:
        result={}
        while (child:=number(stream,'B'))!=0:
            name=string(stream);result[name]=(child,payload(stream,child))
        return result
    raise ValueError('Unsupported NBT type '+str(kind))
def encode_string(value):
    raw=value.encode('utf-8');return struct.pack('>H',len(raw))+raw
def encode(kind,value):
    if kind in FORMATS:return struct.pack('>'+FORMATS[kind],value)
    if kind==8:return encode_string(value)
    if kind in (7,11,12):return struct.pack('>i',value[0])+value[1]
    if kind==9:return bytes([value[0]])+struct.pack('>i',len(value[1]))+b''.join(encode(value[0],x) for x in value[1])
    if kind==10:return b''.join(bytes([k])+encode_string(name)+encode(k,v) for name,(k,v) in value.items())+b'\0'
    raise ValueError(kind)
def sanitize(source,destination):
    stream=io.BytesIO(gzip.decompress(source.read_bytes()))
    kind=number(stream,'B');name=string(stream);root=payload(stream,kind)
    count=len(root.get('entities',(9,(10,[])))[1][1]);root['entities']=(9,(10,[]))
    forbidden=set()
    for i,block in enumerate(root.get('palette',(9,(10,[])))[1][1]):
        if block['Name'][1] in ('minecraft:command_block','minecraft:chain_command_block','minecraft:repeating_command_block','minecraft:structure_block'):
            forbidden.add(i);block.clear();block['Name']=(8,'minecraft:air')
    for block in root.get('blocks',(9,(10,[])))[1][1]:
        if block.get('state',(3,-1))[1] in forbidden:block.pop('nbt',None)
    destination.parent.mkdir(parents=True,exist_ok=True)
    with gzip.GzipFile(filename=str(destination),mode='wb',mtime=0) as f:f.write(bytes([kind])+encode_string(name)+encode(kind,root))
    return {'stripped_entities':count,'stripped_executable_palette_entries':len(forbidden)}
