package com.flubberschnub.nyctocosm;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;

/** Original dais/scan layout, expressed as block predicates instead of commands. */
public final class DaisRitual {
    public static boolean matches(Level level,BlockPos anvil){
        if(!level.getBlockState(anvil).is(Blocks.ANVIL)||!level.getBlockState(anvil.below()).is(Blocks.BEACON))return false;
        BlockPos base=anvil.below();
        for(Direction direction:Direction.Plane.HORIZONTAL){
            if(!level.getBlockState(base.relative(direction)).is(Blocks.QUARTZ_BLOCK))return false;
            var stair=level.getBlockState(base.relative(direction,2));
            if(!stair.is(Blocks.QUARTZ_STAIRS)||stair.getValue(StairBlock.FACING)!=direction.getOpposite()
                ||stair.getValue(StairBlock.HALF)!=Half.BOTTOM||stair.getValue(StairBlock.SHAPE)!=StairsShape.STRAIGHT)return false;
        }
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            var slab=level.getBlockState(base.offset(x,0,z));
            if(!slab.is(Blocks.WAXED_CUT_COPPER_SLAB)||slab.getValue(SlabBlock.TYPE)!=SlabType.BOTTOM)return false;
        }
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            boolean candle=(Math.abs(x)==3&&Math.abs(z)<=1)||(Math.abs(z)==3&&Math.abs(x)<=1)||(Math.abs(x)==2&&Math.abs(z)==2);
            if(!candle)continue;
            var state=level.getBlockState(base.offset(x,0,z));
            if(!state.is(BlockTags.CANDLES)||!state.hasProperty(CandleBlock.LIT)||!state.getValue(CandleBlock.LIT))return false;
        }
        return true;
    }
    public static boolean activate(ServerLevel level,BlockPos anvil){
        if(!matches(level,anvil))return false;
        // Keep the physical ritual base intact. A real block replaces the anvil and opens a native menu.
        if(!level.setBlockAndUpdate(anvil,Content.DAIS.get().defaultBlockState()))return false;
        Vec3 center=Vec3.atCenterOf(anvil);
        NativeEvents.sound(level,center,"dais.construct",5);
        level.sendParticles(ParticleTypes.SOUL,center.x,center.y,center.z,60,1.5,.7,1.5,.02);
        return true;
    }
    private DaisRitual(){}
}
