package com.flubberschnub.nyctocosm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

public final class SpellBolt extends Projectile {
    private static final EntityDataAccessor<Integer> ELEMENT=SynchedEntityData.defineId(SpellBolt.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EFFECT=SynchedEntityData.defineId(SpellBolt.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WEAPON=SynchedEntityData.defineId(SpellBolt.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> COLLISION_HELD=SynchedEntityData.defineId(SpellBolt.class,EntityDataSerializers.BOOLEAN);
    public enum Effect { DAMAGE, COUNTER, GRAVITY, LIGHTNING, FIRE, HOMING, METEOR, SEEKING_SINGULARITY }
    public enum FlightProfile { STANDARD, EMISSARY_FIREBALL, SINGULARITY_CHARGE, PLAYER_ABHORRENCE, PLAYER_FULMINANCE, PLAYER_GRAVITANCE }
    private enum FireballImpactReason { ENTITY, BLOCK, TIMEOUT }
    private float damage=10, radius;
    private int life=100;
    private int counterAge;
    private long lastReflectanceGameTime=Long.MIN_VALUE;
    private String impactSound="";
    private UUID homingTarget;
    private float homingTurn=.25f;
    private boolean ignoreBlocks;
    private SpellCollisionPolicy collisionPolicy=SpellCollisionPolicy.VANILLA_PHYSICAL;
    private FlightProfile flightProfile=FlightProfile.STANDARD;
    private int fireballTerminalImpactCount;
    private FireballImpactReason lastFireballImpactReason;
    private boolean singularityExploded;
    public SpellBolt(EntityType<? extends SpellBolt> type,Level level) { super(type,level); }
    public static SpellBolt launch(ServerLevel level,LivingEntity owner,Vec3 at,Vec3 direction,float damage,Combat.Element element,float speed,float radius) {
        SpellBolt bolt=new SpellBolt(Content.BOLT.get(),level);
        bolt.setOwner(owner); bolt.setPos(at); bolt.damage=damage; bolt.radius=radius;
        bolt.entityData.set(ELEMENT,element.ordinal()); bolt.setDeltaMovement(direction.normalize().scale(speed));
        level.addFreshEntity(bolt); return bolt;
    }
    public static SpellBolt launchMeteor(ServerLevel level,LivingEntity owner,Vec3 at,Vec3 direction,float damage) {
        SpellBolt bolt=new SpellBolt(Content.BOLT.get(),level);
        bolt.setOwner(owner);bolt.setPos(at);bolt.damage=damage;bolt.radius=5;
        bolt.entityData.set(ELEMENT,Combat.Element.VOID.ordinal());bolt.setDeltaMovement(direction.normalize().scale(1.5));
        bolt.setNoGravity(true);
        bolt.effect(Effect.METEOR,25).weaponVisual(6).impactSound("sorcerer.voidmeteor.impact");
        level.addFreshEntity(bolt);
        return bolt;
    }
    public Combat.Element element() { return Combat.Element.values()[Math.floorMod(entityData.get(ELEMENT),6)]; }
    public float spellDamage(){return damage;}
    SpellCollisionPolicy collisionPolicyForTest(){return collisionPolicy;}
    int remainingLife(){return life;}
    FlightProfile flightProfileForTest(){return flightProfile;}
    boolean handlesOwnReflection(){return flightProfile==FlightProfile.SINGULARITY_CHARGE||flightProfile==FlightProfile.PLAYER_ABHORRENCE||flightProfile==FlightProfile.PLAYER_FULMINANCE||flightProfile==FlightProfile.PLAYER_GRAVITANCE;}
    int fireballTerminalImpactCountForTest(){return fireballTerminalImpactCount;}
    String lastFireballImpactReasonForTest(){return lastFireballImpactReason==null?"":lastFireballImpactReason.name();}
    boolean reflectedByReflectanceThisTick(){return lastReflectanceGameTime==level().getGameTime();}
    public void reflectFrom(LivingEntity newOwner,Vec3 reflectedDirection){
        double speed=getDeltaMovement().length();Effect previous=effect();setOwner(newOwner);homingTarget=null;
        lastReflectanceGameTime=level().getGameTime();
        if(previous==Effect.HOMING||previous==Effect.SEEKING_SINGULARITY){entityData.set(EFFECT,Effect.DAMAGE.ordinal());if(previous==Effect.SEEKING_SINGULARITY)radius=0;}
        Vec3 direction=reflectedDirection.normalize();
        setDeltaMovement(direction.scale(speed));
        setRotationFromDirection(direction);
        hurtMarked=true;
    }
    public SpellBolt effect(Effect effect,int lifetime){entityData.set(EFFECT,effect.ordinal());life=lifetime;return this;}
    public SpellBolt playerAbhorrenceFlight(){flightProfile=FlightProfile.PLAYER_ABHORRENCE;radius=0;setNoGravity(true);setRotationFromDirection(getDeltaMovement());return this;}
    public SpellBolt playerFulminanceFlight(){flightProfile=FlightProfile.PLAYER_FULMINANCE;radius=0;setNoGravity(true);setRotationFromDirection(getDeltaMovement());return this;}
    public SpellBolt playerGravitanceFlight(){flightProfile=FlightProfile.PLAYER_GRAVITANCE;radius=0;setNoGravity(true);setRotationFromDirection(getDeltaMovement());return this;}
    public SpellBolt emissaryFireballFlight(){flightProfile=FlightProfile.EMISSARY_FIREBALL;life=60;setDeltaMovement(getDeltaMovement().normalize().scale(2));return this;}
    public SpellBolt impactSound(String sound){impactSound=sound;return this;}
    public SpellBolt homeOn(LivingEntity target){homingTarget=target==null?null:target.getUUID();return this;}
    public SpellBolt seekingSingularity(int lifetime){
        entityData.set(EFFECT,Effect.SEEKING_SINGULARITY.ordinal());life=lifetime;radius=0;ignoreBlocks=true;collisionPolicy=SpellCollisionPolicy.IGNORE_BLOCKS;
        setRotationFromDirection(getDeltaMovement().normalize());return this;
    }
    public SpellBolt collisionPolicy(SpellCollisionPolicy policy){
        collisionPolicy=java.util.Objects.requireNonNull(policy);
        ignoreBlocks=policy==SpellCollisionPolicy.IGNORE_BLOCKS;
        return this;
    }
    public SpellBolt homingTurn(float turn){homingTurn=Math.max(0,Math.min(1,turn));return this;}
    public Effect effect(){return Effect.values()[Math.floorMod(entityData.get(EFFECT),Effect.values().length)];}
    public SpellBolt weaponVisual(int kind){
        entityData.set(WEAPON,kind);
        if(kind==4){flightProfile=FlightProfile.SINGULARITY_CHARGE;collisionPolicy=SpellCollisionPolicy.SOURCE_PASSTHROUGH;setNoGravity(true);setRotationFromDirection(getDeltaMovement());}
        return this;
    }
    @Override protected void defineSynchedData() { entityData.define(ELEMENT,0);entityData.define(EFFECT,0);entityData.define(WEAPON,0);entityData.define(COLLISION_HELD,false); }
    @Override public void tick() {
        Vec3 tickOrigin=position();boolean sourceFireball=flightProfile==FlightProfile.EMISSARY_FIREBALL&&!level().isClientSide;
        Vec3 sourceMovement=sourceFireball?getDeltaMovement():null;
        if(sourceFireball)setDeltaMovement(Vec3.ZERO);
        super.tick();
        if(sourceFireball){setPos(tickOrigin);setDeltaMovement(sourceMovement);if(sourceMovement.lengthSqr()>1.0E-12)setRotationFromDirection(sourceMovement.normalize());}
        Vec3 movement=level().isClientSide&&entityData.get(COLLISION_HELD)?Vec3.ZERO:getDeltaMovement();
        if (!level().isClientSide) {
            if (!level().hasChunkAt(blockPosition())){discard();return;}
            if(effect()==Effect.COUNTER){tickCounter((ServerLevel)level());return;}
            if(effect()==Effect.METEOR){tickMeteor((ServerLevel)level());return;}
            if(effect()==Effect.SEEKING_SINGULARITY){tickSeekingSingularity((ServerLevel)level());return;}
            if(flightProfile==FlightProfile.SINGULARITY_CHARGE){tickSingularityCharge((ServerLevel)level());return;}
            if(flightProfile==FlightProfile.PLAYER_ABHORRENCE){tickPlayerAbhorrence((ServerLevel)level());return;}
            if(flightProfile==FlightProfile.PLAYER_FULMINANCE){tickPlayerFulminance((ServerLevel)level());return;}
            if(flightProfile==FlightProfile.PLAYER_GRAVITANCE){tickPlayerGravitance((ServerLevel)level());return;}
            if(--life<=0){
                if(flightProfile==FlightProfile.EMISSARY_FIREBALL){fireballImpact((ServerLevel)level(),new BlockHitResult(position(),Direction.UP,blockPosition(),false),FireballImpactReason.TIMEOUT);return;}
                if(effect()==Effect.GRAVITY)((ServerLevel)level()).sendParticles(ParticleTypes.PORTAL,getX(),getY(),getZ(),2000,0,0,0,10);
                discard();return;
            }
            if(effect()==Effect.GRAVITY){
                for(LivingEntity target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(9)))if(Combat.canHit(livingOwner(),target)&&!SpellArea.preventsForcedMovement(target)){
                    Vec3 pull=position().subtract(target.getBoundingBox().getCenter());
                    double distance=Combat.distanceToBoxSqr(position(),target.getBoundingBox());
                    if(distance>81)continue;
                    double speed=distance<=36?.4:.2;
                    target.setDeltaMovement(target.getDeltaMovement().scale(.5).add(pull.normalize().scale(speed*.5)));target.hurtMarked=true;
                    // Original block/77 damages the inner 3-block region throughout the
                    // pull, after HurtTime ends. Never bypass another mod's damage events.
                    if(distance<=9&&target.hurtTime==0)Combat.hit(livingOwner(),target,damage,element());
                }
            }
            if(effect()==Effect.HOMING){
                LivingEntity nearest=null;double nearestDistance=40*40;
                LivingEntity owner=livingOwner();
                if(homingTarget!=null&&((ServerLevel)level()).getEntity(homingTarget) instanceof LivingEntity locked&&Combat.canHit(owner,locked))nearest=locked;
                if(nearest==null&&owner instanceof Mob mob&&Combat.canHit(owner,mob.getTarget())){nearest=mob.getTarget();nearestDistance=Combat.distanceToBoxSqr(position(),nearest.getBoundingBox());}
                if(nearest==null)for(LivingEntity candidate:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(40)))if(Combat.canHit(owner,candidate)){
                    double distance=Combat.distanceToBoxSqr(position(),candidate.getBoundingBox());if(distance<nearestDistance){nearest=candidate;nearestDistance=distance;}
                }
                if(nearest!=null){
                    double speed=Math.max(.2,getDeltaMovement().length());
                    Vec3 desired=nearest.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed);
                    setDeltaMovement(getDeltaMovement().scale(1-homingTurn).add(desired.scale(homingTurn)).normalize().scale(speed));
                    movement=getDeltaMovement();
                }
            }
            if(flightProfile==FlightProfile.EMISSARY_FIREBALL){
                Vec3 direction=movement.lengthSqr()<1.0E-12?getDeltaMovement().normalize():movement.normalize();
                for(int substep=0;substep<2;substep++){
                    Vec3 step=direction;
                    SpellCollisionPolicy.HitResolution collision=collisionPolicy.resolve(this,step);
                    if(collision.unloadedBoundary()){discard();return;}
                    HitResult result=collision.hit();
                    if(result!=null&&result.getType()!=HitResult.Type.MISS&&!net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,result)){
                        setPos(result.getLocation());
                        FireballImpactReason reason=result instanceof EntityHitResult?FireballImpactReason.ENTITY:FireballImpactReason.BLOCK;
                        fireballImpact((ServerLevel)level(),result,reason);return;
                    }
                    setPos(position().add(step));
                }
                setDeltaMovement(direction.scale(2));hurtMarked=true;entityData.set(COLLISION_HELD,false);
            }else{
                SpellCollisionPolicy.HitResolution collision=collisionPolicy.resolve(this,movement);
                if(collision.unloadedBoundary()){discard();return;}
                if(collision.holdPosition()){movement=Vec3.ZERO;entityData.set(COLLISION_HELD,true);hurtMarked=true;}
                else entityData.set(COLLISION_HELD,false);
                HitResult result=collision.hit();
                if (result!=null && result.getType()!=HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,result)) {
                    setPos(result.getLocation());
                    if(effect()==Effect.GRAVITY){setDeltaMovement(Vec3.ZERO);return;}
                    onHit(result); return;
                }
            }
        } else {
            if(effect()==Effect.GRAVITY){
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),2),getX(),getY(),getZ(),0,0,0);
                double angle=tickCount*.6;
                for(int n=0;n<8;n++){double a=angle+n*Math.PI/4;level().addParticle(ParticleTypes.PORTAL,getX()+Math.cos(a)*1.5,getY(),getZ()+Math.sin(a)*1.5,-Math.cos(a)*.3,0,-Math.sin(a)*.3);}
            }else if(entityData.get(WEAPON)==7){
                trailParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,.651f,1),1),position(),3,.3,0);
                trailParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.741f,0,1),1),position(),3,.3,0);
                trailParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),2),position(),3,.3,0);
            }else if(entityData.get(WEAPON)==1){
                Vec3 side=new Vec3(movement.z,0,-movement.x).normalize();
                for(int n=-8;n<=8;n++){
                    Vec3 p=position().add(side.scale(n*.15));
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1,0,0),.75f),p.x,p.y,p.z,0,0,0);
                    Vec3 behind=p.subtract(movement.normalize());
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),.75f),behind.x,behind.y,behind.z,0,0,0);
                }
            }else if(entityData.get(WEAPON)==3){
                for(int sample=0;sample<3;sample++){
                    Vec3 center=position().add(movement.scale(sample/3.0));
                    trailParticle(ParticleTypes.FLAME,center,5,.4,.02);
                    trailParticle(ParticleTypes.END_ROD,center,2,.2,.01);
                    trailParticle(ParticleTypes.SMOKE,center,5,.4,.02);
                    trailParticle(ParticleTypes.LAVA,center,2,.4,0);
                }
            }else if(entityData.get(WEAPON)==4){
                VoidSwirlEffects.clientSingle(level(),position(),getUUID().getLeastSignificantBits()^level().getGameTime(),true,false);
                trailParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),3),position(),5,.3,0);
            }else if(entityData.get(WEAPON)==5){
                for(int n=0;n<3;n++){
                    Vec3 p=position().add(movement.scale(n/3.0));
                    level().addParticle(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),p.x,p.y,p.z,0,0,0);
                    level().addParticle(ParticleTypes.SNOWFLAKE,p.x,p.y,p.z,0,.04,0);
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK,p.x,p.y,p.z,0,0,0);
                }
            }else if(entityData.get(WEAPON)==6){
                trailParticle(ParticleTypes.SMOKE,position(),5,.35,.01);
                trailParticle(ParticleTypes.FLAME,position(),3,.3,.01);
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.741f,0,1),1.8f),getX(),getY(),getZ(),0,0,0);
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0,0,0),1.8f),getX(),getY(),getZ(),0,0,0);
            }else for(int n=0;n<3;n++) {
                Vec3 p=position().add(movement.scale(n/3.0));
                level().addParticle(element()==Combat.Element.DISSONANCE?ParticleTypes.FLAME:ParticleTypes.END_ROD,p.x,p.y,p.z,0,0,0);
            }
        }
        if(flightProfile==FlightProfile.EMISSARY_FIREBALL){if(level().isClientSide)setPos(position().add(movement));}
        else setPos(position().add(movement));
        updateRotation();
    }
    private void tickCounter(ServerLevel level){
        // Source block/61 moves through blocks and only starts dispelling spells on its third tick.
        setPos(position().add(getDeltaMovement()));updateRotation();counterAge++;
        boolean portal=NativePortalBlock.counter(level,blockPosition());
        for(PortalActivation activation:level.getEntitiesOfClass(PortalActivation.class,getBoundingBox().inflate(25),effect->effect.nearSurface(position(),3))){activation.discard();portal=true;}
        if(portal){discard();return;}
        boolean countered=false;UUID caster=getOwner()==null?null:getOwner().getUUID();
        if(counterAge>=3)for(Entity other:level.getEntities(this,getBoundingBox().inflate(9),entity->entity.distanceToSqr(position())<=(entity instanceof EmissaryHazard?81:25))){
            UUID otherOwner;
            if(other instanceof SpellBolt bolt)otherOwner=bolt.getOwner()==null?null:bolt.getOwner().getUUID();
            else if(other instanceof SpellArea area)otherOwner=area.ownerId();
            else if(other instanceof SorcererHail hail&&hail.counterable()&&hail.distanceToSqr(position())>=.0001)otherOwner=hail.ownerId();
            else if(other instanceof FrozenBlade blade)otherOwner=blade.damageOwnerId();
            else if(other instanceof EmissarySeekingMissile missile)otherOwner=missile.damageOwnerId();
            else if(other instanceof SorcererBeamNode node&&node.counterable())otherOwner=node.damageOwnerId();
            else if(other instanceof SeelicField field&&!field.isHypertrace())otherOwner=field.ownerId();
            else if(other instanceof EmissaryHazard hazard&&hazard.counterable())otherOwner=hazard.ownerId();
            else continue;
            if(caster!=null&&caster.equals(otherOwner))continue;
            if(other instanceof EmissaryHazard hazard){if(!hazard.counterAt(position()))continue;}
            else if(other instanceof SeelicField field)field.cancelCast();else other.discard();
            countered=true;
            level.sendParticles(ParticleTypes.CRIT,other.getX(),other.getY(),other.getZ(),50,.5,.5,.5,1);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,other.getX(),other.getY(),other.getZ(),30,.2,.2,.2,1);
            NativeEvents.sound(level,other.position(),"spells.impact.forbiddance",5);
        }
        if(countered||--life<=0)discard();
    }
    static Vec3 singularityChargeHeading(Vec3 forward,Vec3 toward){return forward.normalize().scale(5).add(toward.normalize().scale(.5)).normalize();}
    private void tickSingularityCharge(ServerLevel server){
        if(singularityExploded||isRemoved())return;
        // The source function's CommandSourceStack keeps its entry position after tp @s.
        Vec3 functionPosition=position();
        life--;
        Vec3 forward=Vec3.directionFromRotation(getXRot(),getYRot());Vec3 next=position().add(forward.scale(.3));
        if(!server.hasChunkAt(BlockPos.containing(next))){discard();return;}
        setPos(next);
        Player nearest=null;double nearestDistance=40*40;
        for(Player candidate:server.getEntitiesOfClass(Player.class,new AABB(next,next).inflate(40),player->!player.isSpectator())){
            double distance=candidate.position().distanceToSqr(next);
            if(distance<=nearestDistance){nearest=candidate;nearestDistance=distance;}
        }
        Vec3 heading=nearest==null?forward:singularityChargeHeading(forward,nearest.position().subtract(next));
        setRotationFromDirection(heading);setDeltaMovement(heading.scale(.3));hurtMarked=true;
        NativeEvents.resonanceSound(server,functionPosition,"sorcerer.singularitycharge.hum",1,30);
        BlockPos sourceBlock=BlockPos.containing(functionPosition);
        boolean blocked=!server.getBlockState(sourceBlock).is(net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,new net.minecraft.resources.ResourceLocation(NativeMod.ID,"passthrough")));
        if(blocked&&net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,new BlockHitResult(functionPosition,Direction.UP,sourceBlock,false)))blocked=false;
        boolean nearPlayer=server.getEntitiesOfClass(Player.class,new AABB(functionPosition,functionPosition).inflate(3),
            player->!player.isSpectator()&&player.position().distanceToSqr(functionPosition)<=9).size()>0;
        if(life<=0||blocked||nearPlayer){explodeSingularityCharge(server,functionPosition);return;}
        // Source reflection follows the proximity/timeout checks and adds one redirected 0.3-block move.
        var reflection=Reflectance.tryReflect(server,new ReflectableAttack(functionPosition,getOwner()==null?null:getOwner().getUUID(),damage));
        if(reflection!=null){reflectFrom(reflection.owner(),reflection.direction());setPos(functionPosition.add(getDeltaMovement().normalize().scale(.3)));}
    }
    private void explodeSingularityCharge(ServerLevel server,Vec3 at){
        if(singularityExploded)return;singularityExploded=true;
        LivingEntity owner=livingOwner();
        for(Player victim:server.getEntitiesOfClass(Player.class,new AABB(at,at).inflate(5)))
            if(victim.position().distanceToSqr(at)<=25&&Combat.canHit(owner,victim))Combat.hit(owner,victim,damage,element());
        for(var player:server.players())server.sendParticles(player,ParticleTypes.EXPLOSION_EMITTER,true,at.x,at.y,at.z,1,0,0,0,0);
        NativeEvents.resonanceSound(server,at,"sorcerer.singularitycharge.explode",5,40);discard();
    }
    private void tickMeteor(ServerLevel server){
        if(life<=0){meteorImpact(server);return;}
        var passthrough=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
            new net.minecraft.resources.ResourceLocation(NativeMod.ID,"passthrough"));
        Vec3 direction=getDeltaMovement().normalize();
        for(int step=0;step<3;step++){
            Player nearest=null;double nearestDistance=50*50;
            AABB targetSearch=new AABB(position(),position()).inflate(50);
            for(Player player:server.getEntitiesOfClass(Player.class,targetSearch,entity->!entity.isSpectator())){
                double distance=player.distanceToSqr(this);
                if(distance<=nearestDistance){nearest=player;nearestDistance=distance;}
            }
            if(nearest!=null){
                Vec3 toward=nearest.position().subtract(position());
                if(toward.lengthSqr()>.0001)direction=toward.normalize();
            }
            Vec3 next=position().add(direction.scale(.5));
            setPos(next);
            BlockPos blockPos=BlockPos.containing(next);
            BlockState state=server.getBlockState(blockPos);
            if(!state.is(passthrough)){meteorImpact(server);return;}
        }
        setDeltaMovement(direction.scale(1.5));hurtMarked=true;updateRotation();
        if(--life<=0)meteorImpact(server);
    }
    private void tickSeekingSingularity(ServerLevel server){
        life--;
        Vec3 forward=Vec3.directionFromRotation(getXRot(),getYRot()).normalize();
        Player nearest=nearestSeekingPlayer(server);
        Vec3 direction=forward;
        if(nearest!=null){
            Vec3 towardEyes=nearest.getEyePosition().subtract(position());
            if(towardEyes.lengthSqr()>.0001)direction=seekingTurnDirection(forward,towardEyes);
        }
        setRotationFromDirection(direction);
        Vec3 movement=direction.scale(.24),next=position().add(movement);
        if(!server.hasChunkAt(BlockPos.containing(next))){discard();return;}
        setDeltaMovement(movement);setPos(next);hurtMarked=true;
        NativeEvents.resonanceSound(server,position(),"spells.hum.gravitance",.2f,30);

        // The source performs two overlapping dx=0 player-box checks after movement.
        // Their common region is this half-block cube; blocks are intentionally ignored.
        Vec3 at=position();AABB sourceHitBox=new AABB(at.x-.25,at.y-.25,at.z-.25,at.x+.25,at.y+.25,at.z+.25);
        LivingEntity owner=livingOwner();boolean hit=false;
        for(Player player:server.getEntitiesOfClass(Player.class,sourceHitBox,candidate->!candidate.isSpectator()&&candidate.isAlive())){
            if(!player.getBoundingBox().intersects(sourceHitBox))continue;
            hit=true;
            if(Combat.canHit(owner,player))Combat.hit(owner,player,damage,element());
            // Source hit.mcfunction applies Slowness after the damage function without
            // checking whether Forge or target invulnerability accepted the damage.
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,100,1,false,false));
        }
        if(hit||life<=0)finishSeekingSingularity(server);
    }
    private Player nearestSeekingPlayer(ServerLevel server){
        AABB search=new AABB(position(),position()).inflate(50);Player nearest=null;double nearestDistance=50*50;
        for(Player candidate:server.getEntitiesOfClass(Player.class,search,player->!player.isSpectator()&&player.isAlive())){
            double distance=candidate.distanceToSqr(position());
            if(nearest==null||distance<nearestDistance){nearest=candidate;nearestDistance=distance;}
        }
        return nearest;
    }
    static Vec3 seekingTurnDirection(Vec3 currentForward,Vec3 towardTarget){
        Vec3 forward=currentForward.normalize(),toward=towardTarget.normalize();
        return forward.scale(6).add(toward.scale(.5)).normalize();
    }
    private void setRotationFromDirection(Vec3 direction){
        Vec3 unit=direction.normalize();float yaw=(float)Math.toDegrees(Math.atan2(-unit.x,unit.z));
        float pitch=(float)-Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,unit.y))));
        setYRot(yaw);yRotO=yaw;setXRot(pitch);xRotO=pitch;
    }
    private void finishSeekingSingularity(ServerLevel server){
        server.sendParticles(ParticleTypes.FLASH,getX(),getY(),getZ(),1,0,0,0,0);
        if(!impactSound.isBlank())NativeEvents.sound(server,position(),impactSound,5);
        discard();
    }
    private void meteorImpact(ServerLevel server){
        Vec3 impact=position();
        LivingEntity owner=livingOwner();
        AABB blast=new AABB(impact,impact).inflate(5);
        for(Player player:server.getEntitiesOfClass(Player.class,blast,entity->!entity.isSpectator()&&entity.distanceToSqr(impact)<=25))
            Combat.hit(owner,player,damage,element());
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,impact.x,impact.y,impact.z,1,0,0,0,0);
        server.sendParticles(ParticleTypes.SMOKE,impact.x,impact.y,impact.z,35,1,1,1,.04);
        if(!impactSound.isBlank())NativeEvents.sound(server,impact,impactSound,5);
        discard();
    }
    private void trailParticle(net.minecraft.core.particles.ParticleOptions particle,Vec3 center,int count,double spread,double speed){
        for(int n=0;n<count;n++)level().addParticle(particle,
            center.x+(random.nextDouble()*2-1)*spread,center.y+(random.nextDouble()*2-1)*spread,center.z+(random.nextDouble()*2-1)*spread,
            random.nextGaussian()*speed,random.nextGaussian()*speed,random.nextGaussian()*speed);
    }
    @Override protected boolean canHitEntity(Entity entity) {
        LivingEntity living=Combat.damageTarget(entity);
        if(flightProfile==FlightProfile.EMISSARY_FIREBALL
                &&(!(living instanceof Player)||living.isSpectator()||living.getTags().contains("emissarydebug")))return false;
        return effect()!=Effect.COUNTER&&effect()!=Effect.GRAVITY&&super.canHitEntity(entity) && living!=null
            &&(!(ignoreBlocks||collisionPolicy==SpellCollisionPolicy.IGNORE_BLOCKS)||living instanceof Player)
            &&(effect()!=Effect.HOMING||homingTarget==null||living.getUUID().equals(homingTarget))&&Combat.canHit(livingOwner(),living);
    }
    private LivingEntity livingOwner(){return getOwner() instanceof LivingEntity owner?owner:null;}
    @Override protected void onHit(HitResult result) {
        if (!(level() instanceof ServerLevel server)) return;
        if(flightProfile==FlightProfile.EMISSARY_FIREBALL){
            fireballImpact(server,result,result instanceof EntityHitResult?FireballImpactReason.ENTITY:FireballImpactReason.BLOCK);return;
        }
        resolveImpact(server,result);
    }
    private void fireballImpact(ServerLevel server,HitResult result,FireballImpactReason reason){
        // Keep the terminal cause explicit so source fire placement can later be
        // handled under the selected world-protection policy.
        if(fireballTerminalImpactCount>0){discard();return;}
        fireballTerminalImpactCount++;lastFireballImpactReason=reason;
        if(reason==FireballImpactReason.TIMEOUT){setPos(result.getLocation());}
        Vec3 impact=position();LivingEntity owner=livingOwner();
        // Source impact selects players by feet distance. Entity fire and extra push
        // belong to generic FIRE bolts, not this attack; world fire awaits C03/D19.
        for(Player player:server.getEntitiesOfClass(Player.class,new AABB(impact,impact).inflate(5+1.0e-4)))
            if(!player.isSpectator()&&!player.getTags().contains("emissarydebug")
                    &&player.position().distanceToSqr(impact)<=25&&Combat.canHit(owner,player))
                Combat.hit(owner,player,damage,element());
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,impact.x,impact.y,impact.z,1,0,0,0,0);
        server.playSound(null,impact.x,impact.y,impact.z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
            net.minecraft.sounds.SoundSource.MASTER,10,1);
        discard();
    }
    private void tickPlayerAbhorrence(ServerLevel server){
        --life;Vec3 direction=getDeltaMovement().normalize();Vec3 next=position().add(direction.scale(1.5));
        if(!server.hasChunkAt(BlockPos.containing(next))){discard();return;}setPos(next);
        boolean blocked=!SpellCollisionPolicy.isSourcePassthrough(server.getBlockState(blockPosition()))
            &&!SpellCollisionPolicy.isSourcePassthrough(server.getBlockState(BlockPos.containing(position().subtract(direction))));
        if(blocked){var hit=new BlockHitResult(position(),Direction.UP,blockPosition(),false);if(!net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,hit)){discard();return;}}
        var reflection=Reflectance.tryReflect(server,new ReflectableAttack(position(),getOwner()==null?null:getOwner().getUUID(),damage));
        if(reflection!=null)reflectFrom(reflection.owner(),reflection.direction());
        var hit=playerEndpointVictims(server);
        for(var living:hit)Combat.hit(livingOwner(),living,damage,element());
        if(!hit.isEmpty()){NativeEvents.resonanceSound(server,position(),"spells.impact.abhorrence",5,10);discard();}
        else if(life<=0)discard();hurtMarked=true;
    }
    private void tickPlayerFulminance(ServerLevel server){
        --life;boolean hit=false;
        for(int substep=0;substep<6&&!hit;substep++){
            Vec3 direction=getDeltaMovement().normalize(),next=position().add(direction.scale(1.5));
            if(!server.hasChunkAt(BlockPos.containing(next))){discard();return;}setPos(next);
            if(!SpellCollisionPolicy.isSourcePassthrough(server.getBlockState(blockPosition()))
                    &&!SpellCollisionPolicy.isSourcePassthrough(server.getBlockState(BlockPos.containing(position().subtract(direction))))){
                hit=!net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,new BlockHitResult(position(),Direction.UP,blockPosition(),false));
            }
            var reflection=Reflectance.tryReflect(server,new ReflectableAttack(position(),getOwner()==null?null:getOwner().getUUID(),damage));
            if(reflection!=null)reflectFrom(reflection.owner(),reflection.direction());
            var victims=playerEndpointVictims(server);
            for(var victim:victims){Combat.hit(livingOwner(),victim,damage,Combat.Element.DISSONANCE);NativeEvents.resonanceSound(server,victim.position(),"spells.impact.fulminance",5,10);}
            hit|=!victims.isEmpty();
        }
        if(hit){
            // World fire/lightning damage remains under the unresolved C03 policy.
            var lightning=EntityType.LIGHTNING_BOLT.create(server);lightning.setPos(position());lightning.setVisualOnly(true);server.addFreshEntity(lightning);discard();
        }else if(life<=0)discard();hurtMarked=true;
    }
    private java.util.Set<LivingEntity> playerEndpointVictims(ServerLevel server){
        var box=new AABB(position().add(-.5,-.5,-.5),position().add(.5,.5,.5));
        var candidates=new java.util.LinkedHashSet<Entity>(server.getEntities(this,box));
        for(var entity:server.getAllEntities())if(entity instanceof NativeBoss boss&&boss.isMultipartEntity()&&boss.intersectsDamageParts(box))
            for(var part:boss.getParts())if(part.isPickable()&&box.intersects(part.getBoundingBox()))candidates.add(part);
        var victims=new java.util.LinkedHashSet<LivingEntity>();
        for(Entity candidate:candidates){
            if(candidate instanceof NativeBoss boss&&boss.isMultipartEntity())continue;
            var living=Combat.damageTarget(candidate);
            if(living!=null&&Combat.canHit(livingOwner(),living)&&box.intersects(candidate.getBoundingBox())
                    &&!net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,new EntityHitResult(candidate,position())))victims.add(living);
        }
        return victims;
    }
    private void tickPlayerGravitance(ServerLevel server){
        --life;
        if(SpellCollisionPolicy.isSourcePassthrough(server.getBlockState(blockPosition()))){
            Vec3 next=position().add(getDeltaMovement());if(!server.hasChunkAt(BlockPos.containing(next))){discard();return;}setPos(next);
        }
        var reflection=Reflectance.tryReflect(server,new ReflectableAttack(position(),getOwner()==null?null:getOwner().getUUID(),damage));
        if(reflection!=null)reflectFrom(reflection.owner(),reflection.direction());
        if((tickCount&1)==0)NativeEvents.resonanceSound(server,position(),"spells.hum.gravitance",2,30);
        if(life<=0){server.sendParticles(ParticleTypes.PORTAL,getX(),getY(),getZ(),2000,0,0,0,10);discard();return;}
        for(var target:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(20))){
            if(!Combat.canHit(livingOwner(),target)||target instanceof NativeBoss||SpellArea.preventsForcedMovement(target))continue;
            var closest=server.getEntitiesOfClass(SpellBolt.class,target.getBoundingBox().inflate(9),bolt->bolt.effect()==Effect.GRAVITY&&!bolt.isRemoved()&&bolt.position().distanceToSqr(target.position())<=81)
                .stream().min(java.util.Comparator.comparingDouble(bolt->bolt.position().distanceToSqr(target.position()))).orElse(null);
            if(closest==null)continue;
            double distance=target.position().distanceToSqr(closest.position());
            if(distance>=36&&distance<=81)sourceGravityStep(target,closest.position(),.2);
            if(target.position().distanceToSqr(closest.position())<=36)sourceGravityStep(target,closest.position(),.4);
            if(target.hurtTime==0&&target.position().distanceToSqr(closest.position())<=9)Combat.hit(livingOwner(),target,damage,element());
        }
        hurtMarked=true;
    }
    private static void sourceGravityStep(LivingEntity target,Vec3 point,double step){
        Vec3 next=target.position().add(SorcererCastMath.facing(point.subtract(target.position())).forward().scale(step));
        if(!target.level().hasChunkAt(BlockPos.containing(next)))return;
        target.teleportTo(next.x,next.y,next.z);target.hurtMarked=true;
    }
    private void resolveImpact(ServerLevel server,HitResult result){
        LivingEntity owner=getOwner() instanceof LivingEntity living?living:null;
        LivingEntity directTarget=null;
        if(radius>0) Combat.area(server,owner,position(),radius,damage,element(),.5);
        // The direct target can lie outside the splash sphere (e.g. a large modded mob).
        if(result instanceof EntityHitResult hit){
            directTarget=Combat.damageTarget(hit.getEntity());
            if(directTarget!=null&&(radius<=0||Combat.distanceToDamageBoxSqr(position(),directTarget)>radius*radius))Combat.hit(owner,directTarget,damage,element());
        }
        if(effect()==Effect.FIRE){
            java.util.Set<LivingEntity> fireTargets=new java.util.LinkedHashSet<>(server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(Math.max(radius,1))));
            if(directTarget!=null)fireTargets.add(directTarget);
            for(LivingEntity target:fireTargets)if(Combat.canHit(owner,target))target.setSecondsOnFire(5);
        }
        if(effect()==Effect.LIGHTNING)server.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY(),getZ(),70,1,3,1,.2);
        if(entityData.get(WEAPON)==5)server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),getX(),getY(),getZ(),30,.4,.4,.4,.1);
        if(entityData.get(WEAPON)==6){server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY(),getZ(),1,0,0,0,0);server.sendParticles(ParticleTypes.SMOKE,getX(),getY(),getZ(),35,1,1,1,.04);}
        server.sendParticles(ParticleTypes.FLASH,getX(),getY(),getZ(),1,0,0,0,0);
        if(!impactSound.isBlank())NativeEvents.sound(server,position(),impactSound,5);
        discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putFloat("Damage",damage);tag.putFloat("Radius",radius);tag.putInt("Life",life);tag.putInt("CounterAge",counterAge);tag.putLong("ReflectanceGameTime",lastReflectanceGameTime);tag.putInt("Element",entityData.get(ELEMENT));tag.putInt("Effect",entityData.get(EFFECT));tag.putInt("WeaponVisual",entityData.get(WEAPON));tag.putString("ImpactSound",impactSound);tag.putFloat("HomingTurn",homingTurn);tag.putBoolean("IgnoreBlocks",ignoreBlocks);tag.putString("CollisionPolicy",collisionPolicy.name());tag.putString("FlightProfile",flightProfile.name());tag.putBoolean("SingularityExploded",singularityExploded);if(homingTarget!=null)tag.putUUID("HomingTarget",homingTarget); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);damage=tag.getFloat("Damage");radius=tag.getFloat("Radius");life=tag.getInt("Life");counterAge=Math.max(0,tag.getInt("CounterAge"));lastReflectanceGameTime=tag.contains("ReflectanceGameTime")?tag.getLong("ReflectanceGameTime"):Long.MIN_VALUE;entityData.set(ELEMENT,tag.getInt("Element"));entityData.set(EFFECT,tag.getInt("Effect"));entityData.set(WEAPON,tag.getInt("WeaponVisual"));impactSound=tag.getString("ImpactSound");homingTarget=tag.hasUUID("HomingTarget")?tag.getUUID("HomingTarget"):null;homingTurn=tag.contains("HomingTurn")?Math.max(0,Math.min(1,tag.getFloat("HomingTurn"))):.25f;ignoreBlocks=tag.getBoolean("IgnoreBlocks");collisionPolicy=SpellCollisionPolicy.VANILLA_PHYSICAL;if(tag.contains("CollisionPolicy",net.minecraft.nbt.Tag.TAG_STRING)){try{collisionPolicy=SpellCollisionPolicy.valueOf(tag.getString("CollisionPolicy"));}catch(IllegalArgumentException ignored){collisionPolicy=ignoreBlocks?SpellCollisionPolicy.IGNORE_BLOCKS:SpellCollisionPolicy.VANILLA_PHYSICAL;}}else if(ignoreBlocks)collisionPolicy=SpellCollisionPolicy.IGNORE_BLOCKS;flightProfile=FlightProfile.STANDARD;if(tag.contains("FlightProfile",net.minecraft.nbt.Tag.TAG_STRING)){try{flightProfile=FlightProfile.valueOf(tag.getString("FlightProfile"));}catch(IllegalArgumentException ignored){flightProfile=FlightProfile.STANDARD;}} if(entityData.get(WEAPON)==4&&flightProfile==FlightProfile.STANDARD){flightProfile=FlightProfile.SINGULARITY_CHARGE;collisionPolicy=SpellCollisionPolicy.SOURCE_PASSTHROUGH;ignoreBlocks=false;setNoGravity(true);}singularityExploded=tag.getBoolean("SingularityExploded"); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
