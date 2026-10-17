package com.flubberschnub.nyctocosm;

import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Source death_swirl marker motion, kept alive independently of the removed boss. */
public final class EmissarySwirlEffects {
    public static Vec3 point(Vec3 origin,long seed,int age){
        var random=new Random(seed);int twist=5+random.nextInt(5);float pitch=random.nextInt(180)-90,yaw=random.nextInt(360)-180;Vec3 at=origin;
        for(int tick=0;tick<=age;tick++){
            at=at.add(SerpentMotion.localForward(yaw,pitch));
            if(--twist<=0){twist=3+random.nextInt(4);pitch+=random.nextInt(100)-50;yaw+=random.nextInt(100)-50;}
        }
        return at;
    }
    public static void clientFrame(Level level,Vec3 origin,long seed,int age){
        if(!level.isClientSide||age<0||age>=30)return;
        if(age==0){level.addParticle(ParticleTypes.FLASH,true,origin.x,origin.y,origin.z,0,0,0);
            for(int i=0;i<2;i++)level.addParticle(ParticleTypes.EXPLOSION,true,origin.x+level.random.nextGaussian(),origin.y+level.random.nextGaussian(),origin.z+level.random.nextGaussian(),0,0,0);}
        Vec3 at=point(origin,seed,age);
        for(var particle:new net.minecraft.core.particles.ParticleOptions[]{ParticleTypes.WAX_ON,ParticleTypes.END_ROD,ParticleTypes.FLAME})for(int i=0;i<3;i++)
            level.addParticle(particle,true,at.x+level.random.nextGaussian()*.1,at.y+level.random.nextGaussian()*.1,at.z+level.random.nextGaussian()*.1,level.random.nextGaussian()*.05,level.random.nextGaussian()*.05,level.random.nextGaussian()*.05);
    }
    private EmissarySwirlEffects(){}
}
