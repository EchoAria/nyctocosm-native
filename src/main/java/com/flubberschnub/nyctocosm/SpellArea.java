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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Persistent spell effects are separate from the focus, allowing overlapping casts. */
public final class SpellArea extends Entity {
    private static final EntityDataAccessor<Integer> SPELL=SynchedEntityData.defineId(SpellArea.class,EntityDataSerializers.INT);
    private UUID owner;
    private int life;
    public SpellArea(EntityType<? extends SpellArea> type,Level level){super(type,level);}
    public static SpellArea create(ServerLevel level,Player player,Vec3 origin,Spell spell){
        SpellArea area=new SpellArea(Content.AREA.get(),level);area.owner=player.getUUID();area.life=spell.duration;area.entityData.set(SPELL,spell.ordinal());
        area.setPos(origin);area.setYRot(player.getYRot());area.setXRot(player.getXRot());level.addFreshEntity(area);return area;
    }
    public Spell spell(){return Spell.values()[Math.floorMod(entityData.get(SPELL),Spell.values().length)];}
    @Override protected void defineSynchedData(){entityData.define(SPELL,Spell.ADAMANCE.ordinal());}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel server))return;
        Player player=owner==null?null:level().getPlayerByUUID(owner);
        if(--life<=0||player==null||!player.isAlive()){discard();return;}
        switch(spell()){
            case ADAMANCE -> {if(!player.isShiftKeyDown()&&distanceToSqr(player)<144){player.teleportTo(getX(),getY(),getZ());player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;}}
            case REFLECTANCE -> {
                Vec3 normal=getLookAngle();
                for(Projectile projectile:server.getEntitiesOfClass(Projectile.class,getBoundingBox().inflate(3))){
                    if(projectile.getOwner()==player)continue;
                    Vec3 velocity=projectile.getDeltaMovement();
                    projectile.setOwner(player);projectile.setDeltaMovement(velocity.subtract(normal.scale(2*velocity.dot(normal))));projectile.hurtMarked=true;
                }
            }
            case VACANCE -> {
                repel(server,player,6,1);
                if(tickCount%5==0){
                    var vacable=TagKey.create(Registries.BLOCK,new ResourceLocation("nyctocosm","vacable"));
                    for(BlockPos pos:BlockPos.betweenClosed(blockPosition().offset(-6,-6,-6),blockPosition().offset(6,6,6)))
                        if(server.getBlockState(pos).is(vacable))server.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                }
            }
            case REPELLANCE -> {repel(server,player,8,.6);setPos(position().add(getLookAngle().scale(.25)));}
            default -> {}
        }
        if(tickCount%5==0)server.sendParticles(ParticleTypes.ENCHANT,getX(),getY()+.5,getZ(),12,2,.5,2,.02);
    }
    private void repel(ServerLevel level,Player player,double radius,double power){
        for(Entity entity:level.getEntities(this,getBoundingBox().inflate(radius))){
            if(entity==player||entity instanceof SpellArea||entity instanceof SeelicField||entity instanceof net.minecraft.world.entity.decoration.ArmorStand)continue;
            if(entity instanceof LivingEntity living&&!Combat.canHit(player,living))continue;
            if(!(entity instanceof LivingEntity)&&!(entity instanceof Projectile)&&!(entity instanceof net.minecraft.world.entity.item.ItemEntity))continue;
            entity.setDeltaMovement(entity.position().subtract(position()).normalize().scale(power));entity.hurtMarked=true;
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);tag.putInt("Life",life);tag.putInt("Spell",entityData.get(SPELL));}
    @Override protected void readAdditionalSaveData(CompoundTag tag){owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;life=tag.getInt("Life");entityData.set(SPELL,tag.getInt("Spell"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
