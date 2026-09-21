package com.flubberschnub.nyctocosm;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/** One of the Serpent's twenty independently delayed, telegraphed nightmare columns. */
public final class SerpentBeamHazard extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(SerpentBeamHazard.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DELAY=SynchedEntityData.defineId(SerpentBeamHazard.class,EntityDataSerializers.INT);
    private static final DustParticleOptions RED=new DustParticleOptions(new Vector3f(1,0,0),1.4f);
    private static final DustParticleOptions BLACK=new DustParticleOptions(new Vector3f(0,0,0),1.7f);

    public SerpentBeamHazard(EntityType<? extends SerpentBeamHazard> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(OWNER,Optional.empty());entityData.define(DELAY,200);}

    public static SerpentBeamHazard spawn(ServerLevel level,LivingEntity owner,Vec3 at,int delay){
        SerpentBeamHazard beam=Content.SERPENT_BEAM.get().create(level);
        if(beam==null)throw new IllegalStateException("Serpent beam entity is unavailable");
        beam.entityData.set(OWNER,Optional.of(owner.getUUID()));beam.entityData.set(DELAY,delay);beam.setPos(at);level.addFreshEntity(beam);return beam;
    }

    public int delay(){return entityData.get(DELAY);}
    public Optional<UUID> ownerId(){return entityData.get(OWNER);}
    void setDelayForTest(int delay){entityData.set(DELAY,delay);}
    private LivingEntity owner(){
        if(!(level() instanceof ServerLevel server)||entityData.get(OWNER).isEmpty())return null;
        return server.getEntity(entityData.get(OWNER).get()) instanceof LivingEntity living?living:null;
    }

    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        ServerLevel server=(ServerLevel)level();LivingEntity caster=owner();int delay=entityData.get(DELAY)-1;entityData.set(DELAY,delay);
        if(caster==null||delay<=0){discard();return;}
        if(delay==140)NativeEvents.sound(server,position(),"serpent.beam.charge",4);
        if(delay<=139&&delay>=81){
            server.sendParticles(RED,getX(),getY()+.1,getZ(),3,2.5,.05,2.5,.01);
            server.sendParticles(BLACK,getX(),getY()+.1,getZ(),2,2.5,.05,2.5,.01);
            if((delay&3)==0)vertical(server,RED,1.5);
        }
        if(delay==80){
            NativeEvents.sound(server,position(),"serpent.beam.shoot",4);
            server.sendParticles(ParticleTypes.FLASH,getX(),getY()+.2,getZ(),3,1,0,1,0);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY()+.2,getZ(),1,0,0,0,0);
        }
        if(delay<=60){
            vertical(server,(delay&1)==0?RED:BLACK,2.5);
            for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(5,3,5))){
                if(Combat.hit(caster,target,18,Combat.Element.NIGHTMARE))target.addEffect(new MobEffectInstance(MobEffects.DARKNESS,300,0,true,true));
            }
        }
    }

    private void vertical(ServerLevel server,DustParticleOptions particle,double spread){
        for(int y=0;y<=24;y++)server.sendParticles(particle,getX(),getY()+y,getZ(),1,spread*.05,.03,spread*.05,0);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag){if(tag.hasUUID("Owner"))entityData.set(OWNER,Optional.of(tag.getUUID("Owner")));entityData.set(DELAY,tag.getInt("Delay"));}
    @Override protected void addAdditionalSaveData(CompoundTag tag){entityData.get(OWNER).ifPresent(id->tag.putUUID("Owner",id));tag.putInt("Delay",delay());}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
