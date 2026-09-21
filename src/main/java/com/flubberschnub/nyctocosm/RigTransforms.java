package com.flubberschnub.nyctocosm;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Common math seam so renderer transforms can be checked on the dedicated test server. */
public final class RigTransforms {
    public static Quaternionf orientation(float yaw){return new Quaternionf().rotationY((float)Math.toRadians(-yaw));}
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
