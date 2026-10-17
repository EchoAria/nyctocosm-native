package com.flubberschnub.nyctocosm;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared source-equivalent selector, ownership and capacity rules for reflectable attacks. */
final class Reflectance {
    static final double SOURCE_RADIUS = 3;
    private static final double SOURCE_RADIUS_SQR = SOURCE_RADIUS * SOURCE_RADIUS;

    record Result(SpellArea reflector, Player owner, Vec3 direction) {}

    static Result tryReflect(ServerLevel level, ReflectableAttack attack) {
        if (attack.energyCost() <= 0) return null;
        Vec3 sample = attack.sample();
        AABB broadPhase = new AABB(sample, sample).inflate(SOURCE_RADIUS + 1.0e-4);
        List<SpellArea> nearby = new ArrayList<>(level.getEntitiesOfClass(SpellArea.class, broadPhase, area ->
            area.spell() == Spell.REFLECTANCE && !area.isRemoved()
                && area.distanceToSqr(sample) <= SOURCE_RADIUS_SQR + 1.0e-7));
        if (nearby.isEmpty()) return null;

        nearby.sort(Comparator.comparingDouble((SpellArea area) -> area.distanceToSqr(sample))
            .thenComparing(area -> area.getUUID().toString()));
        SpellArea selected = null;
        Player redirectedOwner = null;
        for (SpellArea area : nearby) {
            UUID reflectorOwner = area.ownerId();
            if (reflectorOwner == null || reflectorOwner.equals(attack.owner())
                || attack.alreadyReflected().contains(area.getUUID())) continue;
            Player candidate = area.reflectionOwner(level);
            if (candidate == null) continue;
            selected = area;
            redirectedOwner = candidate;
            break;
        }
        // The source only enters its reflection branch when at least one nearby reflector
        // belongs to a different owner; once entered it spends capacity on every nearby node.
        if (selected == null) return null;

        for (SpellArea area : nearby) area.consumeReflectance(attack.energyCost());
        return new Result(selected, redirectedOwner, selected.getLookAngle());
    }

    private Reflectance() {}
}
