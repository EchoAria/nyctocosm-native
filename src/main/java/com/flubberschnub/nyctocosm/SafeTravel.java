package com.flubberschnub.nyctocosm;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;

/** Bounded, read-only destination search. Never builds platforms or clears user blocks. */
public final class SafeTravel {
    public static boolean canStand(ServerLevel level,Player player,Vec3 at){
        if(!Double.isFinite(at.x)||!Double.isFinite(at.y)||!Double.isFinite(at.z))return false;
        AABB box=player.getBoundingBox().move(at.subtract(player.position()));
        if(box.minY<=level.getMinBuildHeight()||box.maxY>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(box))return false;
        if(!level.noCollision(player,box)||level.containsAnyLiquid(box))return false;
        var below=level.getBlockState(BlockPos.containing(at).below());
        if(!below.isCollisionShapeFullBlock(level,BlockPos.containing(at).below()))return false;
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY-.1,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
            var state=level.getBlockState(pos);
            if(!state.getFluidState().isEmpty()||state.is(Blocks.MAGMA_BLOCK)||state.is(Blocks.CACTUS)||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)
                ||state.is(Blocks.CAMPFIRE)||state.is(Blocks.SOUL_CAMPFIRE)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.POWDER_SNOW)||state.is(Content.PORTAL.get()))return false;
        }
        return true;
    }
    public static Optional<Vec3> find(ServerLevel level,Player player,Vec3 requested){
        if(!Double.isFinite(requested.x)||!Double.isFinite(requested.y)||!Double.isFinite(requested.z))return Optional.empty();
        if(!level.getWorldBorder().isWithinBounds(BlockPos.containing(requested)))return Optional.empty();
        if(canStand(level,player,requested))return Optional.of(requested);
        BlockPos center=BlockPos.containing(requested);
        for(int radius=0;radius<=16;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
            BlockPos column=center.offset(x,0,z);
            if(!level.getWorldBorder().isWithinBounds(column))continue;
            // First try the old elevation, important for underground return points.
            for(int dy:new int[]{0,1,-1,2,-2}){
                Vec3 candidate=Vec3.atBottomCenterOf(column.offset(0,dy,0));
                if(canStand(level,player,candidate))return Optional.of(candidate);
            }
            BlockPos surface=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column);
            Vec3 candidate=Vec3.atBottomCenterOf(surface);
            if(canStand(level,player,candidate))return Optional.of(candidate);
        }
        return Optional.empty();
    }
    private SafeTravel(){}
}
