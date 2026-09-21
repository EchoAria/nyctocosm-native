package com.flubberschnub.nyctocosm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

public final class SpellBolt extends Projectile {
    private static final EntityDataAccessor<Integer> ELEMENT=SynchedEntityData.defineId(SpellBolt.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EFFECT=SynchedEntityData.defineId(SpellBolt.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WEAPON=SynchedEntityData.defineId(SpellBolt.class,EntityDataSerializers.INT);
    public enum Effect { DAMAGE, COUNTER, GRAVITY, LIGHTNING, FIRE, HOMING }
    private float damage=10, radius;
    private int life=100;
    private String impactSound="";
    private UUID homingTarget;
    public SpellBolt(EntityType<? extends SpellBolt> type,Level level) { super(type,level); }
    public static SpellBolt launch(ServerLevel level,LivingEntity owner,Vec3 at,Vec3 direction,float damage,Combat.Element element,float speed,float radius) {
        SpellBolt bolt=new SpellBolt(Content.BOLT.get(),level);
        bolt.setOwner(owner); bolt.setPos(at); bolt.damage=damage; bolt.radius=radius;
        bolt.entityData.set(ELEMENT,element.ordinal()); bolt.setDeltaMovement(direction.normalize().scale(speed));
        level.addFreshEntity(bolt); return bolt;
    }
    public Combat.Element element() { return Combat.Element.values()[Math.floorMod(entityData.get(ELEMENT),6)]; }
    public SpellBolt effect(Effect effect,int lifetime){entityData.set(EFFECT,effect.ordinal());life=lifetime;return this;}
    public SpellBolt impactSound(String sound){impactSound=sound;return this;}
    public SpellBolt homeOn(LivingEntity target){homingTarget=target==null?null:target.getUUID();return this;}
    public Effect effect(){return Effect.values()[Math.floorMod(entityData.get(EFFECT),Effect.values().length)];}
    public SpellBolt weaponVisual(int kind){entityData.set(WEAPON,kind);return this;}
    @Override protected void defineSynchedData() { entityData.define(ELEMENT,0);entityData.define(EFFECT,0);entityData.define(WEAPON,0); }
    @Override public void tick() {
        super.tick();
        Vec3 movement=getDeltaMovement();
        if (!level().isClientSide) {
            if (!level().hasChunkAt(blockPosition())){discard();return;}
            if(--life<=0){
                if(effect()==Effect.GRAVITY)((ServerLevel)level()).sendParticles(ParticleTypes.PORTAL,getX(),getY(),getZ(),2000,0,0,0,10);
                discard();return;
            }
            if(effect()==Effect.COUNTER){
                for(Projectile other:level().getEntitiesOfClass(Projectile.class,getBoundingBox().inflate(5)))
                    if(other!=this&&other.getOwner()!=getOwner()){other.discard();discard();return;}
            }
            if(effect()==Effect.GRAVITY){
                for(LivingEntity target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(9)))if(Combat.canHit(livingOwner(),target)){
                    Vec3 pull=position().subtract(target.getBoundingBox().getCenter());
                    double distance=Combat.distanceToBoxSqr(position(),target.getBoundingBox());
                    if(distance>81)continue;
                    double speed=distance<=36?.4:.2;
                    target.setDeltaMovement(target.getDeltaMovement().scale(.5).add(pull.normalize().scale(speed*.5)));target.hurtMarked=true;
                    // Original block/77 damages the inner 3-block region throughout the
                    // pull, after HurtTime ends. Never bypass another mod's damage events.
                    if(distance<=9&&target.hurtTime==0)Combat.hit(livingOwner(),target,damage,element());
                }
            }
            if(effect()==Effect.HOMING){
                LivingEntity nearest=null;double nearestDistance=40*40;
                LivingEntity owner=livingOwner();
                if(homingTarget!=null&&((ServerLevel)level()).getEntity(homingTarget) instanceof LivingEntity locked&&Combat.canHit(owner,locked))nearest=locked;
                if(nearest==null&&owner instanceof Mob mob&&Combat.canHit(owner,mob.getTarget())){nearest=mob.getTarget();nearestDistance=Combat.distanceToBoxSqr(position(),nearest.getBoundingBox());}
                if(nearest==null)for(LivingEntity candidate:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(40)))if(Combat.canHit(owner,candidate)){
                    double distance=Combat.distanceToBoxSqr(position(),candidate.getBoundingBox());if(distance<nearestDistance){nearest=candidate;nearestDistance=distance;}
                }
                if(nearest!=null){
                    double speed=Math.max(.2,getDeltaMovement().length());
                    Vec3 desired=nearest.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed);
                    setDeltaMovement(getDeltaMovement().scale(.75).add(desired.scale(.25)).normalize().scale(speed));
                    movement=getDeltaMovement();
                }
            }
            HitResult result=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
            if (result.getType()!=HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,result)) {
                setPos(result.getLocation());
                if(effect()==Effect.GRAVITY){setDeltaMovement(Vec3.ZERO);return;}
                onHit(result); return;
            }
        } else {
            if(effect()==Effect.GRAVITY){
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),2),getX(),getY(),getZ(),0,0,0);
                double angle=tickCount*.6;
                for(int n=0;n<8;n++){double a=angle+n*Math.PI/4;level().addParticle(ParticleTypes.PORTAL,getX()+Math.cos(a)*1.5,getY(),getZ()+Math.sin(a)*1.5,-Math.cos(a)*.3,0,-Math.sin(a)*.3);}
            }else if(entityData.get(WEAPON)==1){
                Vec3 side=new Vec3(movement.z,0,-movement.x).normalize();
                for(int n=-8;n<=8;n++){
                    Vec3 p=position().add(side.scale(n*.15));
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1,0,0),.75f),p.x,p.y,p.z,0,0,0);
                    Vec3 behind=p.subtract(movement.normalize());
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),.75f),behind.x,behind.y,behind.z,0,0,0);
                }
            }else if(entityData.get(WEAPON)==3){
                for(int sample=0;sample<3;sample++){
                    Vec3 center=position().add(movement.scale(sample/3.0));
                    trailParticle(ParticleTypes.FLAME,center,5,.4,.02);
                    trailParticle(ParticleTypes.END_ROD,center,2,.2,.01);
                    trailParticle(ParticleTypes.SMOKE,center,5,.4,.02);
                    trailParticle(ParticleTypes.LAVA,center,2,.4,0);
                }
            }else if(entityData.get(WEAPON)==4){
                for(int n=0;n<3;n++){
                    Vec3 p=position().add(movement.scale(n/3.0));
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.741f,0,1),1.1f),p.x,p.y,p.z,0,0,0);
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,.651f,1),.8f),p.x+.2,p.y,p.z,0,0,0);
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),1.4f),p.x-.2,p.y,p.z,0,0,0);
                    level().addParticle(ParticleTypes.REVERSE_PORTAL,p.x,p.y,p.z,0,0,0);
                }
            }else if(entityData.get(WEAPON)==5){
                for(int n=0;n<3;n++){
                    Vec3 p=position().add(movement.scale(n/3.0));
                    level().addParticle(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),p.x,p.y,p.z,0,0,0);
                    level().addParticle(ParticleTypes.SNOWFLAKE,p.x,p.y,p.z,0,.04,0);
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK,p.x,p.y,p.z,0,0,0);
                }
            }else if(entityData.get(WEAPON)==6){
                trailParticle(ParticleTypes.SMOKE,position(),5,.35,.01);
                trailParticle(ParticleTypes.FLAME,position(),3,.3,.01);
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.741f,0,1),1.8f),getX(),getY(),getZ(),0,0,0);
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),1.8f),getX(),getY(),getZ(),0,0,0);
            }else for(int n=0;n<3;n++) {
                Vec3 p=position().add(movement.scale(n/3.0));
                level().addParticle(element()==Combat.Element.DISSONANCE?ParticleTypes.FLAME:ParticleTypes.END_ROD,p.x,p.y,p.z,0,0,0);
            }
        }
        setPos(position().add(movement));
        updateRotation();
    }
    private void trailParticle(net.minecraft.core.particles.ParticleOptions particle,Vec3 center,int count,double spread,double speed){
        for(int n=0;n<count;n++)level().addParticle(particle,
            center.x+(random.nextDouble()*2-1)*spread,center.y+(random.nextDouble()*2-1)*spread,center.z+(random.nextDouble()*2-1)*spread,
            random.nextGaussian()*speed,random.nextGaussian()*speed,random.nextGaussian()*speed);
    }
    @Override protected boolean canHitEntity(Entity entity) {
        return effect()!=Effect.COUNTER&&effect()!=Effect.GRAVITY&&super.canHitEntity(entity) && entity instanceof LivingEntity living
            &&(effect()!=Effect.HOMING||homingTarget==null||entity.getUUID().equals(homingTarget))&&Combat.canHit(livingOwner(),living);
    }
    private LivingEntity livingOwner(){return getOwner() instanceof LivingEntity owner?owner:null;}
    @Override protected void onHit(HitResult result) {
        if (!(level() instanceof ServerLevel server)) return;
        LivingEntity owner=getOwner() instanceof LivingEntity living?living:null;
        if(radius>0) Combat.area(server,owner,position(),radius,damage,element(),.5);
        // The direct target can lie outside the splash sphere (e.g. a large modded mob).
        if(result instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living
            && (radius<=0 || living.getBoundingBox().getCenter().distanceToSqr(position())>radius*radius)) Combat.hit(owner,living,damage,element());
        if(effect()==Effect.FIRE)for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(Math.max(radius,1))))if(Combat.canHit(owner,target))target.setSecondsOnFire(5);
        if(effect()==Effect.LIGHTNING)server.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY(),getZ(),70,1,3,1,.2);
        if(entityData.get(WEAPON)==5)server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),getX(),getY(),getZ(),30,.4,.4,.4,.1);
        if(entityData.get(WEAPON)==6){server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY(),getZ(),1,0,0,0,0);server.sendParticles(ParticleTypes.SMOKE,getX(),getY(),getZ(),35,1,1,1,.04);}
        server.sendParticles(ParticleTypes.FLASH,getX(),getY(),getZ(),1,0,0,0,0);
        if(!impactSound.isBlank())NativeEvents.sound(server,position(),impactSound,5);
        discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putFloat("Damage",damage);tag.putFloat("Radius",radius);tag.putInt("Life",life);tag.putInt("Element",entityData.get(ELEMENT));tag.putInt("Effect",entityData.get(EFFECT));tag.putInt("WeaponVisual",entityData.get(WEAPON));tag.putString("ImpactSound",impactSound);if(homingTarget!=null)tag.putUUID("HomingTarget",homingTarget); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);damage=tag.getFloat("Damage");radius=tag.getFloat("Radius");life=tag.getInt("Life");entityData.set(ELEMENT,tag.getInt("Element"));entityData.set(EFFECT,tag.getInt("Effect"));entityData.set(WEAPON,tag.getInt("WeaponVisual"));impactSound=tag.getString("ImpactSound");homingTarget=tag.hasUUID("HomingTarget")?tag.getUUID("HomingTarget"):null; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
