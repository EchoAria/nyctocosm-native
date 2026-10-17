package com.flubberschnub.nyctocosm;

import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Attack clocks which outlive a rig animation. No shared marker tags or cross-boss state. */
public final class EmissaryHazard extends Entity {
    enum Kind { METEOR_SHOWER, LIGHTNING_POINT, DIVINE_BLAST, GODRAYS, WINGBEAT }
    private Kind kind=Kind.METEOR_SHOWER;
    private UUID owner;
    private UUID damageOwner;
    private int age,missingOwnerTicks,godrayMask=15,wingbeatSteps;
    private Vec3 lastBeamEnd;
    public EmissaryHazard(EntityType<? extends EmissaryHazard> type,Level level){super(type,level);noPhysics=true;}
    static EmissaryHazard create(ServerLevel level,NativeBoss boss,LivingEntity target,Kind kind,Vec3 at){
        var hazard=Content.EMISSARY_HAZARD.get().create(level);hazard.owner=boss.getUUID();hazard.damageOwner=boss.getUUID();hazard.kind=kind;hazard.setPos(at);
        if(kind==Kind.DIVINE_BLAST||kind==Kind.GODRAYS){
            var players=level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,new AABB(at,at).inflate(100),p->sourceBeamPlayer(p)&&p.position().distanceToSqr(at)<=100*100);
            target=players.stream().min(kind==Kind.GODRAYS?java.util.Comparator.comparingDouble((net.minecraft.world.entity.player.Player p)->-p.position().distanceToSqr(at)):java.util.Comparator.comparingDouble(p->p.position().distanceToSqr(at))).orElse(null);
        }
        var rotation=target==null?new SorcererCastMath.Rotation(boss.getYRot(),boss.getXRot()):SorcererCastMath.facing(target.position().subtract(at));
        hazard.setYRot(rotation.yaw());hazard.setXRot(rotation.pitch());
        level.addFreshEntity(hazard);
        if(kind==Kind.GODRAYS){
            NativeEvents.resonanceSound(level,at,"emissary.attack.godrays.shoot",50,200);
            for(int spoke=0;spoke<4;spoke++){float yaw=hazard.getYRot()+45+spoke*90;Vec3 origin=at.add(SerpentMotion.localForward(yaw,0).scale(4));hazard.beam(level,boss,origin,SerpentMotion.localForward(yaw,hazard.getXRot()),40,true,true);}
        }
        return hazard;
    }
    static void lightningField(ServerLevel level,NativeBoss boss){
        float yaw=boss.getRandom().nextInt(360)-180;
        for(int distance=6;distance<=34;distance+=4)for(int spoke=0;spoke<4;spoke++)
            create(level,boss,null,Kind.LIGHTNING_POINT,boss.position().add(Vec3.directionFromRotation(0,yaw+45+90*spoke).scale(distance)));
        NativeEvents.sound(level,boss.position(),"emissary.attack.lightning_field.activate",5);
    }
    static void wingbeat(ServerLevel level,NativeBoss boss,LivingEntity target){
        // The source invokes Wingbeat at frame 23 and emits from its animated center locator.
        var locator=EmissaryDamagePose.sampleAt(boss,"phase1.attack.wingbeat","center",23);
        Vec3 center=EmissaryDamagePose.worldPosition(boss,locator);
        Vec3 forward=target.position().subtract(center).normalize();
        Vec3 left=new Vec3(forward.z,0,-forward.x).normalize();if(left.lengthSqr()<.001)left=new Vec3(1,0,0);
        Vec3 up=forward.cross(left).normalize();
        float yaw=(float)(Math.toDegrees(Math.atan2(forward.z,forward.x))-90);
        float pitch=(float)-Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,forward.y))));
        for(int row=0;row<4;row++)for(int column=0;column<4;column++){
            var node=create(level,boss,target,Kind.WINGBEAT,center.add(left.scale(-7.5+5*column)).add(up.scale(-7.5+5*row)));
            node.setYRot(yaw);node.setXRot(pitch);node.wingbeatSteps=boss.getRandom().nextInt(50);
        }
    }
    UUID ownerId(){return owner;}
    Vec3 lastBeamEndForTest(){return lastBeamEnd;}
    UUID damageOwnerIdForTest(){return damageOwner;}
    boolean counterable(){return kind!=Kind.METEOR_SHOWER;}
    boolean counterAt(Vec3 at){
        if(!counterable())return false;
        if(kind!=Kind.GODRAYS){if(distanceToSqr(at)>25)return false;discard();return true;}
        // The original origin is not a spell; Forbiddance removes nearby beam nodes independently.
        int previous=godrayMask;
        for(int spoke=0;spoke<4;spoke++)if(position().add(Vec3.directionFromRotation(0,getYRot()+45+spoke*90).scale(4)).distanceToSqr(at)<=25)godrayMask&=~(1<<spoke);
        return previous!=godrayMask;
    }
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel server))return;
        Entity found=owner==null?null:server.getEntity(owner);
        if(!(found instanceof NativeBoss boss)){
            // Owner and attack may be deserialized in either order; do not erase a saved cast immediately.
            if(++missingOwnerTicks>20)discard();return;
        }
        if(!boss.isAlive()){discard();return;}missingOwnerTicks=0;age++;
        switch(kind){
            case WINGBEAT -> tickWingbeat(server);
            case METEOR_SHOWER -> {
                if(age<=150&&age%15==0)shootMeteor(server,boss);
                if(age>=160)discard();
            }
            case LIGHTNING_POINT -> {
                if(age<=20)server.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY()+.15,getZ(),8,.8,.1,.8,.02);
                else{
                    impact(server,boss,position(),4,35,Combat.Element.DISSONANCE);
                    var lightning=EntityType.LIGHTNING_BOLT.create(server);lightning.setPos(position());lightning.setVisualOnly(true);server.addFreshEntity(lightning);
                    discard();
                }
            }
            case DIVINE_BLAST -> {
                if(age<24){server.sendParticles(ParticleTypes.END_ROD,getX(),getY(),getZ(),6,.4,.4,.4,.01);return;}
                if(age==24)NativeEvents.resonanceSound(server,position(),"emissary.attack.divine_blast.shoot",10,100);
                beam(server,boss,position(),SerpentMotion.localForward(getYRot(),getXRot()),80,true,false);
                if(age>=49)discard(); // Activated timer 1..26, inclusive.
            }
            case GODRAYS -> {
                var center=EmissaryDamagePose.sampleAt(boss,"phase4.attack.godrays","center",boss.animationTime(0));
                setPos(EmissaryDamagePose.worldPosition(boss,center).add(0,1,0));
                for(int spoke=0;spoke<4;spoke++)if((godrayMask&(1<<spoke))!=0){
                    Vec3 point=position().add(SerpentMotion.localForward(getYRot()+45+spoke*90,0).scale(4));
                    server.sendParticles(ParticleTypes.FLASH,point.x,point.y,point.z,1,0,0,0,0);
                    server.sendParticles(ParticleTypes.EXPLOSION,point.x,point.y,point.z,1,.1,.1,.1,0);
                }
                if(age<15)return;
                for(int step=0;step<3;step++){
                    setYRot(getYRot()+1);
                    for(int spoke=0;spoke<4;spoke++){
                        if((godrayMask&(1<<spoke))==0)continue;
                        float yaw=getYRot()+45+spoke*90;
                        Vec3 origin=position().add(SerpentMotion.localForward(yaw,0).scale(4));
                        beam(server,boss,origin,SerpentMotion.localForward(yaw,getXRot()),40,true,false);
                    }
                }
                if(age>=30)discard();
            }
        }
    }
    private void tickWingbeat(ServerLevel server){
        for(int step=0;step<3;step++){
            boolean expired=++wingbeatSteps>=200;
            var reflection=Reflectance.tryReflect(server,new ReflectableAttack(position(),damageOwner==null?owner:damageOwner,5));
            if(reflection!=null){
                damageOwner=reflection.owner().getUUID();
                setYRot(reflection.reflector().getYRot());setXRot(reflection.reflector().getXRot());
            }
            Vec3 next=position().add(getLookAngle());
            if(!server.hasChunkAt(BlockPos.containing(next))){discard();return;}
            setPos(next);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY(),getZ(),2,2,2,2,.02);
            server.sendParticles(ParticleTypes.FLAME,getX(),getY(),getZ(),2,2,2,2,0);
            var victims=server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,getBoundingBox().inflate(5),
                player->player.isAlive()&&!player.isSpectator()&&player.distanceToSqr(this)<=25&&!SpellArea.preventsForcedMovement(player));
            if(!victims.isEmpty()){
                for(var player:victims)player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION,20,30,false,false));
                NativeEvents.sound(server,position(),"emissary.attack.wingbeat.impact",5);
                server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),5,1,1,1,0);
                server.sendParticles(ParticleTypes.FLASH,getX(),getY(),getZ(),5,1,1,1,0);
            }
            // The source only lifts players on contact; walls/expiry alone do not explode or deal direct damage.
            if(expired||!NativeBoss.emissaryPassable(server,blockPosition())||!victims.isEmpty()){discard();return;}
        }
    }
    private void shootMeteor(ServerLevel server,NativeBoss boss){
        Vec3 at=position().add(random.nextInt(2000)*.02-20,random.nextInt(2000)*.02-20,random.nextInt(2000)*.02-20);
        LivingEntity target=server.getNearestPlayer(at.x,at.y,at.z,300,p->p instanceof LivingEntity living&&Combat.canHit(boss,living));
        if(target==null&&Combat.canHit(boss,boss.getTarget()))target=boss.getTarget();
        if(target==null)return;
        Vec3 aim=target.position().subtract(at).normalize();
        float yaw=(float)(Math.toDegrees(Math.atan2(aim.z,aim.x))-90)+random.nextInt(30)-15;
        float pitch=(float)-Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,aim.y))))-10+random.nextInt(30)-15;
        SpellBolt.launch(server,boss,at,Vec3.directionFromRotation(pitch,yaw),40,Combat.Element.DISSONANCE,2,3)
            .effect(SpellBolt.Effect.HOMING,61).homeOn(target).homingTurn(2f/29).impactSound("emissary.attack.meteor_shower.impact");
        NativeEvents.sound(server,at,"emissary.attack.meteor_shower.shoot",5);
        server.sendParticles(ParticleTypes.EXPLOSION,at.x,at.y,at.z,3,.5,.5,.5,0);
    }
    private static boolean sourceBeamPlayer(net.minecraft.world.entity.player.Player player){return !player.isSpectator()&&!player.getTags().contains("emissarydebug");}
    private void beam(ServerLevel server,LivingEntity caster,Vec3 origin,Vec3 direction,float damage,boolean visual,boolean signal){
        Vec3 point=origin;
        var samples=new java.util.ArrayList<SorcererBeamNode.Sample>();
        var reflected=new HashSet<UUID>();
        for(int step=0;step<70;step++){
            if(!server.hasChunkAt(BlockPos.containing(point)))break;
            boolean blocked=!NativeBoss.emissaryPassable(server,BlockPos.containing(point));
            var hitBox=new AABB(point,point).inflate(.5);
            boolean hit=false;
            for(var victim:server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,hitBox))if(sourceBeamPlayer(victim)){hit=true;break;}
            if(blocked||hit||step==69)break;
            var rotation=SorcererCastMath.facing(direction);samples.add(new SorcererBeamNode.Sample(point,rotation.yaw(),rotation.pitch()));
            var reflection=signal?null:Reflectance.tryReflect(server,new ReflectableAttack(point,caster.getUUID(),damage,reflected));
            if(reflection!=null){
                SpellArea reflector=reflection.reflector();
                reflected.add(reflector.getUUID());caster=reflection.owner();direction=SerpentMotion.localForward(reflector.getYRot(),reflector.getXRot());
            }
            point=point.add(direction);
        }
        lastBeamEnd=point;
        if(visual)NativeNetwork.emissaryRay(this,samples,point,damage>=80,signal);
        if(signal)return;
        for(var victim:server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,new AABB(point,point).inflate(4+1e-4)))
            if(sourceBeamPlayer(victim)&&victim.hurtTime==0&&victim.position().distanceToSqr(point)<=16)Combat.hit(caster,victim,damage,Combat.Element.RADIANCE);
        NativeEvents.masterSound(server,point,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,1,30);
    }
    private static void impact(ServerLevel server,LivingEntity caster,Vec3 at,double radius,float damage,Combat.Element element){
        for(LivingEntity victim:server.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius)))
            if(victim.hurtTime==0&&Combat.distanceToBoxSqr(at,victim.getBoundingBox())<=radius*radius)Combat.hit(caster,victim,damage,element);
    }
    @Override protected void defineSynchedData(){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(owner!=null)tag.putUUID("Owner",owner);if(damageOwner!=null)tag.putUUID("DamageOwner",damageOwner);tag.putString("Kind",kind.name());tag.putInt("Age",age);tag.putInt("GodrayMask",godrayMask);tag.putInt("WingbeatSteps",wingbeatSteps);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;damageOwner=tag.hasUUID("DamageOwner")?tag.getUUID("DamageOwner"):owner;try{kind=Kind.valueOf(tag.getString("Kind"));}catch(IllegalArgumentException ignored){kind=Kind.METEOR_SHOWER;}age=Math.max(0,tag.getInt("Age"));godrayMask=tag.contains("GodrayMask")?tag.getInt("GodrayMask")&15:15;wingbeatSteps=Math.max(0,tag.getInt("WingbeatSteps"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
