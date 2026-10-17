package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Twenty source strands, one block per tick, changing direction every 3..6 ticks. */
public final class VoidVeinEffects {
    public static List<Vec3> explodePlan(Vec3 center,long seed,int age){
        if(age<1||age>30)return List.of();
        var result=new ArrayList<Vec3>(20);
        for(int strand=0;strand<20;strand++){
            var random=RandomSource.create(seed+997L*strand);
            float pitch=random.nextInt(180)-90,yaw=random.nextInt(360)-180;int twist=5+random.nextInt(5);
            Vec3 at=center;
            for(int tick=1;tick<=age;tick++){
                if(tick==age)result.add(at); // Direct particles retain this function's position before tp @s.
                at=at.add(SerpentMotion.localForward(yaw,pitch));
                if(--twist<=0){twist=3+random.nextInt(4);pitch=(float)Math.floor(pitch)+random.nextInt(60)-30;yaw=(float)Math.floor(yaw)+random.nextInt(60)-30;}
            }
        }
        return result;
    }
    public static void clientExplode(Level level,Vec3 center,long seed,int age){
        if(!level.isClientSide)return;
        var purple=new DustParticleOptions(new Vector3f(.741f,0,1),2);
        var black=new DustParticleOptions(new Vector3f(0,0,0),2.5f);
        for(Vec3 at:explodePlan(center,seed,age))for(int i=0;i<2;i++){
            level.addParticle(purple,true,at.x,at.y+level.random.nextGaussian()*.1,at.z+level.random.nextGaussian()*.1,
                level.random.nextGaussian()*.1,level.random.nextGaussian()*.1,level.random.nextGaussian()*.1);
            level.addParticle(black,true,at.x,at.y,at.z,0,0,0);
        }
    }
    private VoidVeinEffects(){}
}
