package com.flubberschnub.nyctocosm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server selects the chapter; clients only display the resulting immutable book. */
public final class NativeNetwork {
    private static final String VERSION="1";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        new ResourceLocation(NativeMod.ID,"main"),()->VERSION,VERSION::equals,VERSION::equals);
    public record OpenBook(ItemStack stack) {}
    public record WeaponEffect(int kind,Vec3 eye,Vec3 end,Vec3 forward) {}
    public static void register(){
        CHANNEL.messageBuilder(OpenBook.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message,buffer)->buffer.writeItem(message.stack()))
            .decoder(buffer->new OpenBook(buffer.readItem()))
            .consumerMainThread((message,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                ()->()->com.flubberschnub.nyctocosm.client.ClientEvents.openBook(message.stack())))
            .add();
        CHANNEL.messageBuilder(WeaponEffect.class,1,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message,buffer)->{
                buffer.writeVarInt(message.kind());writeVec(buffer,message.eye());writeVec(buffer,message.end());writeVec(buffer,message.forward());
            })
            .decoder(buffer->new WeaponEffect(buffer.readVarInt(),readVec(buffer),readVec(buffer),readVec(buffer)))
            .consumerMainThread((message,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                ()->()->com.flubberschnub.nyctocosm.client.ClientEvents.weaponEffect(message.kind(),message.eye(),message.end(),message.forward())))
            .add();
    }
    public static void openBook(ServerPlayer player,ItemStack book){
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new OpenBook(book));
    }
    public static void weaponEffect(LivingEntity owner,int kind,Vec3 eye,Vec3 end,Vec3 forward){
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->owner),new WeaponEffect(kind,eye,end,forward));
    }
    private static void writeVec(net.minecraft.network.FriendlyByteBuf buffer,Vec3 value){buffer.writeDouble(value.x);buffer.writeDouble(value.y);buffer.writeDouble(value.z);}
    private static Vec3 readVec(net.minecraft.network.FriendlyByteBuf buffer){return new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble());}
    private NativeNetwork(){}
}
