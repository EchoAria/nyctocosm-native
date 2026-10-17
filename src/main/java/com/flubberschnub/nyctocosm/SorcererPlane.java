package com.flubberschnub.nyctocosm;

import org.joml.Matrix4f;

/** Original armor-stand helmet pipeline, before the resource's HEAD item transform. */
public final class SorcererPlane {
    public static Matrix4f headTransform(float yaw,float roll){
        return RigTransforms.armorStandHead(yaw,.01f,.01f,roll);
    }
    public static double riseStep(int frame){return frame>=59&&frame<=114?.08:frame>=44&&frame<=126?.05:0;}
    private SorcererPlane(){}
}
