package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** One real, owned entity replaces the original field rig, marker and scoreboards. */
public final class SeelicField extends Entity {
    private static final EntityDataAccessor<Boolean> MOBILE=SynchedEntityData.defineId(SeelicField.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TRACE=SynchedEntityData.defineId(SeelicField.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SPELL=SynchedEntityData.defineId(SeelicField.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE=SynchedEntityData.defineId(SeelicField.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAST=SynchedEntityData.defineId(SeelicField.class,EntityDataSerializers.INT);
    private UUID owner;
    private int life=2400, cooldown, element;
    public SeelicField(EntityType<? extends SeelicField> type,Level level) { super(type,level); }
    @Override protected void defineSynchedData() { entityData.define(MOBILE,false);entityData.define(TRACE,false);entityData.define(SPELL,-1);entityData.define(CHARGE,5);entityData.define(CAST,0); }
    public void configure(Player player,boolean mobile,boolean trace) { owner=player.getUUID();entityData.set(MOBILE,mobile);entityData.set(TRACE,trace);life=trace?1200:2400; }
    public boolean ownedBy(Player player) { return player.getUUID().equals(owner); }
    public boolean isHypertrace() { return entityData.get(TRACE); }
    public boolean mobile() { return entityData.get(MOBILE); }
    public int charges() { return entityData.get(CHARGE); }
    public void consume(int n) { entityData.set(CHARGE,Math.max(0,charges()-n));if(charges()==0) discard(); }
    public int castTicks() { return entityData.get(CAST); }
    public Spell spell() { int n=entityData.get(SPELL);return n<0?null:Spell.values()[Math.floorMod(n,Spell.values().length)]; }
    public Player owner() { return owner==null?null:level().getPlayerByUUID(owner); }
    @Override public boolean isPickable() { return isHypertrace(); }
    @Override public boolean hurt(DamageSource source,float damage) {
        if(level().isClientSide || !isHypertrace() || damage<=0) return false;
        Player player=owner();
        if(player!=null && source.getEntity()==player) {
            if(NativeEvents.hasArtifact(player,"amelioration")&&distanceToSqr(player)<=36){player.heal(6);player.addEffect(new MobEffectInstance(MobEffects.SATURATION,60,0,false,false));}
            if(NativeEvents.hasArtifact(player,"extraction")) NativeEvents.addSoul(player,1);
            if(NativeEvents.hasArtifact(player,"expulsion")) Combat.area((ServerLevel)level(),player,position(),5,charges()*3,Combat.Element.DISSONANCE,1);
            discard();return true;
        }
        return false;
    }
    @Override public void tick() {
        super.tick();
        if(!(level() instanceof ServerLevel server)) return;
        Player player=owner();
        if(--life<=0 || player==null || !player.isAlive()) { discard();return; }
        if(mobile() || NativeEvents.hasArtifact(player,"transposition")) setPos(player.position().add(0,.08,0));
        if(isHypertrace()) { if(tickCount%240==0) consume(1); return; }
        if(player.isShiftKeyDown() && player.getXRot()>88) { discard();return; }
        if(castTicks()>0) {
            entityData.set(CAST,castTicks()-1);
            if(tickCount%5==0) player.displayClientMessage(Component.translatable("message.nyctocosm.casting",Component.translatable("spell.nyctocosm.native."+spell().id()),castTicks()),true);
            if(castTicks()==0) finish(server,player);
        } else if(cooldown>0) {
            cooldown--;
        } else {
            for(ItemEntity item:level().getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(3))) {
                if(item.getOwner()!=player || item.getAge()>80 || item.getAge()<2 || distanceToSqr(player)>9) continue;
                Spell selected=Spell.from(item.getItem().getItem());
                if(selected==null) continue;
                item.getItem().shrink(1); if(item.getItem().isEmpty())item.discard();
                entityData.set(SPELL,selected.ordinal());entityData.set(CAST,selected.castTicks);
                if(selected.castTicks==0) finish(server,player);
                break;
            }
        }
    }
    private void finish(ServerLevel server,Player player) {
        Spell s=spell(); if(s==null)return;
        cooldown=50;
        switch(s) {
            case ABHORRENCE, INCANDESCENCE, FULMINANCE, FORBIDDANCE, GRAVITANCE -> {
                var effect=s==Spell.INCANDESCENCE?SpellBolt.Effect.FIRE:s==Spell.FULMINANCE?SpellBolt.Effect.LIGHTNING:s==Spell.FORBIDDANCE?SpellBolt.Effect.COUNTER:s==Spell.GRAVITANCE?SpellBolt.Effect.GRAVITY:SpellBolt.Effect.DAMAGE;
                Vec3 origin=player.getEyePosition();
                if(s==Spell.GRAVITANCE)origin=origin.add(new Vec3(player.getLookAngle().x,0,player.getLookAngle().z).normalize().scale(3));
                SpellBolt.launch(server,player,origin,player.getLookAngle(),s.damage,s.elements[0],s==Spell.GRAVITANCE?.3f:1.2f,s==Spell.FORBIDDANCE?0:2).effect(effect,s.duration);
            }
            case LUMINESCENCE -> { player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,s.duration,0,false,false));player.getPersistentData().putLong("NyctocosmLuminous",server.getGameTime()+s.duration); }
            case RESONANCE -> Combat.beam(server,player,48,s.damage,s.elements[0],.65);
            case EVANESCENCE -> NativeEvents.recall(player);
            case INCURRENCE -> NativeEvents.incur(player);
            case SUBSISTENCE -> {
                for(Player ally:server.getEntitiesOfClass(Player.class,getBoundingBox().inflate(3))){ally.heal(s.damage);ally.addEffect(new MobEffectInstance(MobEffects.SATURATION,60,0));}
            }
            case SUBMERGENCE -> {
                server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(7),target->Combat.canHit(player,target)&&target.onGround()&&distanceToSqr(target)>=9)
                    .stream().sorted(java.util.Comparator.comparingDouble(this::distanceToSqr)).limit(6).forEach(target->{
                        Combat.hit(player,target,s.damage,Combat.Element.VOID);target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,4));
                        server.sendParticles(ParticleTypes.SOUL,target.getX(),target.getY(),target.getZ(),30,.3,1,.3,.02);
                    });
            }
            case ADAMANCE, VACANCE, REFLECTANCE, REPELLANCE -> {
                Vec3 origin=s==Spell.REFLECTANCE||s==Spell.REPELLANCE?player.getEyePosition().add(player.getLookAngle().scale(4)):position();
                SpellArea.create(server,player,origin,s);
            }
            case RESISTANCE -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,s.duration,1));
            case TRANSMITTANCE -> { Vec3 at=Combat.endpoint(player,24).subtract(player.getLookAngle()); if(server.noCollision(player,player.getBoundingBox().move(at.subtract(player.position())))) {player.teleportTo(at.x,at.y,at.z);player.fallDistance=0;Combat.area(server,player,at,3,s.damage,s.elements[0],.5);} }
        }
        NativeEvents.sound(server,position(),"spells.transmutate."+s.id(),.8f);
        if(random.nextFloat()<.15f+(NativeEvents.hasArtifact(player,"accumulation")?.05f*NativeEvents.traces(player).size():0)) {
            if(NativeEvents.traces(player).size()<8) {
                SeelicField trace=new SeelicField(Content.FIELD.get(),server);
                trace.configure(player,false,true);trace.element=s.elements[0].ordinal();
                trace.setPos(position().add(random.nextDouble()*4-2,1,random.nextDouble()*4-2)); server.addFreshEntity(trace);
            }
        }
        entityData.set(SPELL,-1);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if(owner!=null)tag.putUUID("Owner",owner);tag.putBoolean("Mobile",mobile());tag.putBoolean("Trace",isHypertrace());
        tag.putInt("Spell",entityData.get(SPELL));tag.putInt("Cast",castTicks());tag.putInt("Charges",charges());tag.putInt("Life",life);tag.putInt("Cooldown",cooldown);tag.putInt("Element",element);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;entityData.set(MOBILE,tag.getBoolean("Mobile"));entityData.set(TRACE,tag.getBoolean("Trace"));
        entityData.set(SPELL,tag.getInt("Spell"));entityData.set(CAST,Math.max(0,tag.getInt("Cast")));entityData.set(CHARGE,tag.getInt("Charges"));life=tag.getInt("Life");cooldown=tag.getInt("Cooldown");element=tag.getInt("Element");
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
