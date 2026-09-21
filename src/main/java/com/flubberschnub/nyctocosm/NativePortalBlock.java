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
            BlockState portal=Content.PORTAL.get().defaultBlockState().setValue(AXIS,axis.getAxis());
            for(int w=1;w<=20;w++)for(int h=1;h<=6;h++)level.setBlock(corner.relative(axis,w).above(h),portal,2);
            level.updateNeighborsAt(corner,Content.PORTAL.get());return true;
        }
        return false;
    }
    private static boolean frame(ServerLevel level,BlockPos corner,Direction axis){
        for(int x=1;x<=20;x++)if(!level.getBlockState(corner.relative(axis,x)).is(Blocks.REINFORCED_DEEPSLATE)||!level.getBlockState(corner.relative(axis,x).above(7)).is(Blocks.REINFORCED_DEEPSLATE))return false;
        for(int y=1;y<=6;y++)if(!level.getBlockState(corner.above(y)).is(Blocks.REINFORCED_DEEPSLATE)||!level.getBlockState(corner.relative(axis,21).above(y)).is(Blocks.REINFORCED_DEEPSLATE))return false;
        for(int x=1;x<=20;x++)for(int y=1;y<=6;y++){
            var state=level.getBlockState(corner.relative(axis,x).above(y));if(!state.isAir()&&!state.is(Content.PORTAL.get()))return false;
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
    }
}
