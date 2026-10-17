package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Two source procast calls: twelve vertical arc samples each, one .4-block movement. */
public final class FrozenBlade extends Entity {
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(FrozenBlade.class,EntityDataSerializers.INT);
    private UUID owner,damageOwner;
    private long bornTick=Long.MIN_VALUE;
    private boolean killed;
    public FrozenBlade(EntityType<? extends FrozenBlade> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);addTag("native_frozen_blade");}
    @Override protected void defineSynchedData(){entityData.define(AGE,0);}
    @Override protected float getEyeHeight(Pose pose,EntityDimensions dimensions){return 0;}
    UUID ownerId(){return owner;}
    UUID damageOwnerId(){return damageOwner;}
    int age(){return entityData.get(AGE);}
    static FrozenBlade create(ServerLevel server,UUID owner,Vec3 at,float yaw,float pitch){
        var blade=Content.FROZEN_BLADE.get().create(server);blade.owner=owner;blade.damageOwner=owner;
        blade.setPos(at);blade.setYRot(yaw);blade.setXRot(pitch);blade.yRotO=yaw;blade.xRotO=pitch;blade.bornTick=server.getGameTime();server.addFreshEntity(blade);return blade;
    }
    static void launchPair(ServerLevel server,NativeBoss boss){
        for(int side:new int[]{1,-1}){
            float yaw=boss.getYRot()+side*90,pitch=boss.getXRot()+20;
            // Source hand ItemDisplays remain at the root; their render matrix does not move command coordinates.
            var blade=create(server,boss.getUUID(),boss.position().add(0,3,0).add(SerpentMotion.localForward(yaw,pitch).scale(.5)),yaw,pitch);
            blade.attackTick(server);
        }
    }
    public static List<Vec3> arc(Vec3 origin,float yaw,float pitch){
        var samples=new ArrayList<Vec3>(12);
        for(int offset=-60;offset<=50;offset+=10)samples.add(origin.add(SerpentMotion.localForward(yaw,pitch+offset).scale(1.5)));
        return List.copyOf(samples);
    }
    static Vec3 heading(Vec3 forward,Vec3 toward){return forward.normalize().scale(5).add(toward.normalize().scale(.5)).normalize();}
    static void charge(ServerLevel server,NativeBoss boss){
        for(int side:new int[]{1,-1}){
            float yaw=boss.getYRot()+side*90,pitch=boss.getXRot()+20;
            Vec3 center=boss.position().add(0,3.5,0).add(SerpentMotion.localForward(yaw,pitch).scale(1.7));
            for(double distance:new double[]{1,.77778,.55556,.33333,.11111,-.11111,-.33333,-.55556,-.77778,-1})
                force(server,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),center.add(SerpentMotion.localForward(yaw,pitch-90).scale(distance)),1,0);
        }
    }
    static void swing(ServerLevel server,NativeBoss boss){force(server,ParticleTypes.SWEEP_ATTACK,boss.position().add(0,1.6,0),4,.3);}
    @Override public void tick(){super.tick();if(level() instanceof ServerLevel server&&bornTick!=server.getGameTime())attackTick(server);}
    void attackTickForTest(){if(level() instanceof ServerLevel server)attackTick(server);}
    private void attackTick(ServerLevel server){
        if(isRemoved())return;entityData.set(AGE,age()+1);if(age()>=100)killed=true;
        for(int substep=0;substep<2;substep++){
            Vec3 entry=position();float yaw=getYRot(),pitch=getXRot();Vec3 forward=SerpentMotion.localForward(yaw,pitch);
            Player target=server.getEntitiesOfClass(Player.class,new AABB(entry,entry).inflate(50),p->!p.isSpectator()&&p.position().distanceToSqr(entry)<=2500)
                .stream().min(Comparator.comparingDouble(p->p.position().distanceToSqr(entry))).orElse(null);
            if(target!=null){var aimed=SorcererCastMath.turn(yaw,pitch,target.position().subtract(entry),5);setYRot(aimed.yaw());setXRot(aimed.pitch());}
            if(!NativeBoss.emissaryPassable(server,BlockPos.containing(entry)))killed=true;
            hitArc(server,entry,yaw,pitch);
            if(substep==0){
                var reflection=Reflectance.tryReflect(server,new ReflectableAttack(position(),damageOwner,14));
                Vec3 step=forward;
                if(reflection!=null){damageOwner=reflection.owner().getUUID();yaw=reflection.reflector().getYRot();pitch=reflection.reflector().getXRot();step=SerpentMotion.localForward(yaw,pitch);}
                Vec3 next=entry.add(step.scale(.4));
                if(!server.hasChunkAt(BlockPos.containing(next))){discard();return;}
                // The movement tp carries `~ ~` from this procast's entry rotation;
                // it resets the first turn. Only the second, endcast turn survives.
                setYRot(yaw);setXRot(pitch);setPos(next);
            }
        }
        hurtMarked=true;
        if(killed){NativeEvents.masterSound(server,position(),SoundEvents.GLASS_BREAK,1,30);discard();}
    }
    private void hitArc(ServerLevel server,Vec3 origin,float yaw,float pitch){
        Set<UUID> hit=new HashSet<>();LivingEntity caster=damageOwner!=null&&server.getEntity(damageOwner) instanceof LivingEntity living?living:null;
        for(Vec3 point:arc(origin,yaw,pitch)){
            force(server,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),point,1,0);
            force(server,ParticleTypes.ELECTRIC_SPARK,point,1,0);
            for(Player player:server.getEntitiesOfClass(Player.class,new AABB(point,point).inflate(2.00001),p->!p.isSpectator()&&p.position().distanceToSqr(point)<=4+1e-7)){
                killed=true;if(hit.add(player.getUUID()))Combat.hit(caster,player,14,Combat.Element.VOID);
                // Source Slowness II is pending D28; no opener or guessed cancellation behavior.
            }
        }
    }
    private static void force(ServerLevel server,ParticleOptions particle,Vec3 at,int count,double spread){
        for(ServerPlayer player:server.players())server.sendParticles(player,particle,true,at.x,at.y,at.z,count,spread,spread,spread,0);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);if(damageOwner!=null)tag.putUUID("DamageOwner",damageOwner);tag.putInt("Age",age());tag.putBoolean("Killed",killed);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;damageOwner=tag.hasUUID("DamageOwner")?tag.getUUID("DamageOwner"):owner;entityData.set(AGE,Math.max(0,tag.getInt("Age")));killed=tag.getBoolean("Killed");bornTick=Long.MIN_VALUE;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
