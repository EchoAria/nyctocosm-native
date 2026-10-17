package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Source 90 velocity samples at four-degree intervals; xflat pitch is -23..21. */
public final class VoidSwirlEffects {
    public static List<Vec3> velocityPlan(long seed,boolean reverse,boolean flat){
        var random=RandomSource.create(seed);
        float yaw=random.nextInt(360)-180,pitch=flat?random.nextInt(45)-23:random.nextInt(180)-90;
        Vec3 forward=SerpentMotion.localForward(yaw,pitch),up=SerpentMotion.localForward(yaw,pitch-90),left=forward.cross(up).scale(-1);
        double speed=reverse?.5:2.5;List<Vec3> result=new ArrayList<>(90);
        for(int sample=0;sample<90;sample++){
            double angle=Math.toRadians(sample*4);
            result.add(left.scale(Math.cos(angle)*speed).add(forward.scale(Math.sin(angle)*speed)));
        }
        return result;
    }
    public static void clientSingle(Level level,Vec3 at,long seed,boolean reverse,boolean flat){
        if(!level.isClientSide)return;
        for(var velocity:velocityPlan(seed,reverse,flat))level.addParticle(reverse?ParticleTypes.REVERSE_PORTAL:ParticleTypes.PORTAL,true,
            at.x,at.y,at.z,velocity.x,velocity.y,velocity.z);
    }
    private VoidSwirlEffects(){}
}
