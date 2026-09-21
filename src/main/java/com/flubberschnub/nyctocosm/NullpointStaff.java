package com.flubberschnub.nyctocosm;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** The Sorcerer's original Nullpoint is a separate 300-health combat objective. */
public final class NullpointStaff extends Monster {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(NullpointStaff.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> ANIMATION=SynchedEntityData.defineId(NullpointStaff.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(NullpointStaff.class,EntityDataSerializers.LONG);
    private int animationRemaining;
    private int recallRemaining;
    private boolean notified;

    public NullpointStaff(EntityType<? extends NullpointStaff> type,Level level){
        super(type,level);
        setNoAi(true);setNoGravity(true);setPersistenceRequired();xpReward=0;
    }

    public static AttributeSupplier.Builder attributes(){
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,300).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,1);
    }

    @Override protected void defineSynchedData(){
        super.defineSynchedData();entityData.define(OWNER,Optional.empty());entityData.define(ANIMATION,"idle");entityData.define(START,0L);
    }

    public void bind(NativeBoss owner){entityData.set(OWNER,Optional.of(owner.getUUID()));}
    public Optional<UUID> ownerId(){return entityData.get(OWNER);}
    public String animation(){return entityData.get(ANIMATION);}
    public float animationTime(float partial){return (float)(level().getGameTime()-entityData.get(START))+partial;}
    public void emit(){play("emit",41);}

    private void play(String animation,int duration){
        entityData.set(ANIMATION,animation);entityData.set(START,level().getGameTime());animationRemaining=duration;
    }

    private NativeBoss owner(){
        if(!(level() instanceof ServerLevel server)||ownerId().isEmpty())return null;
        return server.getEntity(ownerId().get()) instanceof NativeBoss boss?boss:null;
    }

    public void recall(){
        if(recallRemaining>0)return;
        recallRemaining=25;play("recall",25);
        if(level() instanceof ServerLevel server)NativeEvents.sound(server,position(),"sorcerer.fall.recall",5);
    }

    @Override public void tick(){
        super.tick();setDeltaMovement(0,0,0);
        if(level().isClientSide)return;
        ServerLevel server=(ServerLevel)level();
        if(animationRemaining>0&&--animationRemaining==0&&recallRemaining==0)play("idle",0);
        if(recallRemaining>0&&--recallRemaining==0){discard();return;}
        if(tickCount%4==0){
            int count=Math.max(1,(int)Math.ceil(13*getHealth()/getMaxHealth()));
            server.sendParticles(new DustParticleOptions(new Vector3f(.741f,0,1),1.2f),getX(),getY()+1.7,getZ(),count,.32,.75,.32,.01);
        }
        if(owner()==null&&tickCount>40)discard();
    }

    @Override public boolean hurt(DamageSource source,float amount){
        if(recallRemaining>0||amount<=0)return false;
        NativeBoss owner=owner();
        if(owner!=null&&owner.transitionInvulnerable()&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
        if(amount>=getHealth()){
            setHealth(1);startRecallAndNotify();return true;
        }
        boolean hit=super.hurt(source,amount);
        if(hit&&isAlive()){
            play("hurt",6);
            if(level() instanceof ServerLevel server)NativeEvents.sound(server,position(),"sorcerer.staff.hurt",4);
        }
        return hit;
    }

    private void startRecallAndNotify(){
        if(!notified){notified=true;NativeBoss boss=owner();if(boss!=null)boss.onStaffBroken(this);}
        recall();
    }

    @Override public void die(DamageSource source){startRecallAndNotify();}
    @Override public boolean isPushable(){return false;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){}

    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);ownerId().ifPresent(id->tag.putUUID("Owner",id));tag.putString("StaffAnimation",animation());tag.putInt("Recall",recallRemaining);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);if(tag.hasUUID("Owner"))entityData.set(OWNER,Optional.of(tag.getUUID("Owner")));
        recallRemaining=tag.getInt("Recall");play(recallRemaining>0?"recall":"idle",recallRemaining);
    }
}
