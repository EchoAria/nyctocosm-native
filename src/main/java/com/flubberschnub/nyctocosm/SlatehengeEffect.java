package com.flubberschnub.nyctocosm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Persistent firework marker embedded in both world-generated and dynamically placed Slatehenges. */
public final class SlatehengeEffect extends Entity {
    public SlatehengeEffect(EntityType<? extends SlatehengeEffect> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        boolean active = !level().getBlockState(blockPosition()).isAir();
        server.sendParticles(ParticleTypes.FIREWORK, getX(), getY(), getZ(), 2, 1, 1, 1, 0);
        if (!active) discard();
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
