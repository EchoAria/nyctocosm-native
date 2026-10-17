package com.flubberschnub.nyctocosm;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Thirty source-oriented wave arms with independently anchored, persistent spike records. */
public final class IceSpikeWave extends Entity {
    private static final int ARM_COUNT = 30;
    private static final int RING_COUNT = 5;
    private static final int WAVE_MARKER_END_AGE = 25;
    private static final int ENTITY_END_AGE = 46;
    private static final int NO_SURFACE = Integer.MIN_VALUE;
    private static final String AGE_KEY = "Age";
    private static final String YAW_KEY = "BaseYaw";
    private static final String MARKER_SURFACE_KEY = "MarkerSurfaceY";
    private static final String SPIKE_SURFACE_KEY = "SpikeSurfaceY";
    private static final TagKey<Block> PASSTHROUGH = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(NativeMod.ID, "passthrough"));
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(IceSpikeWave.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<CompoundTag> STATE = SynchedEntityData.defineId(IceSpikeWave.class, EntityDataSerializers.COMPOUND_TAG);

    private int waveAge;
    private float baseYaw;
    private final int[] markerSurfaceY = new int[ARM_COUNT];
    private final int[] spikeSurfaceY = new int[ARM_COUNT * RING_COUNT];

    public IceSpikeWave(EntityType<? extends IceSpikeWave> type, Level level) {
        super(type, level);
        noPhysics = true;
        Arrays.fill(markerSurfaceY, NO_SURFACE);
        Arrays.fill(spikeSurfaceY, NO_SURFACE);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER, Optional.empty());
        entityData.define(STATE, new CompoundTag());
    }

    public static IceSpikeWave spawn(ServerLevel level, LivingEntity owner, Vec3 origin) {
        return spawn(level, owner, origin, owner.getYRot());
    }

    static IceSpikeWave spawn(ServerLevel level, LivingEntity owner, Vec3 origin, float baseYaw) {
        IceSpikeWave wave = Content.ICE_SPIKE_WAVE.get().create(level);
        if (wave == null) throw new IllegalStateException("Ice spike wave entity is unavailable");
        wave.entityData.set(OWNER, Optional.of(owner.getUUID()));
        wave.baseYaw = baseYaw;
        wave.setPos(origin);
        wave.publishState();
        level.addFreshEntity(wave);
        return wave;
    }

    Optional<UUID> ownerId() {
        return entityData.get(OWNER);
    }

    private LivingEntity owner() {
        if (!(level() instanceof ServerLevel server) || entityData.get(OWNER).isEmpty()) return null;
        return server.getEntity(entityData.get(OWNER).get()) instanceof LivingEntity living ? living : null;
    }

    public static Vec3 point(Vec3 origin, int arm, double radius) {
        return point(origin, arm, radius, 0);
    }

    public static Vec3 point(Vec3 origin, int arm, double radius, float baseYaw) {
        double angle = Math.toRadians(baseYaw + arm * 12.0);
        return origin.add(-Math.sin(angle) * radius, 0, Math.cos(angle) * radius);
    }

    static double markerRadiusAtAge(int age) {
        return .4 + age * .5;
    }

    static int ringBirthAge(int ring) {
        return 1 + ring * 5;
    }

    static double spikeRootOffset(int spikeAge) {
        if (spikeAge <= 5) return -2 + spikeAge * .4;
        if (spikeAge < 20) return 0;
        return -.4 * (spikeAge - 19);
    }

    static double spikeVisualOffset(double spikeAge) {
        if (spikeAge <= 5) return spikeAge * .4;
        if (spikeAge < 20) return 2;
        return Math.max(0, 2 - .4 * (spikeAge - 19));
    }

    static boolean withinImpactSphere(Vec3 targetFeet, Vec3 spikeRoot) {
        return targetFeet.distanceToSqr(spikeRoot) <= 4;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel server = (ServerLevel) level();
        LivingEntity caster = owner();
        if (caster == null) {
            discard();
            return;
        }

        waveAge++;
        if (waveAge >= ENTITY_END_AGE) {
            publishState();
            discard();
            return;
        }

        if (waveAge <= WAVE_MARKER_END_AGE) tickWaveMarker(server);
        tickSpikes(server, caster);
        publishState();
    }

    private void tickWaveMarker(ServerLevel server) {
        int ring = waveAge <= 21 && (waveAge - 1) % 5 == 0 ? (waveAge - 1) / 5 : -1;
        if (ring >= 0) {
            double radius = markerRadiusAtAge(waveAge);
            for (int arm = 0; arm < ARM_COUNT; arm++) {
                Vec3 marker = point(position(), arm, radius, baseYaw);
                int surface = surfaceY(server, marker.x, marker.z).orElse(NO_SURFACE);
                markerSurfaceY[arm] = surface;
                spikeSurfaceY[ring * ARM_COUNT + arm] = surface;
            }
        }

        double radius = markerRadiusAtAge(waveAge);
        for (int arm = 0; arm < ARM_COUNT; arm++) {
            int surface = markerSurfaceY[arm];
            if (surface == NO_SURFACE) continue;
            Vec3 marker = point(position(), arm, radius, baseYaw);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                marker.x, surface, marker.z, 5, .1, .1, .1, 1);
        }
    }

    private void tickSpikes(ServerLevel server, LivingEntity caster) {
        for (int ring = 0; ring < RING_COUNT; ring++) {
            int spikeAge = waveAge - ringBirthAge(ring);
            if (spikeAge < 0 || spikeAge >= 25) continue;
            double radius = markerRadiusAtAge(ringBirthAge(ring));
            for (int arm = 0; arm < ARM_COUNT; arm++) {
                int surface = spikeSurfaceY[ring * ARM_COUNT + arm];
                if (surface == NO_SURFACE) continue;
                Vec3 horizontal = point(position(), arm, radius, baseYaw);
                Vec3 root = new Vec3(horizontal.x, surface + spikeRootOffset(spikeAge), horizontal.z);

                if (spikeAge >= 1 && spikeAge <= 5) {
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW.defaultBlockState()),
                        root.x, root.y + 1, root.z, 5, .1, .1, .1, 1);
                }
                if (spikeAge >= 6 && spikeAge <= 10) {
                    AABB query = new AABB(root, root).inflate(2);
                    for (Player target : server.getEntitiesOfClass(Player.class, query)) {
                        if (!withinImpactSphere(target.position(), root) || !Combat.canHit(caster, target)) continue;
                        if (Combat.hit(caster, target, 3, Combat.Element.VOID))
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, true, true));
                    }
                }
            }
        }
    }

    private static OptionalInt surfaceY(ServerLevel level, double x, double z) {
        int blockX = net.minecraft.core.BlockPos.containing(x, 0, z).getX();
        int blockZ = net.minecraft.core.BlockPos.containing(x, 0, z).getZ();
        BlockPos column = new BlockPos(blockX, level.getMinBuildHeight(), blockZ);
        if (!level.hasChunkAt(column)) return OptionalInt.empty();

        for (int y = level.getMaxBuildHeight(); y > level.getMinBuildHeight(); y--) {
            BlockPos below = new BlockPos(blockX, y - 1, blockZ);
            if (!level.hasChunkAt(below)) return OptionalInt.empty();
            if (!level.getBlockState(below).is(PASSTHROUGH)) return OptionalInt.of(y);
        }
        return OptionalInt.empty();
    }

    private void publishState() {
        CompoundTag state = new CompoundTag();
        state.putInt(AGE_KEY, waveAge);
        state.putFloat(YAW_KEY, baseYaw);
        state.putIntArray(MARKER_SURFACE_KEY, markerSurfaceY.clone());
        state.putIntArray(SPIKE_SURFACE_KEY, spikeSurfaceY.clone());
        entityData.set(STATE, state);
    }

    private CompoundTag state() {
        return entityData.get(STATE);
    }

    public int waveAge() {
        return state().getInt(AGE_KEY);
    }

    public float baseYaw() {
        return state().getFloat(YAW_KEY);
    }

    int markerSurfaceY(int arm) {
        return arrayValue(state().getIntArray(MARKER_SURFACE_KEY), arm);
    }

    public int spikeSurfaceY(int ring, int arm) {
        return arrayValue(state().getIntArray(SPIKE_SURFACE_KEY), ring * ARM_COUNT + arm);
    }

    public static boolean hasSurface(int surfaceY) {
        return surfaceY != NO_SURFACE;
    }

    private static int arrayValue(int[] values, int index) {
        return index < 0 || index >= values.length ? NO_SURFACE : values[index];
    }

    public float spikeVisualOffset(int ring, float partialTick) {
        float age = waveAge() + partialTick - ringBirthAge(ring);
        if (age < 0 || age >= 25) return -1;
        return (float) spikeVisualOffset(age);
    }

    double currentMarkerRadius() {
        return markerRadiusAtAge(waveAge());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        waveAge = Math.max(0, Math.min(ENTITY_END_AGE, tag.contains(AGE_KEY) ? tag.getInt(AGE_KEY) : tickCount));
        baseYaw = tag.contains(YAW_KEY) ? tag.getFloat(YAW_KEY) : getYRot();
        Arrays.fill(markerSurfaceY, NO_SURFACE);
        Arrays.fill(spikeSurfaceY, NO_SURFACE);
        copySurfaceArray(tag.getIntArray(MARKER_SURFACE_KEY), markerSurfaceY);
        copySurfaceArray(tag.getIntArray(SPIKE_SURFACE_KEY), spikeSurfaceY);
        publishState();
    }

    private static void copySurfaceArray(int[] source, int[] target) {
        if (source.length == target.length) System.arraycopy(source, 0, target, 0, target.length);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(OWNER).ifPresent(id -> tag.putUUID("Owner", id));
        tag.putInt(AGE_KEY, waveAge);
        tag.putFloat(YAW_KEY, baseYaw);
        tag.putIntArray(MARKER_SURFACE_KEY, markerSurfaceY.clone());
        tag.putIntArray(SPIKE_SURFACE_KEY, spikeSurfaceY.clone());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
