package com.flubberschnub.nyctocosm;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Particle geometry compiled from the original Resonance and portal functions. */
public final class ResonanceEffects {
    record Particle(ParticleOptions type,Vec3 at,Vec3 delta,double speed,int count){}
    private static final JsonObject GEOMETRY=load();
    private static JsonObject load(){
        try(var stream=ResonanceEffects.class.getResourceAsStream("/assets/nyctocosm/effects/resonance.json")){
            return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException("Missing Resonance particle geometry",e);}
    }
    private static ParticleOptions type(String name){return switch(name){
        case "sculk_charge"->new SculkChargeParticleOptions(0);
        case "sculk_charge_pop"->ParticleTypes.SCULK_CHARGE_POP;
        case "soul_fire_flame"->ParticleTypes.SOUL_FIRE_FLAME;
        case "sculk_soul"->ParticleTypes.SCULK_SOUL;
        case "end_rod"->ParticleTypes.END_ROD;
        case "firework"->ParticleTypes.FIREWORK;
        default->throw new IllegalArgumentException("Unknown Resonance particle "+name);
    };}
    private static Vec3 local(Vec3 values,Vec3 forward){
        float yaw=forward.horizontalDistance()<1e-6?0:SerpentMotion.facingYaw(forward),pitch=SerpentMotion.facingPitch(forward);
        Vec3 z=SerpentMotion.localForward(yaw,pitch),y=SerpentMotion.localForward(yaw,pitch-90),x=z.cross(y).scale(-1);
        return x.scale(values.x).add(y.scale(values.y)).add(z.scale(values.z));
    }
    static List<Particle> geometry(String group,int frame,Vec3 at,Vec3 forward){
        var value=GEOMETRY.getAsJsonObject(group).get(Integer.toString(frame));if(value==null)return List.of();
        var plan=new ArrayList<Particle>();
        for(var element:value.getAsJsonArray()){
            var row=element.getAsJsonArray();Vec3 offset=new Vec3(row.get(1).getAsDouble(),row.get(2).getAsDouble(),row.get(3).getAsDouble());
            Vec3 velocity=new Vec3(row.get(4).getAsDouble(),row.get(5).getAsDouble(),row.get(6).getAsDouble());
            plan.add(new Particle(type(row.get(0).getAsString()),at.add(local(offset,forward)),local(velocity,forward),1,0));
        }
        return plan;
    }
    public static void clientCharge(Level level,Vec3 at,int age){
        if(!level.isClientSide)return;
        for(int shell=0;shell<=Math.min(79,age);shell++)emit(level,new Particle(ParticleTypes.SOUL_FIRE_FLAME,at,new Vec3(1,1,1).scale(shell/70d),0,1));
    }
    static List<Particle> rayPlan(Vec3 origin,Vec3 forward){
        var plan=new ArrayList<Particle>();
        for(int step=0;step<ResonanceSpell.RANGE;step++)plan.add(new Particle(ParticleTypes.SONIC_BOOM,origin.add(forward.scale(step)),new Vec3(.1,.1,.1),0,1));
        return plan;
    }
    public static void clientRay(Level level,Vec3 at,Vec3 forward){rayPlan(at,forward).forEach(p->emit(level,p));}
    public static void clientBurst(Level level,Vec3 at,Vec3 forward,int age){
        Vec3 moving=at.add(forward.scale(age-1));emit(level,new Particle(ParticleTypes.FLASH,moving,Vec3.ZERO,0,1));
        geometry("burst",age,moving,forward).forEach(p->emit(level,p));
    }
    static List<Particle> portalPlan(Vec3 center,Direction.Axis axis,int age){
        var plan=new ArrayList<Particle>();Vec3 normal=axis==Direction.Axis.X?new Vec3(0,0,1):new Vec3(1,0,0);
        Vec3 width=axis==Direction.Axis.X?new Vec3(1,0,0):new Vec3(0,0,1);
        for(int wave=0;wave<3;wave++){
            int tick=age-wave*16;if(tick<0||tick>90)continue;
            Vec3 flashSpread=width.scale(5).add(normal.scale(.1)).add(0,2,0);
            if(tick==0)plan.add(new Particle(ParticleTypes.FLASH,center,flashSpread,0,10));
            if(tick==90){
                plan.add(new Particle(ParticleTypes.EXPLOSION_EMITTER,center,Vec3.ZERO,0,1));
                plan.add(new Particle(ParticleTypes.FLASH,center,flashSpread,0,100));
                plan.add(new Particle(ParticleTypes.FIREWORK,center,width.scale(2).add(normal.scale(.1)).add(0,1,0),1,500));continue;
            }
            double spread=Math.cos(Math.toRadians(-90+tick*2));
            for(int ring=0;ring<2;ring++){
                double radius=ring==0?.8+wave*.6:wave==0?2.4:wave==1?2.8:3.4;
                double distance=(ring==0?7:11)+wave*2;
                for(int side:new int[]{-1,1})for(int sample=0;sample<36;sample++){
                    double angle=Math.toRadians(sample*10);
                    Vec3 point=center.add(normal.scale(side*distance*spread)).add(width.scale(Math.cos(angle)*radius)).add(0,Math.sin(angle)*radius,0);
                    plan.add(new Particle(ParticleTypes.SCULK_CHARGE_POP,point,new Vec3(.1,.1,.1),0,1));
                }
            }
        }
        int shockAge=age-PortalActivation.OPEN_TICK;
        for(int side:new int[]{-1,1})plan.addAll(geometry("shockwave",shockAge,center.add(normal.scale(side*3)),new Vec3(0,-1,0)));
        // Rotate shockwave's local horizontal axis for the other frame orientation.
        if(axis==Direction.Axis.Z&&shockAge>0){
            // Geometry was emitted in XY; its positions are centered, only velocities need a quarter turn.
            for(int i=0;i<plan.size();i++){var p=plan.get(i);Vec3 d=p.delta();plan.set(i,new Particle(p.type(),p.at(),new Vec3(d.z,d.y,-d.x),p.speed(),p.count()));}
        }
        plan.addAll(swirlPlan(center,shockAge));
        return plan;
    }
    public static float shakeStrength(int age,double distanceSquared){
        if(distanceSquared>2500)return 0;
        for(int wave=0;wave<3;wave++)if(age>=90+16*wave&&age<100+16*wave)return (wave+1)*.5f;
        return 0;
    }
    // Source: 15 strands, delay 0..9, life 4..11, ten 0.2-block steps per tick.
    static List<Particle> swirlPlan(Vec3 center,int age){
        var plan=new ArrayList<Particle>();if(age<1||age>20)return plan;
        long seed=Double.doubleToLongBits(center.x)^Double.doubleToLongBits(center.z)^731L;
        for(Vec3 at:SoulSwirlEffects.explodePlan(center,seed,age-1)){
            plan.add(new Particle(ParticleTypes.WAX_ON,at,Vec3.ZERO,0,1));
            plan.add(new Particle(ParticleTypes.SOUL_FIRE_FLAME,at,Vec3.ZERO,.01,1));
        }
        return plan;
    }
    public static void clientPortal(Level level,Vec3 center,Direction.Axis axis,int age,boolean joined){
        if(joined&&age>0&&age<4)portalPlan(center,axis,0).stream().filter(p->p.type()==ParticleTypes.FLASH).forEach(p->emit(level,p));
        portalPlan(center,axis,age).forEach(p->emit(level,p));
    }
    private static void emit(Level level,Particle p){
        if(!level.isClientSide)return;
        if(p.count()==0){level.addParticle(p.type(),true,p.at().x,p.at().y,p.at().z,p.delta().x*p.speed(),p.delta().y*p.speed(),p.delta().z*p.speed());return;}
        var random=level.random;
        for(int i=0;i<p.count();i++)level.addParticle(p.type(),true,p.at().x+random.nextGaussian()*p.delta().x,p.at().y+random.nextGaussian()*p.delta().y,p.at().z+random.nextGaussian()*p.delta().z,random.nextGaussian()*p.speed(),random.nextGaussian()*p.speed(),random.nextGaussian()*p.speed());
    }
    private ResonanceEffects(){}
}
