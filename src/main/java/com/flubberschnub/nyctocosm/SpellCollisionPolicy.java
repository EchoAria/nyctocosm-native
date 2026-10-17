package com.flubberschnub.nyctocosm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/** Block-collision semantics for Nyctocosm-owned moving attacks. */
public enum SpellCollisionPolicy {
    /** Use vanilla collision shapes, as unreviewed/native-only attacks currently do. */
    VANILLA_PHYSICAL,
    /** Treat every voxel outside nyctocosm:passthrough as a block hit. */
    SOURCE_PASSTHROUGH,
    /** Stay at the current point while it is outside nyctocosm:passthrough. */
    SOURCE_HOLD_IN_BLOCKED_VOXEL,
    /** Ignore blocks while retaining the caller's entity sweep. */
    IGNORE_BLOCKS;

    private static final TagKey<Block> PASSTHROUGH=TagKey.create(Registries.BLOCK,
        new ResourceLocation(NativeMod.ID,"passthrough"));
    private static final double TIE_EPSILON=1.0E-9;
    private static final int MAX_VOXELS_PER_SWEEP=1024;

    static boolean isSourcePassthrough(BlockState state){return state.is(PASSTHROUGH);}

    HitResolution resolve(SpellBolt bolt,Vec3 movement) {
        if(this==SOURCE_HOLD_IN_BLOCKED_VOXEL&&!isSourcePassthrough(bolt.level().getBlockState(bolt.blockPosition())))
            return new HitResolution(null,false,true);

        Vec3 start=bolt.position(),end=start.add(movement);
        if(this==VANILLA_PHYSICAL)return new HitResolution(
            ProjectileUtil.getHitResultOnMoveVector(bolt,bolt::canHitEntity),false,false);

        HitResult entityHit=ProjectileUtil.getEntityHitResult(bolt.level(),bolt,start,end,
            bolt.getBoundingBox().expandTowards(movement).inflate(1),bolt::canHitEntity);
        if(this==IGNORE_BLOCKS||this==SOURCE_HOLD_IN_BLOCKED_VOXEL)return new HitResolution(entityHit,false,false);

        VoxelSweep blockSweep=firstNonPassthroughVoxel(bolt.level(),start,end);
        if(blockSweep.unloadedBoundary())return new HitResolution(null,true,false);
        BlockHitResult blockHit=blockSweep.hit();
        if(blockHit==null)return new HitResolution(entityHit,false,false);
        if(entityHit==null||entityHit.getType()==HitResult.Type.MISS
            ||entityHit.getLocation().distanceToSqr(start)>=blockHit.getLocation().distanceToSqr(start))
            return new HitResolution(blockHit,false,false);
        return new HitResolution(entityHit,false,false);
    }

