package com.flubberschnub.nyctocosm;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class ArtifactItem extends Item {
    public final String effect;
    public ArtifactItem(String effect) { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); this.effect=effect; }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.nyctocosm.artifact_passive"));
    }
}
