package com.flubberschnub.nyctocosm;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Common math seam so renderer transforms can be checked on the dedicated test server. */
public final class RigTransforms {
    /** ArmorStandRenderer -> head pose -> CustomHeadLayer, before the item's HEAD transform. */
    public static Matrix4f armorStandHead(float yaw,float pitch,float headYaw,float roll){
        float radians=(float)(Math.PI/180);
        return new Matrix4f().rotateY((180-yaw)*radians)
            .scale(-1,-1,1).translate(0,-1.501f,0)
            .translate(0,1f/16,0).rotateZ(roll*radians).rotateY(headYaw*radians).rotateX(pitch*radians)
            .translate(0,-.25f,0).rotateY((float)Math.PI).scale(.625f,-.625f,-.625f);
    }
    public static Quaternionf orientation(float yaw){return orientation(yaw,0);}
    /** Emissary model local +Z follows Minecraft yaw/pitch; pitch tilts the whole rig around local X. */
    public static Quaternionf orientation(float yaw,float pitch){
        return new Quaternionf().rotationY((float)Math.toRadians(-yaw)).rotateX((float)Math.toRadians(pitch));
    }
    // DisplayRenderer applies this AFTER the exported bone transform, not at the rig root.
    public static Matrix4f itemTransform(Matrix4f bone){return new Matrix4f(bone).rotateY((float)Math.PI);}
    public static Matrix4f interpolate(Matrix4f a,Matrix4f b,float t){
        if(Math.abs(a.determinant())<1e-8||Math.abs(b.determinant())<1e-8)return new Matrix4f(a).lerp(b,t);
        Vector3f position=a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()),t);
        Vector3f scale=a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()),t);
        Quaternionf rotation=a.getUnnormalizedRotation(new Quaternionf()).slerp(b.getUnnormalizedRotation(new Quaternionf()),t);
        return new Matrix4f().translationRotateScale(position,rotation,scale);
    }
    private RigTransforms(){}
}
