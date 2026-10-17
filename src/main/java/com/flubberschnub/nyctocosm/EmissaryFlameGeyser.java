package com.flubberschnub.nyctocosm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** One delayed, ground-placed Flame Geysers eruption. World fire placement is awaiting C03. */
public final class EmissaryFlameGeyser extends Entity {
    static final int SOURCE_NODES_PER_TARGET=14;
    static final int SOURCE_MIN_TIMER=20;
    static final int SOURCE_MAX_TIMER=149;
    static final int SOURCE_ERUPTION_HEIGHT=49;
    static final float SOURCE_DAMAGE=40;
    private static final int[] SOURCE_HAND_CUE_FRAMES={10,20,25,30,33,36,38};
    private UUID owner,targetGroup,targetPlayer;
    private int timer=SOURCE_MIN_TIMER,age,missingOwnerTicks,warningTicks;

    public EmissaryFlameGeyser(EntityType<? extends EmissaryFlameGeyser> type,Level level){
        super(type,level);noPhysics=true;setNoGravity(true);setDeltaMovement(Vec3.ZERO);
    }

    static int launch(ServerLevel level,NativeBoss boss){
        Set<Player> candidates=new LinkedHashSet<>(level.players());
        candidates.addAll(level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(300)));
        List<Player> eligible=candidates.stream().filter(player->!player.isSpectator()
            &&!player.getTags().contains("emissarydebug")&&player.distanceToSqr(boss)<=300*300).toList();
        List<Player> selected=new ArrayList<>(eligible);
        int made=0;
        for(int count=0;count<Math.min(2,selected.size());count++){
            Player target=selected.remove(boss.getRandom().nextInt(selected.size()));
            UUID group=target.getUUID();
            var positions=spreadOnSurface(level,target.position(),boss,SOURCE_NODES_PER_TARGET);
            for(Vec3 at:positions){
                var geyser=Content.EMISSARY_FLAME_GEYSER.get().create(level);
                geyser.owner=boss.getUUID();geyser.targetGroup=group;geyser.targetPlayer=target.getUUID();
                geyser.timer=SOURCE_MIN_TIMER+boss.getRandom().nextInt(SOURCE_MAX_TIMER-SOURCE_MIN_TIMER+1);
                geyser.setPos(at);level.addFreshEntity(geyser);made++;
            }
            // The source runs activate from each selected player's position.
            NativeEvents.resonanceSound(level,target.position(),"emissary.attack.flame_geysers.start",50,300);
        }
        return made;
    }

    static boolean isHandCueFrame(int frame){
        for(int cue:SOURCE_HAND_CUE_FRAMES)if(cue==frame)return true;
        return false;
    }

    static Vec3 handCuePositionForTest(NativeBoss boss,int frame){
        if(!isHandCueFrame(frame))throw new IllegalArgumentException("No Flame Geysers hand cue at frame "+frame);
        var locator=EmissaryDamagePose.sampleAt(boss,"phase2.attack.flame_geysers","righthand",frame);
        return EmissaryDamagePose.worldPosition(boss,locator);
    }

    static void handCue(ServerLevel server,NativeBoss boss,int frame){
        Vec3 at=handCuePositionForTest(boss,frame);
        server.sendParticles(ParticleTypes.FLAME,at.x,at.y,at.z,5,.1,.1,.1,.05);
        server.sendParticles(ParticleTypes.FLASH,at.x,at.y,at.z,1,.1,.1,.1,0);
        server.sendParticles(ParticleTypes.LAVA,at.x,at.y,at.z,10,.1,.1,.1,0);
    }

    private static List<Vec3> spreadOnSurface(ServerLevel level,Vec3 center,NativeBoss boss,int count){
        List<Vec3> result=new ArrayList<>();
        int attempts=0,maxAttempts=count*500;
        while(result.size()<count&&attempts++<maxAttempts){
            double angle=boss.getRandom().nextDouble()*Math.PI*2;
            double radius=Math.sqrt(boss.getRandom().nextDouble())*25;
            double x=center.x+Math.cos(angle)*radius;
            double z=center.z+Math.sin(angle)*radius;
            BlockPos column=BlockPos.containing(x,center.y,z);
            if(!level.hasChunkAt(column))continue;
            int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,column.getX(),column.getZ());
            if(y<=level.getMinBuildHeight()||y>=level.getMaxBuildHeight()-1)continue;
            BlockPos floor=new BlockPos(column.getX(),y-1,column.getZ());
            if(level.getBlockState(floor).isAir()||!level.getFluidState(floor).isEmpty())continue;
            Vec3 at=new Vec3(x,y,z);
            boolean crowded=result.stream().anyMatch(other->{
                double dx=other.x-at.x,dz=other.z-at.z;
                return dx*dx+dz*dz<25;
            });
            if(!crowded)result.add(at);
        }
        return result;
    }

    UUID ownerForTest(){return owner;}
    UUID groupForTest(){return targetGroup;}
    UUID targetForTest(){return targetPlayer;}
    int timerForTest(){return timer;}
    int ageForTest(){return age;}
    int warningTicksForTest(){return warningTicks;}
    void configureForTest(NativeBoss boss,Player target,int sourceTimer){
        owner=boss.getUUID();targetGroup=target.getUUID();targetPlayer=target.getUUID();timer=Mth.clamp(sourceTimer,SOURCE_MIN_TIMER,SOURCE_MAX_TIMER);
    }
    void timerForTest(int value){timer=Math.max(0,value);}

    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel server))return;
        Entity found=owner==null?null:server.getEntity(owner);
        if(!(found instanceof NativeBoss boss)){
            if(++missingOwnerTicks>20)discard();
            return;
        }
        if(!boss.isAlive()){discard();return;}
        missingOwnerTicks=0;age++;timer--;
        if(timer==20)NativeEvents.resonanceSound(server,position(),"emissary.attack.flame_geysers.erupt",1.5f,300);
        if(timer<=0){erupt(server,boss);discard();return;}
        if(timer<=40)warn(server);
    }

    private void warn(ServerLevel server){
        warningTicks++;
        for(int degrees=0;degrees<360;degrees+=10){
            double radians=Math.toRadians(degrees);
            server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1,0,0),1),
                getX()+Math.cos(radians)*5,getY()+.05,getZ()+Math.sin(radians)*5,1,0,0,0,0);
        }
    }

    private void erupt(ServerLevel server,NativeBoss boss){
        // Aggregate source's 50 one-block particle rings into a bounded plume packet.
        server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY()+25,getZ(),50,.5,25,.5,0);
        server.sendParticles(ParticleTypes.FLAME,getX(),getY()+25,getZ(),200,1,25,1,.2);
        server.sendParticles(ParticleTypes.LAVA,getX(),getY()+25,getZ(),100,1,25,1,0);
        server.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+25,getZ(),150,1,25,1,0);
        server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),5,.4,.4,.4,0);

        AABB broadphase=new AABB(getX()-5,getY()-5,getZ()-5,getX()+5,getY()+SOURCE_ERUPTION_HEIGHT+6,getZ()+5);
        for(Player player:server.getEntitiesOfClass(Player.class,broadphase,candidate->
            !candidate.isSpectator()&&!candidate.getTags().contains("emissarydebug"))){
            int nearestStep=Mth.clamp(Mth.floor(player.getY()-getY()+.5),0,SOURCE_ERUPTION_HEIGHT);
            Vec3 point=position().add(0,nearestStep,0);
            if(player.position().distanceToSqr(point)<=25)Combat.hit(boss,player,SOURCE_DAMAGE,Combat.Element.DISSONANCE);
        }

        // Source shoots the fire plume into air and fills a 7x7x7 fire volume at the base.
        // Keep the block mutation absent until the user's C03 fire rule is chosen.
    }

    @Override protected void defineSynchedData(){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){
        if(owner!=null)tag.putUUID("Owner",owner);if(targetGroup!=null)tag.putUUID("TargetGroup",targetGroup);
        if(targetPlayer!=null)tag.putUUID("TargetPlayer",targetPlayer);tag.putInt("Timer",timer);tag.putInt("Age",age);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;targetGroup=tag.hasUUID("TargetGroup")?tag.getUUID("TargetGroup"):null;
        targetPlayer=tag.hasUUID("TargetPlayer")?tag.getUUID("TargetPlayer"):null;
        timer=Mth.clamp(tag.getInt("Timer"),0,SOURCE_MAX_TIMER);age=Math.max(0,tag.getInt("Age"));setDeltaMovement(Vec3.ZERO);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
