package com.flubberschnub.nyctocosm.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/** Asset-only sampled skeletal animation. The original commands are never loaded. */
public final class RigLibrary {
    public record Bone(String name,ResourceLocation model,Matrix4f rest){}
    public record Frame(int tick,Map<String,Matrix4f> poses){}
    public record Rig(List<Bone> bones,Map<String,List<Frame>> animations,Map<String,Map<String,ResourceLocation>> variants){}
    private static final Map<String,Rig> CACHE=new HashMap<>();
    public static void clear(){CACHE.clear();}
    public static Set<ResourceLocation> allModels(){
        Set<ResourceLocation> result=new HashSet<>();
        try(var reader=new InputStreamReader(Objects.requireNonNull(RigLibrary.class.getResourceAsStream("/assets/nyctocosm/native_rigs/index.json")),StandardCharsets.UTF_8)){
            for(JsonElement name:JsonParser.parseReader(reader).getAsJsonArray()){
                try(var stream=RigLibrary.class.getResourceAsStream("/assets/nyctocosm/native_rigs/"+name.getAsString()+".json.gz")){
                    Rig rig=parse(Objects.requireNonNull(stream));for(Bone bone:rig.bones)result.add(bone.model);
                    rig.variants.values().forEach(variant->result.addAll(variant.values()));
                }
            }
        }catch(IOException e){throw new IllegalStateException(e);}
        return result;
    }
    public static Rig get(String id){return CACHE.computeIfAbsent(id,name->{
        var resource=Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("nyctocosm","native_rigs/"+name+".json.gz"));
        if(resource.isEmpty())throw new IllegalStateException("Missing native rig "+name);
        try(var stream=resource.get().open()){return parse(stream);}catch(IOException e){throw new IllegalStateException(e);}
    });}
    private static Rig parse(InputStream stream)throws IOException{
        JsonObject root;
        try(var reader=new InputStreamReader(new GZIPInputStream(stream),StandardCharsets.UTF_8)){root=JsonParser.parseReader(reader).getAsJsonObject();}
        List<Bone> bones=new ArrayList<>();
        for(JsonElement element:root.getAsJsonArray("bones")){
            JsonObject bone=element.getAsJsonObject();bones.add(new Bone(bone.get("name").getAsString(),new ResourceLocation(bone.get("model").getAsString()),matrix(bone.getAsJsonArray("transform"))));
        }
        Map<String,List<Frame>> animations=new HashMap<>();
        for(var entry:root.getAsJsonObject("animations").entrySet()){
            List<Frame> frames=new ArrayList<>();Map<String,Matrix4f> previous=new HashMap<>();
            for(Bone bone:bones)previous.put(bone.name,bone.rest);
            for(JsonElement element:entry.getValue().getAsJsonArray()){
                JsonObject frame=element.getAsJsonObject();Map<String,Matrix4f> poses=new HashMap<>(previous);
                frame.getAsJsonObject("bones").entrySet().forEach(e->poses.put(e.getKey(),matrix(e.getValue().getAsJsonArray())));
                frames.add(new Frame(frame.get("tick").getAsInt(),poses));previous=poses;
            }
            animations.put(entry.getKey(),frames);
        }
        Map<String,Map<String,ResourceLocation>> variants=new HashMap<>();
        if(root.has("variants"))for(var entry:root.getAsJsonObject("variants").entrySet()){
            Map<String,ResourceLocation> models=new HashMap<>();entry.getValue().getAsJsonObject().entrySet().forEach(e->models.put(e.getKey(),new ResourceLocation(e.getValue().getAsString())));
            variants.put(entry.getKey(),models);
        }
        return new Rig(bones,animations,variants);
    }
    private static Matrix4f matrix(JsonArray data){float[] values=new float[16];for(int i=0;i<16;i++)values[i]=data.get(i).getAsFloat();return new Matrix4f().set(values).transpose();}
    public static Matrix4f pose(Rig rig,Bone bone,String animation,float tick,boolean loop){
        List<Frame> frames=rig.animations.get(animation);if(frames==null||frames.isEmpty())return bone.rest;
        int last=frames.get(frames.size()-1).tick;
        float time=loop?tick%Math.max(1,last):Math.min(tick,last);
        int upper=0;while(upper<frames.size()-1&&frames.get(upper).tick<time)upper++;
        Frame b=frames.get(upper),a=frames.get(Math.max(0,upper-1));
        Matrix4f ma=a.poses.getOrDefault(bone.name,bone.rest),mb=b.poses.getOrDefault(bone.name,ma);
        float t=a.tick==b.tick?0:Math.max(0,Math.min(1,(time-a.tick)/(b.tick-a.tick)));
        return com.flubberschnub.nyctocosm.RigTransforms.interpolate(ma,mb,t);
    }
    private RigLibrary(){}
}
