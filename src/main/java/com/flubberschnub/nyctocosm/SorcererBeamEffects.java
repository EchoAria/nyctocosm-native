package com.flubberschnub.nyctocosm;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Numeric source geometry: clients receive ray samples and never execute functions. */
public final class SorcererBeamEffects {
    private record Cue(ParticleOptions particle,Vec3 local,boolean force){}
    private static final Map<String,List<Cue>> CUES=load();
    private static Map<String,List<Cue>> load(){
        Map<String,List<Cue>> result=new HashMap<>();
        try(var stream=Objects.requireNonNull(SorcererBeamEffects.class.getResourceAsStream("/assets/nyctocosm/effects/sorcerer_beam_particles.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            for(var entry:JsonParser.parseReader(reader).getAsJsonObject().entrySet()){
                var cues=new ArrayList<Cue>();
                for(var element:entry.getValue().getAsJsonArray()){
                    var row=element.getAsJsonObject();var local=row.getAsJsonArray("local");
                    cues.add(new Cue(ParticleArgument.readParticle(new StringReader(row.get("particle").getAsString()),BuiltInRegistries.PARTICLE_TYPE.asLookup()),new Vec3(local.get(0).getAsDouble(),local.get(1).getAsDouble(),local.get(2).getAsDouble()),row.get("force").getAsBoolean()));
                }
                result.put(entry.getKey(),List.copyOf(cues));
            }
        }catch(Exception failure){throw new IllegalStateException("Invalid Sorcerer beam particle data",failure);}
        return Map.copyOf(result);
    }
    public static int count(String helper){return CUES.get(helper).size();}
    public static void emit(Level level,String helper,Vec3 origin,float yaw,float pitch){
        if(!level.isClientSide)return;
        Vec3 forward=SerpentMotion.localForward(yaw,pitch),up=SerpentMotion.localForward(yaw,pitch-90),left=forward.cross(up).scale(-1);
        for(var cue:CUES.get(helper)){Vec3 at=origin.add(left.scale(cue.local.x)).add(up.scale(cue.local.y)).add(forward.scale(cue.local.z));level.addParticle(cue.particle,cue.force,at.x,at.y,at.z,0,0,0);}
    }
    public static void ray(Level level,List<SorcererBeamNode.Sample> samples,boolean halo){
        if(level==null||!level.isClientSide||samples.isEmpty())return;
        for(var sample:samples){
            emit(level,"halobeambeam",sample.position(),sample.yaw(),sample.pitch()-90);
            if(halo){Vec3 at=sample.position();level.addParticle(ParticleTypes.END_ROD,true,at.x,at.y,at.z,level.random.nextGaussian()*.01,level.random.nextGaussian()*.01,level.random.nextGaussian()*.01);}
        }
        Vec3 end=samples.get(samples.size()-1).position();level.addParticle(ParticleTypes.FLASH,end.x,end.y,end.z,0,0,0);
    }
    private SorcererBeamEffects(){}
}
