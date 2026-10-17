package com.flubberschnub.nyctocosm;

import java.util.Comparator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** The short-lived soul marker produced by a Seelic Scythe kill. */
public final class SoulEntity extends Entity {
    private int age;

    public SoulEntity(EntityType<? extends SoulEntity> type, Level level) {
        super(type, level);
        noPhysics=true;
    }

    @Override public void tick() {
        super.tick();
        if(level().isClientSide)return;
        ServerLevel server=(ServerLevel)level();
        for(var player:server.players())server.sendParticles(player,ParticleTypes.SCULK_SOUL,true,getX(),getY(),getZ(),2,.2,.4,.2,.02);
        if(++age>=100){discard();return;}
        server.getEntitiesOfClass(Player.class,getBoundingBox().inflate(5),this::canCapture).stream()
            .min(Comparator.comparingDouble(this::distanceToSqr)).ifPresent(this::capture);
    }

    private boolean canCapture(Player player) {
        return player.isAlive()&&!player.isSpectator()&&distanceToSqr(player)<=25&&(player.getMainHandItem().is(Items.GLASS_BOTTLE)
            ||player.getMainHandItem().is(Content.get("soul_bottle"))
            ||player.getOffhandItem().is(Content.get("planar_coil")));
    }

    private void capture(Player player) {
        ItemStack main=player.getMainHandItem();
        if(main.is(Items.GLASS_BOTTLE)){
            if(!player.getAbilities().instabuild)main.shrink(1);
            ItemStack bottle=new ItemStack(Content.get("soul_bottle"));
            bottle.getOrCreateTag().putInt("Souls",1);
            if(!player.getInventory().add(bottle))player.drop(bottle,false);
            NativeEvents.masterSound((ServerLevel)level(),player.position(),SoundEvents.BOTTLE_FILL_DRAGONBREATH,1,10);
        }else if(main.is(Content.get("soul_bottle"))){
            FocusItem.addSoul(main,1);
            NativeEvents.masterSound((ServerLevel)level(),player.position(),SoundEvents.BOTTLE_FILL_DRAGONBREATH,1,10);
        }else{
            FocusItem.addSoul(player.getOffhandItem(),1);
            if(level() instanceof ServerLevel server)NativeEvents.resonanceSound(server,player.position(),"spells.ring.absorb",1,10);
        }
        discard();
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) { age=tag.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putInt("Age",age); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
