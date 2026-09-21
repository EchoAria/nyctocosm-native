package com.flubberschnub.nyctocosm;

import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Native particle emitters using the source weapons' colors and sampling geometry. */
public final class WeaponEffects {
    private static final DustColorTransitionOptions FISSURE=new DustColorTransitionOptions(new Vector3f(.741f,0,1),new Vector3f(0,.569f,1),1.5f);
    private static final DustParticleOptions BLACK=new DustParticleOptions(new Vector3f(0,0,0),2);
    private static final DustParticleOptions FISSURE_BLACK=new DustParticleOptions(new Vector3f(0,0,0),.6f);
    private static final DustParticleOptions FISSURE_PURPLE=new DustParticleOptions(new Vector3f(.741f,0,1),1);
    record ParticleCall(ParticleOptions particle,Vec3 position,int count,double spread,double speed){}
    static List<ParticleCall> offhandPlan(int kind,Vec3 eye,Vec3 end,Vec3 forward){
        Vec3 axis=forward.normalize();
        if(axis.lengthSqr()<1e-8)return List.of();
        List<ParticleCall> calls=new ArrayList<>();
        if(kind==2){
            Vec3 ray=end.subtract(eye);double length=ray.length(),start=Math.min(2,length);
            Vec3 reference=Math.abs(axis.y)<.99?new Vec3(0,1,0):new Vec3(1,0,0);
            Vec3 right=axis.cross(reference).normalize(),up=right.cross(axis).normalize();
            calls.add(new ParticleCall(ParticleTypes.FLASH,eye.add(axis.scale(.5)),1,0,0));
            for(double distance=start;distance<=length+.001;distance+=1){
                Vec3 center=eye.add(axis.scale(distance));
                ring(calls,FISSURE_BLACK,center,right,up,.2,18);
                ring(calls,FISSURE_PURPLE,center,right,up,.4,20);
            }
        }else if(kind==3){
            Vec3 muzzle=eye.add(axis);
            calls.add(new ParticleCall(ParticleTypes.FLASH,muzzle,1,0,0));
            calls.add(new ParticleCall(ParticleTypes.EXPLOSION,muzzle,1,0,0));
            calls.add(new ParticleCall(ParticleTypes.LAVA,muzzle,10,0,.2));
            calls.add(new ParticleCall(ParticleTypes.FALLING_LAVA,muzzle,20,.6,0));
            // Six rays are emitted immediately; the source effect adds three more
            // over its following three subticks (see dissonanceAfterburstPlan).
            for(int rayIndex=0;rayIndex<6;rayIndex++)dissonanceRay(calls,muzzle,rayIndex);
        }
        return List.copyOf(calls);
    }
    static List<ParticleCall> dissonanceAfterburstPlan(Vec3 eye,Vec3 forward,int rayIndex){
        Vec3 axis=forward.normalize();if(axis.lengthSqr()<1e-8)return List.of();
        List<ParticleCall> calls=new ArrayList<>();dissonanceRay(calls,eye.add(axis),6+Math.floorMod(rayIndex,3));return List.copyOf(calls);
    }
    private static void dissonanceRay(List<ParticleCall> calls,Vec3 muzzle,int rayIndex){
        double golden=Math.PI*(3-Math.sqrt(5)),y=1-(rayIndex+.5)*2/9,radial=Math.sqrt(Math.max(0,1-y*y)),angle=rayIndex*golden;
        Vec3 direction=new Vec3(Math.cos(angle)*radial,y,Math.sin(angle)*radial);
        for(int n=0;n<=5;n++)calls.add(new ParticleCall(ParticleTypes.FLAME,muzzle.add(direction.scale(n*.2)),1,0,.000003));
        for(int n=0;n<=2;n++)calls.add(new ParticleCall(ParticleTypes.LARGE_SMOKE,muzzle.subtract(direction.scale(n*.2)),1,0,.0000005));
    }
    private static void ring(List<ParticleCall> calls,ParticleOptions particle,Vec3 center,Vec3 right,Vec3 up,double radius,int points){
        for(int n=0;n<points;n++){
            double angle=n*Math.PI*2/points;
            calls.add(new ParticleCall(particle,center.add(right.scale(Math.cos(angle)*radius)).add(up.scale(Math.sin(angle)*radius)),1,0,0));
        }
    }
    public static void offhand(ServerLevel level,LivingEntity owner,int kind,Vec3 end){
        Vec3 eye=owner.getEyePosition(),endpoint=end==null?eye.add(owner.getLookAngle().scale(40)):end;
        NativeNetwork.weaponEffect(owner,kind,eye,endpoint,owner.getLookAngle());
    }
    public static void clientOffhand(Level level,int kind,Vec3 eye,Vec3 end,Vec3 forward){
        if(level==null||!level.isClientSide)return;
        emit(level,offhandPlan(kind,eye,end,forward));
    }
    public static void clientDissonanceAfterburst(Level level,Vec3 eye,Vec3 forward,int rayIndex){
        if(level!=null&&level.isClientSide)emit(level,dissonanceAfterburstPlan(eye,forward,rayIndex));
    }
    private static void emit(Level level,List<ParticleCall> calls){
        for(ParticleCall call:calls){
            Vec3 p=call.position;
            for(int n=0;n<call.count;n++)level.addParticle(call.particle,
                p.x+(level.random.nextDouble()*2-1)*call.spread,p.y+(level.random.nextDouble()*2-1)*call.spread,p.z+(level.random.nextDouble()*2-1)*call.spread,
                level.random.nextGaussian()*call.speed,level.random.nextGaussian()*call.speed,level.random.nextGaussian()*call.speed);
        }
    }
    public static void sweep(ServerLevel level,LivingEntity owner,int kind){
        Vec3 forward=owner.getLookAngle(),left=new Vec3(forward.z,0,-forward.x).normalize(),up=forward.cross(left).normalize();
        Vec3 origin=owner.getEyePosition();
        if(kind==2){
            for(int i=0;i<=24;i++){
                double offset=-.4+i/30.0;
                Vec3 direction=forward.scale(.2).add((owner.onGround()?left:up).scale(offset)).normalize();
                Vec3 p=origin.add(direction.scale(3));
                level.sendParticles(FISSURE,p.x,p.y,p.z,1,0,0,0,0);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT,p.x,p.y,p.z,1,0,0,0,0);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,p.x,p.y,p.z,1,.1,.1,.1,0);
            }
        }else if(kind==3){
            origin=owner.position().add(0,owner.onGround()?1.5:.9,0);
            for(int i=0;i<72;i++){
                double angle=i*5; // Source generated coordinates use these radian samples.
                Vec3 p=origin.add((owner.onGround()?left:up).scale(Math.sin(angle)*2)).add(forward.scale(Math.cos(angle)*2));
                level.sendParticles(ParticleTypes.FLAME,p.x,p.y,p.z,1,0,0,0,.01);
                level.sendParticles(ParticleTypes.SMOKE,p.x,p.y,p.z,1,0,0,0,0);
                level.sendParticles(ParticleTypes.FIREWORK,p.x,p.y,p.z,1,0,0,0,0);
            }
        }
    }
    public static void dash(ServerLevel level,Vec3 start,Vec3 end){
        Vec3 path=end.subtract(start);int count=Math.max(1,(int)Math.ceil(path.length()));
        for(int i=0;i<=count;i++){
            Vec3 p=start.add(path.scale((double)i/count)).add(0,1,0);
            level.sendParticles(BLACK,p.x,p.y,p.z,50,.2,.4,.2,.1);
            level.sendParticles(ParticleTypes.PORTAL,p.x,p.y,p.z,10,.2,.4,.2,1);
        }
    }
    private WeaponEffects(){}
}
