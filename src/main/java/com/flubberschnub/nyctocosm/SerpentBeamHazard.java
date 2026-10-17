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
    private static final DustParticleOptions RED=new DustParticleOptions(new Vector3f(1,0,0),4);
    private static final DustParticleOptions BLACK=new DustParticleOptions(new Vector3f(0,0,0),4);

    private UUID targetId;

    public SerpentBeamHazard(EntityType<? extends SerpentBeamHazard> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(OWNER,Optional.empty());entityData.define(DELAY,200);}

    public static SerpentBeamHazard spawn(ServerLevel level,LivingEntity owner,Vec3 at,int delay){
        SerpentBeamHazard beam=Content.SERPENT_BEAM.get().create(level);
        if(beam==null)throw new IllegalStateException("Serpent beam entity is unavailable");
        beam.entityData.set(OWNER,Optional.of(owner.getUUID()));beam.entityData.set(DELAY,delay);beam.setPos(at);level.addFreshEntity(beam);return beam;
    }

    public static SerpentBeamHazard spawn(ServerLevel level,LivingEntity owner,LivingEntity target,int delay){
        SerpentBeamHazard beam=spawn(level,owner,target.position(),delay);beam.targetId=target.getUUID();return beam;
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
        UUID selected=caster instanceof NativeBoss boss&&boss.serpentLastSelectedTarget()!=null?boss.serpentLastSelectedTarget():targetId;
        if(delay==140&&selected!=null&&server.getEntity(selected) instanceof LivingEntity target&&target.isAlive()){
            Vec3 landing=chooseLanding(server,target.position(),random);
            if(landing!=null)setPos(landing);
            NativeEvents.sound(server,target.position(),"serpent.beam.charge",4);
        }
        if(delay==80){
            NativeEvents.sound(server,position(),"serpent.beam.shoot",4);
            server.sendParticles(ParticleTypes.FLASH,getX(),getY()+.2,getZ(),3,1,0,1,0);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY()+.2,getZ(),1,0,0,0,0);
        }
        if(delay<=60){
            AABB bounds=new AABB(position().add(0,1,0),position().add(0,80,0)).inflate(5);
            for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,bounds)){
                // Original selectors use feet-distance to each integer ray step, not a square column.
                if(target.hurtTime==0&&containsFeet(position(),target.position())&&Combat.hit(caster,target,18,Combat.Element.NIGHTMARE))
                    target.addEffect(new MobEffectInstance(MobEffects.DARKNESS,300,0,true,true));
            }
        }
    }
    static boolean containsFeet(Vec3 base,Vec3 feet){
        double step=net.minecraft.util.Mth.clamp(Math.floor(feet.y-base.y+.5),1,80);
        return feet.distanceToSqr(base.add(0,step,0))<=25;
    }
    public static int visualRows(int budget){return Math.min(40,Math.max(0,budget/2));}
    public void clientVisualTick(int budget){
        if(!level().isClientSide||budget<=0)return;
        int delay=delay();
        if(delay<=139&&delay>=81){
            int count=Math.min(28,budget);
            for(int i=0;i<count;i++){
                Vec3 at=position().add(random.nextGaussian()*2,0,random.nextGaussian()*2);
                level().addParticle(i%10<7?BLACK:RED,true,at.x,at.y,at.z,0,0,0);
            }
        }else if(delay>0&&delay<=60){
            int rows=visualRows(budget);
            for(int row=0;row<rows;row++){
                double height=rows==1?40:1+79.0*row/(rows-1);
                Vec3 at=position().add(random.nextGaussian(),height+random.nextGaussian(),random.nextGaussian());
                level().addParticle(RED,true,at.x,at.y,at.z,0,0,0);
                level().addParticle(BLACK,true,at.x,at.y,at.z,0,0,0);
            }
        }
    }
    /** Single-entity spreadplayers: square distribution, block centers, two air blocks above safe ground. */
    static Vec3 chooseLanding(ServerLevel level,Vec3 center,net.minecraft.util.RandomSource random){
        for(int attempt=0;attempt<10000;attempt++){
            int x=net.minecraft.util.Mth.floor(center.x-20+random.nextDouble()*40);
            int z=net.minecraft.util.Mth.floor(center.z-20+random.nextDouble()*40);
            for(int y=level.getMaxBuildHeight()-1;y>=level.getMinBuildHeight();y--){
                var ground=new net.minecraft.core.BlockPos(x,y,z);var state=level.getBlockState(ground);
                if(state.isAir()||!level.getBlockState(ground.above()).isAir()||!level.getBlockState(ground.above(2)).isAir())continue;
                if(!state.liquid()&&!state.is(net.minecraft.tags.BlockTags.FIRE))return new Vec3(x+.5,y+1,z+.5);
                break;
            }
        }
        return null; // Like a failed spreadplayers command, retain the marker's previous position.
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag){targetId=tag.hasUUID("Target")?tag.getUUID("Target"):null;if(tag.hasUUID("Owner"))entityData.set(OWNER,Optional.of(tag.getUUID("Owner")));entityData.set(DELAY,tag.getInt("Delay"));}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(targetId!=null)tag.putUUID("Target",targetId);entityData.get(OWNER).ifPresent(id->tag.putUUID("Owner",id));tag.putInt("Delay",delay());}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
