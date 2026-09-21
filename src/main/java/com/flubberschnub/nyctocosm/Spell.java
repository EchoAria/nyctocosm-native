package com.flubberschnub.nyctocosm;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import static com.flubberschnub.nyctocosm.Combat.Element.*;

/** Base values transcribed from spells:load in Nyctocosm v0.2.0. */
public enum Spell {
    LUMINESCENCE(Items.GLOWSTONE,0,6000,0,RADIANCE),
    RESONANCE(Items.ECHO_SHARD,60,0,80,Combat.Element.RESONANCE),
    EVANESCENCE(Items.ENDER_EYE,0,0,100,Combat.Element.RESONANCE,VOID),
    INCURRENCE(Items.WITHER_ROSE,0,0,180,NIGHTMARE),
    ABHORRENCE(Items.COAL,15,30,40,NIGHTMARE),
    FORBIDDANCE(Items.GHAST_TEAR,0,35,10,DISSONANCE),
    GRAVITANCE(Items.CRYING_OBSIDIAN,20,200,60,VOID),
    SUBSISTENCE(Items.APPLE,10,0,40,RADIANCE),
    REFLECTANCE(Items.IRON_INGOT,20,200,30,Combat.Element.RESONANCE),
    SUBMERGENCE(Items.SOUL_SAND,8,0,80,DREAM,VOID),
    ADAMANCE(Items.COBBLESTONE,0,200,20,VOID),
    INCANDESCENCE(Items.GUNPOWDER,10,40,20,RADIANCE,DISSONANCE),
    RESISTANCE(Items.COOKED_BEEF,0,250,40,RADIANCE),
    TRANSMITTANCE(Items.ENDER_PEARL,15,0,100,VOID),
    VACANCE(Items.GLASS_BOTTLE,0,100,20,RADIANCE),
    REPELLANCE(Items.FEATHER,0,50,50,DISSONANCE),
    FULMINANCE(Items.QUARTZ,7,20,30,RADIANCE,DISSONANCE);
    public final Item reagent;
    public final int damage, duration, castTicks;
    public final Combat.Element[] elements;
    Spell(Item reagent,int damage,int duration,int castTicks,Combat.Element... elements) {
        this.reagent=reagent; this.damage=damage; this.duration=duration; this.castTicks=castTicks; this.elements=elements;
    }
    public static Spell from(Item item) {
        for (Spell spell:values()) if (spell.reagent==item) return spell;
        return null;
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
}
