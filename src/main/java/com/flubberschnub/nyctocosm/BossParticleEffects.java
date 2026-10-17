package com.flubberschnub.nyctocosm;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

/** Static particle values exported from source keyframes; damage and sound remain server-owned. */
public final class BossParticleEffects {
    private record Cue(ParticleOptions particle,Vec3 local,Vec3 world,Vec3 delta,double speed,int count,boolean force,boolean deltaLocal,boolean postMove,String locator,Float yawAbsolute,float yawOffset,Float pitchAbsolute,float pitchOffset){}
    private static final Map<String,Map<String,Map<Integer,List<Cue>>>> CUES=load();
    private static Map<String,Map<String,Map<Integer,List<Cue>>>> load(){
        Map<String,Map<String,Map<Integer,List<Cue>>>> rigs=new HashMap<>();
        for(String resource:List.of("sorcerer_keyframes.json","feedback_keyframes.json","combat_repair_keyframes.json"))try(var stream=BossParticleEffects.class.getResourceAsStream("/assets/nyctocosm/effects/"+resource)){
            var json=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();
            for(var rig:json.entrySet()){
                Map<String,Map<Integer,List<Cue>>> animations=rigs.computeIfAbsent(rig.getKey(),key->new HashMap<>());
                for(var animation:rig.getValue().getAsJsonObject().entrySet()){
                    Map<Integer,List<Cue>> frames=new HashMap<>();animations.put(animation.getKey(),frames);
                    for(var frame:animation.getValue().getAsJsonObject().entrySet()){
                        List<Cue> cues=new ArrayList<>();frames.put(Integer.parseInt(frame.getKey()),cues);
                        for(var value:frame.getValue().getAsJsonArray()){
                            var row=value.getAsJsonObject();
                            var particle=ParticleArgument.readParticle(new StringReader(row.get("particle").getAsString()),BuiltInRegistries.PARTICLE_TYPE.asLookup());
                            cues.add(new Cue(particle,vec(row,"local"),vec(row,"world"),vec(row,"delta"),row.get("speed").getAsDouble(),row.get("count").getAsInt(),row.get("force").getAsBoolean(),row.has("delta_local")&&row.get("delta_local").getAsBoolean(),row.has("post_move")&&row.get("post_move").getAsBoolean(),row.has("locator")?row.get("locator").getAsString():"",optional(row,"yaw_absolute"),row.has("yaw_offset")?row.get("yaw_offset").getAsFloat():0,optional(row,"pitch_absolute"),row.has("pitch_offset")?row.get("pitch_offset").getAsFloat():0));
                        }
                    }
                }
            }
        }catch(Exception error){throw new IllegalStateException("Invalid compiled Sorcerer particles",error);}
        return rigs;
    }
    private static Vec3 vec(JsonObject row,String key){var array=row.getAsJsonArray(key);return new Vec3(array.get(0).getAsDouble(),array.get(1).getAsDouble(),array.get(2).getAsDouble());}
    private static Float optional(JsonObject row,String key){return row.has(key)?row.get(key).getAsFloat():null;}
    public static int duration(String rig,String clip){return CUES.getOrDefault(rig,Map.of()).getOrDefault(clip,Map.of()).keySet().stream().mapToInt(Integer::intValue).max().orElse(0)+1;}
    public static int cueCount(String rig,String animation,int frame){return CUES.getOrDefault(rig,Map.of()).getOrDefault(animation,Map.of()).getOrDefault(frame,List.of()).size();}
    public static int particleCount(String rig,String animation,int frame){return CUES.getOrDefault(rig,Map.of()).getOrDefault(animation,Map.of()).getOrDefault(frame,List.of()).stream().mapToInt(cue->cue.count).sum();}
    public static void clientFrame(NativeBoss boss,int frame){
        if(!boss.level().isClientSide||(boss.kind!=1&&boss.kind!=2))return;
        draw(boss.level(),boss.rig(),boss.animation(),frame,boss.position(),boss.getYRot(),boss.getXRot(),boss.position(),boss);
    }
    public static void clientFrame(net.minecraft.world.level.Level level,String rig,String clip,int frame,Vec3 entry,float yaw,float pitch,Vec3 after){
        draw(level,rig,clip,frame,entry,yaw,pitch,after,null);
    }
    private static void draw(net.minecraft.world.level.Level level,String rig,String clip,int frame,Vec3 entry,float yaw,float pitch,Vec3 after,NativeBoss boss){
        if(!level.isClientSide)return;var random=level.random;
        for(var cue:CUES.getOrDefault(rig,Map.of()).getOrDefault(clip,Map.of()).getOrDefault(frame,List.of())){
            Vec3 origin=cue.postMove?after:entry;
            if(boss!=null&&!cue.locator.isEmpty())origin=EmissaryDamagePose.worldPosition(boss,EmissaryDamagePose.sampleAt(boss,clip,cue.locator,frame));
            float rowYaw=(cue.yawAbsolute==null?yaw:cue.yawAbsolute)+cue.yawOffset,rowPitch=(cue.pitchAbsolute==null?pitch:cue.pitchAbsolute)+cue.pitchOffset;
            Vec3 forward=SerpentMotion.localForward(rowYaw,rowPitch);Vec3 up=SerpentMotion.localForward(rowYaw,rowPitch-90);Vec3 left=forward.cross(up).scale(-1);
            Vec3 at=origin.add(left.scale(cue.local.x)).add(up.scale(cue.local.y)).add(forward.scale(cue.local.z)).add(cue.world);
            if(cue.count==0){Vec3 velocity=(cue.deltaLocal?left.scale(cue.delta.x).add(up.scale(cue.delta.y)).add(forward.scale(cue.delta.z)):cue.delta).scale(cue.speed);level.addParticle(cue.particle,cue.force,at.x,at.y,at.z,velocity.x,velocity.y,velocity.z);}
            else for(int i=0;i<cue.count;i++)level.addParticle(cue.particle,cue.force,
                at.x+random.nextGaussian()*cue.delta.x,at.y+random.nextGaussian()*cue.delta.y,at.z+random.nextGaussian()*cue.delta.z,
                random.nextGaussian()*cue.speed,random.nextGaussian()*cue.speed,random.nextGaussian()*cue.speed);
        }
    }
    private BossParticleEffects(){}
}
