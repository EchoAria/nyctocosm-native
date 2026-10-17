package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Native vehicle for grapple passengers; never teleports an unrelated combat target. */
public final class EmissaryGrappleSeat extends Entity {
    private UUID owner;
    private int missingOwnerTicks;
    public EmissaryGrappleSeat(EntityType<? extends EmissaryGrappleSeat> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    static EmissaryGrappleSeat create(ServerLevel level,NativeBoss boss){
        var seat=Content.EMISSARY_GRAPPLE.get().create(level);seat.owner=boss.getUUID();seat.setPos(boss.emissaryGrapplePosition());level.addFreshEntity(seat);return seat;
    }
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel server))return;
        Entity entity=owner==null?null:server.getEntity(owner);
        if(!(entity instanceof NativeBoss boss)){
            // Chunk/entity load order is not guaranteed. Never strand a player on an orphan seat.
            if(++missingOwnerTicks>20){ejectPassengers();discard();}return;
        }
        missingOwnerTicks=0;
        if(!boss.isAlive()||!boss.holdsEmissaryGrapple(getUUID())){ejectPassengers();discard();return;}
        follow(boss);
    }
    void follow(NativeBoss boss){
        setPos(boss.emissaryGrapplePosition());
        for(Entity passenger:getPassengers()){
            if(!passenger.isAlive()||passenger.isSpectator()){passenger.stopRiding();continue;}
            passenger.fallDistance=0;positionRider(passenger);
            if(passenger instanceof LivingEntity living&&living.hurtTime<5&&!NativeBoss.emissaryPassable(level(),BlockPos.containing(position())))
                living.hurt(level().damageSources().mobAttack(boss),25);
        }
        if(!getPassengers().isEmpty()&&level() instanceof ServerLevel server&&!NativeBoss.emissaryPassable(level(),blockPosition()))
            server.sendParticles(ParticleTypes.FLAME,getX(),getY(),getZ(),3,.3,.3,.3,.05);
    }
    @Override protected boolean canAddPassenger(Entity passenger){return true;}
    @Override public double getPassengersRidingOffset(){return 0;}
    @Override protected void positionRider(Entity passenger,MoveFunction move){if(hasPassenger(passenger))move.accept(passenger,getX(),getY(),getZ());}
    @Override protected void defineSynchedData(){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
