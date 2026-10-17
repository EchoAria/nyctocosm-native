package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** One source-style curved projectile from the Emissary's Apocalypse Rain. */
public final class EmissaryRainShard extends Entity {
    static final int SOURCE_LIFETIME = 60;
    static final float SOURCE_DAMAGE = 12;
    private UUID owner;
    private UUID damageOwner;
    private boolean upper;
    private int age;
    private int substeps;
    private boolean resolved;

    public EmissaryRainShard(EntityType<? extends EmissaryRainShard> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    static EmissaryRainShard launch(ServerLevel level, NativeBoss boss, float yawOffset, float pitch, boolean upper) {
        EmissaryRainShard shard = Content.EMISSARY_RAIN_SHARD.get().create(level);
        shard.owner = boss.getUUID();
        shard.damageOwner = boss.getUUID();
        shard.upper = upper;
        shard.setPos(boss.position());
        shard.setYRot(boss.getYRot() + yawOffset + boss.getRandom().nextInt(16) - 8);
        shard.setXRot(pitch + boss.getRandom().nextInt(16) - 8);
        shard.yRotO = shard.getYRot();
        shard.xRotO = shard.getXRot();
        level.addFreshEntity(shard);
        return shard;
    }

    static void fire(ServerLevel level, NativeBoss boss) {
        NativeEvents.resonanceSound(level, boss.position(), "emissary.attack.apocalypse_rain.shoot", 20, 200);
        launch(level, boss, 90, -70, true);
        launch(level, boss, -90, -70, true);
        launch(level, boss, 90, 60, false);
        launch(level, boss, -90, 60, false);
    }

    UUID ownerId() { return owner; }
    boolean upper() { return upper; }
    int ageForTest() { return age; }
    int substepsForTest() { return substeps; }
    void upperForTest(boolean value) { upper = value; }
    void ageForTest(int value) { age = Math.max(0, Math.min(SOURCE_LIFETIME, value)); }
    void setOwnerForTest(NativeBoss boss) { owner = boss == null ? null : boss.getUUID(); damageOwner = owner; }
    UUID damageOwnerForTest() { return damageOwner; }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || resolved) return;
        age++;
        if (age >= SOURCE_LIFETIME) {
            impact(server);
            return;
        }

        steerPitch();
        slowcast(server);
        if (!resolved) slowcast(server);
    }

    private void steerPitch() {
        if (!upper) {
            if (getXRot() <= 80) setXRot(getXRot() + 3);
            return;
        }
        // These are sequential source commands. The later selectors see the pitch
        // written by the earlier command, so several increments can stack in one tick.
        if (getXRot() >= -90 && getXRot() <= -1) setXRot(getXRot() + 5);
        if (getXRot() >= 0 && getXRot() <= 15) setXRot(getXRot() + 8);
        if (getXRot() >= 0 && getXRot() <= 30) setXRot(getXRot() + 3);
        if (getXRot() >= 0 && getXRot() <= 60) setXRot(getXRot() + 1.5f);
        if (getXRot() >= 0 && getXRot() <= 90) setXRot(getXRot() + .8f);
    }

    private void slowcast(ServerLevel server) {
        reflect(server);
        Vec3 next = position().add(getLookAngle());
        BlockPos nextBlock = BlockPos.containing(next);
        if (!server.hasChunkAt(nextBlock)) {
            discard();
            return;
        }
        setPos(next);
        substeps++;
        emitTrail(server);
        if (!NativeBoss.emissaryPassable(server, nextBlock) || touchesPlayer(server)) resolve(server);
    }

    private void reflect(ServerLevel server) {
        var reflection=Reflectance.tryReflect(server,new ReflectableAttack(position(),damageOwner==null?owner:damageOwner,SOURCE_DAMAGE));
        if(reflection==null)return;
        damageOwner=reflection.owner().getUUID();
        setYRot(reflection.reflector().getYRot());
        setXRot(reflection.reflector().getXRot());
    }

    private boolean touchesPlayer(ServerLevel server) {
        AABB cell = new AABB(BlockPos.containing(position()));
        return !server.getEntitiesOfClass(Player.class, cell, player -> !player.isSpectator()
            && !player.getTags().contains("emissarydebug") && player.getBoundingBox().intersects(cell)).isEmpty();
    }

    private void emitTrail(ServerLevel server) {
        forceParticle(server, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, .01);
        forceParticle(server, ParticleTypes.FLAME, 0);
        forceParticle(server, ParticleTypes.FIREWORK, 0);
        forceParticle(server, ParticleTypes.WAX_ON, 0);
        forceParticle(server, ParticleTypes.CLOUD, 0);
    }

    private void forceParticle(ServerLevel server, ParticleOptions particle, double speed) {
        for (ServerPlayer viewer : server.players()) if (viewer.distanceToSqr(this) <= 512 * 512)
            server.sendParticles(viewer, particle, true, getX(), getY(), getZ(), 1, 0, 0, 0, speed);
    }

    private void impact(ServerLevel server) {
        if (resolved) return;
        resolved = true;
        Entity found = damageOwner == null ? null : server.getEntity(damageOwner);
        LivingEntity attacker = found instanceof LivingEntity living && living.isAlive() ? living : null;
        AABB search = new AABB(position(), position()).inflate(5);
        for (Player player : server.getEntitiesOfClass(Player.class, search, candidate -> !candidate.isSpectator()
            && !candidate.getTags().contains("emissarydebug") && candidate.distanceToSqr(this) <= 25)) {
            Combat.hit(attacker,player,SOURCE_DAMAGE,Combat.Element.DISSONANCE);
        }
        NativeEvents.masterSound(server, position(), SoundEvents.GENERIC_EXPLODE, 10, 50);
        forceParticle(server, ParticleTypes.EXPLOSION_EMITTER, 0);
        discard();
    }

    private void resolve(ServerLevel server) { impact(server); }

    @Override protected void defineSynchedData() {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        if (damageOwner != null) tag.putUUID("DamageOwner", damageOwner);
        tag.putBoolean("Upper", upper);
        tag.putInt("Age", age);
        tag.putInt("Substeps", substeps);
        tag.putBoolean("Resolved", resolved);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        damageOwner = tag.hasUUID("DamageOwner") ? tag.getUUID("DamageOwner") : owner;
        upper = tag.getBoolean("Upper");
        age = Math.max(0, Math.min(SOURCE_LIFETIME, tag.getInt("Age")));
        substeps = Math.max(0, tag.getInt("Substeps"));
        resolved = tag.getBoolean("Resolved");
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
