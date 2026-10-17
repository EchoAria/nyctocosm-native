package com.flubberschnub.nyctocosm;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Replaces the audited crypt end-piece marker that conditionally places a Slatehenge. */
public final class CryptSlatehengeTrigger extends Entity {
    private static final ResourceLocation SLATEHENGE = new ResourceLocation(NativeMod.ID, "slatehenge");
    private static final double LANDMARK_RADIUS = 200.0;

    public CryptSlatehengeTrigger(EntityType<? extends CryptSlatehengeTrigger> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && process((ServerLevel) level(), blockPosition())) discard();
    }

    /** Mirrors the source function: test/place once, then consume the crypt marker unconditionally. */
    static boolean process(ServerLevel level, BlockPos marker) {
        var data = StructureEventSavedData.get(level);
        if (data.processed(marker)) return true;

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, marker);
        Vec3 sourceOrigin = new Vec3(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        if (hasNearbyLandmark(level, sourceOrigin, LANDMARK_RADIUS)) {
            data.markProcessed(marker);
            return true;
        }

        StructureTemplate template = level.getStructureManager().get(SLATEHENGE).orElse(null);
        if (template == null) {
            data.markProcessed(marker);
            return true;
        }
        var rotatable = List.of(Blocks.BASALT, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES,
            Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.COBBLED_DEEPSLATE_WALL);
        var rotatableHolders = rotatable.stream().map(BuiltInRegistries.BLOCK::wrapAsHolder).toList();
        var settings = new StructurePlaceSettings()
            .setIgnoreEntities(false)
            .setRotation(Rotation.getRandom(level.random))
            .addProcessor(new BlockRotProcessor(HolderSet.direct(rotatableHolders), 0.75F));
        template.placeInWorld(level, surface, surface, settings, level.random, 2);
        data.markProcessed(marker);
        return true;
    }

    static boolean hasNearbyLandmark(ServerLevel level, Vec3 origin, double radius) {
        AABB search = new AABB(origin, origin).inflate(radius);
        double radiusSquared = radius * radius;
        return level.getEntitiesOfClass(SlatehengeEffect.class, search,
            entity -> !entity.isRemoved() && withinRadius(origin, entity.position(), radiusSquared)).size() > 0;
    }

    static boolean withinRadius(Vec3 origin, Vec3 landmark, double radiusSquared) {
        return landmark.distanceToSqr(origin) <= radiusSquared;
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
