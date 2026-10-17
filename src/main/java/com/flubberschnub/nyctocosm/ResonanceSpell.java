package com.flubberschnub.nyctocosm;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Resonance has its own penetrating 70-sample ray, rather than the generic blocked beam. */
public final class ResonanceSpell {
    public static final int RANGE=70;
    public static void fire(ServerLevel level,Player caster,Vec3 origin,Vec3 forward){
        var hit=new HashSet<java.util.UUID>();var checked=new HashSet<BlockPos>();
        for(int step=0;step<RANGE;step++){
            Vec3 at=origin.add(forward.scale(step));BlockPos block=BlockPos.containing(at);
            if(!level.hasChunkAt(block))continue;
            if(checked.add(block))NativePortalBlock.activate(level,block);
            for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(.5))){
                if(Combat.canHit(caster,target)&&hit.add(target.getUUID()))Combat.hit(caster,target,Spell.RESONANCE.damage,Combat.Element.RESONANCE);
            }
        }
        NativeNetwork.weaponEffect(caster,4,origin,origin.add(forward.scale(RANGE-1)),forward);
    }
    private ResonanceSpell(){}
}
