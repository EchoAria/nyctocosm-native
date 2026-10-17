package com.flubberschnub.nyctocosm;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** A persistent, telegraphed Celestial Smite origin and its bounded source-style raycast. */
public final class EmissaryCelestialTear extends Entity {
    static final int SOURCE_RANGE = 150;
    static final double SOURCE_CONTACT_RADIUS = 5;
    static final double SOURCE_IMPACT_RADIUS = 15;
    static final float SOURCE_DAMAGE = 80;
    private UUID owner;
    private int age;
    private int missingOwnerTicks;
    private int traceChecks;
    private boolean active;
    private boolean resolved;
    private boolean impacted;
    private Vec3 lastEndpoint;

    public EmissaryCelestialTear(EntityType<? extends EmissaryCelestialTear> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** The source start function runs tear, circles, and activate immediately, before its later keyframes. */
    static void fireStartupSequence(ServerLevel level, NativeBoss boss) {
        EmissaryCelestialTear tear = spawn(level, boss);
        if (tear == null) return;
        tear.activate();
        tear.fire(level, boss);
    }

    /** Source leaf_26 creates a second marker, which waits for leaf_50 and leaf_93. */
    static EmissaryCelestialTear createTelegraph(ServerLevel level, NativeBoss boss) {
        return spawn(level, boss);
    }

    private static EmissaryCelestialTear spawn(ServerLevel level, NativeBoss boss) {
        List<ServerPlayer> candidates = level.players().stream().filter(EmissaryCelestialTear::sourceTarget)
            .filter(player -> player.distanceToSqr(boss) <= 300 * 300).toList();
        if (candidates.isEmpty()) return null;

        ServerPlayer selected = candidates.get(boss.getRandom().nextInt(candidates.size()));
        Vec3 origin = selected.position().add(0, 100, 0);
        EmissaryCelestialTear tear = Content.EMISSARY_CELESTIAL_TEAR.get().create(level);
        if (tear == null) return null;
        tear.owner = boss.getUUID();
        tear.setPos(origin);
        ServerPlayer aimer = level.players().stream().filter(EmissaryCelestialTear::sourceTarget)
            .min(java.util.Comparator.comparingDouble(player -> player.distanceToSqr(origin))).orElse(selected);
        tear.aimAt(aimer.position());
        tear.yRotO = tear.getYRot();
        tear.xRotO = tear.getXRot();
        level.addFreshEntity(tear);
        tear.emitTearCue(level);
        NativeEvents.resonanceSound(level, origin, "emissary.attack.celestial_smite.tear", 50, 300);
        return tear;
    }

    private static boolean sourceTarget(Player player) {
        return !player.isSpectator() && !player.getTags().contains("emissarydebug");
    }

    static void activateOwned(ServerLevel level, NativeBoss boss) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EmissaryCelestialTear tear && boss.getUUID().equals(tear.owner) && !tear.isRemoved())
                tear.activate();
        }
    }

    static void fireOwned(ServerLevel level, NativeBoss boss) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EmissaryCelestialTear tear && boss.getUUID().equals(tear.owner) && !tear.isRemoved())
                tear.fire(level, boss);
        }
    }

    private void aimAt(Vec3 target) {
        Vec3 direction = target.subtract(position()).normalize();
        setYRot((float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90));
        setXRot((float) -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, direction.y)))));
    }

    private void emitTearCue(ServerLevel server) {
        Vec3 horizontal = Vec3.directionFromRotation(0, getYRot());
        Vec3 side = new Vec3(-horizontal.z, 0, horizontal.x);
        for (int point = 0; point < 16; point++) {
            double angle = Math.PI * 2 * point / 16;
            Vec3 ring = horizontal.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).scale(2.5);
            forceParticle(server, ParticleTypes.CLOUD, position().add(ring).add(0, 0.1, 0), 2, 0, 0, 0, 0);
        }
    }

    private void activate() {
        if (resolved) return;
        active = true;
        if (level() instanceof ServerLevel server) emitWarning(server);
    }

    private void emitWarning(ServerLevel server) {
        // The source's celestial_smite item effect is a two-circle telegraph. Keep it server-authoritative
        // and particle-only here while preserving the marker's saved position and direction.
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        double pulse = .85 + .15 * Math.sin(age * Math.PI / 6.0);
        for (int point = 0; point < 16; point++) {
            double angle = Math.PI * 2 * point / 16;
            Vec3 radial = forward.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
            forceParticle(server, ParticleTypes.END_ROD, position().add(radial.scale(4 * pulse)).add(0, .1, 0), 1, 0, 0, 0, 0);
            forceParticle(server, ParticleTypes.SOUL_FIRE_FLAME, position().add(radial.scale(2 * pulse)).add(0, .1, 0), 1, 0, 0, 0, 0);
        }
        forceParticle(server, ParticleTypes.FLAME, position().add(0, .15, 0), 4, .2, .2, .2, .01);
    }

    private void emitMeteorShockwave(ServerLevel server) {
        forceParticle(server, ParticleTypes.CLOUD, position(), 32, 2, .25, 2, .01);
        forceParticle(server, ParticleTypes.END_ROD, position(), 16, 1.5, .15, 1.5, .01);
    }

    private void fire(ServerLevel server, NativeBoss boss) {
        if (!active || resolved) return;
        resolved = true;
        TraceResult result = trace(server);
        impacted = result.impact;
        lastEndpoint = result.endpoint;
        if (result.impact) impact(server, boss, result.endpoint);

        // block/391 plays these at the original marker after raycast has returned.
        emitMeteorShockwave(server);
        forceParticle(server, ParticleTypes.EXPLOSION_EMITTER, position(), 1, 0, 0, 0, 0);
        NativeEvents.resonanceSound(server, position(), "emissary.attack.celestial_smite.shoot", 50, 300);
        discard();
    }

    private TraceResult trace(ServerLevel server) {
        Vec3 origin = position();
        Vec3 direction = getLookAngle();
        Vec3 point = origin;
        traceChecks = 0;
        for (int step = 1; step <= SOURCE_RANGE; step++) {
            traceChecks = step;
            BlockPos block = BlockPos.containing(point);
            if (!server.hasChunkAt(block)) return new TraceResult(point, false);

            boolean blocked = !NativeBoss.emissaryPassable(server, block);
            boolean playerContact = playerAt(server, point);
            if (blocked || playerContact) return new TraceResult(point, true);
            if (step == SOURCE_RANGE) return new TraceResult(point, false);

            emitTrail(server, point, step);
            point = point.add(direction);
        }
        return new TraceResult(point, false);
    }

    private static boolean playerAt(ServerLevel server, Vec3 point) {
        for (ServerPlayer player : server.players())
            if (player.position().distanceToSqr(point) <= SOURCE_CONTACT_RADIUS * SOURCE_CONTACT_RADIUS) return true;
        AABB search = new AABB(point, point).inflate(SOURCE_CONTACT_RADIUS);
        return !server.getEntitiesOfClass(Player.class, search,
            player -> player.position().distanceToSqr(point) <= SOURCE_CONTACT_RADIUS * SOURCE_CONTACT_RADIUS).isEmpty();
    }

    private void emitTrail(ServerLevel server, Vec3 point, int range) {
        forceParticle(server, ParticleTypes.EXPLOSION, point, 3, 1, 1, 1, 0);
        forceParticle(server, ParticleTypes.FLAME, point, 5, 1, 1, 1, .05);
        forceParticle(server, ParticleTypes.END_ROD, point, 5, 1, 1, 1, .05);
        forceParticle(server, ParticleTypes.WAX_ON, point, 5, 1, 1, 1, 0);
        if (range == 10 || range == 22 || range == 58 || range == 62 || range == 75) emitWavePulse(server, point, range < 60 ? 6 : 4);
    }

    private void emitWavePulse(ServerLevel server, Vec3 center, double radius) {
        for (int point = 0; point < 16; point++) {
            double angle = Math.PI * 2 * point / 16;
            Vec3 ring = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            forceParticle(server, ParticleTypes.END_ROD, ring, 2, .15, .15, .15, .01);
            forceParticle(server, ParticleTypes.CLOUD, ring, 1, .15, .15, .15, 0);
        }
    }

    private void impact(ServerLevel server, NativeBoss boss, Vec3 center) {
        AABB bounds = new AABB(center, center).inflate(SOURCE_IMPACT_RADIUS);
        java.util.Set<Player> candidates = new java.util.LinkedHashSet<>(server.players());
        candidates.addAll(server.getEntitiesOfClass(Player.class, bounds));
        for (Player player : candidates) {
            if (player.position().distanceToSqr(center) > SOURCE_IMPACT_RADIUS * SOURCE_IMPACT_RADIUS) continue;
            Combat.hit(boss, player, SOURCE_DAMAGE, Combat.Element.RADIANCE);
        }
        Vec3 effect = center.add(0, 1, 0);
        forceParticle(server, ParticleTypes.EXPLOSION_EMITTER, effect, 1, 0, 0, 0, 0);
        forceParticle(server, ParticleTypes.FLASH, effect, 1, 0, 0, 0, 0);
        forceParticle(server, ParticleTypes.END_ROD, effect, 40, 3, 1, 3, .08);
        NativeEvents.resonanceSound(server, center, "emissary.attack.celestial_smite.explosion", 50, 300);
        // The source's 9x5x9 `fill fire replace air` remains disabled until C03 is explicitly decided.
    }

    private void forceParticle(ServerLevel server, ParticleOptions particle, Vec3 at, int count,
                               double dx, double dy, double dz, double speed) {
        for (ServerPlayer viewer : server.players()) {
            if (viewer.distanceToSqr(at) <= 512 * 512)
                server.sendParticles(viewer, particle, true, at.x, at.y, at.z, count, dx, dy, dz, speed);
        }
    }

    UUID ownerForTest() { return owner; }
    boolean activeForTest() { return active; }
    int ageForTest() { return age; }
    int traceChecksForTest() { return traceChecks; }
    boolean impactedForTest() { return impacted; }
    Vec3 endpointForTest() { return lastEndpoint; }
    void activateForTest() { activate(); }
    void configureForTest(NativeBoss boss, boolean isActive) { owner = boss.getUUID(); active = isActive; }
    void ageForTest(int value) { age = Math.max(0, value); }
    void fireForTest(ServerLevel server, NativeBoss boss) { fire(server, boss); }
    static EmissaryCelestialTear createForTest(ServerLevel level, NativeBoss boss, Vec3 at, float yaw, float pitch, boolean active) {
        EmissaryCelestialTear tear = Content.EMISSARY_CELESTIAL_TEAR.get().create(level);
        tear.configureForTest(boss, active);
        tear.setPos(at);
        tear.setYRot(yaw);
        tear.setXRot(pitch);
        level.addFreshEntity(tear);
        return tear;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || resolved) return;
        age++;
        Entity found = owner == null ? null : server.getEntity(owner);
        if (!(found instanceof NativeBoss boss) || !boss.isAlive()) {
            if (++missingOwnerTicks > 20) discard();
            return;
        }
        missingOwnerTicks = 0;
        if (active) emitWarning(server);
    }

    private record TraceResult(Vec3 endpoint, boolean impact) {}

    @Override protected void defineSynchedData() {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Age", age);
        tag.putInt("MissingOwnerTicks", missingOwnerTicks);
        tag.putInt("TraceChecks", traceChecks);
        tag.putBoolean("Active", active);
        tag.putBoolean("Resolved", resolved);
        tag.putBoolean("Impacted", impacted);
        if (lastEndpoint != null) {
            tag.putDouble("EndpointX", lastEndpoint.x);
            tag.putDouble("EndpointY", lastEndpoint.y);
            tag.putDouble("EndpointZ", lastEndpoint.z);
        }
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        age = Math.max(0, tag.getInt("Age"));
        missingOwnerTicks = Math.max(0, tag.getInt("MissingOwnerTicks"));
        traceChecks = Math.max(0, Math.min(SOURCE_RANGE, tag.getInt("TraceChecks")));
        active = tag.getBoolean("Active");
        resolved = tag.getBoolean("Resolved");
        impacted = tag.getBoolean("Impacted");
        lastEndpoint = tag.contains("EndpointX") ? new Vec3(tag.getDouble("EndpointX"), tag.getDouble("EndpointY"), tag.getDouble("EndpointZ")) : null;
        setDeltaMovement(Vec3.ZERO);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
