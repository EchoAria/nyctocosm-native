package com.flubberschnub.nyctocosm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Native replacement for the crypt advancement/marker driven Tony event. */
public final class TonyManager {
    private static final String COOLDOWN="NyctocosmTonyCooldown";
    private static final String TONY="NyctocosmTony";
    private static final String LIFE="NyctocosmTonyLife";
    private static final ResourceKey<Structure> DREAM_CRYPT=ResourceKey.create(Registries.STRUCTURE,new ResourceLocation("nyctocosm","crypts/dream"));

    public static void tickPlayer(ServerPlayer player) {
        if(!player.level().dimension().location().equals(new ResourceLocation("nyctocosm","nyctocosm")))return;
        var data=player.getPersistentData();int cooldown=data.getInt(COOLDOWN);
        if(cooldown>0){data.putInt(COOLDOWN,cooldown-1);return;}
        if(player.tickCount%20!=0||player.level().getMaxLocalRawBrightness(player.blockPosition())!=0)return;
        ServerLevel level=player.serverLevel();
        if(!level.structureManager().getStructureWithPieceAt(player.blockPosition(),DREAM_CRYPT).isValid())return;
        trySpawn(level,player);
        data.putInt(COOLDOWN,12000+level.random.nextInt(12000));
    }

    private static void trySpawn(ServerLevel level,ServerPlayer player) {
        for(int attempt=0;attempt<4;attempt++){
            BlockPos.MutableBlockPos at=player.blockPosition().offset(level.random.nextInt(7)-3,level.random.nextInt(7)-3,level.random.nextInt(7)-3).mutable();
            for(int scan=0;scan<12;scan++){
                if(level.getBlockState(at).isAir()&&!level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty())break;
                at.move(0,scan%2==0?scan+1:-(scan+1),0);
            }
            if(player.distanceToSqr(at.getX()+.5,at.getY(),at.getZ()+.5)>100||!level.getBlockState(at).isAir()
                ||level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty())continue;
            Cat cat=EntityType.CAT.create(level);if(cat==null)return;
            cat.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,level.random.nextFloat()*360,0);
            cat.setVariant(BuiltInRegistries.CAT_VARIANT.getHolderOrThrow(CatVariant.BLACK).value());
            cat.setInvulnerable(true);cat.setOrderedToSit(true);cat.setPersistenceRequired();cat.setCustomNameVisible(true);
            cat.setCustomName(Component.literal("Tony").withStyle(ChatFormatting.LIGHT_PURPLE));
            cat.getPersistentData().putBoolean(TONY,true);cat.getPersistentData().putInt(LIFE,200+level.random.nextInt(200));
            cat.getPersistentData().putDouble("NyctocosmTonyX",cat.getX());cat.getPersistentData().putDouble("NyctocosmTonyY",cat.getY());cat.getPersistentData().putDouble("NyctocosmTonyZ",cat.getZ());
            if(level.addFreshEntity(cat)){
                level.sendParticles(new DustParticleOptions(new Vector3f(0,0,0),2),cat.getX(),cat.getY()+.5,cat.getZ(),500,.6,.6,.6,0);
                level.playSound(null,cat.blockPosition(),SoundEvents.CAT_AMBIENT,SoundSource.MASTER,1,1);
                NativeEvents.sound(level,cat.position(),"tony.spawn",1);
            }
            return;
        }
    }

    public static void tickCat(Cat cat) {
        if(!(cat.level() instanceof ServerLevel level)||!cat.getPersistentData().getBoolean(TONY))return;
        var data=cat.getPersistentData();cat.setPos(data.getDouble("NyctocosmTonyX"),data.getDouble("NyctocosmTonyY"),data.getDouble("NyctocosmTonyZ"));
        cat.setDeltaMovement(Vec3.ZERO);cat.getNavigation().stop();cat.setOrderedToSit(true);
        var nearest=level.getNearestPlayer(cat,20);if(nearest!=null)cat.getLookControl().setLookAt(nearest,90,90);
        int remaining=cat.getPersistentData().getInt(LIFE)-1;cat.getPersistentData().putInt(LIFE,remaining);
        if(remaining>0)return;
        NativeEvents.sound(level,cat.position(),"tony.disappear",1);
        level.sendParticles(new DustParticleOptions(new Vector3f(0,0,0),2),cat.getX(),cat.getY()+.5,cat.getZ(),500,.6,.6,.6,0);
        ItemStack dreams=new ItemStack(Content.get("dreams"));
        level.addFreshEntity(new ItemEntity(level,cat.getX(),cat.getY(),cat.getZ(),dreams));cat.discard();
    }

    private TonyManager() {}
}
