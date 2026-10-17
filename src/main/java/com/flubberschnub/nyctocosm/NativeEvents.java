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
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class NativeEvents {
    private static final java.util.UUID RESISTANCE_KNOCKBACK=java.util.UUID.fromString("6dba0d62-367a-411b-9453-ecf8c69416a5");
    private static final ResourceLocation NYCTOCOSM_DIMENSION=new ResourceLocation("nyctocosm","nyctocosm");
    private static final ResourceLocation EMISSARY_DOMAIN=new ResourceLocation("nyctocosm","emissary_domain");
    private static final String ENCOUNTER_MODE_CHANGED="NyctocosmEncounterModeChanged";
    private static final String ENCOUNTER_PREVIOUS_MODE="NyctocosmEncounterPreviousMode";
    private static final String ENCOUNTER_MANAGED_MODE="NyctocosmEncounterManagedMode";
    @SubscribeEvent public void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event){
        var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if(id!=null&&id.getNamespace().equals(NativeMod.ID))event.getToolTip().add(Component.translatable("tooltip.nyctocosm.item."+id.getPath()).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @SubscribeEvent public void cloned(net.minecraftforge.event.entity.player.PlayerEvent.Clone event){
        copyPersistentProgress(event.getOriginal(),event.getEntity());
        if(event.isWasDeath()&&event.getOriginal().level().dimension().location().equals(EMISSARY_DOMAIN)
                &&event.getEntity() instanceof ServerPlayer replacement)restoreEmissaryMode(replacement);
    }
    @SubscribeEvent public void respawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer player)restoreEmissaryMode(player);
    }
    @SubscribeEvent public void changedDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){
        if(EMISSARY_DOMAIN.equals(event.getFrom().location())&&!EMISSARY_DOMAIN.equals(event.getTo().location())
                &&event.getEntity() instanceof ServerPlayer player)restoreEmissaryMode(player);
    }
    @SubscribeEvent public void loggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event){
        Player player=event.getEntity();boolean legacy=LegacyMigration.hasLegacyScores(player);
        int items=LegacyMigration.migrate(player);
        if(player instanceof ServerPlayer serverPlayer)WeaponAbilityCooldowns.sync(serverPlayer);
        if((legacy||items>0)&&!player.getPersistentData().getBoolean("NyctocosmLegacyWarningShown")){
            player.sendSystemMessage(Component.translatable("message.nyctocosm.legacy_world_warning").withStyle(net.minecraft.ChatFormatting.RED));
            player.getPersistentData().putBoolean("NyctocosmLegacyWarningShown",true);
        }
    }
    public static void copyPersistentProgress(Player original,Player replacement){
        var source=original.getPersistentData();var target=replacement.getPersistentData();
        for(String key:source.getAllKeys())if(key.startsWith("Nyctocosm"))target.put(key,source.get(key).copy());
    }
    public static boolean hasArtifact(Player player,String name){return player.getInventory().contains(new ItemStack(Content.get("artifact_"+name)));}
    public static List<SeelicField> traces(Player player){return player.level().getEntitiesOfClass(SeelicField.class,player.getBoundingBox().inflate(6),f->f.isHypertrace()&&f.ownedBy(player)&&f.distanceToSqr(player)<=36);}
    public static void addSoul(Player player,int amount){for(ItemStack stack:player.getInventory().items)if(stack.getItem() instanceof FocusItem){FocusItem.addSoul(stack,amount);return;}}
    public static int hypertraceCharges(Player player){return Math.max(0,Math.min(15,player.getPersistentData().getInt("NyctocosmHypertraceCharge")));}
    public static int hypertraceMaxCharges(Player player){return Math.max(0,Math.min(15,player.getPersistentData().contains("NyctocosmHypertraceMax")?player.getPersistentData().getInt("NyctocosmHypertraceMax"):5));}
    public static void addHypertraceCharge(Player player,int amount){player.getPersistentData().putInt("NyctocosmHypertraceCharge",Math.max(0,Math.min(hypertraceMaxCharges(player),hypertraceCharges(player)+amount)));}
    public static void sound(ServerLevel level,Vec3 position,String id,float volume){
        sound(level,position,id,volume,1);
    }
    public static void sound(ServerLevel level,Vec3 position,String id,float volume,float pitch){
        var sound=Content.SOUND_MAP.get(id);
        if(sound!=null)level.playSound(null,position.x,position.y,position.z,sound.get(),SoundSource.HOSTILE,volume,pitch);
    }
    public static void resonanceSound(ServerLevel level,Vec3 at,String id,float volume,double radius){
        var sound=Content.SOUND_MAP.get(id);if(sound==null)return;
        var holder=net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.get());
        long seed=level.random.nextLong();
        for(var player:level.players())if(player.position().distanceToSqr(at)<=radius*radius)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(holder,SoundSource.MASTER,at.x,at.y,at.z,volume,1,seed));
    }
    public static void masterSound(ServerLevel level,Vec3 at,net.minecraft.sounds.SoundEvent sound,float volume,double radius){
        masterSound(level,at,sound,volume,radius,1);
    }
    public static void masterSound(ServerLevel level,Vec3 at,net.minecraft.sounds.SoundEvent sound,float volume,double radius,float pitch){
        var holder=net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        for(var player:level.players())if(player.position().distanceToSqr(at)<=radius*radius)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(holder,SoundSource.MASTER,
                at.x,at.y,at.z,volume,pitch,player.getRandom().nextLong()));
    }
    @SubscribeEvent public void killed(LivingDeathEvent event){
        if(event.getSource().getEntity() instanceof Player player){
            if(player.getMainHandItem().getItem() instanceof WeaponItem weapon && weapon.kind==0&&player.level() instanceof ServerLevel server){
                SoulEntity soul=Content.SOUL.get().create(server);
                if(soul!=null){Vec3 at=player.position().add(0,1,0).add(SerpentMotion.localForward(player.getYRot(),0).scale(2));soul.setPos(at);server.addFreshEntity(soul);}
                resonanceSound(server,player.position(),"spells.soul_reap",4,10);
            }
            if(hasArtifact(player,"expulsion")&&event.getSource().getMsgId().equals("nyctocosm.dissonance")&&player.level() instanceof ServerLevel server)
                ExpulsionBurst.schedule(server,player,event.getEntity().position());
        }
    }
    @SubscribeEvent public void joined(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof Illusioner illusioner)||!(event.getLevel() instanceof ServerLevel level))return;
        var biome=level.getBiome(illusioner.blockPosition()).unwrapKey().orElse(null);
        if(biome==null||!biome.location().getNamespace().equals(NativeMod.ID))return;
        String path=biome.location().getPath();NativeBoss replacement=null;
        if(path.equals("night_wastes")&&level.random.nextFloat()<.5f&&level.getEntities(Content.APPARITION.get(),e->true).size()<30)
            replacement=Content.APPARITION.get().create(level);
        else if(path.equals("cimmerian_gorge")&&level.random.nextFloat()<.2f&&level.getEntities(Content.DEATH_WALKER.get(),e->true).size()<3)
            replacement=Content.DEATH_WALKER.get().create(level);
        if(replacement!=null){replacement.moveTo(illusioner.getX(),illusioner.getY(),illusioner.getZ(),illusioner.getYRot(),illusioner.getXRot());level.addFreshEntity(replacement);}
        event.setCanceled(true);
    }
    @SubscribeEvent public void livingTick(LivingEvent.LivingTickEvent event){if(event.getEntity() instanceof Cat cat)TonyManager.tickCat(cat);}
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
        if(hasArtifact(player,"protection_"+element))event.setAmount(event.getAmount()*(1-hypertraceCharges(player)*.03f));
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void damaged(LivingDamageEvent event){
        if(event.getAmount()>0&&event.getSource().getMsgId().equals("nyctocosm.void")&&event.getSource().getEntity() instanceof Player player
            &&player!=event.getEntity()&&!player.level().isClientSide&&hasArtifact(player,"absorption"))player.heal(event.getAmount()*.2f);
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide)return;
        Player player=event.player;
        tickArtifacts(player);
        tickSpellStatuses(player);
        if(player instanceof ServerPlayer serverPlayer){
            WeaponAbilityCooldowns.tick(serverPlayer);
            if(shouldTickFear(serverPlayer))tickFear(serverPlayer);
            TonyManager.tickPlayer(serverPlayer);renderHypertraceCharge(serverPlayer);
        }
    }
    static void tickArtifacts(Player player){
        if(hypertraceCharges(player)>hypertraceMaxCharges(player))addHypertraceCharge(player,0);
        var persistent=player.getPersistentData();if(!player.isShiftKeyDown())persistent.remove("NyctocosmLevitationHeld");
        if(player.isShiftKeyDown()&&!persistent.getBoolean("NyctocosmLevitationHeld")&&hasArtifact(player,"levitation")
            &&player.level().getBlockState(player.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.AIR)
            &&player.level().getBlockState(player.blockPosition().below(2)).is(net.minecraft.world.level.block.Blocks.AIR)){
            if(hypertraceCharges(player)>=3){addHypertraceCharge(player,-3);player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100,0,true,false));persistent.putBoolean("NyctocosmLevitationHeld",true);}
        }
        boolean protection=false;
        for(Combat.Element element:Combat.Element.values())if(hasArtifact(player,"protection_"+element.name().toLowerCase(java.util.Locale.ROOT))){protection=true;break;}
        // All six protections share one timer. It pauses when none is held, as in the source scoreboard.
        if(protection){
            int elapsed=persistent.getInt("NyctocosmProtectionChargeTimer")+1;
            if(elapsed>=200){addHypertraceCharge(player,-1);elapsed=0;}
            persistent.putInt("NyctocosmProtectionChargeTimer",elapsed);
        }
    }
    private static void renderHypertraceCharge(ServerPlayer player){
        int charge=hypertraceCharges(player);if(charge==0)return;
        Vec3 horizontal=player.getLookAngle().multiply(1,0,1).normalize();
        Vec3 right=new Vec3(horizontal.z,0,-horizontal.x);
        Vec3 base=player.position().add(right.scale(1.5)).add(horizontal.scale(1.5)).add(0,1.625,0);
        var orange=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1,.667f,0),.1f);
        for(int n=0;n<charge;n++){
            Vec3 at=base.add(0,n/15.0,0);
            ((ServerLevel)player.level()).sendParticles(player,orange,false,at.x,at.y,at.z,3,0,0,0,0);
        }
    }
    static boolean isFearDimension(ResourceLocation id){
        return id.getNamespace().equals(NativeMod.ID)&&switch(id.getPath()){
            case "nyctocosm","dusk_plane","emissary_domain"->true;
            default->false;
        };
    }
    private static boolean hasActiveLuminescence(Player player){
        var data=player.getPersistentData();
        return data.contains("NyctocosmLuminous")&&data.getLong("NyctocosmLuminous")>=player.level().getGameTime();
    }
    static void tickSpellStatuses(Player player){
        var data=player.getPersistentData();long now=player.level().getGameTime();
        if(data.contains("NyctocosmLuminous")){
            long remaining=data.getLong("NyctocosmLuminous")-now;
            if(remaining>0){
                player.removeEffect(MobEffects.DARKNESS);player.removeEffect(MobEffects.BLINDNESS);
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING,(int)Math.min(Integer.MAX_VALUE,remaining),0,false,false));
            }else{data.remove("NyctocosmLuminous");player.removeEffect(MobEffects.GLOWING);}
        }
        var knockback=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        if(data.contains("NyctocosmResistanceUntil")){
            long remaining=data.getLong("NyctocosmResistanceUntil")-now;
            if(remaining>0){
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,(int)Math.min(Integer.MAX_VALUE,remaining),0,false,false));
                if(knockback!=null&&knockback.getModifier(RESISTANCE_KNOCKBACK)==null)knockback.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RESISTANCE_KNOCKBACK,"Nyctocosm spell resistance",1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                if(player.level() instanceof ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,player.getX(),player.getY()+1,player.getZ(),1,.2,.3,.2,0);
            }else{data.remove("NyctocosmResistanceUntil");if(knockback!=null)knockback.removeModifier(RESISTANCE_KNOCKBACK);}
        }else if(knockback!=null)knockback.removeModifier(RESISTANCE_KNOCKBACK);
    }
    static boolean shouldTickFear(ServerPlayer player){
        return isFearDimension(player.level().dimension().location())
            &&player.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&!hasActiveLuminescence(player);
    }
    private static void tickFear(ServerPlayer player){
        if(!shouldTickFear(player))return;
        var data=player.getPersistentData();
        boolean light=player.level().getMaxLocalRawBrightness(player.blockPosition())>=5;
        if(light){data.remove("NyctocosmFearDark");data.remove("NyctocosmFearActive");data.remove("NyctocosmFearBeat");data.remove("NyctocosmFearEvent");player.removeEffect(MobEffects.DARKNESS);return;}
        if(!data.getBoolean("NyctocosmFearActive")){
            int dark=data.getInt("NyctocosmFearDark")+1;data.putInt("NyctocosmFearDark",dark);
            if(dark<200)return;data.putBoolean("NyctocosmFearActive",true);data.putInt("NyctocosmFearCalm",30);
        }
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,100,0,true,false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,1,true,false));
        int beat=data.getInt("NyctocosmFearBeat")+1,calm=Math.max(1,data.getInt("NyctocosmFearCalm"));
        if(beat>=calm){personalSound(player,player.position(),Content.SOUND_MAP.get("heartbeat").get(),1);beat=0;}data.putInt("NyctocosmFearBeat",beat);
        int timer=data.getInt("NyctocosmFearEvent")-1;
        if(timer<=0){
            if(calm>=10)data.putInt("NyctocosmFearCalm",calm-3);data.putInt("NyctocosmFearEvent",20+player.getRandom().nextInt(180));
            Vec3 at=player.position().add((player.getRandom().nextDouble()-.5)*20,(player.getRandom().nextDouble()-.5)*20,(player.getRandom().nextDouble()-.5)*20);
            int choice=player.getRandom().nextInt(3);
            if(choice==1)at=player.position().add((player.getRandom().nextDouble()-.5)*4,(player.getRandom().nextDouble()-.5)*4,(player.getRandom().nextDouble()-.5)*4);
            var sound=choice==0?Content.SOUND_MAP.get("nightmare_apparition.attack").get():choice==1?SoundEvents.DEEPSLATE_STEP:Content.SOUND_MAP.get("nightmare_apparition.dissipate").get();
            personalSound(player,at,sound,5);
        }else data.putInt("NyctocosmFearEvent",timer);
    }
    private static void personalSound(ServerPlayer player,Vec3 at,SoundEvent event,float volume){
        var holder=net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(holder,SoundSource.MASTER,at.x,at.y,at.z,volume,1,player.getRandom().nextLong()));
    }
    public static boolean travel(Player player,String destination){
        if(!(player instanceof ServerPlayer serverPlayer))return false;
        ServerLevel target=serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation("nyctocosm",destination)));
        if(target==null){player.displayClientMessage(Component.translatable("message.nyctocosm.dimension_unavailable"),true);return false;}
        if(target==player.level())return true;
        var landing=findArrival(target,player,player.position());
        if(landing.isEmpty()){player.displayClientMessage(Component.translatable("message.nyctocosm.no_safe_landing"),true);return false;}
        completeTravel(serverPlayer,target,landing.get());return true;
    }
    private static void completeTravel(ServerPlayer player,ServerLevel target,Vec3 at){
        var saved=player.getPersistentData();
        var history=saved.getList("NyctocosmReturnHistory",10);var entry=new net.minecraft.nbt.CompoundTag();
        entry.putString("Dimension",player.level().dimension().location().toString());entry.putDouble("X",player.getX());entry.putDouble("Y",player.getY());entry.putDouble("Z",player.getZ());
        if(history.size()>=32)history.remove(0);history.add(entry);saved.put("NyctocosmReturnHistory",history);
        player.teleportTo(target,at.x,at.y,at.z,player.getYRot(),player.getXRot());player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        if(target.dimension().location().equals(NYCTOCOSM_DIMENSION)){
            var advancement=player.server.getAdvancements().getAdvancement(new ResourceLocation(NativeMod.ID,"root"));
            if(advancement!=null)player.getAdvancements().award(advancement,"entered");
        }
    }
    static java.util.Optional<Vec3> findArrival(ServerLevel level,Player player,Vec3 requested){
        // Match the source's "execute in": X/Z scale between dimensions, Y comes from the destination surface.
        double scale=net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(player.level().dimensionType(),level.dimensionType());
        return SafeTravel.findSurface(level,player,requested.multiply(scale,1,scale));
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
    static void enterEmissaryMode(ServerPlayer player){
        var data=player.getPersistentData();
        if(data.getBoolean(ENCOUNTER_MODE_CHANGED)||player.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL)return;
        data.putBoolean(ENCOUNTER_MODE_CHANGED,true);
        data.putInt(ENCOUNTER_PREVIOUS_MODE,player.gameMode.getGameModeForPlayer().getId());
        data.putInt(ENCOUNTER_MANAGED_MODE,GameType.ADVENTURE.getId());
        player.setGameMode(GameType.ADVENTURE);
    }
    static void restoreEmissaryMode(ServerPlayer player){
        var data=player.getPersistentData();if(!data.getBoolean(ENCOUNTER_MODE_CHANGED))return;
        int previous=data.getInt(ENCOUNTER_PREVIOUS_MODE),managed=data.getInt(ENCOUNTER_MANAGED_MODE);
        if(player.gameMode.getGameModeForPlayer().getId()==managed)player.setGameMode(GameType.byId(previous));
        data.remove(ENCOUNTER_MODE_CHANGED);data.remove(ENCOUNTER_PREVIOUS_MODE);data.remove(ENCOUNTER_MANAGED_MODE);
    }
    public static int spawn(ServerLevel level,Vec3 pos,String name){
        var type=switch(name){case "serpent"->Content.SERPENT;case "sorcerer"->Content.SORCERER;default->Content.EMISSARY;};
        if(!level.getEntities(type.get(),entity->name.equals("emissary")?!entity.isRemoved():entity.isAlive()).isEmpty())return 0;
        NativeBoss boss=type.get().create(level);if(boss==null)return 0;
        if(name.equals("emissary")){
            var landing=SafeTravel.findSurface(level,boss,pos);if(landing.isEmpty())return 0;pos=landing.get();
        }
        boss.moveTo(pos.x,pos.y+(name.equals("serpent")?10:0),pos.z,0,0);boss.beginEntrance();
        return level.addFreshEntity(boss)?1:0;
    }
    public static void incur(Player player){
        incur(player,player.position());
    }
    public static void incur(Player player,Vec3 castOrigin){
        if(!(player instanceof ServerPlayer serverPlayer)||!(player.level() instanceof ServerLevel level))return;
        String boss=incurrenceBossForBiome(level.getBiome(BlockPos.containing(castOrigin)).unwrapKey().map(k->k.location()).orElse(null));
        if(boss.isEmpty()){player.displayClientMessage(Component.translatable("message.nyctocosm.incur_unavailable"),true);return;}
        if(boss.equals("emissary")){
            ServerLevel domain=serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation("nyctocosm","emissary_domain")));
            if(domain==null||!domain.getEntities(Content.EMISSARY.get(),entity->!entity.isRemoved()).isEmpty())return;
            var landing=findArrival(domain,player,castOrigin);NativeBoss emissary=Content.EMISSARY.get().create(domain);
            var bossLanding=landing.isEmpty()||emissary==null?java.util.Optional.<Vec3>empty():findEmissaryLanding(domain,emissary,landing.get());
            if(landing.isEmpty()||bossLanding.isEmpty()){player.displayClientMessage(Component.translatable("message.nyctocosm.no_safe_landing"),true);return;}
            Vec3 at=bossLanding.get(),toward=landing.get().subtract(at);
            emissary.moveTo(at.x,at.y,at.z,(float)(Math.toDegrees(Math.atan2(toward.z,toward.x))-90),0);
            // Validate both bodies before changing the player's dimension or return stack.
            if(!domain.addFreshEntity(emissary))return;
            completeTravel(serverPlayer,domain,landing.get());enterEmissaryMode(serverPlayer);emissary.setTarget(player);emissary.beginEntrance();return;
        }
        spawn((ServerLevel)player.level(),player.position().add(player.getLookAngle().multiply(12,0,12)),boss);
    }
    static String incurrenceBossForBiome(ResourceLocation biome){
        if(biome==null||!NativeMod.ID.equals(biome.getNamespace()))return "";
        return switch(biome.getPath()){case "night_wastes"->"serpent";case "desert_of_rime"->"sorcerer";case "infernal_chasms"->"emissary";default->"";};
    }
    static java.util.Optional<Vec3> findEmissaryLanding(ServerLevel level,NativeBoss boss,Vec3 playerLanding){
        double start=level.random.nextDouble()*Math.PI*2;
        for(int attempt=0;attempt<16;attempt++){
            double angle=start+attempt*Math.PI/8,radius=20+level.random.nextDouble()*10;
            var landing=SafeTravel.findSurface(level,boss,playerLanding.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius),4);
            if(landing.isPresent())return landing;
        }
        return java.util.Optional.empty();
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event){
        var root=Commands.literal("nyctocosm");
        var summon=Commands.literal("summon").requires(source->source.hasPermission(2));
        for(String boss:new String[]{"serpent","sorcerer","emissary"})summon.then(Commands.literal(boss).executes(ctx->spawn(ctx.getSource().getLevel(),ctx.getSource().getPosition().add(0,0,8),boss)));
        root.then(summon);
        var travel=Commands.literal("travel").requires(source->source.hasPermission(2));
        for(String dim:new String[]{"nyctocosm","dusk_plane","emissary_domain"})travel.then(Commands.literal(dim).executes(ctx->travel(ctx.getSource().getPlayerOrException(),dim)?1:0));
        root.then(travel);
        root.then(Commands.literal("return").executes(ctx->{
            return returnFromDimension(ctx.getSource().getPlayerOrException())?1:0;
        }));
        event.getDispatcher().register(root);
    }
}
