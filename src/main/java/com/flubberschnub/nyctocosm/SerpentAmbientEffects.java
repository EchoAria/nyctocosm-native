package com.flubberschnub.nyctocosm;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Original head/tail mist. Beam particles retain their separate shared budget. */
final class SerpentAmbientEffects {
    static Vec3 head(NativeBoss boss){return boss.position().add(0,1.5,0).add(boss.getLookAngle().scale(2));}
    static Vec3 tail(NativeBoss boss,int index){
        Vec3 at=boss.serpentRenderSegment(41,1);
        Vec3 forward=SerpentMotion.localForward(boss.serpentSegmentYaw(41,1),boss.serpentSegmentPitch(41,1));
        return at.add(0,1.5,0).add(forward.scale(-(2+index*4)));
    }
    static void emit(ServerLevel level,NativeBoss boss){
        dust(level,head(boss),2,30,.7);
        for(int index=0;index<5;index++)dust(level,tail(boss,index),3,5,1.2);
    }
    private static void dust(ServerLevel level,Vec3 at,float size,int count,double spread){
        var dust=new DustParticleOptions(new Vector3f(0,0,0),size);
        for(var viewer:level.players())if(viewer.position().distanceToSqr(at)<=512*512)level.sendParticles(viewer,dust,true,at.x,at.y,at.z,count,spread,spread,spread,0);
    }
    private SerpentAmbientEffects(){}
}
