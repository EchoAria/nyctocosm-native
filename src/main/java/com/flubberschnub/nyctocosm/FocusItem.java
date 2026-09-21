package com.flubberschnub.nyctocosm;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class FocusItem extends Item {
    public final boolean mobile;
    public FocusItem(boolean mobile) { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); this.mobile=mobile; }
    public static int souls(ItemStack stack) { return stack.hasTag() && stack.getTag().contains("Souls") ? stack.getTag().getInt("Souls") : 1; }
    public static void addSoul(ItemStack stack, int count) { stack.getOrCreateTag().putInt("Souls",Math.max(0,Math.min(999,souls(stack)+count))); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if (!level.isClientSide) {
            var fields=level.getEntitiesOfClass(SeelicField.class,player.getBoundingBox().inflate(64),field -> field.ownedBy(player) && !field.isHypertrace());
            if (!fields.isEmpty()) { fields.forEach(SeelicField::discard); return InteractionResultHolder.success(stack); }
            if (souls(stack)<=0 && !player.isCreative()) { player.displayClientMessage(Component.translatable("message.nyctocosm.no_souls"),true); return InteractionResultHolder.fail(stack); }
            SeelicField field=new SeelicField(Content.FIELD.get(),level);
            field.configure(player,mobile,false);
            field.setPos(player.getX(),player.getY()+.08,player.getZ());
            if (level.addFreshEntity(field)) {
                if(!player.isCreative())addSoul(stack,-1);
                if(!mobile){
                    double yaw=Math.toRadians(player.getYRot());
                    var ahead=net.minecraft.core.BlockPos.containing(player.getX()-Math.sin(yaw),player.getY(),player.getZ()+Math.cos(yaw));
                    DaisRitual.activate((net.minecraft.server.level.ServerLevel)level,ahead);
                }
            }
            player.getCooldowns().addCooldown(this,10);
            player.displayClientMessage(Component.translatable("message.nyctocosm.field_open"),true);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.nyctocosm.focus_souls",souls(stack)));
        lines.add(Component.translatable("tooltip.nyctocosm.focus_use"));
        lines.add(Component.translatable("tooltip.nyctocosm.focus_cast"));
    }
}
