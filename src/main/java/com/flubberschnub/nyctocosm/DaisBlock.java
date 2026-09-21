package com.flubberschnub.nyctocosm;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class DaisBlock extends Block {
    public DaisBlock(){super(BlockBehaviour.Properties.of().strength(5,1200).lightLevel(state->7).noOcclusion());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide)player.openMenu(new SimpleMenuProvider((id,inventory,p)->new DaisMenu(id,inventory,ContainerLevelAccess.create(level,pos)),Component.translatable("gui.nyctocosm.dais_title")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
