package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Independent source Shard Hail carriers. Only the rising ball has the source spell tag. */
public final class SorcererHail extends ArmorStand {
    public enum Stage { BALL, NODE, ICICLE }
    private static final EntityDataAccessor<Integer> STAGE=SynchedEntityData.defineId(SorcererHail.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIMER=SynchedEntityData.defineId(SorcererHail.class,EntityDataSerializers.INT);
    private UUID owner;
    private long bornTick=Long.MIN_VALUE;

    public SorcererHail(EntityType<? extends ArmorStand> type,Level level){
        super(type,level);setInvisible(true);setInvulnerable(true);setNoBasePlate(true);setNoGravity(true);
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(STAGE,0);entityData.define(TIMER,0);}
    public Stage stage(){return Stage.values()[Math.floorMod(entityData.get(STAGE),3)];}
    public int timer(){return entityData.get(TIMER);}
    UUID ownerId(){return owner;}
    boolean counterable(){return stage()==Stage.BALL;}
    private LivingEntity attacker(ServerLevel level){return owner!=null&&level.getEntity(owner) instanceof LivingEntity living?living:null;}
    static SorcererHail spawn(ServerLevel level,UUID owner,Vec3 at,Stage stage,int timer){
        var hail=Content.SORCERER_HAIL.get().create(level);
        hail.owner=owner;hail.entityData.set(STAGE,stage.ordinal());hail.entityData.set(TIMER,timer);
        hail.setPos(at);hail.configureStage();hail.bornTick=level.getGameTime();level.addFreshEntity(hail);return hail;
    }
    static SorcererHail launchBall(ServerLevel level,NativeBoss boss){
        var ball=spawn(level,boss.getUUID(),EmissaryPose.localPoint(boss.position(),boss.getYRot(),boss.getXRot(),new Vec3(0,2,.6)),Stage.BALL,0);
        // Animated Java runs before sorcerer:tick, so the new ball gets timer=1 in its creation world tick.
        ball.attackTick(level);return ball;
    }
    private void configureStage(){
        for(String tag:new String[]{"native_hail_ball","native_hail_node","native_hail_icicle"})removeTag(tag);
        addTag(switch(stage()){case BALL->"native_hail_ball";case NODE->"native_hail_node";case ICICLE->"native_hail_icicle";});
        setNoGravity(stage()!=Stage.ICICLE);
        setItemSlot(EquipmentSlot.HEAD,stage()==Stage.ICICLE?new ItemStack(Content.SORCERER_ICICLE_VISUAL.get()):ItemStack.EMPTY);
    }
    @Override public void tick(){
        // Source functions inspect OnGround before this world's vanilla ArmorStand physics.
        if(level() instanceof ServerLevel server&&bornTick!=server.getGameTime())attackTick(server);
        if(!isRemoved())super.tick();
        if(getY()<level().getMinBuildHeight()-64)discard();
    }
    void attackTickForTest(){if(level() instanceof ServerLevel server)attackTick(server);}
    private void attackTick(ServerLevel server){
        if(isRemoved())return;
        switch(stage()){
            case BALL -> {
                entityData.set(TIMER,timer()+1);Vec3 entry=position();
                particle(server,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),entry,10,.1,0,true);
                particle(server,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.BLUE_ICE.defaultBlockState()),entry,10,.2,0,true);
                particle(server,new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(Content.SORCERER_VOID_PARTICLE_VISUAL.get())),entry,20,.2,0,true);
                setPos(entry.add(0,1,0));
                if(timer()>=30){
                    for(int i=0;i<18;i++)spawn(server,owner,position().add(0,-30,0),Stage.NODE,random.nextInt(40));
                    particle(server,ParticleTypes.SNOWFLAKE,position(),100,1,1,true);
                    particle(server,ParticleTypes.FIREWORK,position(),50,1,1,true);
                    particle(server,ParticleTypes.FLASH,position(),1,0,0,true);
                    particle(server,ParticleTypes.EXPLOSION_EMITTER,position(),1,0,0,true);discard();
                }
            }
            case NODE -> {
                Vec3 entry=position();entityData.set(TIMER,timer()+1);
                if(timer()==41){
                    // The selector is sorted but has no limit: all centers are fixed at command entry.
                    var players=server.getEntitiesOfClass(Player.class,new AABB(entry,entry).inflate(80),p->!p.isSpectator()&&p.position().distanceToSqr(entry)<=6400);
                    players.sort(Comparator.comparingDouble(p->p.position().distanceToSqr(entry)));
                    for(Player player:players){Vec3 landing=chooseLanding(server,player.position(),random);if(landing!=null)setPos(landing);}
                }
                if(timer()>=41)for(int yaw=0;yaw<360;yaw+=20){
                    particle(server,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),position().add(Vec3.directionFromRotation(0,yaw).scale(2)),1,0,0,false);
                    particle(server,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.SNOW.defaultBlockState()),position().add(Vec3.directionFromRotation(0,yaw+1).scale(2)),1,0,0,false);
                }
                // Unlike the nested `at @s` branches, this selector retains block/21's entry position after spreadplayers.
                for(Player player:server.getEntitiesOfClass(Player.class,new AABB(entry,entry).inflate(1.40001),p->p.position().distanceToSqr(entry)<=1.96+1e-7))
                    if(Combat.canHit(attacker(server),player))player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,1,false,false));
                if(timer()==41)spawn(server,owner,position().add(0,30,0),Stage.ICICLE,0);
                if(timer()>=90)discard();
            }
            case ICICLE -> {
                if(onGround())entityData.set(TIMER,timer()+1);
                if(timer()>=5)discard();
                particle(server,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),position(),2,.1,0,false);
                if(onGround()&&timer()==2){
                    Combat.playerFeetSphere(server,attacker(server),position(),2,10,Combat.Element.VOID);
                    // Hit Slowness II is intentionally pending the user's D28 cancellation policy.
                    NativeEvents.masterSound(server,position(),SoundEvents.GLASS_BREAK,2,15,.7f);
                }
            }
        }
    }
    /** Vanilla single-entity spreadplayers: square +/-15, block centers, highest safe two-air surface. */
    static Vec3 chooseLanding(ServerLevel server,Vec3 center,net.minecraft.util.RandomSource random){
        for(int attempt=0;attempt<10000;attempt++){
            int x=net.minecraft.util.Mth.floor(center.x-15+random.nextDouble()*30),z=net.minecraft.util.Mth.floor(center.z-15+random.nextDouble()*30);
            if(!server.hasChunkAt(new BlockPos(x,0,z)))continue;
            for(int y=server.getMaxBuildHeight()-2;y>=server.getMinBuildHeight();y--){
                var ground=new BlockPos(x,y,z);var state=server.getBlockState(ground);
                if(state.isAir()||!server.getBlockState(ground.above()).isAir()||!server.getBlockState(ground.above(2)).isAir())continue;
                if(!state.liquid()&&!state.is(net.minecraft.tags.BlockTags.FIRE))return new Vec3(x+.5,y+1,z+.5);
                break;
            }
        }
        return null;
    }
    private void particle(ServerLevel server,ParticleOptions particle,Vec3 at,int count,double spread,double speed,boolean force){
        if(!force){server.sendParticles(particle,at.x,at.y,at.z,count,spread,spread,spread,speed);return;}
        for(ServerPlayer player:server.players())server.sendParticles(player,particle,true,at.x,at.y,at.z,count,spread,spread,spread,speed);
    }
    @Override public boolean hurt(DamageSource source,float amount){return false;}
    @Override public net.minecraft.world.InteractionResult interactAt(Player player,Vec3 at,net.minecraft.world.InteractionHand hand){return net.minecraft.world.InteractionResult.PASS;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return stage()==Stage.ICICLE&&distance<=256*256;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("HailOwner",owner);tag.putInt("HailStage",stage().ordinal());tag.putInt("HailTimer",timer());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);owner=tag.hasUUID("HailOwner")?tag.getUUID("HailOwner"):null;entityData.set(STAGE,tag.getInt("HailStage"));entityData.set(TIMER,tag.getInt("HailTimer"));configureStage();bornTick=Long.MIN_VALUE;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
