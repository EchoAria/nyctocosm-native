package com.flubberschnub.nyctocosm;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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

/** Thirty radial source-authored ice waves; every fifth tick leaves a rising spike ring. */
public final class IceSpikeWave extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(IceSpikeWave.class,EntityDataSerializers.OPTIONAL_UUID);

    public IceSpikeWave(EntityType<? extends IceSpikeWave> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(OWNER,Optional.empty());}

    public static IceSpikeWave spawn(ServerLevel level,LivingEntity owner,Vec3 origin){
        IceSpikeWave wave=Content.ICE_SPIKE_WAVE.get().create(level);
        if(wave==null)throw new IllegalStateException("Ice spike wave entity is unavailable");
        wave.entityData.set(OWNER,Optional.of(owner.getUUID()));wave.setPos(origin);level.addFreshEntity(wave);return wave;
    }

    private LivingEntity owner(){
        if(!(level() instanceof ServerLevel server)||entityData.get(OWNER).isEmpty())return null;
        return server.getEntity(entityData.get(OWNER).get()) instanceof LivingEntity living?living:null;
    }

    public static Vec3 point(Vec3 origin,int arm,double radius){
        double angle=Math.toRadians(arm*12);return origin.add(Math.sin(angle)*radius,0,Math.cos(angle)*radius);
    }

    @Override public void tick(){
        super.tick();
        if(level().isClientSide)return;
        ServerLevel server=(ServerLevel)level();LivingEntity caster=owner();
        if(caster==null||tickCount>50){discard();return;}
        if(tickCount<=25){
            double radius=tickCount*.5;
            for(int arm=0;arm<30;arm++){
                Vec3 raw=point(position(),arm,radius);double surface=surfaceY(server,raw.x,raw.z,getY()+5);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),raw.x,surface+.12,raw.z,2,.08,.08,.08,.02);
            }
        }
        for(int born=5;born<=25;born+=5){
            int spikeAge=tickCount-born;if(spikeAge<0||spikeAge>=25)continue;
            double radius=born*.5;
            for(int arm=0;arm<30;arm++){
                Vec3 raw=point(position(),arm,radius);double surface=surfaceY(server,raw.x,raw.z,getY()+5);
                double rise=spikeAge<5?spikeAge*.4:spikeAge<20?2:(25-spikeAge)*.4;
                Vec3 spike=new Vec3(raw.x,surface+rise,raw.z);
                server.sendParticles(ParticleTypes.SNOWFLAKE,spike.x,spike.y+.5,spike.z,2,.08,.2,.08,.01);
                if(spikeAge>=6&&spikeAge<=10){
                    for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,new AABB(spike,spike).inflate(2))){
                        if(Combat.hit(caster,target,3,Combat.Element.VOID))target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,1,true,true));
                    }
                }
            }
        }
    }

    private static double surfaceY(ServerLevel level,double x,double z,double start){
        BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos((int)Math.floor(x),(int)Math.floor(start),(int)Math.floor(z));
        for(int step=0;step<24&&cursor.getY()>level.getMinBuildHeight()+1;step++,cursor.move(0,-1,0)){
            if(!level.getBlockState(cursor.below()).isAir()&&!level.getBlockState(cursor.below()).getCollisionShape(level,cursor.below()).isEmpty())return cursor.getY();
        }
        return Math.floor(start-5);
    }

    public float spikeRise(int born,float partial){
        float age=tickCount+partial-born;if(age<0||age>=25)return -1;
        return age<5?age*.4f:age<20?2:(25-age)*.4f;
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag){if(tag.hasUUID("Owner"))entityData.set(OWNER,Optional.of(tag.getUUID("Owner")));}
    @Override protected void addAdditionalSaveData(CompoundTag tag){entityData.get(OWNER).ifPresent(id->tag.putUUID("Owner",id));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
