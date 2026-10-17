package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Native values replacing the source's four persistent AI markers. See docs/SERPENT_BEHAVIOR.md. */
final class SerpentMotion {
    static final int ROAM=0, TWIRL=1, DIVE=2, LASER=3;
    private Vec3 area, current, toward, target;
    private float targetYaw;
    private int state, attack, age, passiveTimer, cooldown=100, interp=1, speed=2;
    private boolean twirling;
    private UUID targetId, lastSelectedTargetId;

    record Step(Vec3 position, Vec3 direction, boolean startedAttack, boolean startedDive, boolean laser, int speed) {}

    void initialize(Vec3 head, Vec3 look, RandomSource random) {
        if(area!=null)return;
        area=head;target=head;current=head.add(look.normalize().scale(30));toward=current;
        cooldown=100+random.nextInt(100);
    }
    void begin(Vec3 head, float yaw, float pitch, int type, UUID selectedTarget) {
        state=TWIRL;attack=type;age=0;twirling=true;speed=3;interp=3;targetId=selectedTarget;lastSelectedTargetId=selectedTarget;
        target=head.add(0,10,0).add(localForward(yaw,pitch).scale(10));
        targetYaw=Mth.wrapDegrees(yaw+90);
        advanceTwirl(); // The first loop body executes immediately inside attack/twirl.
    }
    void beginDive(Vec3 head, Vec3 look, Vec3 feet, UUID id) {
        state=DIVE;attack=1;age=0;speed=2;interp=2;twirling=false;targetId=id;
        target=feet;current=head.add(look.scale(30));toward=current;
    }
    Step tick(Vec3 head, Vec3 look, Vec3 liveTarget, UUID liveId, RandomSource random) {
        initialize(head,look,random);
        // headai reads LAST tick's currentfacing, then resets both facing markers.
        Vec3 previousFacing=current.subtract(head);
        Vec3 direction=facing(previousFacing,look);
        current=head.add(direction.scale(30));
        toward=head.add(facing(target.subtract(head),direction).scale(30));
        boolean started=false, dive=false, laser=false;
        if(state!=ROAM&&(liveTarget==null||head.distanceToSqr(area)>=200*200))resetToRoam();
        if(state==ROAM){
            if(--passiveTimer<=0){
                passiveTimer=20+random.nextInt(30);
                target=new Vec3(Math.floor(area.x*100),Math.floor(area.y*100),Math.floor(area.z*100)).add(random.nextInt(3000)*2-3000,
                    Math.floorDiv(random.nextInt(3000)*2-3000,10),random.nextInt(3000)*2-3000).scale(.01);
            }
            if(liveTarget!=null&&head.distanceToSqr(liveTarget)<=130*130&&--cooldown<=0){
                cooldown=100+random.nextInt(100);
                begin(head,facingYaw(previousFacing),facingPitch(previousFacing),1+random.nextInt(3),liveId);started=true;
            }
        }else if(state==TWIRL&&!twirling){
            age=0;
            if(attack==1){state=DIVE;interp=2;dive=true;}
            else if(attack==2){
                state=LASER;speed=1;interp=1;
                float facing=facingYaw(head.subtract(liveTarget));
                target=liveTarget.add(forward(facing).scale(20));targetYaw=Mth.wrapDegrees(facing+180);
            }else {resetToRoam();cooldown=20;}
        }else if(state==DIVE){
            if(++age>=400)resetToRoam();
        }
        // Laser uses a marker moving along a FIXED horizontal heading, not live player aim.
        if(state==LASER){
            age++;target=target.add(forward(targetYaw).scale(.5));laser=true;
            if(age>=100)resetToRoam(); // source still casts the final ray, at normal movement speed
        }
        int movementSpeed=speed;
        Vec3 next=head.add(direction.scale(speed));
        // ai/currentfacing runs after the head has moved. toward remains at its pre-move value.
        Vec3 correction=(interp==2?target:toward).subtract(current);
        double step=interp==3?6:interp==1?4:3;
        if(interp==0||correction.length()>=step+.1)current=current.add(facing(correction,direction).scale(step));
        // The live target marker is updated AFTER currentfacing's correction.
        if(state==DIVE&&liveTarget!=null)target=liveTarget;
        // Scheduled twirl functions run after the datapack tick. A newly started loop already ran.
        if(state==TWIRL&&twirling&&!started){
            if(age<61)advanceTwirl();
            else {twirling=false;speed=2;interp=1;}
        }
        return new Step(next,direction,started,dive,laser,movementSpeed);
    }
    private void advanceTwirl(){targetYaw=Mth.wrapDegrees(targetYaw+9);target=target.add(0,1,0).add(forward(targetYaw).scale(3));age++;}
    private void resetToRoam(){state=ROAM;age=0;speed=2;interp=1;twirling=false;targetId=null;}
    static Vec3 forward(float yaw){return localForward(yaw,0);}
    static float yaw(Vec3 direction){return (float)Math.toDegrees(Math.atan2(-direction.x,direction.z));}
    // Use the same float angles and trig table as LocalCoordinates/CommandSourceStack.facing.
    // Normalized double vectors drift from the command version over repeated marker updates.
    private static Vec3 facing(Vec3 delta,Vec3 fallback){
        if(delta.lengthSqr()<1e-12)return fallback;
        return localForward(facingYaw(delta),facingPitch(delta));
    }
    static float facingYaw(Vec3 delta){return Mth.wrapDegrees((float)(Mth.atan2(delta.z,delta.x)*57.2957763671875)-90);}
    static float facingPitch(Vec3 delta){return Mth.wrapDegrees((float)(-Mth.atan2(delta.y,delta.horizontalDistance())*57.2957763671875));}
    static Vec3 localForward(float yaw,float pitch){
        float r=(float)(Math.PI/180);
        float x=Mth.cos((yaw+90)*r),z=Mth.sin((yaw+90)*r),horizontal=Mth.cos(-pitch*r),y=Mth.sin(-pitch*r);
        return new Vec3(x*horizontal,y,z*horizontal);
    }
    int state(){return state;}
    int attack(){return attack;}
    Vec3 target(){return target;}
    UUID targetId(){return targetId;}
    void bindTarget(UUID id){targetId=id;lastSelectedTargetId=id;}
    UUID lastSelectedTargetId(){return lastSelectedTargetId;}

