package com.flubberschnub.nyctocosm;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Independent carrier for the Emissary's 110-tick Sword Plunge attack. */
public final class EmissarySwordPlunge extends Entity {
    static final int MAX_AGE = 110;
    static final int FAST_SUBSTEPS = 200;
    static final int TOTAL_SUBSTEPS = 250;
    private static final int SUBSTEPS_PER_TICK = 3;
    private static final float SOURCE_DAMAGE = 60;
    private UUID owner;
    private int age;
    private int substeps;
    private boolean shockwavePlayed;
    private boolean explosionPlayed;

    public EmissarySwordPlunge(EntityType<? extends EmissarySwordPlunge> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO); // The source is an ItemDisplay; its NBT Motion is not integrated by Display.tick.
    }

    static EmissarySwordPlunge create(ServerLevel level, NativeBoss boss) {
        // The source selector is @a (online players), rather than all Player entities in nearby chunks.
        var nearby = new java.util.LinkedHashSet<Player>(level.players());
        nearby.addAll(level.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(300)));
        var candidates = nearby.stream().filter(player ->
            !player.isSpectator() && !player.getTags().contains("emissarydebug") && player.distanceToSqr(boss) <= 300 * 300).toList();
        if (candidates.isEmpty()) return null;
        return createAtTarget(level, boss, candidates.get(boss.getRandom().nextInt(candidates.size())), boss.getRandom());
    }

    static EmissarySwordPlunge createAtTarget(ServerLevel level, NativeBoss boss, Player target) {
        return createAtTarget(level, boss, target, boss.getRandom());
    }

    static void handCue(ServerLevel level,NativeBoss boss,int frame){
        var hand=EmissaryDamagePose.sampleAt(boss,"phase2.attack.sword_plunge","righthand",frame);
        for(int sample=0;sample<15;sample++){
            Vec3 at=EmissaryDamagePose.offsetFromLocator(boss,hand,0,0,sample*.2);
            level.sendParticles(ParticleTypes.FIREWORK,at.x,at.y,at.z,3,.2,.2,.2,.2);
            level.sendParticles(ParticleTypes.FLASH,at.x,at.y,at.z,2,.2,.2,.2,0);
        }
    }

    private static EmissarySwordPlunge createAtTarget(ServerLevel level, NativeBoss boss, Player target, net.minecraft.util.RandomSource random) {
        if (target == null || target.isSpectator() || target.getTags().contains("emissarydebug")
            || target.level() != level || target.distanceToSqr(boss) > 300 * 300) return null;

        // Sword Plunge block344 has no Rotation; block335 belongs to a different attack.
        return createAtTarget(level, boss, target, 0, 0);
    }

    static EmissarySwordPlunge createAtTarget(ServerLevel level, NativeBoss boss, Player target, int yaw, int pitch) {
        if (target == null || target.isSpectator() || target.getTags().contains("emissarydebug")
            || target.level() != level || target.distanceToSqr(boss) > 300 * 300) return null;
        int surfaceY = surfaceY(level, target.position());
        var sword = Content.EMISSARY_SWORD_PLUNGE.get().create(level);
        sword.owner = boss.getUUID();
        sword.setPos(target.getX(), surfaceY + 220, target.getZ());
        sword.setYRot(0);
        sword.setXRot(0);
        sword.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(sword);
        NativeEvents.sound(level, new Vec3(target.getX(), surfaceY, target.getZ()), "emissary.attack.sword_plunge.fall", 20);
        return sword;
    }

    int ageForTest() { return age; }
    int substepsForTest() { return substeps; }
    boolean shockwavePlayedForTest() { return shockwavePlayed; }
    boolean explosionPlayedForTest() { return explosionPlayed; }
    UUID ownerForTest() { return owner; }
    @Override public AABB getBoundingBoxForCulling(){return getBoundingBox().inflate(80);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<=256*256;}

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (explosionPlayed) { discard(); return; }
        age++;

        Set<UUID> hitThisTick = new HashSet<>();
        for (int step = 0; step < SUBSTEPS_PER_TICK && substeps < TOTAL_SUBSTEPS; step++) {
            substeps++;
            double distance = substeps <= FAST_SUBSTEPS ? 1.0 : 0.2;
            setPos(getX(), getY() - distance, getZ());
            collectSourceSweepPlayers(server, hitThisTick);
        }
        applySweepHits(server, hitThisTick);

        if (age == 105 && !shockwavePlayed) {
            shockwavePlayed = true;
            sourceShockwave(server);
        }
        if (age >= MAX_AGE && !explosionPlayed) explode(server);
    }

    private void collectSourceSweepPlayers(ServerLevel server, Set<UUID> hitThisTick) {
        // slowcast.mcfunction selects dx=19,dy=100,dz=3 after positioning at sword (-10,-60,-2).
        // Selector dimensions expand by one block on their positive ends, matching Minecraft's delta box.
        var selection = new AABB(getX() - 10, getY() - 60, getZ() - 2,
            getX() + 10, getY() + 41, getZ() + 2);
        for (Player player : server.getEntitiesOfClass(Player.class, selection,
            candidate -> !candidate.isSpectator() && !candidate.getTags().contains("emissarydebug")
                && selection.contains(candidate.position()))) hitThisTick.add(player.getUUID());
    }

    private void applySweepHits(ServerLevel server, Set<UUID> hitThisTick) {
        Entity found = owner == null ? null : server.getEntity(owner);
        NativeBoss boss = found instanceof NativeBoss emissary ? emissary : null;
        for (UUID id : hitThisTick) {
            Entity entity = server.getEntity(id);
            if (entity instanceof Player player)
                Combat.hitGeneric(this, boss, player, SOURCE_DAMAGE);
        }
    }

    private void sourceShockwave(ServerLevel server) {
        // block 334 invokes six source shockwaves at -10, 0, 10, 20, 30, and 40 from the sword.
        for (int y : new int[]{-10, 0, 10, 20, 30, 40})
            server.sendParticles(ParticleTypes.FLASH, getX(), getY() + y, getZ(), 4, .2, .2, .2, 0);
    }

    private void explode(ServerLevel server) {
        explosionPlayed = true; // Persist the one-shot before dispatching damage or visual events.
        Vec3 ground = new Vec3(getX(), surfaceY(server, position()), getZ());
        NativeEvents.sound(server, position(), "emissary.attack.sword_plunge.explode", 50);
        Vec3 base = position().add(0, -20, 0);
        for (int layer = 0; layer <= 9; layer++) {
            double y = base.y + layer * 8;
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), y, getZ(), 2, 4, 4, 4, 0);
            server.sendParticles(ParticleTypes.FLAME, getX(), y, getZ(), 24, 4, 4, 4, .4);
            server.sendParticles(ParticleTypes.FLASH, getX(), y, getZ(), 12, 4, 4, 4, 0);
            server.sendParticles(ParticleTypes.FIREWORK, getX(), y, getZ(), 18, 4, 4, 4, .4);
        }

        Entity found = owner == null ? null : server.getEntity(owner);
        NativeBoss boss = found instanceof NativeBoss emissary ? emissary : null;
        var area = new AABB(ground, ground).inflate(15);
        for (Player player : server.getEntitiesOfClass(Player.class, area,
            candidate -> !candidate.isSpectator() && !candidate.getTags().contains("emissarydebug")
                && candidate.hurtTime == 0 && candidate.distanceToSqr(ground) <= 15 * 15))
            Combat.hitGeneric(this, boss, player, SOURCE_DAMAGE);
        discard();
    }

    private static int surfaceY(ServerLevel level, Vec3 at) {
        BlockPos column = BlockPos.containing(at.x, at.y, at.z);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
    }

    @Override protected void defineSynchedData() {}

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Age", age);
        tag.putInt("Substeps", substeps);
        tag.putBoolean("ShockwavePlayed", shockwavePlayed);
        tag.putBoolean("ExplosionPlayed", explosionPlayed);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        age = Math.max(0, Math.min(MAX_AGE, tag.getInt("Age")));
        substeps = Math.max(0, Math.min(TOTAL_SUBSTEPS, tag.getInt("Substeps")));
        shockwavePlayed = tag.getBoolean("ShockwavePlayed");
        explosionPlayed = tag.getBoolean("ExplosionPlayed");
        setDeltaMovement(Vec3.ZERO);
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
