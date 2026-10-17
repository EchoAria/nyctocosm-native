package com.flubberschnub.nyctocosm;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/** The source selector and ownership data for one Reflectance sample. */
record ReflectableAttack(Vec3 sample, UUID owner, float energyCost, Set<UUID> alreadyReflected) {
    ReflectableAttack(Vec3 sample, UUID owner, float energyCost) {
        this(sample, owner, energyCost, Set.of());
    }

    ReflectableAttack {
        alreadyReflected = alreadyReflected == null ? Set.of() : Set.copyOf(alreadyReflected);
    }
}
