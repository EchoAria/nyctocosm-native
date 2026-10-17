package com.flubberschnub.nyctocosm;

import java.util.function.IntUnaryOperator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Source-specific creation rules for player-owned Hypertraces. */
final class HypertraceService {
    private static final int BASE_GLOBAL_CHANCE = 15;
    private static final int BASE_ACCUMULATION_BONUS = 10;

    private HypertraceService() {}

    static SeelicField onSpellStarted(ServerLevel level, Player caster, Spell spell, IntUnaryOperator roll) {
        if (spell == null) return null;
        // All 17 source cast functions roll from global tracechance before their spell resolves.
        Vec3 origin = caster.position().add(caster.getLookAngle().scale(3));
        Combat.Element element = spell.elements.length == 0 ? Combat.Element.DREAM : spell.elements[0];
        return attempt(level, caster, origin, caster.getYRot(), element, chance(caster), roll);
    }

    static SeelicField onSpellHit(ServerLevel level, Player attacker, LivingEntity victim,
                                  Combat.Element element, IntUnaryOperator roll) {
        if (element != Combat.Element.NIGHTMARE && element != Combat.Element.VOID) return null;
        // The source block/58 and block/81 branches roll elemental + global tracechance,
        // then summon as the player attacker while retaining the victim's position context.
        return attempt(level, attacker, victim.position(), victim.getYRot(), element, chance(attacker), roll);
    }

    static int chance(Player owner) {
        int accumulation = NativeEvents.hasArtifact(owner, "accumulation")
            ? BASE_ACCUMULATION_BONUS * NativeEvents.traces(owner).size()
            : 0;
        // D22's non-default global-power modifier is not available in the native stat pipeline yet.
        return Math.min(100, BASE_GLOBAL_CHANCE + accumulation);
    }

    static boolean succeeds(int chance, IntUnaryOperator roll) {
        int value = roll.applyAsInt(100);
        if (value < 0 || value >= 100) throw new IllegalArgumentException("Trace roll must be in [0, 99]");
        return value <= chance;
    }

    private static SeelicField attempt(ServerLevel level, Player owner, Vec3 context, float yaw,
                                       Combat.Element element, int chance, IntUnaryOperator roll) {
        if (!succeeds(chance, roll)) return null;
        SeelicField trace = Content.FIELD.get().create(level);
        if (trace == null) return null;
        trace.configure(owner, false, true);
        trace.setElement(element.ordinal());
        trace.setPos(context.x, context.y + 1, context.z);
        trace.setYRot(yaw);
        trace.setXRot(0);
        trace.yRotO = yaw;
        trace.xRotO = 0;
        level.addFreshEntity(trace);
        return trace;
    }
}
