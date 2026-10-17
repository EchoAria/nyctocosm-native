package com.flubberschnub.nyctocosm;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.GameTestHolder;

/** Source-derived regressions for the alpha.18 combat repair. Excluded from release jars. */
@GameTestHolder(NativeMod.ID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class NativeGameTestsBossRepair {
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void sorcerer_ground_attack_keeps_source_seven_eight_twenty_gates(GameTestHelper helper){
        for(int phase:new int[]{2,4,6}){
            helper.assertTrue(NativeBoss.selectSorcererAttack(phase,7,0)!=null,"Ground close boundary was lost");
            helper.assertTrue(NativeBoss.selectSorcererAttack(phase,7.5,0)==null,"Source has no ground attack in (7,8)");
            helper.assertTrue(NativeBoss.selectSorcererAttack(phase,8,0)!=null,"Ground far boundary was lost");
            helper.assertTrue(NativeBoss.selectSorcererAttack(phase,20,0)!=null,"Ground far inclusive20 was lost");
            helper.assertTrue(NativeBoss.selectSorcererAttack(phase,20.001,0)==null,"Ground attack beyond source20 still starts");
        }
        helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void sorcerer_death_survives_source_hold_until_frame180(GameTestHelper helper){
        var boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(origin(helper));boss.setNoAi(true);helper.getLevel().addFreshEntity(boss);
        boss.hurt(helper.getLevel().damageSources().genericKill(),2000);
        for(int i=0;i<179;i++)boss.tickDeath();
        helper.assertTrue(!boss.isRemoved(),"Sorcerer was removed before source death frame180");
        boss.tickDeath();helper.assertTrue(boss.isRemoved(),"Sorcerer did not finish at source180");helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void emissary_seven_missiles_hold_position_before_activation23(GameTestHelper helper){
        var level=(ServerLevel)helper.getLevel();Vec3 at=origin(helper);
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(at);level.addFreshEntity(boss);
        var player=player(helper,at.add(0,0,35));boss.startEmissaryAttackForTest(player,"phase3.attack.seeking_missiles");
        for(int frame:new int[]{2,4,6,8,10,12,14})frame(boss,level,player,frame);
        List<Entity> missiles=new ArrayList<>();
        for(Entity entity:level.getAllEntities())if(entity instanceof SpellBolt||entity.getType().toString().contains("emissary_seeking_missile"))
            if(entity.distanceToSqr(at)<40*40)missiles.add(entity);
        helper.assertTrue(missiles.size()==7,"Source seven suspended missiles were not created: "+missiles.size());
        for(Entity missile:missiles){Vec3 before=missile.position();for(int i=0;i<8;i++)missile.tick();
            helper.assertTrue(missile.position().distanceToSqr(before)<1e-10,"Missile moved before source activation frame23");}
        missiles.forEach(Entity::discard);player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void emissary_frame23_activates_all_seven_nodes_and_save_keeps_life(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(at);level.addFreshEntity(boss);
        var player=player(helper,at.add(0,0,35));boss.startEmissaryAttackForTest(player,"phase3.attack.seeking_missiles");
        for(int tick:new int[]{2,4,6,8,10,12,14})frame(boss,level,player,tick);
        var missiles=level.getEntitiesOfClass(EmissarySeekingMissile.class,new AABB(at,at).inflate(40));
        frame(boss,level,player,22);helper.assertTrue(missiles.stream().noneMatch(EmissarySeekingMissile::active),"Missiles activated at22");
        frame(boss,level,player,23);helper.assertTrue(missiles.size()==7&&missiles.stream().allMatch(m->m.active()&&m.remainingForTest()>=60&&m.remainingForTest()<=89),"Source activation23/life60..89 missing");
        var original=missiles.get(0);original.tick();var tag=new CompoundTag();original.saveWithoutId(tag);
        var restored=Content.EMISSARY_SEEKING_MISSILE.get().create(level);restored.load(tag);
        helper.assertTrue(restored.active()&&restored.remainingForTest()==original.remainingForTest()&&restored.ownerId().equals(boss.getUUID())&&restored.position().equals(original.position()),"Activated missile lost state during native save/load");
        int life=restored.remainingForTest();restored.activate();helper.assertTrue(restored.remainingForTest()==life,"Activation replay reset saved life");
        missiles.forEach(Entity::discard);restored.discard();player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void emissary_active_missile_two_substeps_use_nearest_player_and_vanilla_turn(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var boss=Content.EMISSARY.get().create(level);boss.setPos(at);boss.setNoAi(true);level.addFreshEntity(boss);
        var near=player(helper,at.add(10,0,0));var far=player(helper,at.add(0,0,20));var debug=player(helper,at.add(0,0,1));debug.addTag("emissarydebug");
        var missile=EmissarySeekingMissile.summon(level,boss,2);configure(missile,at,0,0,60);
        var marker=net.minecraft.world.entity.EntityType.MARKER.create(level);marker.setPos(at);level.addFreshEntity(marker);
        for(int i=0;i<2;i++){
            var stack=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(marker.position()).withRotation(new Vec2(marker.getXRot(),marker.getYRot())).withSuppressedOutput();
            Vec3 eyes=near.getEyePosition();String aim="execute anchored eyes facing "+eyes.x+" "+eyes.y+" "+eyes.z+" positioned ^ ^ ^0.5 rotated as @s positioned ^ ^ ^6 facing entity @s feet facing ^ ^ ^-1 positioned as @s run tp @s ~ ~ ~ ~ ~";
            level.getServer().getCommands().performPrefixedCommand(stack,aim);
            stack=stack.withPosition(marker.position()).withRotation(new Vec2(marker.getXRot(),marker.getYRot()));level.getServer().getCommands().performPrefixedCommand(stack,"tp @s ^ ^ ^0.7");
        }
        missile.tick();helper.assertTrue(missile.substepsForTest()==2&&missile.remainingForTest()==59,"Source two .7 substeps / one timer decrement changed: steps="+missile.substepsForTest()+", life="+missile.remainingForTest()+", removed="+missile.isRemoved()+", at="+missile.position());
        helper.assertTrue(missile.position().distanceTo(marker.position())<1e-5,"Nearest nondebug player / source6+.5 vanilla homing differs: "+missile.position()+" vs "+marker.position());
        missile.discard();marker.discard();near.discard();far.discard();debug.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void emissary_missile_endpoint_cube_hits_once_and_timeout_has_no_splash(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var boss=Content.EMISSARY.get().create(level);boss.setPos(at);boss.setNoAi(true);level.addFreshEntity(boss);
        var victim=player(helper,at.add(0,-.8,.7));var outside=player(helper,at.add(2,-.8,.7));
        var missile=EmissarySeekingMissile.summon(level,boss,2);
        // Use deterministic homing, with the victim's eyes on the source forward ray.
        victim.setPos(at.add(0,-victim.getEyeHeight(),.7));configure(missile,at,0,0,60);float health=victim.getHealth(),other=outside.getHealth();
        missile.tick();helper.assertTrue(missile.isRemoved()&&victim.getHealth()<health,"Endpoint cube failed to resolve source15 damage");
        helper.assertTrue(outside.getHealth()==other,"Missile incorrectly added area splash outside endpoint cube");float after=victim.getHealth();missile.tick();helper.assertTrue(victim.getHealth()==after,"Resolved missile dealt damage twice");
        victim.discard();outside.discard();var timeout=EmissarySeekingMissile.summon(level,boss,2);configure(timeout,at.add(0,12,0),0,0,1);timeout.tick();
        helper.assertTrue(timeout.isRemoved()&&timeout.substepsForTest()==1,"Source timeout must stop before second slowcast without impact");
        boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void emissary_missile_reflects_with_capacity_and_persists_damage_owner(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var boss=Content.EMISSARY.get().create(level);boss.setPos(at);boss.setNoAi(true);level.addFreshEntity(boss);
        var player=player(helper,at.add(0,-4,0));player.setYRot(90);var shield=SpellArea.create(level,player,at,Spell.REFLECTANCE);
        var missile=EmissarySeekingMissile.summon(level,boss,2);configure(missile,at,0,0,60);missile.tick();
        var tag=new CompoundTag();missile.saveWithoutId(tag);var shieldTag=new CompoundTag();shield.saveWithoutId(shieldTag);
        helper.assertTrue(tag.getUUID("DamageOwner").equals(player.getUUID()),"Reflected missile did not persist new damage owner");
        helper.assertTrue(Math.abs(shieldTag.getFloat("ReflectanceEnergy")-5)<1e-5,"Source15 reflection capacity was not spent exactly once");
        helper.assertTrue(missile.substepsForTest()==2&&!missile.isRemoved(),"Reflected projectile failed to continue source double slowcast: steps="+missile.substepsForTest()+", life="+missile.remainingForTest()+", at="+missile.position());
        missile.discard();shield.discard();player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void sorcerer_saved_death_resumes_hold_without_restarting(GameTestHelper helper){
        var level=helper.getLevel();var boss=Content.SORCERER.get().create(level);boss.setNoAi(true);boss.setPos(origin(helper));level.addFreshEntity(boss);boss.hurt(level.damageSources().genericKill(),2000);
        for(int i=0;i<140;i++)boss.tickDeath();var tag=new CompoundTag();boss.saveWithoutId(tag);boss.discard();
        var restored=Content.SORCERER.get().create(level);restored.load(tag);level.addFreshEntity(restored);
        helper.assertTrue(restored.animation().equals("die")&&restored.deathTime==140&&Math.abs(restored.animationTime(0)-140)<1e-5,"Saved death restarted or lost source hold phase");
        for(int i=0;i<39;i++)restored.tickDeath();helper.assertTrue(!restored.isRemoved(),"Saved death ended before180");restored.tickDeath();helper.assertTrue(restored.isRemoved(),"Saved death did not end at180");helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void combat_repair_particle_and_sound_source_rows_are_complete(GameTestHelper helper){
        helper.assertTrue(BossParticleEffects.cueCount("ajemissary","phase1_transition",46)==3,"Phase1 frame46 direct rows missing");
        helper.assertTrue(BossParticleEffects.particleCount("ajemissary","phase3.attack.ground_slam",7)==105,"Ground slam source lava5/stone100 missing");
        helper.assertTrue(BossParticleEffects.cueCount("combat_repair","repair_seeker_idle",0)==44&&BossParticleEffects.cueCount("combat_repair","repair_crucify",0)==82&&BossParticleEffects.cueCount("combat_repair","repair_emissary_final",0)==900,"Source seeker/cross/final helpers incomplete");
        helper.assertTrue(BossParticleEffects.cueCount("combat_repair","repair_voidplane",0)==180&&BossParticleEffects.cueCount("combat_repair","repair_voidplane",3)==460&&BossParticleEffects.cueCount("combat_repair","repair_voidplane",5)==360,"Voidplane source shockwave score1/4/6 missing");
        helper.assertTrue(BossParticleEffects.cueCount("combat_repair","repair_transmittance",0)==130&&BossParticleEffects.cueCount("combat_repair","repair_transmittance",3)==460&&BossParticleEffects.cueCount("combat_repair","repair_transmittance",5)==540,"Voidwave source transmittance shockwave missing");
        int sounds=0,helpers=0;for(int f=0;f<=180;f++){sounds+=BossEffectTimeline.soundCount("phase3_transition",f);helpers+=BossEffectTimeline.helperCount("die",f);}
        helper.assertTrue(sounds==9&&helpers==29&&BossEffectTimeline.soundCount("phase2_transition",60)==1,"Source phase/death event count lost");helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void staff_health_thresholds_and_serpent_tail_offsets_match_source(GameTestHelper helper){
        helper.assertTrue(NullpointStaff.ambientCount(300)==13&&NullpointStaff.ambientCount(259)==12&&NullpointStaff.ambientCount(20)==1&&NullpointStaff.ambientCount(19)==0,"Staff source20-point thresholds changed");
        var boss=Content.SERPENT.get().create(helper.getLevel());boss.setPos(origin(helper));boss.setYRot(0);boss.setXRot(0);
        helper.assertTrue(SerpentAmbientEffects.head(boss).distanceTo(boss.position().add(0,1.5,2))<1e-6,"Head mist lost source forward2 / worldY1.5");
        var tail=boss.serpentRenderSegment(41,1).add(0,1.5,0);var direction=SerpentMotion.localForward(boss.serpentSegmentYaw(41,1),boss.serpentSegmentPitch(41,1));
        for(int i=0;i<5;i++)helper.assertTrue(SerpentAmbientEffects.tail(boss,i).distanceTo(tail.add(direction.scale(-new int[]{2,6,10,14,18}[i])))<1e-6,"Tail mist source offset missing at"+i);boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void sorcerer_ground_real_ai_preserves_fixed_origin_and_gap(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var boss=Content.SORCERER.get().create(level);boss.setNoAi(true);boss.setPos(at);
        var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",2);tag.putInt("Cooldown",0);boss.load(tag);level.addFreshEntity(boss);
        var target=player(helper,at.add(0,0,7.5));boss.setTarget(target);
        for(int i=0;i<20;i++)boss.customServerAiStep();boss.saveWithoutId(tag);
        helper.assertTrue(boss.getNavigation().isDone()&&boss.position().distanceToSqr(at)<1e-10&&tag.getString("ActiveSorcererAttack").isEmpty(),"Ground gap drove vanilla chase or launched attack");
        target.setPos(at.add(0,0,8));boss.customServerAiStep();boss.saveWithoutId(tag);
        helper.assertTrue(!tag.getString("ActiveSorcererAttack").isEmpty(),"Ground source8 boundary did not start a real attack");
        target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void emissary_missile_owner_cleanup_is_scoped_and_chunk_unload_preserves_nodes(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var first=Content.EMISSARY.get().create(level);first.setPos(at);first.setNoAi(true);level.addFreshEntity(first);
        var second=Content.EMISSARY.get().create(level);second.setPos(at.add(5,0,0));second.setNoAi(true);level.addFreshEntity(second);
        var node=EmissarySeekingMissile.summon(level,first,2);var other=EmissarySeekingMissile.summon(level,second,2);
        first.discard();helper.assertTrue(node.isRemoved()&&!other.isRemoved(),"Owner removal leaked missile or removed another encounter's node");
        second.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);helper.assertTrue(!other.isRemoved(),"Chunk unload destroyed a saved suspended node");other.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void emissary_ground_strafe_does_not_invent_unresolved_skid_callback(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var player=player(helper,at.add(0,0,40));
        for(String side:new String[]{"left","right"}){
            var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(at);level.addFreshEntity(boss);boss.startEmissaryAttackForTest(player,"phase3.attack.strafe"+side);
            Vec3 before=boss.position();frame(boss,level,player,9);helper.assertTrue(Math.abs(boss.position().distanceTo(before)-1)<1e-6,"Source strafe frame9 lost one-block dash");
            before=boss.position();frame(boss,level,player,10);frame(boss,level,player,14);helper.assertTrue(boss.position().distanceToSqr(before)<1e-10,"Unresolved source dash_skid must not invent .3 movement");boss.discard();
        }
        player.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_repair18",timeoutTicks=40)
    public static void forbiddance_counters_new_missile_after_three_ticks_and_respects_reflected_owner(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=origin(helper);var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(at);level.addFreshEntity(boss);
        var caster=player(helper,at.add(-10,0,0));var enemy=EmissarySeekingMissile.summon(level,boss,2);enemy.setPos(at.add(2,0,0));
        var own=EmissarySeekingMissile.summon(level,boss,4);var tag=new CompoundTag();own.saveWithoutId(tag);tag.putUUID("DamageOwner",caster.getUUID());own.load(tag);own.setPos(at.add(0,0,2));
        var counter=SpellBolt.launch(level,caster,at,Vec3.ZERO,0,Combat.Element.DISSONANCE,0,0).effect(SpellBolt.Effect.COUNTER,35);
        counter.tick();counter.tick();helper.assertTrue(!enemy.isRemoved()&&!own.isRemoved(),"Forbiddance started before source third tick");counter.tick();
        helper.assertTrue(enemy.isRemoved()&&counter.isRemoved()&&!own.isRemoved(),"New missile lost counterspell support or same-owner reflected missile was canceled");
        own.discard();caster.discard();boss.discard();helper.succeed();
    }
    static void configure(EmissarySeekingMissile missile,Vec3 at,float yaw,float pitch,int remaining){
        var state=new CompoundTag();missile.saveWithoutId(state);state.putBoolean("Active",true);state.putInt("Remaining",remaining);state.putInt("Substeps",0);missile.load(state);missile.setPos(at);missile.setYRot(yaw);missile.setXRot(pitch);
    }
    static void frame(NativeBoss boss,ServerLevel level,Player target,int frame){
        try{var age=NativeBoss.class.getDeclaredField("attackAge");age.setAccessible(true);age.setInt(boss,frame);
            var perform=NativeBoss.class.getDeclaredMethod("performEmissary",ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);perform.invoke(boss,level,target);
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static int areaIndex;
    static Vec3 origin(GameTestHelper helper){
        // Each fixture must be farther apart than the source's 300-block player selector.
        Vec3 at=new Vec3(300000+(areaIndex++)*1024+.5,240,300000.5);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)helper.getLevel().getChunkAt(BlockPos.containing(at).offset(x*16,0,z*16));
        return at;
    }
    static Player player(GameTestHelper helper,Vec3 at){var player=helper.makeMockSurvivalPlayer();player.setPos(at);player.setNoGravity(true);helper.getLevel().addFreshEntity(player);return player;}
    private NativeGameTestsBossRepair(){}
}
