package com.flubberschnub.nyctocosm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Server-safe sampled Emissary locator tracks used by its multipart damage bodies. */
final class EmissaryDamagePose {
    record Locator(Vec3 localPosition, float yaw, float pitch) {}
    private record Frame(int tick, Map<String, Locator> locators) {}
    private record Track(int duration, boolean loop, List<Frame> frames) {}
    private static final String[] LOCATOR_NAMES={"center","leftmiddlewingtip0","rightmiddlewingtip0",
        "rightupperwingtip1","leftupperwingtip1","rightmiddlewingtip1","leftmiddlewingtip1","rightlowerwingtip1","leftlowerwingtip1",
        "righthand","lefthand","rightmiddlewingtip2","rightupperwingtip2","leftmiddlewingtip2","leftupperwingtip2","rightmiddlewingtip3","leftupperwingtip3","rightupperwingtip3"};
    private static final Map<String,Track> TRACKS=load();

    static Vec3 worldPosition(NativeBoss boss,Locator locator){
        return localPoint(boss.position(),boss.getYRot(),boss.getXRot(),locator.localPosition());
    }
    static Vec3 offsetFromLocator(NativeBoss boss,Locator locator,double x,double y,double z){
        Vec3 origin=worldPosition(boss,locator);
        return localPoint(origin,boss.getYRot()+locator.yaw(),boss.getXRot()+locator.pitch(),new Vec3(x,y,z));
    }
    static Locator sample(NativeBoss boss,String name){
        return sampleAt(boss,boss.animation(),name,boss.animationTime(0));
    }
    static Locator sampleAt(String animation,String name,float sampleTime){
        return sampleAt(null,animation,name,sampleTime);
    }
    static Locator sampleAt(NativeBoss boss,String animation,String name,float sampleTime){
        Track track=TRACKS.get(animation);
        if(track==null&&boss!=null)track=TRACKS.get("phase"+boss.phase()+"_idle");
        if(track==null)throw new IllegalStateException("Missing Emissary locator animation "+animation);
        float time=Math.max(0,sampleTime);
        if(track.loop()){
            time%=track.duration();
            if(time<0)time+=track.duration();
        }else time=Math.min(time,track.duration());

        List<Frame> frames=track.frames();
        int upper=0;
        while(upper<frames.size()-1&&frames.get(upper).tick()<time)upper++;
        Frame b=frames.get(upper),a=frames.get(Math.max(0,upper-1));
        Locator from=a.locators().get(name),to=b.locators().get(name);
        if(from==null||to==null)throw new IllegalStateException("Missing "+name+" track for "+boss.animation());
        float fraction=a.tick()==b.tick()?0:Math.max(0,Math.min(1,(time-a.tick())/(b.tick()-a.tick())));
        Vec3 position=new Vec3(Mth.lerp(fraction,from.localPosition().x,to.localPosition().x),
            Mth.lerp(fraction,from.localPosition().y,to.localPosition().y),
            Mth.lerp(fraction,from.localPosition().z,to.localPosition().z));
        return new Locator(position,Mth.rotLerp(fraction,from.yaw(),to.yaw()),Mth.rotLerp(fraction,from.pitch(),to.pitch()));
    }
    private static Vec3 localPoint(Vec3 origin,float yaw,float pitch,Vec3 local){
        return EmissaryPose.localPoint(origin,yaw,pitch,local);
    }
    private static Map<String,Track> load(){
        String path="/assets/nyctocosm/native_rigs/emissary_damage_locators.json.gz";
        try(InputStream stream=Objects.requireNonNull(EmissaryDamagePose.class.getResourceAsStream(path),"Missing "+path);
            var gzip=new GZIPInputStream(stream);
            var reader=new InputStreamReader(gzip,StandardCharsets.UTF_8)){
            JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
            if(root.get("schema").getAsInt()!=1)throw new IOException("Unsupported Emissary locator schema");
            Map<String,Track> result=new HashMap<>();
            for(var entry:root.getAsJsonObject("animations").entrySet()){
                JsonObject animation=entry.getValue().getAsJsonObject();List<Frame> frames=new ArrayList<>();
                for(JsonElement frameElement:animation.getAsJsonArray("frames")){
                    JsonObject frame=frameElement.getAsJsonObject();Map<String,Locator> locators=new HashMap<>();
                    for(String name:LOCATOR_NAMES){
                        var values=frame.getAsJsonObject("locators").getAsJsonArray(name);
                        locators.put(name,new Locator(new Vec3(values.get(0).getAsDouble(),values.get(1).getAsDouble(),values.get(2).getAsDouble()),
                            values.get(3).getAsFloat(),values.get(4).getAsFloat()));
                    }
                    frames.add(new Frame(frame.get("tick").getAsInt(),locators));
                }
                result.put(entry.getKey(),new Track(animation.get("duration").getAsInt(),animation.get("loop").getAsBoolean(),List.copyOf(frames)));
            }
            if(result.size()<40)throw new IOException("Incomplete Emissary locator tracks: "+result.size());
            return Map.copyOf(result);
        }catch(IOException|RuntimeException exception){throw new ExceptionInInitializerError(exception);}
    }
    private EmissaryDamagePose(){}
}
