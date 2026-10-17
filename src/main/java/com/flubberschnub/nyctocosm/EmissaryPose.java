package com.flubberschnub.nyctocosm;

import net.minecraft.world.phys.Vec3;

/** Shared Emissary root-space transforms for server locators and the client rig pose. */
public final class EmissaryPose {
    public static Vec3 localPoint(Vec3 origin,float yaw,float pitch,Vec3 local){
        Vec3 forward=Vec3.directionFromRotation(pitch,yaw);
        double radians=Math.toRadians(yaw);
        Vec3 left=new Vec3(Math.cos(radians),0,Math.sin(radians));
        Vec3 up=forward.cross(left);
        return origin.add(left.scale(local.x)).add(up.scale(local.y)).add(forward.scale(local.z));
    }

    private EmissaryPose(){}
}
