package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Independent source Halo ages and stationary Void Beam aiming markers. */
public final class SorcererBeamNode extends Entity {
    public record Sample(Vec3 position,float yaw,float pitch){}
    private static final EntityDataAccessor<Boolean> HALO=SynchedEntityData.defineId(SorcererBeamNode.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(SorcererBeamNode.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(SorcererBeamNode.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RING_YAW=SynchedEntityData.defineId(SorcererBeamNode.class,EntityDataSerializers.FLOAT);
    private UUID owner,damageOwner;
    private boolean fired;
    private long bornTick=Long.MIN_VALUE;
    private List<Sample> lastSamples=List.of();
    public SorcererBeamNode(EntityType<? extends SorcererBeamNode> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(HALO,true);entityData.define(ACTIVE,false);entityData.define(AGE,0);entityData.define(RING_YAW,0f);}
    public boolean halo(){return entityData.get(HALO);}
    int age(){return entityData.get(AGE);}
    UUID ownerId(){return owner;}
    UUID damageOwnerId(){return damageOwner;}
    boolean counterable(){return halo();}
    List<Sample> lastSamples(){return lastSamples;}
    static SorcererBeamNode create(ServerLevel server,UUID owner,Vec3 at,float yaw,float pitch,boolean halo){
        var node=Content.SORCERER_BEAM_NODE.get().create(server);node.owner=owner;node.damageOwner=owner;node.entityData.set(HALO,halo);
        node.addTag(halo?"native_halo_node":"native_void_beam_node");node.setPos(at);node.setYRot(yaw);node.setXRot(pitch);node.entityData.set(RING_YAW,yaw);
        node.bornTick=server.getGameTime();server.addFreshEntity(node);return node;
    }
    static Vec3 haloPosition(NativeBoss boss,int frame){
        int index=frame/5-1;float[] pitches={0,-45,-90,45,0,-45,-90,45};double distance=index>=3&&index<=6?-5:5;
        return boss.position().add(0,2,0).add(SerpentMotion.localForward(boss.getYRot()+90,pitches[index]).scale(distance));
    }
    static SorcererBeamNode summonHalo(ServerLevel server,NativeBoss boss,int frame){var node=create(server,boss.getUUID(),haloPosition(boss,frame),boss.getYRot(),boss.getXRot(),true);node.attackTick(server);return node;}
    static List<Player> players(ServerLevel server,Vec3 at,double range){
        var players=server.getEntitiesOfClass(Player.class,new AABB(at,at).inflate(range),p->!p.isSpectator()&&p.position().distanceToSqr(at)<=range*range);players.sort(Comparator.comparingDouble(p->p.position().distanceToSqr(at)));return players;
    }
    static void aimVoid(ServerLevel server,NativeBoss boss){
        for(Player player:players(server,boss.position(),40)){var look=SorcererCastMath.facing(player.position().subtract(boss.position()));boss.setYRot(look.yaw());boss.setXRot(0);}
        Vec3 at=boss.position().add(0,3,0).add(SerpentMotion.localForward(boss.getYRot(),0).scale(2.5));
        for(Player player:players(server,at,40)){var look=SorcererCastMath.facing(player.getEyePosition().subtract(at));create(server,boss.getUUID(),at,look.yaw(),look.pitch(),false);}
    }
    static void activate(ServerLevel server,UUID owner){for(Entity entity:server.getAllEntities())if(entity instanceof SorcererBeamNode node&&node.halo()&&Objects.equals(owner,node.owner))node.entityData.set(ACTIVE,true);}
    static void voidBeams(ServerLevel server,UUID owner){for(Entity entity:server.getAllEntities())if(entity instanceof SorcererBeamNode node&&!node.halo()&&Objects.equals(owner,node.owner))node.fireVoid(server);}
    static void finishVoid(ServerLevel server,UUID owner){for(Entity entity:server.getAllEntities())if(entity instanceof SorcererBeamNode node&&!node.halo()&&Objects.equals(owner,node.owner))node.discard();}
    @Override public void tick(){super.tick();if(level() instanceof ServerLevel server&&bornTick!=server.getGameTime())attackTick(server);}
    void attackTickForTest(){if(level() instanceof ServerLevel server)attackTick(server);}
    private void attackTick(ServerLevel server){
        if(isRemoved()||!halo())return;if(fired){discard();return;}entityData.set(AGE,age()+1);entityData.set(RING_YAW,getYRot());
        if(age()<=65){var targets=players(server,position(),30);if(!targets.isEmpty()){var look=SorcererCastMath.facing(targets.get(0).getEyePosition().subtract(position()));setYRot(look.yaw());setXRot(look.pitch());}}
        if(entityData.get(ACTIVE)&&age()>=70&&!fired){
            fired=true;cast(server,10);NativeEvents.resonanceSound(server,position(),"sorcerer.halobeam.shoot",5,30);
            server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),5,.3,.3,.3,0);server.sendParticles(ParticleTypes.FLASH,getX(),getY(),getZ(),1,0,0,0,0);discard();
        }
    }
    void activateForTest(){entityData.set(ACTIVE,true);}
    void fireVoid(ServerLevel server){
        if(isRemoved()||halo())return;
        // Execute forks construct all contexts before terminal tp runs. Each uses the entry rotation.
        float yaw=getYRot(),pitch=getXRot();
        for(Player player:players(server,position(),40)){var look=SorcererCastMath.turn(yaw,pitch,player.getEyePosition().subtract(position()),2);setYRot(look.yaw());setXRot(look.pitch());}
        cast(server,3);
    }
    private void cast(ServerLevel server,float damage){
        List<Sample> samples=new ArrayList<>(60);Vec3 point=position();float yaw=getYRot(),pitch=getXRot();
        for(int range=1;range<=60;range++){
            if(!server.hasChunkAt(BlockPos.containing(point)))break;
            samples.add(new Sample(point,yaw,pitch));boolean end=range==60||!NativeBoss.emissaryPassable(server,BlockPos.containing(point));
            var reflection=Reflectance.tryReflect(server,new ReflectableAttack(point,damageOwner,damage));
            if(reflection!=null){damageOwner=reflection.owner().getUUID();yaw=reflection.reflector().getYRot();pitch=reflection.reflector().getXRot();}
            AABB first=new AABB(point.add(-.25,-.25,-.25),point.add(.75,.75,.75)),second=new AABB(point.add(-.75,-.75,-.75),point.add(.25,.25,.25));
            LivingEntity caster=damageOwner!=null&&server.getEntity(damageOwner) instanceof LivingEntity living?living:null;
            for(Player player:server.getEntitiesOfClass(Player.class,first,p->!p.isSpectator()&&p.getBoundingBox().intersects(second))){Combat.hit(caster,player,damage,Combat.Element.VOID);end=true;}
            if(end)break;point=point.add(SerpentMotion.localForward(yaw,pitch).scale(.5));
        }
        lastSamples=List.copyOf(samples);NativeNetwork.sorcererRay(this,lastSamples,halo());
    }
    public void clientVisualTick(){if(level().isClientSide&&halo())SorcererBeamEffects.emit(level(),entityData.get(ACTIVE)?"halobeamfinal":"halobeaminitial",position(),entityData.get(RING_YAW),-90);}
    @Override protected float getEyeHeight(Pose pose,EntityDimensions dimensions){return 0;}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);if(damageOwner!=null)tag.putUUID("DamageOwner",damageOwner);tag.putBoolean("Halo",halo());tag.putBoolean("Active",entityData.get(ACTIVE));tag.putInt("Age",age());tag.putBoolean("Fired",fired);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;damageOwner=tag.hasUUID("DamageOwner")?tag.getUUID("DamageOwner"):owner;entityData.set(HALO,!tag.contains("Halo")||tag.getBoolean("Halo"));entityData.set(ACTIVE,tag.getBoolean("Active"));entityData.set(AGE,Math.max(0,tag.getInt("Age")));fired=tag.getBoolean("Fired");entityData.set(RING_YAW,getYRot());addTag(halo()?"native_halo_node":"native_void_beam_node");bornTick=Long.MIN_VALUE;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
