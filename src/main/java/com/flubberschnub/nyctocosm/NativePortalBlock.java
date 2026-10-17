package com.flubberschnub.nyctocosm;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

/** Native block portal activated by Resonance in the original 20 x 6 inner frame. */
public final class NativePortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.HORIZONTAL_AXIS;
    public NativePortalBlock(){super(BlockBehaviour.Properties.of().noCollission().strength(-1).noLootTable().lightLevel(s->10).noOcclusion());registerDefaultState(stateDefinition.any().setValue(AXIS,Direction.Axis.X));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AXIS);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(AXIS)==Direction.Axis.X?box(0,0,7,16,16,9):box(7,0,0,9,16,16);}
    public static boolean activate(ServerLevel level,BlockPos hit){
        for(Direction axis:new Direction[]{Direction.EAST,Direction.SOUTH})for(int x=0;x<=21;x++)for(int y=0;y<=7;y++){
            BlockPos corner=hit.relative(axis,-x).below(y);
            if(!frame(level,corner,axis))continue;
            if(level.getBlockState(corner.relative(axis).above()).is(Content.PORTAL.get()))return false;
            var bounds=new net.minecraft.world.phys.AABB(corner,corner.relative(axis,21).above(7)).inflate(1);
            if(level.getEntitiesOfClass(PortalActivation.class,bounds,e->e.matches(corner,axis)).isEmpty())PortalActivation.start(level,corner,axis);
            return true;
        }
        return false;
    }
    static void open(ServerLevel level,BlockPos corner,Direction axis){
        BlockState portal=Content.PORTAL.get().defaultBlockState().setValue(AXIS,axis.getAxis());
        for(int w=1;w<=20;w++)for(int h=1;h<=6;h++)level.setBlock(corner.relative(axis,w).above(h),portal,2);
        level.updateNeighborsAt(corner,Content.PORTAL.get());
    }
    /** Remove only the connected native surface struck by a counterspell. */
    static boolean counter(ServerLevel level,BlockPos near){
        BlockPos first=null;
        for(BlockPos pos:BlockPos.betweenClosed(near.offset(-3,-3,-3),near.offset(3,3,3))){
            if(pos.distSqr(near)>9||!level.hasChunkAt(pos)||!level.getBlockState(pos).is(Content.PORTAL.get()))continue;
            first=pos.immutable();break;
        }
        if(first==null)return false;
        Direction.Axis axis=level.getBlockState(first).getValue(AXIS);
        var pending=new java.util.ArrayDeque<BlockPos>();var found=new java.util.HashSet<BlockPos>();pending.add(first);
        while(!pending.isEmpty()&&found.size()<120){
            BlockPos pos=pending.removeFirst();
            if(found.contains(pos)||!level.hasChunkAt(pos))continue;
            BlockState state=level.getBlockState(pos);
            if(!state.is(Content.PORTAL.get())||state.getValue(AXIS)!=axis)continue;
            found.add(pos);
            pending.add(pos.above());pending.add(pos.below());
            pending.add(pos.relative(axis==Direction.Axis.X?Direction.EAST:Direction.SOUTH));
            pending.add(pos.relative(axis==Direction.Axis.X?Direction.WEST:Direction.NORTH));
        }
        for(BlockPos pos:found)level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        NativeEvents.sound(level,Vec3.atCenterOf(first),"portal.deactivate",5);
        level.sendParticles(ParticleTypes.FLASH,first.getX()+.5,first.getY()+.5,first.getZ()+.5,10,1,2,1,0);
        return true;
    }
    static boolean frame(ServerLevel level,BlockPos corner,Direction axis){
        if(!level.hasChunkAt(corner)||!level.hasChunkAt(corner.relative(axis,21)))return false;
        for(int x=1;x<=20;x++)if(!level.getBlockState(corner.relative(axis,x)).is(Blocks.REINFORCED_DEEPSLATE)||!level.getBlockState(corner.relative(axis,x).above(7)).is(Blocks.REINFORCED_DEEPSLATE))return false;
        for(int y=1;y<=6;y++)if(!level.getBlockState(corner.above(y)).is(Blocks.REINFORCED_DEEPSLATE)||!level.getBlockState(corner.relative(axis,21).above(y)).is(Blocks.REINFORCED_DEEPSLATE))return false;
        for(int x=1;x<=20;x++)for(int y=1;y<=6;y++){
            var state=level.getBlockState(corner.relative(axis,x).above(y));if(!state.isAir()&&!state.is(net.minecraft.world.level.block.Blocks.SCULK_VEIN)&&!state.is(Content.PORTAL.get()))return false;
        }
        return true;
    }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        if(direction.getAxis()==Direction.Axis.Y||direction.getAxis()==state.getValue(AXIS)){
            if(!neighbor.is(this)&&!neighbor.is(Blocks.REINFORCED_DEEPSLATE))return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        if(!(entity instanceof ServerPlayer player)||player.isPassenger()||player.isVehicle())return;
        long time=player.server.overworld().getGameTime();
        var tag=player.getPersistentData();if(tag.getLong("NyctocosmPortalCooldown")>time)return;
        tag.putLong("NyctocosmPortalCooldown",time+100);
        if(level.dimension().location().getNamespace().equals(NativeMod.ID))NativeEvents.returnFromDimension(player);
        else NativeEvents.travel(player,"nyctocosm");
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        level.addParticle(ParticleTypes.SCULK_SOUL,pos.getX()+random.nextDouble(),pos.getY()+random.nextDouble(),pos.getZ()+random.nextDouble(),0,.02,0);
        if(random.nextInt(80)==0){var sound=Content.SOUND_MAP.get("portal.hum");if(sound!=null)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,sound.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1,false);}
    }
}
