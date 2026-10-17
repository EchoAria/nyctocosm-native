package com.flubberschnub.nyctocosm;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The source uses LocalCoordinates and CommandSourceStack.facing's lookup-table angles. */
final class SorcererCastMath {
    record Rotation(float yaw,float pitch) { Vec3 forward(){return SerpentMotion.localForward(yaw,pitch);} }
    static Rotation facing(Vec3 delta){
        double horizontal=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
        float pitch=Mth.wrapDegrees((float)(-Mth.atan2(delta.y,horizontal)*57.2957763671875));
        float yaw=Mth.wrapDegrees((float)(Mth.atan2(delta.z,delta.x)*57.2957763671875)-90);
        return new Rotation(yaw,pitch);
    }
    static Rotation turn(float yaw,float pitch,Vec3 toward,double forwardWeight){
        Vec3 targetStep=facing(toward).forward().scale(.5);
        Vec3 sum=SerpentMotion.localForward(yaw,pitch).scale(forwardWeight).add(targetStep);
        // `facing entity @s eyes facing ^ ^ ^-1` consists of two vanilla facing operations.
        return facing(facing(sum.scale(-1)).forward().scale(-1));
    }
    private SorcererCastMath(){}
}
