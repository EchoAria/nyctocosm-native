package com.flubberschnub.nyctocosm;

import java.util.Comparator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Source root teleports, with a saved world destination and bounded safe landing. */
final class SorcererDash {
    private Vec3 destination;
    private float lockedYaw;
    private boolean finished,managedGravity,originalGravity;
    void begin(NativeBoss boss){finish(boss);destination=null;finished=false;originalGravity=boss.isNoGravity();managedGravity=true;hold(boss);}
    boolean active(){return managedGravity;}
    private void hold(NativeBoss boss){boss.setNoGravity(true);boss.getNavigation().stop();boss.setDeltaMovement(Vec3.ZERO);boss.fallDistance=0;if(destination!=null){boss.setYRot(lockedYaw);boss.setXRot(0);boss.yBodyRot=lockedYaw;}}
    void finish(NativeBoss boss){if(managedGravity)boss.setNoGravity(originalGravity);managedGravity=false;destination=null;}
    static double riseStep(int frame){return frame>=5&&frame<=12?.6:frame>=17&&frame<=19?.1:.3;}
    void rise(NativeBoss boss,int frame){hold(boss);boss.setPos(boss.position().add(0,riseStep(frame),0));}
    void aim(ServerLevel level,NativeBoss boss,boolean air){
        hold(boss);Vec3 origin=boss.position();
        Player nearest=level.getEntitiesOfClass(Player.class,new AABB(origin,origin).inflate(30),p->!p.isSpectator()&&p.position().distanceToSqr(origin)<=900)
            .stream().min(Comparator.comparingDouble(p->p.position().distanceToSqr(origin))).orElse(null);
        if(nearest==null)return;
        var facing=SorcererCastMath.facing(nearest.position().subtract(origin));boss.setYRot(facing.yaw());boss.setXRot(0);boss.yBodyRot=boss.getYRot();
        Vec3 requested=air?nearest.position():origin.add(SerpentMotion.localForward(boss.getYRot(),0).scale(15));
        destination=safeDestination(level,boss,requested);lockedYaw=boss.getYRot();finished=false;
    }
    private static Vec3 safeDestination(ServerLevel level,NativeBoss boss,Vec3 requested){
        // Preserve exact source X/Z where the surface is safe; only a blocked footprint needs a bounded search.
        BlockPos column=BlockPos.containing(requested);level.getChunkAt(column);
        double y=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column).getY();Vec3 exact=new Vec3(requested.x,y,requested.z);
        if(SafeTravel.canStand(level,boss,exact))return exact;
        return SafeTravel.findSurface(level,boss,requested,8).orElse(null);
    }
    Vec3 step(NativeBoss boss){
        hold(boss);Vec3 entry=boss.position();
        if(destination!=null&&!finished)boss.setPos(entry.add(SorcererCastMath.facing(destination.subtract(entry)).forward().scale(1.5)));
        return entry;
    }
    void arrive(ServerLevel level,NativeBoss boss){
        hold(boss);if(finished)return;finished=true;
        if(destination!=null){Vec3 safe=safeDestination(level,boss,destination);if(safe!=null)boss.setPos(safe);}
    }
    boolean finished(){return finished;}
    Vec3 destination(){return destination;}
    CompoundTag save(){CompoundTag tag=new CompoundTag();tag.putFloat("LockedYaw",lockedYaw);tag.putBoolean("ManagedGravity",managedGravity);tag.putBoolean("OriginalGravity",originalGravity);tag.putBoolean("Finished",finished);if(destination!=null){tag.putDouble("X",destination.x);tag.putDouble("Y",destination.y);tag.putDouble("Z",destination.z);}return tag;}
    void load(NativeBoss boss,CompoundTag tag){destination=tag.contains("X")?new Vec3(tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z")):null;lockedYaw=tag.getFloat("LockedYaw");finished=tag.getBoolean("Finished");managedGravity=tag.getBoolean("ManagedGravity");originalGravity=tag.getBoolean("OriginalGravity");if(managedGravity)hold(boss);}
}
