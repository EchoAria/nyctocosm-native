package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Source single: delay 0..9, life 4..11, ten 0.2-block moves, reflected pitch and 60% straight paths. */
public final class SoulSwirlEffects {
    public static List<Vec3> explodePlan(Vec3 center,long seed,int age){
        var points=new ArrayList<Vec3>(150);
        for(int strand=0;strand<15;strand++)points.addAll(singlePlan(center,seed+997L*strand,age));
        return points;
    }
    public static List<Vec3> singlePlan(Vec3 center,long seed,int age){
        var random=RandomSource.create(seed);float pitch=random.nextInt(90)-45,yaw=random.nextInt(360)-180;
        int life=4+random.nextInt(8),delay=random.nextInt(10),radius=3+random.nextInt(12);
        if(random.nextFloat()<.6f)radius=0;
        float speed=(100+random.nextInt(170))*.1f;
        int firstActive=Math.max(0,delay-1),frame=age-firstActive;if(frame<0||frame>=life)return List.of();
        Vec3 direction=SerpentMotion.localForward(yaw,pitch);
        float emitterYaw=yaw+270,emitterPitch=0;boolean flipped=false;
        List<Vec3> points=new ArrayList<>(10);
        int step=0;
        for(int tick=0;tick<=age;tick++){
            if(tick>=firstActive)for(int substep=0;substep<10;substep++){
                emitterPitch+=flipped?-speed:speed;
                if(emitterPitch>=90){emitterPitch=90;emitterYaw+=180;flipped=true;}
                else if(emitterPitch<=-90){emitterPitch=-90;emitterYaw+=180;flipped=false;}
                if(tick==age)points.add(center.add(direction.scale(.01+step*.2))
                    .add(SerpentMotion.localForward(emitterYaw,emitterPitch).scale(radius*.1)));
                step++;
            }
            // Emitter radius grows after origin emission, including frames spent waiting.
            if(tick<age&&random.nextFloat()<.3f)radius++;
        }
        return points;
    }
    public static void clientSingle(Level level,Vec3 center,long seed,int age){
        if(!level.isClientSide)return;
        emit(level,singlePlan(center,seed,age));
    }
    public static void clientExplode(Level level,Vec3 center,long seed,int age){
        if(!level.isClientSide)return;
        emit(level,explodePlan(center,seed,age));
    }
    private static void emit(Level level,List<Vec3> points){
        for(Vec3 at:points){
            level.addParticle(ParticleTypes.WAX_ON,true,at.x,at.y,at.z,0,0,0);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,true,at.x,at.y,at.z,level.random.nextGaussian()*.01,level.random.nextGaussian()*.01,level.random.nextGaussian()*.01);
        }
    }
    private SoulSwirlEffects(){}
}
