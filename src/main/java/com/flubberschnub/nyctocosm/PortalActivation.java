package com.flubberschnub.nyctocosm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A saved, server-owned opening sequence; particles are produced locally by each observer. */
public final class PortalActivation extends Entity {
    public static final int OPEN_TICK=122;
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(PortalActivation.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> X_AXIS=SynchedEntityData.defineId(PortalActivation.class,EntityDataSerializers.BOOLEAN);
    private BlockPos corner=BlockPos.ZERO;
    private int lastClientAge=-1;
    public PortalActivation(EntityType<? extends PortalActivation> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(AGE,0);entityData.define(X_AXIS,true);}
    public int age(){return entityData.get(AGE);}
    public Direction axis(){return entityData.get(X_AXIS)?Direction.EAST:Direction.SOUTH;}
    public boolean matches(BlockPos at,Direction direction){return corner.equals(at)&&axis()==direction;}
    public boolean nearSurface(Vec3 point,double radius){
        double start=axis()==Direction.EAST?corner.getX()+1:corner.getZ()+1;
        double along=axis()==Direction.EAST?point.x:point.z;
        double plane=axis()==Direction.EAST?point.z-(corner.getZ()+.5):point.x-(corner.getX()+.5);
        double horizontal=along<start?start-along:Math.max(0,along-(start+20));
        double vertical=point.y<corner.getY()+1?corner.getY()+1-point.y:Math.max(0,point.y-(corner.getY()+7));
        return plane*plane+horizontal*horizontal+vertical*vertical<=radius*radius;
    }
    public static PortalActivation start(ServerLevel level,BlockPos corner,Direction axis){
        var effect=new PortalActivation(Content.PORTAL_ACTIVATION.get(),level);effect.corner=corner.immutable();effect.entityData.set(X_AXIS,axis==Direction.EAST);
        Vec3 center=Vec3.atLowerCornerOf(corner).add(axis==Direction.EAST?11:.5,4,axis==Direction.SOUTH?11:.5);
        effect.setPos(center);level.addFreshEntity(effect);effect.sound(0,false);return effect;
    }
    @Override public void tick(){
        super.tick();
        if(level().isClientSide)return;
        var server=(ServerLevel)level();
        if(!NativePortalBlock.frame(server,corner,axis())){discard();return;}
        int next=age()+1;entityData.set(AGE,next);
        for(int wave=0;wave<3;wave++){
            if(next==wave*16&&wave>0)sound(wave,false);
            if(next==90+wave*16)sound(wave,true);
        }
        if(next==OPEN_TICK)NativePortalBlock.open(server,corner,axis());
        if(next>=OPEN_TICK+24)discard();
    }
    /** Runs even when an optimisation skips this invisible entity's tick. */
    public void clientVisualTick(){
        if(level().isClientSide){
            int age=age();
            if(age!=lastClientAge){
                int start=lastClientAge<0?age:Math.max(lastClientAge+1,age-20);
                for(int frame=start;frame<=age;frame++)ResonanceEffects.clientPortal(level(),position(),axis().getAxis(),frame,lastClientAge<0);
                lastClientAge=age;
            }
        }
    }
    private void sound(int wave,boolean collapse){
        if(level() instanceof ServerLevel server)NativeEvents.resonanceSound(server,position(),"portal.external_resonance."+(collapse?"collapse.":"")+new String[]{"first","second","third"}[wave],50,100);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putLong("Corner",corner.asLong());tag.putBoolean("XAxis",axis()==Direction.EAST);tag.putInt("Age",age());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){corner=BlockPos.of(tag.getLong("Corner"));entityData.set(X_AXIS,tag.getBoolean("XAxis"));entityData.set(AGE,Math.max(0,tag.getInt("Age")));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
