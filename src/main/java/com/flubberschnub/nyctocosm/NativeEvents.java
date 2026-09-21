package com.flubberschnub.nyctocosm;

import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class NativeEvents {
    @SubscribeEvent public void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event){
        var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if(id!=null&&id.getNamespace().equals(NativeMod.ID))event.getToolTip().add(Component.translatable("tooltip.nyctocosm.item."+id.getPath()).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @SubscribeEvent public void cloned(net.minecraftforge.event.entity.player.PlayerEvent.Clone event){
        copyPersistentProgress(event.getOriginal(),event.getEntity());
    }
    public static void copyPersistentProgress(Player original,Player replacement){
        var source=original.getPersistentData();var target=replacement.getPersistentData();
        for(String key:source.getAllKeys())if(key.equals("NyctocosmLore")||key.startsWith("NyctocosmReturn"))target.put(key,source.get(key).copy());
    }
    public static boolean hasArtifact(Player player,String name){return player.getInventory().contains(new ItemStack(Content.get("artifact_"+name)));}
    public static List<SeelicField> traces(Player player){return player.level().getEntitiesOfClass(SeelicField.class,player.getBoundingBox().inflate(6),f->f.isHypertrace()&&f.ownedBy(player));}
    public static void addSoul(Player player,int amount){for(ItemStack stack:player.getInventory().items)if(stack.getItem() instanceof FocusItem){FocusItem.addSoul(stack,amount);return;}}
    public static void sound(ServerLevel level,Vec3 position,String id,float volume){
        sound(level,position,id,volume,1);
    }
    public static void sound(ServerLevel level,Vec3 position,String id,float volume,float pitch){
        var sound=Content.SOUND_MAP.get(id);
        if(sound!=null)level.playSound(null,position.x,position.y,position.z,sound.get(),SoundSource.HOSTILE,volume,pitch);
    }
    @SubscribeEvent public void killed(LivingDeathEvent event){
        if(event.getSource().getEntity() instanceof Player player){
            if(player.getMainHandItem().getItem() instanceof WeaponItem weapon && weapon.kind==0)addSoul(player,1);
            if(hasArtifact(player,"expulsion")&&event.getSource().getMsgId().equals("nyctocosm.dissonance")&&player.level() instanceof ServerLevel server)
                Combat.area(server,player,event.getEntity().position(),4,8,Combat.Element.DISSONANCE,.5);
        }
    }
    @SubscribeEvent public void hurt(LivingHurtEvent event){
        String id=event.getSource().getMsgId();
        if(!id.startsWith("nyctocosm."))return;
        String element=id.substring("nyctocosm.".length());
        LivingEntity target=event.getEntity();
        if(element.equals("void")&&target.level() instanceof ServerLevel server){
            boolean weakened=server.getEntitiesOfClass(SeelicField.class,target.getBoundingBox().inflate(6),field->field.isHypertrace()
                &&field.distanceToSqr(target)<=36&&field.owner()!=null&&hasArtifact(field.owner(),"absorption")&&Combat.canHit(field.owner(),target)).size()>0;
            if(weakened)event.setAmount(event.getAmount()*1.2f);
        }
        if(!(target instanceof Player player))return;
        int charges=traces(player).stream().mapToInt(SeelicField::charges).sum();
        if(hasArtifact(player,"protection_"+element))event.setAmount(event.getAmount()*(1-Math.min(.75f,charges*.03f)));
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void damaged(LivingDamageEvent event){
        if(event.getAmount()>0&&event.getSource().getMsgId().equals("nyctocosm.void")&&event.getSource().getEntity() instanceof Player player
            &&player!=event.getEntity()&&!player.level().isClientSide&&hasArtifact(player,"absorption"))player.heal(event.getAmount()*.2f);
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide)return;
        Player player=event.player;
        if(player.isShiftKeyDown()&&!player.onGround()&&player.getDeltaMovement().y<0&&hasArtifact(player,"levitation")&&!player.hasEffect(MobEffects.LEVITATION)){
            var nearby=traces(player);int charges=nearby.stream().mapToInt(SeelicField::charges).sum();
            if(charges>=3){int remaining=3;for(SeelicField field:nearby){int n=Math.min(remaining,field.charges());field.consume(n);remaining-=n;if(remaining==0)break;}player.addEffect(new MobEffectInstance(MobEffects.LEVITATION,40,1));}
        }
        if(player.tickCount%100==0&&player.level().dimension().location().equals(new ResourceLocation("nyctocosm","nyctocosm"))&&!player.isCreative()
            &&player.getPersistentData().getLong("NyctocosmLuminous")<player.level().getGameTime())player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,120,0,false,false));
    }
    public static boolean travel(Player player,String destination){
        if(!(player instanceof ServerPlayer serverPlayer))return false;
        ServerLevel target=serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation("nyctocosm",destination)));
        if(target==null){player.displayClientMessage(Component.translatable("message.nyctocosm.dimension_unavailable"),true);return false;}
        if(target==player.level())return true;
        var landing=SafeTravel.find(target,player,player.position());
        if(landing.isEmpty()){player.displayClientMessage(Component.translatable("message.nyctocosm.no_safe_landing"),true);return false;}
        var saved=player.getPersistentData();
        var history=saved.getList("NyctocosmReturnHistory",10);var entry=new net.minecraft.nbt.CompoundTag();
        entry.putString("Dimension",player.level().dimension().location().toString());entry.putDouble("X",player.getX());entry.putDouble("Y",player.getY());entry.putDouble("Z",player.getZ());
        if(history.size()>=32)history.remove(0);history.add(entry);saved.put("NyctocosmReturnHistory",history);
        Vec3 at=landing.get();serverPlayer.teleportTo(target,at.x,at.y,at.z,player.getYRot(),player.getXRot());player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;return true;
    }
    public static void recall(Player player){
        if(!(player instanceof ServerPlayer serverPlayer)||player.level().dimension().location().getPath().equals("emissary_domain"))return;
        // Original Evanescence recalls to the overworld spawn, it is not dimension hopping.
        ServerLevel overworld=serverPlayer.server.overworld();BlockPos spawn=serverPlayer.getRespawnPosition();
        if(spawn==null||!serverPlayer.getRespawnDimension().equals(Level.OVERWORLD))spawn=overworld.getSharedSpawnPos();
        var landing=SafeTravel.find(overworld,player,Vec3.atBottomCenterOf(spawn));
        if(landing.isEmpty()){player.displayClientMessage(Component.translatable("message.nyctocosm.no_safe_spawn"),true);return;}
        Vec3 at=landing.get();serverPlayer.teleportTo(overworld,at.x,at.y,at.z,player.getYRot(),player.getXRot());player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        player.getPersistentData().remove("NyctocosmReturnHistory");player.getPersistentData().remove("NyctocosmReturnDimension");
    }
    public static boolean returnFromDimension(ServerPlayer player){
        var tag=player.getPersistentData();var history=tag.getList("NyctocosmReturnHistory",10);
        boolean legacy=history.isEmpty();if(legacy&&!tag.contains("NyctocosmReturnDimension"))return false;
        var entry=legacy?tag:history.getCompound(history.size()-1);String prefix=legacy?"NyctocosmReturn":"";
        ResourceLocation id=ResourceLocation.tryParse(entry.getString(prefix+"Dimension"));if(id==null)return false;
        ServerLevel level=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));if(level==null)return false;
        var landing=SafeTravel.find(level,player,new Vec3(entry.getDouble(prefix+"X"),entry.getDouble(prefix+"Y"),entry.getDouble(prefix+"Z")));
        if(landing.isEmpty()){player.displayClientMessage(Component.translatable("message.nyctocosm.no_safe_return"),true);return false;}
        Vec3 at=landing.get();player.teleportTo(level,at.x,at.y,at.z,player.getYRot(),player.getXRot());player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        if(legacy)tag.remove("NyctocosmReturnDimension");else{history.remove(history.size()-1);tag.put("NyctocosmReturnHistory",history);}return true;
    }
    public static int spawn(ServerLevel level,Vec3 pos,String name){
        var type=switch(name){case "serpent"->Content.SERPENT;case "sorcerer"->Content.SORCERER;default->Content.EMISSARY;};
        NativeBoss boss=type.get().create(level);if(boss==null)return 0;
        boss.moveTo(pos.x,pos.y+(name.equals("serpent")?10:0),pos.z,0,0);boss.beginEntrance();
        return level.addFreshEntity(boss)?1:0;
    }
    public static void incur(Player player){
        if(!(player.level() instanceof ServerLevel level))return;
        String biome=level.getBiome(player.blockPosition()).unwrapKey().map(k->k.location().getPath()).orElse("");
        String boss=switch(biome){case "night_wastes"->"serpent";case "desert_of_rime"->"sorcerer";case "infernal_chasms"->"emissary";default->"";};
        if(boss.isEmpty()){player.displayClientMessage(Component.translatable("message.nyctocosm.incur_unavailable"),true);return;}
        if(boss.equals("emissary")&&!travel(player,"emissary_domain"))return;
        spawn((ServerLevel)player.level(),player.position().add(player.getLookAngle().multiply(12,0,12)),boss);
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event){
        var root=Commands.literal("nyctocosm").requires(source->source.hasPermission(2));
        var summon=Commands.literal("summon");
        for(String boss:new String[]{"serpent","sorcerer","emissary"})summon.then(Commands.literal(boss).executes(ctx->spawn(ctx.getSource().getLevel(),ctx.getSource().getPosition().add(0,0,8),boss)));
        root.then(summon);
        for(String dim:new String[]{"nyctocosm","dusk_plane","emissary_domain"})root.then(Commands.literal("travel").then(Commands.literal(dim).executes(ctx->travel(ctx.getSource().getPlayerOrException(),dim)?1:0)));
        root.then(Commands.literal("return").executes(ctx->{
            return returnFromDimension(ctx.getSource().getPlayerOrException())?1:0;
        }));
        event.getDispatcher().register(root);
    }
}
