package com.flubberschnub.nyctocosm;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** A single source Flame Slash wavefront with its own steering, sampled surface and lifetime. */
public final class EmissaryFlameSlashWave extends Entity {
    static final int SOURCE_LIFETIME = 40;
    static final int SURFACE_SAMPLES = 20;
    static final double SOURCE_SPEED = .8;
    static final double SOURCE_RADIUS = 2;
    static final float SOURCE_DAMAGE = 50;

    private UUID owner;
    private UUID damageOwner;
    private int age;
    private boolean ending;
    private final Set<UUID> pendingTargets = new LinkedHashSet<>();

    public EmissaryFlameSlashWave(EntityType<? extends EmissaryFlameSlashWave> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    static EmissaryFlameSlashWave launch(ServerLevel level, NativeBoss boss, boolean firstSlash) {
        float yaw = boss.getYRot() + (firstSlash ? 60 : -60);
        double offset = firstSlash ? 1.5 : 2;
        Vec3 forward = look(yaw, 0);
        EmissaryFlameSlashWave wave = Content.EMISSARY_FLAME_SLASH_WAVE.get().create(level);
        wave.owner = boss.getUUID();
        wave.damageOwner = boss.getUUID();
        wave.setPos(boss.position().add(0, 1.5, 0).add(forward.scale(offset)));
        wave.setYRot(yaw);
        wave.setXRot(0);
        level.addFreshEntity(wave);
        NativeEvents.resonanceSound(level, boss.position(),
            firstSlash ? "emissary.attack.flame_slash.shoot" : "emissary.attack.flame_slash.shoot2", 5, 100);
        return wave;
    }

    void configureForTest(NativeBoss boss) { owner = boss.getUUID(); damageOwner = owner; }
    UUID ownerForTest() { return owner; }
    UUID damageOwnerForTest() { return damageOwner; }
    int ageForTest() { return age; }
    boolean endingForTest() { return ending; }
    int pendingTargetCountForTest() { return pendingTargets.size(); }
    Vec3 sampleForTest(int index) { return surfaceSample(index); }
    int captureSurfaceContactsForTest(ServerLevel server) {
        int before = pendingTargets.size();
        collectSurfaceContacts(server);
        return pendingTargets.size() - before;
    }
    void resolveContactsForTest(ServerLevel server) {
        resolveContacts(server);
        discard();
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (!server.hasChunkAt(blockPosition())) { discard(); return; }
        if (ending) { resolveContacts(server); discard(); return; }

        age++;
        steerToNearestPlayer(server);
        reflect(server);
        Vec3 next = position().add(getLookAngle().scale(SOURCE_SPEED));
        BlockPos nextBlock = BlockPos.containing(next);
        if (!server.hasChunkAt(nextBlock)) { discard(); return; }
        setPos(next);
        emitWavefront(server);
        collectSurfaceContacts(server);
        if (age >= SOURCE_LIFETIME || !NativeBoss.emissaryPassable(server, nextBlock)) ending = true;
    }

    private void steerToNearestPlayer(ServerLevel server) {
        Player target = server.getNearestPlayer(getX(), getY(), getZ(), 300,
            candidate -> candidate instanceof Player player && !player.isSpectator()
                && !player.getTags().contains("emissarydebug"));
        if (target == null) return;
        Vec3 aim = target.getEyePosition().subtract(position());
        if (aim.lengthSqr() < 1.0e-8) return;
        aim = aim.normalize();
        setYRot((float) (Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90));
        setXRot((float) -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, aim.y)))));
    }

    private void reflect(ServerLevel server) {
        var reflection=Reflectance.tryReflect(server,new ReflectableAttack(position(),damageOwner==null?owner:damageOwner,SOURCE_DAMAGE));
        if(reflection==null)return;
        damageOwner=reflection.owner().getUUID();
        setYRot(reflection.reflector().getYRot());
        setXRot(reflection.reflector().getXRot());
    }

    private void collectSurfaceContacts(ServerLevel server) {
        for (int sample = 0; sample < SURFACE_SAMPLES; sample++) {
            BlockPos cell = BlockPos.containing(surfaceSample(sample));
            AABB cellBox = new AABB(cell);
            for (Player player : server.getEntitiesOfClass(Player.class, cellBox, candidate ->
                !candidate.isSpectator() && !candidate.getTags().contains("emissarydebug")
                    && candidate.getBoundingBox().intersects(cellBox))) {
                if (player.blockPosition().equals(cell)) pendingTargets.add(player.getUUID());
            }
        }
    }

    private Vec3 surfaceSample(int index) {
        double angle = Math.floorMod(index, SURFACE_SAMPLES) * (Math.PI * 2 / SURFACE_SAMPLES);
        double yaw = Math.toRadians(getYRot());
        double pitch = Math.toRadians(getXRot());
        Vec3 forward = getLookAngle().normalize();
        Vec3 localUp = new Vec3(Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch),
            -Math.cos(yaw) * Math.sin(pitch));
        return position().add(localUp.scale(SOURCE_RADIUS * Math.cos(angle)))
            .add(forward.scale(SOURCE_RADIUS * Math.sin(angle)));
    }

    private void emitWavefront(ServerLevel server) {
        server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 3, .2, .2, .2, 0);
        server.sendParticles(ParticleTypes.WAX_ON, getX(), getY(), getZ(), 5, .2, .2, .2, .05);
        for (int sample = 0; sample < SURFACE_SAMPLES; sample++) {
            Vec3 at = surfaceSample(sample);
            server.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0, 0, 0, .05);
            server.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0, 0, 0, .05);
        }
        // Extend the source's force-particle cue beyond the default tracking radius.
        for (ServerPlayer viewer : server.players()) if (viewer.distanceToSqr(this) <= 512 * 512) {
            server.sendParticles(viewer, ParticleTypes.FLAME, true, getX(), getY() + 2, getZ(), 1, 0, 0, 0, .05);
            server.sendParticles(viewer, ParticleTypes.SMOKE, true, getX(), getY() + 2, getZ(), 1, 0, 0, 0, .05);
        }
    }

    private void resolveContacts(ServerLevel server) {
        Entity found = owner == null ? null : server.getEntity(owner);
        NativeBoss boss = found instanceof NativeBoss nativeBoss ? nativeBoss : null;
        Entity damageSource = damageOwner == null ? null : server.getEntity(damageOwner);
        LivingEntity attacker = damageSource instanceof LivingEntity living && living.isAlive() ? living : boss;
        for (UUID targetId : pendingTargets) {
            Entity entity = server.getEntity(targetId);
            if (entity instanceof Player player && Combat.canHit(attacker, player))
                Combat.hit(attacker, player, SOURCE_DAMAGE, Combat.Element.DISSONANCE);
        }
        pendingTargets.clear();
    }

    private static Vec3 look(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees);
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
            Math.cos(yaw) * Math.cos(pitch)).normalize();
    }

    @Override protected void defineSynchedData() {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        if (damageOwner != null) tag.putUUID("DamageOwner", damageOwner);
        tag.putInt("Age", age);
        tag.putBoolean("Ending", ending);
        ListTag targets = new ListTag();
        for (UUID target : pendingTargets) targets.add(StringTag.valueOf(target.toString()));
        tag.put("PendingTargets", targets);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        damageOwner = tag.hasUUID("DamageOwner") ? tag.getUUID("DamageOwner") : owner;
        age = Math.max(0, Math.min(SOURCE_LIFETIME, tag.getInt("Age")));
        ending = tag.getBoolean("Ending");
        pendingTargets.clear();
        ListTag targets = tag.getList("PendingTargets", 8);
        for (int index = 0; index < targets.size(); index++) {
            try { pendingTargets.add(UUID.fromString(targets.getString(index))); }
            catch (IllegalArgumentException ignored) { }
        }
        setNoGravity(true);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
