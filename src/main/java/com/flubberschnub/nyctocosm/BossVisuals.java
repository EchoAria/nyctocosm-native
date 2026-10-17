package com.flubberschnub.nyctocosm;

/** Presentation exceptions explicitly requested by the player. */
public final class BossVisuals {
    public static boolean visibleRigBone(String rig,String animation,String bone){
        // One two-sided smear texture contains both unwanted entrance arc pieces.
        return !(rig.equals("ajsorcerer")&&animation.equals("initial_spawn")&&bone.equals("smear1"));
    }
    private BossVisuals(){}
}
