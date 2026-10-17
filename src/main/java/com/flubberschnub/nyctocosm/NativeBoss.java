package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.PartEntity;
import org.joml.Vector3f;

/** Per-entity state machines and typed attack carriers, without global boss health or commands. */
public final class NativeBoss extends Monster implements net.minecraftforge.entity.IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ANIMATION=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<CompoundTag> PLANE=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> BODY=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> APPEARANCE=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.COMPOUND_TAG);
    public final int kind;
    private final ServerBossEvent bar;
    private int remaining=0, attackAge=0, cooldown=40;
    private Attack attack;
    private final SerpentBody serpentBody=new SerpentBody();
    private int serpentBodySpeed=2;
    private CompoundTag pendingSerpentBody;
    private SerpentPart[] parts;
    private EmissaryPart[] emissaryParts;
    private int emissaryPartsPhase=-1;
    private UUID staffId;
    private double sorcererGroundY=Double.NaN;
    private boolean staffBroken;
    private boolean staffShield;
    private UUID sorcererTargetId;
    private int bossInvulnerableTicks;
    private final Vec3[] halos=new Vec3[8];
    private boolean independentHalos;
    private final SorcererDash sorcererDash=new SorcererDash();
    private Vec3 meteorTear,meteorLanding,hailCenter;
    private int meteorTearAge=-1,hailAge=-1;
    private int meteorShotsEmitted;
    private float meteorTearYaw,meteorTearPitch;
    private final int[] hailDelay=new int[18];
    private int lastHurtSound=-100;
    private final SerpentMotion serpentMotion=new SerpentMotion();
    private boolean serpentInSolid;
    private final Set<UUID> participants=new LinkedHashSet<>();
    private boolean defeatRewardsGiven;
    private int ordinaryTimer,ordinaryState,farTicks,floatTimer=40;
    private boolean apparitionAlerted,floatUp;
    private boolean emissaryFlying;
    private Vec3 emissaryPoint;
    private Vec3 emissaryFlightAnchor;
    private Vec3 emissaryFlyTarget;
    private final List<Vec3> emissaryOtherFlyTargets=new ArrayList<>();
    private UUID emissaryTargetId,emissarySeatId;
    private int emissaryFlySpeed=5,emissaryFlyTimer,emissaryPoise,emissaryCorkscrewScore;
    private boolean emissaryGrappleTrying,emissaryCorkscrew,emissaryPendingDeath,emissaryQuickTurn;
    private boolean emissaryDamageApplication,emissaryDamageBypasses;
    private Vec3 emissaryEntranceLanding;
    private boolean emissaryHoverEstablished;
    // Animation names are the original artist's clips. Durations are their exported keyframe counts.
    public record Attack(String animation,int ticks,float damage,Pattern pattern,Combat.Element element) {}
    public enum Pattern { BEAM, VOLLEY, RAIN, RING, DASH, PILLARS, PULL, MELEE, MOVE }
    private static Attack a(String name,int ticks,float damage,Pattern pattern,Combat.Element element) {return new Attack(name,ticks,damage,pattern,element);}
    private static final Attack[] SORCERER_A={
        a("halobeam",133,10,Pattern.BEAM,Combat.Element.VOID),a("shardhail",64,10,Pattern.VOLLEY,Combat.Element.DREAM),
        a("frozenblades",67,14,Pattern.RING,Combat.Element.DREAM),a("singularitycharge",63,20,Pattern.PULL,Combat.Element.VOID),
        a("voidmeteor",67,15,Pattern.RAIN,Combat.Element.VOID)};
    private static final Attack[] SORCERER_B={
        a("dashstrike",71,15,Pattern.DASH,Combat.Element.VOID),a("javelinswing",67,16,Pattern.MELEE,Combat.Element.VOID),
        a("seekingsingularities",66,13,Pattern.VOLLEY,Combat.Element.VOID),a("voidbeam",67,3,Pattern.BEAM,Combat.Element.VOID),
        a("voidwave",66,20,Pattern.RING,Combat.Element.VOID),a("airdash",66,15,Pattern.DASH,Combat.Element.VOID)};
    private static final Attack ICE_SPIKES=a("icespikes",41,3,Pattern.PILLARS,Combat.Element.VOID);
    private static final Attack[][] EMISSARY_ATTACKS={
        {a("phase1.attack.lightning_javelin",51,45,Pattern.VOLLEY,Combat.Element.DISSONANCE),a("phase1.attack.meteor_shower",51,40,Pattern.RAIN,Combat.Element.DISSONANCE),a("phase1.attack.pillars",71,35,Pattern.PILLARS,Combat.Element.DISSONANCE),a("phase1.attack.wingbeat",61,5,Pattern.RING,Combat.Element.DISSONANCE)},
        {a("phase2.attack.celestial_smite",111,80,Pattern.PILLARS,Combat.Element.RADIANCE),a("phase2.attack.flame_geysers",71,40,Pattern.PILLARS,Combat.Element.DISSONANCE),a("phase2.attack.sword_plunge",61,60,Pattern.DASH,Combat.Element.DISSONANCE),a("phase2.attack.wing_blast",81,40,Pattern.RING,Combat.Element.RADIANCE)},
        {a("phase3.attack.cross",21,35,Pattern.BEAM,Combat.Element.RADIANCE),a("phase3.attack.flame_slash",41,50,Pattern.VOLLEY,Combat.Element.DISSONANCE),a("phase3.attack.ground_slam",26,60,Pattern.RING,Combat.Element.DISSONANCE),a("phase3.attack.lightning_field",61,35,Pattern.PILLARS,Combat.Element.DISSONANCE),a("phase3.attack.seeking_missiles",51,15,Pattern.VOLLEY,Combat.Element.DISSONANCE),a("phase3.attack.swordspin",31,60,Pattern.RING,Combat.Element.DISSONANCE),a("phase3.attack.swordswing",21,50,Pattern.MELEE,Combat.Element.DISSONANCE)},
        {a("phase4.attack.apocalypse_rain",61,12,Pattern.RAIN,Combat.Element.DISSONANCE),a("phase4.attack.corkscrew",81,60,Pattern.DASH,Combat.Element.RADIANCE),a("phase4.attack.divine_blast",66,80,Pattern.RING,Combat.Element.RADIANCE),a("phase4.attack.fireball",41,35,Pattern.VOLLEY,Combat.Element.DISSONANCE),a("phase4.attack.godrays",61,40,Pattern.PILLARS,Combat.Element.RADIANCE),a("phase4.attack.grapple",91,25,Pattern.PULL,Combat.Element.DISSONANCE)}};
    private static final Attack STRAFE_LEFT=a("phase2.attack.strafeleft",21,0,Pattern.MOVE,Combat.Element.RADIANCE),STRAFE_RIGHT=a("phase2.attack.straferight",21,0,Pattern.MOVE,Combat.Element.RADIANCE),
        RUN_CLOSE=a("phase3.attack.run",21,0,Pattern.MOVE,Combat.Element.RADIANCE),FLIP_FAR=a("phase3.attack.flipfar",33,0,Pattern.MOVE,Combat.Element.RADIANCE),
        FRONT_KICK=a("phase3.attack.frontkick",21,35,Pattern.MELEE,Combat.Element.DISSONANCE),TAKE_OFF=a("phase4_idle_transition_flying",31,0,Pattern.MOVE,Combat.Element.RADIANCE),LAND=a("phase4_flying_transition_idle",31,0,Pattern.MOVE,Combat.Element.RADIANCE);
    private static final Attack STRAFE_LEFT_3=a("phase3.attack.strafeleft",21,0,Pattern.MOVE,Combat.Element.RADIANCE),STRAFE_RIGHT_3=a("phase3.attack.straferight",21,0,Pattern.MOVE,Combat.Element.RADIANCE),
        GRAPPLE_GRAB=a("phase4.attack.grapple_grab",181,25,Pattern.PULL,Combat.Element.DISSONANCE);

    public NativeBoss(EntityType<? extends NativeBoss> type,Level level,int kind) {
        super(type,level);this.kind=kind;
        if(kind==2){entityData.set(ANIMATION,"phase1_idle");setNoGravity(true);}
        bar=new ServerBossEvent(getDisplayName(),kind==0?BossEvent.BossBarColor.RED:kind==2?BossEvent.BossBarColor.YELLOW:BossEvent.BossBarColor.PURPLE,
            kind==0?BossEvent.BossBarOverlay.NOTCHED_12:kind==1?BossEvent.BossBarOverlay.NOTCHED_6:BossEvent.BossBarOverlay.PROGRESS);
        xpReward=kind==0?2800:kind==1?9000:kind==2?12000:kind==3?5:0;
        if(kind<3)setPersistenceRequired();
        if(kind==4)setNoGravity(true);
        if(kind==0) {setNoGravity(true);cooldown=100+random.nextInt(100);parts=new SerpentPart[20];for(int i=0;i<parts.length;i++)parts[i]=new SerpentPart(this);}
        if(kind==2){emissaryParts=new EmissaryPart[4];for(int i=0;i<emissaryParts.length;i++)emissaryParts[i]=new EmissaryPart(this,i);}
    }
    public static AttributeSupplier.Builder attributes(double health,double damage) {
        return attributes(health,damage,80);
    }
    public static AttributeSupplier.Builder attributes(double health,double damage,double followRange) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.ATTACK_DAMAGE,damage)
            .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,followRange).add(Attributes.KNOCKBACK_RESISTANCE,.9);
    }
    @Override public void knockback(double strength,double x,double z){
        // Source attacks move the proxy hitbox, never the marker that owns the Sorcerer's model.
        // Also covers existing native entities with the former .9 attribute saved in NBT.
        if(kind!=1)super.knockback(strength,x,z);
    }
    @Override protected void registerGoals() {
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        // The source selects these two encounters by range, not line of sight.
        // The vanilla goal's mustSee flag only controls retaining a target, so
        // also disable the TargetingConditions sight check used during selection.
        targetSelector.addGoal(2,new SourcePlayerTargetGoal(this,kind<3));
    }
    private static final class SourcePlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
        SourcePlayerTargetGoal(NativeBoss boss,boolean ignoreSight){
            super(boss,Player.class,boss.kind>=2);
            if(ignoreSight)targetConditions=targetConditions.copy().ignoreLineOfSight();
        }
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(PHASE,1);entityData.define(ANIMATION,"idle");entityData.define(START,0L);entityData.define(PLANE,new CompoundTag());entityData.define(BODY,new CompoundTag());entityData.define(APPEARANCE,new CompoundTag());}
    public boolean hasSorcererPlane(){return kind==1&&entityData.get(PLANE).contains("Start");}
    public Vec3 sorcererPlanePosition(){return readVec(entityData.get(PLANE),"Origin");}
    public float sorcererPlaneYaw(){return entityData.get(PLANE).getFloat("Yaw");}
    public float sorcererPlaneRoll(float partial){
        long age=level().getGameTime()-entityData.get(PLANE).getLong("Start");
        return age==0?.01f:(age%360)+partial;
    }
    private void summonSorcererPlane(ServerLevel server){
        CompoundTag plane=new CompoundTag();plane.putLong("Start",level().getGameTime());plane.putFloat("Yaw",getYRot());
        putVec(plane,"Origin",position().add(0,1,0).add(SerpentMotion.forward(getYRot()).scale(-2)));
        entityData.set(PLANE,plane);NativeEvents.resonanceSound(server,position(),"spells.cast.magic_plane",5,30);
        // Source voidplane is a short summon shockwave, never an endlessly spinning particle ring.
        Vec3 origin=sorcererPlanePosition().add(0,1.6,0);
        NativeNetwork.sourceVisual(server,"repair_voidplane",origin,getYRot(),-90);
    }
    private void killSorcererPlane(){entityData.set(PLANE,new CompoundTag());}
    public int phase(){return entityData.get(PHASE);}
    public String animation(){return entityData.get(ANIMATION);}
    int emissaryAttackAge(){return attackAge;}
    boolean isPerformingEmissary(String animation){return kind==2&&attack!=null&&attack.animation().equals(animation);}
    public float animationTime(float partial){return (float)(level().getGameTime()-entityData.get(START))+partial;}
    public String rig() {
        if(kind==1){
            if(animation().equals("initial_spawn"))return "ajsorcerer";
            if(animation().equals("falltransition"))return "ajsorcererm1";
            if(animation().equals("risetransition")||animation().equals("fall")||animation().equals("die"))return "ajsorcererm2";
            return (phase()&1)==0?"ajsorcererm2":"ajsorcererm1";
        }
        return kind==2?"ajemissary":"ajdeathwalker";
    }
    private void animate(String name,int duration){entityData.set(ANIMATION,name);entityData.set(START,level().getGameTime());remaining=duration;if(kind==2&&!level().isClientSide)advanceEmissaryAppearance(0);}
    private void advanceEmissaryAppearance(int frame){
        var before=entityData.get(APPEARANCE);if(before.isEmpty())before=EmissaryAppearance.initial(phase());
        var after=EmissaryAppearance.at(before,animation(),frame);if(!after.equals(entityData.get(APPEARANCE)))entityData.set(APPEARANCE,after);
    }
    public String emissaryModelVariant(String bone){
        var state=entityData.get(APPEARANCE);String variant=(state.isEmpty()?EmissaryAppearance.initial(phase()):state).getString(bone);
        return variant.equals("wing_config_1")?"wing_phase2":variant;
    }
    public void beginEntrance(){
        if(kind==1)animate("initial_spawn",331);
        else if(kind==2&&level() instanceof ServerLevel server){
            var safe=SafeTravel.findSurface(server,this,position());
            if(safe.isEmpty()){discard();return;}
            emissaryEntranceLanding=safe.get();emissaryHoverEstablished=false;emissaryFlying=false;
            attack=null;attackAge=0;
            getNavigation().stop();setDeltaMovement(Vec3.ZERO);setNoGravity(true);setXRot(0);yBodyRot=getYRot();
            setPos(emissaryEntranceLanding.subtract(Vec3.directionFromRotation(50,getYRot()).scale(100)));
            keepEmissaryEntranceTicking(server);
            animate("arrival",95);NativeEvents.sound(server,emissaryEntranceLanding,"emissary.spawn.meteor",50);
        }
    }
    public boolean emissaryApproaching(){return kind==2&&animation().equals("arrival");}
    public String emissaryWingVariant(float partial){
        return emissaryModelVariant("rightupperwing");
    }
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){
        var data=super.finalizeSpawn(level,difficulty,reason,group,tag);beginEntrance();return data;
    }
    @Override public void tick() {
        if(!level().isClientSide&&isAddedToWorld()&&(kind==0||kind==1)&&level() instanceof ServerLevel server
            &&!hasEncounterPlayerWithin(server,encounterRemovalRange(kind))){
            discardEncounterArtifacts(server);bar.removeAllPlayers();discard();return;
        }
        if(kind==0)serpentBody.initialize(position());
        if(kind==2&&!level().isClientSide){
            updateEmissaryGravity();
            if(phase()<=2||emissaryApproaching()||animation().equals("spawn")){setDeltaMovement(Vec3.ZERO);fallDistance=0;}
        }
        super.tick();
        if(kind==0){
            if(!level().isClientSide){
                // Source block/6 teleports each hitbox before moving its owning segment.
                for(int i=0;i<parts.length;i++)parts[i].setPos(serpentBody.currentPosition((i+1)*2));
                serpentBody.tick(position(),serpentBodySpeed,getYRot(),getXRot());entityData.set(BODY,serpentBody.snapshot());
            }else {
                if(pendingSerpentBody!=null){serpentBody.receive(pendingSerpentBody,true);pendingSerpentBody=null;}
                else serpentBody.finishInterpolation();
                for(int i=0;i<parts.length;i++)parts[i].setPos(serpentRenderSegment((i+1)*2,1));
            }
        }
        if(kind==2)updateEmissaryParts();
    }
    static double encounterRemovalRange(int bossKind){return bossKind==0?500:bossKind==1?100:Double.POSITIVE_INFINITY;}
    private boolean hasEncounterPlayerWithin(ServerLevel server,double range){
        double rangeSqr=range*range;
        for(ServerPlayer player:server.players())if(distanceToSqr(player)<=rangeSqr)return true;
        return false;
    }
    private void discardEncounterArtifacts(ServerLevel server){
        UUID ownerId=getUUID();List<Entity> snapshot=new ArrayList<>();server.getAllEntities().forEach(snapshot::add);
        for(Entity entity:snapshot){
            if(kind==0&&entity instanceof SerpentBeamHazard beam&&beam.ownerId().filter(ownerId::equals).isPresent())entity.discard();
            if(kind==1&&(
                entity instanceof SpellBolt bolt&&bolt.getOwner()==this
                ||entity instanceof SpellArea area&&ownerId.equals(area.ownerId())
                ||entity instanceof IceSpikeWave wave&&wave.ownerId().filter(ownerId::equals).isPresent()
                ||entity instanceof SorcererHail hail&&ownerId.equals(hail.ownerId())
                ||entity instanceof FrozenBlade blade&&ownerId.equals(blade.ownerId())
                ||entity instanceof SorcererBeamNode node&&ownerId.equals(node.ownerId())
                ||entity instanceof NullpointStaff staff&&staff.ownerId().filter(ownerId::equals).isPresent()))entity.discard();
        }
        if(kind==1){killSorcererPlane();staffId=null;}
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(BODY.equals(key)&&level().isClientSide&&serpentBody!=null)pendingSerpentBody=entityData.get(BODY).copy();
    }
    public Vec3 segment(int back){return serpentRenderSegment(Math.min(41,Math.max(0,back/4)),1);}
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(Math.max(0,getHealth()/getMaxHealth()));bar.setName(getDisplayName());
        if(!isAlive())return;
        ServerLevel server=(ServerLevel)level();
        if(bossInvulnerableTicks>0)bossInvulnerableTicks--;
        if(kind==1)tickSorcererFields(server);
        LivingEntity target=getTarget();
        if(kind==2&&target==null&&emissaryTargetId!=null&&server.getEntity(emissaryTargetId) instanceof LivingEntity savedTarget){setTarget(savedTarget);target=savedTarget;}
        if(target!=null && !Combat.canHit(this,target)) {setTarget(null);target=null;}
        if(kind==1&&attack!=null&&target==null&&sorcererTargetId!=null
            &&server.getEntity(sorcererTargetId) instanceof LivingEntity savedTarget&&Combat.canHit(this,savedTarget)){
            setTarget(savedTarget);target=savedTarget;
        }
        if(kind==1&&attack!=null&&target!=null)sorcererTargetId=target.getUUID();
        if(kind==0){tickSerpent(server,target);return;}
        if(kind>=3){tickOrdinary(server);return;}
        if(kind==2){updateEmissaryGravity();establishEmissaryHover(server);}
        if(remaining>0){
            remaining--;
            if(kind==1)tickSorcererAnimation(server,target);
            if(kind==2)tickEmissaryLifecycle(server,target);
            if(kind==2)advanceEmissaryAppearance(attack!=null?attackAge+1:EmissaryAppearance.duration(animation())-remaining);
            if(attack!=null) {attackAge++;if(target!=null||kind==1&&(attack.animation().equals("halobeam")||attack.animation().equals("voidbeam")||sorcererDash.active()))perform(server,target);else if(kind==2&&attack.animation().endsWith("apocalypse_rain"))performEmissary(server,null);else if(kind==2)tickEmissaryWithoutTarget(server);}
            if(kind==2)tickEmissaryFlight(server,target);
            if(remaining==0)finishAnimation(server);
            return;
        }
        if(kind==1){
            if(staffBroken&&(phase()&1)==1){staffBroken=false;entityData.set(PHASE,Math.min(6,phase()+1));animate("falltransition",64);return;}
            if(((phase()==2&&getHealth()<=666)||(phase()==4&&getHealth()<=333))){
                entityData.set(PHASE,phase()+1);bossInvulnerableTicks=140;animate("risetransition",60);return;
            }
            if((phase()&1)==1&&findStaff(server)==null&&!staffBroken){plantStaff(server,false);animate("rise",133);return;}
            updateSorcererHover(server);
        }
        if(kind==2){
            // The source advances one phase at a time, and only after the current attack ends.
            int threshold=phase()==1?1000:phase()==2?600:200;
            if(phase()<4&&getHealth()<=threshold){
                getNavigation().stop();setDeltaMovement(Vec3.ZERO);animate("phase"+phase()+"_transition",phase()==1?81:phase()==2?101:181);
                tickEmissaryLifecycle(server,target);return;
            }
            if(phase()==4&&emissaryPendingDeath){emissaryPendingDeath=false;super.hurt(server.damageSources().genericKill(),Float.MAX_VALUE);return;}
            tickEmissaryFlight(server,target);
        }
        if(target==null)return;
        if(kind!=2||!emissaryFlying)getLookControl().setLookAt(target,30,30);
        Vec3 difference=target.position().subtract(position());
        if(kind!=2||!emissaryFlying){setYRot((float)(Math.toDegrees(Math.atan2(difference.z,difference.x))-90));setXRot(0);yBodyRot=getYRot();}
        if(kind==2||kind==1)getNavigation().stop();
        else if(distanceTo(target)> (kind==1?10:kind==2?6:2)) getNavigation().moveTo(target,kind==2?1.4:1);
        else getNavigation().stop();
        if(--cooldown>0)return;
        double attackDistance=kind==2&&phase()==4?(emissaryFlightPlayers(60).isEmpty()?Double.POSITIVE_INFINITY:0):distanceTo(target);
        Attack nextAttack=kind==1?selectSorcererAttack(phase(),distanceTo(target),random.nextInt(35)):kind==2?selectEmissaryAttack(phase(),attackDistance,random.nextInt(100),emissaryFlying):a("slam",91,kind==3?12:8,Pattern.MELEE,Combat.Element.NIGHTMARE);
        if(nextAttack!=null)beginAttack(server,target,nextAttack);
        else if(kind==2&&phase()==4)cooldown=random.nextInt(30); // choose sets AttackTimer even in its decided/no-attack branch.
    }
    private void tickOrdinary(ServerLevel server){
        Player nearest=server.getNearestPlayer(this,kind==3?200:200);
        if(nearest==null){if(++farTicks>=(kind==3?500:200))discard();}
        else farTicks=0;
        if(kind==4){tickApparition(server,nearest);return;}
        Player target=server.getNearestPlayer(this,30);
        if(target!=null&&(target.isCreative()||target.isSpectator()))target=null;
        if(remaining>0){
            remaining--;getNavigation().stop();
            if(remaining==41){
                Vec3 center=position().add(getLookAngle().multiply(3,0,3));
                Combat.area(server,this,center,10,15,Combat.Element.NIGHTMARE,1);
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,center.x,center.y,center.z,1,0,0,0,0);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIRT.defaultBlockState()),center.x,center.y,center.z,1000,3,.5,3,.2);
                server.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.sounds.SoundSource.MASTER,3,1);
            }
            if(remaining==0){animate("walk",0);ordinaryState=2;}
            return;
        }
        if(target!=null){
            ordinaryState=2;getLookControl().setLookAt(target,90,90);lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET,target.position());
            getNavigation().moveTo(target,.72);
            if(distanceTo(target)<=7){getNavigation().stop();animate("slam",91);remaining=91;ordinaryState=3;}
            else if(!animation().equals("walk"))animate("walk",0);
            return;
        }
        getNavigation().stop();
        if(ordinaryTimer--<=0){
            if(ordinaryState==1){ordinaryState=0;ordinaryTimer=100+random.nextInt(100);animate("idle",0);}
            else{ordinaryState=1;ordinaryTimer=100+random.nextInt(900);setYRot(random.nextFloat()*360-180);animate("walk",0);}
        }
        if(ordinaryState==1){Vec3 f=SerpentMotion.forward(getYRot()).scale(.2);setDeltaMovement(f.x,getDeltaMovement().y,f.z);hurtMarked=true;}
    }
    private void tickApparition(ServerLevel server,Player nearest){
        setDeltaMovement(Vec3.ZERO);hurtMarked=true;
        if(--floatTimer<=0){floatTimer=20+random.nextInt(60);floatUp=!floatUp;}
        setPos(getX(),getY()+(floatUp?.005:-.005),getZ());
        if(tickCount%8==0)NativeEvents.sound(server,position(),"nightmare_apparition.whisper",1);
        Player target=server.getNearestPlayer(this,20);if(target!=null)lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET,target.position());
        if(!server.getBlockState(blockPosition()).isAir())setPos(getX(),getY()+.05,getZ());
        if(server.getMaxLocalRawBrightness(blockPosition())>=5){Vec3 back=getLookAngle().multiply(-1.5,0,-1.5);setPos(getX()+back.x,getY(),getZ()+back.z);}
        if(!apparitionAlerted){
            if(target!=null&&!target.isCreative()&&!target.isSpectator()){
                double distance=distanceTo(target);
                if(distance>=2&&distance<=13){Vec3 toward=target.position().subtract(position());if(toward.lengthSqr()>.001){toward=toward.normalize().scale(.05);setPos(getX()+toward.x,getY(),getZ()+toward.z);}}
                boolean moving=target.getDeltaMovement().multiply(1,0,1).lengthSqr()>.0025;
                if(distance<=13&&!target.isShiftKeyDown()&&(target.isSprinting()||moving)){
                    apparitionAlerted=true;ordinaryTimer=200+random.nextInt(50);NativeEvents.sound(server,position(),"nightmare_apparition.attack",5);
                }
            }
            return;
        }
        if(--ordinaryTimer<=0){dissipate(server);return;}
        target=server.getNearestPlayer(this,200);if(target==null)return;
        Vec3 toward=target.position().subtract(position());if(toward.lengthSqr()>.001){toward=toward.normalize().scale(.2);setPos(getX()+toward.x,getY()+toward.y,getZ()+toward.z);}
        if(distanceTo(target)<=1){target.addEffect(new MobEffectInstance(MobEffects.WITHER,200,0));target.hurt(server.damageSources().magic(),6);dissipate(server);}
    }
    private void dissipate(ServerLevel server){server.sendParticles(ParticleTypes.SQUID_INK,getX(),getY()+1,getZ(),200,1,1,1,.1);NativeEvents.sound(server,position(),"nightmare_apparition.dissipate",5);discard();}
    private void beginAttack(ServerLevel server,LivingEntity target,Attack next){
        if(kind==1){sorcererDash.finish(this);if(next.animation().equals("dashstrike")||next.animation().equals("airdash"))sorcererDash.begin(this);}
        attack=next;attackAge=0;
        independentHalos=false;
        if(kind==1)sorcererTargetId=target==null?null:target.getUUID();
        if(kind==2){
            getNavigation().stop();setDeltaMovement(Vec3.ZERO);
            if(target!=null){emissaryPoint=target.position();emissaryTargetId=target.getUUID();}
            if(next==TAKE_OFF){emissaryFlySpeed=5;chooseEmissaryFlyTarget(target);}
        }
        Arrays.fill(halos,null);
        if(next==ICE_SPIKES){animate("idle",next.ticks);NullpointStaff staff=findStaff(server);if(staff!=null)staff.emit();}
        else animate(next.animation,next.ticks);
        perform(server,target);
    }
    static Attack selectEmissaryAttack(int phase,double distance,int roll,boolean flying){
        int r=Math.floorMod(roll,100);phase=Math.max(1,Math.min(4,phase));
        if(phase==1)return r<46?EMISSARY_ATTACKS[0][0]:r<61?EMISSARY_ATTACKS[0][2]:r<76?EMISSARY_ATTACKS[0][1]:EMISSARY_ATTACKS[0][3];
        if(phase==2)return r<21?STRAFE_RIGHT:r<41?STRAFE_LEFT:r<56?EMISSARY_ATTACKS[1][0]:r<71?EMISSARY_ATTACKS[1][2]:r<86?EMISSARY_ATTACKS[1][3]:EMISSARY_ATTACKS[1][1];
        if(phase==3){
            if(distance>10)return r<41?RUN_CLOSE:r<61?EMISSARY_ATTACKS[2][1]:r<81?EMISSARY_ATTACKS[2][4]:EMISSARY_ATTACKS[2][3];
            return r<11?STRAFE_RIGHT_3:r<21?STRAFE_LEFT_3:r<38?EMISSARY_ATTACKS[2][6]:r<47?EMISSARY_ATTACKS[2][0]:r<55?FRONT_KICK:r<73?EMISSARY_ATTACKS[2][5]:r<90?EMISSARY_ATTACKS[2][2]:FLIP_FAR;
        }
        // Source phase4 block661 decides immediately when no eligible player is within60.
        if(distance>60)return flying?null:TAKE_OFF;
        if(flying)return r<51?LAND:r<66?EMISSARY_ATTACKS[3][0]:r<86?EMISSARY_ATTACKS[3][1]:EMISSARY_ATTACKS[3][5];
        return r<51?TAKE_OFF:r<66?EMISSARY_ATTACKS[3][4]:r<86?EMISSARY_ATTACKS[3][3]:EMISSARY_ATTACKS[3][2];
    }
    static List<Player> emissaryFireballTargets(Vec3 origin,Collection<? extends Player> candidates){
        return candidates.stream()
            .filter(player->!player.isSpectator()&&!player.getTags().contains("emissarydebug")&&player.distanceToSqr(origin)<=100*100)
            .map(player->(Player)player).toList();
    }
    static Vec3 emissaryFireballDirection(Vec3 origin,float fallbackYaw,float fallbackPitch,
            List<? extends Player> eligible,int selectedIndex){
        Vec3 fallback=Vec3.directionFromRotation(fallbackPitch,fallbackYaw);
        if(eligible.isEmpty())return fallback;
        Vec3 toward=eligible.get(Math.floorMod(selectedIndex,eligible.size())).position().subtract(origin);
        return toward.lengthSqr()<1.0E-12?fallback:toward;
    }
    void startEmissaryAttackForTest(LivingEntity target,String clip){
        setTarget(target);
        beginAttack((ServerLevel)level(),target,Objects.requireNonNull(emissaryAttack(clip)));
    }
    private static Attack emissaryAttack(String clip){
        return Arrays.stream(EMISSARY_ATTACKS).flatMap(Arrays::stream).filter(value->value.animation.equals(clip)).findFirst()
            .orElseGet(()->Arrays.stream(new Attack[]{STRAFE_LEFT,STRAFE_RIGHT,STRAFE_LEFT_3,STRAFE_RIGHT_3,RUN_CLOSE,FLIP_FAR,FRONT_KICK,TAKE_OFF,LAND,GRAPPLE_GRAB}).filter(value->value.animation.equals(clip)).findFirst().orElse(null));
    }
    static Attack selectSorcererAttack(int phase,double distance,int roll){
        if((phase&1)==0&&distance>7&&(distance<8||distance>20))return null;
        return switch(Math.max(1,Math.min(6,phase))){
            case 1 -> distance<=15?SORCERER_A[1]:SORCERER_A[2];
            case 2 -> distance<=7?SORCERER_B[1]:SORCERER_B[0];
            case 3 -> distance<=15?(Math.floorMod(roll,2)==0?SORCERER_A[1]:ICE_SPIKES):(Math.floorMod(roll,5)<2?SORCERER_A[2]:SORCERER_A[0]);
            case 4 -> distance<=7?(Math.floorMod(roll,3)==0?SORCERER_B[1]:SORCERER_B[2]):(Math.floorMod(roll,5)<2?SORCERER_B[0]:SORCERER_B[5]);
            case 5 -> distance<=15?(Math.floorMod(roll,7)<2?SORCERER_A[1]:Math.floorMod(roll,7)<4?ICE_SPIKES:SORCERER_A[4]):(Math.floorMod(roll,7)<2?SORCERER_A[2]:Math.floorMod(roll,7)<4?SORCERER_A[0]:SORCERER_A[3]);
            default -> distance<=7?(Math.floorMod(roll,7)<2?SORCERER_B[1]:Math.floorMod(roll,7)<4?SORCERER_B[2]:SORCERER_B[4]):(Math.floorMod(roll,7)<2?SORCERER_B[0]:Math.floorMod(roll,7)<4?SORCERER_B[5]:SORCERER_B[3]);
        };
    }

    private void tickSorcererAnimation(ServerLevel server,LivingEntity target){
        int age=(int)(level().getGameTime()-entityData.get(START));String clip=animation();
        if(clip.equals("initial_spawn")){
            if(age==161)NativeEvents.sound(server,position(),"sorcerer.spawn.conjure",5);
            if(age==193)NativeEvents.sound(server,position(),"sorcerer.spawn.brandish",5);
            if(age==230)NativeEvents.sound(server,position(),"sorcerer.spawn.twirl",5);
            if(age==250)NativeEvents.sound(server,position(),"sorcerer.spawn.plunge",5);
            if(age==253)plantStaff(server,true);
            if(age==330){animate("rise",133);return;}
        }else if(clip.equals("falltransition")){
            if(age==5){killSorcererPlane();NativeEvents.sound(server,position(),"sorcerer.spawn.conjure",5);}
            if(age==25){bossInvulnerableTicks=0;staffShield=false;setNoGravity(false);animate("fall",66);return;}
        }else if(clip.equals("fall")){
            if(age==30)NativeEvents.sound(server,position(),"sorcerer.staff.woosh",5);
            if(age==37)NativeEvents.sound(server,position(),"sorcerer.fall.blinkout",5);
            if(age==48){NativeEvents.sound(server,position(),"sorcerer.fall.blinkin",5);NativeEvents.sound(server,position(),"sorcerer.staff.woosh",5);}
        }else if(clip.equals("risetransition")){
            if(age==48)NativeEvents.sound(server,position(),"sorcerer.spawn.plunge",5);
            if(age==51)plantStaff(server,true);
            if(age==59){animate("rise",133);return;}
        }else if(clip.equals("rise")){
            setPos(position().add(0,SorcererPlane.riseStep(age),0));
            if(age==44)NativeEvents.sound(server,position(),"sorcerer.rise.woosh",5);
            if(age==127)summonSorcererPlane(server);
        }
        updateSorcererHover(server);
    }

    private void updateEmissaryGravity(){
        // The phase-four source root remains a marker even between flying attacks.
        // Vanilla gravity was undoing Godrays' lift and dropping landing/takeoff roots.
        setNoGravity(isAlive()&&(phase()<=2||phase()==4||emissaryFlying||emissaryApproaching()||animation().equals("spawn")));
    }
    private void establishEmissaryHover(ServerLevel server){
        if(emissaryHoverEstablished||phase()>2||emissaryApproaching()||animation().equals("spawn"))return;
        // Older saves lost every root-motion command and may contain a phase-one boss on/in the ground.
        server.getChunkAt(blockPosition());
        double surface=server.getHeight(Heightmap.Types.WORLD_SURFACE,getBlockX(),getBlockZ());
        double lift=37*1.5*Math.sin(Math.toRadians(35));
        if(getY()<surface+lift){
            var landing=SafeTravel.findSurface(server,this,position());
            if(landing.isEmpty())return;
            Vec3 at=landing.get().add(0,lift,0);setPos(at);hurtMarked=true;
        }
        emissaryHoverEstablished=true;setDeltaMovement(Vec3.ZERO);
    }
    private void tickEmissaryLifecycle(ServerLevel server,LivingEntity target){
        String clip=animation();
        int cueAge=clip.matches("phase[1-3]_transition")?EmissaryAppearance.duration(clip)-remaining:(int)animationTime(0);
        BossEffectTimeline.serverFrame(server,this,clip,cueAge);
        if(clip.equals("arrival")){
            if(emissaryEntranceLanding==null){beginEntrance();return;}
            int age=95-remaining;
            Vec3 at=emissaryEntranceLanding.subtract(Vec3.directionFromRotation(50,getYRot()).scale(100-age));
            server.getChunkAt(BlockPos.containing(at));setPos(at);setDeltaMovement(Vec3.ZERO);hurtMarked=true;
            // Source particles are forced: ordinary 32-block delivery hides the distant approach.
            for(ServerPlayer viewer:server.players())if(viewer.distanceToSqr(this)<=300*300){
                server.sendParticles(viewer,ParticleTypes.FLAME,true,getX(),getY(),getZ(),20,1,1,1,.2);
                server.sendParticles(viewer,ParticleTypes.LAVA,true,getX(),getY(),getZ(),10,1,1,1,.4);
                server.sendParticles(viewer,ParticleTypes.CAMPFIRE_COSY_SMOKE,true,getX(),getY(),getZ(),5,1,1,1,.1);
                server.sendParticles(viewer,ParticleTypes.FLASH,true,getX(),getY(),getZ(),1,1,1,1,0);
                server.sendParticles(viewer,ParticleTypes.SMOKE,true,getX(),getY(),getZ(),10,1,1,1,0);
                server.sendParticles(viewer,ParticleTypes.WAX_ON,true,getX(),getY(),getZ(),5,1,1,1,0);
                server.sendParticles(viewer,ParticleTypes.END_ROD,true,getX(),getY(),getZ(),5,1,1,1,0);
            }
            if(remaining==0){
                NativeEvents.sound(server,emissaryEntranceLanding,"emissary.spawn.land",50);
                animate("spawn",141);tickEmissaryLifecycle(server,target); // Original spawn keyframe zero moves one block.
            }
        }else if(clip.equals("spawn")){
            int age=141-remaining;
            if(age<=8&&age%2==0)setPos(position().add(Vec3.directionFromRotation(50,getYRot())));
            if(age==20)NativeEvents.sound(server,position(),"emissary.spawn.open",50);
            if(age==60)NativeEvents.sound(server,position(),"emissary.spawn.flap",50);
            if(age>=62&&age<99){
                setPos(position().subtract(Vec3.directionFromRotation(35,getYRot()).scale(1.5)));
                server.sendParticles(ParticleTypes.FLAME,getX(),getY(),getZ(),10,1,1,1,.08);hurtMarked=true;
            }
        }else if(clip.equals("phase1_transition")){
            // Direct particles are client keyframes; sounds use the source event table above.
        }else if(clip.equals("phase2_transition")){
            int age=101-remaining;
            if(age<5)setPos(position().subtract(SerpentMotion.forward(getYRot()).scale(5)));
            if(age>=41&&age<60)setPos(position().add(SerpentMotion.forward(getYRot()).scale(4)).add(0,-.8,0));
            if(age==60){
                Vec3 toward=target==null?SerpentMotion.forward(getYRot()):target.position().subtract(position()).multiply(1,0,1).normalize();
                var landing=SafeTravel.findSurface(server,this,position().subtract(toward.scale(20)));
                if(landing.isEmpty())landing=SafeTravel.findSurface(server,this,position());
                landing.ifPresent(this::setPos);setDeltaMovement(Vec3.ZERO);hurtMarked=true;
            }
        }
        if(clip.equals("arrival")||clip.equals("spawn"))keepEmissaryEntranceTicking(server);
    }
    private void keepEmissaryEntranceTicking(ServerLevel server){
        // A short-lived ticket follows the approach even at low simulation distance; never forceload a save permanently.
        server.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT,new net.minecraft.world.level.ChunkPos(blockPosition()),2,getId());
    }
    private void finishAnimation(ServerLevel server){
        String finished=animation();attack=null;
        if(kind==1){
            sorcererDash.finish(this);
            sorcererTargetId=null;Arrays.fill(halos,null);
            if(finished.equals("initial_spawn")&&findStaff(server)==null)plantStaff(server,true);
            if(finished.equals("initial_spawn")){animate("rise",133);return;}
            if(finished.equals("risetransition")){animate("rise",133);return;}
            cooldown=10+random.nextInt(50);
        }else if(kind==2){
            if(finished.equals("spawn"))emissaryHoverEstablished=emissaryEntranceLanding!=null;
            if(finished.matches("phase[1-3]_transition"))entityData.set(PHASE,finished.charAt(5)-'0'+1);
            releaseEmissaryGrapple(server);emissaryCorkscrew=false;emissaryGrappleTrying=false;emissaryQuickTurn=false;
            cooldown=phase()==1?20+random.nextInt(80):phase()==2?random.nextInt(50):phase()==3?random.nextInt(15):random.nextInt(30);
            updateEmissaryGravity();
        }
        else cooldown=35;
        animate(idle(),0);
    }

    private NullpointStaff findStaff(ServerLevel server){
        if(staffId!=null&&server.getEntity(staffId) instanceof NullpointStaff staff&&!staff.isRemoved())return staff;
        for(NullpointStaff staff:server.getEntitiesOfClass(NullpointStaff.class,getBoundingBox().inflate(32)))if(staff.ownerId().filter(getUUID()::equals).isPresent()){staffId=staff.getUUID();return staff;}
        return null;
    }

    private void plantStaff(ServerLevel server,boolean plunge){
        if(findStaff(server)!=null)return;
        Vec3 forward=getLookAngle();forward=new Vec3(forward.x,0,forward.z);if(forward.lengthSqr()<.001)forward=new Vec3(0,0,1);else forward=forward.normalize();
        Vec3 right=new Vec3(forward.z,0,-forward.x);Vec3 raw=position().add(forward.scale(.85)).subtract(right.scale(.7));
        double surface=localSurface(server,raw,20);
        NullpointStaff staff=Content.NULLPOINT.get().create(server);if(staff==null)return;
        staff.bind(this);staff.setPos(raw.x,surface,raw.z);staff.setYRot(getYRot());staff.setHealth(300);server.addFreshEntity(staff);staffId=staff.getUUID();staffBroken=false;staffShield=true;sorcererGroundY=surface;setNoGravity(true);getNavigation().stop();setDeltaMovement(Vec3.ZERO);
        if(plunge){
            bossInvulnerableTicks=Math.max(bossInvulnerableTicks,animation().equals("initial_spawn")?200:140);
            server.sendParticles(ParticleTypes.FLASH,staff.getX(),staff.getY()+1,staff.getZ(),5,.1,.1,.1,0);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),staff.getX(),staff.getY()+.2,staff.getZ(),100,.3,.2,.3,1);
            for(LivingEntity living:server.getEntitiesOfClass(LivingEntity.class,staff.getBoundingBox().inflate(4)))if(Combat.canHit(this,living))living.hurt(server.damageSources().mobAttack(this),20);
        }
    }
    private static double localSurface(ServerLevel server,Vec3 at,int scanDepth){
        BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos((int)Math.floor(at.x),(int)Math.floor(at.y+2),(int)Math.floor(at.z));
        for(int step=0;step<scanDepth&&cursor.getY()>server.getMinBuildHeight()+1;step++,cursor.move(0,-1,0)){
            BlockPos below=cursor.below();if(!server.getBlockState(below).getCollisionShape(server,below).isEmpty())return cursor.getY();
        }
        return Math.floor(at.y);
    }
    private void updateSorcererHover(ServerLevel server){
        if(kind!=1||sorcererDash.active())return;
        if((phase()&1)==0){if(animation().equals("fall")&&Double.isFinite(sorcererGroundY)&&getY()>sorcererGroundY+.1)setPos(getX(),Math.max(sorcererGroundY,getY()-.3),getZ());return;}
        NullpointStaff staff=findStaff(server);if(staff==null||staffBroken||staff.isRemoved())return;
        sorcererGroundY=staff.getY();setNoGravity(true);getNavigation().stop();setDeltaMovement(Vec3.ZERO);
        if(animation().equals("initial_spawn")||animation().equals("risetransition")||animation().equals("rise")||hasSorcererPlane())return;
        double target=sorcererGroundY+5.83;
        if(getY()<target-.03)setPos(getX(),Math.min(target,getY()+.14),getZ());
        else if(getY()>target+.1)setPos(getX(),Math.max(target,getY()-.14),getZ());
    }
    private void tickSorcererFields(ServerLevel server){
        DustParticleOptions purple=new DustParticleOptions(new Vector3f(.741f,0,1),1.15f);
        DustParticleOptions black=new DustParticleOptions(new Vector3f(0,0,0),1.2f);
        if(meteorTear!=null){
            if(meteorTearAge>=0){
                meteorTearAge++;
                if(meteorTearAge>60){meteorTearAge=-1;meteorTear=null;}
            }
            if(meteorTear!=null){
                if(meteorTearAge>=0&&meteorTearAge%2==0)ring(server,meteorTear,new Vec3(1,0,0),new Vec3(0,0,1),purple,2.5,24);
                server.sendParticles(black,meteorTear.x,meteorTear.y,meteorTear.z,4,.5,.2,.5,.02);
                if(meteorTearAge>=10&&meteorTearAge<=60&&meteorTearAge%10==0){
                    Player nearest=server.players().stream().filter(player->!player.isSpectator())
                        .filter(player->player.distanceToSqr(meteorTear)<=50*50)
                        .min(Comparator.comparingDouble(player->player.distanceToSqr(meteorTear))).orElse(null);
                    if(nearest!=null){
                        meteorShotsEmitted++;
                        server.sendParticles(ParticleTypes.FLASH,meteorTear.x,meteorTear.y,meteorTear.z,1,0,0,0,0);
                        float yaw=meteorTearYaw+random.nextInt(80)-40;
                        float pitch=meteorTearPitch+random.nextInt(80)-40;
                        SpellBolt.launchMeteor(server,this,meteorTear,Vec3.directionFromRotation(pitch,yaw),15);
                    }
                }
            }
        }
        if(hailAge>=0&&hailCenter!=null){
            hailAge++;
            Vec3 ball=hailCenter.add(0,Math.min(30,hailAge)*.45+5,0);
            if(hailAge<30){
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),ball.x,ball.y,ball.z,8,.4,.4,.4,.02);
                server.sendParticles(ParticleTypes.SNOWFLAKE,ball.x,ball.y,ball.z,4,.5,.5,.5,.02);
            }
            if(hailAge==30){server.sendParticles(ParticleTypes.FLASH,ball.x,ball.y,ball.z,2,.3,.3,.3,0);server.sendParticles(ParticleTypes.SNOWFLAKE,ball.x,ball.y,ball.z,100,1,1,1,.1);}
            for(int n=0;n<hailDelay.length;n++)if(hailAge==30+hailDelay[n]){
                Vec3 at=hailCenter.add(random.nextDouble()*30-15,22+random.nextDouble()*4,random.nextDouble()*30-15);
                SpellBolt.launch(server,this,at,new Vec3(0,-1,0),10,Combat.Element.DREAM,.95f,1.4f).weaponVisual(5);
            }
            if(hailAge>75){hailAge=-1;hailCenter=null;}
        }
    }

    void onStaffBroken(NullpointStaff staff){if(kind==1&&staffId!=null&&staffId.equals(staff.getUUID()))staffBroken=true;}
    public Optional<NullpointStaff> nullpoint(){return level() instanceof ServerLevel server?Optional.ofNullable(findStaff(server)):Optional.empty();}

    private void tickSerpent(ServerLevel server,LivingEntity target){
        SerpentAmbientEffects.emit(server,this);
        boolean solid=!server.getBlockState(blockPosition()).isAir();
        if(solid!=serpentInSolid){serpentInSolid=solid;NativeEvents.sound(server,position(),"serpent.breach",5);}
        if(random.nextInt(180)==0)NativeEvents.sound(server,segment(20+random.nextInt(100)),"serpent.woosh",30);
        if(solid&&random.nextInt(80)==0)NativeEvents.sound(server,position(),"serpent.rumble",30,.5f);
        Vec3 before=position();
        for(LivingEntity living:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(2.5)))
            if(Combat.canHit(this,living)&&distanceTo(living)<=2.5&&living.invulnerableTime==0)
                Combat.hit(this,living,distanceTo(living)<=2?19.5f:10,Combat.Element.NIGHTMARE);
        UUID locked=serpentMotion.targetId();
        if(locked!=null){
            Entity found=server.getEntity(locked);
            if(found==null&&target!=null&&locked.equals(target.getUUID()))found=target;
            target=found instanceof LivingEntity living&&Combat.canHit(this,living)?living:null;
        }
        var step=serpentMotion.tick(before,getLookAngle(),target==null?null:target.position(),target==null?null:target.getUUID(),random);
        if(step.startedAttack()){
            var players=server.players().stream().filter(player->Combat.canHit(this,player)&&distanceToSqr(player)<=200*200).toList();
            if(!players.isEmpty()){target=players.get(random.nextInt(players.size()));serpentMotion.bindTarget(target.getUUID());}
            NativeEvents.sound(server,before,"serpent.scream",50);
            if(serpentAttack()==2)NativeEvents.sound(server,before,"serpent.laser",50);
            if(serpentAttack()==3&&target!=null)spawnSerpentBeams(server,target);
        }
        if(step.startedDive())NativeEvents.sound(server,before,"serpent.dive",40);
        if(step.laser()){
            Vec3 direction=serpentMotion.target().subtract(before).normalize();Vec3 end=before;
            var air=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,new ResourceLocation(NativeMod.ID,"air"));
            for(int distance=1;distance<=150;distance++){
                end=before.add(direction.scale(distance));
                if(!server.getBlockState(BlockPos.containing(end)).is(air))break;
            }
            Combat.serpentLaser(server,this,before,end,12);
            particleLine(server,before,end,new DustParticleOptions(new Vector3f(1,0,0),1.5f),new DustParticleOptions(new Vector3f(0,0,0),2),1);
        }
        serpentBodySpeed=step.speed();setDeltaMovement(Vec3.ZERO);setPos(step.position());hurtMarked=true;
        setYRot(SerpentMotion.yaw(step.direction()));
        setXRot((float)-Math.toDegrees(Math.atan2(step.direction().y,step.direction().horizontalDistance())));yBodyRot=getYRot();
    }

    private void spawnSerpentBeams(ServerLevel server,LivingEntity target){
        for(int n=0;n<20;n++)SerpentBeamHazard.spawn(server,this,target,140+random.nextInt(140));
    }

    int serpentState(){return serpentMotion.state();}
    UUID serpentLastSelectedTarget(){return serpentMotion.lastSelectedTargetId();}
    int serpentAttack(){return serpentMotion.attack();}
    CompoundTag serpentMotionForTrace(){return serpentMotion.save();}
    static double serpentSpeedForTier(int tier){return tier<=1?1:tier==2?2:3;}
    int meteorShotCount(){return 6;}
    int meteorShotsEmitted(){return meteorShotsEmitted;}
    public Vec3 serpentRenderSegment(int index,float partial){return serpentBody.position(index,partial,getPosition(partial));}
    public Vec3[] serpentRenderSegments(float partial){
        Vec3[] positions=new Vec3[42];for(int i=0;i<positions.length;i++)positions[i]=serpentRenderSegment(i,partial);return positions;
    }
    public float serpentSegmentYaw(int index,float partial){return serpentBody.yaw(index,partial);}
    public float serpentSegmentPitch(int index,float partial){return serpentBody.pitch(index,partial);}
    int participantCount(){return participants.size();}
    boolean transitionInvulnerable(){return bossInvulnerableTicks>0;}
    NullpointStaff spawnNullpointForTest(ServerLevel server){plantStaff(server,false);return findStaff(server);}
    void startSerpentAttackForTest(ServerLevel server,LivingEntity target,int type){
        setTarget(target);serpentMotion.initialize(position(),getLookAngle(),random);
        serpentMotion.begin(position(),getYRot(),getXRot(),Math.max(1,Math.min(3,type)),target.getUUID());
        if(serpentAttack()==3)spawnSerpentBeams(server,target);
    }
    void startSerpentDiveForTest(LivingEntity target){
        setTarget(target);Vec3 direction=target.getEyePosition().subtract(position()).normalize();
        serpentMotion.initialize(position(),direction,random);serpentMotion.beginDive(position(),direction,target.position(),target.getUUID());
    }
    void startSorcererAttackForTest(ServerLevel server,LivingEntity target,String clip){
        if(kind!=1)return;
        for(Attack candidate:SORCERER_A)if(candidate.animation().equals(clip)){beginAttack(server,target,candidate);return;}
        for(Attack candidate:SORCERER_B)if(candidate.animation().equals(clip)){beginAttack(server,target,candidate);return;}
        if(ICE_SPIKES.animation().equals(clip))beginAttack(server,target,ICE_SPIKES);
    }
    private static Attack sorcererAttack(String clip){
        for(Attack candidate:SORCERER_A)if(candidate.animation().equals(clip))return candidate;
        for(Attack candidate:SORCERER_B)if(candidate.animation().equals(clip))return candidate;
        return ICE_SPIKES.animation().equals(clip)?ICE_SPIKES:null;
    }
    private String idle(){return kind==2?"phase"+phase()+"_idle"+(phase()==4&&emissaryFlying?"_flying":""):"idle";}
    private void perform(ServerLevel server,LivingEntity target) {
        if(kind==1&&SorcererTimeline.attacks().contains(attack.animation)){
            Vec3 particleOrigin=position();float particleYaw=getYRot(),particlePitch=getXRot();
            if(attack.animation.equals("halobeam"))tickHalos(server,target);
            for(SorcererTimeline.Event event:SorcererTimeline.at(attack.animation,attackAge))performSorcerer(server,target,event);
            if(sorcererDash.active())NativeNetwork.sorcererCue(this,attack.animation,attackAge,particleOrigin,particleYaw,particlePitch,position());
            return;
        }
        if(kind==2){performEmissary(server,target);return;}
        int strike=Math.max(8,attack.ticks/2);
        Vec3 center=target.position();
        if(attackAge<strike){
            if(attackAge%5==0)server.sendParticles(ParticleTypes.ENCHANT,center.x,center.y+.1,center.z,15,2,.05,2,0);
            return;
        }
        if(attackAge!=strike)return;
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());
        switch(attack.pattern){
            case BEAM -> Combat.beam(server,this,50,attack.damage,attack.element,1);
            case VOLLEY -> {for(int n=0;n<5;n++)SpellBolt.launch(server,this,getEyePosition(),target.getEyePosition().subtract(getEyePosition()).add(random.nextGaussian()*.7,random.nextGaussian()*.3,random.nextGaussian()*.7),attack.damage,attack.element,1.2f,1.5f);}
            case RAIN -> {for(int n=0;n<12;n++){Vec3 at=center.add(random.nextDouble()*12-6,15+random.nextDouble()*5,random.nextDouble()*12-6);SpellBolt.launch(server,this,at,new Vec3(0,-1,0),attack.damage,attack.element,.8f,3);}}
            case RING -> {Combat.area(server,this,position(),8,attack.damage,attack.element,1.3);for(int n=0;n<16;n++){double angle=n*Math.PI/8;SpellBolt.launch(server,this,position().add(0,1,0),new Vec3(Math.cos(angle),0,Math.sin(angle)),attack.damage,attack.element,.8f,1);}}
            case DASH -> {setDeltaMovement(target.position().subtract(position()).normalize().scale(1.7));hurtMarked=true;Combat.beam(server,this,Math.min(15,distanceTo(target)+2),attack.damage,attack.element,1.5);}
            case PILLARS -> {Combat.area(server,this,center,3.5,attack.damage,attack.element,.7);for(int n=0;n<8;n++)server.sendParticles(ParticleTypes.END_ROD,center.x,center.y+n,center.z,20,1,.2,1,.1);}
            case PULL -> {for(LivingEntity living:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(12)))if(Combat.canHit(this,living)){Vec3 pull=position().subtract(living.position()).normalize().scale(1.5);living.setDeltaMovement(pull);living.hurtMarked=true;Combat.hit(this,living,attack.damage,attack.element);}}
            case MELEE -> Combat.area(server,this,position().add(getLookAngle().scale(2)),4,attack.damage,attack.element,1);
            case MOVE -> {}
        }
    }
    static boolean emissaryPassable(Level level,BlockPos pos){
        return level.getBlockState(pos).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"passthrough")));
    }
    Vec3 emissaryGrapplePosition(){
        if(attack==GRAPPLE_GRAB){
            var hand=EmissaryDamagePose.sampleAt(this,"phase4.attack.grapple_grab","rightmiddlewingtip3",attackAge);
            return EmissaryDamagePose.worldPosition(this,hand);
        }
        return EmissaryPose.localPoint(position(),getYRot(),getXRot(),new Vec3(0,-1,6));
    }
    boolean holdsEmissaryGrapple(UUID seat){return seat.equals(emissarySeatId)&&animation().equals(GRAPPLE_GRAB.animation)&&attackAge<160;}
    private void releaseEmissaryGrapple(ServerLevel server){
        if(emissarySeatId!=null&&server.getEntity(emissarySeatId) instanceof EmissaryGrappleSeat seat){seat.ejectPassengers();seat.discard();}
        emissarySeatId=null;
    }
    private void chooseEmissaryFlyTarget(LivingEntity target){
        Vec3 center=emissaryFlightCenter(target);var players=shuffledEmissaryFlightPlayers(300);
        emissaryOtherFlyTargets.clear();
        if(players.isEmpty())emissaryFlyTarget=randomEmissaryWaypoint(center);
        else for(int i=0;i<players.size();i++){
            Vec3 point=randomEmissaryWaypoint(players.get(i).position());
            if(i==players.size()-1)emissaryFlyTarget=point;else emissaryOtherFlyTargets.add(point);
        }
    }
    private Vec3 randomEmissaryWaypoint(Vec3 center){
        // Source data-get uses floor at scale100, including negative coordinates.
        return new Vec3(Math.floor(center.x*100)*.01,Math.floor((center.y+20)*100)*.01,Math.floor(center.z*100)*.01)
            .add(random.nextInt(1500)*.02-15,random.nextInt(1500)*.02-15,random.nextInt(1500)*.02-15);
    }
    private List<Player> emissaryFlightPlayers(double radius){
        var server=(ServerLevel)level();var players=new LinkedHashSet<Player>(server.players());
        players.addAll(server.getEntitiesOfClass(Player.class,new AABB(position(),position()).inflate(radius)));
        return players.stream().filter(p->!p.isSpectator()&&!p.getTags().contains("emissarydebug")&&distanceToSqr(p)<=radius*radius).toList();
    }
    private List<Player> shuffledEmissaryFlightPlayers(double radius){
        var players=new ArrayList<>(emissaryFlightPlayers(radius));for(int i=players.size()-1;i>0;i--)Collections.swap(players,i,random.nextInt(i+1));return players;
    }
    private Vec3 emissaryAttackFlyTarget(LivingEntity fallback,double radius,double up,boolean all){
        var players=shuffledEmissaryFlightPlayers(radius);emissaryOtherFlyTargets.clear();
        if(all)for(int i=0;i<players.size()-1;i++)emissaryOtherFlyTargets.add(players.get(i).position().add(0,up,0));
        return (players.isEmpty()?fallback==null?emissaryFlightCenter(null):fallback.position():players.get(players.size()-1).position()).add(0,up,0);
    }
    private void singleEmissaryFlyTarget(Vec3 point){emissaryOtherFlyTargets.clear();emissaryFlyTarget=point;}
    private boolean reachedEmissaryFlyTarget(){return position().distanceToSqr(emissaryFlyTarget)<=25||emissaryOtherFlyTargets.stream().anyMatch(point->position().distanceToSqr(point)<=25);}
    private void tickEmissaryWithoutTarget(ServerLevel server){
        if(attack==GRAPPLE_GRAB){
            if(attackAge==60)singleEmissaryFlyTarget(position().add(SerpentMotion.localForward(getYRot(),50).scale(150)));
            if(attackAge==160){releaseEmissaryGrapple(server);chooseEmissaryFlyTarget(null);}
        }
        if(attackAge==70){emissaryGrappleTrying=false;emissaryQuickTurn=false;}
        if(attackAge==64){emissaryCorkscrew=false;emissaryQuickTurn=false;}
    }
    static Vec3 emissaryFlightHeading(Vec3 forward,Vec3 toward,boolean quick){
        Vec3 aimed=forward.scale(25).add(toward.normalize().scale(.5)).normalize();
        return quick?aimed.scale(10).add(toward.normalize().scale(.5)).normalize():aimed;
    }
    static SorcererCastMath.Rotation emissaryFlightRotation(float yaw,float pitch,Vec3 toward,boolean quick){
        var turn=SorcererCastMath.turn(yaw,pitch,toward,25);
        return quick?SorcererCastMath.turn(turn.yaw(),turn.pitch(),toward,10):turn;
    }
    private Vec3 emissaryFlightCenter(LivingEntity target){
        if(target!=null&&target.isAlive()&&!hasIndirectPassenger(target))emissaryFlightAnchor=target.position();
        if(emissaryFlightAnchor==null)emissaryFlightAnchor=position();
        return emissaryFlightAnchor;
    }
    private void tickEmissaryFlight(ServerLevel server,LivingEntity target){
        if(!emissaryFlying)return;
        Vec3 anchor=emissaryFlightCenter(target);
        getNavigation().stop();setNoGravity(true);setDeltaMovement(Vec3.ZERO);
        if(emissaryFlyTarget==null)chooseEmissaryFlyTarget(target);
        for(int step=0;step<Math.max(1,emissaryFlySpeed-1);step++){
            var turn=turnToEmissaryFlyTargets(25);
            if(emissaryQuickTurn)turn=turnToEmissaryFlyTargets(10);
            // Preserve source turns near the player; steer back before the eight-chunk visibility limit.
            if(position().distanceToSqr(anchor)>96*96){
                singleEmissaryFlyTarget(anchor.add(0,20,0));turn=SorcererCastMath.facing(emissaryFlyTarget.subtract(position()));
            }
            setYRot(turn.yaw());setXRot(turn.pitch());yBodyRot=getYRot();
            Vec3 next=position().add(turn.forward().scale(.5));
            Vec3 fromAnchor=next.subtract(anchor);if(fromAnchor.lengthSqr()>128*128)next=anchor.add(fromAnchor.normalize().scale(128));
            if(!server.hasChunkAt(BlockPos.containing(next)))break;
            setPos(next);fallDistance=0;
            if(!emissaryPassable(server,blockPosition())){
                setPos(getX(),server.getHeight(Heightmap.Types.WORLD_SURFACE,getBlockX(),getBlockZ()),getZ());setXRot(0);
                if(emissarySeatId!=null){singleEmissaryFlyTarget(position().add(SerpentMotion.forward(getYRot()).scale(50)));NativeEvents.sound(server,position(),"emissary.attack.grapple.slam",3);}
                else if(emissaryCorkscrew){chooseEmissaryFlyTarget(target);emissaryQuickTurn=false;}
            }
            if(reachedEmissaryFlyTarget()){chooseEmissaryFlyTarget(target);if(emissaryCorkscrew)emissaryQuickTurn=false;}
            if(emissaryGrappleTrying){
                var victims=server.getEntitiesOfClass(Player.class,getBoundingBox().inflate(1.5),p->Combat.canHit(this,p)&&p.getBoundingBox().intersects(new AABB(position().add(-1.5,-1.5,-1.5),position().add(1.5,1.5,1.5))));
                if(!victims.isEmpty()){
                    emissaryGrappleTrying=false;singleEmissaryFlyTarget(position().add(SerpentMotion.localForward(getYRot(),-35).scale(100)));
                    beginAttack(server,target,GRAPPLE_GRAB);
                    var seat=EmissaryGrappleSeat.create(server,this);emissarySeatId=seat.getUUID();
                    for(Player player:server.getEntitiesOfClass(Player.class,getBoundingBox().inflate(5),p->Combat.canHit(this,p)&&distanceToSqr(p)<=25))player.startRiding(seat,true);
                    NativeEvents.sound(server,position(),"emissary.attack.grapple.grab",3);
                }
            }
            if(emissarySeatId!=null&&server.getEntity(emissarySeatId) instanceof EmissaryGrappleSeat seat)seat.follow(this);
            if(emissaryCorkscrew){
                emissaryCorkscrewScore=(emissaryCorkscrewScore+1)%36;
                NativeNetwork.sourceVisual(server,"corkscrew",emissaryCorkscrewScore,position(),getYRot(),getXRot());
                var corkBox=new AABB(position().add(-.5,-.5,-.5),position().add(.5,.5,.5));
                for(Player victim:server.getEntitiesOfClass(Player.class,corkBox))if(!victim.isSpectator()&&!victim.getTags().contains("emissarydebug")&&victim.hurtTime==0)emissaryPhysicalHit(this,victim,60);
            }
            NativeNetwork.sourceVisual(server,"flight_step",EmissaryPose.localPoint(position(),getYRot(),getXRot(),new Vec3(0,.5,0)),getYRot(),getXRot());
        }
        NativeNetwork.sourceVisual(server,"flight",position(),getYRot(),getXRot());
        NativeEvents.resonanceSound(server,position(),"emissary.movement.fly",3,200);hurtMarked=true;
        // ai/subtick -> block736/737 remains reachable for every ordinary flying model.
        // Only the three source tags pause this clock; Apocalypse Rain does not.
        if(!emissaryCorkscrew&&!emissaryGrappleTrying&&emissarySeatId==null)--emissaryFlyTimer;
        if(emissaryFlyTimer<=0){chooseEmissaryFlyTarget(target);emissaryFlyTimer=20+random.nextInt(40);}
    }
    private SorcererCastMath.Rotation turnToEmissaryFlyTargets(double distance){
        SorcererCastMath.Rotation turn=null;
        // execute facing entity forks in marker creation order, including multiplayer targets.
        var targets=new ArrayList<>(emissaryOtherFlyTargets);targets.add(emissaryFlyTarget);
        for(Vec3 point:targets){
            turn=SorcererCastMath.turn(getYRot(),getXRot(),point.subtract(position()),distance);
            setYRot(turn.yaw());setXRot(turn.pitch());
        }
        return turn;
    }
    void parryEmissary(ServerLevel server){
        if(kind!=2||phase()!=3||!isAlive())return;
        for(var region:server.getEntitiesOfClass(EmissaryHitRegion.class,getBoundingBox().inflate(64),r->r.ownedBy(this)))region.discard();
        attack=null;attackAge=0;getNavigation().stop();setDeltaMovement(Vec3.ZERO);
        boolean broken=++emissaryPoise>=7;if(broken)emissaryPoise=0;
        animate(broken?"phase3.parry.break":"phase3.parry",broken?61:21);
        NativeEvents.sound(server,position(),broken?"emissary.attack.parrybreak":"emissary.attack.parry",2);
        server.sendParticles(ParticleTypes.FLASH,getX(),getY()+1.7,getZ(),1,0,0,0,0);
    }
    private void emissaryLunge(LivingEntity target,double step,int repeats){
        for(int i=0;i<repeats&&distanceTo(target)>1.5;i++)setPos(position().add(SerpentMotion.forward(getYRot()).scale(step)));
    }
    private void emissaryRegion(ServerLevel server,double forward,double up,float width,float height,float damage,boolean parry){
        // Source melee commands use generic damage, not one of the six spell elements.
        EmissaryHitRegion.create(server,this,position().add(SerpentMotion.forward(getYRot()).scale(forward)).add(0,up,0),width,height,damage,null,parry);
    }
    static boolean emissaryPhysicalHit(LivingEntity owner,LivingEntity target,float damage){
        return Combat.canHit(owner,target)&&target.hurt(target.level().damageSources().mobAttack(owner),damage);
    }
    private void performEmissary(ServerLevel server,LivingEntity target){
        String id=attack.animation;Vec3 point=emissaryPoint==null?target.position():emissaryPoint;
        if(attack.pattern==Pattern.MOVE){
            Vec3 toward=target.position().subtract(position());if(toward.lengthSqr()<.001)toward=getLookAngle();toward=toward.normalize();Vec3 right=new Vec3(-toward.z,0,toward.x);
            if(attack==STRAFE_LEFT||attack==STRAFE_RIGHT){if(attackAge>=3&&attackAge<17)setPos(position().add(right.scale(attack==STRAFE_LEFT?-4:4)));}
            else if(attack==STRAFE_LEFT_3||attack==STRAFE_RIGHT_3){if(attackAge<=9)setPos(position().add(right.scale(attack==STRAFE_LEFT_3?-1:1)));}
            else if(attack==RUN_CLOSE){setYRot((float)(Math.toDegrees(Math.atan2(toward.z,toward.x))-90));setPos(position().add(toward.multiply(.7,0,.7)));if(distanceTo(target)<=5){finishAnimation(server);return;}if(attackAge>=10)attackAge=-1;remaining=12;}
            else if(attack==FLIP_FAR&&attackAge>=4&&attackAge<=16)setPos(position().add(toward.multiply(-.7,0,-.7)));
            else if(attack==TAKE_OFF){
                setNoGravity(true);
                if(attackAge>=1&&attackAge<=5)setPos(position().add(0,.80625,0));
                if(attackAge==14)emissaryFlying=true;
                if(attackAge==1)NativeEvents.sound(server,position(),"emissary.movement.flytransition",5);
            }else if(attack==LAND){
                if(attackAge>=0&&attackAge<=4){
                    double y=getY()-.80625;
                    // Preserve the existing protection against burying a native boss in terrain.
                    if(server.hasChunkAt(blockPosition()))y=Math.max(y,server.getHeight(Heightmap.Types.WORLD_SURFACE,getBlockX(),getBlockZ()));
                    setPos(getX(),y,getZ());
                }
                if(attackAge==0||attackAge==3||attackAge==6||attackAge>=8&&attackAge<=11)emissaryFlySpeed=Math.max(1,emissaryFlySpeed-1);
                if(attackAge==4){emissaryFlying=false;setNoGravity(true);setXRot(0);}
            }
            if(attack==FLIP_FAR&&attackAge==24)emissaryCombo(server,target);
            hurtMarked=true;return;
        }
        if(id.endsWith("lightning_javelin")){
            if(attackAge==5)NativeEvents.sound(server,position(),"emissary.attack.lightning_javelin",5);
            // Frame 5 creates the marker, which charges twenty ticks before throwing.
            if(attackAge==25){
                var hand=EmissaryDamagePose.sampleAt(this,id,"righthand",attackAge);
                Vec3 origin=EmissaryDamagePose.offsetFromLocator(this,hand,0,0,-3);
                SpellBolt.launch(server,this,origin,target.getEyePosition().subtract(origin),45,Combat.Element.DISSONANCE,5,2).effect(SpellBolt.Effect.LIGHTNING,40).collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
            }
        }else if(id.endsWith("meteor_shower")){
            if(attackAge==10){
                NativeEvents.sound(server,position(),"emissary.attack.meteor_shower.activate",5);
                var center=EmissaryDamagePose.sampleAt(this,id,"center",attackAge);
                Vec3 locatorPosition=EmissaryDamagePose.worldPosition(this,center);
                Vec3 origin=EmissaryPose.localPoint(locatorPosition,getYRot()+center.yaw(),0,new Vec3(0,0,10));
                EmissaryHazard.create(server,this,target,EmissaryHazard.Kind.METEOR_SHOWER,origin);
            }
        }else if(id.endsWith("pillars")){
            if(attackAge==10)NativeEvents.resonanceSound(server,position(),"emissary.attack.pillars.throw",50,300);
            if(attackAge==33)EmissaryPillar.launch(server,this);
        }else if(id.endsWith("wingbeat")){
            if(attackAge==20)NativeEvents.sound(server,position(),"emissary.attack.wingbeat.flap",5);
            if(attackAge==23)EmissaryHazard.wingbeat(server,this,target);
        }else if(id.endsWith("celestial_smite")){
            if(attackAge==0){NativeEvents.resonanceSound(server,position(),"emissary.attack.celestial_smite.rise",50,300);EmissaryCelestialTear.fireStartupSequence(server,this);}
            if(attackAge==26)EmissaryCelestialTear.createTelegraph(server,this);
            if(attackAge==50)EmissaryCelestialTear.activateOwned(server,this);
            if(attackAge==93)EmissaryCelestialTear.fireOwned(server,this);
        }else if(id.endsWith("flame_geysers")){
            if(attackAge==0)NativeEvents.resonanceSound(server,position(),"emissary.attack.flame_geysers.charge",50,300);
            if(EmissaryFlameGeyser.isHandCueFrame(attackAge))EmissaryFlameGeyser.handCue(server,this,attackAge);
            if(attackAge==40)EmissaryFlameGeyser.launch(server,this);
        }else if(id.endsWith("sword_plunge")){
            if(attackAge==5)NativeEvents.sound(server,position(),"emissary.attack.sword_plunge.conjure",50);
            if(attackAge==45){
                NativeEvents.sound(server,position(),"emissary.attack.sword_plunge.drop",50);
                EmissarySwordPlunge.handCue(server,this,attackAge);
                EmissarySwordPlunge.create(server,this);
            }
        }else if(id.endsWith("wing_blast")){
            if(attackAge==0)NativeEvents.resonanceSound(server,position(),"emissary.attack.wing_blast.rise",50,300);
            if(attackAge==12){NativeEvents.resonanceSound(server,position(),"emissary.attack.wing_blast.eyes",50,300);EmissaryWingBlastRay.start(server,this);}
            if(attackAge==35)EmissaryWingBlastRay.activate(server,this);
        }else if(id.endsWith("cross")){
            if(attackAge>=5&&attackAge<=7)emissaryLunge(target,.65,2);
            if(attackAge==6)for(double at:new double[]{1,2.5,4})emissaryRegion(server,at,1.7,1.5f,1.5f,35,true);
        }else if(id.endsWith("flame_slash")){
            if(attackAge==2)EmissaryFlameSlashWave.launch(server,this,true);
            if(attackAge==13)EmissaryFlameSlashWave.launch(server,this,false);
        }else if(id.endsWith("ground_slam")){
            if(attackAge>=1&&attackAge<=4)emissaryLunge(target,.8,1);
            if(attackAge==5){emissaryRegion(server,2.5,0,3,.1f,60,false);NativeEvents.sound(server,position(),"emissary.attack.stomp",2);}
        }else if(id.endsWith("lightning_field")){
            if(attackAge==5)NativeEvents.sound(server,position(),"emissary.attack.lightning_field.charge",5);
            if(attackAge==34)EmissaryHazard.lightningField(server,this);
        }else if(id.endsWith("seeking_missiles")){
            if(attackAge==0)NativeEvents.resonanceSound(server,position(),"emissary.attack.seeking_missiles.charge",50,300);
            if(attackAge>=2&&attackAge<=14&&attackAge%2==0){
                EmissarySeekingMissile.summon(server,this,attackAge);
            }
            if(attackAge==23){
                for(Entity entity:server.getAllEntities())if(entity instanceof EmissarySeekingMissile missile&&getUUID().equals(missile.ownerId()))missile.activate();
                NativeEvents.resonanceSound(server,position(),"emissary.attack.seeking_missiles.activate",5,100);
            }
        }else if(id.endsWith("swordspin")){
            if(attackAge>=6&&attackAge<=8)emissaryLunge(target,.6,2);
            if(attackAge==7){emissaryRegion(server,3,1.5,4,1.5f,60,true);NativeEvents.sound(server,position(),"emissary.attack.sword_swing",2);}
        }else if(id.endsWith("swordswing")||id.endsWith("frontkick")){
            if(id.endsWith("frontkick")){
                if(attackAge>=4&&attackAge<=6)emissaryLunge(target,.5,3);
                if(attackAge==4){for(int n=1;n<=5;n++)emissaryRegion(server,n,1,1,1.5f,35,true);NativeEvents.sound(server,position(),"emissary.attack.punch",2);}
            }else{
                if(attackAge==1){emissaryRegion(server,2.5,1.7,4,.8f,50,true);NativeEvents.sound(server,position(),"emissary.attack.sword_swing",2);}
                if(attackAge==2||attackAge==3)emissaryLunge(target,.8,1);
            }
        }else if(id.endsWith("apocalypse_rain")){
            if(attackAge==0){emissaryFlyTarget=emissaryAttackFlyTarget(target,100,30,false);NativeEvents.resonanceSound(server,position(),"emissary.attack.apocalypse_rain.charge",5,300);}
            if(attackAge>=10&&attackAge<=44)EmissaryRainShard.fire(server,this);
        }else if(id.endsWith("corkscrew")){
            if(attackAge==13){emissaryCorkscrew=true;emissaryQuickTurn=true;emissaryFlyTarget=emissaryAttackFlyTarget(target,300,0,true);NativeEvents.sound(server,position(),"emissary.attack.corkscrew.spin",5);}
            if(attackAge==64){emissaryCorkscrew=false;emissaryQuickTurn=false;chooseEmissaryFlyTarget(target);}
        }else if(id.endsWith("divine_blast")){
            if(attackAge==0){
                var center=EmissaryDamagePose.sampleAt(this,id,"center",attackAge);
                Vec3 origin=EmissaryPose.localPoint(EmissaryDamagePose.worldPosition(this,center),getYRot()+center.yaw(),getXRot()+center.pitch(),new Vec3(0,.5,0));
                EmissaryHazard.create(server,this,target,EmissaryHazard.Kind.DIVINE_BLAST,origin);NativeEvents.sound(server,position(),"emissary.attack.divine_blast.charge",5);
            }
        }else if(id.endsWith("fireball")){
            if(attackAge==8||attackAge==18){
                String handName=attackAge==8?"righthand":"lefthand";
                var hand=EmissaryDamagePose.sampleAt(this,id,handName,attackAge);
                Vec3 origin=EmissaryDamagePose.worldPosition(this,hand);
                Collection<Player> candidates=server.getEntitiesOfClass(Player.class,new AABB(origin,origin).inflate(100));
                List<Player> eligible=emissaryFireballTargets(origin,candidates);
                int selectedIndex=eligible.isEmpty()?0:random.nextInt(eligible.size());
                Vec3 direction=emissaryFireballDirection(origin,getYRot()+hand.yaw(),getXRot()+hand.pitch(),eligible,selectedIndex);
                SpellBolt.launch(server,this,origin,direction,35,Combat.Element.DISSONANCE,1,3).effect(SpellBolt.Effect.FIRE,60).emissaryFireballFlight().collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
                NativeEvents.sound(server,position(),"emissary.attack.fireball.shoot",5);
            }
        }else if(id.endsWith("godrays")){
            if(attackAge>=5&&attackAge<=9){setPos(position().add(0,1.5,0));hurtMarked=true;}
            if(attackAge==15){
                var center=EmissaryDamagePose.sampleAt(this,id,"center",attackAge);
                Vec3 origin=EmissaryDamagePose.worldPosition(this,center).add(0,1,0);
                EmissaryHazard.create(server,this,target,EmissaryHazard.Kind.GODRAYS,origin);NativeEvents.sound(server,position(),"emissary.attack.godrays.charge",5);
            }
        }else if(id.endsWith("grapple")){
            if(attackAge==1)NativeEvents.sound(server,position(),"emissary.attack.grapple.ready",5);
            if(attackAge==15){emissaryGrappleTrying=true;emissaryQuickTurn=true;emissaryFlyTarget=emissaryAttackFlyTarget(target,100,.7,true);}
            if(attackAge==70){emissaryGrappleTrying=false;emissaryQuickTurn=false;chooseEmissaryFlyTarget(target);}
        }else if(attack==GRAPPLE_GRAB){
            if(attackAge==60)singleEmissaryFlyTarget(position().add(SerpentMotion.localForward(getYRot(),50).scale(150)));
            if(attackAge==160){releaseEmissaryGrapple(server);chooseEmissaryFlyTarget(target);}
        }
        int comboFrame=switch(id){case "phase3.attack.swordswing"->10;case "phase3.attack.cross"->12;case "phase3.attack.frontkick","phase3.attack.ground_slam"->14;case "phase3.attack.swordspin"->17;case "phase3.attack.flipfar"->24;default->-1;};
        if(attackAge==comboFrame)emissaryCombo(server,target);
    }
    private void emissaryCombo(ServerLevel server,LivingEntity target){
        if(random.nextFloat()<.7f){
            Vec3 toward=target.position().subtract(position());setYRot((float)(Math.toDegrees(Math.atan2(toward.z,toward.x))-90));setXRot(0);yBodyRot=getYRot();
            beginAttack(server,target,selectEmissaryAttack(3,distanceTo(target),random.nextInt(100),false));
        }
    }
    Vec3 emissarySeekerMissileOrigin(int frame){
        String locator;
        Vec3 offset;
        switch(frame){
            case 2 -> {locator="rightmiddlewingtip2";offset=new Vec3(1,-1,0);}
            case 4 -> {locator="rightmiddlewingtip2";offset=new Vec3(1,1,0);}
            case 6 -> {locator="rightupperwingtip2";offset=new Vec3(-2,-3,0);}
            case 8 -> {return EmissaryPose.localPoint(position(),getYRot(),getXRot(),new Vec3(0,8.5,0));}
            case 10 -> {locator="leftupperwingtip2";offset=new Vec3(2,-3,0);}
            case 12 -> {locator="leftmiddlewingtip2";offset=new Vec3(-1,1,0);}
            case 14 -> {locator="leftmiddlewingtip2";offset=new Vec3(-1,-1,0);}
            default -> throw new IllegalArgumentException("No source seeking missile node at frame "+frame);
        }
        var node=EmissaryDamagePose.sampleAt(this,"phase3.attack.seeking_missiles",locator,frame);
        return EmissaryDamagePose.offsetFromLocator(this,node,offset.x,offset.y,offset.z);
    }
    private void performSorcerer(ServerLevel server,LivingEntity target,SorcererTimeline.Event event){
        if(event.sound()!=null&&event.action()!=SorcererTimeline.Action.HALO_SUMMON&&event.action()!=SorcererTimeline.Action.HALO_FIRE)NativeEvents.sound(server,event.action()==SorcererTimeline.Action.METEOR_RAIN&&meteorTear!=null?meteorTear:position(),event.sound(),5);
        Vec3 center=target==null?position().add(getLookAngle().scale(10)):target.getBoundingBox().getCenter();
        Vec3 forward=center.subtract(getEyePosition()).normalize();
        Vec3 horizontal=new Vec3(forward.x,0,forward.z);
        if(horizontal.lengthSqr()<.001)horizontal=new Vec3(0,0,1);else horizontal=horizontal.normalize();
        Vec3 right=new Vec3(horizontal.z,0,-horizontal.x);
        DustParticleOptions purple=new DustParticleOptions(new Vector3f(.741f,0,1),1.5f);
        DustParticleOptions blue=new DustParticleOptions(new Vector3f(0,.651f,1),1.25f);
        DustParticleOptions black=new DustParticleOptions(new Vector3f(0,0,0),2);
        switch(event.action()){
            case CHARGE -> {} // Source client keyframes supply charge particles.
            case HALO_SUMMON -> {
                int index=Math.max(0,event.tick()/5-1);var node=SorcererBeamNode.summonHalo(server,this,event.tick());halos[index]=node.position();independentHalos=true;
                NativeEvents.resonanceSound(server,node.position(),"sorcerer.halobeam.summon",5,30);
            }
            case HALO_ACTIVATE -> {
                if(independentHalos){SorcererBeamNode.activate(server,getUUID());break;}
                server.sendParticles(ParticleTypes.FLASH,getX(),getY()+2,getZ(),5,.4,.4,.4,0);
                for(Vec3 halo:halos)if(halo!=null)ring(server,halo,right,new Vec3(0,1,0),blue,1.3,24);
            }
            case HALO_FIRE -> {
                if(independentHalos)break; // Only the old native array-save compatibility path uses derived firing events.
                int index=Math.max(0,(event.tick()-75)/5);Vec3 start=halos[index]==null?haloPosition(target,index):halos[index];Vec3 end=sorcererRayEnd(server,start,target.getEyePosition());
                Combat.line(server,this,start,end,attack.damage,attack.element,1,false);
                sorcererBeam(server,start,end,.6);
                server.sendParticles(ParticleTypes.FLASH,start.x,start.y,start.z,1,0,0,0,0);
            }
            case ICE_FOCUS -> {} // Source block/item particles are exported from the rig.
            case HAIL -> {
                SorcererHail.launchBall(server,this);
            }
            case VOID_FOCUS -> {} // Source particle rows and xflat portal velocities are client-owned.
            case SINGULARITY -> {
                Vec3 origin=EmissaryPose.localPoint(position(),getYRot(),getXRot(),new Vec3(0,2,.5));
                Player nearest=server.getEntitiesOfClass(Player.class,new AABB(origin,origin).inflate(50),player->!player.isSpectator()&&player.position().distanceToSqr(origin)<=50*50)
                    .stream().min(Comparator.comparingDouble(player->player.position().distanceToSqr(origin))).orElse(null);
                if(nearest!=null)SpellBolt.launch(server,this,origin,nearest.position().subtract(origin),attack.damage,attack.element,.3f,5)
                    .effect(SpellBolt.Effect.DAMAGE,100).weaponVisual(4).impactSound("sorcerer.singularitycharge.explode");
            }
            case BLADE_FOCUS -> {
                FrozenBlade.charge(server,this);
            }
            case BLADE_SWING -> {
                if(attack.animation().equals("frozenblades"))FrozenBlade.swing(server,this);
                else server.sendParticles(ParticleTypes.SWEEP_ATTACK,getX()+forward.x*2,getY()+1,getZ()+forward.z*2,7,.5,.2,.5,1);
            }
            case FROZEN_BLADES -> {
                FrozenBlade.launchPair(server,this);
            }
            case METEOR_TEAR -> {
                meteorTear=position().add(0,12,0);meteorLanding=center;meteorTearAge=-2;meteorTearYaw=getYRot();meteorTearPitch=getXRot();
                ring(server,meteorTear,new Vec3(1,0,0),new Vec3(0,0,1),purple,2.5,32);
                server.sendParticles(black,meteorTear.x,meteorTear.y,meteorTear.z,20,.3,.3,.3,0);
            }
            case METEOR_RAIN -> {
                if(meteorTear!=null){meteorTearAge=0;meteorShotsEmitted=0;server.sendParticles(ParticleTypes.FLASH,meteorTear.x,meteorTear.y,meteorTear.z,1,0,0,0,0);}
            }
            case DASH_AIM -> sorcererDash.aim(server,this,false);
            case DASH_STEP -> {
                Vec3 entry=event.tick()==50?position():sorcererDash.step(this);
                Combat.playerFeetSphere(server,this,entry,1.5,15,Combat.Element.VOID);
            }
            case DASH_FINISH -> {
                Vec3 entry=position();sorcererDash.step(this);sorcererDash.arrive(server,this);
                Combat.playerFeetSphere(server,this,entry.add(SerpentMotion.localForward(getYRot(),getXRot()).scale(1.5)),2,15,Combat.Element.VOID);
                Combat.playerFeetSphere(server,this,position(),1.5,15,Combat.Element.VOID);
            }
            case VOIDWAVE_FOCUS -> {} // Source direct cues are already compiled; no extra tear/flash burst.
            case VOIDWAVE -> {
                Vec3 source=position().add(0,2,0);
                Combat.playerFeetSphere(server,this,source,6,attack.damage,attack.element);
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,source.x,source.y,source.z,1,0,0,0,0);
                server.sendParticles(ParticleTypes.FLASH,source.x,source.y,source.z,4,.2,.2,.2,0);
                NativeNetwork.sourceVisual(server,"repair_transmittance",source,getYRot(),getXRot());
            }
            case JAVELIN -> {
                Vec3 source=sorcererJavelinOrigin();
                Combat.playerFeetSphere(server,this,source,3,attack.damage,attack.element);
                server.sendParticles(ParticleTypes.SWEEP_ATTACK,source.x,source.y,source.z,7,.5,.2,.5,1);
            }
            case AIR_RISE -> sorcererDash.rise(this,event.tick());
            case AIR_AIM -> sorcererDash.aim(server,this,true);
            case AIR_DASH -> sorcererDash.step(this); // aj.frame=39 is unused by the source's ajsorcererm2 engine.
            case AIR_SLAM -> {
                if(sorcererDash.finished())break;Vec3 entry=position();sorcererDash.arrive(server,this);
                Combat.playerFeetSphere(server,this,entry,4,15,Combat.Element.VOID);
            }
            case VOIDBEAM_AIM -> {SorcererBeamNode.aimVoid(server,this);server.sendParticles(ParticleTypes.FLASH,getX(),getY()+2,getZ(),1,0,0,0,0);}
            case VOIDBEAM -> {
                SorcererBeamNode.voidBeams(server,getUUID());
            }
            case VOIDBEAM_FINISH -> SorcererBeamNode.finishVoid(server,getUUID());
            case SEEKING_ORBS -> {
                for(int n=0;n<8;n++){
                    Vec3 direction=new Vec3(random.nextDouble()*2-1,random.nextDouble()*2-1,random.nextDouble()*2-1).normalize();
                    SpellBolt.launch(server,this,getEyePosition().add(forward.scale(1.5)),direction,attack.damage,attack.element,.24f,1)
                        .seekingSingularity(70+random.nextInt(20)).weaponVisual(7).impactSound("sorcerer.seekingsingularities.impact");
                }
            }
            case ICE_SPIKES -> {
                NullpointStaff staff=findStaff(server);Vec3 origin=staff==null?position():staff.position().add(0,2,0);
                IceSpikeWave.spawn(server,this,origin,staff==null?getYRot():staff.getYRot());
            }
        }
    }
    private Vec3 sorcererJavelinOrigin(){
        double yaw=Math.toRadians(getYRot()),pitch=Math.toRadians(getXRot());
        Vec3 localUp=new Vec3(-Math.sin(yaw)*Math.sin(pitch),Math.cos(pitch),Math.cos(yaw)*Math.sin(pitch));
        return position().add(localUp).add(getLookAngle().scale(2));
    }
    private Vec3 haloPosition(LivingEntity target,int index){
        Vec3 forward=target.getBoundingBox().getCenter().subtract(getEyePosition());forward=new Vec3(forward.x,0,forward.z);
        if(forward.lengthSqr()<.001)forward=new Vec3(0,0,1);else forward=forward.normalize();
        Vec3 right=new Vec3(forward.z,0,-forward.x);double angle=index*Math.PI/4;
        return position().add(0,2,0).add(right.scale(Math.cos(angle)*5)).add(0,Math.sin(angle)*5,0);
    }
    private void tickHalos(ServerLevel server,LivingEntity target){
        if(independentHalos||target==null)return;
        DustParticleOptions purple=new DustParticleOptions(new Vector3f(.741f,0,1),.7f);
        Vec3 aim=target.getEyePosition();
        for(int n=0;n<halos.length;n++)if(halos[n]!=null&&attackAge>=5+n*5&&attackAge<114){
            Vec3 at=halos[n];Vec3 toward=aim.subtract(at);Vec3 axis=toward.lengthSqr()<.01?new Vec3(0,0,1):toward.normalize();
            Vec3 tangent=new Vec3(axis.z,0,-axis.x);if(tangent.lengthSqr()<.01)tangent=new Vec3(1,0,0);
            ring(server,at,tangent,axis.cross(tangent),purple,attackAge>=67?.5:1,12);
            if(attackAge%2==0)particleLine(server,at,at.add(axis.scale(attackAge>=67?2:3)),purple,purple,.4);
        }
    }
    private static void sorcererBeam(ServerLevel server,Vec3 start,Vec3 end,double step){
        Vec3 delta=end.subtract(start);double length=Math.min(60,delta.length());if(length<.001)return;
        Vec3 direction=delta.normalize();
        DustParticleOptions purple=new DustParticleOptions(new Vector3f(.741f,0,1),.8f),black=new DustParticleOptions(new Vector3f(0,0,0),.6f);
        for(double along=0;along<=length;along+=step){
            Vec3 ringCenter=start.add(direction.scale(along));
            // Two batched envelopes preserve the source's black core/purple outer shell
            // without thousands of individual particle packets per active beam tick.
            server.sendParticles(black,ringCenter.x,ringCenter.y,ringCenter.z,9,.18,.18,.18,0);
            server.sendParticles(purple,ringCenter.x,ringCenter.y,ringCenter.z,12,.38,.38,.38,0);
        }
        server.sendParticles(ParticleTypes.END_ROD,end.x,end.y,end.z,4,.15,.15,.15,.02);
    }
    private static Vec3 sorcererRayEnd(ServerLevel server,Vec3 start,Vec3 intended){
        // Both source rays advance 0.5 block per recursion and stop after 60
        // recursions or on a block outside nyctocosm:passthrough.
        var passthrough=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,new ResourceLocation(NativeMod.ID,"passthrough"));
        Vec3 delta=intended.subtract(start);double length=Math.min(30,delta.length());if(length<.001)return start;
        Vec3 unit=delta.normalize();
        for(double travel=0;travel<=length;travel+=.5){Vec3 point=start.add(unit.scale(travel));
            if(!server.getBlockState(BlockPos.containing(point)).is(passthrough))return point;
        }
        return start.add(unit.scale(length));
    }
    private static void ring(ServerLevel server,Vec3 center,Vec3 axisA,Vec3 axisB,DustParticleOptions particle,double radius,int count){
        Vec3 a=axisA.normalize(),b=axisB.normalize();for(int n=0;n<count;n++){double angle=n*Math.PI*2/count;Vec3 p=center.add(a.scale(Math.cos(angle)*radius)).add(b.scale(Math.sin(angle)*radius));server.sendParticles(particle,p.x,p.y,p.z,1,0,0,0,0);}
    }
    private static void particleLine(ServerLevel server,Vec3 start,Vec3 end,DustParticleOptions first,DustParticleOptions second,double step){
        Vec3 delta=end.subtract(start);double length=delta.length();if(length<.001)return;Vec3 direction=delta.scale(1/length);
        for(double d=0;d<=length;d+=step){Vec3 p=start.add(direction.scale(d));server.sendParticles(((int)(d/step)&1)==0?first:second,p.x,p.y,p.z,1,0,0,0,0);}
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(kind==4&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
        if(kind==0&&source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL))return false;
        if(((animation().equals("spawn")||animation().equals("initial_spawn")||emissaryApproaching()||kind==2&&animation().matches("phase[1-3]_transition"))&&remaining>0||bossInvulnerableTicks>0||kind==1&&staffShield)
            &&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
        boolean hit=super.hurt(source,amount);
        if(hit&&source.getEntity() instanceof ServerPlayer player)participants.add(player.getUUID());
        if(hit&&level() instanceof ServerLevel server&&tickCount-lastHurtSound>=8){
            if(kind==0)NativeEvents.sound(server,position(),"serpent.hurt",50);
            else if(kind==1)NativeEvents.sound(server,position(),"sorcerer.hurt",4);
            lastHurtSound=tickCount;
        }
        return hit;
    }
    @Override protected void actuallyHurt(DamageSource source,float amount){
        emissaryDamageApplication=kind==2;emissaryDamageBypasses=source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
        try{super.actuallyHurt(source,amount);}finally{emissaryDamageApplication=false;emissaryDamageBypasses=false;}
    }
    @Override public void setHealth(float health){
        // Defer only genuinely lethal damage AFTER Forge events, armour and absorption.
        if(emissaryDamageApplication&&!emissaryDamageBypasses&&health<=0&&(phase()<4||remaining>0)){emissaryPendingDeath=true;health=1;}
        super.setHealth(health);
    }
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);if(kind<3)bar.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}
    @Override public void die(DamageSource source){
        if(kind==1){killSorcererPlane();sorcererDash.finish(this);}
        if(kind==2&&level() instanceof ServerLevel server){releaseEmissaryGrapple(server);emissaryCorkscrew=false;emissaryGrappleTrying=false;setDeltaMovement(Vec3.ZERO);}
        super.die(source);bar.removeAllPlayers();animate(kind==3?"death":"die",deathDuration());
        if(level() instanceof ServerLevel server){
            if(kind==2)BossEffectTimeline.serverFrame(server,this,"die",0);
            if(kind==0)NativeEvents.sound(server,position(),"serpent.death",50);
            if(kind==1){NullpointStaff staff=findStaff(server);if(staff!=null)staff.recall();}
        }
    }
    @Override public void remove(RemovalReason reason){
        if(kind==2&&(reason==RemovalReason.KILLED||reason==RemovalReason.DISCARDED)&&level() instanceof ServerLevel server){
            List<Entity> snapshot=new ArrayList<>();server.getAllEntities().forEach(snapshot::add);
            for(Entity entity:snapshot)if(entity instanceof EmissarySeekingMissile missile&&getUUID().equals(missile.ownerId()))missile.discard();
        }
        super.remove(reason);
    }
    private int deathDuration(){return kind==0?100:kind==1?180:kind==2?100:60;}
    @Override protected void tickDeath(){
        deathTime++;
        if(level() instanceof ServerLevel server){
            if(kind==0){
                Vec3 at=segment(random.nextInt(160));server.sendParticles(ParticleTypes.PORTAL,at.x,at.y,at.z,12,.8,.8,.8,.2);
                if(deathTime==100){for(int i=0;i<40;i++){Vec3 p=segment(i*4);server.sendParticles(ParticleTypes.FLASH,p.x,p.y,p.z,1,0,0,0,0);}server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY(),getZ(),4,2,2,2,0);server.playSound(null,getX(),getY(),getZ(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.sounds.SoundSource.HOSTILE,50,.5f);NativeEvents.sound(server,position(),"serpent.defeat",50,.5f);}
            }else if(kind==1){
                if(deathTime==11)NativeEvents.resonanceSound(server,position(),"sorcerer.die.float",5,30);
                if(deathTime==56){NativeEvents.resonanceSound(server,position(),"spells.soul_release",5,30);NativeEvents.resonanceSound(server,position(),"sorcerer.die.stab",5,30);}
                if(deathTime==116)NativeEvents.resonanceSound(server,position(),"sorcerer.die.poof",5,30);
                if(deathTime==126||deathTime==130)NativeEvents.resonanceSound(server,position(),"sorcerer.die.speardrop",5,30);
            }else if(kind==2){
                BossEffectTimeline.serverFrame(server,this,"die",deathTime);
                if(deathTime==100){
                    Vec3 at=EmissaryDamagePose.worldPosition(this,EmissaryDamagePose.sampleAt(this,"die","center",100));
                    NativeNetwork.sourceVisual(server,"repair_emissary_final",at,getYRot(),getXRot());
                    NativeEvents.resonanceSound(server,at,"emissary.death.finaldeath",50,300);
                }
            }
            if(deathTime>=deathDuration()){completeDefeat(server);remove(RemovalReason.KILLED);}
        }
    }
    private void completeDefeat(ServerLevel server){
        if(defeatRewardsGiven||kind>2)return;defeatRewardsGiven=true;
        String boss=kind==0?"serpent":kind==1?"sorcerer":"emissary";net.minecraft.ChatFormatting color=kind==0?net.minecraft.ChatFormatting.DARK_RED:kind==1?net.minecraft.ChatFormatting.DARK_PURPLE:net.minecraft.ChatFormatting.WHITE;
        var advancement=server.getServer().getAdvancements().getAdvancement(new ResourceLocation(NativeMod.ID,"bosses/defeat_"+boss));
        for(UUID id:participants){
            ServerPlayer player=server.getEntity(id) instanceof ServerPlayer inLevel?inLevel:server.getServer().getPlayerList().getPlayer(id);if(player==null)continue;
            var destroy=Content.SOUND_MAP.get("boss.destroy");if(destroy!=null)player.playNotifySound(destroy.get(),net.minecraft.sounds.SoundSource.HOSTILE,1,1);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20,100,20));
            var title=kind==2?net.minecraft.network.chat.Component.translatable("entity.nyctocosm.celestial_defeated",net.minecraft.network.chat.Component.translatable("entity.nyctocosm.celestial_defeated1").withStyle(color)).withStyle(net.minecraft.ChatFormatting.GOLD)
                :net.minecraft.network.chat.Component.translatable("entity.nyctocosm.boss_defeated",net.minecraft.network.chat.Component.translatable("entity.nyctocosm.boss_defeated1").withStyle(color)).withStyle(net.minecraft.ChatFormatting.GOLD);
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            ItemStack lore=new ItemStack(Content.get("adherent_"+boss));if(!player.getInventory().add(lore))player.drop(lore,false);
            if(advancement!=null)player.getAdvancements().award(advancement,"defeated");
        }
        if(kind==2)for(ServerPlayer player:server.players()){
            if(!player.level().dimension().location().getPath().equals("emissary_domain"))continue;
            NativeEvents.restoreEmissaryMode(player);
            if(advancement!=null)player.getAdvancements().award(advancement,"defeated");
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dialogue.nyctocosm.emissary_defeat.prompt_escape")
                .withStyle(style->style.withColor(net.minecraft.ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,"/nyctocosm return"))));
        }
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        super.dropCustomDeathLoot(source,looting,recentlyHit);
        if(kind==0){spawnAtLocation(Content.get("nightmare_scale"),24+random.nextInt(9)+random.nextInt(9));spawnAtLocation(Content.get("seelic_essence"),20+random.nextInt(36));}
        if(kind==1){
            int splinters=0,shards=0;for(int roll=0;roll<3;roll++)if(random.nextBoolean())splinters++;else shards+=12+random.nextInt(9);
            if(splinters>0)spawnAtLocation(Content.get("null_splinter"),splinters);if(shards>0)spawnAtLocation(Content.get("void_shard"),shards);
            spawnAtLocation(Content.get("seelic_essence"),20+random.nextInt(36));
        }
        if(kind==2){if(random.nextFloat()<.3)spawnAtLocation(Content.get("semblanceofdoom"));spawnAtLocation(Content.get("seelic_essence"),20+random.nextInt(36));}
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        tag.putBoolean("IndependentHalos",independentHalos);
        if(kind==1&&sorcererDash.active())tag.put("SorcererDash",sorcererDash.save());
        super.addAdditionalSaveData(tag);tag.putInt("NativePhase",phase());tag.putString("Animation",animation());tag.putInt("Remaining",remaining);tag.putInt("AttackAge",attackAge);tag.putInt("Cooldown",cooldown);
        tag.putInt("AnimationAge",(int)animationTime(0));
        if(hasSorcererPlane()){CompoundTag plane=entityData.get(PLANE).copy();plane.putLong("Age",level().getGameTime()-plane.getLong("Start"));tag.put("SorcererPlane",plane);}
        if(staffId!=null)tag.putUUID("Nullpoint",staffId);tag.putBoolean("StaffBroken",staffBroken);tag.putBoolean("StaffShield",staffShield);tag.putInt("BossInvulnerable",bossInvulnerableTicks);if(Double.isFinite(sorcererGroundY))tag.putDouble("SorcererGroundY",sorcererGroundY);
        if(meteorTear!=null){tag.putInt("MeteorTearAge",meteorTearAge);putVec(tag,"MeteorTear",meteorTear);if(meteorLanding!=null)putVec(tag,"MeteorLanding",meteorLanding);tag.putInt("MeteorShots",meteorShotsEmitted);tag.putFloat("MeteorTearYaw",meteorTearYaw);tag.putFloat("MeteorTearPitch",meteorTearPitch);}
        if(hailAge>=0&&hailCenter!=null){tag.putInt("HailAge",hailAge);putVec(tag,"HailCenter",hailCenter);tag.putIntArray("HailDelay",hailDelay);}
        if(kind==1&&attack!=null){
            tag.putString("ActiveSorcererAttack",attack.animation());
            if(sorcererTargetId!=null)tag.putUUID("SorcererTarget",sorcererTargetId);
            if(attack.animation().equals("halobeam")){
                CompoundTag savedHalos=new CompoundTag();boolean any=false;
                for(int index=0;index<halos.length;index++)if(halos[index]!=null){putVec(savedHalos,"Halo"+index,halos[index]);any=true;}
                if(any)tag.put("SorcererHalos",savedHalos);
            }
        }
        if(kind==0){
            tag.put("SerpentMotion",serpentMotion.save());if(serpentMotion.target()!=null)putVec(tag,"SerpentGoal",serpentMotion.target());
            tag.put("SerpentBody",serpentBody.save());
        }
        if(kind==2){
            tag.put("EmissaryAppearance",entityData.get(APPEARANCE).copy());
            tag.putBoolean("EmissaryFlying",emissaryFlying);if(emissaryPoint!=null)putVec(tag,"EmissaryPoint",emissaryPoint);
            tag.putBoolean("EmissaryHoverEstablished",emissaryHoverEstablished);
            if(emissaryEntranceLanding!=null)putVec(tag,"EmissaryEntranceLanding",emissaryEntranceLanding);
            if(emissaryFlyTarget!=null)putVec(tag,"EmissaryFlyTarget",emissaryFlyTarget);
            var otherTargets=new net.minecraft.nbt.ListTag();for(var point:emissaryOtherFlyTargets){var row=new CompoundTag();putVec(row,"Point",point);otherTargets.add(row);}tag.put("EmissaryOtherFlyTargets",otherTargets);
            if(emissaryFlightAnchor!=null)putVec(tag,"EmissaryFlightAnchor",emissaryFlightAnchor);
            if(emissaryTargetId!=null)tag.putUUID("EmissaryTarget",emissaryTargetId);if(emissarySeatId!=null)tag.putUUID("EmissarySeat",emissarySeatId);
            tag.putInt("EmissaryFlySpeed",emissaryFlySpeed);tag.putInt("EmissaryFlyTimer",emissaryFlyTimer);tag.putInt("EmissaryPoise",emissaryPoise);
            tag.putBoolean("EmissaryGrappleTrying",emissaryGrappleTrying);tag.putBoolean("EmissaryCorkscrew",emissaryCorkscrew);tag.putBoolean("EmissaryPendingDeath",emissaryPendingDeath);
            tag.putBoolean("EmissaryQuickTurn",emissaryQuickTurn);
            tag.putInt("EmissaryCorkscrewScore",emissaryCorkscrewScore);
        }
        if(kind>=3){tag.putInt("OrdinaryTimer",ordinaryTimer);tag.putInt("OrdinaryState",ordinaryState);tag.putInt("FarTicks",farTicks);tag.putInt("FloatTimer",floatTimer);tag.putBoolean("ApparitionAlerted",apparitionAlerted);tag.putBoolean("FloatUp",floatUp);}
        tag.putInt("ParticipantCount",participants.size());int participantIndex=0;for(UUID id:participants)tag.putUUID("Participant"+(participantIndex++),id);tag.putBoolean("DefeatRewards",defeatRewardsGiven);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        sorcererDash.finish(this);
        independentHalos=tag.getBoolean("IndependentHalos");
        super.readAdditionalSaveData(tag);entityData.set(PHASE,Math.max(1,Math.min(kind==2?4:kind==1?6:1,tag.getInt("NativePhase"))));
        // Resume persisted attack/lifecycle state below; unknown clips fail safely to idle.
        animate(idle(),0);attack=null;cooldown=tag.contains("Cooldown")?tag.getInt("Cooldown"):40;
        if(kind==1){
            killSorcererPlane();attackAge=0;sorcererTargetId=null;Arrays.fill(halos,null);
            String clip=tag.getString("Animation");
            String savedAttackId=tag.contains("ActiveSorcererAttack")?tag.getString("ActiveSorcererAttack"):clip;
            Attack savedAttack=sorcererAttack(savedAttackId);
            if(tag.getInt("Remaining")>0&&savedAttack!=null){
                attack=savedAttack;attackAge=Math.max(0,Math.min(savedAttack.ticks(),tag.getInt("AttackAge")));
                sorcererTargetId=tag.hasUUID("SorcererTarget")?tag.getUUID("SorcererTarget"):null;
                String attackClip=savedAttack==ICE_SPIKES?"idle":savedAttack.animation();
                animate(attackClip,tag.getInt("Remaining"));entityData.set(START,level().getGameTime()-Math.max(0,tag.getInt("AnimationAge")));
                if(savedAttack.animation().equals("halobeam")){
                    CompoundTag savedHalos=tag.getCompound("SorcererHalos");
                    for(int index=0;index<halos.length;index++)if(savedHalos.contains("Halo"+index+"X"))halos[index]=readVec(savedHalos,"Halo"+index);
                }
            }else if(tag.contains("AnimationAge")&&tag.getInt("Remaining")>0&&Set.of("initial_spawn","risetransition","rise","falltransition","fall").contains(clip)){
                animate(clip,tag.getInt("Remaining"));entityData.set(START,level().getGameTime()-tag.getInt("AnimationAge"));
            }
            if(tag.contains("SorcererPlane")){CompoundTag plane=tag.getCompound("SorcererPlane").copy();plane.putLong("Start",level().getGameTime()-plane.getLong("Age"));entityData.set(PLANE,plane);}
            if(attack!=null&&(attack.animation().equals("dashstrike")||attack.animation().equals("airdash"))){if(tag.contains("SorcererDash"))sorcererDash.load(this,tag.getCompound("SorcererDash"));else sorcererDash.begin(this);}
        }
        if(tag.hasUUID("Nullpoint"))staffId=tag.getUUID("Nullpoint");staffBroken=tag.getBoolean("StaffBroken");staffShield=tag.contains("StaffShield")?tag.getBoolean("StaffShield"):tag.hasUUID("Nullpoint")&&(phase()&1)==1;bossInvulnerableTicks=tag.getInt("BossInvulnerable");if(tag.contains("SorcererGroundY"))sorcererGroundY=tag.getDouble("SorcererGroundY");
        if(tag.contains("MeteorTearAge")&&tag.contains("MeteorTearX")){meteorTearAge=tag.getInt("MeteorTearAge");meteorTear=readVec(tag,"MeteorTear");if(tag.contains("MeteorLandingX"))meteorLanding=readVec(tag,"MeteorLanding");meteorShotsEmitted=tag.contains("MeteorShots")?tag.getInt("MeteorShots"):tag.getInt("MeteorWaves");meteorTearYaw=tag.getFloat("MeteorTearYaw");meteorTearPitch=tag.getFloat("MeteorTearPitch");}
        if(tag.contains("HailAge")&&tag.contains("HailCenterX")){hailAge=tag.getInt("HailAge");hailCenter=readVec(tag,"HailCenter");int[] saved=tag.getIntArray("HailDelay");System.arraycopy(saved,0,hailDelay,0,Math.min(saved.length,hailDelay.length));}
        if(kind==0){
            serpentMotion.load(tag.getCompound("SerpentMotion"));
            serpentBody.load(tag.getCompound("SerpentBody"));
        }
        if(kind==2){
            emissaryFlying=tag.getBoolean("EmissaryFlying");if(tag.contains("EmissaryPointX"))emissaryPoint=readVec(tag,"EmissaryPoint");
            var range=getAttribute(Attributes.FOLLOW_RANGE);
            if(range!=null&&range.getBaseValue()==80)range.setBaseValue(300); // Migrate the old native default stored in entity NBT.
            emissaryHoverEstablished=tag.getBoolean("EmissaryHoverEstablished");
            emissaryEntranceLanding=tag.contains("EmissaryEntranceLandingX")?readVec(tag,"EmissaryEntranceLanding"):null;
            if(tag.contains("EmissaryFlyTargetX"))emissaryFlyTarget=readVec(tag,"EmissaryFlyTarget");
            emissaryOtherFlyTargets.clear();for(var row:tag.getList("EmissaryOtherFlyTargets",net.minecraft.nbt.Tag.TAG_COMPOUND))emissaryOtherFlyTargets.add(readVec((CompoundTag)row,"Point"));
            emissaryFlightAnchor=tag.contains("EmissaryFlightAnchorX")?readVec(tag,"EmissaryFlightAnchor"):null;
            emissaryTargetId=tag.hasUUID("EmissaryTarget")?tag.getUUID("EmissaryTarget"):null;emissarySeatId=tag.hasUUID("EmissarySeat")?tag.getUUID("EmissarySeat"):null;
            emissaryFlySpeed=tag.contains("EmissaryFlySpeed")?Math.max(1,Math.min(10,tag.getInt("EmissaryFlySpeed"))):5;emissaryFlyTimer=tag.getInt("EmissaryFlyTimer");emissaryPoise=tag.getInt("EmissaryPoise");
            emissaryGrappleTrying=tag.getBoolean("EmissaryGrappleTrying");emissaryCorkscrew=tag.getBoolean("EmissaryCorkscrew");emissaryPendingDeath=tag.getBoolean("EmissaryPendingDeath");
            emissaryQuickTurn=tag.contains("EmissaryQuickTurn")?tag.getBoolean("EmissaryQuickTurn"):emissaryGrappleTrying||emissaryCorkscrew;
            emissaryCorkscrewScore=Math.floorMod(tag.getInt("EmissaryCorkscrewScore"),36);
            String clip=tag.getString("Animation");Attack saved=emissaryAttack(clip);
            if(tag.getInt("Remaining")>0&&(saved!=null||clip.matches("phase[1-3]_transition")||clip.startsWith("phase3.parry")||clip.equals("spawn")||clip.equals("arrival"))){
                attack=saved;attackAge=Math.max(0,tag.getInt("AttackAge"));animate(clip,tag.getInt("Remaining"));entityData.set(START,level().getGameTime()-tag.getInt("AnimationAge"));
            }else animate(idle(),0);
            entityData.set(APPEARANCE,tag.contains("EmissaryAppearance")&&!tag.getCompound("EmissaryAppearance").isEmpty()?tag.getCompound("EmissaryAppearance").copy():EmissaryAppearance.initial(phase()));
            updateEmissaryGravity();
            if((emissaryApproaching()||animation().equals("spawn"))&&level() instanceof ServerLevel server)keepEmissaryEntranceTicking(server);
        }
        if(kind>=3){
            ordinaryTimer=tag.getInt("OrdinaryTimer");ordinaryState=tag.getInt("OrdinaryState");farTicks=tag.getInt("FarTicks");floatTimer=tag.getInt("FloatTimer");apparitionAlerted=tag.getBoolean("ApparitionAlerted");floatUp=tag.getBoolean("FloatUp");
            int slamRemaining=tag.getInt("Remaining");
            if(kind==3&&tag.getString("Animation").equals("slam")&&slamRemaining>0){
                ordinaryState=3;animate("slam",Math.min(91,slamRemaining));entityData.set(START,level().getGameTime()-Math.max(0,tag.getInt("AnimationAge")));
            }else if(kind==3&&ordinaryState==3)ordinaryState=2;
        }
        participants.clear();for(int index=0;index<tag.getInt("ParticipantCount");index++)if(tag.hasUUID("Participant"+index))participants.add(tag.getUUID("Participant"+index));defeatRewardsGiven=tag.getBoolean("DefeatRewards");
        if(kind<3&&getHealth()<=0){attack=null;animate("die",Math.max(1,deathDuration()-deathTime));entityData.set(START,level().getGameTime()-deathTime);}
    }
    @Override public boolean removeWhenFarAway(double distance){return kind>=3;}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void writeSpawnData(net.minecraft.network.FriendlyByteBuf buffer){
        buffer.writeNbt(kind==0?serpentBody.snapshot():new CompoundTag());
    }
    @Override public void readSpawnData(net.minecraft.network.FriendlyByteBuf buffer){
        CompoundTag tag=buffer.readNbt();if(kind==0&&tag!=null)serpentBody.receive(tag,false);
    }
    private static void putVec(CompoundTag tag,String key,Vec3 at){tag.putDouble(key+"X",at.x);tag.putDouble(key+"Y",at.y);tag.putDouble(key+"Z",at.z);}
    private static Vec3 readVec(CompoundTag tag,String key){return new Vec3(tag.getDouble(key+"X"),tag.getDouble(key+"Y"),tag.getDouble(key+"Z"));}
    private void updateEmissaryParts(){
        if(emissaryParts==null)return;
        int currentPhase=phase();
        if(emissaryPartsPhase!=currentPhase){
            for(EmissaryPart part:emissaryParts)part.refreshDimensions();
            emissaryPartsPhase=currentPhase;
        }
        if(currentPhase==1){
            var left=EmissaryDamagePose.sample(this,"leftmiddlewingtip0");
            var right=EmissaryDamagePose.sample(this,"rightmiddlewingtip0");
            emissaryParts[0].setPos(EmissaryDamagePose.offsetFromLocator(this,left,-3,2,0));
            emissaryParts[1].setPos(EmissaryDamagePose.offsetFromLocator(this,right,3,2,0));
            emissaryParts[2].setPos(EmissaryDamagePose.offsetFromLocator(this,left,-6,4,0));
            emissaryParts[3].setPos(EmissaryDamagePose.offsetFromLocator(this,right,6,4,0));
        }else{
            var center=EmissaryDamagePose.worldPosition(this,EmissaryDamagePose.sample(this,"center")).add(0,-1.3,0);
            emissaryParts[0].setPos(center);
            for(int i=1;i<emissaryParts.length;i++)emissaryParts[i].setPos(getX(),getY()-128,getZ());
        }
    }
    private PartEntity<?>[] activeDamageParts(){return kind==0?parts:kind==2?emissaryParts:null;}
    boolean intersectsDamageParts(AABB box){
        PartEntity<?>[] active=activeDamageParts();if(active==null)return false;
        for(PartEntity<?> part:active)if(part.isPickable()&&part.getBoundingBox().intersects(box))return true;
        return false;
    }
    double damagePartsDistanceToSqr(Vec3 point){
        double nearest=Double.POSITIVE_INFINITY;
        PartEntity<?>[] active=activeDamageParts();
        if(active!=null)for(PartEntity<?> part:active)if(part.isPickable())nearest=Math.min(nearest,Combat.distanceToBoxSqr(point,part.getBoundingBox()));
        return nearest;
    }
    boolean damagePartsOnLine(Vec3 start,Vec3 end,double width){
        PartEntity<?>[] active=activeDamageParts();if(active==null)return false;
        for(PartEntity<?> part:active)if(part.isPickable()){
            AABB box=part.getBoundingBox().inflate(width);
            if(box.contains(start)||box.clip(start,end).isPresent())return true;
        }
        return false;
    }
    @Override public boolean isMultipartEntity(){return kind==0||kind==2;}
    @Override public PartEntity<?>[] getParts(){return kind==0?parts:kind==2?emissaryParts:null;}
    @Override public void setId(int id){super.setId(id);if(parts!=null)for(int i=0;i<parts.length;i++)parts[i].setId(id+i+1);if(emissaryParts!=null)for(int i=0;i<emissaryParts.length;i++)emissaryParts[i].setId(id+i+1);}
    private static final class SerpentPart extends PartEntity<NativeBoss> {
        SerpentPart(NativeBoss parent){super(parent);refreshDimensions();}
        @Override public EntityDimensions getDimensions(Pose pose){return EntityType.SLIME.getDimensions().scale(.255f*8);}
        @Override protected void defineSynchedData(){}
        @Override protected void readAdditionalSaveData(CompoundTag tag){}
        @Override protected void addAdditionalSaveData(CompoundTag tag){}
        @Override public boolean isPickable(){return true;}
        @Override public boolean hurt(DamageSource source,float amount){
        return getParent().hurt(source,amount);}
        @Override public boolean is(Entity entity){return this==entity||getParent()==entity;}
    }
    private static final class EmissaryPart extends PartEntity<NativeBoss> {
        private final int index;
        EmissaryPart(NativeBoss parent,int index){super(parent);this.index=index;refreshDimensions();}
        @Override public EntityDimensions getDimensions(Pose pose){
            NativeBoss parent=getParent();int stage=parent.phase();
            if(stage==1)return EntityType.SLIME.getDimensions().scale(.255f*4);
            if(index>0)return EntityDimensions.fixed(.01f,.01f);
            return stage==2?EntityType.GIANT.getDimensions():EntityType.WITHER_SKELETON.getDimensions();
        }
        @Override protected void defineSynchedData(){}
        @Override protected void readAdditionalSaveData(CompoundTag tag){}
        @Override protected void addAdditionalSaveData(CompoundTag tag){}
        @Override public boolean isPickable(){return getParent().isAlive()&&(getParent().phase()==1||index==0);}
        @Override public boolean hurt(DamageSource source,float amount){return isPickable()&&getParent().hurt(source,amount);}
        @Override public boolean is(Entity entity){return this==entity||getParent()==entity;}
    }
}
