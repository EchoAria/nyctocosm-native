package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** The source Expulsion kill marker waits fifteen ticks before its explosion. */
public final class ExpulsionBurst extends Entity {
    private UUID owner;
    private int delay=15;

    public ExpulsionBurst(EntityType<? extends ExpulsionBurst> type,Level level){super(type,level);noPhysics=true;}

    public static void schedule(ServerLevel level,Player player,Vec3 position){
        var burst=new ExpulsionBurst(Content.EXPULSION_BURST.get(),level);
        burst.owner=player.getUUID();burst.setPos(position);level.addFreshEntity(burst);
    }

    public static void explode(ServerLevel level,Player player,Vec3 position){
        level.sendParticles(ParticleTypes.EXPLOSION,position.x,position.y,position.z,20,1,1,1,0);
        level.sendParticles(ParticleTypes.FLAME,position.x,position.y,position.z,20,0,0,0,.3);
        level.sendParticles(ParticleTypes.LAVA,position.x,position.y,position.z,20,.4,.4,.4,.3);
        level.playSound(null,position.x,position.y,position.z,SoundEvents.GENERIC_EXPLODE,SoundSource.MASTER,1,1);
        Combat.livingFeetSphere(level,player,position,6,7,Combat.Element.DISSONANCE);
    }

    @Override public void tick(){
        if(isRemoved())return;
        super.tick();
        if(!(level() instanceof ServerLevel level)||--delay>0)return;
        // Remove before applying damage: new kills schedule separate markers, never recurse here.
        discard();
        Player player=owner==null?null:level.getPlayerByUUID(owner);
        if(player!=null)explode(level,player,position());
    }
    @Override protected void defineSynchedData(){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);tag.putInt("Delay",delay);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;delay=Math.max(1,tag.getInt("Delay"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
