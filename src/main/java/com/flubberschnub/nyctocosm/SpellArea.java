package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Persistent spell effects are separate from the focus, allowing overlapping casts. */
public final class SpellArea extends Entity {
    private static final EntityDataAccessor<Integer> SPELL=SynchedEntityData.defineId(SpellArea.class,EntityDataSerializers.INT);
    private UUID owner;
    private UUID target;
    private int life;
    private int gatheringTicks=26;
    private float reflectanceEnergy=20;
    public SpellArea(EntityType<? extends SpellArea> type,Level level){super(type,level);}
    public static SpellArea create(ServerLevel level,Player player,Vec3 origin,Spell spell){
        SpellArea area=new SpellArea(Content.AREA.get(),level);area.owner=player.getUUID();area.life=spell.duration;area.entityData.set(SPELL,spell.ordinal());
        area.setPos(origin);area.setYRot(player.getYRot());area.setXRot(player.getXRot());level.addFreshEntity(area);return area;
    }
    public static SpellArea createSubmergence(ServerLevel level,Player player,LivingEntity target){
        SpellArea area=new SpellArea(Content.AREA.get(),level);area.owner=player.getUUID();area.target=target.getUUID();area.life=200;
        area.entityData.set(SPELL,Spell.SUBMERGENCE.ordinal());area.setPos(target.getX(),target.getY()-5,target.getZ());level.addFreshEntity(area);return area;
    }
    public Spell spell(){return Spell.values()[Math.floorMod(entityData.get(SPELL),Spell.values().length)];}
    UUID ownerId(){return owner;}
    Player reflectionOwner(ServerLevel server){
        if(isRemoved()||spell()!=Spell.REFLECTANCE||owner==null)return null;
        if(!(server.getEntity(owner) instanceof Player player)||!player.isAlive())return null;
        return player;
    }
    void consumeReflectance(float damage){
        if(isRemoved()||spell()!=Spell.REFLECTANCE||damage<=0||!(level() instanceof ServerLevel server))return;
        reflectanceEnergy-=damage;
        NativeEvents.sound(server,position(),"spells.impact.reflectance",10);
        server.sendParticles(ParticleTypes.FLASH,getX(),getY(),getZ(),3,.2,.2,.2,0);
        if(reflectanceEnergy<=0)discard();
    }
    static boolean preventsForcedMovement(Entity entity){
        var data=entity.getPersistentData();
        return data.contains("NyctocosmImmoveableUntil")&&data.getLong("NyctocosmImmoveableUntil")>=entity.level().getGameTime();
    }
    private void protectMovement(Player player){player.getPersistentData().putLong("NyctocosmImmoveableUntil",level().getGameTime()+1);}
    @Override protected void defineSynchedData(){entityData.define(SPELL,Spell.ADAMANCE.ordinal());}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel server))return;
        Player player=owner==null?null:level().getPlayerByUUID(owner);
        if(player==null||!player.isAlive()){discard();return;}
        if(spell()==Spell.REPELLANCE){
            if(!server.getBlockState(blockPosition()).is(TagKey.create(Registries.BLOCK,new ResourceLocation(NativeMod.ID,"passthrough")))){discard();return;}
            if(gatheringTicks>0){
                server.sendParticles(ParticleTypes.CLOUD,getX(),getY(),getZ(),3,.1,.1,.1,.3);
                if(--gatheringTicks>0)return;
                // Removing the source gathering tag also runs the movement branch on tick 26.
                server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),5,.4,.4,.4,0);
                server.sendParticles(ParticleTypes.CLOUD,getX(),getY(),getZ(),100,.1,.1,.1,.7);
            }
            if(life<=0){discard();return;}
            --life;
        }else if(--life<=0){discard();return;}
        switch(spell()){
            case SUBMERGENCE -> {
                Entity found=target==null?null:server.getEntity(target);
                if(!(found instanceof LivingEntity victim)||!victim.isAlive()||!Combat.canHit(player,victim)){discard();return;}
                int age=200-life;
                if(age<=80){
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,2,10,false,false));
                    victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION,2,2,false,false));
                }else{
                    if(age==81)Combat.hit(player,victim,Spell.SUBMERGENCE.damage,Combat.Element.VOID);
                    Vec3 pull=position().subtract(victim.position());
                    if(pull.lengthSqr()<=1){discard();return;}
                    victim.setDeltaMovement(pull.normalize());victim.hurtMarked=true;
                }
                Vec3 feet=victim.position(),anchor=position().add(0,5,0),delta=anchor.subtract(feet);
                int samples=Math.min(60,Math.max(1,(int)Math.ceil(delta.length()/.5)));
                for(int n=0;n<=samples;n++){
                    Vec3 point=feet.add(delta.scale(n/(double)samples));
                    server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,point.x,point.y,point.z,2,0,0,0,.01);
                }
                server.sendParticles(ParticleTypes.REVERSE_PORTAL,getX(),getY()+5,getZ(),10,.1,.1,.1,.1);
            }
            case ADAMANCE -> {
                protectMovement(player);
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION,20,255,false,false));
                if(!player.isShiftKeyDown()){
                    // FakePlayer ignores teleportTo; update the server position as well.
                    player.setPos(getX(),getY(),getZ());
                    player.teleportTo(getX(),getY(),getZ());player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
                }
            }
            case REFLECTANCE -> {
                Vec3 normal=getLookAngle();
                Vec3 behind=position().subtract(normal.scale(3));
                for(Player protectedPlayer:server.getEntitiesOfClass(Player.class,new net.minecraft.world.phys.AABB(behind,behind).inflate(3),candidate->candidate.position().distanceToSqr(behind)<=9))protectMovement(protectedPlayer);
                AABB projectileSearch=new AABB(position(),position()).inflate(Reflectance.SOURCE_RADIUS+1.0e-4);
                for(SpellBolt projectile:server.getEntitiesOfClass(SpellBolt.class,projectileSearch,
                    candidate->candidate.position().distanceToSqr(position())<=Reflectance.SOURCE_RADIUS*Reflectance.SOURCE_RADIUS+1.0e-7
                        &&!candidate.reflectedByReflectanceThisTick()&&!candidate.handlesOwnReflection())){
                    var reflection=Reflectance.tryReflect(server,new ReflectableAttack(projectile.position(),
                        projectile.getOwner()==null?null:projectile.getOwner().getUUID(),projectile.spellDamage()));
                    if(reflection==null)continue;
                    projectile.reflectFrom(reflection.owner(),reflection.direction());
                    server.sendParticles(ParticleTypes.FLASH,projectile.getX(),projectile.getY(),projectile.getZ(),5,.2,.2,.2,0);
                }
            }
            case VACANCE -> {
                pushEntities(server,player,6,1,.5,true);
                clearVacable(server);
            }
            case REPELLANCE -> {
                setPos(position().add(getLookAngle().scale(2)));
                clearVacable(server);
                if(!server.getBlockState(blockPosition()).is(TagKey.create(Registries.BLOCK,new ResourceLocation(NativeMod.ID,"passthrough")))){discard();return;}
                pushEntities(server,player,6,1.5,2,false);
                server.sendParticles(ParticleTypes.CLOUD,getX(),getY(),getZ(),20,4,4,4,.05);
                if(life==0)discard();
            }
            default -> {}
        }
        if(tickCount%5==0)server.sendParticles(ParticleTypes.ENCHANT,getX(),getY()+.5,getZ(),12,2,.5,2,.02);
    }
    private void clearVacable(ServerLevel level){
        var vacable=TagKey.create(Registries.BLOCK,new ResourceLocation(NativeMod.ID,"vacable"));
        for(BlockPos pos:BlockPos.betweenClosed(blockPosition().offset(-6,-6,-6),blockPosition().offset(6,6,6)))
            if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(vacable))level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    }
    private void pushEntities(ServerLevel level,Player player,double radius,double power,double playerStep,boolean radial){
        for(Entity entity:level.getEntities(this,getBoundingBox().inflate(radius))){
            if(entity==player||entity instanceof SpellArea||entity instanceof SeelicField||entity instanceof net.minecraft.world.entity.decoration.ArmorStand)continue;
            if(entity.distanceToSqr(position())>radius*radius)continue;
            if(!radial&&preventsForcedMovement(entity))continue;
            if(entity instanceof LivingEntity living&&!Combat.canHit(player,living))continue;
            if(!(entity instanceof LivingEntity)&&!(entity instanceof Projectile)&&!(entity instanceof net.minecraft.world.entity.item.ItemEntity))continue;
            // 'facing entity @s' is evaluated at the bubble; Repellance inherits the gust rotation.
            Vec3 direction=radial?entity.position().subtract(position()).normalize():getLookAngle();
            if(entity instanceof Player affected){
                if(!radial&&affected.isPassenger())continue;
                Vec3 at=entity.position().add(direction.scale(playerStep));
                if(level.noCollision(affected,affected.getBoundingBox().move(at.subtract(affected.position())))){
                    affected.teleportTo(at.x,at.y,at.z);affected.setPos(at);
                }
            }else{entity.setDeltaMovement(direction.scale(power));entity.hurtMarked=true;}
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);if(target!=null)tag.putUUID("Target",target);tag.putInt("Life",life);tag.putInt("GatheringTicks",gatheringTicks);tag.putInt("Spell",entityData.get(SPELL));tag.putFloat("ReflectanceEnergy",reflectanceEnergy);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;target=tag.hasUUID("Target")?tag.getUUID("Target"):null;
        life=tag.getInt("Life");entityData.set(SPELL,tag.getInt("Spell"));reflectanceEnergy=tag.contains("ReflectanceEnergy")?tag.getFloat("ReflectanceEnergy"):20;
        gatheringTicks=tag.contains("GatheringTicks")?Math.max(0,Math.min(26,tag.getInt("GatheringTicks"))):Math.max(0,26-(Spell.REPELLANCE.duration-life));
        if(spell()==Spell.REPELLANCE&&!tag.contains("GatheringTicks")&&gatheringTicks>0)life=Spell.REPELLANCE.duration;
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