    /**
     * Supercover voxel traversal. Simultaneous axis crossings inspect the touched
     * side cells too, so a diagonal corner cannot skip an untagged block.
     */
    static VoxelSweep firstNonPassthroughVoxel(Level level,Vec3 start,Vec3 end) {
        Vec3 delta=end.subtract(start);
        if(!Double.isFinite(start.x)||!Double.isFinite(start.y)||!Double.isFinite(start.z)
            ||!Double.isFinite(end.x)||!Double.isFinite(end.y)||!Double.isFinite(end.z)
            ||Math.max(Math.abs(delta.x),Math.max(Math.abs(delta.y),Math.abs(delta.z)))>MAX_VOXELS_PER_SWEEP)
            return new VoxelSweep(null,true);

        int x=BlockPos.containing(start).getX(),y=BlockPos.containing(start).getY(),z=BlockPos.containing(start).getZ();
        BlockPos initial=new BlockPos(x,y,z);
        BlockHitResult initialHit=check(level,initial,start,oppositeDominantFace(delta));
        if(initialHit!=null)return new VoxelSweep(initialHit,!level.hasChunkAt(initial));
        if(!level.hasChunkAt(initial))return new VoxelSweep(null,true);

        int sx=sign(delta.x),sy=sign(delta.y),sz=sign(delta.z);
        double tx=firstBoundary(start.x,x,delta.x,sx),ty=firstBoundary(start.y,y,delta.y,sy),tz=firstBoundary(start.z,z,delta.z,sz);
        double dx=delta.x==0?Double.POSITIVE_INFINITY:Math.abs(1.0/delta.x);
        double dy=delta.y==0?Double.POSITIVE_INFINITY:Math.abs(1.0/delta.y);
        double dz=delta.z==0?Double.POSITIVE_INFINITY:Math.abs(1.0/delta.z);

        for(int crossed=0;crossed<MAX_VOXELS_PER_SWEEP;crossed++){
            double next=Math.min(tx,Math.min(ty,tz));
            if(next>1.0+TIE_EPSILON||!Double.isFinite(next))return new VoxelSweep(null,false);
            boolean crossX=Math.abs(tx-next)<=TIE_EPSILON;
            boolean crossY=Math.abs(ty-next)<=TIE_EPSILON;
            boolean crossZ=Math.abs(tz-next)<=TIE_EPSILON;
            int mask=(crossX?1:0)|(crossY?2:0)|(crossZ?4:0);
            Vec3 at=start.add(delta.scale(Math.max(0,next)));

            // Check every non-empty subset of tied axes, in stable X/Y/Z order.
            for(int bits=1;bits<=7;bits++){
                if((bits&mask)!=bits)continue;
                int nx=x+(((bits&1)!=0)?sx:0),ny=y+(((bits&2)!=0)?sy:0),nz=z+(((bits&4)!=0)?sz:0);
                Direction face=(bits&1)!=0?faceFor(Direction.Axis.X,sx):(bits&2)!=0?faceFor(Direction.Axis.Y,sy):faceFor(Direction.Axis.Z,sz);
                BlockPos pos=new BlockPos(nx,ny,nz);
                BlockHitResult hit=check(level,pos,at,face);
                if(hit!=null)return new VoxelSweep(hit,!level.hasChunkAt(pos));
                if(!level.hasChunkAt(pos))return new VoxelSweep(null,true);
            }

            if(crossX){x+=sx;tx+=dx;}
            if(crossY){y+=sy;ty+=dy;}
            if(crossZ){z+=sz;tz+=dz;}
        }
        return new VoxelSweep(null,true);
    }

    private static BlockHitResult check(Level level,BlockPos pos,Vec3 at,Direction face){
        if(pos.getY()<level.getMinBuildHeight()||pos.getY()>=level.getMaxBuildHeight())
            return new BlockHitResult(at,face,pos,false);
        if(!level.hasChunkAt(pos))return null;
        return isSourcePassthrough(level.getBlockState(pos))?null:new BlockHitResult(at,face,pos,false);
    }
    private static double firstBoundary(double start,int cell,double delta,int step){
        if(step>0)return ((cell+1.0)-start)/delta;
        if(step<0)return (start-cell)/-delta;
        return Double.POSITIVE_INFINITY;
    }
    private static int sign(double value){return value>0?1:value<0?-1:0;}
    private static Direction faceFor(Direction.Axis axis,int step){
        return switch(axis){case X->step>0?Direction.WEST:Direction.EAST;case Y->step>0?Direction.DOWN:Direction.UP;case Z->step>0?Direction.NORTH:Direction.SOUTH;};
    }
    private static Direction oppositeDominantFace(Vec3 delta){
        double ax=Math.abs(delta.x),ay=Math.abs(delta.y),az=Math.abs(delta.z);
        if(ax>=ay&&ax>=az)return faceFor(Direction.Axis.X,sign(delta.x)==0?1:sign(delta.x));
        if(ay>=az)return faceFor(Direction.Axis.Y,sign(delta.y)==0?1:sign(delta.y));
        return faceFor(Direction.Axis.Z,sign(delta.z)==0?1:sign(delta.z));
    }

    record HitResolution(HitResult hit,boolean unloadedBoundary,boolean holdPosition){}
    record VoxelSweep(BlockHitResult hit,boolean unloadedBoundary){}
}
