package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Five-tick parry window exported as interaction entities in the source pack. */
public final class EmissaryHitRegion extends Entity {
    private static final EntityDataAccessor<Float> WIDTH=SynchedEntityData.defineId(EmissaryHitRegion.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT=SynchedEntityData.defineId(EmissaryHitRegion.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PARRY=SynchedEntityData.defineId(EmissaryHitRegion.class,EntityDataSerializers.BOOLEAN);
    private UUID owner;
    private int remaining=5,missingOwnerTicks;
    private float damage;
    private Combat.Element element=Combat.Element.DISSONANCE;
    public EmissaryHitRegion(EntityType<? extends EmissaryHitRegion> type,Level level){super(type,level);noPhysics=true;}
    static EmissaryHitRegion create(ServerLevel level,NativeBoss boss,Vec3 at,float width,float height,float damage,Combat.Element element,boolean parry){
        var region=Content.EMISSARY_HIT_REGION.get().create(level);region.owner=boss.getUUID();region.damage=damage;region.element=element;
        region.entityData.set(WIDTH,width);region.entityData.set(HEIGHT,height);region.entityData.set(PARRY,parry);region.refreshDimensions();region.setPos(at);level.addFreshEntity(region);return region;
    }
    boolean ownedBy(NativeBoss boss){return boss.getUUID().equals(owner);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel server))return;
        if(owner==null||!(server.getEntity(owner) instanceof NativeBoss boss)){if(++missingOwnerTicks>20)discard();return;}
        if(!boss.isAlive()){discard();return;}missingOwnerTicks=0;
        if(--remaining>0)return;
        // Intersect the exported interaction volume, not a sphere centred on the boss.
        for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.3,0,.3)))
            if(element==null)NativeBoss.emissaryPhysicalHit(boss,target,damage);else Combat.hit(boss,target,damage,element);
        discard();
    }
    @Override public boolean isPickable(){return entityData.get(PARRY)&&!isRemoved();}
    @Override public boolean hurt(DamageSource source,float amount){
        if(!isPickable()||!(source.getEntity() instanceof Player player)||!(level() instanceof ServerLevel server)
            ||owner==null||!(server.getEntity(owner) instanceof NativeBoss boss)||!Combat.canHit(boss,player))return false;
        boss.parryEmissary(server);return true;
    }
    @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(entityData.get(WIDTH),entityData.get(HEIGHT));}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(WIDTH.equals(key)||HEIGHT.equals(key))refreshDimensions();}
    @Override protected void defineSynchedData(){entityData.define(WIDTH,1f);entityData.define(HEIGHT,1f);entityData.define(PARRY,true);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);tag.putInt("Remaining",remaining);tag.putFloat("Damage",damage);tag.putFloat("Width",entityData.get(WIDTH));tag.putFloat("Height",entityData.get(HEIGHT));tag.putBoolean("Parry",entityData.get(PARRY));tag.putInt("Element",element==null?-1:element.ordinal());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;remaining=tag.getInt("Remaining");damage=tag.getFloat("Damage");entityData.set(WIDTH,tag.getFloat("Width"));entityData.set(HEIGHT,tag.getFloat("Height"));entityData.set(PARRY,tag.getBoolean("Parry"));element=tag.getInt("Element")==-1?null:Combat.Element.values()[Math.floorMod(tag.getInt("Element"),6)];refreshDimensions();}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
