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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.PartEntity;
import org.joml.Vector3f;

/** Per-entity state machine. No global boss health, marker entities or command execution. */
public final class NativeBoss extends Monster {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ANIMATION=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(NativeBoss.class,EntityDataSerializers.LONG);
    public final int kind;
    private final ServerBossEvent bar;
    private int remaining=0, attackAge=0, cooldown=40;
    private Attack attack;
    private final Vec3[] trail=new Vec3[200];
    private int trailIndex;
    private Vec3 lastHead;
    private SerpentPart[] parts;
    private UUID staffId;
    private double sorcererGroundY=Double.NaN;
    private boolean staffBroken;
    private boolean staffShield;
    private int bossInvulnerableTicks;
    private final Vec3[] halos=new Vec3[8];
    private Vec3 meteorTear,meteorLanding,hailCenter;
    private int meteorTearAge=-1,hailAge=-1;
    private int meteorWavesEmitted;
    private final int[] hailDelay=new int[18];
    private int lastHurtSound=-100;
    private int serpentState;
    private int serpentAttack;
    private int serpentAge;
    private int serpentRetarget;
    private Vec3 serpentGoal;
    private Vec3 serpentLaserAim;
    private boolean serpentInSolid;
    private final Set<UUID> participants=new LinkedHashSet<>();
    private boolean defeatRewardsGiven;
    // Animation names are the original artist's clips. Durations are their exported keyframe counts.
    public record Attack(String animation,int ticks,float damage,Pattern pattern,Combat.Element element) {}
    public enum Pattern { BEAM, VOLLEY, RAIN, RING, DASH, PILLARS, PULL, MELEE }
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
        {a("phase1.attack.lightning_javelin",51,45,Pattern.VOLLEY,Combat.Element.RADIANCE),a("phase1.attack.meteor_shower",51,40,Pattern.RAIN,Combat.Element.DISSONANCE),a("phase1.attack.pillars",71,35,Pattern.PILLARS,Combat.Element.RADIANCE),a("phase1.attack.wingbeat",61,5,Pattern.RING,Combat.Element.DISSONANCE)},
        {a("phase2.attack.celestial_smite",111,80,Pattern.PILLARS,Combat.Element.RADIANCE),a("phase2.attack.flame_geysers",71,40,Pattern.PILLARS,Combat.Element.DISSONANCE),a("phase2.attack.sword_plunge",61,60,Pattern.DASH,Combat.Element.DISSONANCE),a("phase2.attack.wing_blast",81,40,Pattern.RING,Combat.Element.DISSONANCE)},
        {a("phase3.attack.cross",21,35,Pattern.BEAM,Combat.Element.RADIANCE),a("phase3.attack.flame_slash",41,50,Pattern.VOLLEY,Combat.Element.DISSONANCE),a("phase3.attack.ground_slam",26,60,Pattern.RING,Combat.Element.DISSONANCE),a("phase3.attack.lightning_field",61,35,Pattern.PILLARS,Combat.Element.RADIANCE),a("phase3.attack.seeking_missiles",51,15,Pattern.VOLLEY,Combat.Element.RADIANCE),a("phase3.attack.swordspin",31,60,Pattern.RING,Combat.Element.DISSONANCE),a("phase3.attack.swordswing",21,50,Pattern.MELEE,Combat.Element.DISSONANCE)},
        {a("phase4.attack.apocalypse_rain",61,12,Pattern.RAIN,Combat.Element.DISSONANCE),a("phase4.attack.corkscrew",81,60,Pattern.DASH,Combat.Element.RADIANCE),a("phase4.attack.divine_blast",66,80,Pattern.RING,Combat.Element.RADIANCE),a("phase4.attack.fireball",41,35,Pattern.VOLLEY,Combat.Element.DISSONANCE),a("phase4.attack.godrays",61,40,Pattern.PILLARS,Combat.Element.RADIANCE),a("phase4.attack.grapple",91,25,Pattern.PULL,Combat.Element.DISSONANCE)}};

    public NativeBoss(EntityType<? extends NativeBoss> type,Level level,int kind) {
        super(type,level);this.kind=kind;
        if(kind==2)entityData.set(ANIMATION,"phase1_idle");
        bar=new ServerBossEvent(getDisplayName(),kind==0?BossEvent.BossBarColor.RED:kind==2?BossEvent.BossBarColor.YELLOW:BossEvent.BossBarColor.PURPLE,
            kind==0?BossEvent.BossBarOverlay.NOTCHED_12:kind==1?BossEvent.BossBarOverlay.NOTCHED_6:BossEvent.BossBarOverlay.PROGRESS);
        xpReward=kind==0?2800:kind==1?9000:kind==2?100:10;
        if(kind<3)setPersistenceRequired();
        if(kind==0) {setNoGravity(true);cooldown=100+random.nextInt(100);parts=new SerpentPart[20];for(int i=0;i<parts.length;i++)parts[i]=new SerpentPart(this);}
    }
    public static AttributeSupplier.Builder attributes(double health,double damage) {
        return attributes(health,damage,80);
    }
    public static AttributeSupplier.Builder attributes(double health,double damage,double followRange) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.ATTACK_DAMAGE,damage)
            .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,followRange).add(Attributes.KNOCKBACK_RESISTANCE,.9);
    }
    @Override protected void registerGoals() {
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(PHASE,1);entityData.define(ANIMATION,"idle");entityData.define(START,0L);}
    public int phase(){return entityData.get(PHASE);}
    public String animation(){return entityData.get(ANIMATION);}
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
    private void animate(String name,int duration){entityData.set(ANIMATION,name);entityData.set(START,level().getGameTime());remaining=duration;}
    public void beginEntrance(){if(kind==1)animate("initial_spawn",331);else if(kind==2)animate("spawn",141);}
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){
        var data=super.finalizeSpawn(level,difficulty,reason,group,tag);beginEntrance();return data;
    }
    @Override public void tick() {
        super.tick();
        if(kind==0){
            recordSerpentTrail();
            for(int i=0;i<parts.length;i++){Vec3 p=segment((i+1)*8);parts[i].setPos(p);}
        }
    }
    private void recordSerpentTrail(){
        Vec3 head=position();
        if(lastHead==null||lastHead.distanceToSqr(head)>10000){
            lastHead=head;Vec3 backward=getLookAngle().scale(-1);
            for(int i=0;i<trail.length;i++)trail[Math.floorMod(trailIndex-i,trail.length)]=head.add(backward.scale(i));return;
        }
        Vec3 movement=head.subtract(lastHead);double distance=movement.length();
        if(distance>=.25){
            Vec3 direction=movement.scale(1/distance);
            for(double step=1;step<distance;step+=1){trailIndex=(trailIndex+1)%trail.length;trail[trailIndex]=lastHead.add(direction.scale(step));}
            trailIndex=(trailIndex+1)%trail.length;trail[trailIndex]=head;lastHead=head;
        }
    }
    public Vec3 segment(int back){return serpentPath(back,1);}
    private Vec3 serpentPath(double back,float partial){
        Vec3 from=getPosition(partial);if(lastHead==null)return from;
        double traveled=0;Vec3 point=from;
        for(int i=0;i<trail.length;i++){
            Vec3 previous=trail[Math.floorMod(trailIndex-i,trail.length)];if(previous==null)break;
            double distance=point.distanceTo(previous);
            if(traveled+distance>=back&&distance>.00001)return point.lerp(previous,(back-traveled)/distance);
            traveled+=distance;point=previous;
        }
        return point;
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(Math.max(0,getHealth()/getMaxHealth()));bar.setName(getDisplayName());
        if(!isAlive())return;
        ServerLevel server=(ServerLevel)level();
        if(bossInvulnerableTicks>0)bossInvulnerableTicks--;
        if(kind==1)tickSorcererFields(server);
        LivingEntity target=getTarget();
        if(target!=null && !Combat.canHit(this,target)) {setTarget(null);target=null;}
        if(kind==0){tickSerpent(server,target);return;}
        if(remaining>0){
            remaining--;
            if(kind==1)tickSorcererAnimation(server,target);
            if(attack!=null && target!=null) {attackAge++;perform((ServerLevel)level(),target);}
            if(remaining==0)finishAnimation(server);
            return;
        }
        if(kind==1){
            if(staffBroken&&(phase()&1)==1){staffBroken=false;entityData.set(PHASE,Math.min(6,phase()+1));animate("falltransition",64);return;}
            if(((phase()==2&&getHealth()<=666)||(phase()==4&&getHealth()<=333))){
                entityData.set(PHASE,phase()+1);bossInvulnerableTicks=140;animate("risetransition",60);return;
            }
            if((phase()&1)==1&&findStaff(server)==null&&!staffBroken)plantStaff(server,false);
            updateSorcererHover(server);
        }
        int next=kind==2?(getHealth()<=200?4:getHealth()<=600?3:getHealth()<=1000?2:1):phase();
        if(next!=phase()) {entityData.set(PHASE,next);animate("phase"+(next-1)+"_transition",next==4?181:next==3?101:81);return;}
        if(target==null)return;
        getLookControl().setLookAt(target,30,30);
        Vec3 difference=target.position().subtract(position());
        setYRot((float)(Math.toDegrees(Math.atan2(difference.z,difference.x))-90));yBodyRot=getYRot();
        if(kind==1&&(phase()&1)==1)getNavigation().stop();
        else if(distanceTo(target)> (kind==1?10:kind==2?6:2)) getNavigation().moveTo(target,kind==2?1.4:1);
        else getNavigation().stop();
        if(--cooldown>0)return;
        Attack nextAttack=kind==1?selectSorcererAttack(phase(),distanceTo(target),random.nextInt(35)):kind==2?EMISSARY_ATTACKS[phase()-1][random.nextInt(EMISSARY_ATTACKS[phase()-1].length)]:a("slam",91,kind==3?12:8,Pattern.MELEE,Combat.Element.NIGHTMARE);
        beginAttack(server,target,nextAttack);
    }
    private void beginAttack(ServerLevel server,LivingEntity target,Attack next){
        attack=next;attackAge=0;
        Arrays.fill(halos,null);
        if(kind==1&&target!=null){
            switch(next.animation()){
                case "shardhail","frozenblades","singularitycharge","dashstrike","voidwave","javelinswing","airdash","icespikes" ->
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,1));
                case "seekingsingularities" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,1));
            }
            if(next.animation().equals("voidmeteor"))target.addEffect(new MobEffectInstance(MobEffects.DARKNESS,100,0));
            if(next.animation().equals("voidwave"))target.addEffect(new MobEffectInstance(MobEffects.DARKNESS,80,0));
        }
        if(next==ICE_SPIKES){animate("idle",next.ticks);NullpointStaff staff=findStaff(server);if(staff!=null)staff.emit();}
        else animate(next.animation,next.ticks);
        perform(server,target);
    }
    static Attack selectSorcererAttack(int phase,double distance,int roll){
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
        }else if(clip.equals("falltransition")){
            if(age==5)NativeEvents.sound(server,position(),"sorcerer.spawn.conjure",5);
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
            if(age==44)NativeEvents.sound(server,position(),"sorcerer.rise.woosh",5);
            if(age==127){NativeEvents.sound(server,position(),"spells.cast.magic_plane",5);voidPlane(server);}
        }
        updateSorcererHover(server);
    }

    private void finishAnimation(ServerLevel server){
        String finished=animation();attack=null;
        if(kind==1){
            if(finished.equals("initial_spawn")&&findStaff(server)==null)plantStaff(server,true);
            if(finished.equals("risetransition")){animate("rise",133);return;}
            cooldown=10+random.nextInt(50);
        }else cooldown=kind==2?20:35;
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
        if(kind!=1)return;
        if((phase()&1)==0){if(animation().equals("fall")&&Double.isFinite(sorcererGroundY)&&getY()>sorcererGroundY+.1)setPos(getX(),Math.max(sorcererGroundY,getY()-.3),getZ());return;}
        NullpointStaff staff=findStaff(server);if(staff==null||staffBroken||staff.isRemoved())return;
        sorcererGroundY=staff.getY();setNoGravity(true);getNavigation().stop();setDeltaMovement(Vec3.ZERO);
        double target=sorcererGroundY+6;
        if(getY()<target-.03)setPos(getX(),Math.min(target,getY()+.14),getZ());
        else if(getY()>target+.1)setPos(getX(),Math.max(target,getY()-.14),getZ());
        if(tickCount%3==0)voidPlane(server);
    }
    private void voidPlane(ServerLevel server){
        DustParticleOptions purple=new DustParticleOptions(new Vector3f(.741f,0,1),.8f);
        DustParticleOptions black=new DustParticleOptions(new Vector3f(0,0,0),.8f);
        double y=getY()-.25,radius=1.4;
        for(int n=0;n<24;n++){double angle=n*Math.PI/12+tickCount*.035;Vec3 p=position().add(Math.cos(angle)*radius,y-getY(),Math.sin(angle)*radius);
            server.sendParticles((n&1)==0?purple:black,p.x,p.y,p.z,1,0,0,0,0);}
    }
    private void tickSorcererFields(ServerLevel server){
        DustParticleOptions purple=new DustParticleOptions(new Vector3f(.741f,0,1),1.15f);
        DustParticleOptions black=new DustParticleOptions(new Vector3f(0,0,0),1.2f);
        if(meteorTearAge>=0&&meteorTear!=null){
            meteorTearAge++;
            if(meteorTearAge<140){
                if(meteorTearAge%2==0)ring(server,meteorTear,new Vec3(1,0,0),new Vec3(0,0,1),purple,2.5,24);
                server.sendParticles(black,meteorTear.x,meteorTear.y,meteorTear.z,4,.5,.2,.5,.02);
                if(meteorTearAge>=20&&meteorTearAge<=120&&meteorTearAge%20==0){
                    meteorWavesEmitted++;
                    server.sendParticles(ParticleTypes.FLASH,meteorTear.x,meteorTear.y,meteorTear.z,1,0,0,0,0);
                    for(int n=0;n<meteorBoltsPerWave();n++){
                        Vec3 landing=meteorLanding.add(random.nextDouble()*24-12,0,random.nextDouble()*24-12);
                        Vec3 at=landing.add(0,15+random.nextDouble()*9,0);
                        SpellBolt.launch(server,this,at,landing.subtract(at),15,Combat.Element.VOID,.8f,5)
                            .weaponVisual(6).impactSound("sorcerer.voidmeteor.impact");
                    }
                }
            }else{meteorTearAge=-1;meteorTear=null;}
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
        server.sendParticles(new DustParticleOptions(new Vector3f(0,0,0),1.8f),getX(),getY()+1.5,getZ(),30,.7,.7,.7,.01);
        boolean solid=!server.getBlockState(blockPosition()).isAir();
        if(solid!=serpentInSolid){serpentInSolid=solid;NativeEvents.sound(server,position(),"serpent.breach",5);}
        if(random.nextInt(180)==0)NativeEvents.sound(server,segment(20+random.nextInt(100)),"serpent.woosh",30);
        if(solid&&random.nextInt(80)==0)NativeEvents.sound(server,position(),"serpent.rumble",30,.5f);
        if(target!=null&&distanceTo(target)<=2.5&&target.invulnerableTime==0){
            Combat.hit(this,target,distanceTo(target)<=2?19.5f:10,Combat.Element.NIGHTMARE);
        }
        if(target==null){serpentRoam(server,null);return;}
        switch(serpentState){
            case 1 -> {
                serpentAge++;double angle=Math.toRadians(serpentAge*9);serpentGoal=target.position().add(Math.sin(angle)*10,6+Math.sin(angle*2)*2,Math.cos(angle)*10);steerSerpent(serpentGoal,serpentSpeedForTier(3),.35);
                if(serpentAge>=61){
                    serpentAge=0;
                    if(serpentAttack==1){serpentState=2;NativeEvents.sound(server,position(),"serpent.dive",40);}
                    else if(serpentAttack==2){serpentState=3;serpentLaserAim=target.getEyePosition();}
                    else {serpentState=0;cooldown=20;}
                }
            }
            case 2 -> {
                serpentAge++;steerSerpentDive(target.position());
                if(serpentAge>=400){serpentState=0;cooldown=100+random.nextInt(100);}
            }
            case 3 -> {
                serpentAge++;serpentLaserAim=serpentLaserAim.scale(.85).add(target.getEyePosition().scale(.15));
                Vec3 through=target.position().subtract(position());Vec3 farSide=through.lengthSqr()<.001?target.position():target.position().add(through.normalize().scale(20));steerSerpent(farSide,serpentSpeedForTier(1),.18);
                Vec3 start=getEyePosition();Vec3 direction=serpentLaserAim.subtract(start);if(direction.lengthSqr()<.001)direction=getLookAngle();
                Vec3 wanted=start.add(direction.normalize().scale(Math.min(130,direction.length()+20)));Vec3 end=server.clip(new ClipContext(start,wanted,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getLocation();
                Combat.line(server,this,start,end,12,Combat.Element.NIGHTMARE,3,false);
                particleLine(server,start,end,new DustParticleOptions(new Vector3f(1,0,0),1.5f),new DustParticleOptions(new Vector3f(0,0,0),2),.55);
                if(server.clip(new ClipContext(start,wanted,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS&&serpentAge%5==0)server.sendParticles(ParticleTypes.EXPLOSION,end.x,end.y,end.z,1,.2,.2,.2,0);
                if(serpentAge>=100){serpentState=0;cooldown=100+random.nextInt(100);}
            }
            default -> serpentRoam(server,target);
        }
    }

    private void serpentRoam(ServerLevel server,LivingEntity target){
        if(serpentGoal==null||--serpentRetarget<=0||position().distanceToSqr(serpentGoal)<9){
            Vec3 center=target==null?position():target.position();serpentGoal=center.add(random.nextDouble()*60-30,5+random.nextDouble()*6-3,random.nextDouble()*60-30);serpentRetarget=20+random.nextInt(30);
        }
        steerSerpent(serpentGoal,serpentSpeedForTier(2),.14);
        if(target!=null&&--cooldown<=0){
            serpentAttack=1+random.nextInt(3);serpentState=1;serpentAge=0;NativeEvents.sound(server,position(),"serpent.scream",50);
            if(serpentAttack==2)NativeEvents.sound(server,position(),"serpent.laser",50);
            if(serpentAttack==3)spawnSerpentBeams(server,target);
        }
    }

    private void spawnSerpentBeams(ServerLevel server,LivingEntity target){
        for(int n=0;n<20;n++){
            double angle=random.nextDouble()*Math.PI*2,radius=3+random.nextDouble()*17;double x=target.getX()+Math.cos(angle)*radius,z=target.getZ()+Math.sin(angle)*radius;
            BlockPos surface=server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,BlockPos.containing(x,target.getY(),z));
            SerpentBeamHazard.spawn(server,this,new Vec3(x,surface.getY(),z),140+random.nextInt(140));
        }
    }

    private void steerSerpent(Vec3 goal,double speed,double turn){
        Vec3 towards=goal.subtract(position());if(towards.lengthSqr()<.001)return;
        Vec3 prior=lastHead==null?Vec3.ZERO:position().subtract(lastHead);
        Vec3 direction=prior.lengthSqr()<.01?towards.normalize():prior.normalize().scale(1-turn).add(towards.normalize().scale(turn)).normalize();
        Vec3 motion=direction.scale(Math.min(speed,towards.length()));
        setDeltaMovement(Vec3.ZERO);setPos(position().add(motion));hurtMarked=true;
        setYRot((float)(Math.toDegrees(Math.atan2(motion.z,motion.x))-90));setXRot((float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance())));yBodyRot=getYRot();
    }
    private void steerSerpentDive(Vec3 targetFeet){
        // Original Interp=2 marker chain: currentfacing is placed 30 blocks in
        // front of the head, then advances only 3 blocks toward the live target.
        // The head follows that marker at the fixed tier-two speed, so it keeps
        // its momentum and flies past a player rather than converging on them.
        Vec3 prior=lastHead==null?getLookAngle():position().subtract(lastHead);
        if(prior.lengthSqr()<.0001)prior=getLookAngle();prior=prior.normalize();
        Vec3 facing=position().add(prior.scale(30));Vec3 correction=targetFeet.subtract(facing);
        if(correction.length()>3.1)facing=facing.add(correction.normalize().scale(3));
        Vec3 direction=facing.subtract(position());if(direction.lengthSqr()<.0001)return;
        Vec3 motion=direction.normalize().scale(serpentSpeedForTier(2));
        setDeltaMovement(Vec3.ZERO);setPos(position().add(motion));hurtMarked=true;
        setYRot((float)(Math.toDegrees(Math.atan2(motion.z,motion.x))-90));setXRot((float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance())));yBodyRot=getYRot();
    }

    int serpentState(){return serpentState;}
    int serpentAttack(){return serpentAttack;}
    static double serpentSpeedForTier(int tier){return tier<=1?1:tier==2?2:3;}
    int meteorWaveCount(){return 6;}
    int meteorBoltsPerWave(){return 20;}
    int meteorWavesEmitted(){return meteorWavesEmitted;}
    public Vec3 serpentRenderSegment(int index,float partial){return serpentPath(index*4,partial);}
    public Vec3[] serpentRenderSegments(float partial){
        Vec3[] positions=new Vec3[41];positions[0]=getPosition(partial);
        if(lastHead==null){Arrays.fill(positions,positions[0]);return positions;}
        Vec3 cursor=positions[0];double traveled=0;int next=1;
        for(int i=0;i<trail.length&&next<positions.length;i++){
            Vec3 previous=trail[Math.floorMod(trailIndex-i,trail.length)];if(previous==null)break;
            double length=cursor.distanceTo(previous);
            while(length>.00001&&next<positions.length&&next*4<=traveled+length){
                positions[next]=cursor.lerp(previous,(next*4-traveled)/length);next++;
            }
            traveled+=length;cursor=previous;
        }
        while(next<positions.length)positions[next++]=cursor;
        return positions;
    }
    int participantCount(){return participants.size();}
    boolean transitionInvulnerable(){return bossInvulnerableTicks>0;}
    NullpointStaff spawnNullpointForTest(ServerLevel server){plantStaff(server,false);return findStaff(server);}
    void startSerpentAttackForTest(ServerLevel server,LivingEntity target,int type){serpentAttack=Math.max(1,Math.min(3,type));serpentState=1;serpentAge=0;if(serpentAttack==3)spawnSerpentBeams(server,target);}
    void startSerpentDiveForTest(LivingEntity target){
        setTarget(target);serpentAttack=1;serpentState=2;serpentAge=0;
        Vec3 direction=target.getEyePosition().subtract(position()).normalize();lastHead=position().subtract(direction);
    }
    void startSorcererAttackForTest(ServerLevel server,LivingEntity target,String clip){
        if(kind!=1)return;
        for(Attack candidate:SORCERER_A)if(candidate.animation().equals(clip)){beginAttack(server,target,candidate);return;}
        for(Attack candidate:SORCERER_B)if(candidate.animation().equals(clip)){beginAttack(server,target,candidate);return;}
    }
    private String idle(){return kind==2?"phase"+phase()+"_idle":"idle";}
    private void perform(ServerLevel server,LivingEntity target) {
        if(kind==1&&SorcererTimeline.attacks().contains(attack.animation)){
            if(attack.animation.equals("halobeam"))tickHalos(server,target);
            for(SorcererTimeline.Event event:SorcererTimeline.at(attack.animation,attackAge))performSorcerer(server,target,event);
            return;
        }
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
        }
    }
    private void performSorcerer(ServerLevel server,LivingEntity target,SorcererTimeline.Event event){
        if(event.sound()!=null)NativeEvents.sound(server,position(),event.sound(),5);
        Vec3 center=target.getBoundingBox().getCenter();
        Vec3 forward=center.subtract(getEyePosition()).normalize();
        Vec3 horizontal=new Vec3(forward.x,0,forward.z);
        if(horizontal.lengthSqr()<.001)horizontal=new Vec3(0,0,1);else horizontal=horizontal.normalize();
        Vec3 right=new Vec3(horizontal.z,0,-horizontal.x);
        DustParticleOptions purple=new DustParticleOptions(new Vector3f(.741f,0,1),1.5f);
        DustParticleOptions blue=new DustParticleOptions(new Vector3f(0,.651f,1),1.25f);
        DustParticleOptions black=new DustParticleOptions(new Vector3f(0,0,0),2);
        switch(event.action()){
            case CHARGE -> server.sendParticles(purple,getX(),getY()+2,getZ(),20,.35,.5,.35,.02);
            case HALO_SUMMON -> {
                int index=Math.max(0,event.tick()/5-1);Vec3 halo=haloPosition(target,index);
                halos[index]=halo;
                ring(server,halo,right,new Vec3(0,1,0),purple,1.2,24);
                server.sendParticles(ParticleTypes.END_ROD,halo.x,halo.y,halo.z,10,.15,.15,.15,.01);
            }
            case HALO_ACTIVATE -> {
                server.sendParticles(ParticleTypes.FLASH,getX(),getY()+2,getZ(),5,.4,.4,.4,0);
                for(Vec3 halo:halos)if(halo!=null)ring(server,halo,right,new Vec3(0,1,0),blue,1.3,24);
            }
            case HALO_FIRE -> {
                int index=Math.max(0,(event.tick()-75)/5);Vec3 start=halos[index]==null?haloPosition(target,index):halos[index];Vec3 end=sorcererRayEnd(server,start,target.getEyePosition());
                Combat.line(server,this,start,end,attack.damage,attack.element,1,false);
                sorcererBeam(server,start,end,.6);
                server.sendParticles(ParticleTypes.FLASH,start.x,start.y,start.z,1,0,0,0,0);
            }
            case ICE_FOCUS -> {
                Vec3 focus=getEyePosition().add(forward.scale(.6));
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),focus.x,focus.y,focus.z,5,.12,.12,.12,.05);
                server.sendParticles(ParticleTypes.SNOWFLAKE,focus.x,focus.y,focus.z,3,.15,.15,.15,.01);
            }
            case HAIL -> {
                hailCenter=center;hailAge=0;
                for(int n=0;n<hailDelay.length;n++)hailDelay[n]=random.nextInt(40);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,1));
            }
            case VOID_FOCUS -> {
                Vec3 focus=getEyePosition().add(forward.scale(.5));
                server.sendParticles(purple,focus.x,focus.y,focus.z,3,.2,.2,.2,0);
                for(int n=0;n<8;n++){double angle=n*Math.PI/4+event.tick()*.25;server.sendParticles(ParticleTypes.PORTAL,focus.x+Math.cos(angle),focus.y,focus.z+Math.sin(angle),1,0,0,0,.2);}
                if(event.tick()%5==4)server.sendParticles(ParticleTypes.FLASH,focus.x,focus.y,focus.z,1,0,0,0,0);
            }
            case SINGULARITY -> SpellBolt.launch(server,this,getEyePosition().add(forward.scale(.5)),forward,attack.damage,attack.element,.3f,5)
                .effect(SpellBolt.Effect.DAMAGE,100).weaponVisual(4).impactSound("sorcerer.singularitycharge.explode");
            case BLADE_FOCUS -> {
                Vec3 hand=getEyePosition().add(forward.scale(.5)).add(right.scale(event.tick()%2==0?.65:-.65)).add(0,-.3,0);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ICE.defaultBlockState()),hand.x,hand.y,hand.z,12,.05,.35,.05,.02);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK,hand.x,hand.y,hand.z,7,.12,.35,.12,.03);
                server.sendParticles(ParticleTypes.SNOWFLAKE,hand.x,hand.y,hand.z,3,.12,.4,.12,.02);
            }
            case BLADE_SWING -> server.sendParticles(ParticleTypes.SWEEP_ATTACK,getX()+forward.x*2,getY()+1,getZ()+forward.z*2,7,.5,.2,.5,1);
            case FROZEN_BLADES -> {
                Vec3 origin=getEyePosition().add(forward.scale(1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,1));
                SpellBolt.launch(server,this,origin.add(right.scale(.7)),center.subtract(origin.add(right.scale(.7))),attack.damage,attack.element,1.25f,1).weaponVisual(5);
                SpellBolt.launch(server,this,origin.subtract(right.scale(.7)),center.subtract(origin.subtract(right.scale(.7))),attack.damage,attack.element,1.25f,1).weaponVisual(5);
            }
            case METEOR_TEAR -> {
                meteorTear=position().add(0,12,0);meteorLanding=center;meteorTearAge=0;meteorWavesEmitted=0;
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS,100,0));
                ring(server,meteorTear,new Vec3(1,0,0),new Vec3(0,0,1),purple,2.5,32);
                server.sendParticles(black,meteorTear.x,meteorTear.y,meteorTear.z,20,.3,.3,.3,0);
            }
            case METEOR_RAIN -> {if(meteorTear!=null)server.sendParticles(ParticleTypes.FLASH,meteorTear.x,meteorTear.y,meteorTear.z,1,0,0,0,0);}
            case DASH_AIM -> {lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());server.sendParticles(ParticleTypes.FLASH,getX(),getY()+2,getZ(),1,0,0,0,0);}
            case DASH_STEP -> dashToward(server,target,1.5,attack.damage,false,purple,blue);
            case DASH_FINISH -> dashToward(server,target,1.5,attack.damage,true,purple,blue);
            case VOIDWAVE_FOCUS -> {server.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR,getX()+forward.x*.5,getY()+2,getZ()+forward.z*.5,10,.2,.2,.2,0);if(event.tick()%6==5)server.sendParticles(ParticleTypes.FLASH,getX(),getY()+2,getZ(),2,.2,.2,.2,0);}
            case VOIDWAVE -> {
                Combat.area(server,this,position(),6,attack.damage,attack.element,1.3);
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY()+1,getZ(),1,0,0,0,0);
                server.sendParticles(ParticleTypes.FLASH,getX(),getY()+1,getZ(),4,.2,.2,.2,0);
                for(int n=0;n<48;n++){double angle=n*Math.PI/24;server.sendParticles(purple,getX()+Math.cos(angle)*6,getY()+.2,getZ()+Math.sin(angle)*6,1,0,0,0,0);}
            }
            case JAVELIN -> Combat.area(server,this,position().add(forward.scale(2)),3,attack.damage,attack.element,1);
            case AIR_RISE -> {setDeltaMovement(getDeltaMovement().multiply(.3,0,.3).add(0,event.tick()<5?.3:event.tick()<14?.6:.1,0));hurtMarked=true;server.sendParticles(purple,getX(),getY()+1,getZ(),12,.5,.7,.5,.05);}
            case AIR_AIM -> {lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());server.sendParticles(ParticleTypes.FLASH,getX(),getY()+2,getZ(),1,0,0,0,0);}
            case AIR_DASH -> dashToward(server,target,1.7,attack.damage,false,purple,black);
            case AIR_SLAM -> {
                Combat.area(server,this,position(),4,attack.damage,attack.element,1.5);
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY()+1,getZ(),1,0,0,0,0);server.sendParticles(ParticleTypes.FLASH,getX(),getY()+1,getZ(),2,.3,.3,.3,0);
            }
            case VOIDBEAM_AIM -> {lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());server.sendParticles(ParticleTypes.FLASH,getX(),getY()+2,getZ(),1,0,0,0,0);}
            case VOIDBEAM -> {
                lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());Vec3 start=getEyePosition().add(getLookAngle().scale(2.5));Vec3 end=sorcererRayEnd(server,start,start.add(getLookAngle().scale(30)));
                Combat.line(server,this,start,end,attack.damage,attack.element,.7,false);sorcererBeam(server,start,end,1);
            }
            case SEEKING_ORBS -> {
                for(int n=0;n<8;n++){
                    Vec3 direction=new Vec3(random.nextDouble()*2-1,random.nextDouble()*2-1,random.nextDouble()*2-1).normalize();
                    SpellBolt.launch(server,this,getEyePosition().add(forward.scale(1.5)),direction,attack.damage,attack.element,.24f,1)
                        .effect(SpellBolt.Effect.HOMING,70+random.nextInt(20)).homeOn(target).weaponVisual(4).impactSound("sorcerer.seekingsingularities.impact");
                }
            }
            case ICE_SPIKES -> {
                NullpointStaff staff=findStaff(server);Vec3 origin=staff==null?position():staff.position().add(0,2,0);
                IceSpikeWave.spawn(server,this,origin);
            }
        }
    }
    private Vec3 haloPosition(LivingEntity target,int index){
        Vec3 forward=target.getBoundingBox().getCenter().subtract(getEyePosition());forward=new Vec3(forward.x,0,forward.z);
        if(forward.lengthSqr()<.001)forward=new Vec3(0,0,1);else forward=forward.normalize();
        Vec3 right=new Vec3(forward.z,0,-forward.x);double angle=index*Math.PI/4;
        return position().add(0,2,0).add(right.scale(Math.cos(angle)*5)).add(0,Math.sin(angle)*5,0);
    }
    private void tickHalos(ServerLevel server,LivingEntity target){
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
    private void dashToward(ServerLevel server,LivingEntity target,double speed,float damage,boolean finish,DustParticleOptions first,DustParticleOptions second){
        Vec3 direction=target.position().subtract(position());if(direction.lengthSqr()>.001)setDeltaMovement(direction.normalize().scale(speed));hurtMarked=true;
        server.sendParticles(first,getX(),getY()+1.6,getZ(),5,.1,.1,.1,0);server.sendParticles(second,getX(),getY()+2,getZ(),20,.5,1,.5,0);
        Combat.area(server,this,position(),finish?2:1.5,damage,attack.element,finish?1:0);
    }
    private static void ring(ServerLevel server,Vec3 center,Vec3 axisA,Vec3 axisB,DustParticleOptions particle,double radius,int count){
        Vec3 a=axisA.normalize(),b=axisB.normalize();for(int n=0;n<count;n++){double angle=n*Math.PI*2/count;Vec3 p=center.add(a.scale(Math.cos(angle)*radius)).add(b.scale(Math.sin(angle)*radius));server.sendParticles(particle,p.x,p.y,p.z,1,0,0,0,0);}
    }
    private static void particleLine(ServerLevel server,Vec3 start,Vec3 end,DustParticleOptions first,DustParticleOptions second,double step){
        Vec3 delta=end.subtract(start);double length=delta.length();if(length<.001)return;Vec3 direction=delta.scale(1/length);
        for(double d=0;d<=length;d+=step){Vec3 p=start.add(direction.scale(d));server.sendParticles(((int)(d/step)&1)==0?first:second,p.x,p.y,p.z,1,0,0,0,0);}
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(((animation().equals("spawn")||animation().equals("initial_spawn"))&&remaining>0||bossInvulnerableTicks>0||kind==1&&staffShield)
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
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);if(kind<3)bar.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}
    @Override public void die(DamageSource source){
        super.die(source);bar.removeAllPlayers();animate(kind==3?"death":"die",deathDuration());
        if(level() instanceof ServerLevel server){
            if(kind==0)NativeEvents.sound(server,position(),"serpent.death",50);
            if(kind==1){NullpointStaff staff=findStaff(server);if(staff!=null)staff.recall();}
        }
    }
    private int deathDuration(){return kind==0?100:kind==1?133:kind==2?87:60;}
    @Override protected void tickDeath(){
        deathTime++;
        if(level() instanceof ServerLevel server){
            if(kind==0){
                Vec3 at=segment(random.nextInt(160));server.sendParticles(ParticleTypes.PORTAL,at.x,at.y,at.z,12,.8,.8,.8,.2);
                if(deathTime==100){for(int i=0;i<40;i++){Vec3 p=segment(i*4);server.sendParticles(ParticleTypes.FLASH,p.x,p.y,p.z,1,0,0,0,0);}server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY(),getZ(),4,2,2,2,0);server.playSound(null,getX(),getY(),getZ(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.sounds.SoundSource.HOSTILE,50,.5f);NativeEvents.sound(server,position(),"serpent.defeat",50,.5f);}
            }else if(kind==1){
                if(deathTime>=15&&deathTime<=72&&deathTime%3==0)server.sendParticles(ParticleTypes.PORTAL,getX(),getY()+2,getZ(),20,.1,.1,.1,1);
                if(deathTime==11)NativeEvents.sound(server,position(),"sorcerer.die.float",5);
                if(deathTime==56){NativeEvents.sound(server,position(),"spells.soul_release",5);NativeEvents.sound(server,position(),"sorcerer.die.stab",5);}
                if(deathTime==116){server.sendParticles(ParticleTypes.POOF,getX(),getY()+2,getZ(),120,1,2,1,.2);NativeEvents.sound(server,position(),"sorcerer.die.poof",5);}
                if(deathTime==126||deathTime==130)NativeEvents.sound(server,position(),"sorcerer.die.speardrop",5);
            }
            if(deathTime>=deathDuration()){completeDefeat(server);remove(RemovalReason.KILLED);}
        }
    }
    private void completeDefeat(ServerLevel server){
        if(defeatRewardsGiven||kind>1)return;defeatRewardsGiven=true;
        String boss=kind==0?"serpent":"sorcerer";net.minecraft.ChatFormatting color=kind==0?net.minecraft.ChatFormatting.DARK_RED:net.minecraft.ChatFormatting.DARK_PURPLE;
        var advancement=server.getServer().getAdvancements().getAdvancement(new ResourceLocation(NativeMod.ID,"bosses/defeat_"+boss));
        for(UUID id:participants){
            ServerPlayer player=server.getEntity(id) instanceof ServerPlayer inLevel?inLevel:server.getServer().getPlayerList().getPlayer(id);if(player==null)continue;
            var destroy=Content.SOUND_MAP.get("boss.destroy");if(destroy!=null)player.playNotifySound(destroy.get(),net.minecraft.sounds.SoundSource.HOSTILE,1,1);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20,100,20));
            player.connection.send(new ClientboundSetTitleTextPacket(net.minecraft.network.chat.Component.translatable("entity.nyctocosm.boss_defeated",net.minecraft.network.chat.Component.translatable("entity.nyctocosm.boss_defeated1").withStyle(color)).withStyle(net.minecraft.ChatFormatting.GOLD)));
            ItemStack lore=new ItemStack(Content.get("adherent_"+boss));if(!player.getInventory().add(lore))player.drop(lore,false);
            if(advancement!=null)player.getAdvancements().award(advancement,"defeated");
        }
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        super.dropCustomDeathLoot(source,looting,recentlyHit);
        if(kind==0){spawnAtLocation(Content.get("nightmare_scale"),24+random.nextInt(17));spawnAtLocation(Content.get("seelic_essence"),20+random.nextInt(36));}
        if(kind==1){
            int splinters=0,shards=0;for(int roll=0;roll<3;roll++)if(random.nextBoolean())splinters++;else shards+=12+random.nextInt(9);
            if(splinters>0)spawnAtLocation(Content.get("null_splinter"),splinters);if(shards>0)spawnAtLocation(Content.get("void_shard"),shards);
            spawnAtLocation(Content.get("seelic_essence"),20+random.nextInt(36));
        }
        if(kind==2){if(random.nextFloat()<.3)spawnAtLocation(Content.get("semblanceofdoom"));spawnAtLocation(Content.get("seelic_essence"),20+random.nextInt(36));}
        if(kind==3&&random.nextFloat()<.1)spawnAtLocation(Content.get("seelic_scythe"));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("NativePhase",phase());tag.putString("Animation",animation());tag.putInt("Remaining",remaining);tag.putInt("AttackAge",attackAge);tag.putInt("Cooldown",cooldown);
        if(staffId!=null)tag.putUUID("Nullpoint",staffId);tag.putBoolean("StaffBroken",staffBroken);tag.putBoolean("StaffShield",staffShield);tag.putInt("BossInvulnerable",bossInvulnerableTicks);if(Double.isFinite(sorcererGroundY))tag.putDouble("SorcererGroundY",sorcererGroundY);
        if(meteorTearAge>=0&&meteorTear!=null&&meteorLanding!=null){tag.putInt("MeteorTearAge",meteorTearAge);putVec(tag,"MeteorTear",meteorTear);putVec(tag,"MeteorLanding",meteorLanding);tag.putInt("MeteorWaves",meteorWavesEmitted);}
        if(hailAge>=0&&hailCenter!=null){tag.putInt("HailAge",hailAge);putVec(tag,"HailCenter",hailCenter);tag.putIntArray("HailDelay",hailDelay);}
        tag.putInt("SerpentState",serpentState);tag.putInt("SerpentAttack",serpentAttack);tag.putInt("SerpentAge",serpentAge);
        if(serpentGoal!=null){tag.putDouble("SerpentGoalX",serpentGoal.x);tag.putDouble("SerpentGoalY",serpentGoal.y);tag.putDouble("SerpentGoalZ",serpentGoal.z);}
        tag.putInt("ParticipantCount",participants.size());int participantIndex=0;for(UUID id:participants)tag.putUUID("Participant"+(participantIndex++),id);tag.putBoolean("DefeatRewards",defeatRewardsGiven);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);entityData.set(PHASE,Math.max(1,Math.min(kind==2?4:kind==1?6:1,tag.getInt("NativePhase"))));
        // Resume safely at idle after reload: an interrupted attack cannot hit twice.
        animate(idle(),0);attack=null;cooldown=tag.contains("Cooldown")?tag.getInt("Cooldown"):40;
        if(tag.hasUUID("Nullpoint"))staffId=tag.getUUID("Nullpoint");staffBroken=tag.getBoolean("StaffBroken");staffShield=tag.contains("StaffShield")?tag.getBoolean("StaffShield"):tag.hasUUID("Nullpoint")&&(phase()&1)==1;bossInvulnerableTicks=tag.getInt("BossInvulnerable");if(tag.contains("SorcererGroundY"))sorcererGroundY=tag.getDouble("SorcererGroundY");
        if(tag.contains("MeteorTearAge")&&tag.contains("MeteorTearX")&&tag.contains("MeteorLandingX")){meteorTearAge=tag.getInt("MeteorTearAge");meteorTear=readVec(tag,"MeteorTear");meteorLanding=readVec(tag,"MeteorLanding");meteorWavesEmitted=tag.getInt("MeteorWaves");}
        if(tag.contains("HailAge")&&tag.contains("HailCenterX")){hailAge=tag.getInt("HailAge");hailCenter=readVec(tag,"HailCenter");int[] saved=tag.getIntArray("HailDelay");System.arraycopy(saved,0,hailDelay,0,Math.min(saved.length,hailDelay.length));}
        serpentState=tag.getInt("SerpentState");serpentAttack=tag.getInt("SerpentAttack");serpentAge=tag.getInt("SerpentAge");
        if(tag.contains("SerpentGoalX"))serpentGoal=new Vec3(tag.getDouble("SerpentGoalX"),tag.getDouble("SerpentGoalY"),tag.getDouble("SerpentGoalZ"));
        participants.clear();for(int index=0;index<tag.getInt("ParticipantCount");index++)if(tag.hasUUID("Participant"+index))participants.add(tag.getUUID("Participant"+index));defeatRewardsGiven=tag.getBoolean("DefeatRewards");
    }
    @Override public boolean removeWhenFarAway(double distance){return kind>=3;}
    private static void putVec(CompoundTag tag,String key,Vec3 at){tag.putDouble(key+"X",at.x);tag.putDouble(key+"Y",at.y);tag.putDouble(key+"Z",at.z);}
    private static Vec3 readVec(CompoundTag tag,String key){return new Vec3(tag.getDouble(key+"X"),tag.getDouble(key+"Y"),tag.getDouble(key+"Z"));}
    @Override public boolean isMultipartEntity(){return kind==0;}
    @Override public PartEntity<?>[] getParts(){return parts;}
    @Override public void setId(int id){super.setId(id);if(parts!=null)for(int i=0;i<parts.length;i++)parts[i].setId(id+i+1);}
    private static final class SerpentPart extends PartEntity<NativeBoss> {
        SerpentPart(NativeBoss parent){super(parent);refreshDimensions();}
        @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(3,3);}
        @Override protected void defineSynchedData(){}
        @Override protected void readAdditionalSaveData(CompoundTag tag){}
        @Override protected void addAdditionalSaveData(CompoundTag tag){}
        @Override public boolean isPickable(){return true;}
        @Override public boolean hurt(DamageSource source,float amount){return getParent().hurt(source,amount);}
        @Override public boolean is(Entity entity){return this==entity||getParent()==entity;}
    }
}
