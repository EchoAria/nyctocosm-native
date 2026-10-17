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

/** Server selects gameplay outcomes and visual samples; clients present immutable messages. */
public final class NativeNetwork {
    private static final String VERSION="6";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        new ResourceLocation(NativeMod.ID,"main"),()->VERSION,VERSION::equals,VERSION::equals);
    public record OpenBook(ItemStack stack) {}
    public record WeaponEffect(int kind,Vec3 eye,Vec3 end,Vec3 forward) {}
    public record EmissaryBeam(Vec3 start,Vec3 end,boolean wide) {}
    public record WeaponCooldowns(java.util.UUID playerId,int nullfissureSwing,int nullfissureCast,int semblanceSwing,int semblanceCast) {}
    public record SorcererRay(java.util.List<SorcererBeamNode.Sample> samples,boolean halo) {}
    public record SorcererCue(String clip,int frame,Vec3 entry,float yaw,float pitch,Vec3 after) {}
    public record SourceVisual(String clip,int frame,Vec3 at,float yaw,float pitch,long seed) {}
    public record EmissaryRay(java.util.List<SorcererBeamNode.Sample> samples,Vec3 end,boolean wide,boolean signal) {}
    public static void register(){
        CHANNEL.messageBuilder(EmissaryRay.class,7,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeBoolean(m.wide());b.writeBoolean(m.signal());writeVec(b,m.end());b.writeVarInt(m.samples().size());for(var sample:m.samples()){writeVec(b,sample.position());b.writeFloat(sample.yaw());b.writeFloat(sample.pitch());}})
            .decoder(b->{boolean wide=b.readBoolean(),signal=b.readBoolean();Vec3 end=readVec(b);int count=b.readVarInt();if(count<0||count>70)throw new IllegalArgumentException("Invalid Emissary ray samples");var samples=new java.util.ArrayList<SorcererBeamNode.Sample>();for(int i=0;i<count;i++)samples.add(new SorcererBeamNode.Sample(readVec(b),b.readFloat(),b.readFloat()));return new EmissaryRay(java.util.List.copyOf(samples),end,wide,signal);})
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.flubberschnub.nyctocosm.client.ClientEvents.emissaryRay(m))).add();
        CHANNEL.messageBuilder(SourceVisual.class,6,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeUtf(m.clip(),32);b.writeVarInt(m.frame());writeVec(b,m.at());b.writeFloat(m.yaw());b.writeFloat(m.pitch());b.writeLong(m.seed());})
            .decoder(b->new SourceVisual(b.readUtf(32),b.readVarInt(),readVec(b),b.readFloat(),b.readFloat(),b.readLong()))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.flubberschnub.nyctocosm.client.FocusEffectController.play(m))).add();
        CHANNEL.messageBuilder(SorcererCue.class,5,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeUtf(m.clip(),32);b.writeVarInt(m.frame());writeVec(b,m.entry());b.writeFloat(m.yaw());b.writeFloat(m.pitch());writeVec(b,m.after());})
            .decoder(b->new SorcererCue(b.readUtf(32),b.readVarInt(),readVec(b),b.readFloat(),b.readFloat(),readVec(b)))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.flubberschnub.nyctocosm.client.ClientEvents.sorcererCue(m)))
            .add();
        CHANNEL.messageBuilder(SorcererRay.class,4,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message,buffer)->{buffer.writeBoolean(message.halo());buffer.writeVarInt(message.samples().size());for(var sample:message.samples()){writeVec(buffer,sample.position());buffer.writeFloat(sample.yaw());buffer.writeFloat(sample.pitch());}})
            .decoder(buffer->{boolean halo=buffer.readBoolean();int count=buffer.readVarInt();if(count<0||count>60)throw new IllegalArgumentException("Invalid ray sample count");var samples=new java.util.ArrayList<SorcererBeamNode.Sample>(count);for(int i=0;i<count;i++)samples.add(new SorcererBeamNode.Sample(readVec(buffer),buffer.readFloat(),buffer.readFloat()));return new SorcererRay(java.util.List.copyOf(samples),halo);})
            .consumerMainThread((message,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.flubberschnub.nyctocosm.client.ClientEvents.sorcererRay(message.samples(),message.halo())))
            .add();
        CHANNEL.messageBuilder(EmissaryBeam.class,2,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message,buffer)->{writeVec(buffer,message.start());writeVec(buffer,message.end());buffer.writeBoolean(message.wide());})
            .decoder(buffer->new EmissaryBeam(readVec(buffer),readVec(buffer),buffer.readBoolean()))
            .consumerMainThread((message,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                ()->()->com.flubberschnub.nyctocosm.client.ClientEvents.emissaryBeam(message.start(),message.end(),message.wide())))
            .add();
        CHANNEL.messageBuilder(WeaponCooldowns.class,3,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message,buffer)->{
                buffer.writeUUID(message.playerId());buffer.writeVarInt(message.nullfissureSwing());buffer.writeVarInt(message.nullfissureCast());
                buffer.writeVarInt(message.semblanceSwing());buffer.writeVarInt(message.semblanceCast());
            })
            .decoder(buffer->new WeaponCooldowns(buffer.readUUID(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt()))
            .consumerMainThread((message,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                ()->()->com.flubberschnub.nyctocosm.client.ClientEvents.weaponCooldowns(message.playerId(),message.nullfissureSwing(),message.nullfissureCast(),message.semblanceSwing(),message.semblanceCast())))
            .add();
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
    public static void sourceVisual(net.minecraft.server.level.ServerLevel level,String clip,Vec3 at,float yaw,float pitch){
        sourceVisual(level,clip,0,at,yaw,pitch);
    }
    public static void sourceVisual(net.minecraft.server.level.ServerLevel level,String clip,int frame,Vec3 at,float yaw,float pitch){
        var cue=new SourceVisual(clip,frame,at,yaw,pitch,level.random.nextLong());
        for(var player:level.players())if(player.position().distanceToSqr(at)<=512*512)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),cue);
    }
    public static void weaponEffect(LivingEntity owner,int kind,Vec3 eye,Vec3 end,Vec3 forward){
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->owner),new WeaponEffect(kind,eye,end,forward));
    }
    public static void emissaryBeam(net.minecraft.world.entity.Entity owner,Vec3 start,Vec3 end,boolean wide){
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->owner),new EmissaryBeam(start,end,wide));
    }
    public static void emissaryRay(net.minecraft.world.entity.Entity owner,java.util.List<SorcererBeamNode.Sample> samples,Vec3 end,boolean wide,boolean signal){
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->owner),new EmissaryRay(java.util.List.copyOf(samples),end,wide,signal));
    }
    public static void sorcererRay(net.minecraft.world.entity.Entity owner,java.util.List<SorcererBeamNode.Sample> samples,boolean halo){CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->owner),new SorcererRay(samples,halo));}
    public static void sorcererCue(NativeBoss owner,String clip,int frame,Vec3 entry,float yaw,float pitch,Vec3 after){CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->owner),new SorcererCue(clip,frame,entry,yaw,pitch,after));}
    public static void weaponCooldowns(ServerPlayer player,int nullfissureSwing,int nullfissureCast,int semblanceSwing,int semblanceCast){
        if(player.connection==null||!player.connection.connection.isConnected())return;
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new WeaponCooldowns(player.getUUID(),nullfissureSwing,nullfissureCast,semblanceSwing,semblanceCast));
    }
    private static void writeVec(net.minecraft.network.FriendlyByteBuf buffer,Vec3 value){buffer.writeDouble(value.x);buffer.writeDouble(value.y);buffer.writeDouble(value.z);}
    private static Vec3 readVec(net.minecraft.network.FriendlyByteBuf buffer){return new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble());}
    private NativeNetwork(){}
}