    CompoundTag save(){
        CompoundTag tag=new CompoundTag();if(area==null)return tag;
        tag.putInt("Version",1);put(tag,"Area",area);put(tag,"Current",current);put(tag,"Toward",toward);put(tag,"Target",target);
        tag.putFloat("TargetYaw",targetYaw);tag.putInt("State",state);tag.putInt("Attack",attack);tag.putInt("Age",age);
        tag.putInt("PassiveTimer",passiveTimer);tag.putInt("Cooldown",cooldown);tag.putInt("Interp",interp);tag.putInt("Speed",speed);
        if(lastSelectedTargetId!=null)tag.putUUID("LastSelectedTarget",lastSelectedTargetId);
        tag.putBoolean("Twirling",twirling);if(targetId!=null)tag.putUUID("TargetId",targetId);return tag;
    }
    void load(CompoundTag tag){
        if(tag.getInt("Version")!=1)return; // alpha.8 has no persistent marker chain: initialize safely at roam.
        area=get(tag,"Area");current=get(tag,"Current");toward=get(tag,"Toward");target=get(tag,"Target");
        targetYaw=tag.getFloat("TargetYaw");state=tag.getInt("State");attack=tag.getInt("Attack");age=tag.getInt("Age");
        passiveTimer=tag.getInt("PassiveTimer");cooldown=tag.getInt("Cooldown");interp=tag.getInt("Interp");speed=tag.getInt("Speed");
        lastSelectedTargetId=tag.hasUUID("LastSelectedTarget")?tag.getUUID("LastSelectedTarget"):null;
        twirling=tag.getBoolean("Twirling");targetId=tag.hasUUID("TargetId")?tag.getUUID("TargetId"):null;
    }
    private static void put(CompoundTag tag,String key,Vec3 p){CompoundTag v=new CompoundTag();v.putDouble("X",p.x);v.putDouble("Y",p.y);v.putDouble("Z",p.z);tag.put(key,v);}
    private static Vec3 get(CompoundTag tag,String key){CompoundTag v=tag.getCompound(key);return new Vec3(v.getDouble("X"),v.getDouble("Y"),v.getDouble("Z"));}
}
