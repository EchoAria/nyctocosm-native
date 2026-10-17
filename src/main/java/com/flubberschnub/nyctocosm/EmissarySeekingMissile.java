package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Seven suspended nodes, activated together by the source animation's frame23. */
public final class EmissarySeekingMissile extends Entity {
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(EmissarySeekingMissile.class,EntityDataSerializers.BOOLEAN);
    private UUID owner,damageOwner;
    private int remaining,parkedAge,substeps;
    private boolean resolved;
    public EmissarySeekingMissile(EntityType<? extends EmissarySeekingMissile> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    static EmissarySeekingMissile summon(ServerLevel level,NativeBoss boss,int frame){
        var missile=Content.EMISSARY_SEEKING_MISSILE.get().create(level);
        missile.owner=boss.getUUID();missile.damageOwner=boss.getUUID();missile.setPos(boss.emissarySeekerMissileOrigin(frame));
        float yaw=switch(frame){case 2,4,6->110;case 8->180;default->-110;};
        float pitch=switch(frame){case 2,14->0;case 4,12->-20;case 6,10->-70;default->-80;};
        missile.setYRot(boss.getYRot()+yaw);missile.setXRot(pitch);level.addFreshEntity(missile);
        NativeEvents.resonanceSound(level,missile.position(),"emissary.attack.seeking_missiles.summon",5,100);
        return missile;
    }
    void activate(){if(active()||resolved)return;entityData.set(ACTIVE,true);remaining=60+random.nextInt(30);particle((ServerLevel)level(),ParticleTypes.FLASH,1,0,0,0,0);}
    boolean active(){return entityData.get(ACTIVE);}
    UUID ownerId(){return owner;}
    UUID damageOwnerId(){return damageOwner;}
    int remainingForTest(){return remaining;}
    int substepsForTest(){return substeps;}
    @Override public void tick(){
        super.tick();setDeltaMovement(Vec3.ZERO);
        if(!(level() instanceof ServerLevel server)||resolved)return;
        if(!active()){
            NativeNetwork.sourceVisual(server,"repair_seeker_idle",position(),getYRot(),getXRot());
            if(++parkedAge>100&&(owner==null||server.getEntity(owner)==null))discard();
            return;
        }
        remaining--;
        slowcast(server);if(!resolved)slowcast(server);
    }
    private void slowcast(ServerLevel server){
        var reflected=Reflectance.tryReflect(server,new ReflectableAttack(position(),damageOwner,15));
        if(reflected!=null){damageOwner=reflected.owner().getUUID();setYRot(reflected.reflector().getYRot());setXRot(reflected.reflector().getXRot());}
        if(remaining>60){setXRot(getXRot()+random.nextInt(20)-10);setYRot(getYRot()+random.nextInt(20)-10);}
        else{
            Player nearest=server.getEntitiesOfClass(Player.class,new AABB(position(),position()).inflate(300),EmissarySeekingMissile::eligible).stream()
                .filter(player->player.distanceToSqr(this)<=300*300).min(java.util.Comparator.comparingDouble(player->player.distanceToSqr(this))).orElse(null);
            if(nearest!=null){var turn=SorcererCastMath.turn(getYRot(),getXRot(),nearest.getEyePosition().subtract(position()),6);setYRot(turn.yaw());setXRot(turn.pitch());}
        }
        Vec3 next=position().add(SerpentMotion.localForward(getYRot(),getXRot()).scale(.7));if(!server.hasChunkAt(BlockPos.containing(next))){discard();resolved=true;return;}
        setPos(next);substeps++;
        particle(server,ParticleTypes.CLOUD,2,.1,.1,.1,.05);particle(server,ParticleTypes.FIREWORK,1,.1,.1,.1,0);
        particle(server,ParticleTypes.ELECTRIC_SPARK,5,.1,.1,.1,0);particle(server,ParticleTypes.FLAME,5,.1,.1,.1,0);
        var box=new AABB(position().subtract(.5,.5,.5),position().add(.5,.5,.5));
        boolean contact=!server.getEntitiesOfClass(Player.class,box,EmissarySeekingMissile::eligible).isEmpty();
        if(contact||!NativeBoss.emissaryPassable(server,blockPosition()))impact(server,box);
        else if(remaining<=0){resolved=true;discard();} // Source timeout kills without impact damage.
    }
    private static boolean eligible(Player player){return !player.isRemoved()&&!player.isSpectator()&&!player.getTags().contains("emissarydebug");}
    private void impact(ServerLevel server,AABB box){
        if(resolved)return;resolved=true;
        Entity source=damageOwner==null?null:server.getEntity(damageOwner);
        LivingEntity attacker=source instanceof LivingEntity living?living:null;
        for(Player player:server.getEntitiesOfClass(Player.class,box,EmissarySeekingMissile::eligible))Combat.hit(attacker,player,15,Combat.Element.DISSONANCE);
        NativeEvents.resonanceSound(server,position(),"emissary.attack.seeking_missiles.impact",5,100);
        particle(server,ParticleTypes.EXPLOSION,1,0,0,0,0);discard();
    }
    private void particle(ServerLevel server,ParticleOptions particle,int count,double x,double y,double z,double speed){
        for(ServerPlayer viewer:server.players())if(viewer.distanceToSqr(this)<=512*512)server.sendParticles(viewer,particle,true,getX(),getY(),getZ(),count,x,y,z,speed);
    }
    @Override protected void defineSynchedData(){entityData.define(ACTIVE,false);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){
        if(owner!=null)tag.putUUID("Owner",owner);if(damageOwner!=null)tag.putUUID("DamageOwner",damageOwner);
        tag.putBoolean("Active",active());tag.putInt("Remaining",remaining);tag.putInt("ParkedAge",parkedAge);tag.putInt("Substeps",substeps);tag.putBoolean("Resolved",resolved);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;damageOwner=tag.hasUUID("DamageOwner")?tag.getUUID("DamageOwner"):owner;
        entityData.set(ACTIVE,tag.getBoolean("Active"));remaining=Math.max(0,Math.min(89,tag.getInt("Remaining")));parkedAge=Math.max(0,tag.getInt("ParkedAge"));substeps=Math.max(0,tag.getInt("Substeps"));resolved=tag.getBoolean("Resolved");
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
