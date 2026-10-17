package com.flubberschnub.nyctocosm;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.*;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import java.util.UUID;

@GameTestHolder(NativeMod.ID)
@PrefixGameTestTemplate(false)
public final class NativeGameTests {
    @GameTest(template="empty",batch="feedback_luminous_circle",timeoutTicks=1200)
    public static void luminous_circle_lasts_twenty_ticks_and_keeps_source_world_origin_after_load(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,240);
        inLoadedTestArea(helper,origin,24,()->{
            var player=encounterTestPlayer(helper,origin,net.minecraft.world.level.GameType.SURVIVAL);
            var field=new SeelicField(Content.FIELD.get(),level);field.configure(player,true,false);field.setPos(origin);field.beginCasting(Spell.LUMINESCENCE);level.addFreshEntity(field);
            try{var finish=SeelicField.class.getDeclaredMethod("finish",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.player.Player.class);finish.setAccessible(true);finish.invoke(field,level,player);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
            helper.assertTrue(field.luminousCircle().getInt("Left")==20&&field.spell()==null,"Instant Glowing erased its source20-tick circle");
            player.setPos(origin.add(5,0,0));field.tick();var tag=new CompoundTag();field.saveWithoutId(tag);field.discard();var restored=Content.FIELD.get().create(level);restored.load(tag);level.addFreshEntity(restored);
            helper.assertTrue(restored.luminousCircle().getInt("Left")==19&&restored.luminousCircle().getDouble("X")==origin.x,"Moving/loading plane moved or restarted its independently located circle");
            for(int tick=0;tick<19;tick++)restored.tick();helper.assertTrue(restored.luminousCircle().isEmpty(),"Luminescence circle persisted after its source timer expired");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_player_wing_hit",timeoutTicks=1200)
    public static void player_abhorrence_endpoint_hits_emissary_wing_part(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,240);
        inLoadedTestArea(helper,origin,40,()->{
            var owner=emissaryPlayer(helper,origin.add(-25,0,0));
            var boss=Content.EMISSARY.get().create(level);boss.setPos(origin);boss.setNoAi(true);level.addFreshEntity(boss);boss.tick();
            var part=boss.getParts()[0];helper.assertTrue(part.isPickable(),"Wing fixture has no active source part");Vec3 end=part.getBoundingBox().getCenter();float health=boss.getHealth();
            var bolt=SpellBolt.launch(level,owner,end.add(0,0,-1.5),new Vec3(0,0,1),15,Combat.Element.NIGHTMARE,1.5f,0).effect(SpellBolt.Effect.DAMAGE,30).playerAbhorrenceFlight();bolt.tick();
            helper.assertTrue(bolt.isRemoved()&&boss.getHealth()<health,"Player source endpoint carrier lost existing multipart wing hits");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_abhorrence_steps",timeoutTicks=1200)
    public static void player_abhorrence_moves_thirty_times_without_splash_damage(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,240);
        inLoadedTestArea(helper,origin,64,()->{
            var owner=emissaryPlayer(helper,origin.add(-10,0,0));
            var bolt=SpellBolt.launch(level,owner,origin.add(0,1,0),new Vec3(0,0,1),15,Combat.Element.NIGHTMARE,1.5f,2).effect(SpellBolt.Effect.DAMAGE,30).playerAbhorrenceFlight();
            for(int tick=0;tick<30;tick++)bolt.tick();
            helper.assertTrue(bolt.isRemoved()&&Math.abs(bolt.getZ()-origin.z-45)<1e-6,"Abhorrence did not move on its final source lifetime tick");
            var direct=EntityType.COW.create(level);direct.setPos(origin.add(0,0,1.5));direct.setNoAi(true);direct.setNoGravity(true);level.addFreshEntity(direct);
            var bystander=EntityType.COW.create(level);bystander.setPos(origin.add(1.2,0,1.5));bystander.setNoAi(true);bystander.setNoGravity(true);level.addFreshEntity(bystander);
            bolt=SpellBolt.launch(level,owner,origin.add(0,1,0),new Vec3(0,0,1),15,Combat.Element.NIGHTMARE,1.5f,2).effect(SpellBolt.Effect.DAMAGE,30).playerAbhorrenceFlight();bolt.tick();
            helper.assertTrue(bolt.isRemoved()&&direct.getHealth()<direct.getMaxHealth()&&bystander.getHealth()==bystander.getMaxHealth(),"Abhorrence expanded its endpoint selector into splash damage");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_fulminance_steps",timeoutTicks=1200)
    public static void player_fulminance_uses_six_samples_and_persists_profile(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,240);
        inLoadedTestArea(helper,origin,64,()->{
            var owner=emissaryPlayer(helper,origin.add(-10,0,0));
            var bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),7,Combat.Element.RADIANCE,9,0).effect(SpellBolt.Effect.LIGHTNING,20).playerFulminanceFlight();
            bolt.tick();helper.assertTrue(Math.abs(bolt.getZ()-origin.z-9)<1e-6,"Fulminance omitted its six1.5-block steps");
            var tag=new CompoundTag();bolt.saveWithoutId(tag);bolt.discard();var restored=Content.BOLT.get().create(level);restored.load(tag);level.addFreshEntity(restored);restored.tick();
            helper.assertTrue(restored.flightProfileForTest()==SpellBolt.FlightProfile.PLAYER_FULMINANCE&&Math.abs(restored.getZ()-origin.z-18)<1e-6,"Loaded Fulminance lost its flight profile");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_gravity_steps",timeoutTicks=1200)
    public static void player_gravitance_uses_feet_teleports_and_holds_in_blocked_voxel(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,240);
        inLoadedTestArea(helper,origin,32,()->{
            var owner=emissaryPlayer(helper,origin.add(-10,0,0));
            var target=EntityType.COW.create(level);target.setPos(origin.add(0,0,6.1));target.setNoAi(true);target.setNoGravity(true);level.addFreshEntity(target);
            var marker=EntityType.MARKER.create(level);marker.setPos(target.position());level.addFreshEntity(marker);
            var point=EntityType.MARKER.create(level);point.setPos(origin);point.addTag("feedback_gravity_point");level.addFreshEntity(point);
            var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withSuppressedOutput();
            level.getServer().getCommands().performPrefixedCommand(source,"execute at @s facing entity @e[tag=feedback_gravity_point,limit=1] feet run tp @s ^ ^ ^0.2");
            level.getServer().getCommands().performPrefixedCommand(source,"execute at @s facing entity @e[tag=feedback_gravity_point,limit=1] feet run tp @s ^ ^ ^0.4");
            var bolt=SpellBolt.launch(level,owner,origin,Vec3.ZERO,20,Combat.Element.VOID,0,0).effect(SpellBolt.Effect.GRAVITY,200).playerGravitanceFlight();bolt.tick();
            helper.assertTrue(target.position().distanceTo(marker.position())<1e-6&&target.getDeltaMovement().lengthSqr()==0,"Gravitance used center/velocity smoothing instead of sequential feet0.2/0.4 teleports");
            level.setBlock(BlockPos.containing(origin),Blocks.STONE.defaultBlockState(),2);bolt.setDeltaMovement(new Vec3(0,0,.3));bolt.tick();
            helper.assertTrue(bolt.position().equals(origin)&&bolt.getDeltaMovement().z==.3,"Gravitance moved inside a blocked current voxel or erased its source heading");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_emissary_player_ray",timeoutTicks=1200)
    public static void emissary_phase4_beam_ignores_npc_and_stops_at_player(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,240);
        inLoadedTestArea(helper,origin,80,()->{
            var boss=Content.EMISSARY.get().create(level);boss.setPos(origin);boss.setNoAi(true);level.addFreshEntity(boss);
            var target=emissaryPlayer(helper,origin.add(0,0,20));
            var cow=EntityType.COW.create(level);cow.setPos(origin.add(0,0,8));cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);
            var ray=EmissaryHazard.create(level,boss,target,EmissaryHazard.Kind.DIVINE_BLAST,origin.add(0,.7,0));
            for(int tick=0;tick<24;tick++)ray.tick();
            helper.assertTrue(ray.lastBeamEndForTest().z-origin.z>18&&target.getHealth()<200&&cow.getHealth()==cow.getMaxHealth(),"Phase4 ray was stopped by an NPC or expanded player-only impact to it");
            helper.assertTrue(BossParticleEffects.cueCount("feedback","divine_ray",0)==27&&BossParticleEffects.cueCount("feedback","god_ray",0)==3,"Source ray numeric helper is incomplete");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_player_origins",timeoutTicks=1200)
    public static void player_spell_default_launch_offsets_and_speeds_match_source(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,24,()->{
            var caster=encounterTestPlayer(helper,origin,net.minecraft.world.level.GameType.SURVIVAL);caster.setYRot(0);caster.setXRot(-30);
            var field=new SeelicField(Content.FIELD.get(),level);field.configure(caster,false,false);field.setPos(origin);level.addFreshEntity(field);
            try{
                var finish=SeelicField.class.getDeclaredMethod("finish",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.player.Player.class);finish.setAccessible(true);
                for(Spell spell:new Spell[]{Spell.ABHORRENCE,Spell.FORBIDDANCE,Spell.GRAVITANCE,Spell.INCANDESCENCE,Spell.FULMINANCE}){
                    field.beginCasting(spell);finish.invoke(field,level,caster);
                    var bolt=level.getEntitiesOfClass(SpellBolt.class,new AABB(origin,origin).inflate(15)).get(0);
                    Vec3 expected=origin.add(0,1.625,0).add(spell==Spell.INCANDESCENCE||spell==Spell.FULMINANCE?SerpentMotion.localForward(0,-30).scale(4):SerpentMotion.localForward(0,0).scale(3));
                    double speed=spell==Spell.GRAVITANCE?.3:spell==Spell.FORBIDDANCE?1.2:spell==Spell.FULMINANCE?9:1.5;
                    helper.assertTrue(bolt.position().distanceTo(expected)<1e-5&&Math.abs(bolt.getDeltaMovement().length()-speed)<1e-5,"Player "+spell+" origin or speed differs from its source transmutate/movement: "+bolt.position()+" speed="+bolt.getDeltaMovement().length());bolt.discard();
                }
            }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_variants")
    public static void emissary_source_sword_and_armor_variants_persist_per_bone(GameTestHelper helper){
        var state=EmissaryAppearance.initial(1);
        state=EmissaryAppearance.at(state,"phase1_transition",40);
        state=EmissaryAppearance.at(state,"phase2.attack.sword_plunge",0);
        helper.assertTrue(state.getString("sword").equals("swordless"),"Conjuring began with a visible hand sword");
        state=EmissaryAppearance.at(state,"phase2.attack.sword_plunge",10);
        helper.assertTrue(state.getString("sword").equals("swordful"),"Source frame10 did not conjure hand sword");
        state=EmissaryAppearance.at(state,"phase2.attack.sword_plunge",45);
        state=EmissaryAppearance.at(state,"phase2_idle",0);
        helper.assertTrue(state.getString("sword").equals("swordless"),"Idle restored the sword after source frame45 drop");
        state=EmissaryAppearance.at(state,"phase2_transition",30);
        helper.assertTrue(state.getString("head").equals("default"),"Ground melee transition stripped the source armor");
        state=EmissaryAppearance.at(state,"phase3_transition",57);
        helper.assertTrue(!state.getString("head").equals("armorless"),"Wing change stripped armor before frame171");
        state=EmissaryAppearance.at(state,"phase3_transition",171);
        helper.assertTrue(state.getString("head").equals("armorless")&&state.getString("sword").equals("armorless"),"Source armorless variant was not applied to its individual bones");helper.succeed();
    }
    @GameTest(template="empty",batch="feedback_variant_nbt",timeoutTicks=1200)
    public static void emissary_live_variant_drop_survives_idle_and_fresh_entity_load(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,220);
        inLoadedTestArea(helper,origin,24,()->{
            var boss=Content.EMISSARY.get().create(level);boss.setPos(origin);boss.setNoAi(true);
            CompoundTag tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",2);boss.load(tag);boss.setHealth(900);level.addFreshEntity(boss);
            var cow=EntityType.COW.create(level);cow.setPos(origin.add(0,0,15));cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);
            boss.startEmissaryAttackForTest(cow,"phase2.attack.sword_plunge");
            for(int i=0;i<10;i++)boss.customServerAiStep();helper.assertTrue(boss.emissaryModelVariant("sword").equals("swordful"),"Server did not synchronize source conjure frame10");
            for(int i=10;i<61;i++)boss.customServerAiStep();helper.assertTrue(boss.emissaryModelVariant("sword").equals("swordless"),"Hand sword remained after falling sword and idle");
            boss.saveWithoutId(tag);boss.discard();var restored=Content.EMISSARY.get().create(level);restored.load(tag);level.addFreshEntity(restored);
            helper.assertTrue(restored.emissaryModelVariant("sword").equals("swordless"),"Fresh loaded boss forgot its dropped sword");
            tag.putInt("NativePhase",3);tag.remove("EmissaryAppearance");restored.load(tag);
            helper.assertTrue(restored.emissaryModelVariant("head").equals("armorful"),"Ground melee form changed body material");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_flight_math",timeoutTicks=1200)
    public static void emissary_flight_rotation_matches_vanilla_execute_local_coordinates(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,32,()->{
            var marker=EntityType.MARKER.create(level);marker.setPos(origin);level.addFreshEntity(marker);
            var point=EntityType.MARKER.create(level);point.setPos(origin.add(14,6,-17));point.addTag("feedback_flight_point");level.addFreshEntity(point);
            for(boolean quick:new boolean[]{false,true})for(float yaw:new float[]{-170,0,73}){
                marker.setPos(origin);marker.setYRot(yaw);marker.setXRot(27);
                String construction="execute at @s facing entity @e[tag=feedback_flight_point,limit=1] feet positioned ^ ^ ^0.5 rotated as @s positioned ^ ^ ^%s facing entity @s feet facing ^ ^ ^-1 positioned as @s run tp @s ~ ~ ~ ~ ~";
                var command=level.getServer().getCommands();var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin).withSuppressedOutput();
                command.performPrefixedCommand(source,String.format(java.util.Locale.ROOT,construction,"25"));
                if(quick)command.performPrefixedCommand(source,String.format(java.util.Locale.ROOT,construction,"10"));
                var nativeTurn=NativeBoss.emissaryFlightRotation(yaw,27,point.position().subtract(origin),quick);
                helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(marker.getYRot()-nativeTurn.yaw()))<1e-4&&Math.abs(marker.getXRot()-nativeTurn.pitch())<1e-4,"Flight turn differs from vanilla command context: "+yaw+" quick="+quick);
            }
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_statuses")
    public static void luminance_refresh_and_resistance_preserve_other_attribute_modifiers(GameTestHelper helper){
        var player=helper.makeMockPlayer();var level=helper.getLevel();var attribute=player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        attribute.setBaseValue(.2);player.getPersistentData().putLong("NyctocosmResistanceUntil",level.getGameTime()+250);
        NativeEvents.tickSpellStatuses(player);
        helper.assertTrue(attribute.getValue()==1&&player.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE).getAmplifier()==0,"Resistance does not give source levelI and full temporary knockback resistance");
        player.getPersistentData().putLong("NyctocosmLuminous",level.getGameTime()+6000);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS,100));player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,100));
        NativeEvents.tickSpellStatuses(player);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)&&!player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)&&!player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"Luminance did not clear source darkness and blindness");
        player.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);NativeEvents.tickSpellStatuses(player);helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Active source luminous timer did not refresh Glowing");
        player.getPersistentData().putLong("NyctocosmResistanceUntil",level.getGameTime());NativeEvents.tickSpellStatuses(player);helper.assertTrue(Math.abs(attribute.getValue()-.2)<1e-9,"Resistance expiration overwrote unrelated knockback base");
        player.getPersistentData().putLong("NyctocosmLuminous",level.getGameTime());NativeEvents.tickSpellStatuses(player);helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Expired Luminance left permanent Glowing");helper.succeed();
    }
    @GameTest(template="empty",batch="feedback_particle_data")
    public static void focus_source_shockwave_and_radiance_numeric_cues_are_complete(GameTestHelper helper){
        helper.assertTrue(BossParticleEffects.cueCount("feedback","plane",0)==180&&BossParticleEffects.cueCount("feedback","plane",3)==460&&BossParticleEffects.cueCount("feedback","plane",5)==360,"Focus spellplane shockwave lost source1/4/6 score rings");
        helper.assertTrue(BossParticleEffects.cueCount("feedback","radiance",1)==144&&BossParticleEffects.cueCount("feedback","flight",0)==10,"Radiance transmutate or flight end-rod ring is incomplete");
        helper.assertTrue(BossParticleEffects.cueCount("feedback","corkscrew",35)==2,"Corkscrew source36-score trail lost final pair");helper.succeed();
    }
    @GameTest(template="empty",batch="feedback_sword_vertical",timeoutTicks=1200)
    public static void sword_plunge_source_display_has_no_random_root_tilt(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,220);
        inLoadedTestArea(helper,origin,24,()->{
            var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(origin);level.addFreshEntity(boss);
            var player=emissaryPlayer(helper,origin.add(0,0,8));
            var sword=EmissarySwordPlunge.createAtTarget(level,boss,player);
            helper.assertTrue(sword!=null&&sword.getXRot()==0&&sword.getYRot()==0,"Source block344 summons an unrotated ItemDisplay; giant sword acquired random root rotation");
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="feedback_flight_target",timeoutTicks=1200)
    public static void emissary_flight_keeps_waypoint_before_source_timer_then_retargets(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,80,()->{
            var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(origin);
            CompoundTag state=new CompoundTag();boss.saveWithoutId(state);state.putInt("NativePhase",4);state.putBoolean("EmissaryFlying",true);
            Vec3 point=origin.add(0,0,60);
            for(String axis:new String[]{"X","Y","Z"})state.putDouble("EmissaryFlyTarget"+axis,axis.equals("X")?point.x:axis.equals("Y")?point.y:point.z);
            state.putInt("EmissaryFlyTimer",2);boss.load(state);level.addFreshEntity(boss);
            boss.customServerAiStep();boss.saveWithoutId(state);
            helper.assertTrue(new Vec3(state.getDouble("EmissaryFlyTargetX"),state.getDouble("EmissaryFlyTargetY"),state.getDouble("EmissaryFlyTargetZ")).equals(point)&&state.getInt("EmissaryFlyTimer")==1,"Waypoint changed before block736's timer expired");
            boss.customServerAiStep();boss.saveWithoutId(state);
            helper.assertTrue(!new Vec3(state.getDouble("EmissaryFlyTargetX"),state.getDouble("EmissaryFlyTargetY"),state.getDouble("EmissaryFlyTargetZ")).equals(point)&&state.getInt("EmissaryFlyTimer")>=20&&state.getInt("EmissaryFlyTimer")<=59,"Reachable block737 did not retarget and reset20..59 at expiry");
            helper.succeed();
        });
    }
    private static void invokeSorcererKeyframe(NativeBoss boss,net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity target,String clip,int frame){
        for(var event:SorcererTimeline.at(clip,frame))fireSorcererEventForTest(boss,level,target,event);
    }
    private static Vec3 sourceDashDestination(GameTestHelper helper,Vec3 origin,Vec3 target){
        var level=helper.getLevel();var marker=EntityType.MARKER.create(level);marker.setPos(origin);level.addFreshEntity(marker);
        var stack=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin).withRotation(new net.minecraft.world.phys.Vec2(0,0)).withSuppressedOutput().facing(target);
        level.getServer().getCommands().performPrefixedCommand(stack,"execute rotated ~ 0 run tp @s ~ ~ ~ ~ ~");
        stack=stack.withRotation(new net.minecraft.world.phys.Vec2(marker.getXRot(),marker.getYRot()));level.getServer().getCommands().performPrefixedCommand(stack,"tp @s ^ ^ ^15");
        Vec3 point=marker.position();marker.discard();return point;
    }
    private static Vec3 sourceDashStep(GameTestHelper helper,Vec3 origin,Vec3 destination){
        var level=helper.getLevel();var marker=EntityType.MARKER.create(level);marker.setPos(origin);level.addFreshEntity(marker);
        var stack=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin).withSuppressedOutput().facing(destination);
        level.getServer().getCommands().performPrefixedCommand(stack,"tp @s ^ ^ ^1.5");Vec3 point=marker.position();marker.discard();return point;
    }
    @GameTest(template="empty",batch="sorcerer_dash_source",timeoutTicks=1200)
    public static void dash_strike_moves_toward_saved_world_marker_after_player_turns(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,36,()->{
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setNoAi(true);level.addFreshEntity(boss);var target=emissaryPlayer(helper,origin.add(0,0,20));boss.startSorcererAttackForTest(level,target,"dashstrike");
            Vec3 sourcePoint=sourceDashDestination(helper,origin,target.position()),expected=sourceDashStep(helper,origin,sourcePoint);
            invokeSorcererKeyframe(boss,level,target,"dashstrike",37);target.setPos(origin.add(20,0,0));invokeSorcererKeyframe(boss,level,target,"dashstrike",41);
            CompoundTag trace=new CompoundTag();boss.saveWithoutId(trace);
            helper.assertTrue(boss.position().distanceTo(expected)<1e-6&&boss.getDeltaMovement().lengthSqr()==0,"Dash Strike uses velocity/real-time player rather than source locked marker position: actual="+boss.position()+" expected="+expected+" lock="+trace.getCompound("SorcererDash"));helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_air_source",timeoutTicks=1200)
    public static void air_dash_rise_and_fixed_destination_match_source_keyframes(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,35,()->{
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setNoAi(true);level.addFreshEntity(boss);var target=emissaryPlayer(helper,origin.add(0,0,15));boss.startSorcererAttackForTest(level,target,"airdash");
            double y=origin.y;for(int frame=1;frame<=19;frame++){invokeSorcererKeyframe(boss,level,target,"airdash",frame);y+=frame>=5&&frame<=12?.6:frame>=17?.1:.3;helper.assertTrue(Math.abs(boss.getY()-y)<1e-6,"Air Dash root rise differs at source frame "+frame);}
            helper.assertTrue(Math.abs(boss.getY()-origin.y-7.5)<1e-6,"Air Dash ascent did not total source7.5 blocks");invokeSorcererKeyframe(boss,level,target,"airdash",20);Vec3 entry=boss.position();target.setPos(origin.add(20,0,0));
            invokeSorcererKeyframe(boss,level,target,"airdash",26);
            var marker=EntityType.MARKER.create(level);marker.setPos(entry);level.addFreshEntity(marker);
            var point=EntityType.MARKER.create(level);point.setPos(origin.add(0,0,15));point.addTag("d33_source_destination");level.addFreshEntity(point);
            var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(entry).withRotation(new net.minecraft.world.phys.Vec2(0,0)).withSuppressedOutput();
            level.getServer().getCommands().performPrefixedCommand(source,"execute facing entity @e[tag=d33_source_destination,limit=1] feet run tp @s ^ ^ ^1.5");
            helper.assertTrue(boss.position().distanceTo(marker.position())<1e-6&&boss.getDeltaMovement().lengthSqr()==0,"Air Dash retargeted moving player or used physical velocity instead of source1.5 tp");marker.discard();point.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_dash_hit_origin",timeoutTicks=1200)
    public static void dash_strike_damage_uses_function_entry_and_player_feet(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,36,()->{
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setNoAi(true);level.addFreshEntity(boss);var target=emissaryPlayer(helper,origin.add(0,0,20));boss.startSorcererAttackForTest(level,target,"dashstrike");invokeSorcererKeyframe(boss,level,target,"dashstrike",37);
            var rear=emissaryPlayer(helper,origin.add(0,0,-1.49));var ahead=emissaryPlayer(helper,origin.add(0,0,2.99));var cow=semblanceTestCow(level,origin,200);
            invokeSorcererKeyframe(boss,level,target,"dashstrike",41);
            helper.assertTrue(rear.getHealth()<200&&ahead.getHealth()==200&&cow.getHealth()==200,"Dash hit followed post-tp origin, body overlap or NPC area instead of entering-position player feet sphere");
            Vec3 before=boss.position();invokeSorcererKeyframe(boss,level,target,"dashstrike",50);helper.assertTrue(boss.position().equals(before),"Dash Strike frame50 made an extra movement");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_dash_final",timeoutTicks=1200)
    public static void dash_strike_frame49_keeps_two_source_selection_origins(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,36,()->{
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setNoAi(true);level.addFreshEntity(boss);var target=emissaryPlayer(helper,origin.add(0,0,20));boss.startSorcererAttackForTest(level,target,"dashstrike");invokeSorcererKeyframe(boss,level,target,"dashstrike",37);
            Vec3 expected=sourceDashDestination(helper,origin,target.position());var oldOrigin=emissaryPlayer(helper,origin.add(0,0,-.4));var finalOrigin=emissaryPlayer(helper,origin.add(0,0,15));invokeSorcererKeyframe(boss,level,target,"dashstrike",49);
            helper.assertTrue(oldOrigin.getHealth()<200&&finalOrigin.getHealth()<200&&boss.position().distanceTo(expected)<1e-6,"Dash49 lost entry+1.5/radius2 hit or final-marker/radius1.5 hit: oldHP="+oldOrigin.getHealth()+" finalHP="+finalOrigin.getHealth()+" pos="+boss.position()+" expected="+expected);helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_air_slam",timeoutTicks=1200)
    public static void air_dash_only_slams_at40_using_entering_position_and_once(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,36,()->{
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setNoAi(true);level.addFreshEntity(boss);var target=emissaryPlayer(helper,origin.add(0,0,10));boss.startSorcererAttackForTest(level,target,"airdash");invokeSorcererKeyframe(boss,level,target,"airdash",20);
            boss.setPos(origin.add(0,0,9));var along=emissaryPlayer(helper,boss.position());invokeSorcererKeyframe(boss,level,target,"airdash",26);invokeSorcererKeyframe(boss,level,target,"airdash",27);
            helper.assertTrue(along.getHealth()==200&&target.getHealth()==200,"Air Dash made an early slam or damages continuously during movement");
            Vec3 entry=origin.add(0,5,0);boss.setPos(entry);var hit=emissaryPlayer(helper,entry.add(3.99,0,0));var outside=emissaryPlayer(helper,entry.add(4.01,0,0));var cow=semblanceTestCow(level,entry,200);
            invokeSorcererKeyframe(boss,level,target,"airdash",40);float hp=hit.getHealth();
            helper.assertTrue(hp<200&&outside.getHealth()==200&&target.getHealth()==200&&cow.getHealth()==200&&boss.position().distanceTo(origin.add(0,0,10))<1e-6,"Air40 hit at final tp rather than function entering origin, widened feet radius or affected NPC");
            hit.invulnerableTime=0;invokeSorcererKeyframe(boss,level,target,"airdash",40);helper.assertTrue(hit.getHealth()==hp,"Air slam replayed after consuming the saved marker");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_dash_nbt",timeoutTicks=1200)
    public static void sorcerer_dash_lock_and_gravity_resume_from_nbt(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,36,()->{
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setNoAi(true);boss.setNoGravity(false);level.addFreshEntity(boss);var target=emissaryPlayer(helper,origin.add(0,0,20));boss.startSorcererAttackForTest(level,target,"dashstrike");invokeSorcererKeyframe(boss,level,target,"dashstrike",37);
            Vec3 expected=sourceDashStep(helper,origin,sourceDashDestination(helper,origin,target.position()));CompoundTag save=new CompoundTag();boss.saveWithoutId(save);save.putInt("AttackAge",40);save.putInt("AnimationAge",40);save.putInt("Remaining",31);boss.discard();target.setPos(origin.add(20,0,0));boss=Content.SORCERER.get().create(level);boss.load(save);level.addFreshEntity(boss);
            invokeSorcererKeyframe(boss,level,target,"dashstrike",41);helper.assertTrue(boss.position().distanceTo(expected)<1e-6&&boss.isNoGravity(),"Restored dash lost lock or restored gravity too early");
            invokeSorcererKeyframe(boss,level,target,"dashstrike",49);boss.startSorcererAttackForTest(level,target,"voidbeam");helper.assertTrue(!boss.isNoGravity(),"Dash did not restore pre-cast gravity on replacement");
            helper.assertTrue(BossParticleEffects.particleCount("ajsorcererm2","airdash",20)==321&&BossParticleEffects.particleCount("ajsorcererm2","airdash",26)==35,"Air Dash aim/flight helper geometry incomplete");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_beam_selector",timeoutTicks=1200)
    public static void sorcerer_ray_double_selection_box_matches_vanilla_dx_zero(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,40,()->{
            for(double x:new double[]{.54,.56}){
                var player=encounterTestPlayer(helper,origin.add(x,-1.7,0),net.minecraft.world.level.GameType.SURVIVAL);
                var marker=EntityType.MARKER.create(level);marker.setPos(origin);level.addFreshEntity(marker);
                var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin).withSuppressedOutput();
                level.getServer().getCommands().performPrefixedCommand(source,"execute positioned ~-0.25 ~-0.25 ~-0.25 as @a[dx=0] positioned ~-0.5 ~-0.5 ~-0.5 if entity @s[dx=0] run tag @s add source_beam_selector");
                boolean expected=player.getTags().contains("source_beam_selector");helper.assertTrue(expected==(x<.55),"Vanilla double cube boundary differs at "+x);
                var node=SorcererBeamNode.create(level,UUID.randomUUID(),origin,0,0,true);CompoundTag save=new CompoundTag();node.saveWithoutId(save);save.putInt("Age",69);save.putBoolean("Active",true);node.discard();node=Content.SORCERER_BEAM_NODE.get().create(level);node.load(save);level.addFreshEntity(node);node.attackTickForTest();
                helper.assertTrue(node.lastSamples().size()==(expected?1:60),"Native ray used player feet sphere or a single enlarged selection box at "+x+" count="+node.lastSamples().size());player.discard();marker.discard();
            }
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="void_beam_context",timeoutTicks=1200)
    public static void void_beam_multiplayer_facing_matches_vanilla_execute_forks(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,45,()->{
            encounterTestPlayer(helper,origin.add(10,0,10),net.minecraft.world.level.GameType.SURVIVAL);encounterTestPlayer(helper,origin.add(0,0,20),net.minecraft.world.level.GameType.SURVIVAL);
            var marker=EntityType.MARKER.create(level);marker.setPos(origin);marker.setYRot(90);level.addFreshEntity(marker);
            var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin).withRotation(new net.minecraft.world.phys.Vec2(0,90)).withSuppressedOutput();
            String turn="execute at @s anchored eyes facing entity @a[gamemode=!spectator,sort=nearest,distance=..40] eyes positioned ^ ^ ^0.5 rotated as @s positioned ^ ^ ^2 facing entity @s eyes facing ^ ^ ^-1 positioned as @s run tp @s ~ ~ ~ ~ ~";
            helper.assertTrue(level.getServer().getCommands().performPrefixedCommand(source,turn)>0,"Could not run source multiple-player Void Beam turn");
            var node=SorcererBeamNode.create(level,UUID.randomUUID(),origin,90,0,false);node.fireVoid(level);
            helper.assertTrue(Vec3.directionFromRotation(node.getXRot(),node.getYRot()).distanceTo(Vec3.directionFromRotation(marker.getXRot(),marker.getYRot()))<1e-6,
                "Void Beam cumulatively turns instead of following execute fork context: "+node.getYRot()+","+node.getXRot()+" expected "+marker.getYRot()+","+marker.getXRot());helper.succeed();
        });
    }
    @GameTest(template="empty",batch="halo_beam_lock",timeoutTicks=1200)
    public static void halo_beam_locks_at_65_fires_70_and_saves_independently(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,45,()->{
            var target=emissaryPlayer(helper,origin.add(0,0,10));var node=SorcererBeamNode.create(level,UUID.randomUUID(),origin,0,0,true);
            CompoundTag save=new CompoundTag();node.saveWithoutId(save);save.putInt("Age",64);node.discard();node=Content.SORCERER_BEAM_NODE.get().create(level);node.load(save);level.addFreshEntity(node);node.attackTickForTest();
            float yaw=node.getYRot(),pitch=node.getXRot();target.setPos(origin.add(20,0,0));node.attackTickForTest();
            helper.assertTrue(node.age()==66&&node.getYRot()==yaw&&node.getXRot()==pitch,"Halo continues aiming after its own score65");
            save=new CompoundTag();node.activateForTest();node.saveWithoutId(save);node.discard();target.discard();node=Content.SORCERER_BEAM_NODE.get().create(level);node.load(save);level.addFreshEntity(node);
            for(int age=67;age<=69;age++){node.attackTickForTest();helper.assertTrue(!node.isRemoved(),"Halo fired before its own score70");}node.attackTickForTest();
            helper.assertTrue(node.isRemoved()&&node.lastSamples().size()==60&&node.lastSamples().get(59).position().distanceTo(origin.add(SerpentMotion.localForward(yaw,pitch).scale(29.5)))<1e-6,"Halo restarted, shortened ray or skipped score70");
            var samples=node.lastSamples();node.attackTickForTest();helper.assertTrue(node.lastSamples()==samples,"Removed Halo replayed its shot");
            helper.assertTrue(SorcererBeamEffects.count("halobeaminitial")==35&&SorcererBeamEffects.count("halobeamfinal")==70&&SorcererBeamEffects.count("halobeambeam")==38,"Beam helper geometry incomplete");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_beam_ray",timeoutTicks=1200)
    public static void sorcerer_beam_ray_stops_at_first_player_or_wall_and_reflects_once(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,45,()->{
            var first=emissaryPlayer(helper,origin.add(0,-1,5));var behind=emissaryPlayer(helper,origin.add(0,-1,10));
            var node=SorcererBeamNode.create(level,UUID.randomUUID(),origin,0,0,true);CompoundTag save=new CompoundTag();node.saveWithoutId(save);save.putInt("Age",69);save.putBoolean("Active",true);node.discard();node=Content.SORCERER_BEAM_NODE.get().create(level);node.load(save);level.addFreshEntity(node);node.attackTickForTest();
            helper.assertTrue(first.getHealth()<200&&behind.getHealth()==200&&node.lastSamples().size()<20,"Ray pierced first player or replaced its selection cubes with feet sphere");first.discard();behind.discard();
            BlockPos wall=BlockPos.containing(origin.add(0,0,5));level.setBlock(wall,Blocks.STONE.defaultBlockState(),2);
            node=SorcererBeamNode.create(level,UUID.randomUUID(),origin,0,0,false);node.fireVoid(level);
            helper.assertTrue(node.lastSamples().size()==10&&BlockPos.containing(node.lastSamples().get(9).position()).equals(wall),"Ray omitted blocking sample or changed half-block step");level.setBlock(wall,Blocks.AIR.defaultBlockState(),2);node.discard();
            var owner=encounterTestPlayer(helper,origin.add(-10,0,0),net.minecraft.world.level.GameType.SURVIVAL);var shield=SpellArea.create(level,owner,origin,Spell.REFLECTANCE);shield.setYRot(-90);shield.setXRot(0);
            node=SorcererBeamNode.create(level,UUID.randomUUID(),origin,0,0,false);node.fireVoid(level);CompoundTag energy=new CompoundTag();shield.saveWithoutId(energy);
            helper.assertTrue(owner.getUUID().equals(node.damageOwnerId())&&Math.abs(energy.getFloat("ReflectanceEnergy")-17)<1e-6&&node.lastSamples().get(59).position().distanceTo(origin.add(29.5,0,0))<1e-5,"Ray lost reflection direction, owner or source3 cost");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_beam_counter",timeoutTicks=1200)
    public static void forbiddance_only_cancels_spell_tagged_halo_nodes(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,20,()->{
            UUID owner=UUID.randomUUID();var halo=SorcererBeamNode.create(level,owner,origin.add(.5,0,0),0,0,true);var voidNode=SorcererBeamNode.create(level,owner,origin.add(1,0,0),0,0,false);
            var caster=emissaryPlayer(helper,origin.add(-10,0,0));var counter=SpellBolt.launch(level,caster,origin,Vec3.ZERO,0,Combat.Element.RESONANCE,0,0).effect(SpellBolt.Effect.COUNTER,20);
            for(int i=0;i<3;i++)counter.tick();helper.assertTrue(halo.isRemoved()&&!voidNode.isRemoved(),"Forbiddance confused Halo/Void spell tags");halo.activateForTest();halo.attackTickForTest();helper.assertTrue(halo.lastSamples().isEmpty(),"Countered halo fired");
            CompoundTag save=new CompoundTag();voidNode.saveWithoutId(save);var restored=Content.SORCERER_BEAM_NODE.get().create(level);restored.load(save);helper.assertTrue(!restored.halo()&&owner.equals(restored.ownerId()),"Void restore changed tag or owner");restored.discard();
            SorcererBeamNode.finishVoid(level,owner);helper.assertTrue(voidNode.isRemoved(),"Source58 cleanup missed Void marker");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="sorcerer_beam_source",timeoutTicks=1200)
    public static void sorcerer_halo_and_void_beam_create_independent_source_markers(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,240);
        inLoadedTestArea(helper,origin,45,()->{
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setYRot(0);boss.setNoAi(true);level.addFreshEntity(boss);
            var near=emissaryPlayer(helper,origin.add(0,0,10));var far=emissaryPlayer(helper,origin.add(20,0,0));boss.startSorcererAttackForTest(level,near,"halobeam");
            try{
                var keyframe=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);keyframe.setAccessible(true);
                keyframe.invoke(boss,level,near,SorcererTimeline.at("halobeam",5).get(0));
                var halos=level.getEntities(boss,boss.getBoundingBox().inflate(10),e->e.getTags().contains("native_halo_node"));
                helper.assertTrue(halos.size()==1&&halos.get(0).position().distanceTo(origin.add(-5,2,0))<1e-6,"Halo frame five is still a target-dependent array, not a source root-relative node");
                boss.startSorcererAttackForTest(level,near,"voidbeam");keyframe.invoke(boss,level,near,SorcererTimeline.at("voidbeam",20).get(0));
                var voids=level.getEntities(boss,boss.getBoundingBox().inflate(10),e->e.getTags().contains("native_void_beam_node"));
                helper.assertTrue(voids.size()==2,"Void Beam aim has no limit and must create one independent marker per eligible player");
            }catch(ReflectiveOperationException exception){throw new IllegalStateException(exception);}
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="frozen_blades_arc",timeoutTicks=1200)
    public static void frozen_blades_vertical_arc_hits_once_per_substep_and_keeps_damage_events(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,20,()->{
            var samples=FrozenBlade.arc(origin,0,0);helper.assertTrue(samples.size()==12,"Blade lost its twelve source arc samples");
            for(int i=0;i<12;i++){
                var source=level.getServer().createCommandSourceStack().withLevel(level).withPosition(origin).withRotation(new net.minecraft.world.phys.Vec2(-60+i*10,0));
                Vec3 expected=new net.minecraft.commands.arguments.coordinates.LocalCoordinates(0,0,1.5).getPosition(source);
                helper.assertTrue(samples.get(i).distanceTo(expected)<1e-6,"Blade sample differs from source LocalCoordinates at angle "+(-60+i*10));
            }
            var edge=emissaryPlayer(helper,origin.add(0,0,3.5));var outside=emissaryPlayer(helper,origin.add(2.01,0,1.5));
            var npc=EntityType.COW.create(level);npc.setPos(origin.add(0,0,1.5));level.addFreshEntity(npc);float health=npc.getHealth();
            int[] attacks={0};Object cancel=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void hit(net.minecraftforge.event.entity.living.LivingAttackEvent event){if(event.getEntity()==edge){attacks[0]++;event.setCanceled(true);}}};
            var blade=FrozenBlade.create(level,UUID.randomUUID(),origin,0,0);MinecraftForge.EVENT_BUS.register(cancel);
            try{blade.attackTickForTest();}finally{MinecraftForge.EVENT_BUS.unregister(cancel);}
            helper.assertTrue(attacks[0]==2&&edge.getHealth()==200&&blade.isRemoved(),"Arc sampling duplicated damage within one hitbox, bypassed Forge or survived a source player hit: "+attacks[0]);
            helper.assertTrue(outside.getHealth()==200&&npc.getHealth()==health,"Blade uses generic radius or LivingEntity hit selection");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="frozen_blades_restore",timeoutTicks=1200)
    public static void frozen_blades_lifetime_block_reflection_counter_and_restore(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,48,()->{
            var blade=FrozenBlade.create(level,UUID.randomUUID(),origin,0,0);CompoundTag save=new CompoundTag();blade.saveWithoutId(save);save.putInt("Age",98);blade.discard();
            blade=Content.FROZEN_BLADE.get().create(level);blade.load(save);level.addFreshEntity(blade);blade.attackTickForTest();
            helper.assertTrue(!blade.isRemoved()&&blade.age()==99&&blade.position().distanceTo(origin.add(0,0,.4))<1e-6,"Saved blade restarted lifetime or changed source step");blade.attackTickForTest();
            helper.assertTrue(blade.isRemoved()&&blade.age()==100&&blade.position().distanceTo(origin.add(0,0,.8))<1e-6,"Source lifetime one hundred skipped its final two procasts");
            BlockPos block=BlockPos.containing(origin);level.setBlock(block,Blocks.STONE.defaultBlockState(),2);blade=FrozenBlade.create(level,UUID.randomUUID(),origin,0,0);blade.attackTickForTest();
            helper.assertTrue(blade.isRemoved()&&blade.position().distanceTo(origin.add(0,0,.4))<1e-6,"Blocking voxel prematurely stopped source procast cleanup");level.setBlock(block,Blocks.AIR.defaultBlockState(),2);
            var player=encounterTestPlayer(helper,origin.add(-10,0,0),net.minecraft.world.level.GameType.SURVIVAL);player.setYRot(-90);player.setXRot(0);
            var shield=SpellArea.create(level,player,origin.add(0,0,1),Spell.REFLECTANCE);shield.setYRot(-90);shield.setXRot(0);
            blade=FrozenBlade.create(level,UUID.randomUUID(),origin,0,0);blade.attackTickForTest();
            helper.assertTrue(player.getUUID().equals(blade.damageOwnerId())&&blade.position().distanceTo(origin.add(.4,0,0))<1e-6,"Blade did not reflect before its sole movement");
            CompoundTag energy=new CompoundTag();shield.saveWithoutId(energy);helper.assertTrue(Math.abs(energy.getFloat("ReflectanceEnergy")-6)<1e-6,"Blade reflected repeatedly within its two procasts");shield.discard();
            save=new CompoundTag();blade.saveWithoutId(save);blade.discard();var restored=Content.FROZEN_BLADE.get().create(level);restored.load(save);level.addFreshEntity(restored);
            helper.assertTrue(restored.age()==1&&player.getUUID().equals(restored.damageOwnerId()),"Blade restore lost reflected owner or age");
            var other=emissaryPlayer(helper,origin.add(-20,0,0));var counter=SpellBolt.launch(level,other,origin,Vec3.ZERO,0,Combat.Element.RESONANCE,0,0).effect(SpellBolt.Effect.COUNTER,20);
            for(int i=0;i<3;i++)counter.tick();helper.assertTrue(restored.isRemoved(),"Forbiddance could not remove source spell-tagged blade");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="frozen_blades_context",timeoutTicks=1200)
    public static void frozen_blades_bone_coordinates_and_recursive_turn_match_vanilla_commands(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,35,()->{
            var player=encounterTestPlayer(helper,origin.add(10,0,0),net.minecraft.world.level.GameType.SURVIVAL);
            var marker=EntityType.MARKER.create(level);marker.setPos(origin);level.addFreshEntity(marker);
            var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin)
                .withRotation(new net.minecraft.world.phys.Vec2(0,0)).withSuppressedOutput();var commands=level.getServer().getCommands();
            String turn="execute at @s anchored eyes facing entity @a[sort=nearest,distance=..50,gamemode=!spectator,limit=1] feet positioned ^ ^ ^0.5 rotated as @s positioned ^ ^ ^5 facing entity @s eyes facing ^ ^ ^-1 positioned as @s run tp @s ~ ~ ~ ~ ~";
            helper.assertTrue(commands.performPrefixedCommand(source,turn)>0,"Could not execute source Frozen Blades steering");
            helper.assertTrue(commands.performPrefixedCommand(source,"execute positioned ^ ^ ^0.4 run tp @s ~ ~ ~ ~ ~")>0,"Could not execute source recursive movement");
            helper.assertTrue(marker.position().distanceTo(origin.add(0,0,.4))<1e-6&&Math.abs(marker.getYRot())<1e-6,"Function entry rotation did not reset the first turn during movement");
            commands.performPrefixedCommand(source,turn);Vec3 expected=Vec3.directionFromRotation(marker.getXRot(),marker.getYRot());
            var blade=FrozenBlade.create(level,UUID.randomUUID(),origin,0,0);blade.attackTickForTest();
            helper.assertTrue(blade.position().distanceTo(marker.position())<1e-6&&Vec3.directionFromRotation(blade.getXRot(),blade.getYRot()).distanceTo(expected)<1e-6,"Native blade differs from source entry rotation: pos="+blade.position()+" expected="+marker.position()+" rot="+blade.getYRot()+","+blade.getXRot()+" expected="+marker.getYRot()+","+marker.getXRot());
            var display=EntityType.ITEM_DISPLAY.create(level);display.setPos(origin);level.addFreshEntity(display);
            var ds=source.withEntity(display);
            commands.performPrefixedCommand(ds,"data merge entity @s {transformation:[1f,0f,0f,20f,0f,1f,0f,12f,0f,0f,1f,-14f,0f,0f,0f,1f]}");
            helper.assertTrue(display.position().equals(origin),"ItemDisplay render matrix incorrectly changed command position");
            helper.assertTrue(commands.performPrefixedCommand(ds,"execute at @s positioned ~ ~3 ~ rotated ~90 ~20 positioned ^ ^ ^0.5 run summon marker ~ ~ ~ {Tags:[\"native_blade_bone_fixture\"]}")>0,"Could not execute source bone-origin fixture");
            var spawn=level.getEntitiesOfClass(net.minecraft.world.entity.Marker.class,new AABB(origin,origin).inflate(6),e->e.getTags().contains("native_blade_bone_fixture")).get(0);
            helper.assertTrue(spawn.position().distanceTo(origin.add(0,3,0).add(Vec3.directionFromRotation(20,90).scale(.5)))<1e-6,"Source bone throw unexpectedly used visual matrix translation");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="frozen_blades_source",timeoutTicks=1200)
    public static void frozen_blades_frame34_creates_two_source_wave_carriers(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,230);
        inLoadedTestArea(helper,origin,30,()->{
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setYRot(0);boss.setXRot(0);boss.setNoAi(true);level.addFreshEntity(boss);
            var target=emissaryPlayer(helper,origin.add(0,0,20));boss.startSorcererAttackForTest(level,target,"frozenblades");
            try{
                var keyframe=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
                keyframe.setAccessible(true);keyframe.invoke(boss,level,target,SorcererTimeline.at("frozenblades",34).get(0));
            }catch(ReflectiveOperationException exception){throw new IllegalStateException(exception);}
            var waves=level.getEntities(boss,boss.getBoundingBox().inflate(5),entity->entity.getTags().contains("native_frozen_blade"));
            helper.assertTrue(waves.size()==2,"Frozen Blades still creates ordinary straight bolts rather than two source arc carriers");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="shard_hail_lifecycle",timeoutTicks=1200)
    public static void shard_hail_hatches_eighteen_delayed_nodes_and_saves_each_stage(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,25,()->{
            UUID owner=UUID.randomUUID();var ball=SorcererHail.spawn(level,owner,origin,SorcererHail.Stage.BALL,29);
            CompoundTag save=new CompoundTag();ball.saveWithoutId(save);ball.discard();ball=Content.SORCERER_HAIL.get().create(level);ball.load(save);level.addFreshEntity(ball);
            ball.attackTickForTest();var nodes=level.getEntitiesOfClass(SorcererHail.class,new AABB(origin,origin).inflate(35),h->h.stage()==SorcererHail.Stage.NODE);
            helper.assertTrue(ball.isRemoved()&&nodes.size()==18,"Source timer thirty must hatch exactly eighteen independent nodes");
            for(var node:nodes)helper.assertTrue(node.timer()>=0&&node.timer()<40&&node.position().distanceTo(origin.add(0,-29,0))<1e-6&&!node.counterable(),"Node lost random initial score, moved origin or gained spell tag");
            var node=nodes.get(0);save=new CompoundTag();node.saveWithoutId(save);save.putInt("HailTimer",40);node.discard();node=Content.SORCERER_HAIL.get().create(level);node.load(save);level.addFreshEntity(node);
            node.attackTickForTest();var icicles=level.getEntitiesOfClass(SorcererHail.class,new AABB(origin,origin).inflate(40),h->h.stage()==SorcererHail.Stage.ICICLE);
            helper.assertTrue(icicles.size()==1&&icicles.get(0).position().distanceTo(node.position().add(0,30,0))<1e-6,"Score forty-one did not create one icicle thirty blocks above the node");
            var icicle=icicles.get(0);CompoundTag fallen=new CompoundTag();icicle.saveWithoutId(fallen);var restored=Content.SORCERER_HAIL.get().create(level);restored.load(fallen);
            helper.assertTrue(restored.stage()==SorcererHail.Stage.ICICLE&&!restored.isNoGravity()&&restored.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Content.SORCERER_ICICLE_VISUAL.get()),"Saved icicle lost gravity or source model");restored.discard();
            CompoundTag placed=new CompoundTag();node.saveWithoutId(placed);node.discard();node=Content.SORCERER_HAIL.get().create(level);node.load(placed);level.addFreshEntity(node);node.attackTickForTest();
            helper.assertTrue(level.getEntitiesOfClass(SorcererHail.class,new AABB(origin,origin).inflate(40),h->h.stage()==SorcererHail.Stage.ICICLE).size()==1,"Restored node repeated its already-fired icicle");
            for(int tick=42;tick<90;tick++)node.attackTickForTest();helper.assertTrue(node.isRemoved(),"Node did not expire at source score ninety");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="shard_hail_spread",timeoutTicks=1200)
    public static void shard_hail_late_spread_visits_all_sorted_player_centers(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,78,()->{
            for(int x=-76;x<=76;x++)for(int z=-20;z<=20;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var near=encounterTestPlayer(helper,origin.add(.5,0,0),net.minecraft.world.level.GameType.SURVIVAL);
            var far=encounterTestPlayer(helper,origin.add(25,0,0),net.minecraft.world.level.GameType.SURVIVAL);
            var node=SorcererHail.spawn(level,UUID.randomUUID(),origin,SorcererHail.Stage.NODE,39);node.attackTickForTest();
            helper.assertTrue(node.position().equals(origin),"Hail picked a player before score forty-one");far.moveTo(origin.x+50,origin.y,origin.z,0,0);
            var marker=EntityType.MARKER.create(level);marker.setPos(origin);level.addFreshEntity(marker);
            var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin).withSuppressedOutput();
            int result=level.getServer().getCommands().performPrefixedCommand(source,"execute at @a[distance=..80,gamemode=!spectator,sort=nearest] run spreadplayers ~ ~ 0 15 false @s");
            helper.assertTrue(result>0&&Math.abs(marker.getX()-far.getX())<=15.5&&Math.abs(marker.getZ()-far.getZ())<=15.5,"Vanilla selector did not execute all centers and end near the farthest player");
            node.attackTickForTest();Vec3 at=node.position();
            helper.assertTrue(at.x>=origin.x+35-1&&at.x<=origin.x+65+1&&Math.abs(at.z-origin.z)<=15.5&&at.y==200,"Native scatter ignored the moved final player or used a circular region: "+at);
            helper.assertTrue(near.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"Node selector lost its function-entry slow region after teleport");
            helper.assertTrue(at.x-Math.floor(at.x)==.5&&at.z-Math.floor(at.z)==.5,"Spread result was not quantized to block centers");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="shard_hail_physics",timeoutTicks=1200)
    public static void shard_hail_icicle_uses_vanilla_fall_and_second_ground_tick_void_hit(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,15,()->{
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)level.setBlock(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var icicle=SorcererHail.spawn(level,null,origin.add(0,30,0),SorcererHail.Stage.ICICLE,0);
            var vanilla=EntityType.ARMOR_STAND.create(level);vanilla.setPos(origin.add(4,30,0));vanilla.setInvisible(true);vanilla.setInvulnerable(true);level.addFreshEntity(vanilla);
            var edge=emissaryPlayer(helper,origin.add(2,0,0));var outside=emissaryPlayer(helper,origin.add(-2.01,0,0));
            var npc=EntityType.COW.create(level);npc.setPos(origin);level.addFreshEntity(npc);float npcHealth=npc.getHealth();
            int ground=0;boolean hit=false;
            for(int tick=0;tick<80&&!icicle.isRemoved();tick++){
                if(icicle.onGround())ground++;icicle.attackTickForTest();
                if(ground<2)helper.assertTrue(edge.getHealth()==200,"Icicle hit before its second grounded spell tick");
                if(ground==2){helper.assertTrue(edge.getHealth()<200&&outside.getHealth()==200&&npc.getHealth()==npcHealth,"Icicle source Void/player-feet radius two differs");hit=true;}
                if(icicle.isRemoved())break;
                icicle.tick();vanilla.tick();helper.assertTrue(Math.abs(icicle.getY()-vanilla.getY())<1e-8&&icicle.getDeltaMovement().distanceTo(vanilla.getDeltaMovement())<1e-8,"Icicle no longer follows vanilla ArmorStand gravity at tick "+tick);
            }
            helper.assertTrue(hit&&ground==5&&icicle.isRemoved(),"Icicle skipped impact or source five-ground-tick cleanup");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="shard_hail_counter",timeoutTicks=1200)
    public static void shard_hail_forbiddance_cancels_ball_but_preserves_untagged_nodes(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,230);
        inLoadedTestArea(helper,origin,15,()->{
            var caster=emissaryPlayer(helper,origin.add(-10,0,0));var foreign=UUID.randomUUID();
            var ball=SorcererHail.spawn(level,foreign,origin.add(.5,0,0),SorcererHail.Stage.BALL,29);
            var node=SorcererHail.spawn(level,foreign,origin.add(1,0,0),SorcererHail.Stage.NODE,0);
            var icicle=SorcererHail.spawn(level,foreign,origin.add(2,0,0),SorcererHail.Stage.ICICLE,0);
            var counter=SpellBolt.launch(level,caster,origin,Vec3.ZERO,0,Combat.Element.RESONANCE,0,0).effect(SpellBolt.Effect.COUNTER,20);
            for(int i=0;i<3;i++)counter.tick();helper.assertTrue(ball.isRemoved()&&!node.isRemoved()&&!icicle.isRemoved(),"Forbiddance must only cancel the spell-tagged HailBall");
            ball.attackTickForTest();helper.assertTrue(level.getEntitiesOfClass(SorcererHail.class,new AABB(origin,origin).inflate(40),h->h.stage()==SorcererHail.Stage.NODE).size()==1,"Countered ball still hatched eighteen nodes");helper.succeed();
        });
    }
    @GameTest(template="empty",batch="shard_hail_source",timeoutTicks=1200)
    public static void shard_hail_frame35_spawns_counterable_rising_ball(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,200);
        inLoadedTestArea(helper,origin,40,()->{
            var boss=Content.SORCERER.get().create(level);boss.setPos(origin);boss.setYRot(0);boss.setNoAi(true);level.addFreshEntity(boss);
            var target=emissaryPlayer(helper,origin.add(10,0,0));boss.startSorcererAttackForTest(level,target,"shardhail");
            try{
                var keyframe=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
                keyframe.setAccessible(true);keyframe.invoke(boss,level,target,SorcererTimeline.at("shardhail",35).get(0));
            }catch(ReflectiveOperationException exception){throw new IllegalStateException(exception);}
            var balls=level.getEntities(boss,boss.getBoundingBox().inflate(4),entity->entity.getTags().contains("native_hail_ball"));
            helper.assertTrue(balls.size()==1,"Frame 35 stored a target-centered delay instead of creating a source HailBall");
            var ball=(SorcererHail)balls.get(0);Vec3 expected=origin.add(0,2,.6);
            helper.assertTrue(ball.timer()==1&&ball.position().distanceTo(expected.add(0,1,0))<1e-6,"HailBall did not use source local (0,2,.6) followed by its creation tick");
            ball.attackTickForTest();helper.assertTrue(ball.position().distanceTo(expected.add(0,2,0))<1e-6,"HailBall rises exactly one block per attack tick");
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="singularity_charge_context",timeoutTicks=1200)
    public static void singularity_charge_selectors_keep_function_entry_position(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,45,()->{
            var owner=EntityType.COW.create(level);owner.setPos(origin.add(-10,0,0));level.addFreshEntity(owner);
            for(double offset:new double[]{3.15,-2.9}){
                var victim=encounterTestPlayer(helper,origin.add(0,0,offset),net.minecraft.world.level.GameType.SURVIVAL);
                var marker=EntityType.MARKER.create(level);marker.setPos(origin);level.addFreshEntity(marker);
                var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withPosition(origin)
                    .withRotation(new net.minecraft.world.phys.Vec2(0,0)).withSuppressedOutput();
                var commands=level.getServer().getCommands();
                helper.assertTrue(commands.performPrefixedCommand(source,"tp @s ^ ^ ^0.3")>0,"Could not run source movement fixture");
                commands.performPrefixedCommand(source,"execute if entity @a[distance=..3,gamemode=!spectator] run tag @s add source_near");
                boolean expected=marker.getTags().contains("source_near");
                helper.assertTrue(expected==(offset<0),"Vanilla function context unexpectedly followed the teleported marker");
                var bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),20,Combat.Element.VOID,.3f,5).effect(SpellBolt.Effect.DAMAGE,100).weaponVisual(4);
                bolt.tick();helper.assertTrue(bolt.isRemoved()==expected,"Native proximity follows moved entity rather than source function position at offset "+offset);
                marker.discard();victim.discard();bolt.discard();
            }
            var victim=emissaryPlayer(helper,origin.add(0,0,-4.99));
            var bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),20,Combat.Element.VOID,.3f,5).effect(SpellBolt.Effect.DAMAGE,1).weaponVisual(4);
            bolt.tick();helper.assertTrue(victim.getHealth()<200,"Timeout blast radius lost the source function's entry center");
            victim.discard();
            var reflectorOwner=encounterTestPlayer(helper,origin.add(10,0,0),net.minecraft.world.level.GameType.SURVIVAL);
            var shield=SpellArea.create(level,reflectorOwner,origin.add(1,0,0),Spell.REFLECTANCE);shield.setYRot(-90);shield.setXRot(0);
            var reflected=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),20,Combat.Element.VOID,.3f,5).effect(SpellBolt.Effect.DAMAGE,100).weaponVisual(4);
            reflected.tick();helper.assertTrue(shield.isRemoved()&&reflected.getOwner()==reflectorOwner,"Source reflection did not consume twenty and transfer owner");
            helper.assertTrue(reflected.position().distanceTo(origin.add(.3,0,0))<1e-6,"Reflected teleport was added to moved entity instead of source function position");
            reflected.discard();reflectorOwner.discard();
            Vec3 edge=origin.add(0,0,.4);BlockPos wall=BlockPos.containing(edge.add(0,0,.3));level.setBlock(wall,Blocks.STONE.defaultBlockState(),2);
            var crossing=SpellBolt.launch(level,owner,edge,new Vec3(0,0,1),20,Combat.Element.VOID,.3f,5).effect(SpellBolt.Effect.DAMAGE,100).weaponVisual(4);
            crossing.tick();helper.assertTrue(!crossing.isRemoved(),"Source block check moved along with the marker");crossing.tick();
            helper.assertTrue(crossing.isRemoved(),"Source next-tick block check failed to explode");
            owner.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="singularity_charge_parity",timeoutTicks=1200)
    public static void singularity_charge_moves_then_turns_and_restores(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,45,()->{
        var owner=EntityType.COW.create(level);owner.setPos(origin.add(-10,0,0));level.addFreshEntity(owner);
        var victim=emissaryPlayer(helper,origin.add(10,0,.3));
        var bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),20,Combat.Element.VOID,.3f,5).effect(SpellBolt.Effect.DAMAGE,100).weaponVisual(4);
        bolt.tick();helper.assertTrue(bolt.position().distanceTo(origin.add(0,0,.3))<1e-7,"Singularity turn was applied before its first movement");
        helper.assertTrue(bolt.getDeltaMovement().normalize().distanceTo(new Vec3(.5,0,5).normalize())<1e-6,"Nearest feet steering differs from source five-block construction");
        var saved=new CompoundTag();bolt.saveWithoutId(saved);bolt.discard();var resumed=Content.BOLT.get().create(level);resumed.load(saved);
        helper.assertTrue(resumed.remainingLife()==99&&resumed.flightProfileForTest()==SpellBolt.FlightProfile.SINGULARITY_CHARGE,"Restore lost source lifetime or flight policy");
        saved.remove("FlightProfile");saved.putBoolean("NoGravity",false);saved.putString("CollisionPolicy","VANILLA_PHYSICAL");
        var previousNative=Content.BOLT.get().create(level);previousNative.load(saved);
        helper.assertTrue(previousNative.flightProfileForTest()==SpellBolt.FlightProfile.SINGULARITY_CHARGE&&previousNative.isNoGravity()
            &&previousNative.collisionPolicyForTest()==SpellCollisionPolicy.SOURCE_PASSTHROUGH,"Previous native save did not acquire source bomb policy");previousNative.discard();
        owner.discard();victim.discard();resumed.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="singularity_charge_boundary",timeoutTicks=1200)
    public static void singularity_charge_proximity_uses_three_block_feet_radius(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,45,()->{
        var owner=EntityType.COW.create(level);owner.setPos(origin.add(-10,0,0));level.addFreshEntity(owner);
        for(double distance:new double[]{3.01,2.99}){
            var victim=emissaryPlayer(helper,origin.add(distance,0,0));
            var bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),20,Combat.Element.VOID,.3f,5).effect(SpellBolt.Effect.DAMAGE,100).weaponVisual(4);
            bolt.tick();helper.assertTrue(bolt.isRemoved()==(distance<3),"Proximity feet boundary differs at "+distance);
            helper.assertTrue((victim.getHealth()<200)==(distance<3),"Proximity blast damaged wrong feet boundary");bolt.discard();victim.discard();
        }
        owner.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="singularity_charge_parity")
    public static void singularity_charge_timeout_explodes_at_player_feet(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(3,4,3)).getCenter();
        var owner=EntityType.COW.create(level);owner.setPos(origin.add(-10,0,0));level.addFreshEntity(owner);
        var victim=emissaryPlayer(helper,origin.add(4.6,0,0));
        var bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),20,Combat.Element.VOID,.3f,5).effect(SpellBolt.Effect.DAMAGE,2).weaponVisual(4);
        bolt.tick();helper.assertTrue(!bolt.isRemoved(),"Singularity exploded before its second timer frame");bolt.tick();
        helper.assertTrue(bolt.isRemoved()&&victim.getHealth()<200,"Timeout silently discarded Singularity without its five-block blast");
        owner.discard();victim.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="visual_particle_plans")
    public static void source_sorcerer_particles_and_bounded_serpent_budget(GameTestHelper helper){
        helper.assertTrue(BossParticleEffects.cueCount("ajsorcererm2","die",15)==1,"Source portal cue missing on death frame fifteen");
        helper.assertTrue(BossParticleEffects.particleCount("ajsorcererm2","die",116)==300,"Source deathpoof ink count differs from its actual helper");
        helper.assertTrue(BossParticleEffects.cueCount("ajsorcererm1","shardhail",34)==2,"Source ice/item charging pair missing");
        helper.assertTrue(BossParticleEffects.cueCount("ajsorcererm2","airdash",1)==150,"Source dashfly geometry missing");
        helper.assertTrue(BossParticleEffects.cueCount("ajsorcererm2","die",116)==131&&BossParticleEffects.cueCount("ajsorcererm2","die",119)==460
            &&BossParticleEffects.cueCount("ajsorcererm2","die",121)==540,"Source animated_java/effects tick order or death shockwave geometry missing");
        helper.assertTrue(VoidSwirlEffects.velocityPlan(72,true,false).size()==90&&VoidSwirlEffects.velocityPlan(72,true,false).stream().allMatch(v->Math.abs(v.length()-.5)<1e-6),"Source reverse swirl has ninety half-block velocities");
        helper.assertTrue(VoidVeinEffects.explodePlan(Vec3.ZERO,72,1).size()==20&&VoidVeinEffects.explodePlan(Vec3.ZERO,72,1).stream().allMatch(at->at.equals(Vec3.ZERO))
            &&VoidVeinEffects.explodePlan(Vec3.ZERO,72,30).size()==20&&VoidVeinEffects.explodePlan(Vec3.ZERO,72,31).isEmpty(),"Source vein function's entry position or thirty-tick lifetime changed");
        int total=0;for(int age=0;age<25;age++){var plan=SoulSwirlEffects.singlePlan(Vec3.ZERO,72,age);helper.assertTrue(plan.size()==0||plan.size()==10,"Soul swirl lost source ten substeps");total+=plan.size();}
        helper.assertTrue(total>=40&&total<=110,"Source soul swirl lifetime must be four to eleven frames");
        helper.assertTrue(SerpentBeamHazard.visualRows(30)*2*20<=600,"Twenty columns exceed observer particle budget");
        helper.assertTrue(SerpentBeamHazard.containsFeet(Vec3.ZERO,new Vec3(0,80,0)),"Particle budget altered damage height");helper.succeed();
    }
    @GameTest(template="empty",batch="visual_soul_count")
    public static void source_soul_explosion_has_fifteen_strands(GameTestHelper helper){
        int exploded=0;for(int age=0;age<25;age++)exploded+=SoulSwirlEffects.explodePlan(Vec3.ZERO,72,age).size();
        helper.assertTrue(exploded>=600&&exploded<=1650,"Source explode invokes fifteen independent single strands, not one: "+exploded);helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_flight_stays_within_eight_chunks(GameTestHelper helper){
        var level=helper.getLevel();var palette=emissaryFixture(helper,2);
        helper.assertTrue(palette.emissaryWingVariant(0).equals("wing_phase2"),"Phase two switched to source's changed palette");palette.discard();
        var boss=emissaryFixture(helper,4);Vec3 anchor=boss.position();
        var target=EntityType.COW.create(level);target.setPos(anchor);level.addFreshEntity(target);
        Vec3 edge=anchor.add(127.8,0,0);level.getChunkAt(BlockPos.containing(edge));boss.setPos(edge);
        var state=new CompoundTag();boss.saveWithoutId(state);state.putBoolean("EmissaryFlying",true);
        state.putInt("EmissaryFlyTimer",40);state.putInt("EmissaryFlySpeed",10);
        state.putDouble("EmissaryFlyTargetX",anchor.x);state.putDouble("EmissaryFlyTargetY",anchor.y+20);state.putDouble("EmissaryFlyTargetZ",anchor.z);
        boss.load(state);boss.startEmissaryAttackForTest(target,"phase4.attack.fireball");boss.setYRot(-90);boss.setXRot(0);boss.customServerAiStep();
        helper.assertTrue(boss.position().distanceTo(anchor)<=128,"Flight left the player visibility envelope: "+boss.position().distanceTo(anchor));
        boss.discard();target.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void sword_plunge_visual_survives_tiny_carrier_bounds(GameTestHelper helper){
        var sword=Content.EMISSARY_SWORD_PLUNGE.get().create(helper.getLevel());
        helper.assertTrue(sword.shouldRenderAtSqrDistance(80*80),"Tenfold sword was distance-culled at eighty blocks");
        helper.assertTrue(sword.getBoundingBoxForCulling().getYsize()>=100,"Sword bounds omit its blade");helper.succeed();
    }
    private static UUID cancelProjectileImpactForTest;
    private static int cancelledProjectileImpactsForTest;
    static { MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,ProjectileImpactEvent.class,NativeGameTests::cancelMarkedProjectileImpact); }
    private static void cancelMarkedProjectileImpact(ProjectileImpactEvent event){
        if(cancelProjectileImpactForTest!=null&&cancelProjectileImpactForTest.equals(event.getProjectile().getUUID())){
            cancelledProjectileImpactsForTest++;event.setCanceled(true);
        }
    }

    @GameTest(template="empty",batch="incurrence",timeoutTicks=20)
    public static void incurrence_biome_routing_matches_source_predicates(GameTestHelper helper){
        helper.assertTrue("serpent".equals(NativeEvents.incurrenceBossForBiome(new net.minecraft.resources.ResourceLocation(NativeMod.ID,"night_wastes"))),
            "Incurrence night_wastes predicate no longer routes to Serpent");
        helper.assertTrue("sorcerer".equals(NativeEvents.incurrenceBossForBiome(new net.minecraft.resources.ResourceLocation(NativeMod.ID,"desert_of_rime"))),
            "Incurrence desert_of_rime predicate no longer routes to Sorcerer");
        helper.assertTrue("emissary".equals(NativeEvents.incurrenceBossForBiome(new net.minecraft.resources.ResourceLocation(NativeMod.ID,"infernal_chasms"))),
            "Incurrence infernal_chasms predicate no longer routes to Emissary");
        helper.assertTrue(NativeEvents.incurrenceBossForBiome(new net.minecraft.resources.ResourceLocation("othermod","infernal_chasms")).isEmpty(),
            "An unrelated biome namespace must not activate a Nyctocosm encounter");
        helper.succeed();
    }

    @GameTest(template="empty",batch="incurrence_mode",timeoutTicks=20)
    public static void emissary_entry_restores_only_survival_mode_changed_by_the_encounter(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 at=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var survival=encounterTestPlayer(helper,at,net.minecraft.world.level.GameType.SURVIVAL);
        NativeEvents.enterEmissaryMode(survival);
        helper.assertTrue(survival.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.ADVENTURE
                &&survival.getPersistentData().getBoolean("NyctocosmEncounterModeChanged"),
            "Source survival participant was not switched to managed Adventure mode");
        NativeEvents.restoreEmissaryMode(survival);
        helper.assertTrue(survival.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.SURVIVAL
                &&!survival.getPersistentData().contains("NyctocosmEncounterModeChanged"),
            "Encounter-managed Adventure mode was not restored to the saved Survival mode");

        var changedByAdmin=encounterTestPlayer(helper,at.add(2,0,0),net.minecraft.world.level.GameType.SURVIVAL);
        NativeEvents.enterEmissaryMode(changedByAdmin);changedByAdmin.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        NativeEvents.restoreEmissaryMode(changedByAdmin);
        helper.assertTrue(changedByAdmin.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.CREATIVE,
            "Encounter exit overwrote a later external game-mode change");

        var creative=encounterTestPlayer(helper,at.add(4,0,0),net.minecraft.world.level.GameType.CREATIVE);
        NativeEvents.enterEmissaryMode(creative);
        helper.assertTrue(creative.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.CREATIVE
                &&!creative.getPersistentData().contains("NyctocosmEncounterModeChanged"),
            "A player whose mode was not changed by the encounter must remain untouched");
        survival.discard();changedByAdmin.discard();creative.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="incurrence_mode_finish",timeoutTicks=40)
    public static void emissary_defeat_restores_only_encounter_managed_modes(GameTestHelper helper){
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain"));
        var domain=helper.getLevel().getServer().getLevel(key);
        helper.assertTrue(domain!=null,"Emissary domain is missing from the completion fixture");
        var players=new java.util.ArrayList<net.minecraft.server.level.ServerPlayer>();
        for(int i=0;i<3;i++){
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(domain,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ModeFinish"+i));
            player.setGameMode(i==2?net.minecraft.world.level.GameType.CREATIVE:net.minecraft.world.level.GameType.SURVIVAL);
            domain.addNewPlayer(player);NativeEvents.enterEmissaryMode(player);players.add(player);
        }
        players.get(1).setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        var boss=Content.EMISSARY.get().create(domain);boss.setNoAi(true);boss.setNoGravity(true);
        try{
            boss.deathTime=98;boss.tickDeath();
            helper.assertTrue(players.get(0).gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.ADVENTURE,
                "Managed mode was restored before the final defeat animation completed");
            boss.tickDeath();
            helper.assertTrue(players.get(0).gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.SURVIVAL
                    &&!players.get(0).getPersistentData().contains("NyctocosmEncounterModeChanged"),
                "Final Emissary defeat did not restore the encounter-managed Survival mode");
            helper.assertTrue(players.get(1).gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.SPECTATOR
                    &&players.get(2).gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.CREATIVE,
                "Final Emissary defeat overwrote an external mode change or a mode it never managed");
            boss.tickDeath();
            helper.assertTrue(players.get(0).gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.SURVIVAL,
                "Repeated defeat completion changed the restored mode");
        }finally{players.forEach(net.minecraft.world.entity.Entity::discard);boss.discard();}
        helper.succeed();
    }

    @GameTest(template="empty",batch="incurrence_mode_overlap",timeoutTicks=1200)
    public static void emissary_entry_waits_for_previous_defeat_animation(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,16,()->{
            var level=helper.getLevel();var boss=Content.EMISSARY.get().create(level);
            boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);
            boss.setHealth(0);boss.deathTime=99;
            helper.assertTrue(!boss.isAlive()&&!boss.isRemoved()&&level.getEntity(boss.getUUID())==boss,
                "Overlap fixture did not retain the previous encounter's dying boss");
            helper.assertTrue(NativeEvents.spawn(level,origin,"emissary")==0,
                "A new Emissary encounter started while the previous defeat animation still owns managed player modes");
            boss.tickDeath();helper.assertTrue(boss.isRemoved(),"The previous encounter did not clear at its final defeat tick");
            helper.succeed();
        });
    }

    @GameTest(template="empty",batch="weapon_ability_cooldowns",timeoutTicks=60)
    public static void nullfissure_abilities_keep_independent_source_cooldowns_across_hands_and_player_copy(GameTestHelper helper){
        var player=helper.makeMockPlayer();
        helper.assertTrue(WeaponAbilityCooldowns.supportsHand(2,net.minecraft.world.InteractionHand.MAIN_HAND)
                &&WeaponAbilityCooldowns.supportsHand(2,net.minecraft.world.InteractionHand.OFF_HAND),
            "Nullfissure must retain its source main-hand swing and offhand cast entries");
        helper.assertTrue(WeaponAbilityCooldowns.duration(2,net.minecraft.world.InteractionHand.MAIN_HAND)==20
                &&WeaponAbilityCooldowns.duration(2,net.minecraft.world.InteractionHand.OFF_HAND)==25,
            "Nullfissure source swing/cast counters must remain 20/25 player ticks");
        helper.assertTrue(WeaponAbilityCooldowns.start(player,2,net.minecraft.world.InteractionHand.MAIN_HAND),"First Nullfissure swing was rejected");
        helper.assertTrue(!WeaponAbilityCooldowns.start(player,2,net.minecraft.world.InteractionHand.MAIN_HAND),"A cooling Nullfissure swing restarted");
        helper.assertTrue(WeaponAbilityCooldowns.start(player,2,net.minecraft.world.InteractionHand.OFF_HAND),"Independent Nullfissure cast was blocked by swing cooldown");
        helper.assertTrue(WeaponAbilityCooldowns.remaining(player,2,net.minecraft.world.InteractionHand.MAIN_HAND)==20
                &&WeaponAbilityCooldowns.remaining(player,2,net.minecraft.world.InteractionHand.OFF_HAND)==25,
            "Main-hand swing and offhand cast did not retain separate cooldown state");
        for(int i=0;i<20;i++)WeaponAbilityCooldowns.tick(player);
        helper.assertTrue(WeaponAbilityCooldowns.remaining(player,2,net.minecraft.world.InteractionHand.MAIN_HAND)==0
                &&WeaponAbilityCooldowns.remaining(player,2,net.minecraft.world.InteractionHand.OFF_HAND)==5,
            "Ability tick countdown did not finish swing independently of the cast");
        helper.assertTrue(WeaponAbilityCooldowns.start(player,2,net.minecraft.world.InteractionHand.MAIN_HAND),
            "Nullfissure swing could not restart after its own counter expired");
        var replacement=helper.makeMockPlayer();NativeEvents.copyPersistentProgress(player,replacement);
        helper.assertTrue(WeaponAbilityCooldowns.remaining(replacement,2,net.minecraft.world.InteractionHand.MAIN_HAND)==20
                &&WeaponAbilityCooldowns.remaining(replacement,2,net.minecraft.world.InteractionHand.OFF_HAND)==5,
            "Player persistent progress copy lost an ability timer or combined the two timers");
        helper.succeed();
    }

    @GameTest(template="empty",batch="weapon_ability_cooldowns",timeoutTicks=40)
    public static void semblance_hands_have_separate_cooldowns_and_scythe_wyrmsliver_reject_offhand_use(GameTestHelper helper){
        var player=helper.makeMockPlayer();
        var scythe=(WeaponItem)Content.get("seelic_scythe");var wyrmsliver=(WeaponItem)Content.get("wyrmsliver");
        helper.assertTrue(!WeaponAbilityCooldowns.supportsHand(0,net.minecraft.world.InteractionHand.OFF_HAND)
                &&!WeaponAbilityCooldowns.supportsHand(1,net.minecraft.world.InteractionHand.OFF_HAND),
            "Scythe and Wyrmsliver must reject offhand ability use");
        helper.assertTrue(scythe.use(helper.getLevel(),player,net.minecraft.world.InteractionHand.OFF_HAND).getResult()==net.minecraft.world.InteractionResult.PASS
                &&wyrmsliver.use(helper.getLevel(),player,net.minecraft.world.InteractionHand.OFF_HAND).getResult()==net.minecraft.world.InteractionResult.PASS,
            "The actual Scythe/Wyrmsliver use route must pass offhand requests without starting a skill");
        helper.assertTrue(WeaponAbilityCooldowns.supportsHand(3,net.minecraft.world.InteractionHand.MAIN_HAND)
                &&WeaponAbilityCooldowns.supportsHand(3,net.minecraft.world.InteractionHand.OFF_HAND),
            "Semblance must retain its source main-hand swing and offhand cast entries");
        helper.assertTrue(WeaponAbilityCooldowns.duration(3,net.minecraft.world.InteractionHand.MAIN_HAND)==30
                &&WeaponAbilityCooldowns.duration(3,net.minecraft.world.InteractionHand.OFF_HAND)==30,
            "Semblance source swing/cast counters must remain 30 player ticks each");
        helper.assertTrue(WeaponAbilityCooldowns.start(player,3,net.minecraft.world.InteractionHand.MAIN_HAND)
                &&WeaponAbilityCooldowns.start(player,3,net.minecraft.world.InteractionHand.OFF_HAND),
            "Semblance swing and cast counters were not independent");
        helper.assertTrue(WeaponAbilityCooldowns.remaining(player,3,net.minecraft.world.InteractionHand.MAIN_HAND)==30
                &&WeaponAbilityCooldowns.remaining(player,3,net.minecraft.world.InteractionHand.OFF_HAND)==30,
            "Semblance did not store one cooldown per ability");
        helper.succeed();
    }

    @GameTest(template="empty",batch="fear_scope",timeoutTicks=20)
    public static void fear_processing_stays_in_source_realms_and_preserves_external_darkness(GameTestHelper helper){
        var server=helper.getLevel().getServer();var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FearScope");
        var overworld=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        var outside=net.minecraftforge.common.util.FakePlayerFactory.get(overworld,profile);outside.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        outside.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS,600,0));
        outside.getPersistentData().putInt("NyctocosmFearDark",137);outside.getPersistentData().putBoolean("NyctocosmFearActive",true);
        new NativeEvents().tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,outside));
        helper.assertTrue(outside.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),"Fear processing outside source dimensions removed another system's Darkness");
        helper.assertTrue(outside.getPersistentData().getInt("NyctocosmFearDark")==137&&outside.getPersistentData().getBoolean("NyctocosmFearActive"),
            "Fear state outside source dimensions was reset despite fear not running there");
        var ids=java.util.List.of("nyctocosm","dusk_plane","emissary_domain");
        for(String id:ids){
            var dimension=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,id));
            var level=server.getLevel(dimension);helper.assertTrue(level!=null,"Source fear dimension failed to load: "+id);
            var candidate=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Fear"+id));
            candidate.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            helper.assertTrue(NativeEvents.shouldTickFear(candidate),"Survival fear is disabled in source dimension "+id);
            candidate.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            helper.assertTrue(!NativeEvents.shouldTickFear(candidate),"Source fear must run for survival only, not adventure: "+id);
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="field_lifecycle",timeoutTicks=20)
    public static void fixed_field_lives_for_nearby_players_not_only_its_owner(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var nearby=encounterTestPlayer(helper,center.add(1,0,0),net.minecraft.world.level.GameType.SURVIVAL);
        var field=new SeelicField(Content.FIELD.get(),level);field.configure(owner,false,false);field.setPos(center);level.addFreshEntity(field);
        owner.discard();field.tick();
        helper.assertTrue(!field.isRemoved()&&field.remainingLife()==-1,"Fixed field must persist without its owner while any player remains within three blocks");
        nearby.setPos(center.add(5,0,0));field.tick();
        helper.assertTrue(field.isRemoved(),"Fixed field must disappear once every player leaves its three-block radius");
        nearby.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="field_lifecycle",timeoutTicks=20)
    public static void mobile_field_uses_idle_lifetime_and_keeps_cast_state_on_save(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var nearby=encounterTestPlayer(helper,center.add(1,0,0),net.minecraft.world.level.GameType.SURVIVAL);
        var field=new SeelicField(Content.FIELD.get(),level);field.configure(owner,true,false);field.setPos(center);level.addFreshEntity(field);
        helper.assertTrue(field.remainingLife()==500,"The source mobile plane default duration is 500 ticks at zero Dream power");
        field.tick();helper.assertTrue(field.remainingLife()==499,"An idle mobile field must lose one duration tick");
        field.beginCasting(Spell.ABHORRENCE);field.tick();
        helper.assertTrue(field.castTicks()==Spell.ABHORRENCE.castTicks-1&&field.remainingLife()==499,
            "A mobile field must pause its duration during casting; cast="+field.castTicks()+", life="+field.remainingLife());
        var saved=new CompoundTag();field.saveWithoutId(saved);var restored=Content.FIELD.get().create(level);restored.load(saved);
        helper.assertTrue(restored.mobile()&&restored.castTicks()==field.castTicks()&&restored.remainingLife()==499,
            "Mobile type, remaining cast, and paused lifetime must survive saving");
        owner.discard();
        for(int tick=0;tick<Spell.ABHORRENCE.castTicks-1;tick++)field.tick();
        helper.assertTrue(!field.isRemoved()&&field.castTicks()==0&&field.remainingLife()==499,
            "The plane's world tick must finish its in-progress cast after the owner leaves, then resume the paused timer");
        field.tick();helper.assertTrue(!field.isRemoved()&&field.remainingLife()==498,
            "An idle mobile field must keep aging after its owner leaves instead of being deleted immediately");
        restored.discard();field.discard();nearby.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="field_lifecycle",timeoutTicks=20)
    public static void field_type_is_fixed_at_creation_and_mobile_fields_follow_their_owner(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var fixedOwner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var fixed=new SeelicField(Content.FIELD.get(),level);fixed.configure(fixedOwner,false,false);fixed.setPos(center);level.addFreshEntity(fixed);
        fixedOwner.getInventory().add(new ItemStack(Content.get("artifact_transposition")));
        fixedOwner.setXRot(-35);fixedOwner.setPos(center.add(1,0,0));fixed.tick();
        helper.assertTrue(!fixed.isRemoved()&&fixed.position().distanceToSqr(center)<1.0e-8&&fixedOwner.getXRot()==-35,
            "Adding Transposition after creation must not turn a fixed field into a following/mobile field");

        var mobileOwner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        mobileOwner.getInventory().add(new ItemStack(Content.get("artifact_transposition")));
        var mobile=new SeelicField(Content.FIELD.get(),level);mobile.configure(mobileOwner,true,false);mobile.setPos(center);level.addFreshEntity(mobile);
        mobileOwner.getInventory().clearContent();mobileOwner.setPos(center.add(1.5,0,0));mobileOwner.setYRot(47);mobileOwner.setXRot(-35);mobile.tick();
        helper.assertTrue(!mobile.isRemoved()&&mobile.position().distanceToSqr(mobileOwner.position())<1.0e-8
                &&mobile.getYRot()==47&&mobile.getXRot()==0&&mobileOwner.getXRot()==-35,
            "A mobile field must follow its linked owner, copy yaw, and keep marker pitch at zero after the artifact is removed");
        mobileOwner.setPos(center.add(2,0,0));mobileOwner.setYRot(-90);mobileOwner.setXRot(25);mobile.tick();
        helper.assertTrue(mobile.position().distanceToSqr(mobileOwner.position())<1.0e-8&&mobile.getYRot()==-90&&mobile.getXRot()==0,
            "The mobile field must continue following position and yaw independent of later inventory changes");
        fixed.discard();mobile.discard();fixedOwner.discard();mobileOwner.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="field_lifecycle",timeoutTicks=20)
    public static void cancelling_a_cast_expires_only_a_mobile_field(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var fixed=new SeelicField(Content.FIELD.get(),level);fixed.configure(owner,false,false);fixed.setPos(center);level.addFreshEntity(fixed);
        var mobile=new SeelicField(Content.FIELD.get(),level);mobile.configure(owner,true,false);mobile.setPos(center);level.addFreshEntity(mobile);
        fixed.beginCasting(Spell.ABHORRENCE);fixed.cancelCast();
        helper.assertTrue(!fixed.isRemoved()&&fixed.castTicks()==0&&fixed.remainingLife()==-1,
            "Source cancel_cast clears a fixed field's cast without expiring the field");
        mobile.beginCasting(Spell.ABHORRENCE);mobile.cancelCast();
        helper.assertTrue(!mobile.isRemoved()&&mobile.castTicks()==0&&mobile.remainingLife()==0,
            "Source cancel_cast sets the mobile field's duration to zero for its next normal lifecycle tick");
        mobile.tick();helper.assertTrue(mobile.isRemoved(),"A mobile field with zero duration must be removed by its normal tick");
        fixed.discard();owner.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="field_lifecycle",timeoutTicks=20)
    public static void mobile_field_is_removed_when_no_player_remains_nearby_during_a_cast(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var field=new SeelicField(Content.FIELD.get(),level);field.configure(owner,true,false);field.setPos(center);level.addFreshEntity(field);
        field.beginCasting(Spell.ABHORRENCE);owner.discard();field.tick();
        helper.assertTrue(field.isRemoved(),"Source block/2 removes every plane, including a mobile plane mid-cast, when no player is within three blocks");
        helper.succeed();
    }

    @GameTest(template="empty",batch="hypertrace",timeoutTicks=20)
    public static void hypertrace_probability_uses_inclusive_zero_to_ninety_nine_rolls(GameTestHelper helper){
        int successes=0;
        for(int roll=0;roll<100;roll++){
            int rollValue=roll;
            if(HypertraceService.succeeds(15,bound->{helper.assertTrue(bound==100,"Hypertrace must draw from integer range 0..99");return rollValue;}))successes++;
        }
        helper.assertTrue(successes==16,"Inclusive score 15 must accept exactly rolls 0 through 15; got "+successes);
        helper.assertTrue(!HypertraceService.succeeds(15,bound->16),"Roll 16 must fail at score 15");
        helper.assertTrue(HypertraceService.succeeds(100,bound->99),"Chance 100 must accept the final source roll");
        helper.succeed();
    }

    @GameTest(template="empty",batch="hypertrace",timeoutTicks=20)
    public static void every_spell_creates_hypertrace_at_cast_start(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        int spellCount=0;
        for(Spell spell:Spell.values()){
            var field=new SeelicField(Content.FIELD.get(),level);field.configure(owner,false,false);field.setPos(center);level.addFreshEntity(field);
            var beforeIds=NativeEvents.traces(owner).stream().map(net.minecraft.world.entity.Entity::getId).collect(java.util.stream.Collectors.toSet());int[] rolls={0};
            field.startCasting(level,owner,spell,bound->{helper.assertTrue(bound==100,"Spell start used a non-source roll range");rolls[0]++;return 15;});
            var traces=NativeEvents.traces(owner);
            var trace=traces.stream().filter(candidate->!beforeIds.contains(candidate.getId())).findFirst().orElse(null);
            helper.assertTrue(rolls[0]==1&&trace!=null,"Spell did not roll and create its trace exactly once at cast start: "+spell);
            Vec3 expected=owner.position().add(0,1,0).add(owner.getLookAngle().scale(3));
            helper.assertTrue(trace.ownedBy(owner)&&trace.remainingLife()==400&&trace.position().distanceToSqr(expected)<1.0e-5,
                "Cast trace owner, 400-tick lifetime, or forward-three-block spawn context was wrong: "+spell+" trace="+trace.position()+" expected="+expected+" life="+trace.remainingLife()+" owner="+trace.ownedBy(owner));
            helper.assertTrue(field.castTicks()==spell.castTicks,"Trace roll was delayed until spell cast completion: "+spell);
            field.cancelCast();
            helper.assertTrue(!trace.isRemoved(),"Canceling the plane cast also removed its already-created trace: "+spell);
            field.discard();trace.discard();spellCount++;
        }
        helper.assertTrue(spellCount==17,"Expected all 17 source spell cast functions to be covered");
        helper.succeed();
    }

    @GameTest(template="empty",batch="sword_plunge",timeoutTicks=20)
    public static void sword_plunge_charge_keeps_the_emissary_anchored(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=emissaryPlayer(helper,origin.add(0,0,8));
        try{
            var attackField=NativeBoss.class.getDeclaredField("attack");attackField.setAccessible(true);
            attackField.set(boss,new NativeBoss.Attack("phase2.attack.sword_plunge",61,60,NativeBoss.Pattern.DASH,Combat.Element.DISSONANCE));
            var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);ageField.setInt(boss,40);
            boss.setDeltaMovement(Vec3.ZERO);
            var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);
            perform.setAccessible(true);perform.invoke(boss,level,target);
            helper.assertTrue(boss.position().distanceToSqr(origin)<1.0e-8&&boss.getDeltaMovement().lengthSqr()<1.0e-8,
                "Sword Plunge's windup must keep the emissary at its hovering anchor; the sword is an independent attack entity");
            ageField.setInt(boss,45);perform.invoke(boss,level,target);
            var swords=level.getEntitiesOfClass(EmissarySwordPlunge.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(500));
            var selectorBox=new net.minecraft.world.phys.AABB(origin,origin).inflate(300);
            var eligible=level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,selectorBox,player->!player.isSpectator()&&!player.getTags().contains("emissarydebug")&&player.distanceToSqr(boss)<=90000);
            helper.assertTrue(swords.size()==1,"Sword Plunge must spawn exactly one independent sword at its source animation frame 45; eligible="+eligible+", players="+level.players().size()+", targetSpectator="+target.isSpectator()+", targetPos="+target.position()+", boss="+boss.position());
            var selected=eligible.stream().filter(player->Math.abs(player.getX()-swords.get(0).getX())<1.0e-6&&Math.abs(player.getZ()-swords.get(0).getZ())<1.0e-6).findFirst().orElse(null);
            helper.assertTrue(selected!=null,"Sword start was not over a source-eligible random player: sword="+swords.get(0).position()+" eligible="+eligible.stream().map(player->player.position().toString()).toList());
            int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,selected.getBlockX(),selected.getBlockZ());
            helper.assertTrue(Math.abs(swords.get(0).getY()-(surface+220))<1.0e-6&&boss.position().distanceToSqr(origin)<1.0e-8,
                "Frame 45 must place the sword over the chosen player's surface without moving the hovering Boss");
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not invoke the Sword Plunge source-timing fixture",failure);}
        target.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="sword_plunge",timeoutTicks=20)
    public static void sword_plunge_uses_surface_plus_220_and_source_substep_speeds(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=encounterTestPlayer(helper,origin.add(0,0,8),net.minecraft.world.level.GameType.CREATIVE);
        var sword=EmissarySwordPlunge.createAtTarget(level,boss,target,-90,-30);
        helper.assertTrue(sword!=null,"Sword Plunge did not create an independent attack entity for an eligible player");
        int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,target.getBlockX(),target.getBlockZ());
        helper.assertTrue(Math.abs(sword.getY()-(surface+220))<1.0e-6,"Sword did not start exactly 220 blocks above the target's world surface");
        for(int tick=0;tick<50;tick++)sword.tick();
        helper.assertTrue(sword.substepsForTest()==150&&Math.abs(sword.getY()-(surface+70))<1.0e-6,
            "First 200 source substeps must each teleport down one block; after 50 ticks y="+sword.getY()+" substeps="+sword.substepsForTest());
        for(int tick=50;tick<67;tick++)sword.tick();
        helper.assertTrue(sword.substepsForTest()==201&&Math.abs(sword.getY()-(surface+19.8))<1.0e-5,
            "Source switches to 0.2-block substeps after exactly 200 fast steps; y="+sword.getY()+" substeps="+sword.substepsForTest());
        for(int tick=67;tick<84;tick++)sword.tick();
        helper.assertTrue(sword.substepsForTest()==EmissarySwordPlunge.TOTAL_SUBSTEPS&&Math.abs(sword.getY()-(surface+10))<1.0e-5,
            "Sword must stop after 250 substeps and remain ten blocks above the recorded surface; y="+sword.getY());
        sword.tick();helper.assertTrue(Math.abs(sword.getY()-(surface+10))<1.0e-5,"Stopped sword continued moving before its timer expired");
        sword.discard();target.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="sword_plunge",timeoutTicks=20)
    public static void sword_plunge_restores_timer_and_detonates_once_at_entity_tick_110(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var sourceTarget=encounterTestPlayer(helper,origin.add(0,0,8),net.minecraft.world.level.GameType.CREATIVE);
        int sourceSurface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,sourceTarget.getBlockX(),sourceTarget.getBlockZ());
        var victim=emissaryPlayer(helper,new Vec3(origin.x+14.25,sourceSurface,origin.z+8));
        var maxHealth=victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        maxHealth.setBaseValue(200);victim.setHealth(200);victim.hurtTime=0;
        var sword=EmissarySwordPlunge.createAtTarget(level,boss,sourceTarget,0,-45);
        helper.assertTrue(sword!=null,"Sword Plunge test fixture failed to create its carrier");
        for(int tick=0;tick<50;tick++)sword.tick();
        helper.assertTrue(sword.ageForTest()==50&&sword.substepsForTest()==150&&!sword.explosionPlayedForTest(),
            "Timer 50 save point must retain partial fall state without triggering the later blast");
        var saved=new CompoundTag();sword.saveWithoutId(saved);sword.discard();
        var restored=Content.EMISSARY_SWORD_PLUNGE.get().create(level);restored.load(saved);level.addFreshEntity(restored);
        helper.assertTrue(restored.ageForTest()==50&&restored.substepsForTest()==150&&restored.ownerForTest().equals(boss.getUUID()),
            "Sword owner, age, and substep position were not restored from its save at tick 50");
        try{
            var attackField=NativeBoss.class.getDeclaredField("attack");attackField.setAccessible(true);attackField.set(boss,null);
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not end the Emissary clip in the source lifetime fixture",failure);}
        for(int tick=50;tick<104;tick++)restored.tick();
        helper.assertTrue(restored.ageForTest()==104&&!restored.explosionPlayedForTest()&&!restored.isRemoved(),
            "Saved Sword Plunge must remain independent after its Boss animation ends and must not explode at age 104");
        restored.tick();helper.assertTrue(restored.ageForTest()==105&&restored.shockwavePlayedForTest()&&!restored.isRemoved(),
            "Source's independent timer 105 event did not fire at tick 105");
        for(int tick=105;tick<109;tick++)restored.tick();
        helper.assertTrue(restored.ageForTest()==109&&!restored.explosionPlayedForTest()&&!restored.isRemoved(),
            "Sword Plunge must retain its carrier through tick 109 without replaying the explosion");
        restored.tick();
        helper.assertTrue(restored.ageForTest()==110&&restored.explosionPlayedForTest()&&restored.isRemoved(),
            "Sword Plunge did not execute exactly once and clean itself up at entity timer 110");
        helper.assertTrue(victim.getHealth()<200,"Tick-110 blast did not use the player-centered surface radius from the source selector; health="+victim.getHealth()+", hurtTime="+victim.hurtTime+", position="+victim.position()+", sword="+restored.position()+", surface="+level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,restored.getBlockX(),restored.getBlockZ()));
        restored.discard();sourceTarget.discard();victim.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="wing_blast",timeoutTicks=1200)
    public static void wing_blast_spawns_six_locator_rays_and_saves_windup_state(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,112,()->{
            var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
            var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(origin);boss.setYRot(27);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
            var target=emissaryPlayer(helper,origin.add(0,0,10));
            try{
                var attackField=NativeBoss.class.getDeclaredField("attack");attackField.setAccessible(true);
                attackField.set(boss,new NativeBoss.Attack("phase2.attack.wing_blast",81,40,NativeBoss.Pattern.RING,Combat.Element.RADIANCE));
                var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);
                var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);
                perform.setAccessible(true);
                ageField.setInt(boss,11);perform.invoke(boss,level,target);
                helper.assertTrue(level.getEntitiesOfClass(EmissaryWingBlastRay.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(48),ray->boss.getUUID().equals(ray.ownerId())).isEmpty(),"Wing Blast created its six markers before source animation frame 12");
                ageField.setInt(boss,12);perform.invoke(boss,level,target);
            }catch(ReflectiveOperationException failure){throw new AssertionError("Could not invoke the Wing Blast source animation fixture",failure);}
            var rays=level.getEntitiesOfClass(EmissaryWingBlastRay.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(48),ray->boss.getUUID().equals(ray.ownerId()));
            helper.assertTrue(rays.size()==6,"Wing Blast must create six independent ray markers; got "+rays.size());
            java.util.Set<Integer> wingIds=new java.util.HashSet<>();
            String[] locators={"rightupperwingtip1","leftupperwingtip1","rightmiddlewingtip1","leftmiddlewingtip1","rightlowerwingtip1","leftlowerwingtip1"};
            for(EmissaryWingBlastRay ray:rays){
                int index=ray.wingForTest();wingIds.add(index);
                var locator=EmissaryDamagePose.sampleAt(boss,"phase2.attack.wing_blast",locators[index],12);
                Vec3 expected=EmissaryDamagePose.worldPosition(boss,locator);
                helper.assertTrue(ray.position().distanceToSqr(expected)<1.0e-8,"Wing ray did not use animation-frame-12 locator "+locators[index]+": got "+ray.position()+" expected "+expected);
                helper.assertTrue(!ray.activeForTest()&&ray.ageForTest()==0&&boss.getUUID().equals(ray.ownerId()),"New Wing Blast ray skipped its warning state or lost owner");
            }
            helper.assertTrue(wingIds.size()==6,"Wing ray entities did not preserve all six wing identities");
            var savedRay=rays.get(0);var saved=new CompoundTag();savedRay.ageForTest(22);savedRay.saveWithoutId(saved);savedRay.discard();
            var restored=Content.EMISSARY_WING_BLAST_RAY.get().create(level);restored.load(saved);level.addFreshEntity(restored);
            helper.assertTrue(restored.ageForTest()==22&&!restored.activeForTest()&&boss.getUUID().equals(restored.ownerId()),"Warning age/owner did not survive a world save");
            try{
                var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);
                var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);
                ageField.setInt(boss,34);perform.invoke(boss,level,target);
                helper.assertTrue(level.getEntitiesOfClass(EmissaryWingBlastRay.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(48),ray->boss.getUUID().equals(ray.ownerId())).stream().noneMatch(EmissaryWingBlastRay::activeForTest),"Wing Blast activated before source animation frame 35");
                ageField.setInt(boss,35);perform.invoke(boss,level,target);
            }catch(ReflectiveOperationException failure){throw new AssertionError("Could not advance the Wing Blast activation fixture",failure);}
            var activeRays=level.getEntitiesOfClass(EmissaryWingBlastRay.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(48),ray->boss.getUUID().equals(ray.ownerId()));
            helper.assertTrue(activeRays.size()==6&&activeRays.stream().allMatch(EmissaryWingBlastRay::activeForTest),"Frame-35 activation did not switch all six independent rays, including the restored node, into blasting state");
            for(EmissaryWingBlastRay ray:activeRays)ray.discard();target.discard();boss.discard();helper.succeed();
        });
    }

    @GameTest(template="empty",batch="wing_blast",timeoutTicks=1200)
    public static void wing_blast_stops_at_solid_blocks_and_uses_four_block_terminal_radius(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,112,()->{
            var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
            var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
            BlockPos wall=BlockPos.containing(origin.x,origin.y,origin.z-5);
            for(int y=-1;y<=2;y++)for(int x=-2;x<=2;x++)level.setBlockAndUpdate(wall.offset(x,y,0),Blocks.STONE.defaultBlockState());
            var near=emissaryPlayer(helper,origin.add(3,0,-5));var far=emissaryPlayer(helper,origin.add(0,0,-12));
            var ray=Content.EMISSARY_WING_BLAST_RAY.get().create(level);ray.configureForTest(boss,2,true);ray.setPos(origin);ray.setYRot(180);ray.setXRot(0);level.addFreshEntity(ray);
            ray.tick();
            helper.assertTrue(ray.lastTraceEndForTest()!=null&&ray.lastTraceEndForTest().distanceToSqr(Vec3.atCenterOf(wall))<2.0,
                "Wing ray did not terminate at the first non-passthrough block; end="+ray.lastTraceEndForTest()+" wall="+wall);
            helper.assertTrue(near.getHealth()<200,"Solid-block terminal burst did not hit a player inside the source four-block radius");
            helper.assertTrue(far.getHealth()==200,"Wing ray passed through a solid block or used an oversized terminal radius; far health="+far.getHealth());
            ray.discard();near.discard();far.discard();boss.discard();helper.succeed();
        });
    }

    @GameTest(template="empty",batch="wing_blast",timeoutTicks=1200)
    public static void wing_blast_player_contact_ends_ray_and_damages_on_cooldown(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,112,()->{
            var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
            var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
            var victim=emissaryPlayer(helper,origin.add(0,0,-9));
            var ray=Content.EMISSARY_WING_BLAST_RAY.get().create(level);ray.configureForTest(boss,2,true);ray.setPos(origin);ray.setYRot(180);ray.setXRot(0);level.addFreshEntity(ray);
            ray.tick();
            helper.assertTrue(victim.getHealth()<200,"An active Wing Blast ray did not burst when its source player selector intersected the target");
            helper.assertTrue(ray.lastTraceEndForTest()!=null&&ray.lastTraceEndForTest().distanceToSqr(victim.position())<16,
                "Player hit did not terminate at the target cell before the four-block damage check");
            ray.discard();victim.discard();boss.discard();helper.succeed();
        });
    }

    @GameTest(template="empty",batch="wing_blast",timeoutTicks=1200)
    public static void wing_blast_reflectance_redirects_the_ray_and_marker_expires_at_source_tick_43(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,112,()->{
            var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
            var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
            var reflectorOwner=emissaryPlayer(helper,origin.add(10,0,0));var reflectedVictim=emissaryPlayer(helper,origin.add(-9,0,-1));
            var reflector=SpellArea.create(level,reflectorOwner,origin.add(0,0,-3),Spell.REFLECTANCE);reflector.setYRot(90);reflector.setXRot(0);
            var ray=Content.EMISSARY_WING_BLAST_RAY.get().create(level);ray.configureForTest(boss,2,true);ray.setPos(origin);ray.setYRot(180);ray.setXRot(0);level.addFreshEntity(ray);
            ray.tick();
            helper.assertTrue(reflectorOwner.getUUID().equals(ray.damageOwnerForTest()),"Reflected wing ray kept the emissary as its damage owner");
            helper.assertTrue(reflector.isRemoved(),"Reflectance did not consume its stored energy when intercepting the source wing ray");
            helper.assertTrue(reflectedVictim.getHealth()<200,"Wing Blast did not continue from Reflectance's facing toward its redirected target; origin="+origin+", end="+ray.lastTraceEndForTest()+", victim="+reflectedVictim.position()+", health="+reflectedVictim.getHealth()+", damageOwner="+ray.damageOwnerForTest());
            var expiring=Content.EMISSARY_WING_BLAST_RAY.get().create(level);expiring.configureForTest(boss,2,false);expiring.ageForTest(42);expiring.setPos(origin.add(0,0,8));level.addFreshEntity(expiring);
            expiring.tick();helper.assertTrue(expiring.ageForTest()==EmissaryWingBlastRay.SOURCE_LIFETIME&&expiring.isRemoved(),"Wing Blast marker did not execute its final source timer-43 tick and clean up");
            ray.discard();reflectorOwner.discard();reflectedVictim.discard();boss.discard();helper.succeed();
        });
    }

    @GameTest(template="empty",batch="emissary_celestial_start",timeoutTicks=30)
    public static void celestial_smite_runs_startup_shot_and_later_26_50_93_frame_tear(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=celestialTestOrigin(helper,new BlockPos(4,8,4));
        var boss=emissaryFixture(helper,2);boss.setPos(origin);boss.setYRot(0);
        var target=encounterTestPlayer(helper,origin.add(0,0,12),net.minecraft.world.level.GameType.SURVIVAL);
        var nearby=emissaryPlayer(helper,origin.add(0,0,10));float before=nearby.getHealth();

        boss.startEmissaryAttackForTest(target,"phase2.attack.celestial_smite");
        helper.assertTrue(nearby.getHealth()<before,
            "The source start function's immediate tear→circles→activate sequence did not damage its terminal area: nearby="+before+"→"+nearby.getHealth()
                +", onlinePlayers="+level.players().size()+", targetListed="+level.players().contains(target)+", targetAlive="+target.isAlive()
                +", animation="+boss.animation()+", attackAge="+boss.emissaryAttackAge());
        helper.assertTrue(level.getEntitiesOfClass(EmissaryCelestialTear.class,new AABB(origin,origin).inflate(140)).isEmpty(),
            "The synchronous startup ray must remove its temporary marker before the attack's later animation events");

        emissaryCelestialFrame(helper,boss,target,25);
        helper.assertTrue(level.getEntitiesOfClass(EmissaryCelestialTear.class,new AABB(origin,origin).inflate(140)).isEmpty(),
            "Celestial Smite created the delayed tear before source leaf_26");
        emissaryCelestialFrame(helper,boss,target,26);
        var tears=level.getEntitiesOfClass(EmissaryCelestialTear.class,new AABB(origin,origin).inflate(140),tear->boss.getUUID().equals(tear.ownerForTest()));
        helper.assertTrue(tears.size()==1,"Source leaf_26 must create one delayed tear; got "+tears.size());
        var tear=tears.get(0);
        helper.assertTrue(tear.position().distanceToSqr(target.position().add(0,100,0))<1.0e-8,
            "Tear origin did not preserve the randomly selected player's exact position plus 100 blocks");
        helper.assertTrue(!tear.activeForTest(),"The frame-26 tear activated before source leaf_50");

        CompoundTag saved=new CompoundTag();tear.saveWithoutId(saved);tear.discard();
        var restored=Content.EMISSARY_CELESTIAL_TEAR.get().create(level);restored.load(saved);level.addFreshEntity(restored);
        helper.assertTrue(restored.ownerForTest().equals(boss.getUUID())&&!restored.activeForTest()
                &&restored.position().distanceToSqr(target.position().add(0,100,0))<1.0e-8,
            "Tear owner, position, direction, or inactive warning phase did not survive save/load");
        emissaryCelestialFrame(helper,boss,target,49);
        helper.assertTrue(!restored.activeForTest(),"Celestial Smite activated before source leaf_50");
        emissaryCelestialFrame(helper,boss,target,50);
        helper.assertTrue(restored.activeForTest(),"Source leaf_50 failed to activate the saved tear");
        emissaryCelestialFrame(helper,boss,target,92);
        helper.assertTrue(!restored.isRemoved(),"Celestial Smite fired before source leaf_93");
        emissaryCelestialFrame(helper,boss,target,93);
        helper.assertTrue(restored.isRemoved(),"Source leaf_93 failed to execute and clean up the active raycast");
        helper.assertTrue(EmissaryCelestialTear.SOURCE_RANGE==150&&EmissaryCelestialTear.SOURCE_DAMAGE==80,
            "Celestial Smite ray range or configured base damage drifted from the source");
        nearby.discard();target.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="emissary_celestial_wall",timeoutTicks=20)
    public static void celestial_smite_wall_ends_ray_and_damages_the_source_15_block_terminal_area(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=celestialTestOrigin(helper,new BlockPos(4,12,4));
        var boss=emissaryFixture(helper,2);boss.setPos(origin);
        BlockPos wall=BlockPos.containing(origin.add(0,0,3));level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        var inside=emissaryPlayer(helper,origin.add(12,0,3));var outside=emissaryPlayer(helper,origin.add(16,0,3));
        float insideBefore=inside.getHealth(),outsideBefore=outside.getHealth();
        var tear=EmissaryCelestialTear.createForTest(level,boss,origin,0,0,true);
        tear.fireForTest(level,boss);
        helper.assertTrue(tear.isRemoved()&&tear.impactedForTest()&&tear.traceChecksForTest()==4,
            "Source ray did not stop on the first non-passthrough block at the correct inclusive step");
        helper.assertTrue(tear.endpointForTest().distanceToSqr(origin.add(0,0,3))<1.0e-8,
            "Terminal point did not remain on the blocking block cell");
        helper.assertTrue(inside.getHealth()<insideBefore,
            "A player 12 blocks from the terminal point was outside Celestial Smite's source radius 15");
        helper.assertTrue(outside.getHealth()==outsideBefore,
            "A player 16 blocks from the terminal point was incorrectly hit");
        for(int x=-4;x<=4;x++)for(int y=-2;y<=2;y++)for(int z=-4;z<=4;z++)
            helper.assertTrue(!level.getBlockState(wall.offset(x,y,z)).is(Blocks.FIRE),
                "Unconfirmed C03 world-fire behavior placed fire at "+wall.offset(x,y,z));
        inside.discard();outside.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="emissary_celestial_range",timeoutTicks=1200)
    public static void celestial_smite_player_contact_stops_early_and_open_ray_checks_at_most_150_cells(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,36,()->{
        // The source contact selector includes spectators: the ticket owns loading without a keeper at ray origin.
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);
        var contact=emissaryPlayer(helper,origin.add(0,0,24));float before=contact.getHealth();
        var touching=EmissaryCelestialTear.createForTest(level,boss,origin,0,0,true);
        touching.fireForTest(level,boss);
        helper.assertTrue(touching.impactedForTest()&&touching.traceChecksForTest()==20,
            "A player five blocks from the ray cell did not terminate source distance<=5 at step 20: checks="+touching.traceChecksForTest()
                +", impact="+touching.impactedForTest()+", endpoint="+touching.endpointForTest()+", player="+contact.position());
        helper.assertTrue(touching.endpointForTest().distanceToSqr(origin.add(0,0,19))<1.0e-8&&contact.getHealth()<before,
            "Player-contact terminal point or its separate 15-block explosion damage was wrong");

        Vec3 longOrigin=origin.add(0,100,0);int chunkX=BlockPos.containing(longOrigin).getX()>>4;
        int startChunkZ=BlockPos.containing(longOrigin).getZ()>>4;
        int endChunkZ=BlockPos.containing(longOrigin.add(0,0,EmissaryCelestialTear.SOURCE_RANGE)).getZ()>>4;
        for(int chunkZ=startChunkZ;chunkZ<=endChunkZ;chunkZ++)
            level.getChunkSource().getChunk(chunkX,chunkZ,net.minecraft.world.level.chunk.ChunkStatus.FULL,true);
        var maximum=EmissaryCelestialTear.createForTest(level,boss,longOrigin,0,0,true);
        maximum.fireForTest(level,boss);
        helper.assertTrue(!maximum.impactedForTest()&&maximum.traceChecksForTest()==150,
            "An unobstructed source ray must end by range exhaustion without becoming an impact");
        helper.assertTrue(maximum.endpointForTest().distanceToSqr(longOrigin.add(0,0,149))<1.0e-6,
            "The 150th source check should occur at the cell 149 blocks forward from its origin");
        contact.discard();boss.discard();helper.succeed();
        });
    }

    @GameTest(template="empty",batch="emissary_pillars",timeoutTicks=20)
    public static void emissary_pillars_spawn_cloud_and_flame_at_animation_frame_33(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=emissaryPlayer(helper,origin.add(0,0,8));
        try{
            var attackField=NativeBoss.class.getDeclaredField("attack");attackField.setAccessible(true);
            attackField.set(boss,new NativeBoss.Attack("phase1.attack.pillars",71,35,NativeBoss.Pattern.PILLARS,Combat.Element.DISSONANCE));
            var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);
            var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);
            ageField.setInt(boss,32);perform.invoke(boss,level,target);
            helper.assertTrue(level.getEntitiesOfClass(EmissaryPillar.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(8)).isEmpty(),
                "Pillars markers were created before the source animation activation frame 33");
            ageField.setInt(boss,33);perform.invoke(boss,level,target);
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not invoke the Pillars source animation fixture",failure);}
        var pillars=level.getEntitiesOfClass(EmissaryPillar.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(8));
        helper.assertTrue(pillars.size()==2,"Pillars activation must create separate cloud and flame carriers; got "+pillars.size());
        helper.assertTrue(pillars.stream().map(EmissaryPillar::kindForTest).collect(java.util.stream.Collectors.toSet())
                .equals(java.util.Set.of(EmissaryPillar.Kind.CLOUD,EmissaryPillar.Kind.FLAME)),
            "Pillars did not retain the separate source cloud/flame modes");
        helper.assertTrue(pillars.stream().allMatch(pillar->boss.getUUID().equals(pillar.ownerForTest())&&pillar.ageForTest()==0),
            "Pillars lost their owner or began aging before their first world tick");
        pillars.forEach(net.minecraft.world.entity.Entity::discard);target.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="emissary_pillars",timeoutTicks=20)
    public static void emissary_cloud_pillar_moves_hits_with_source_effects_and_flame_never_places_blocks_pending_choice(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var victim=emissaryPlayer(helper,origin.add(0,6,0));
        EmissaryPillar.launch(level,boss);
        var pillars=level.getEntitiesOfClass(EmissaryPillar.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(8));
        var cloud=pillars.stream().filter(pillar->pillar.kindForTest()==EmissaryPillar.Kind.CLOUD).findFirst().orElseThrow();
        var flame=pillars.stream().filter(pillar->pillar.kindForTest()==EmissaryPillar.Kind.FLAME).findFirst().orElseThrow();
        cloud.ageForTest(25);var saved=new CompoundTag();cloud.saveWithoutId(saved);cloud.discard();
        var restored=Content.EMISSARY_PILLAR.get().create(level);restored.load(saved);level.addFreshEntity(restored);cloud=restored;
        helper.assertTrue(cloud.ageForTest()==25&&cloud.kindForTest()==EmissaryPillar.Kind.CLOUD&&boss.getUUID().equals(cloud.ownerForTest()),
            "Cloud pillar owner, kind, and lifetime did not survive saving");
        cloud.ageForTest(0);
        Vec3 cloudStart=cloud.position();cloud.tick();
        helper.assertTrue(cloud.position().distanceTo(cloudStart)>.39&&cloud.position().distanceTo(cloudStart)<.41,
            "Cloud pillar did not advance by the source 0.4 blocks per tick");
        helper.assertTrue(victim.getHealth()<200&&victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                &&victim.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                &&victim.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==2,
            "Cloud pillar contact did not apply source damage, blindness, and Slowness III");
        BlockPos min=BlockPos.containing(origin.add(-4,-4,-4)),max=BlockPos.containing(origin.add(4,20,4));
        for(BlockPos pos:BlockPos.betweenClosed(min,max))helper.assertTrue(!level.getBlockState(pos).is(Blocks.FIRE),
            "Unconfirmed Pillars fire placement modified a world block at "+pos);
        flame.ageForTest(EmissaryPillar.SOURCE_LIFETIME-1);flame.tick();
        helper.assertTrue(flame.isRemoved()&&flame.ageForTest()==EmissaryPillar.SOURCE_LIFETIME,
            "Pillars carrier did not stop at the source 300-tick maximum lifetime");
        cloud.discard();victim.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="emissary_pillars",timeoutTicks=20)
    public static void emissary_pillar_redirects_from_a_reflectance_node(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var reflectorOwner=emissaryPlayer(helper,origin.add(8,0,0));
        var reflector=SpellArea.create(level,reflectorOwner,origin.add(0,0,1.5),Spell.REFLECTANCE);reflector.setYRot(90);reflector.setXRot(0);
        var pillar=Content.EMISSARY_PILLAR.get().create(level);
        pillar.configureForTest(boss,EmissaryPillar.Kind.CLOUD);pillar.setPos(origin);pillar.setYRot(0);pillar.setXRot(0);level.addFreshEntity(pillar);
        pillar.tick();
        helper.assertTrue(reflectorOwner.getUUID().equals(pillar.damageOwnerForTest()),"Reflected pillar kept the emissary as its damage owner");
        helper.assertTrue(reflector.isRemoved(),"Pillars marker did not consume Reflectance when crossing its source 3-block check");
        helper.assertTrue(pillar.getX()<origin.x-.35,"Pillars marker did not resume along the reflector's facing after redirection: "+pillar.position());
        pillar.discard();reflectorOwner.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="flame_geysers_spread",timeoutTicks=1200)
    public static void flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,120);
        inLoadedTestArea(helper,origin,48,()->verifyGeyserFrameForty(helper,origin));
    }
    private static void verifyGeyserFrameForty(GameTestHelper helper,Vec3 origin){
        var level=helper.getLevel();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=emissaryPlayer(helper,origin.add(0,0,8));
        prepareGeyserGround(level,target.position(),26);
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,boss.getBoundingBox().inflate(300)).contains(target),
            "Flame Geysers fixture target was absent from the boss's 300-block player selector");
        try{
            var attackField=NativeBoss.class.getDeclaredField("attack");attackField.setAccessible(true);
            attackField.set(boss,new NativeBoss.Attack("phase2.attack.flame_geysers",71,40,NativeBoss.Pattern.PILLARS,Combat.Element.DISSONANCE));
            var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);
            var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);
            for(int frame:new int[]{10,20,25,30,33,36,38}){
                helper.assertTrue(EmissaryFlameGeyser.isHandCueFrame(frame),"A source Flame Geysers hand particle cue frame is missing: "+frame);
                Vec3 hand=EmissaryFlameGeyser.handCuePositionForTest(boss,frame);
                helper.assertTrue(Double.isFinite(hand.x)&&Double.isFinite(hand.y)&&Double.isFinite(hand.z),
                    "Righthand locator was not available for Flame Geysers frame "+frame);
                ageField.setInt(boss,frame);perform.invoke(boss,level,target);
            }
            ageField.setInt(boss,39);perform.invoke(boss,level,target);
            helper.assertTrue(level.getEntitiesOfClass(EmissaryFlameGeyser.class,geyserTestBounds(level,origin),node->boss.getUUID().equals(node.ownerForTest())).isEmpty(),
                "Flame Geyser nodes were created before source animation frame 40");
            ageField.setInt(boss,40);perform.invoke(boss,level,target);
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not invoke the Flame Geysers source animation fixture",failure);}
        var nodes=level.getEntitiesOfClass(EmissaryFlameGeyser.class,geyserTestBounds(level,origin),node->boss.getUUID().equals(node.ownerForTest()));
        helper.assertTrue(nodes.size()==EmissaryFlameGeyser.SOURCE_NODES_PER_TARGET,
            "One selected player must receive fourteen source spread nodes; got "+nodes.size());
        var group=nodes.get(0).groupForTest();var nodePositions=new java.util.ArrayList<Vec3>();
        int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,target.getBlockX(),target.getBlockZ());
        for(var node:nodes){
            helper.assertTrue(boss.getUUID().equals(node.ownerForTest())&&target.getUUID().equals(node.targetForTest())&&group.equals(node.groupForTest()),
                "A geyser node lost its owner, target group, or selected player");
            helper.assertTrue(node.timerForTest()>=EmissaryFlameGeyser.SOURCE_MIN_TIMER&&node.timerForTest()<=EmissaryFlameGeyser.SOURCE_MAX_TIMER,
                "Geyser timer escaped the source random 20..149 range: "+node.timerForTest());
            double dx=node.getX()-target.getX(),dz=node.getZ()-target.getZ(),distance=Math.sqrt(dx*dx+dz*dz);
            helper.assertTrue(distance<=25,
                "Spread geyser escaped the source 25-block maximum radius: node="+node.position()+" target="+target.position()+" radius="+distance);
            helper.assertTrue(node.getY()==level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,node.getBlockX(),node.getBlockZ()),
                "Spread geyser was not placed at the local surface height");
            for(Vec3 previous:nodePositions){
                double separationX=previous.x-node.getX(),separationZ=previous.z-node.getZ();
                helper.assertTrue(separationX*separationX+separationZ*separationZ>=25,
                    "Spread geyser nodes violated the source minimum five-block horizontal separation");
            }
            nodePositions.add(node.position());
        }
        nodes.forEach(net.minecraft.world.entity.Entity::discard);target.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="flame_geysers_groups",timeoutTicks=1200)
    public static void flame_geysers_caps_at_two_players_without_cross_spreading_node_groups(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,120);
        inLoadedTestArea(helper,origin,48,()->verifyGeyserGroups(helper,origin));
    }
    private static void verifyGeyserGroups(GameTestHelper helper,Vec3 origin){
        var level=helper.getLevel();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var p1=emissaryPlayer(helper,origin.add(-8,0,0));var p2=emissaryPlayer(helper,origin.add(8,0,0));var p3=emissaryPlayer(helper,origin.add(0,0,12));
        prepareGeyserGround(level,origin,38);
        int created=EmissaryFlameGeyser.launch(level,boss);
        var nodes=level.getEntitiesOfClass(EmissaryFlameGeyser.class,geyserTestBounds(level,origin),node->boss.getUUID().equals(node.ownerForTest()));
        var counts=nodes.stream().collect(java.util.stream.Collectors.groupingBy(EmissaryFlameGeyser::groupForTest,java.util.stream.Collectors.counting()));
        helper.assertTrue(created==28&&nodes.size()==28&&counts.size()==2&&counts.values().stream().allMatch(count->count==14),
            "Flame Geysers must create fourteen nodes for at most two selected players, without regrouping earlier nodes; created="+created+" count="+counts);
        helper.assertTrue(counts.keySet().stream().allMatch(id->id.equals(p1.getUUID())||id.equals(p2.getUUID())||id.equals(p3.getUUID())),
            "A geyser group targeted a player outside the activation candidate set");
        for(var node:nodes){
            net.minecraft.world.entity.player.Player target=java.util.stream.Stream.of(p1,p2,p3).filter(player->player.getUUID().equals(node.targetForTest())).findFirst().orElseThrow();
            double dx=node.getX()-target.getX(),dz=node.getZ()-target.getZ();
            helper.assertTrue(dx*dx+dz*dz<=625,
                "A selected player's node group was spread around another player's activation point");
        }
        nodes.forEach(net.minecraft.world.entity.Entity::discard);p1.discard();p2.discard();p3.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="flame_geysers",timeoutTicks=20)
    public static void flame_geyser_warns_erupt_vertical_damage_and_does_not_place_unconfirmed_fire(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(origin);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=emissaryPlayer(helper,origin.add(0,24,0));
        var geyser=Content.EMISSARY_FLAME_GEYSER.get().create(level);geyser.configureForTest(boss,target,21);geyser.setPos(origin);level.addFreshEntity(geyser);
        var saved=new CompoundTag();geyser.saveWithoutId(saved);geyser.discard();
        var restored=Content.EMISSARY_FLAME_GEYSER.get().create(level);restored.load(saved);level.addFreshEntity(restored);
        helper.assertTrue(restored.timerForTest()==21&&boss.getUUID().equals(restored.ownerForTest())&&target.getUUID().equals(restored.targetForTest()),
            "Geyser owner, selected group, or delay did not survive a save");
        restored.timerForTest(41);restored.tick();
        helper.assertTrue(restored.timerForTest()==40&&restored.warningTicksForTest()==1,
            "The source warning ring did not begin at countdown 40");
        restored.timerForTest(1);restored.tick();
        helper.assertTrue(restored.isRemoved()&&restored.timerForTest()==0,"Geyser did not erupt and expire at source timer zero");
        helper.assertTrue(restored.warningTicksForTest()==1,"Geyser emitted a second warning ring on its source eruption tick");
        helper.assertTrue(target.getHealth()<200,"Geyser vertical 50-step selector did not hit a player above its ground node");
        BlockPos min=BlockPos.containing(origin.add(-4,-4,-4)),max=BlockPos.containing(origin.add(4,4,4));
        for(BlockPos pos:BlockPos.betweenClosed(min,max))helper.assertTrue(!level.getBlockState(pos).is(Blocks.FIRE),
            "An unconfirmed Flame Geysers fire fill modified a world block at "+pos);
        target.discard();boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="hypertrace",timeoutTicks=20)
    public static void nightmare_and_void_player_hits_create_owned_traces_at_victim(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);target.setNoAi(true);target.setNoGravity(true);target.setPos(center.add(2,0,0));level.addFreshEntity(target);
        helper.assertTrue(Combat.hit(owner,target,1,Combat.Element.NIGHTMARE,bound->{helper.assertTrue(bound==100,"Hit trace used non-source roll range");return 15;}),
            "Nightmare damage did not reach its target");
        var traces=NativeEvents.traces(owner);
        helper.assertTrue(traces.size()==1&&traces.get(0).ownedBy(owner)&&traces.get(0).position().distanceToSqr(target.position().add(0,1,0))<1.0e-5,
            "Nightmare hit trace must belong to the attacker and spawn one block above the victim context");
        var radianceTarget=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);radianceTarget.setNoAi(true);radianceTarget.setNoGravity(true);radianceTarget.setPos(center.add(2,0,1));level.addFreshEntity(radianceTarget);
        helper.assertTrue(Combat.hit(owner,radianceTarget,1,Combat.Element.RADIANCE,bound->0),"Radiance damage setup failed");
        helper.assertTrue(NativeEvents.traces(owner).size()==1,"A non-Nightmare/non-Void hit incorrectly rolled for a trace");
        target.invulnerableTime=20;
        helper.assertTrue(!Combat.hit(owner,target,1,Combat.Element.VOID,bound->{helper.assertTrue(bound==100,"Void hit trace used non-source roll range");return 0;}),
            "Invulnerable target unexpectedly accepted the damage");
        helper.assertTrue(NativeEvents.traces(owner).size()==2,"The source hit function rolls after damage application even when the victim rejects hurt");
        target.discard();radianceTarget.discard();owner.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="hypertrace",timeoutTicks=20)
    public static void accumulation_counts_owned_traces_inside_spherical_six_block_range(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var other=encounterTestPlayer(helper,center.add(0,0,2),net.minecraft.world.level.GameType.SURVIVAL);
        owner.getInventory().add(new ItemStack(Content.get("artifact_accumulation")));
        var inside=new SeelicField(Content.FIELD.get(),level);inside.configure(owner,false,true);inside.setPos(center.add(4,4,0));level.addFreshEntity(inside);
        var outside=new SeelicField(Content.FIELD.get(),level);outside.configure(owner,false,true);outside.setPos(center.add(5.5,4,0));level.addFreshEntity(outside);
        var foreign=new SeelicField(Content.FIELD.get(),level);foreign.configure(other,false,true);foreign.setPos(center.add(1,0,0));level.addFreshEntity(foreign);
        helper.assertTrue(NativeEvents.traces(owner).size()==1,"Accumulation neighborhood must exclude diagonal traces beyond the spherical six-block boundary and other owners");
        helper.assertTrue(HypertraceService.chance(owner)==25,"One nearby owned trace must add the default 10 points to the 15-point global trace chance");
        inside.discard();outside.discard();foreign.discard();owner.discard();other.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="hypertrace",timeoutTicks=20)
    public static void hypertrace_rotates_repels_and_survives_owner_loss_without_transposition_follow(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        owner.getInventory().add(new ItemStack(Content.get("artifact_transposition")));
        var trace=new SeelicField(Content.FIELD.get(),level);trace.configure(owner,false,true);trace.setPos(center.add(4,0,0));trace.setYRot(0);level.addFreshEntity(trace);
        Vec3 fixedPosition=trace.position();trace.tick();
        helper.assertTrue(trace.position().distanceToSqr(fixedPosition)<1.0e-8&&trace.getYRot()==1&&trace.remainingLife()==399,
            "A lone trace must rotate and age without following its Transposition holder");
        var neighbor=new SeelicField(Content.FIELD.get(),level);neighbor.configure(owner,false,true);neighbor.setPos(trace.position().add(.5,0,0));level.addFreshEntity(neighbor);
        double before=trace.distanceTo(neighbor);trace.tick();
        helper.assertTrue(trace.distanceTo(neighbor)>before+.19,"Traces within one block must move 0.2 blocks away from one another");
        owner.discard();
        for(int tick=0;tick<397;tick++)trace.tick();
        helper.assertTrue(!trace.isRemoved()&&trace.remainingLife()==1,"An owner disconnect must not remove a trace before its 400-tick source lifetime");
        trace.tick();helper.assertTrue(trace.isRemoved(),"An ownerless trace must still be removed at 400 ticks");
        neighbor.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="hypertrace",timeoutTicks=20)
    public static void hypertrace_pickup_and_destruction_are_limited_to_its_owner(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 center=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=encounterTestPlayer(helper,center,net.minecraft.world.level.GameType.SURVIVAL);
        var other=encounterTestPlayer(helper,center.add(1,0,0),net.minecraft.world.level.GameType.SURVIVAL);
        var picked=new SeelicField(Content.FIELD.get(),level);picked.configure(owner,false,true);picked.setPos(center);level.addFreshEntity(picked);
        helper.assertTrue(picked.interact(other,net.minecraft.world.InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.PASS&&!picked.isRemoved(),
            "A foreign player must not pick up another player's Hypertrace");
        helper.assertTrue(picked.interact(owner,net.minecraft.world.InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.CONSUME
            &&picked.isRemoved()&&NativeEvents.hypertraceCharges(owner)==1,
            "The trace owner must receive one charge when picking up their Hypertrace");
        var destroyed=new SeelicField(Content.FIELD.get(),level);destroyed.configure(owner,false,true);destroyed.setPos(center.add(0,0,1));level.addFreshEntity(destroyed);
        helper.assertTrue(!destroyed.hurt(level.damageSources().playerAttack(other),1)&&!destroyed.isRemoved(),
            "A foreign player must not destroy another player's Hypertrace");
        helper.assertTrue(destroyed.hurt(level.damageSources().playerAttack(owner),1)&&destroyed.isRemoved(),
            "The trace owner must be able to destroy their Hypertrace");
        owner.discard();other.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="crypt_slatehenge",timeoutTicks=20)
    public static void crypt_and_slatehenge_templates_keep_only_native_event_entities(GameTestHelper helper){
        var level=helper.getLevel();
        var crypt=level.getStructureManager().get(new net.minecraft.resources.ResourceLocation("structures","crypt/hallways/generic/end")).orElseThrow();
        var slatehenge=level.getStructureManager().get(new net.minecraft.resources.ResourceLocation(NativeMod.ID,"slatehenge")).orElseThrow();
        helper.assertTrue(templateHasEntity(crypt,"nyctocosm:crypt_slatehenge_trigger"),"Crypt end piece lost its native Slatehenge trigger event");
        helper.assertTrue(templateHasEntity(slatehenge,"nyctocosm:slatehenge_effect"),"Slatehenge lost its native persistent effect event");
        helper.succeed();
    }

    @GameTest(template="empty",batch="crypt_slatehenge",timeoutTicks=20)
    public static void slatehenge_landmark_radius_is_inclusive_at_200_blocks(GameTestHelper helper){
        Vec3 origin=Vec3.ZERO;
        helper.assertTrue(CryptSlatehengeTrigger.withinRadius(origin,new Vec3(200,0,0),200*200),
            "A Slatehenge exactly 200 blocks away should suppress another placement");
        helper.assertTrue(!CryptSlatehengeTrigger.withinRadius(origin,new Vec3(200.01,0,0),200*200),
            "A Slatehenge beyond the source 200-block radius must not suppress placement");
        helper.succeed();
    }

    @GameTest(template="empty",batch="crypt_slatehenge",timeoutTicks=20)
    public static void slatehenge_effect_tracks_solid_marker_and_discards_in_air(GameTestHelper helper){
        var level=helper.getLevel();var solid=helper.absolutePos(new BlockPos(3,4,3));var air=helper.absolutePos(new BlockPos(7,4,3));
        level.setBlockAndUpdate(solid,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var active=Content.SLATEHENGE_EFFECT.get().create(level);active.setPos(solid.getX()+.5,solid.getY()+.5,solid.getZ()+.5);level.addFreshEntity(active);
        var ended=Content.SLATEHENGE_EFFECT.get().create(level);ended.setPos(air.getX()+.5,air.getY()+.5,air.getZ()+.5);level.addFreshEntity(ended);
        active.tick();ended.tick();
        helper.assertTrue(!active.isRemoved(),"The firework event should persist while its marker block is solid");
        helper.assertTrue(ended.isRemoved(),"The firework event should end when its marker block is air");
        active.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="crypt_slatehenge",timeoutTicks=20)
    public static void crypt_trigger_is_idempotent_when_slatehenge_is_already_nearby(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();var marker=helper.absolutePos(new BlockPos(3,4,3));
        var surface=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,marker);
        var landmark=Content.SLATEHENGE_EFFECT.get().create(level);
        landmark.setPos(surface.getX()+.5,surface.getY()+14,surface.getZ()+.5);level.addFreshEntity(landmark);
        helper.assertTrue(CryptSlatehengeTrigger.process(level,marker),"A nearby landmark should consume the trigger without placing a duplicate");
        helper.assertTrue(StructureEventSavedData.get(level).processed(marker),"The suppressed trigger should be persisted as processed");
        landmark.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="crypt_slatehenge",timeoutTicks=20)
    public static void crypt_trigger_processed_positions_survive_saved_data_reload(GameTestHelper helper){
        var marker=helper.absolutePos(new BlockPos(3,4,3));var first=new StructureEventSavedData();
        first.markProcessed(marker);var restored=StructureEventSavedData.load(first.save(new CompoundTag()));
        helper.assertTrue(restored.processed(marker),"Processed crypt positions must survive native world save/load");
        helper.succeed();
    }

    @GameTest(template="empty",batch="crypt_slatehenge_placement",timeoutTicks=40)
    public static void crypt_trigger_places_one_rotated_slatehenge_with_native_effect(GameTestHelper helper){
        var server=helper.getLevel().getServer();
        var dimension=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain"));
        var level=server.getLevel(dimension);helper.assertTrue(level!=null,"Emissary test dimension failed to load");
        BlockPos marker=new BlockPos(8192,100,8192);
        int chunkX=marker.getX()>>4,chunkZ=marker.getZ()>>4;
        for(int x=chunkX-2;x<=chunkX+2;x++)for(int z=chunkZ-2;z<=chunkZ+2;z++)level.getChunk(x,z);
        BlockPos surface=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,marker);
        Vec3 origin=new Vec3(surface.getX()+.5,surface.getY(),surface.getZ()+.5);
        helper.assertTrue(!CryptSlatehengeTrigger.hasNearbyLandmark(level,origin,200),"Placement fixture unexpectedly has a landmark within range");
        helper.assertTrue(CryptSlatehengeTrigger.process(level,marker),"Crypt trigger failed to place the available Slatehenge template");
        helper.assertTrue(StructureEventSavedData.get(level).processed(marker),"A successful placement must persist the one-shot trigger");
        var effects=level.getEntitiesOfClass(SlatehengeEffect.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(40));
        helper.assertTrue(effects.size()==1,"Placed Slatehenge should instantiate exactly one native firework effect entity; got "+effects.size());
        effects.forEach(effect->effect.discard());helper.succeed();
    }
    private static boolean templateHasEntity(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,String id){
        var entities=template.save(new CompoundTag()).getList("entities",10);
        for(int i=0;i<entities.size();i++)if(entities.getCompound(i).getCompound("nbt").getString("id").equals(id))return true;
        return false;
    }

    @GameTest(template="empty",batch="emissary_damage_parts",timeoutTicks=20)
    public static void emissary_phase_one_wing_hitboxes_forward_damage(GameTestHelper helper){
        var level=helper.getLevel();var boss=emissaryDamageFixture(helper);
        try{
            helper.assertTrue(boss.isMultipartEntity(),"Emissary does not expose multipart damage hitboxes");
            var parts=boss.getParts();helper.assertTrue(parts!=null&&parts.length==4,"Phase one must expose the four source wing hitboxes");
            for(var part:parts)helper.assertTrue(part.isPickable(),"All four phase-one wing proxies must remain targetable");
            var sourceSlime=(net.minecraft.world.entity.monster.Slime)EntityType.SLIME.create(level);sourceSlime.setSize(4,false);
            helper.assertTrue(Math.abs(parts[0].getBbWidth()-sourceSlime.getBbWidth())<.001f&&Math.abs(parts[0].getBbHeight()-sourceSlime.getBbHeight())<.001f,
                "Phase-one proxies must use the measured source Size:4 slime dimensions; part="+parts[0].getBbWidth()+", source="+sourceSlime.getBbWidth());
            Vec3 leftLocator=sourceLocalPoint(boss.position(),boss.getYRot(),boss.getXRot(),-8.33337,-6.96043,5.18305);
            Vec3 sourceLeftLower=sourceLocalPoint(leftLocator,boss.getYRot()-174.66594f,boss.getXRot()-4.43438f,-3,2,0);
            helper.assertTrue(parts[0].position().distanceToSqr(sourceLeftLower)<.00001,
                "First wing hitbox did not follow the source frame-0 locator and local ^-3 ^2 ^ offset: "+parts[0].position()+" vs "+sourceLeftLower);
            float before=boss.getHealth();
            helper.assertTrue(parts[0].hurt(level.damageSources().generic(),4),"Damage to an Emissary hitbox was not forwarded through NativeBoss.hurt");
            helper.assertTrue(boss.getHealth()<before,"Hitbox damage did not change the owning Emissary health");
        }finally{boss.discard();}
        helper.succeed();
    }

    @GameTest(template="empty",batch="emissary_damage_parts",timeoutTicks=20)
    public static void emissary_wing_parts_take_area_spell_damage(GameTestHelper helper){
        var boss=emissaryDamageFixture(helper);
        try{
            var part=boss.getParts()[0];float before=boss.getHealth();
            Combat.area((net.minecraft.server.level.ServerLevel)helper.getLevel(),null,part.position().add(0,.5,0),.1,9,Combat.Element.RADIANCE,0);
            helper.assertTrue(boss.getHealth()<before,"Area spell lookup ignored the Emissary wing multipart boxes");
        }finally{boss.discard();}
        helper.succeed();
    }

    @GameTest(template="empty",batch="emissary_damage_parts",timeoutTicks=20)
    public static void emissary_wing_parts_take_ranged_spell_damage(GameTestHelper helper){
        var level=helper.getLevel();var boss=emissaryDamageFixture(helper);var owner=EntityType.COW.create(level);
        owner.setNoAi(true);owner.setPos(boss.getX()+80,boss.getY(),boss.getZ());level.addFreshEntity(owner);
        try{
            var part=boss.getParts()[0];Vec3 start=part.position().add(-4,.5,0);float before=boss.getHealth();
            var bolt=SpellBolt.launch((net.minecraft.server.level.ServerLevel)level,owner,start,new Vec3(1,0,0),12,Combat.Element.DREAM,1,0);
            for(int tick=0;tick<10&&!bolt.isRemoved()&&boss.getHealth()==before;tick++)bolt.tick();
            helper.assertTrue(boss.getHealth()<before,"SpellBolt could not resolve a projectile hit on an Emissary wing part");
        }finally{boss.discard();owner.discard();}
        helper.succeed();
    }

    @GameTest(template="empty",batch="emissary_damage_parts",timeoutTicks=20)
    public static void emissary_wing_parts_take_vanilla_arrow_damage(GameTestHelper helper){
        var level=helper.getLevel();var boss=emissaryDamageFixture(helper);var owner=EntityType.COW.create(level);
        owner.setNoAi(true);owner.setPos(boss.getX()+80,boss.getY(),boss.getZ());level.addFreshEntity(owner);
        try{
            var part=boss.getParts()[0];Vec3 start=part.position().add(-4,.5,0);float before=boss.getHealth();
            var arrow=(net.minecraft.world.entity.projectile.Arrow)EntityType.ARROW.create(level);
            arrow.setOwner(owner);arrow.setNoGravity(true);arrow.setPos(start);arrow.setDeltaMovement(1,0,0);level.addFreshEntity(arrow);
            for(int tick=0;tick<10&&!arrow.isRemoved()&&boss.getHealth()==before;tick++)arrow.tick();
            helper.assertTrue(boss.getHealth()<before,"A vanilla arrow could not hit an Emissary wing part through Forge multipart collision");
        }finally{boss.discard();owner.discard();}
        helper.succeed();
    }

    private static NativeBoss emissaryDamageFixture(GameTestHelper helper){
        var boss=Content.EMISSARY.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());boss.setNoAi(true);
        resetEmissaryAnimation(boss,1);helper.getLevel().addFreshEntity(boss);boss.tick();return boss;
    }

    @GameTest(template="empty",batch="emissary_damage_parts",timeoutTicks=20)
    public static void emissary_hitbox_type_changes_with_phase(GameTestHelper helper){
        var level=helper.getLevel();var boss=Content.EMISSARY.get().create(level);
        boss.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());boss.setNoAi(true);level.addFreshEntity(boss);
        try{
            resetEmissaryAnimation(boss,2);boss.tick();var parts=boss.getParts();
            helper.assertTrue(parts.length==4&&parts[0].isPickable()&&!parts[1].isPickable()&&!parts[2].isPickable()&&!parts[3].isPickable(),
                "Phase two must replace the four wings with one Giant hitbox");
            helper.assertTrue(Math.abs(parts[0].getBbWidth()-EntityType.GIANT.getDimensions().width)<.01f
                &&Math.abs(parts[0].getBbHeight()-EntityType.GIANT.getDimensions().height)<.01f,"Phase-two damage body does not match EntityType.GIANT");
            Vec3 phaseTwoCenter=sourceLocalPoint(boss.position(),boss.getYRot(),boss.getXRot(),0,5.4375,0).add(0,-1.3,0);
            helper.assertTrue(parts[0].position().distanceToSqr(phaseTwoCenter)<.00001,"Phase-two Giant did not follow the center locator minus the source 1.3-block offset");
            resetEmissaryAnimation(boss,3);boss.tick();
            helper.assertTrue(parts[0].isPickable()&&!parts[1].isPickable()
                &&Math.abs(parts[0].getBbWidth()-EntityType.WITHER_SKELETON.getDimensions().width)<.01f
                &&Math.abs(parts[0].getBbHeight()-EntityType.WITHER_SKELETON.getDimensions().height)<.01f,
                "Phase three must replace the Giant with a Wither Skeleton hitbox");
            Vec3 phaseThreeCenter=sourceLocalPoint(boss.position(),boss.getYRot(),boss.getXRot(),0,1.48097,.09567).add(0,-1.3,0);
            helper.assertTrue(parts[0].position().distanceToSqr(phaseThreeCenter)<.00001,"Phase-three hitbox missed its animated center locator");
            resetEmissaryAnimation(boss,4);boss.tick();
            helper.assertTrue(parts[0].isPickable()&&!parts[1].isPickable()
                &&Math.abs(parts[0].getBbWidth()-EntityType.WITHER_SKELETON.getDimensions().width)<.01f,
                "Phase four must retain the Wither Skeleton damage body");
            Vec3 phaseFourCenter=sourceLocalPoint(boss.position(),boss.getYRot(),boss.getXRot(),0,4.34375,-.0625).add(0,-1.3,0);
            helper.assertTrue(parts[0].position().distanceToSqr(phaseFourCenter)<.00001,"Grounded phase-four hitbox missed its animated center locator");
        }finally{boss.discard();}
        helper.succeed();
    }
    private static void resetEmissaryAnimation(NativeBoss boss,int phase){
        var save=new CompoundTag();boss.saveWithoutId(save);save.putInt("NativePhase",phase);boss.load(save);
    }
    private static Vec3 sourceLocalPoint(Vec3 origin,float yaw,float pitch,double x,double y,double z){
        Vec3 forward=Vec3.directionFromRotation(pitch,yaw);double radians=Math.toRadians(yaw);
        Vec3 left=new Vec3(Math.cos(radians),0,Math.sin(radians));return origin.add(left.scale(x)).add(forward.cross(left).scale(y)).add(forward.scale(z));
    }

    @GameTest(template="empty",batch="emissary_journey")
    public static void safe_arrival_checks_unloaded_solid_space_and_boss_clearance(GameTestHelper helper){
        var domain=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var player=helper.makeMockPlayer();Vec3 underground=new Vec3(2048.5,-30,2048.5);player.setPos(underground);
        helper.assertTrue(!SafeTravel.canStand(domain,player,underground),"An unvisited chunk was accepted before its solid blocks were loaded");
        var boss=Content.EMISSARY.get().create(domain);var playerLanding=NativeEvents.findArrival(domain,player,underground).orElseThrow();
        var bossLanding=NativeEvents.findEmissaryLanding(domain,boss,playerLanding);
        helper.assertTrue(bossLanding.isPresent()&&bossLanding.get().y==193&&SafeTravel.canStand(domain,boss,bossLanding.get()),"Incurrence failed to find a separate surface landing for the full 2x5 boss body");
        helper.assertTrue(bossLanding.get().distanceTo(playerLanding)>=16,"Boss landing overlaps the arriving player");helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void surface_arrival_ignores_grass_and_canopy(GameTestHelper helper){
        var level=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var player=helper.makeMockPlayer();var floor=new BlockPos(4200,253,4200);
        var oldFloor=level.getBlockState(floor);var oldGrass=level.getBlockState(floor.above());var oldLeaves=level.getBlockState(floor.above(6));
        try{
        level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.above(),net.minecraft.world.level.block.Blocks.GRASS.defaultBlockState());
        level.setBlockAndUpdate(floor.above(6),net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState());
        var at=SafeTravel.findSurface(level,player,Vec3.atBottomCenterOf(floor),0);
        helper.assertTrue(at.isPresent()&&at.get().equals(Vec3.atBottomCenterOf(floor.above())),"Surface search selected foliage or failed to find walkable ground beneath a canopy: "+at);
        }finally{level.setBlockAndUpdate(floor,oldFloor);level.setBlockAndUpdate(floor.above(),oldGrass);level.setBlockAndUpdate(floor.above(6),oldLeaves);}helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void emissary_arrival_and_hover_survive_entity_recreation(GameTestHelper helper){
        var domain=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var boss=Content.EMISSARY.get().create(domain);boss.moveTo(600.5,193,600.5,90,0);boss.setNoAi(true);domain.addFreshEntity(boss);
        try{
            boss.beginEntrance();for(int tick=0;tick<37;tick++)boss.customServerAiStep();
            helper.assertTrue(!boss.hurt(domain.damageSources().generic(),20),"Distant arrival can be damaged before combat starts");
            var saved=new CompoundTag();boss.saveWithoutId(saved);Vec3 before=boss.position();boss.discard();
            boss=Content.EMISSARY.get().create(domain);boss.load(saved);domain.addFreshEntity(boss);
            helper.assertTrue(boss.emissaryApproaching()&&boss.position().distanceTo(before)<.001,"Reload restarted or skipped the meteor approach");
            for(int tick=37;tick<95;tick++)boss.customServerAiStep();
            helper.assertTrue(boss.animation().equals("spawn"),"Reloaded approach did not finish at tick 95");
            for(int tick=0;tick<141;tick++)boss.customServerAiStep();
            before=boss.position();saved=new CompoundTag();boss.saveWithoutId(saved);boss.discard();
            boss=Content.EMISSARY.get().create(domain);boss.load(saved);domain.addFreshEntity(boss);
            for(int tick=0;tick<30;tick++)boss.tick();
            helper.assertTrue(boss.animation().equals("phase1_idle")&&boss.isNoGravity()&&boss.position().distanceTo(before)<.001,"Reload lost the established phase-one hover anchor");
        }finally{boss.discard();}helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void emissary_phase_two_transition_descends_to_surface(GameTestHelper helper){
        var domain=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var boss=Content.EMISSARY.get().create(domain);var saved=new CompoundTag();boss.saveWithoutId(saved);saved.putInt("NativePhase",2);boss.load(saved);
        boss.moveTo(800.5,230,800.5,0,0);boss.setHealth(590);boss.setNoAi(true);domain.addFreshEntity(boss);
        var target=EntityType.COW.create(domain);target.moveTo(800.5,193,880.5,0,0);target.setNoAi(true);domain.addFreshEntity(target);boss.setTarget(target);
        try{
            boss.customServerAiStep();for(int tick=0;tick<60;tick++)boss.customServerAiStep();
            helper.assertTrue(Math.abs(boss.getZ()-831.5)<.01&&boss.getY()==193&&SafeTravel.canStand(domain,boss,boss.position()),"Transition skipped source retreat/dive/20-block slide or landed underground: "+boss.position());
            for(int tick=60;tick<101;tick++)boss.customServerAiStep();
            helper.assertTrue(boss.phase()==3&&boss.animation().equals("phase3_idle")&&!boss.isNoGravity(),"Grounded third phase did not replace the aerial second phase");
        }finally{boss.discard();target.discard();}helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void emissary_legacy_grounded_phase_one_recovers_once(GameTestHelper helper){
        var domain=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var boss=Content.EMISSARY.get().create(domain);boss.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(80);var saved=new CompoundTag();boss.saveWithoutId(saved);saved.remove("EmissaryHoverEstablished");boss.load(saved);
        boss.moveTo(1000.5,-30,1000.5,0,0);boss.setNoAi(true);domain.addFreshEntity(boss);
        try{
            boss.customServerAiStep();Vec3 recovered=boss.position();
            helper.assertTrue(recovered.y>223&&boss.isNoGravity(),"Legacy underground phase-one save was not raised above the arena");
            helper.assertTrue(boss.getAttributeValue(Attributes.FOLLOW_RANGE)==300,"Legacy saved attributes still override the corrected 300-block targeting range");
            boss.customServerAiStep();helper.assertTrue(boss.position().equals(recovered),"Legacy recovery keeps moving an already-established hover anchor");
        }finally{boss.discard();}helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void emissary_airborne_ai_acquires_distant_player(GameTestHelper helper){
        var domain=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var boss=Content.EMISSARY.get().create(domain);boss.moveTo(1600.5,225,1600.5,0,0);domain.getChunkAt(boss.blockPosition());domain.addFreshEntity(boss);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(domain,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EmissaryRange"));
        player.moveTo(1600.5,193,1710.5,0,0);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);domain.getChunkAt(player.blockPosition());domain.addNewPlayer(player);
        try{
            for(int tick=0;tick<50;tick++)boss.tick();
            helper.assertTrue(boss.getTarget()==player,"Airborne AI failed to acquire a ground player beyond the former 80-block follow range");
        }finally{boss.discard();player.discard();}helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void dimension_arrival_matches_execute_in_coordinate_scale(GameTestHelper helper){
        var server=helper.getLevel().getServer();
        var nyctocosm=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"nyctocosm")));
        var domain=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(nyctocosm,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ArrivalScale"));player.setPos(160.5,-30,160.5);
        Vec3 expected=player.createCommandSourceStack().withLevel(domain).getPosition();
        var actual=NativeEvents.findArrival(domain,player,player.position());
        helper.assertTrue(actual.isPresent()&&actual.get().multiply(1,0,1).distanceTo(expected.multiply(1,0,1))<1&&actual.get().y==193,"Surface entry did not preserve original execute-in XZ scaling: expected="+expected+", actual="+actual);helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void dimension_arrival_uses_surface_but_return_keeps_cave(GameTestHelper helper){
        var level=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var player=helper.makeMockPlayer();BlockPos floor=new BlockPos(4300,253,4300);var oldFloor=level.getBlockState(floor);var oldRoof=level.getBlockState(floor.above(5));
        try{
        level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.above(5),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        Vec3 cave=Vec3.atBottomCenterOf(floor.above());player.setPos(cave);
        var entry=NativeEvents.findArrival(level,player,cave);
        helper.assertTrue(entry.isPresent()&&entry.get().y==floor.getY()+6,"Cross-dimension entry reused the old cave height instead of the roof surface: "+entry);
        helper.assertTrue(SafeTravel.find(level,player,cave).orElseThrow().equals(cave),"Returning to the original underground portal no longer preserves the safe entrance");
        }finally{level.setBlockAndUpdate(floor,oldFloor);level.setBlockAndUpdate(floor.above(5),oldRoof);}helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void emissary_domain_arrival_uses_generated_surface(GameTestHelper helper){
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain"));
        var domain=helper.getLevel().getServer().getLevel(key);helper.assertTrue(domain!=null,"Missing Emissary dimension");
        var player=helper.makeMockPlayer();Vec3 old=new Vec3(240.5,-30,240.5);player.setPos(old);
        var landing=NativeEvents.findArrival(domain,player,old);
        helper.assertTrue(landing.isPresent()&&landing.get().y==193&&SafeTravel.canStand(domain,player,landing.get()),"Generated domain entry did not place the full player above its 257-layer surface: "+landing);helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void emissary_entrance_includes_distant_approach_and_retreat(GameTestHelper helper){
        var domain=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"emissary_domain")));
        var boss=Content.EMISSARY.get().create(domain);Vec3 ground=new Vec3(400.5,193,400.5);
        boss.setPos(ground);boss.setYRot(0);boss.setNoAi(true);domain.addFreshEntity(boss);
        try{
            boss.beginEntrance();Vec3 approach=boss.position();
            helper.assertTrue(approach.distanceTo(ground)>90&&approach.y>ground.y+60,"Emissary skipped the 100-block sloping approach: "+approach);
            for(int tick=0;tick<95;tick++)boss.customServerAiStep();
            helper.assertTrue(boss.animation().equals("spawn")&&boss.position().distanceTo(ground)<6,"Arrival did not hand off to the original landing animation after 95 ticks");
            for(int tick=0;tick<8;tick++)boss.customServerAiStep();helper.assertTrue(boss.position().distanceTo(ground)<.01,"Landing's five one-block command steps failed to finish on the surface");
            for(int tick=8;tick<141;tick++)boss.customServerAiStep();
            helper.assertTrue(boss.animation().equals("phase1_idle")&&boss.getY()>ground.y+30&&boss.isNoGravity(),"Phase one did not retain the height gained by spawn frames 62..98");
            Vec3 hover=boss.position();for(int tick=0;tick<40;tick++)boss.tick();
            helper.assertTrue(boss.position().distanceTo(hover)<.01,"Phase-one idle fell out of the sky under ordinary entity physics");
        }finally{boss.discard();}helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_journey")
    public static void emissary_phase_one_attack_does_not_fall_under_physics(GameTestHelper helper){
        var boss=Content.EMISSARY.get().create(helper.getLevel());var target=helper.spawn(EntityType.COW,new BlockPos(5,2,5));target.setNoAi(true);
        boss.setPos(target.getX(),helper.getLevel().getMaxBuildHeight()-60,target.getZ()-15);boss.setNoAi(true);helper.getLevel().addFreshEntity(boss);
        try{
            Vec3 anchor=boss.position();boss.startEmissaryAttackForTest(target,"phase1.attack.lightning_javelin");
            for(int tick=0;tick<30;tick++){boss.customServerAiStep();boss.tick();}
            helper.assertTrue(boss.isNoGravity()&&boss.position().distanceTo(anchor)<.01,"Phase-one ranged attack lost its hover anchor under actual Mob ticking: "+boss.position());
            var bolts=helper.getLevel().getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(90),bolt->bolt.getOwner()==boss);
            helper.assertTrue(!bolts.isEmpty(),"Hovering Emissary stopped emitting its ranged attack");bolts.forEach(net.minecraft.world.entity.Entity::discard);
        }finally{boss.discard();target.discard();}helper.succeed();
    }
    @AfterBatch(batch="emissary")
    public static void cleanupEmissaryFixtures(net.minecraft.server.level.ServerLevel level){
        var fixtures=new java.util.HashSet<java.util.UUID>();
        for(var entity:level.getAllEntities())if(entity.getTags().contains("nyctocosm_emissary_test"))fixtures.add(entity.getUUID());
        var cleanup=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        for(var entity:level.getAllEntities()){
            var data=new CompoundTag();
            if(entity instanceof EmissaryHazard||entity instanceof EmissaryHitRegion||entity instanceof EmissaryGrappleSeat||entity instanceof EmissaryRainShard)entity.saveWithoutId(data);
            if(fixtures.contains(entity.getUUID())||(data.hasUUID("Owner")&&fixtures.contains(data.getUUID("Owner")))
                ||(entity instanceof SpellBolt bolt&&bolt.getOwner()!=null&&fixtures.contains(bolt.getOwner().getUUID())))cleanup.add(entity);
        }
        cleanup.forEach(net.minecraft.world.entity.Entity::discard);
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_airborne_wingbeat_reaches_ground_as_wave(GameTestHelper helper){
        var boss=emissaryFixture(helper,1);var player=emissaryPlayer(helper,boss.position().add(0,-32,45));
        boss.startEmissaryAttackForTest(player,"phase1.attack.wingbeat");for(int tick=0;tick<23;tick++)boss.customServerAiStep();
        var nodes=emissaryHazards(helper,boss);helper.assertTrue(nodes.size()==16,"Airborne Wingbeat still uses an instant 10-block hit instead of its 4x4 advancing wave");
        helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION),"Wingbeat hit the ground player before the wave travelled");
        for(int tick=0;tick<30;tick++)for(var node:nodes)if(!node.isRemoved())node.tick();
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)&&player.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION).getAmplifier()==30,"Wingbeat did not travel down from its aerial caster and apply the source levitation");
        helper.assertTrue(player.getHealth()==200,"Wingbeat incorrectly added direct damage to its displacement effect");
        nodes.forEach(net.minecraft.world.entity.Entity::discard);player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_wingbeat_retains_clock_and_respects_protection(GameTestHelper helper){
        var boss=emissaryFixture(helper,1);var player=emissaryPlayer(helper,boss.position().add(0,0,30));
        var at=player.position().add(0,0,-6);var node=EmissaryHazard.create(helper.getLevel(),boss,player,EmissaryHazard.Kind.WINGBEAT,at);
        player.getPersistentData().putLong("NyctocosmImmoveableUntil",helper.getLevel().getGameTime()+100);
        node.tick();helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)&&!node.isRemoved(),"Wingbeat ignored movement protection or consumed its wave on a protected player");
        var saved=new CompoundTag();node.saveWithoutId(saved);Vec3 before=node.position();node.discard();
        var resumed=Content.EMISSARY_HAZARD.get().create(helper.getLevel());resumed.load(saved);helper.getLevel().addFreshEntity(resumed);resumed.tick();
        var after=new CompoundTag();resumed.saveWithoutId(after);
        helper.assertTrue(after.getInt("WingbeatSteps")==saved.getInt("WingbeatSteps")+3&&Math.abs(resumed.position().distanceTo(before)-3)<.001,"Reload lost Wingbeat's three one-block steps per tick");
        player.getPersistentData().remove("NyctocosmImmoveableUntil");resumed.tick();
        helper.assertTrue(resumed.isRemoved()&&player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION),"Wave did not hit once movement protection ended");
        player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="reflectance",timeoutTicks=20)
    public static void emissary_wingbeat_redirects_once_per_reflector_owner(GameTestHelper helper){
        var boss=emissaryFixture(helper,1);Vec3 origin=boss.position().add(0,0,0);
        var reflectorOwner=emissaryPlayer(helper,origin.add(8,0,0));
        var shield=SpellArea.create(helper.getLevel(),reflectorOwner,origin.add(0,0,1.5),Spell.REFLECTANCE);
        shield.setYRot(90);shield.setXRot(0);
        var node=EmissaryHazard.create(helper.getLevel(),boss,reflectorOwner,EmissaryHazard.Kind.WINGBEAT,origin);
        node.setYRot(0);node.setXRot(0);node.tick();
        CompoundTag saved=new CompoundTag();shield.saveWithoutId(saved);
        helper.assertTrue(reflectorOwner.getUUID().equals(node.damageOwnerIdForTest()),"Wingbeat reflection did not transfer attack ownership");
        helper.assertTrue(node.getX()<origin.x-2.9,"Wingbeat did not continue along the reflector's facing for its remaining substeps");
        helper.assertTrue(Math.abs(saved.getFloat("ReflectanceEnergy")-15)<.001&&!shield.isRemoved(),
            "One reflector spent capacity more than once during a single source tick");
        node.discard();shield.discard();reflectorOwner.discard();boss.discard();helper.succeed();
    }
    private static java.util.List<net.minecraft.world.entity.Entity> emissaryHazards(GameTestHelper helper,NativeBoss boss){
        return helper.getLevel().getEntities(boss,boss.getBoundingBox().inflate(80),entity->{
            if(!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals("emissary_hazard"))return false;
            var saved=new CompoundTag();entity.saveWithoutId(saved);return saved.hasUUID("Owner")&&saved.getUUID("Owner").equals(boss.getUUID());
        });
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_meteor_emitter_outlives_animation(GameTestHelper helper){
        var boss=emissaryFixture(helper,1);var target=emissaryPlayer(helper,boss.position().add(0,0,12));
        boss.startEmissaryAttackForTest(target,"phase1.attack.meteor_shower");for(int tick=0;tick<51;tick++)boss.customServerAiStep();
        var hazards=emissaryHazards(helper,boss);helper.assertTrue(hazards.size()==1,"Meteor Shower lost its independent 160-tick emitter after the 51-tick animation");
        var emitter=hazards.get(0);for(int tick=0;tick<150;tick++)emitter.tick();
        var bolts=helper.getLevel().getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(80),bolt->bolt.getOwner()==boss);
        helper.assertTrue(bolts.size()==10,"Meteor Shower must fire ten meteors at 15-tick intervals, got "+bolts.size());
        for(int tick=0;tick<10;tick++)emitter.tick();helper.assertTrue(emitter.isRemoved(),"Meteor emitter did not expire at 160 ticks");
        bolts.forEach(net.minecraft.world.entity.Entity::discard);target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_lightning_field_warns_before_striking(GameTestHelper helper){
        var boss=emissaryFixture(helper,3);var target=emissaryPlayer(helper,boss.position().add(0,0,15));
        boss.startEmissaryAttackForTest(target,"phase3.attack.lightning_field");for(int tick=0;tick<33;tick++)boss.customServerAiStep();
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(80),bolt->bolt.getOwner()==boss).isEmpty(),"Lightning Field still rains damaging bolts before activation frame 34");
        boss.customServerAiStep();var points=emissaryHazards(helper,boss);helper.assertTrue(points.size()==32,"Lightning Field must create four spokes of eight warning points");
        var point=points.get(0);target.setPos(point.position());float health=target.getHealth();
        for(int tick=0;tick<20;tick++)point.tick();helper.assertTrue(target.getHealth()==health&&!point.isRemoved(),"Lightning point struck during its 20-tick warning");
        point.tick();helper.assertTrue(target.getHealth()<health&&point.isRemoved(),"Lightning point did not strike on tick 21");
        points.forEach(net.minecraft.world.entity.Entity::discard);target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_divine_blast_keeps_firing_and_reload_clock(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var target=emissaryPlayer(helper,boss.position().add(0,0,12));
        boss.startEmissaryAttackForTest(target,"phase4.attack.divine_blast");var hazards=emissaryHazards(helper,boss);
        helper.assertTrue(hazards.size()==1,"Divine Blast has no persistent, counterable charge entity at frame zero");
        var beam=hazards.get(0);for(int tick=0;tick<23;tick++)beam.tick();helper.assertTrue(target.getHealth()==200,"Divine Blast fired during charge");
        beam.tick();helper.assertTrue(target.getHealth()<200,"Divine Blast did not start on tick 24");
        target.invulnerableTime=0;target.hurtTime=0;float health=target.getHealth();
        var saved=new CompoundTag();beam.saveWithoutId(saved);beam.load(saved);beam.tick();
        helper.assertTrue(target.getHealth()<health,"Divine Blast became a single hit or lost its active clock after reload");
        for(int tick=0;tick<24;tick++)beam.tick();helper.assertTrue(beam.isRemoved(),"Divine Blast did not stop after 26 active ticks");
        target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_godrays_are_rotating_beams_not_rain(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var target=emissaryPlayer(helper,boss.position().add(0,0,12));
        boss.startEmissaryAttackForTest(target,"phase4.attack.godrays");for(int tick=0;tick<15;tick++)boss.customServerAiStep();
        var hazards=emissaryHazards(helper,boss);helper.assertTrue(hazards.size()==1,"Godrays lacks its four-beam rotating origin at frame 15");
        var rays=hazards.get(0);float yaw=rays.getYRot();for(int tick=0;tick<14;tick++)rays.tick();
        helper.assertTrue(rays.getYRot()==yaw,"Godrays rotated before its 14-tick charge ended");
        var saved=new CompoundTag();rays.saveWithoutId(saved);rays.load(saved);rays.tick();
        helper.assertTrue(Math.abs(rays.getYRot()-yaw-3)<.001,"Godrays must sweep three one-degree substeps per active tick");
        for(int tick=0;tick<15;tick++)rays.tick();helper.assertTrue(rays.isRemoved(),"Godrays persisted beyond origin tick 30");
        target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary",timeoutTicks=40)
    public static void emissary_apocalypse_rain_fires_four_shards_on_each_source_frame(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var target=emissaryPlayer(helper,boss.position().add(0,0,15));
        boss.setYRot(0);boss.startEmissaryAttackForTest(target,"phase4.attack.apocalypse_rain");
        helper.assertTrue(emissaryRainShards(helper,boss).isEmpty(),"Apocalypse Rain fired before animation frame 10");
        for(int frame=1;frame<10;frame++)boss.customServerAiStep();
        helper.assertTrue(emissaryRainShards(helper,boss).isEmpty(),"Apocalypse Rain emitted during charge frames 1..9");
        boss.customServerAiStep();helper.assertTrue(emissaryRainShards(helper,boss).size()==4,"Animation frame 10 must create four grouped shards");
        target.discard();
        for(int frame=11;frame<=44;frame++)boss.customServerAiStep();
        var shards=emissaryRainShards(helper,boss);
        long upper=shards.stream().filter(EmissaryRainShard::upper).count();
        helper.assertTrue(shards.size()==140&&upper==70,"Frames 10..44 must emit 35 four-shard volleys with two upper shards each; got "+shards.size()+" / upper="+upper);
        helper.assertTrue(shards.stream().allMatch(shard->shard.ownerId().equals(boss.getUUID())&&shard.ageForTest()==0),"New shards lost attack attribution or started with a consumed tick");
        long upperPositive=0,upperNegative=0,lowerPositive=0,lowerNegative=0;boolean anglesValid=true,originsValid=true;
        for(var shard:shards){
            boolean positive=shard.getYRot()>0;originsValid&=shard.position().distanceToSqr(boss.position())<.00001;
            if(shard.upper()){anglesValid&=shard.getXRot()>=-78&&shard.getXRot()<=-63;if(positive)upperPositive++;else upperNegative++;}
            else{anglesValid&=shard.getXRot()>=52&&shard.getXRot()<=67;if(positive)lowerPositive++;else lowerNegative++;}
        }
        helper.assertTrue(anglesValid&&originsValid&&upperPositive==35&&upperNegative==35&&lowerPositive==35&&lowerNegative==35,
            "Four source yaw/pitch branches must spawn at boss origin with independent [-8,+7] degree offsets");
        shards.forEach(net.minecraft.world.entity.Entity::discard);target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary",timeoutTicks=40)
    public static void emissary_apocalypse_rain_preserves_sequential_pitch_and_two_substeps(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var target=emissaryPlayer(helper,boss.position().add(0,0,20));
        var shard=Content.EMISSARY_RAIN_SHARD.get().create(helper.getLevel());shard.setPos(boss.position().add(0,12,0));shard.setYRot(0);shard.setXRot(60);shard.upperForTest(false);shard.setOwnerForTest(boss);helper.getLevel().addFreshEntity(shard);
        Vec3 start=shard.position();shard.tick();Vec3 expected=Vec3.directionFromRotation(63,0).scale(2);
        helper.assertTrue(shard.ageForTest()==1&&shard.substepsForTest()==2&&shard.getXRot()==63&&shard.position().distanceTo(start.add(expected))<.001,
            "Lower shard must add three degrees then move two one-block substeps; age="+shard.ageForTest()+" steps="+shard.substepsForTest()+" pitch="+shard.getXRot()+" start="+start+" actual="+shard.position()+" expected="+start.add(expected));
        var upper=Content.EMISSARY_RAIN_SHARD.get().create(helper.getLevel());upper.setPos(boss.position().add(10,12,0));upper.setYRot(90);upper.setXRot(10);upper.upperForTest(true);helper.getLevel().addFreshEntity(upper);
        upper.tick();helper.assertTrue(Math.abs(upper.getXRot()-23.3f)<.001,"Upper pitch selector changes must run sequentially and accumulate within one tick: "+upper.getXRot());
        var saved=new CompoundTag();shard.saveWithoutId(saved);shard.discard();
        var restored=Content.EMISSARY_RAIN_SHARD.get().create(helper.getLevel());restored.load(saved);helper.getLevel().addFreshEntity(restored);
        helper.assertTrue(restored.ageForTest()==1&&restored.substepsForTest()==2&&boss.getUUID().equals(restored.ownerId())&&restored.getXRot()==63,
            "Shard reload lost owner, age, substep progress, or rotation");
        Vec3 beforeExpiry=restored.position();restored.ageForTest(59);restored.tick();
        helper.assertTrue(restored.isRemoved()&&restored.position().distanceToSqr(beforeExpiry)==0,"Tick 60 must resolve without another movement substep");
        upper.discard();target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary",timeoutTicks=40)
    public static void emissary_apocalypse_rain_stops_on_blocks_and_uses_five_block_impact(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);Vec3 origin=boss.position().add(0,12,0);
        var inside=emissaryPlayer(helper,origin.add(0,0,4));var outside=emissaryPlayer(helper,origin.add(0,0,6));
        var shard=Content.EMISSARY_RAIN_SHARD.get().create(helper.getLevel());shard.setPos(origin);shard.setYRot(0);shard.setXRot(90);shard.upperForTest(false);shard.setOwnerForTest(boss);helper.getLevel().addFreshEntity(shard);
        BlockPos wall=BlockPos.containing(origin.add(0,-1,0));helper.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);
        float nearHealth=inside.getHealth(),farHealth=outside.getHealth();shard.tick();
        helper.assertTrue(shard.isRemoved()&&shard.substepsForTest()==1,"A non-passthrough block must end the projectile during its first one-block step");
        float applied=nearHealth-inside.getHealth();
        helper.assertTrue(applied>0&&applied<=EmissaryRainShard.SOURCE_DAMAGE&&outside.getHealth()==farHealth,
            "Impact must apply up to the 12-point Dissonance base through vanilla armor and exclude players outside five blocks; near="+nearHealth+"->"+inside.getHealth()+" far="+farHealth+"->"+outside.getHealth()+" impact="+shard.position()+" inside="+inside.position()+" outside="+outside.position());
        helper.getLevel().setBlock(wall,Blocks.AIR.defaultBlockState(),3);inside.discard();outside.discard();
        var contact=emissaryPlayer(helper,origin.add(0,-1,1));float contactHealth=contact.getHealth();
        var contactShard=Content.EMISSARY_RAIN_SHARD.get().create(helper.getLevel());contactShard.setPos(origin);contactShard.setYRot(0);contactShard.setXRot(0);contactShard.upperForTest(false);contactShard.setOwnerForTest(boss);helper.getLevel().addFreshEntity(contactShard);
        contactShard.tick();helper.assertTrue(contactShard.isRemoved()&&contactShard.substepsForTest()==1&&contact.getHealth()<contactHealth,
            "Player contact in the traversed block must end the shard and resolve its impact");
        contact.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary",timeoutTicks=40)
    public static void emissary_apocalypse_rain_reflects_and_spends_reflectance(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var player=emissaryPlayer(helper,boss.position().add(0,0,15));
        Vec3 origin=boss.position().add(0,12,0);var shield=SpellArea.create(helper.getLevel(),player,origin.add(0,0,1),Spell.REFLECTANCE);shield.setYRot(180);shield.setXRot(0);
        var shard=Content.EMISSARY_RAIN_SHARD.get().create(helper.getLevel());shard.setPos(origin);shard.setYRot(0);shard.setXRot(90);shard.upperForTest(false);shard.setOwnerForTest(boss);helper.getLevel().addFreshEntity(shard);
        shard.tick();
        CompoundTag savedShield=new CompoundTag();shield.saveWithoutId(savedShield);
        helper.assertTrue(!shield.isRemoved()&&Math.abs(savedShield.getFloat("ReflectanceEnergy")-8)<.001
            &&shard.substepsForTest()==2&&shard.getZ()<origin.z-1.9,
            "The first source slowcast should spend twelve capacity, transfer ownership, then let the same-owner second substep pass");
        helper.assertTrue(player.getUUID().equals(shard.damageOwnerForTest()),"Reflected rain shard kept the emissary as its damage owner");
        shard.discard();player.discard();boss.discard();helper.succeed();
    }
    private static java.util.List<EmissaryRainShard> emissaryRainShards(GameTestHelper helper,NativeBoss boss){
        AABB bounds=new AABB(boss.position().subtract(180,180,180),boss.position().add(180,180,180));
        return helper.getLevel().getEntitiesOfClass(EmissaryRainShard.class,bounds,shard->boss.getUUID().equals(shard.ownerId()));
    }
    private static NativeBoss emissaryFixture(GameTestHelper helper,int phase){
        var boss=Content.EMISSARY.get().create(helper.getLevel());CompoundTag tag=new CompoundTag();boss.saveWithoutId(tag);
        tag.putInt("NativePhase",phase);boss.load(tag);boss.setHealth(phase==1?1200:phase==2?800:phase==3?400:150);
        Vec3 origin=helper.absolutePos(new BlockPos(3,3,3)).getCenter();boss.setPos(origin.x,helper.getLevel().getMaxBuildHeight()-96,origin.z);
        boss.addTag("nyctocosm_emissary_test");boss.setNoAi(true);boss.setNoGravity(true);helper.getLevel().addFreshEntity(boss);return boss;
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_hazards_respect_forbiddance_and_saved_godray_nodes(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var player=emissaryPlayer(helper,boss.position().add(0,0,15));
        Vec3 at=boss.position().add(0,6,0);
        var beam=EmissaryHazard.create(helper.getLevel(),boss,player,EmissaryHazard.Kind.DIVINE_BLAST,at);
        var emitter=EmissaryHazard.create(helper.getLevel(),boss,player,EmissaryHazard.Kind.METEOR_SHOWER,at);
        var counter=SpellBolt.launch(helper.getLevel(),player,at,Vec3.ZERO,0,Combat.Element.RESONANCE,0,0).effect(SpellBolt.Effect.COUNTER,20);
        counter.tick();counter.tick();helper.assertTrue(!beam.isRemoved(),"Forbiddance cancelled a charge before its third tick");counter.tick();
        helper.assertTrue(beam.isRemoved()&&!emitter.isRemoved(),"Forbiddance failed to cancel Divine Blast or incorrectly cancelled the non-spell meteor emitter");
        var rays=EmissaryHazard.create(helper.getLevel(),boss,player,EmissaryHazard.Kind.GODRAYS,at);rays.setYRot(0);
        Vec3 counterAt=at.add(Vec3.directionFromRotation(0,45).scale(8));
        var second=SpellBolt.launch(helper.getLevel(),player,counterAt,Vec3.ZERO,0,Combat.Element.RESONANCE,0,0).effect(SpellBolt.Effect.COUNTER,20);
        for(int tick=0;tick<3;tick++)second.tick();var saved=new CompoundTag();rays.saveWithoutId(saved);rays.load(saved);
        helper.assertTrue(saved.getInt("GodrayMask")==14&&!rays.isRemoved(),"Forbiddance must remove only the nearby ray, not all four rays or their non-spell origin");
        rays.saveWithoutId(saved);helper.assertTrue(saved.getInt("GodrayMask")==14,"Reload resurrected a countered Godray");
        rays.discard();emitter.discard();counter.discard();second.discard();player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_beam_reflects_and_consumes_reflectance(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var player=emissaryPlayer(helper,boss.position().add(0,0,12));
        boss.startEmissaryAttackForTest(player,"phase4.attack.divine_blast");var beam=emissaryHazards(helper,boss).get(0);
        var shield=SpellArea.create(helper.getLevel(),player,beam.position().add(beam.getLookAngle().scale(6)),Spell.REFLECTANCE);
        shield.setYRot(beam.getYRot()+180);shield.setXRot(-beam.getXRot());float health=boss.getHealth();
        for(int tick=0;tick<24;tick++)beam.tick();
        helper.assertTrue(shield.isRemoved(),"Reflected beam did not consume Reflectance's 20-point capacity");
        helper.assertTrue(player.getHealth()==200&&boss.getHealth()==health&&((EmissaryHazard)beam).lastBeamEndForTest().subtract(beam.position()).dot(beam.getLookAngle())<0,"Reflected source beam did not reverse its path or damaged a non-player caster despite the source player-only selector");
        beam.discard();player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_meteor_reload_keeps_next_shot_and_clears_dead_owner(GameTestHelper helper){
        var boss=emissaryFixture(helper,1);var player=emissaryPlayer(helper,boss.position().add(0,0,12));boss.setTarget(player);
        var emitter=EmissaryHazard.create(helper.getLevel(),boss,player,EmissaryHazard.Kind.METEOR_SHOWER,boss.position().add(0,8,0));
        for(int tick=0;tick<14;tick++)emitter.tick();var saved=new CompoundTag();emitter.saveWithoutId(saved);emitter.discard();
        var restored=Content.EMISSARY_HAZARD.get().create(helper.getLevel());restored.load(saved);helper.getLevel().addFreshEntity(restored);restored.tick();
        var bolts=helper.getLevel().getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(80),bolt->bolt.getOwner()==boss);
        helper.assertTrue(bolts.size()==1&&bolts.get(0).element()==Combat.Element.DISSONANCE,"Reload lost the meteor's fifteenth-tick emission or damage type");
        var boltSaved=new CompoundTag();bolts.get(0).saveWithoutId(boltSaved);bolts.get(0).load(boltSaved);
        helper.assertTrue(Math.abs(boltSaved.getFloat("HomingTurn")-2f/29)<.0001,"Meteor lost its slow homing turn rate");
        boss.discard();for(int tick=0;tick<21;tick++)restored.tick();helper.assertTrue(restored.isRemoved(),"Unloaded/dead owner left an immortal meteor emitter");
        bolts.forEach(net.minecraft.world.entity.Entity::discard);player.discard();helper.succeed();
    }
    private static Vec3 celestialTestOrigin(GameTestHelper helper,BlockPos local){
        Vec3 near=helper.absolutePos(local).getCenter();
        double x=Math.floor((near.x+5000)/16.0)*16+.5,z=Math.floor((near.z+5000)/16.0)*16+.5;
        Vec3 origin=new Vec3(x,120,z);
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        int chunkX=BlockPos.containing(origin).getX()>>4,chunkZ=BlockPos.containing(origin).getZ()>>4;
        for(int dx=0;dx<=1;dx++)for(int dz=0;dz<=1;dz++)
            level.getChunkSource().getChunk(chunkX+dx,chunkZ+dz,net.minecraft.world.level.chunk.ChunkStatus.FULL,true);
        return origin;
    }
    private static void emissaryCelestialFrame(GameTestHelper helper,NativeBoss boss,net.minecraft.world.entity.LivingEntity target,int frame){
        try{
            var age=NativeBoss.class.getDeclaredField("attackAge");age.setAccessible(true);age.setInt(boss,frame);
            var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);
            perform.setAccessible(true);perform.invoke(boss,(net.minecraft.server.level.ServerLevel)helper.getLevel(),target);
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not invoke Celestial Smite source frame "+frame,failure);}
    }
    private static net.minecraft.world.entity.player.Player emissaryPlayer(GameTestHelper helper,Vec3 at){
        // Forge FakePlayer is permanently damage-immune: use a real survival Player test double.
        var player=helper.makeMockSurvivalPlayer();player.addTag("nyctocosm_emissary_test");player.setPos(at);player.setNoGravity(true);helper.getLevel().addFreshEntity(player);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);player.setHealth(200);return player;
    }
    private static net.minecraft.server.level.ServerPlayer encounterTestPlayer(GameTestHelper helper,Vec3 at,net.minecraft.world.level.GameType mode){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        String name="Encounter"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,12);
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),name);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,profile);
        player.addTag("nyctocosm_encounter_test");player.setGameMode(mode);player.setNoGravity(true);player.moveTo(at.x,at.y,at.z,0,0);level.addNewPlayer(player);return player;
    }
    private static void keepBossLoadedForTest(GameTestHelper helper,net.minecraft.world.entity.Entity boss){
        encounterTestPlayer(helper,boss.position(),net.minecraft.world.level.GameType.SPECTATOR);
    }
    private static void clearEncounterTestPlayers(net.minecraft.server.level.ServerLevel level){
        for(var player:new java.util.ArrayList<>(level.players()))if(player.getTags().contains("nyctocosm_encounter_test"))player.discard();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_melee_has_delayed_parry_window_and_break(GameTestHelper helper){
        var boss=emissaryFixture(helper,3);boss.setYRot(0);boss.setXRot(0);
        var player=emissaryPlayer(helper,boss.position().add(0,0,2.5));float health=player.getHealth();
        boss.startEmissaryAttackForTest(player,"phase3.attack.swordswing");boss.customServerAiStep();
        var regions=helper.getLevel().getEntitiesOfClass(EmissaryHitRegion.class,boss.getBoundingBox().inflate(10),r->r.ownedBy(boss));
        helper.assertTrue(regions.size()==1,"Sword Swing did not create its frame-one parry region");
        var region=regions.get(0);for(int tick=0;tick<4;tick++)region.tick();
        helper.assertTrue(player.getHealth()==health,"Sword Swing damaged before the five-tick parry window ended");
        var saved=new CompoundTag();region.saveWithoutId(saved);region.discard();region=Content.EMISSARY_HIT_REGION.get().create(helper.getLevel());region.load(saved);helper.getLevel().addFreshEntity(region);region.tick();
        helper.assertTrue(player.getHealth()<health,"Saved hit region lost its fifth-tick damage: health="+player.getHealth()+", box="+region.getBoundingBox()+", player="+player.getBoundingBox()+", removed="+region.isRemoved()+", invulnerable="+player.isInvulnerableTo(Combat.source(helper.getLevel(),boss,Combat.Element.DISSONANCE))+", saved="+saved);
        for(int parry=1;parry<=7;parry++){
            boss.startEmissaryAttackForTest(player,"phase3.attack.swordswing");boss.customServerAiStep();
            var hit=helper.getLevel().getEntitiesOfClass(EmissaryHitRegion.class,boss.getBoundingBox().inflate(10),r->r.ownedBy(boss)).get(0);
            helper.assertTrue(hit.hurt(helper.getLevel().damageSources().playerAttack(player),1),"Attack region could not be parried");
            helper.assertTrue(hit.isRemoved(),"Parried region could still deliver its delayed damage");
            helper.assertTrue(boss.animation().equals(parry==7?"phase3.parry.break":"phase3.parry"),"Seven-parry poise threshold differs from source");
        }
        boss.discard();player.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_grapple_mounts_only_on_contact_and_releases(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var player=emissaryPlayer(helper,boss.position().add(0,0,2));
        CompoundTag state=new CompoundTag();boss.saveWithoutId(state);state.putBoolean("EmissaryFlying",true);boss.load(state);boss.setYRot(0);boss.setXRot(0);
        boss.startEmissaryAttackForTest(player,"phase4.attack.grapple");
        for(int tick=0;tick<14;tick++){boss.setPos(player.position().add(0,0,-1));boss.customServerAiStep();}
        helper.assertTrue(!player.isPassenger(),"Grapple grabbed before its frame-fifteen activation");
        boss.setPos(player.position().add(0,0,-1));boss.customServerAiStep();
        helper.assertTrue(player.getVehicle() instanceof EmissaryGrappleSeat&&boss.animation().equals("phase4.attack.grapple_grab"),"Contact did not enter the separate grabbed animation and native vehicle");
        var seat=(EmissaryGrappleSeat)player.getVehicle();helper.assertTrue(player.position().distanceTo(boss.emissaryGrapplePosition())<.001,"Passenger did not follow the forward/down grip anchor");
        float health=player.getHealth();var saved=new CompoundTag();boss.saveWithoutId(saved);boss.load(saved);boss.setTarget(player);
        helper.assertTrue(boss.holdsEmissaryGrapple(seat.getUUID()),"Reload lost ownership of a live grapple seat");
        Vec3 flightOrigin=boss.position();
        // Release timing is independent of flight routing; stay inside this fixture's ticking chunks.
        for(int tick=0;tick<159;tick++){boss.setPos(flightOrigin);boss.customServerAiStep();seat.tick();}
        helper.assertTrue(player.isPassenger()&&player.getHealth()==health,"Airborne grapple released early or dealt an invented timed hit: passenger="+player.isPassenger()+", health="+player.getHealth()+"/"+health+", clip="+boss.animation()+", seatRemoved="+seat.isRemoved()+", boss="+boss.position()+", seat="+seat.position());
        boss.customServerAiStep();helper.assertTrue(!player.isPassenger()&&seat.isRemoved(),"Grapple did not release passengers on grabbed frame 160");
        boss.discard();player.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_grapple_orphan_cannot_strand_player(GameTestHelper helper){
        var boss=emissaryFixture(helper,4);var player=emissaryPlayer(helper,boss.position());var seat=EmissaryGrappleSeat.create(helper.getLevel(),boss);player.startRiding(seat,true);
        boss.discard();for(int tick=0;tick<21;tick++)seat.tick();
        helper.assertTrue(seat.isRemoved()&&!player.isPassenger(),"Removed grapple owner left a player trapped on an invisible vehicle");player.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_flight_uses_source_turning_and_takeoff_frame(GameTestHelper helper){
        Vec3 forward=new Vec3(0,0,1),toward=new Vec3(1,0,0);
        Vec3 ordinary=NativeBoss.emissaryFlightHeading(forward,toward,false),quick=NativeBoss.emissaryFlightHeading(forward,toward,true);
        helper.assertTrue(ordinary.distanceTo(new Vec3(.5,0,25).normalize())<1e-9&&quick.x>ordinary.x,"Flight no longer follows the source 25/10-block turning construction");
        var boss=emissaryFixture(helper,4);var target=EntityType.COW.create(helper.getLevel());target.setPos(boss.position().add(0,0,20));boss.setYRot(0);boss.setXRot(0);
        boss.startEmissaryAttackForTest(target,"phase4_idle_transition_flying");Vec3 before=boss.position();
        for(int tick=0;tick<13;tick++)boss.customServerAiStep();
        helper.assertTrue(boss.position().distanceTo(before.add(0,5*.80625,0))<1e-8,"Takeoff must apply source root lift1..5 before flying14");
        Vec3 lifted=boss.position();boss.customServerAiStep();
        helper.assertTrue(boss.position().distanceTo(lifted)>1.9,"Takeoff did not start the source's flight movement on frame fourteen");
        var saved=new CompoundTag();boss.saveWithoutId(saved);helper.assertTrue(saved.getBoolean("EmissaryFlying"),"Takeoff flight state was not saved");
        boss.startEmissaryAttackForTest(target,"phase4_flying_transition_idle");for(int tick=0;tick<4;tick++)boss.customServerAiStep();
        boss.saveWithoutId(saved);helper.assertTrue(!saved.getBoolean("EmissaryFlying"),"Landing did not stop flight on source frame four");boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_lethal_damage_waits_for_phases_but_absorption_does_not(GameTestHelper helper){
        var boss=emissaryFixture(helper,1);boss.setAbsorptionAmount(5000);
        boss.hurt(helper.getLevel().damageSources().generic(),2000);var saved=new CompoundTag();boss.saveWithoutId(saved);
        helper.assertTrue(!saved.getBoolean("EmissaryPendingDeath")&&boss.isAlive(),"Absorbed damage incorrectly queued Emissary death");
        boss.setAbsorptionAmount(0);boss.invulnerableTime=0;boss.hurt(helper.getLevel().damageSources().generic(),2000);
        helper.assertTrue(boss.isAlive()&&boss.getHealth()==1,"Lethal phase-one damage skipped all later phases");
        for(int tick=0;tick<370&&boss.isAlive();tick++)boss.customServerAiStep();
        helper.assertTrue(boss.phase()==4&&!boss.isAlive(),"Deferred lethal damage did not resolve after all three transitions");boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_cannot_skip_transitions_and_is_protected(GameTestHelper helper){
        var boss=Content.EMISSARY.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(3,3,3)).getCenter());
        boss.setHealth(150);boss.customServerAiStep();
        helper.assertTrue(boss.animation().equals("phase1_transition")&&boss.phase()==1,"Low health skipped Emissary's first transition");
        float health=boss.getHealth();boss.hurt(helper.getLevel().damageSources().generic(),25);
        helper.assertTrue(boss.getHealth()==health,"Emissary took damage during its phase transition");
        CompoundTag saved=new CompoundTag();boss.saveWithoutId(saved);
        var restored=Content.EMISSARY.get().create(helper.getLevel());restored.load(saved);
        helper.assertTrue(restored.animation().equals("phase1_transition"),"Reload discarded Emissary's transition protection");
        for(int tick=0;tick<81;tick++)restored.customServerAiStep();
        helper.assertTrue(restored.phase()==2,"First transition did not enter phase two");
        restored.customServerAiStep();helper.assertTrue(restored.animation().equals("phase2_transition"),"Second transition was skipped");helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_grapple_miss_cannot_damage_distant_target(GameTestHelper helper){
        var boss=Content.EMISSARY.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(2,3,2)).getCenter());
        var target=EntityType.COW.create(helper.getLevel());target.setPos(boss.position().add(150,0,0));
        boss.startEmissaryAttackForTest(target,"phase4.attack.grapple");float health=target.getHealth();
        for(int tick=0;tick<60;tick++)boss.customServerAiStep();
        helper.assertTrue(target.getHealth()==health,"A missed grapple dealt unavoidable damage to a distant target");helper.succeed();
    }
    @GameTest(template="empty",batch="emissary")
    public static void emissary_attack_resumes_after_reload(GameTestHelper helper){
        var boss=Content.EMISSARY.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(3,3,3)).getCenter());
        var target=EntityType.COW.create(helper.getLevel());target.setPos(boss.position().add(20,0,0));
        boss.startEmissaryAttackForTest(target,"phase4.attack.fireball");
        for(int tick=0;tick<7;tick++)boss.customServerAiStep();
        CompoundTag saved=new CompoundTag();boss.saveWithoutId(saved);
        var restored=Content.EMISSARY.get().create(helper.getLevel());restored.load(saved);restored.setTarget(target);
        helper.assertTrue(restored.animation().equals("phase4.attack.fireball"),"Reload cancelled a telegraphed Emissary attack");
        restored.customServerAiStep();
        var bolts=helper.getLevel().getEntitiesOfClass(SpellBolt.class,restored.getBoundingBox().inflate(10),bolt->bolt.getOwner()==restored);
        helper.assertTrue(bolts.size()==1,"First fireball must fire once on source frame eight after reload");bolts.forEach(SpellBolt::discard);helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_twirl_climbs_independently_of_target(GameTestHelper helper){
        NativeBoss boss=Content.SERPENT.get().create(helper.getLevel());
        Vec3 origin=helper.absolutePos(new BlockPos(4,20,4)).getCenter();boss.moveTo(origin.x,origin.y,origin.z,0,0);
        var target=EntityType.COW.create(helper.getLevel());target.setPos(origin.add(50,-15,50));
        boss.setTarget(target);boss.startSerpentAttackForTest(helper.getLevel(),target,1);
        for(int tick=0;tick<5;tick++){target.setPos(target.position().add(7,-1,-5));boss.customServerAiStep();}
        var saved=new CompoundTag();boss.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getDouble("SerpentGoalY")>=origin.y+15,
            "Twirl target must climb from the head's starting position, independently of the player; y="+saved.getDouble("SerpentGoalY"));
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_laser_survives_reload(GameTestHelper helper){
        var target=EntityType.COW.create(helper.getLevel());target.setPos(helper.absolutePos(new BlockPos(4,20,4)).getCenter());
        NativeBoss boss=Content.SERPENT.get().create(helper.getLevel());boss.setPos(target.position().add(0,10,-20));
        boss.setTarget(target);boss.startSerpentAttackForTest(helper.getLevel(),target,2);
        for(int tick=0;tick<65;tick++)boss.customServerAiStep();
        var saved=new CompoundTag();boss.saveWithoutId(saved);
        NativeBoss restored=Content.SERPENT.get().create(helper.getLevel());restored.load(saved);restored.setTarget(target);
        restored.customServerAiStep();
        helper.assertTrue(Double.isFinite(restored.getX()),"Reloaded laser produced an invalid position");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=350)
    public static void sorcerer_entrance_continues_into_rise(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());
        helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);boss.beginEntrance();
        helper.runAfterDelay(335,()->{helper.assertTrue(boss.animation().equals("rise"),"Initial spawn skipped the source rise clip and its plane summon keyframe");helper.succeed();});
    }
    @GameTest(template="empty")
    public static void sorcerer_uses_source_command_keyframes(GameTestHelper helper){
        helper.assertTrue(SorcererTimeline.attacks().size()==12,"Sorcerer attack timeline is incomplete");
        helper.assertTrue(SorcererTimeline.events("halobeam").stream().filter(e->e.action()==SorcererTimeline.Action.HALO_SUMMON).map(SorcererTimeline.Event::tick).toList().equals(java.util.List.of(5,10,15,20,25,30,35,40)),"Halo summon sequence differs from the source rig");
        helper.assertTrue(SorcererTimeline.events("halobeam").stream().filter(e->e.action()==SorcererTimeline.Action.HALO_FIRE).map(SorcererTimeline.Event::tick).toList().equals(java.util.List.of(75,80,85,90,95,100,105,110)),"Halo firing sequence differs from the source marker timers");
        helper.assertTrue(SorcererTimeline.events("voidbeam").stream().filter(e->e.action()==SorcererTimeline.Action.VOIDBEAM).count()==28,"Void beam is not active on source ticks 30 through 57");
        helper.assertTrue(SorcererTimeline.at("voidwave",38).stream().anyMatch(e->e.action()==SorcererTimeline.Action.VOIDWAVE),"Void wave discharge moved away from source tick 38");
        helper.assertTrue(SorcererTimeline.at("airdash",40).stream().anyMatch(e->e.action()==SorcererTimeline.Action.AIR_SLAM),"Air dash slam moved away from source tick 40");
        helper.assertTrue(SorcererTimeline.at("icespikes",40).stream().anyMatch(e->e.action()==SorcererTimeline.Action.ICE_SPIKES),"Nullpoint ice wave moved away from source tick 40");
        for(String attack:SorcererTimeline.attacks())for(var event:SorcererTimeline.events(attack))if(event.sound()!=null)
            helper.assertTrue(Content.SOUND_MAP.containsKey(event.sound()),"Unregistered Sorcerer sound cue: "+event.sound());
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void sorcerer_six_phase_attack_tables_match_source(GameTestHelper helper){
        helper.assertTrue(NativeBoss.selectSorcererAttack(1,10,0).animation().equals("shardhail")&&NativeBoss.selectSorcererAttack(1,20,0).animation().equals("frozenblades"),"Phase one distance split differs");
        helper.assertTrue(NativeBoss.selectSorcererAttack(2,5,0).animation().equals("javelinswing")&&NativeBoss.selectSorcererAttack(2,10,0).animation().equals("dashstrike"),"Phase two distance split differs");
        java.util.Set<String> p3=new java.util.HashSet<>(),p5=new java.util.HashSet<>(),p6=new java.util.HashSet<>();
        for(int roll=0;roll<35;roll++){p3.add(NativeBoss.selectSorcererAttack(3,10,roll).animation());p5.add(NativeBoss.selectSorcererAttack(5,10,roll).animation());p6.add(NativeBoss.selectSorcererAttack(6,20,roll).animation());}
        helper.assertTrue(p3.equals(java.util.Set.of("shardhail","icespikes")),"Phase three lost the Nullpoint attack");
        helper.assertTrue(p5.equals(java.util.Set.of("shardhail","icespikes","voidmeteor")),"Phase five weighted pool differs");
        helper.assertTrue(p6.equals(java.util.Set.of("dashstrike","airdash","voidbeam")),"Phase six weighted pool differs");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void nullpoint_is_independent_300_health_objective(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        NullpointStaff staff=boss.spawnNullpointForTest(helper.getLevel());
        helper.assertTrue(staff!=null&&staff.getMaxHealth()==300&&staff.getHealth()==300,"Nullpoint did not retain its source health");
        staff.hurt(helper.getLevel().damageSources().generic(),75);
        helper.assertTrue(staff.getHealth()==225&&boss.getHealth()==1000,"Staff damage leaked into the Sorcerer");
        staff.hurt(helper.getLevel().damageSources().generic(),300);
        helper.runAfterDelay(3,()->{helper.assertTrue(boss.phase()==2&&boss.animation().equals("falltransition"),"Breaking Nullpoint did not enter staffless phase two");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void sorcerer_even_phase_threshold_restores_nullpoint(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",2);boss.load(tag);
        boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());boss.setHealth(666);helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        helper.runAfterDelay(3,()->{helper.assertTrue(boss.phase()==3&&boss.animation().equals("risetransition"),"666 health did not start phase three rise");helper.assertTrue(boss.nullpoint().isEmpty(),"Nullpoint appeared before source transition frame 51");});
        helper.runAfterDelay(57,()->{
            helper.assertTrue(boss.phase()==3&&boss.nullpoint().isPresent(),"Phase three did not restore Nullpoint at transition frame 51");
            NullpointStaff staff=boss.nullpoint().orElseThrow();helper.assertTrue(staff.getHealth()==300,"Restored Nullpoint has wrong health");
            float health=staff.getHealth();helper.assertTrue(!staff.hurt(helper.getLevel().damageSources().generic(),20)&&staff.getHealth()==health,"Transition invulnerability did not protect Nullpoint");helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=15)
    public static void sorcerer_odd_phase_does_not_skip_staff_cycle(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",3);boss.load(tag);
        boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());boss.setHealth(100);helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        helper.runAfterDelay(4,()->{helper.assertTrue(boss.phase()==3&&boss.nullpoint().isPresent(),"Low health skipped the required phase-three Nullpoint break");helper.succeed();});
    }
    @GameTest(template="empty",batch="ice_spikes_first_ring",timeoutTicks=1200)
    public static void ice_spikes_first_ring_hits_on_source_tick_seven(GameTestHelper helper){
        Vec3 isolated=isolatedTestOrigin(helper,120);
        inLoadedTestArea(helper,isolated,16,()->verifyIceSpikeFirstRing(helper,BlockPos.containing(isolated)));
    }
    private static void verifyIceSpikeFirstRing(GameTestHelper helper,BlockPos center){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        int floorY=level.getMaxBuildHeight()-16;
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)level.setBlock(new BlockPos(center.getX()+x,floorY,center.getZ()+z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        level.setBlock(new BlockPos(center.getX(),floorY+5,center.getZ()+1),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        level.setBlock(new BlockPos(center.getX(),floorY+6,center.getZ()+1),net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState(),3);
        Vec3 origin=Vec3.atCenterOf(center);
        var owner=EntityType.COW.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin);level.addFreshEntity(owner);
        var target=helper.makeMockSurvivalPlayer();target.setNoGravity(true);target.setPos(origin.x,floorY+6+1.99,origin.z+.9);level.addFreshEntity(target);
        float health=target.getHealth();IceSpikeWave wave=IceSpikeWave.spawn(level,owner,origin);
        Vec3 root=IceSpikeWave.point(origin,0,.9);
        helper.assertTrue(IceSpikeWave.withinImpactSphere(root.add(0,1.99,0),root)&&!IceSpikeWave.withinImpactSphere(root.add(0,2.01,0),root),
            "Ice Spike feet-distance sphere lost its inclusive 2-block vertical boundary");
        helper.assertTrue(IceSpikeWave.withinImpactSphere(root.add(1.99,0,0),root)&&!IceSpikeWave.withinImpactSphere(root.add(2.01,0,0),root),
            "Ice Spike feet-distance sphere lost its inclusive 2-block horizontal boundary");
        helper.runAfterDelay(7,()->{
            helper.assertTrue(wave.waveAge()==7&&wave.spikeSurfaceY(0,0)==floorY+6,"First spike anchor mismatch: age="+wave.waveAge()+", surface="+wave.spikeSurfaceY(0,0)+", expected="+(floorY+6));
            helper.assertTrue(target.getHealth()<health,"The source first ring (radius 0.9) did not hit its radius-2 feet sphere at wave tick 7");
            helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void ice_spikes_wavefront_uses_source_first_advance_and_five_tick_rings(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();BlockPos center=helper.absolutePos(new BlockPos(4,4,4));
        int floorY=90;for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)level.setBlock(new BlockPos(center.getX()+x,floorY,center.getZ()+z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var commandSource=level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
        var commands=level.getServer().getCommands();
        helper.assertTrue(commands.performPrefixedCommand(commandSource,"scoreboard objectives add d35v dummy")>0,"Could not create isolated missing-score command fixture");
        commands.performPrefixedCommand(commandSource,"scoreboard players remove D35Missing d35v 1");
        int firstUnsetScore=commands.performPrefixedCommand(commandSource,"scoreboard players get D35Missing d35v");
        helper.assertTrue(firstUnsetScore==-1,"Vanilla remove on an unset score did not create -1; returned "+firstUnsetScore);
        Vec3 origin=Vec3.atCenterOf(center);var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setYRot(0);owner.setPos(origin);level.addFreshEntity(owner);
        IceSpikeWave wave=IceSpikeWave.spawn(level,owner,origin);
        int[] ages={1,5,6,10,11,25};double[] markerRadii={.9,2.9,3.4,5.4,5.9,12.9};int[] ringCounts={1,1,2,2,3,5};double[] latestSpikeRadii={.9,.9,3.4,3.4,5.9,10.9};
        Vec3 quarterTurn=IceSpikeWave.point(origin,0,.9,90).subtract(origin);
        helper.assertTrue(Math.abs(quarterTurn.x+.9)<.001&&Math.abs(quarterTurn.z)<.001,"Wave arms did not follow the staff yaw");
        for(int sample=0;sample<ages.length;sample++){
            final int index=sample;
            helper.runAfterDelay(ages[index],()->{
                helper.assertTrue(wave.waveAge()==ages[index],"Wave clock expected source tick "+ages[index]+" but was "+wave.waveAge());
                double markerRadius=IceSpikeWave.point(origin,0,wave.currentMarkerRadius(),wave.baseYaw()).distanceTo(origin);
                helper.assertTrue(Math.abs(markerRadius-markerRadii[index])<.001,"Wave marker radius at tick "+ages[index]+" was "+markerRadius);
                int count=0;for(int ring=0;ring<5;ring++)if(IceSpikeWave.hasSurface(wave.spikeSurfaceY(ring,0)))count++;
                helper.assertTrue(count==ringCounts[index],"Wave tick "+ages[index]+" emitted "+count+" rings");
                if(index==0||index==2||index==4||index==5){
                    int latest=(index==0?0:index==2?1:index==4?2:4);
                    double spikeRadius=IceSpikeWave.point(origin,0,.4+IceSpikeWave.ringBirthAge(latest)*.5,wave.baseYaw()).distanceTo(origin);
                    helper.assertTrue(Math.abs(spikeRadius-latestSpikeRadii[index])<.001,"Spike ring radius at source tick "+ages[index]+" was "+spikeRadius);
                }
                if(index==ages.length-1)helper.succeed();
            });
        }
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void ice_spikes_age_and_surface_anchors_survive_reload_without_replaying_impacts(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();BlockPos center=helper.absolutePos(new BlockPos(4,4,4));
        int floorY=90;for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)level.setBlock(new BlockPos(center.getX()+x,floorY,center.getZ()+z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        Vec3 origin=Vec3.atCenterOf(center);var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin);level.addFreshEntity(owner);
        IceSpikeWave wave=IceSpikeWave.spawn(level,owner,origin);Vec3 lastRing=IceSpikeWave.point(origin,0,10.9);
        var target=helper.makeMockSurvivalPlayer();target.setNoGravity(true);target.setPos(lastRing.x,floorY+1,lastRing.z);level.addFreshEntity(target);
        helper.runAfterDelay(32,()->{
            helper.assertTrue(wave.waveAge()==32,"Save fixture did not reach post-impact tick 32");
            float health=target.getHealth();CompoundTag saved=new CompoundTag();wave.saveWithoutId(saved);
            int[] savedSurfaces=saved.getIntArray("SpikeSurfaceY");wave.discard();
            IceSpikeWave restored=Content.ICE_SPIKE_WAVE.get().create(level);restored.load(saved);level.addFreshEntity(restored);
            helper.assertTrue(restored.waveAge()==32&&savedSurfaces.length==150,"Reload lost the absolute wave age or per-spike anchors");
            for(int ring=0;ring<5;ring++)helper.assertTrue(IceSpikeWave.hasSurface(restored.spikeSurfaceY(ring,0)),"Reload lost surface anchor for ring "+ring);
            helper.runAfterDelay(1,()->{
                helper.assertTrue(restored.waveAge()==33&&target.getHealth()==health,"Reload replayed a completed inner-ring impact");
                int rings=0;for(int ring=0;ring<5;ring++)if(IceSpikeWave.hasSurface(restored.spikeSurfaceY(ring,0)))rings++;
                helper.assertTrue(rings==5,"Reload re-spawned or lost a source ring");
            });
            helper.runAfterDelay(13,()->helper.assertTrue(restored.waveAge()==45&&!restored.isRemoved(),"Final spike was removed before source age 24"));
            helper.runAfterDelay(14,()->{helper.assertTrue(restored.isRemoved(),"Ice Spike wave outlived the final source timer-25 cleanup");helper.succeed();});
        });
    }
    @GameTest(template="empty",batch="ice_spikes_nonplayers",timeoutTicks=1200)
    public static void ice_spikes_do_not_hit_nonplayers(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,16,()->verifyIceSpikesExcludeNonplayers(helper,origin));
    }
    private static void verifyIceSpikesExcludeNonplayers(GameTestHelper helper,Vec3 origin){
        var level=helper.getLevel();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin);level.addFreshEntity(owner);
        var target=Content.DEATH_WALKER.get().create(level);target.setNoAi(true);target.setNoGravity(true);target.setPos(origin.add(0,0,2));level.addFreshEntity(target);
        float health=target.getHealth();
        IceSpikeWave.spawn(helper.getLevel(),owner,owner.position());
        helper.runAfterDelay(20,()->{helper.assertTrue(target.getHealth()==health,"Ice Spikes source selector @a must not hit a non-player LivingEntity");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void serpent_beam_is_delayed_persistent_nightmare_damage(GameTestHelper helper){
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(2,2,2));owner.setNoAi(true);owner.setNoGravity(true);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(5,2,5));target.setNoAi(true);target.setNoGravity(true);
        var beam=SerpentBeamHazard.spawn(helper.getLevel(),owner,target.position(),61);
        helper.runAfterDelay(3,()->{helper.assertTrue(target.getHealth()<target.getMaxHealth(),"Serpent beam did not damage a registered mod entity");helper.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),"Serpent beam Darkness is missing");helper.assertTrue(!beam.isRemoved(),"Persistent beam vanished after its first hit");helper.succeed();});
    }
    @GameTest(template="empty")
    public static void serpent_keeps_source_body_and_three_attacks(GameTestHelper helper){
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);
        helper.assertTrue(serpent.getParts().length==20,"Serpent no longer has the source's 20 damage hitboxes");
        var target=helper.spawn(EntityType.COW,new BlockPos(6,2,6));serpent.startSerpentAttackForTest(helper.getLevel(),target,3);
        long beams=java.util.stream.StreamSupport.stream(helper.getLevel().getAllEntities().spliterator(),false).filter(SerpentBeamHazard.class::isInstance).map(SerpentBeamHazard.class::cast).filter(beam->beam.ownerId().filter(serpent.getUUID()::equals).isPresent()).count();
        helper.assertTrue(beams==20,"Serpent beam attack created "+beams+" columns instead of 20");
        helper.assertTrue(serpent.serpentAttack()==3&&serpent.serpentState()==1,"Serpent beam attack did not enter its twirl state");helper.succeed();
    }
    @GameTest(template="empty",batch="serpent_multipart",timeoutTicks=20)
    public static void serpent_body_parts_match_vanilla_size_eight_slime(GameTestHelper helper){
        var level=helper.getLevel();var source4=(net.minecraft.world.entity.monster.Slime)EntityType.SLIME.create(level);source4.setSize(4,false);
        var source8=(net.minecraft.world.entity.monster.Slime)EntityType.SLIME.create(level);source8.setSize(8,false);
        helper.assertTrue(Math.abs(source8.getBbWidth()-2*source4.getBbWidth())<.001f,"Vanilla Size:8 dimensions are not twice the Size:4 fixture");
        var emissary=Content.EMISSARY.get().create(level);emissary.setNoAi(true);resetEmissaryAnimation(emissary,1);emissary.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(emissary);emissary.tick();
        helper.assertTrue(Math.abs(emissary.getParts()[0].getBbWidth()-source4.getBbWidth())<.001f,"Emissary Size:4 proxy differs from vanilla slime: "+emissary.getParts()[0].getBbWidth()+" vs "+source4.getBbWidth());emissary.discard();
        var serpent=Content.SERPENT.get().create(level);serpent.setNoAi(true);
        serpent.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);serpent.tick();
        helper.assertTrue(Math.abs(serpent.getParts()[0].getBbWidth()-source8.getBbWidth())<.001
            &&Math.abs(serpent.getParts()[0].getBbHeight()-source8.getBbHeight())<.001,
            "Serpent parts should match the actual vanilla Size:8 slime dimensions; native="+serpent.getParts()[0].getBbWidth()+"x"+serpent.getParts()[0].getBbHeight()+", source="+source8.getBbWidth()+"x"+source8.getBbHeight());
        serpent.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="serpent_multipart",timeoutTicks=20)
    public static void serpent_body_parts_receive_area_and_line_damage_once(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();var serpent=Content.SERPENT.get().create(level);serpent.setNoAi(true);
        serpent.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);serpent.tick();
        var part=serpent.getParts()[19];part.setPos(serpent.position().add(32,0,0));var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setPos(serpent.getX()+80,serpent.getY(),serpent.getZ());level.addFreshEntity(owner);
        try{
            float before=serpent.getHealth();Combat.area(level,owner,part.position(),.1,7,Combat.Element.DREAM,0);
            helper.assertTrue(Math.abs((before-serpent.getHealth())-7)<.01,"Area hit on a distant Serpent body part did not damage the parent exactly once");
            serpent.invulnerableTime=0;serpent.hurtTime=0;serpent.hurtDuration=0;
            before=serpent.getHealth();Vec3 start=part.position().add(-5,0,0),end=part.position().add(5,0,0);
            Combat.line(level,owner,start,end,5,Combat.Element.RADIANCE,.1,false);
            helper.assertTrue(Math.abs((before-serpent.getHealth())-5)<.01,"Line hit on a Serpent body part did not damage its parent exactly once");
        }finally{serpent.discard();owner.discard();}helper.succeed();
    }
    @GameTest(template="empty",batch="serpent_multipart",timeoutTicks=20)
    public static void serpent_body_part_accepts_direct_spellbolt_damage(GameTestHelper helper){
        var level=helper.getLevel();var serpent=Content.SERPENT.get().create(level);serpent.setNoAi(true);
        serpent.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);serpent.tick();
        var part=serpent.getParts()[19];part.setPos(serpent.position().add(18,160,0));level.getChunkAt(part.blockPosition());var owner=EntityType.COW.create(level);
        owner.setNoAi(true);owner.setPos(serpent.getX()-8,serpent.getY(),serpent.getZ());level.addFreshEntity(owner);
        try{
            serpent.invulnerableTime=0;serpent.hurtTime=0;serpent.hurtDuration=0;
            float before=serpent.getHealth();var arrow=(net.minecraft.world.entity.projectile.Arrow)EntityType.ARROW.create(level);
            Vec3 arrowStart=part.position().add(-4,0,0);arrow.setOwner(owner);arrow.setNoGravity(true);arrow.setPos(arrowStart);arrow.setDeltaMovement(1.5,0,0);
            BlockPos arrowBlock=net.minecraft.core.BlockPos.containing(arrowStart);
            for(int step=0;step<=8;step++)for(int dy=-1;dy<=1;dy++){
                BlockPos clear=arrowBlock.offset(step,dy,0);level.getChunkAt(clear);level.setBlock(clear,Blocks.AIR.defaultBlockState(),3);
            }
            helper.assertTrue(level.getBlockState(net.minecraft.core.BlockPos.containing(arrowStart)).isAir(),"Projectile fixture must start in air");
            helper.assertTrue(level.getEntities(arrow,new net.minecraft.world.phys.AABB(arrowStart,arrowStart.add(8,.5,.5)),entity->entity==part).contains(part),
                "Forge's projectile entity query did not return the Serpent body part from its bounding box");
            level.addFreshEntity(arrow);
            for(int tick=0;tick<8&&!arrow.isRemoved()&&serpent.getHealth()==before;tick++)arrow.tick();
            helper.assertTrue(serpent.getHealth()<before,"Vanilla arrow failed to hit Serpent part: arrow="+arrow.position()+", delta="+arrow.getDeltaMovement()+", part="+part.getBoundingBox()+", removed="+arrow.isRemoved());
            serpent.invulnerableTime=0;serpent.hurtTime=0;serpent.hurtDuration=0;before=serpent.getHealth();
            var bolt=SpellBolt.launch((net.minecraft.server.level.ServerLevel)level,owner,part.position().add(-4,0,0),new Vec3(1,0,0),9,Combat.Element.DREAM,1.5f,0);
            for(int tick=0;tick<8&&!bolt.isRemoved()&&serpent.getHealth()==before;tick++)bolt.tick();
            helper.assertTrue(serpent.getHealth()<before,"A direct SpellBolt hit on a Serpent body part did not resolve to its parent: bolt="+bolt.position()+", part="+part.getBoundingBox()+", removed="+bolt.isRemoved()+", health="+serpent.getHealth());
        }finally{serpent.discard();owner.discard();}helper.succeed();
    }
    @GameTest(template="empty",batch="boss_target_serpent_los",timeoutTicks=1200)
    public static void serpent_acquires_player_through_wall_inside_source_range(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 origin=isolatedTestOrigin(helper,250);
        inLoadedTestArea(helper,origin,128,()->{
        var serpent=Content.SERPENT.get().create(level);serpent.setPos(origin);level.addFreshEntity(serpent);
        Vec3 targetPos=serpent.position().add(120,0,0);
        var player=encounterTestPlayer(helper,targetPos,net.minecraft.world.level.GameType.SURVIVAL);
        int wallX=net.minecraft.core.BlockPos.containing(serpent.getX()+60,serpent.getY(),serpent.getZ()).getX();
        int floorY=net.minecraft.core.BlockPos.containing(serpent.getX(),serpent.getY(),serpent.getZ()).getY();
        for(int z=(int)Math.floor(serpent.getZ())-2;z<=(int)Math.floor(serpent.getZ())+2;z++)
            for(int y=floorY-1;y<=floorY+5;y++)level.setBlockAndUpdate(new BlockPos(wallX,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        // Keep the range premise fixed while the real target goal runs; random free flight can otherwise leave 200 blocks.
        helper.onEachTick(()->serpent.setPos(origin));
        helper.runAfterDelay(50,()->{
            helper.assertTrue(Math.abs(serpent.distanceTo(player)-120)<=2.1,"LOS fixture drifted outside its initial range premise");
            helper.assertTrue(serpent.getTarget()==player,"Serpent failed to acquire a player 120 blocks away through a solid wall: target="+serpent.getTarget()+", range="+serpent.getAttributeValue(Attributes.FOLLOW_RANGE)+", sight="+serpent.hasLineOfSight(player)+", alive="+player.isAlive()+", creative="+player.isCreative()+", spectator="+player.isSpectator()+", actualDistance="+serpent.distanceTo(player)+", bossAge="+serpent.tickCount+", removed="+serpent.isRemoved()+", listed="+level.players().contains(player));
            player.discard();serpent.discard();helper.succeed();
        });
        });
    }
    @GameTest(template="empty",batch="boss_target_serpent_boundary",timeoutTicks=20)
    public static void serpent_attack_gate_matches_source_130_block_radius(GameTestHelper helper){
        Vec3 head=Vec3.ZERO,look=new Vec3(0,0,1);var targetId=java.util.UUID.randomUUID();
        var inside=new SerpentMotion();var insideRandom=net.minecraft.util.RandomSource.create(38);inside.initialize(head,look,insideRandom);
        var insideState=inside.save();insideState.putInt("Cooldown",1);inside.load(insideState);
        var reached=inside.tick(head,look,new Vec3(130,0,0),targetId,insideRandom);
        helper.assertTrue(reached.startedAttack(),"Serpent did not start a ready attack with a target exactly 130 blocks away");
        var outside=new SerpentMotion();var outsideRandom=net.minecraft.util.RandomSource.create(39);outside.initialize(head,look,outsideRandom);
        var outsideState=outside.save();outsideState.putInt("Cooldown",1);outside.load(outsideState);
        var held=outside.tick(head,look,new Vec3(130.01,0,0),targetId,outsideRandom);
        helper.assertTrue(!held.startedAttack()&&outside.save().getInt("Cooldown")==1,"Serpent advanced its attack timer beyond the source 130-block radius");
        helper.succeed();
    }
    @GameTest(template="empty",batch="boss_target_sorcerer_los",timeoutTicks=60)
    public static void sorcerer_acquires_player_through_wall_within_source_range(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();clearEncounterTestPlayers(level);var boss=Content.SORCERER.get().create(level);
        boss.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(boss);
        Vec3 targetPos=boss.position().add(40,0,0);level.getChunkAt(net.minecraft.core.BlockPos.containing(targetPos));
        var player=encounterTestPlayer(helper,targetPos,net.minecraft.world.level.GameType.SURVIVAL);
        int wallX=net.minecraft.core.BlockPos.containing(boss.getX()+20,boss.getY(),boss.getZ()).getX();
        int floorY=net.minecraft.core.BlockPos.containing(boss.getX(),boss.getY(),boss.getZ()).getY();
        for(int z=(int)Math.floor(boss.getZ())-2;z<=(int)Math.floor(boss.getZ())+2;z++)
            for(int y=floorY-1;y<=floorY+5;y++)level.setBlockAndUpdate(new BlockPos(wallX,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(50,()->{
            helper.assertTrue(boss.getTarget()==player,"Sorcerer failed to acquire a player 40 blocks away through a solid wall: target="+boss.getTarget()+", range="+boss.getAttributeValue(Attributes.FOLLOW_RANGE)+", sight="+boss.hasLineOfSight(player)+", alive="+player.isAlive()+", creative="+player.isCreative()+", spectator="+player.isSpectator());
            player.discard();boss.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="boss_target_sorcerer_range",timeoutTicks=60)
    public static void sorcerer_does_not_acquire_beyond_source_selector_range(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();clearEncounterTestPlayers(level);var boss=Content.SORCERER.get().create(level);
        boss.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(boss);
        Vec3 targetPos=boss.position().add(55,0,0);level.getChunkAt(net.minecraft.core.BlockPos.containing(targetPos));
        var player=encounterTestPlayer(helper,targetPos,net.minecraft.world.level.GameType.SURVIVAL);
        helper.runAfterDelay(50,()->{
            helper.assertTrue(boss.getTarget()==null,"Sorcerer acquired a player beyond the source 50-block attack selectors");
            helper.assertTrue(boss.getAttributeValue(Attributes.FOLLOW_RANGE)==50,"Sorcerer follow range is not aligned to its 50-block source selectors");
            player.discard();boss.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="boss_target_serpent_departure",timeoutTicks=20)
    public static void serpent_departure_cleanup_uses_source_500_block_radius(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();clearEncounterTestPlayers(level);var boss=Content.SERPENT.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);
        boss.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(boss);
        var player=encounterTestPlayer(helper,boss.position().add(500,0,0),net.minecraft.world.level.GameType.SPECTATOR);
        var beam=SerpentBeamHazard.spawn(level,boss,boss.position(),180);
        boss.tick();helper.assertTrue(!boss.isRemoved(),"Serpent was removed with a player exactly 500 blocks away");
        var second=Content.SERPENT.get().create(level);second.setNoAi(true);second.setNoGravity(true);second.setPos(boss.position().add(1000,0,0));level.getChunkAt(second.blockPosition());level.addFreshEntity(second);
        var secondBeam=SerpentBeamHazard.spawn(level,second,second.position(),180);
        player.setPos(second.position());boss.tick();
        helper.assertTrue(boss.isRemoved()&&beam.isRemoved(),"Departed Serpent or its owned beam survived after all players left the source 500-block radius");
        helper.assertTrue(!second.isRemoved()&&!secondBeam.isRemoved(),"Removing one Serpent also removed another encounter's boss or beam");
        player.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="boss_target_sorcerer_departure",timeoutTicks=20)
    public static void sorcerer_departure_cleanup_uses_source_100_block_radius(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();clearEncounterTestPlayers(level);var boss=Content.SORCERER.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);
        boss.setPos(helper.absolutePos(new BlockPos(3,4,3)).getCenter());level.addFreshEntity(boss);
        var player=encounterTestPlayer(helper,boss.position().add(100,0,0),net.minecraft.world.level.GameType.SPECTATOR);
        var staff=boss.spawnNullpointForTest(level);helper.assertTrue(staff!=null,"Sorcerer fixture failed to create its owned Nullpoint");
        boss.tick();helper.assertTrue(!boss.isRemoved(),"Sorcerer was removed with a player exactly 100 blocks away");
        var second=Content.SORCERER.get().create(level);second.setNoAi(true);second.setNoGravity(true);second.setPos(boss.position().add(200,0,0));level.getChunkAt(second.blockPosition());level.addFreshEntity(second);
        var secondWave=IceSpikeWave.spawn(level,second,second.position());
        player.setPos(second.position());boss.tick();
        helper.assertTrue(boss.isRemoved()&&staff.isRemoved(),"Departed Sorcerer or its owned Nullpoint survived after all players left the source 100-block radius");
        helper.assertTrue(!second.isRemoved()&&!secondWave.isRemoved(),"Removing one Sorcerer also removed another encounter's boss or owned ice wave");
        player.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_uses_source_speed_tiers_and_segment_spacing(GameTestHelper helper){
        helper.assertTrue(NativeBoss.serpentSpeedForTier(1)==1&&NativeBoss.serpentSpeedForTier(2)==2&&NativeBoss.serpentSpeedForTier(3)==3,
            "Serpent movement no longer uses the source 1/2/3-block speed tiers");
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);serpent.tick();
        double spacing=serpent.position().distanceTo(serpent.serpentRenderSegment(1,1));
        helper.assertTrue(Math.abs(spacing-2)<.001,"Serpent's first visible segment is "+spacing+" blocks away at summon after the head moved two blocks from the shared summon point");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void serpent_normally_travels_at_source_two_blocks_per_tick(GameTestHelper helper){
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,5,4)).getCenter());helper.getLevel().addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);
        Vec3[] before={serpent.position()};
        var target=helper.spawn(EntityType.COW,new BlockPos(8,2,8));target.setNoAi(true);serpent.setTarget(target);
        // Curved flight can end close to its origin. Test per-tick travel, not net displacement.
        for(int tick=1;tick<=7;tick++)helper.runAfterDelay(tick,()->{
            helper.assertTrue(Math.abs(serpent.position().distanceTo(before[0])-2)<.01,"Serpent did not move exactly two blocks this tick");
            before[0]=serpent.position();
        });
        helper.runAfterDelay(8,()->{
            double spacing=serpent.serpentRenderSegment(2,1).distanceTo(serpent.serpentRenderSegment(1,1));
            helper.assertTrue(spacing<=4.5,"A segment failed to chase its preceding segment: "+spacing);helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void serpent_body_matches_original_function_files(GameTestHelper helper) throws Exception {
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,90,4)).getCenter();
        var markers=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        for(int i=0;i<42;i++){var marker=EntityType.MARKER.create(level);marker.setPos(origin);marker.addTag("body_"+marker.getStringUUID());level.addFreshEntity(marker);markers.add(marker);}
        var body=new SerpentBody();body.initialize(origin);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
        var root=java.nio.file.Path.of("../../_nyctocosm_source/datapack/data/serpent/functions/__generated__/block");
        String[][] lines=new String[3][41];double[][] minimumDistance=new double[3][41];
        for(int tier=1;tier<=3;tier++)for(int i=0;i<41;i++){
            int file=35+(tier-1)*51+i;
            String original=java.nio.file.Files.readAllLines(root.resolve(file+".mcfunction")).stream().filter(line->line.startsWith("execute ")).findFirst().orElseThrow();
            var distanceMatcher=java.util.regex.Pattern.compile("distance=([0-9.]+)\\.\\.").matcher(original);
            if(!distanceMatcher.find())throw new IllegalStateException("Source serpent body command has no minimum-distance gate: "+original);
            minimumDistance[tier-1][i]=Double.parseDouble(distanceMatcher.group(1));
            // Resolve the known predecessor directly; preserve the source distance gate in Java.
            // `facing entity` is equivalent to facing that entity's feet, so the test uses the
            // already-known feet coordinates and keeps the source local teleport command verbatim.
            lines[tier-1][i]=original.substring(original.indexOf("run ")+4);
        }
        var trace=new StringBuilder("tick,tier,link,error\n");double maximum=0;
        try{
            for(int tick=0;tick<420;tick++){
                int tier=1+(tick/70)%3;
                Vec3 head=origin.add(Math.sin(tick*.035)*34,Math.sin(tick*.06)*14,Math.cos(tick*.04)*38);
                level.getChunkAt(BlockPos.containing(head));markers.get(0).setPos(head);
                body.tick(head,tier);
                for(int i=0;i<41;i++){
                    var marker=markers.get(i+1);
                    Vec3 nativePosition=body.position(i+1,1,head);
                    Vec3 markerBefore=marker.position();var leader=markers.get(i);
                    int commandResult=marker.position().distanceTo(leader.position())>=minimumDistance[tier-1][i]
                        ?level.getServer().getCommands().performPrefixedCommand(source.withEntity(marker),"execute at @s facing "+String.format(java.util.Locale.ROOT,"%.9f %.9f %.9f",leader.getX(),leader.getY(),leader.getZ())+" run "+lines[tier-1][i]):0;
                    double error=nativePosition.distanceTo(marker.position());maximum=Math.max(maximum,error);
                    helper.assertTrue(error<.002,"Source body position diverged at tick "+tick+" link "+(i+1)+": "+error+"; command="+commandResult+" markerBefore="+markerBefore+" markerAfter="+marker.position()+" native="+body.position(i+1,1,head)+" leader="+leader.position()+" function="+lines[tier-1][i]);
                    helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(body.yaw(i+1,1)-marker.getYRot()))<.01,"Segment rotation diverged at tick "+tick+" link "+(i+1)+" native="+body.yaw(i+1,1)+" original="+marker.getYRot());
                    trace.append(tick).append(',').append(tier).append(',').append(i+1).append(',').append(error).append('\n');
                }
                if(tick==210){var saved=body.save();body=new SerpentBody();body.load(saved);}
            }
            var output=java.nio.file.Path.of("../build/validation/serpent-body-source-trace.csv");java.nio.file.Files.createDirectories(output.getParent());java.nio.file.Files.writeString(output,trace);
            var observer=new SerpentBody();observer.receive(body.snapshot(),false);
            Vec3 finalHead=markers.get(0).position().add(3,1,0);body.tick(finalHead,3);observer.receive(body.snapshot(),true);observer.finishInterpolation();
            for(int i=1;i<=41;i++)helper.assertTrue(observer.position(i,0,finalHead).distanceToSqr(observer.position(i,1,finalHead))<.00000001,"A paused body snapshot replayed its interpolation every client tick");
            System.out.println("SERPENT_SOURCE_BODY_MATCH samples=17220 maxError="+maximum);
        }finally{markers.forEach(net.minecraft.world.entity.Entity::discard);}
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void serpent_beam_hits_high_targets_and_keeps_round_boundary(GameTestHelper helper){
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(2,2,2));owner.setNoAi(true);owner.setNoGravity(true);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(5,60,5));target.setNoAi(true);target.setNoGravity(true);
        Vec3 base=target.position().add(0,-58,0);SerpentBeamHazard.spawn(helper.getLevel(),owner,base,61);
        helper.assertTrue(!SerpentBeamHazard.containsFeet(base,base.add(4,30,4)),"Square corners must not damage targets outside the five-block source radius");
        helper.assertTrue(SerpentBeamHazard.containsFeet(base,base.add(0,85,0))&&!SerpentBeamHazard.containsFeet(base,base.add(0,85.01,0)),"Ray tip must end at 80 plus the source selector radius");
        helper.runAfterDelay(3,()->{helper.assertTrue(target.getHealth()<target.getMaxHealth(),"Original 80-block column failed to hit a high target");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=1200,batch="serpent_late_landing")
    public static void serpent_beam_selects_late_target_after_reload(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,144,()->assertSerpentLateLanding(helper,origin));
    }
    private static void assertSerpentLateLanding(GameTestHelper helper,Vec3 origin){
        var level=helper.getLevel();Vec3 destination=origin.add(60,0,60);
        var floorStates=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        int floorY=net.minecraft.util.Mth.floor(destination.y)-1;
        // A floor below a generated ocean is not a valid spreadplayers landing surface.
        // Own a high platform and clear its columns so every possible random landing is safe.
        for(int x=net.minecraft.util.Mth.floor(destination.x)-21;x<=net.minecraft.util.Mth.floor(destination.x)+21;x++)
            for(int z=net.minecraft.util.Mth.floor(destination.z)-21;z<=net.minecraft.util.Mth.floor(destination.z)+21;z++){
                BlockPos floor=new BlockPos(x,floorY,z);floorStates.put(floor,level.getBlockState(floor));
                level.setBlock(floor,Blocks.STONE.defaultBlockState(),2);
                for(int y=floorY+1;y<level.getMaxBuildHeight();y++){
                    BlockPos above=new BlockPos(x,y,z);var state=level.getBlockState(above);
                    if(!state.isAir()){floorStates.put(above,state);level.setBlock(above,Blocks.AIR.defaultBlockState(),2);}
                }
            }
        try{
        helper.assertTrue(SerpentBeamHazard.chooseLanding(level,destination,net.minecraft.util.RandomSource.create(7))!=null,
            "Late-landing fixture did not provide a safe source spreadplayers surface");
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-3,-1,-3));level.addFreshEntity(owner);
        var target=EntityType.COW.create(level);target.setNoAi(true);target.setNoGravity(true);target.setPos(origin);level.addFreshEntity(target);
        helper.assertTrue(level.getEntity(owner.getUUID())==owner&&level.getEntity(target.getUUID())==target,
            "Late-landing fixture entities are not visible in the ready arena");
        var beam=SerpentBeamHazard.spawn(helper.getLevel(),owner,target,143);Vec3 initial=beam.position();
        beam.tick();helper.assertTrue(beam.position().equals(initial),"Column moved before delay 140");
        var saved=new CompoundTag();beam.saveWithoutId(saved);beam.discard();
        helper.assertTrue(saved.hasUUID("Target")&&saved.getUUID("Target").equals(target.getUUID()),"Saving the pending column lost its target UUID");
        var loaded=Content.SERPENT_BEAM.get().create(helper.getLevel());loaded.load(saved);helper.getLevel().addFreshEntity(loaded);
        var loadedState=new CompoundTag();loaded.saveWithoutId(loadedState);
        helper.assertTrue(loadedState.hasUUID("Target")&&loadedState.getUUID("Target").equals(target.getUUID()),"Reloading the pending column changed its target UUID");
        target.teleportToWithTicket(target.getX()+60,target.getY(),target.getZ()+60);
        helper.assertTrue(helper.getLevel().getEntity(target.getUUID())==target,"Late Serpent target was not registered in its forced destination chunk");
        loaded.tick();helper.assertTrue(loaded.position().equals(initial),"Reload selected a landing early");loaded.tick();
        Vec3 landing=loaded.position();
        helper.assertTrue(Math.abs(landing.x-target.getX())<=20.5&&Math.abs(landing.z-target.getZ())<=20.5,
            "Landing did not use the target position at delay 140: landing="+landing+", target="+target.position());
        helper.assertTrue(landing.distanceTo(initial)>30&&landing.x-Math.floor(landing.x)==.5&&landing.z-Math.floor(landing.z)==.5,"Landing must use spreadplayers block centers");
        target.setPos(target.position().add(50,0,50));loaded.tick();helper.assertTrue(loaded.position().equals(landing),"Warning column followed the player after landing selection");
        var edge=SerpentBeamHazard.spawn(helper.getLevel(),owner,target,140);Vec3 edgeStart=edge.position();edge.tick();helper.assertTrue(edge.position().equals(edgeStart),"Source delay 140 initial value decrements to 139 and skips spreadplayers");
        edge.discard();loaded.discard();target.discard();owner.discard();helper.succeed();
        }finally{for(var entry:floorStates.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);}
    }
    @GameTest(template="empty",timeoutTicks=1200,batch="serpent_beam_retarget")
    public static void serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,144,()->assertSerpentBeamRetarget(helper,origin));
    }
    private static void assertSerpentBeamRetarget(GameTestHelper helper,Vec3 origin){
        var level=helper.getLevel();Vec3 destination=origin.add(68,0,68);
        var floorStates=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        int floorY=net.minecraft.util.Mth.floor(destination.y)-1;
        for(int x=net.minecraft.util.Mth.floor(destination.x)-21;x<=net.minecraft.util.Mth.floor(destination.x)+21;x++)
            for(int z=net.minecraft.util.Mth.floor(destination.z)-21;z<=net.minecraft.util.Mth.floor(destination.z)+21;z++){
                BlockPos floor=new BlockPos(x,floorY,z);floorStates.put(floor,level.getBlockState(floor));
                level.setBlock(floor,Blocks.STONE.defaultBlockState(),2);
                for(int y=floorY+1;y<level.getMaxBuildHeight();y++){
                    BlockPos above=new BlockPos(x,y,z);var state=level.getBlockState(above);
                    if(!state.isAir()){floorStates.put(above,state);level.setBlock(above,Blocks.AIR.defaultBlockState(),2);}
                }
            }
        try{
        helper.assertTrue(SerpentBeamHazard.chooseLanding(level,destination,net.minecraft.util.RandomSource.create(7))!=null,
            "Retarget fixture did not provide a safe source spreadplayers surface");
        var owner=Content.SERPENT.get().create(level);owner.setNoAi(true);owner.setPos(origin.add(0,1,0));level.addFreshEntity(owner);
        // Entity-ticking tickets load the arena; the encounter also requires a ServerPlayer
        // within its source departure radius before NativeBoss.tick updates multipart boxes.
        keepBossLoadedForTest(helper,owner);
        var target=EntityType.COW.create(level);target.setNoAi(true);target.setNoGravity(true);target.setPos(origin.add(1,0,1));level.addFreshEntity(target);
        helper.assertTrue(level.getEntity(owner.getUUID())==owner&&level.getEntity(target.getUUID())==target,
            "Retarget fixture entities are not visible in the ready arena");
        owner.startSerpentAttackForTest(level,target,3);var beam=SerpentBeamHazard.spawn(level,owner,target,141);
        var next=EntityType.COW.create(level);next.setNoAi(true);next.setNoGravity(true);next.setPos(origin.add(3,0,3));level.addFreshEntity(next);next.teleportToWithTicket(next.getX()+65,next.getY(),next.getZ()+65);
        helper.assertTrue(level.getEntity(next.getUUID())==next,"Retargeted Serpent target was not registered in its forced destination chunk");owner.startSerpentAttackForTest(level,next,1);
        helper.assertTrue(next.getUUID().equals(owner.serpentLastSelectedTarget()),"Starting the next source attack failed to record its target UUID");
        var saved=new CompoundTag();owner.saveWithoutId(saved);owner.load(saved);
        helper.assertTrue(next.getUUID().equals(owner.serpentLastSelectedTarget()),"Reloading the Serpent lost the most recently selected target UUID");
        beam.tick();helper.assertTrue(beam.position().distanceToSqr(target.position())>30*30&&Math.abs(beam.getX()-next.getX())<=20.5,
            "Pending column ignored owner's most recently selected attack target after reload: beam="+beam.position()+", target="+next.position());
        for(int tick=0;tick<20;tick++){
            owner.tick();helper.assertTrue(!owner.isRemoved(),"Serpent fixture exited its encounter before updating body parts");
            Vec3 previous=owner.serpentRenderSegment(2,1);owner.setPos(owner.position().add(2,0,0));owner.tick();
            helper.assertTrue(!owner.isRemoved(),"Serpent fixture exited its encounter after the manual head move");
            helper.assertTrue(owner.getParts()[0].position().distanceToSqr(previous)<.00001,
                "Hitbox moved after its segment rather than before it: tick="+tick+", part="+owner.getParts()[0].position()+", previous="+previous);
        }
        var motion=new SerpentMotion();var random=net.minecraft.util.RandomSource.create(7);Vec3 head=new Vec3(0,100,0),look=new Vec3(0,0,1);
        motion.initialize(head,look,random);motion.begin(head,0,0,3,target.getUUID());boolean ended=false;
        for(int tick=0;tick<65;tick++){var step=motion.tick(head,look,new Vec3(0,100,0),target.getUUID(),random);head=step.position();look=step.direction();if(motion.state()==SerpentMotion.ROAM){ended=true;break;}}
        helper.assertTrue(ended&&motion.save().getInt("Cooldown")==20,"Beam exit did not set the original 20 tick attack cooldown");helper.succeed();
        }finally{for(var entry:floorStates.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);}
    }
    @GameTest(template="empty",timeoutTicks=150,batch="serpent_dive_reference")
    public static void serpent_dive_matches_vanilla_command_marker_chain(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,60,4)).getCenter();
        var forced=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        // 80 steps * 2 blocks + 30-block look marker + 16-block fixture offset + correction.
        // The former 96-block square could drop UUID selectors midway through a valid trace.
        for(int x=net.minecraft.util.Mth.floor(origin.x-224)>>4;x<=(net.minecraft.util.Mth.floor(origin.x+224)>>4);x++)
            for(int z=net.minecraft.util.Mth.floor(origin.z-224)>>4;z<=(net.minecraft.util.Mth.floor(origin.z+224)>>4);z++){
                if(!level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(x,z))){level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));}
                level.getChunk(x,z);
            }
        // Loading terrain alone is insufficient: wait until forced chunks expose entities to UUID selectors.
        helper.runAfterDelay(10,()->{
            try{
                for(Vec3 offset:new Vec3[]{Vec3.ZERO,new Vec3(16,0,16),new Vec3(-16,0,-16),new Vec3(.125,0,-.25)})assertSerpentDiveTrace(helper,origin.add(offset));
                helper.succeed();
            }finally{for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);}
        });
    }
    private static void assertSerpentDiveTrace(GameTestHelper helper,Vec3 origin){
        var level=helper.getLevel();level.getChunkAt(BlockPos.containing(origin));
        var head=EntityType.ARMOR_STAND.create(level);head.moveTo(origin.x,origin.y,origin.z,0,0);head.setNoGravity(true);level.addFreshEntity(head);
        var current=EntityType.MARKER.create(level);current.setPos(origin.add(0,0,30));level.addFreshEntity(current);
        var toward=EntityType.MARKER.create(level);toward.setPos(current.position());level.addFreshEntity(toward);
        var target=EntityType.MARKER.create(level);target.setPos(origin.add(15,-5,10));level.addFreshEntity(target);
        var motion=new SerpentMotion();var random=net.minecraft.util.RandomSource.create(42);
        motion.initialize(origin,new Vec3(0,0,1),random);motion.beginDive(origin,new Vec3(0,0,1),target.position(),target.getUUID());
        Vec3 nativeHead=origin,look=new Vec3(0,0,1);
        var commands=level.getServer().getCommands();var source=level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
        String h=head.getStringUUID(),c=current.getStringUUID(),t=toward.getStringUUID(),g=target.getStringUUID();
        try{
            for(int tick=0;tick<80;tick++){
                Vec3 live=origin.add(Math.sin(tick*.17)*18,(tick<40?-5:12),Math.cos(tick*.11)*20);
                // These are the source headai/move/ai commands, executed by Minecraft itself.
                helper.assertTrue(commands.performPrefixedCommand(source,"execute as "+h+" at @s facing entity "+c+" feet run tp @s ~ ~ ~ ~ ~")>0,"Vanilla head-facing command failed at "+tick);
                helper.assertTrue(commands.performPrefixedCommand(source,"execute at "+h+" run tp "+c+" ^ ^ ^30")>0,"Vanilla current-marker command failed at "+tick);
                helper.assertTrue(commands.performPrefixedCommand(source,"execute at "+h+" facing entity "+g+" feet run tp "+t+" ^ ^ ^30")>0,"Vanilla toward-marker command failed at "+tick);
                helper.assertTrue(commands.performPrefixedCommand(source,"execute as "+h+" at @s run tp @s ^ ^ ^2")>0,"Vanilla head-movement command failed at "+tick);
                if(current.distanceTo(target)>=3.1)helper.assertTrue(commands.performPrefixedCommand(source,"execute as "+c+" at @s facing entity "+g+" feet run tp @s ^ ^ ^3")>0,"Vanilla current-correction command failed at "+tick);
                target.setPos(live);
                var step=motion.tick(nativeHead,look,live,target.getUUID(),random);nativeHead=step.position();look=step.direction();
                helper.assertTrue(nativeHead.distanceTo(head.position())<.01,"Native marker chain diverged from vanilla commands at tick "+tick+": "+nativeHead.distanceTo(head.position())+", native="+nativeHead+", vanilla="+head.position()+", yaw="+head.getYRot()+", pitch="+head.getXRot()+", nativeCurrent="+motion.save().getCompound("Current")+", vanillaCurrent="+current.position());
                if(tick==35){var saved=motion.save();motion=new SerpentMotion();motion.load(saved);}
            }
        }finally{head.discard();current.discard();toward.discard();target.discard();}
    }
    @GameTest(template="empty")
    public static void serpent_twirl_matches_vanilla_local_coordinates(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,60,4)).getCenter();
        var head=EntityType.ARMOR_STAND.create(level);head.moveTo(origin.x,origin.y,origin.z,37,-24);level.addFreshEntity(head);
        var target=EntityType.MARKER.create(level);target.setPos(origin);level.addFreshEntity(target);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();var commands=level.getServer().getCommands();
        commands.performPrefixedCommand(source,"execute at "+head.getStringUUID()+" positioned ~ ~10 ~ run tp "+target.getStringUUID()+" ^ ^ ^10 ~90 ~");
        var motion=new SerpentMotion();var random=net.minecraft.util.RandomSource.create(9);motion.initialize(origin,head.getLookAngle(),random);
        motion.begin(origin,head.getYRot(),head.getXRot(),1,head.getUUID());
        Vec3 at=origin,look=head.getLookAngle();
        try{
            for(int tick=0;tick<61;tick++){
                commands.performPrefixedCommand(source,"execute as "+target.getStringUUID()+" at @s rotated ~ 0 positioned ~ ~1 ~ rotated ~9 ~ run tp @s ^ ^ ^3 ~ ~");
                if(tick>0){var step=motion.tick(at,look,origin,head.getUUID(),random);at=step.position();look=step.direction();}
                helper.assertTrue(motion.target().distanceTo(target.position())<.002,"Twirl target differs from original local-coordinate commands at tick "+tick);
            }
        }finally{head.discard();target.discard();}
        helper.succeed();
    }
    @GameTest(template="empty",batch="serpent_laser_damage",timeoutTicks=20)
    public static void serpent_laser_uses_source_player_feet_radius(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-10,0,0));level.addFreshEntity(owner);
        var exact=emissaryPlayer(helper,origin.add(3,0,8));float exactHealth=exact.getHealth();
        var bodyOnly=emissaryPlayer(helper,origin.add(3.2,0,4));float bodyHealth=bodyOnly.getHealth();
        Combat.serpentLaser((net.minecraft.server.level.ServerLevel)level,owner,origin,origin.add(0,0,12),12);
        helper.assertTrue(exact.getHealth()<exactHealth,"Source feet selector must include a player exactly three blocks from a ray sample");
        helper.assertTrue(bodyOnly.getHealth()==bodyHealth,"A body AABB grazing the width-expanded line must not hit when player feet are outside the source radius: "+bodyHealth+"→"+bodyOnly.getHealth());
        exact.discard();bodyOnly.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="serpent_laser_damage",timeoutTicks=20)
    public static void serpent_laser_requires_zero_hurt_time(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-10,0,0));level.addFreshEntity(owner);
        var target=emissaryPlayer(helper,origin.add(0,0,6));target.hurtTime=5;float health=target.getHealth();
        Combat.serpentLaser((net.minecraft.server.level.ServerLevel)level,owner,origin,origin.add(0,0,12),12);
        helper.assertTrue(target.getHealth()==health,"Source laser must skip targets with nonzero HurtTime: "+health+"→"+target.getHealth());
        target.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="serpent_laser_damage",timeoutTicks=20)
    public static void serpent_laser_only_selects_players(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-10,0,0));level.addFreshEntity(owner);
        var cow=EntityType.COW.create(level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(origin.add(0,0,6));level.addFreshEntity(cow);float health=cow.getHealth();
        Combat.serpentLaser((net.minecraft.server.level.ServerLevel)level,owner,origin,origin.add(0,0,12),12);
        helper.assertTrue(cow.getHealth()==health,"Source @a laser selector must not damage non-player living entities: "+health+"→"+cow.getHealth());
        cow.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_laser_sweeps_a_fixed_marker_not_the_live_target(GameTestHelper helper){
        var random=net.minecraft.util.RandomSource.create(51);var motion=new SerpentMotion();
        Vec3 head=new Vec3(0,100,0),look=new Vec3(0,0,1),live=new Vec3(10,90,20);var id=java.util.UUID.randomUUID();
        motion.initialize(head,look,random);motion.begin(head,0,0,2,id);
        for(int tick=0;tick<62;tick++){var step=motion.tick(head,look,live,id,random);head=step.position();look=step.direction();}
        helper.assertTrue(motion.state()==SerpentMotion.LASER,"Laser did not begin after the complete twirl loop");
        Vec3 first=motion.target();var one=motion.tick(head,look,live.add(50,30,-20),id,random);Vec3 displacement=motion.target().subtract(first);
        helper.assertTrue(Math.abs(displacement.length()-.5)<.001&&displacement.y==0,"Laser target must move horizontally at half a block per tick");
        Vec3 second=motion.target();motion.tick(one.position(),one.direction(),live.add(-60,-20,60),id,random);
        helper.assertTrue(motion.target().subtract(second).distanceTo(displacement)<.00001,"Laser target follows player changes instead of its fixed heading");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_active_marker_state_roundtrips_and_cleans_up(GameTestHelper helper){
        for(int attack=1;attack<=3;attack++){
            var motion=new SerpentMotion();var random=net.minecraft.util.RandomSource.create(15);
            Vec3 head=new Vec3(0,100,0),look=new Vec3(0,0,1),live=new Vec3(5,95,10);var id=java.util.UUID.randomUUID();
            motion.initialize(head,look,random);motion.begin(head,0,0,attack,id);
            for(int tick=0;tick<70;tick++){
                var restored=new SerpentMotion();restored.load(motion.save());
                var step=motion.tick(head,look,live,id,net.minecraft.util.RandomSource.create(tick));
                var replay=restored.tick(head,look,live,id,net.minecraft.util.RandomSource.create(tick));
                helper.assertTrue(step.equals(replay)&&motion.save().equals(restored.save()),"Reload changed attack "+attack+" marker chain at tick "+tick);
                head=step.position();look=step.direction();live=live.add(.1,.02,-.05);
            }
            motion.tick(head,look,null,null,random);
            helper.assertTrue(motion.state()==SerpentMotion.ROAM&&motion.targetId()==null,"Lost target left an orphaned active attack");
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_body_path_survives_save_and_observer_spawn(GameTestHelper helper){
        var boss=Content.SERPENT.get().create(helper.getLevel());boss.setNoAi(true);
        Vec3 origin=helper.absolutePos(new BlockPos(4,80,4)).getCenter();
        for(int tick=0;tick<120;tick++){boss.setPos(origin.add(Math.sin(tick*.06)*30,tick*.4,Math.cos(tick*.06)*30));boss.tick();}
        var saved=new CompoundTag();boss.saveWithoutId(saved);var loaded=Content.SERPENT.get().create(helper.getLevel());loaded.load(saved);
        var observer=Content.SERPENT.get().create(helper.getLevel());observer.setPos(boss.position());
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{boss.writeSpawnData(buffer);observer.readSpawnData(buffer);}finally{buffer.release();}
        for(int index=0;index<=41;index++){
            Vec3 expected=boss.serpentRenderSegment(index,1);
            helper.assertTrue(expected.distanceTo(loaded.serpentRenderSegment(index,1))<.00001,"Saved body path lost segment "+index);
            helper.assertTrue(expected.distanceTo(observer.serpentRenderSegment(index,1))<.0001,"New observer sees a straight replacement tail at segment "+index);
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_dive_exits_after_400_ticks(GameTestHelper helper){
        var motion=new SerpentMotion();var random=net.minecraft.util.RandomSource.create(15);
        Vec3 head=new Vec3(0,100,0),look=new Vec3(0,0,1),live=head.add(20,0,20);var id=java.util.UUID.randomUUID();
        motion.initialize(head,look,random);motion.beginDive(head,look,live,id);
        for(int tick=1;tick<=400;tick++){
            // A target circling the serpent keeps this timeout scenario inside the arena.
            live=head.add(look.z*30,0,-look.x*30);
            var step=motion.tick(head,look,live,id,random);head=step.position();look=step.direction();
            helper.assertTrue(motion.state()==(tick<400?SerpentMotion.DIVE:SerpentMotion.ROAM),"Dive exited at the wrong tick: "+tick);
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_leaves_dive_at_source_arena_boundary(GameTestHelper helper){
        var motion=new SerpentMotion();var random=net.minecraft.util.RandomSource.create(15);
        Vec3 head=new Vec3(0,100,0),look=new Vec3(0,0,1),live=head.add(0,0,8);var id=java.util.UUID.randomUUID();
        motion.initialize(head,look,random);motion.beginDive(head,look,live,id);
        for(int tick=1;tick<=101;tick++){
            var step=motion.tick(head,look,live,id,random);head=step.position();look=step.direction();
            helper.assertTrue(motion.state()==(tick<=100?SerpentMotion.DIVE:SerpentMotion.ROAM),"Source 200-block arena boundary did not end straight-line pursuit at tick "+tick);
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void sorcerer_plane_is_owned_by_one_boss_and_removed_on_death(GameTestHelper helper){
        var a=Content.SORCERER.get().create(helper.getLevel());var b=Content.SORCERER.get().create(helper.getLevel());
        var save=new CompoundTag();a.saveWithoutId(save);var plane=new CompoundTag();plane.putLong("Start",0);plane.putLong("Age",123);plane.putFloat("Yaw",72);
        plane.putDouble("OriginX",10);plane.putDouble("OriginY",90);plane.putDouble("OriginZ",20);save.put("SorcererPlane",plane);a.load(save);b.load(save);
        b.setUUID(java.util.UUID.randomUUID());
        Vec3 origin=helper.absolutePos(new BlockPos(4,20,4)).getCenter();a.setPos(origin);b.setPos(origin.add(10,0,0));
        helper.getLevel().addFreshEntity(a);helper.getLevel().addFreshEntity(b);
        float angle=b.sorcererPlaneRoll(0);a.die(helper.getLevel().damageSources().generic());
        helper.assertTrue(!a.hasSorcererPlane()&&b.hasSorcererPlane()&&b.sorcererPlaneRoll(0)==angle,"One Sorcerer removed another boss's plane");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void sorcerer_plane_has_source_lifecycle_and_fixed_anchor(GameTestHelper helper){
        var level=helper.getLevel();var boss=Content.SORCERER.get().create(level);boss.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());
        var saved=new CompoundTag();boss.saveWithoutId(saved);saved.putString("Animation","rise");saved.putInt("Remaining",133);saved.putInt("AnimationAge",0);boss.load(saved);
        var staff=boss.spawnNullpointForTest(level);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        helper.runAfterDelay(125,()->helper.assertTrue(!boss.hasSorcererPlane(),"Plane appeared before rise frame 127"));
        helper.runAfterDelay(128,()->{
            helper.assertTrue(boss.hasSorcererPlane(),"Rise frame 127 did not summon the plane");
            Vec3 anchor=boss.sorcererPlanePosition();float yaw=boss.sorcererPlaneYaw(),roll=boss.sorcererPlaneRoll(0);
            boss.setPos(boss.position().add(4,0,0));boss.setYRot(boss.getYRot()+90);
            helper.assertTrue(anchor.equals(boss.sorcererPlanePosition())&&yaw==boss.sorcererPlaneYaw(),"Plane incorrectly follows boss movement or facing");
            var tag=new CompoundTag();boss.saveWithoutId(tag);var restored=Content.SORCERER.get().create(level);restored.load(tag);
            helper.assertTrue(restored.hasSorcererPlane()&&restored.sorcererPlanePosition().equals(anchor)&&restored.sorcererPlaneRoll(0)==roll,"Plane state lost on reload");
        });
        helper.runAfterDelay(136,()->staff.hurt(level.damageSources().generic(),400));
        helper.runAfterDelay(139,()->helper.assertTrue(boss.hasSorcererPlane(),"Plane disappeared before falltransition frame 5"));
        helper.runAfterDelay(145,()->{helper.assertTrue(!boss.hasSorcererPlane(),"Plane survived falltransition frame 5");helper.succeed();});
    }
    @GameTest(template="empty")
    public static void sorcerer_plane_rotates_in_its_original_disc_plane(GameTestHelper helper){
        var item=new org.joml.Matrix4f().translate(0,-4.5f/16,-31.75f/16).rotateX((float)-Math.PI/2).scale(4).translate(-.5f,-.5f,-.5f);
        var a=SorcererPlane.headTransform(0,0).mul(item);var b=SorcererPlane.headTransform(0,90).mul(item);
        var normalA=a.transformDirection(new org.joml.Vector3f(0,1,0)).normalize();var normalB=b.transformDirection(new org.joml.Vector3f(0,1,0)).normalize();
        helper.assertTrue(normalA.dot(normalB)>.999,"Plane wobbles around a world axis instead of rolling around the helmet Z axis");
        helper.assertTrue(Math.abs(normalA.y)<.001,"Source head display transform should make the disc vertical");
        double height=0;for(int tick=0;tick<133;tick++)height+=SorcererPlane.riseStep(tick);
        helper.assertTrue(Math.abs(height-5.83)<.00001,"Rise motion differs from source keyframes");helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_batched_client_samples_match_source_spacing(GameTestHelper helper){
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);serpent.tick();
        Vec3[] samples=serpent.serpentRenderSegments(.5f);
        helper.assertTrue(samples.length==42,"Serpent must render the head, 40 original segments, and a separate tail");
        for(int i=0;i<samples.length;i++)helper.assertTrue(samples[i].distanceToSqr(serpent.serpentRenderSegment(i,.5f))<.0001,
            "Batched render diverged from the source path at segment "+i);
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void serpent_dive_passes_target_instead_of_sticking_to_it(GameTestHelper helper){
        var target=helper.spawn(EntityType.COW,new BlockPos(4,3,10));target.setNoAi(true);target.setNoGravity(true);target.setInvulnerable(true);
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());Vec3 aim=target.getEyePosition();serpent.setPos(aim.x,aim.y,aim.z-8);helper.getLevel().addFreshEntity(serpent);keepBossLoadedForTest(helper,serpent);
        serpent.startSerpentDiveForTest(target);
        helper.runAfterDelay(9,()->{
            helper.assertTrue(serpent.distanceTo(target)>5,"Serpent Dive converged onto the target instead of carrying its momentum past them; distance="+serpent.distanceTo(target));
            helper.assertTrue(serpent.getZ()>target.getZ()+3,"Serpent did not overshoot the player along its source marker-guided heading: serpentZ="+serpent.getZ()+" targetZ="+target.getZ());helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=270)
    public static void living_nullpoint_permanently_shields_sorcerer(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        NullpointStaff staff=boss.spawnNullpointForTest(helper.getLevel());float health=boss.getHealth();
        helper.assertTrue(staff!=null&&!boss.hurt(helper.getLevel().damageSources().generic(),20)&&boss.getHealth()==health,
            "Sorcerer can be damaged while its planted Nullpoint is alive");
        helper.assertTrue(boss.isNoGravity(),"Sorcerer does not hover while protected by Nullpoint");
        helper.runAfterDelay(230,()->{
            helper.assertTrue(!boss.hurt(helper.getLevel().damageSources().generic(),20)&&boss.getHealth()==health,
                "Sorcerer shield expired even though Nullpoint survived");
            helper.assertTrue(boss.getY()>=staff.getY()+5.8,"Sorcerer failed to rise six blocks over its planted staff: bossY="+boss.getY()+" staffY="+staff.getY()+" phase="+boss.phase()+" clip="+boss.animation());helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=75)
    public static void broken_nullpoint_shields_until_fall_keyframe(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        NullpointStaff staff=boss.spawnNullpointForTest(helper.getLevel());staff.hurt(helper.getLevel().damageSources().generic(),400);
        helper.runAfterDelay(3,()->helper.assertTrue(!boss.hurt(helper.getLevel().damageSources().generic(),20),"Sorcerer shield dropped before falltransition frame 25"));
        helper.runAfterDelay(32,()->{helper.assertTrue(boss.hurt(helper.getLevel().damageSources().generic(),20),"Sorcerer stayed shielded after falltransition frame 25");helper.succeed();});
    }
    private static void fireSorcererEventForTest(NativeBoss boss,net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity target,SorcererTimeline.Event event){
        try{
            var fire=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,
                net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            fire.setAccessible(true);fire.invoke(boss,level,target,event);
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not drive the source-authored Sorcerer event",failure);}
    }
    @GameTest(template="empty",timeoutTicks=150,batch="sorcerer_meteor_clock")
    public static void meteor_tear_emits_six_single_shots_after_activation(GameTestHelper helper){
        clearEncounterTestPlayers((net.minecraft.server.level.ServerLevel)helper.getLevel());
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=encounterTestPlayer(helper,boss.position().add(8,0,0),net.minecraft.world.level.GameType.CREATIVE);
        boss.startSorcererAttackForTest(helper.getLevel(),target,"voidmeteor");boss.setTarget(null);
        fireSorcererEventForTest(boss,(net.minecraft.server.level.ServerLevel)helper.getLevel(),target,
            new SorcererTimeline.Event(10,SorcererTimeline.Action.METEOR_TEAR,null));
        helper.runAfterDelay(20,()->{
            helper.assertTrue(boss.meteorShotsEmitted()==0,"Tear fired before the source activation keyframe");
            fireSorcererEventForTest(boss,(net.minecraft.server.level.ServerLevel)helper.getLevel(),target,
                new SorcererTimeline.Event(30,SorcererTimeline.Action.METEOR_RAIN,"sorcerer.voidmeteor.tear"));
            helper.runAfterDelay(9,()->helper.assertTrue(boss.meteorShotsEmitted()==0,"First meteor fired before 20 source timer points (10 world ticks)"));
            helper.runAfterDelay(10,()->{
                helper.assertTrue(boss.meteorShotsEmitted()==1,"First source firing point did not emit exactly one meteor: "+boss.meteorShotsEmitted());
                var live=helper.getLevel().getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(64),bolt->bolt.getOwner()==boss&&bolt.effect()==SpellBolt.Effect.METEOR);
                helper.assertTrue(live.size()==1&&live.get(0).remainingLife()<=25,"Source firing point did not create one 25-tick homing meteor");
            });
            helper.runAfterDelay(20,()->helper.assertTrue(boss.meteorShotsEmitted()==2,"The second meteor must fire at 40 source timer points (20 world ticks)"));
            helper.runAfterDelay(59,()->helper.assertTrue(boss.meteorShotsEmitted()==5,"The fifth meteor must fire by 100 source timer points (50 world ticks)"));
            helper.runAfterDelay(60,()->{
                helper.assertTrue(boss.meteorShotCount()==6&&boss.meteorShotsEmitted()==6,
                    "Activated tear must emit one meteor at each 20 source timer points through 120 (10 world ticks each): "+boss.meteorShotsEmitted());
                helper.succeed();
            });
        });
    }
    @GameTest(template="empty",timeoutTicks=30,batch="sorcerer_meteor_impact")
    public static void meteor_passes_through_passthrough_blocks_and_impacts_players(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var player=helper.makeMockSurvivalPlayer();player.setNoGravity(true);player.setPos(helper.absolutePos(new BlockPos(8,3,4)).getCenter());helper.getLevel().addFreshEntity(player);float health=player.getHealth();
        BlockPos startBlock=helper.absolutePos(new BlockPos(4,3,4));
        helper.getLevel().setBlock(startBlock.offset(1,0,0),Blocks.WATER.defaultBlockState(),3);
        helper.getLevel().setBlock(startBlock.offset(2,0,0),Blocks.STONE.defaultBlockState(),3);
        SpellBolt bolt=SpellBolt.launchMeteor((net.minecraft.server.level.ServerLevel)helper.getLevel(),boss,startBlock.getCenter(),new Vec3(1,0,0),15);
        helper.runAfterDelay(1,()->{
            helper.assertTrue(bolt.isRemoved(),"Meteor did not stop at the first non-passthrough block");
            helper.assertTrue(player.getHealth()<health,"Meteor impact did not damage the player in its five-block radius");helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=35,batch="sorcerer_meteor_lifetime")
    public static void meteor_explodes_after_twenty_five_ticks(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        BlockPos startBlock=helper.absolutePos(new BlockPos(4,80,4));
        for(int dy=0;dy<=40;dy++){
            BlockPos clear=startBlock.offset(0,dy,0);helper.getLevel().getChunkAt(clear);
            helper.getLevel().setBlock(clear,Blocks.AIR.defaultBlockState(),3);
        }
        Vec3 start=startBlock.getCenter();
        SpellBolt emitted=SpellBolt.launchMeteor((net.minecraft.server.level.ServerLevel)helper.getLevel(),boss,start,new Vec3(0,1,0),15);
        CompoundTag saved=new CompoundTag();emitted.saveWithoutId(saved);emitted.discard();
        SpellBolt bolt=Content.BOLT.get().create(helper.getLevel());bolt.load(saved);helper.getLevel().addFreshEntity(bolt);
        helper.assertTrue(bolt.effect()==SpellBolt.Effect.METEOR&&bolt.remainingLife()==25&&bolt.getOwner()==boss,
            "Saving/reloading a meteor lost its 25-tick tracking state, type, or owner");
        helper.runAfterDelay(24,()->helper.assertTrue(!bolt.isRemoved()&&bolt.remainingLife()==1,
            "Meteor should have one tick left before its source 25-tick expiry: removed="+bolt.isRemoved()+", life="+bolt.remainingLife()+", tickCount="+bolt.tickCount));
        helper.runAfterDelay(25,()->{helper.assertTrue(bolt.isRemoved(),"Meteor did not explode on its 25th tick: life="+bolt.remainingLife()+", tickCount="+bolt.tickCount);helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=20,batch="sorcerer_meteor_homing")
    public static void meteor_turns_toward_the_players_current_position(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setNoAi(true);boss.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        BlockPos startBlock=helper.absolutePos(new BlockPos(4,80,4));
        for(int dx=-1;dx<=12;dx++)for(int dz=-1;dz<=12;dz++)for(int dy=-1;dy<=1;dy++){
            BlockPos clear=startBlock.offset(dx,dy,dz);helper.getLevel().getChunkAt(clear);helper.getLevel().setBlock(clear,Blocks.AIR.defaultBlockState(),3);
        }
        Vec3 start=startBlock.getCenter();var player=helper.makeMockSurvivalPlayer();player.setNoGravity(true);player.setPos(start.add(10,0,0));helper.getLevel().addFreshEntity(player);
        SpellBolt bolt=SpellBolt.launchMeteor((net.minecraft.server.level.ServerLevel)helper.getLevel(),boss,start,new Vec3(0,0,1),15);
        bolt.tick();Vec3 first=bolt.position().subtract(start);
        helper.assertTrue(first.x>1.49&&Math.abs(first.z)<.01,"First meteor substeps did not home toward the player: "+first);
        player.setPos(start.add(0,0,10));bolt.tick();Vec3 turned=bolt.getDeltaMovement().normalize();
        helper.assertTrue(turned.z>.9&&turned.x<0,"Meteor ignored the player's updated position: velocity="+turned);
        bolt.discard();player.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=10,batch="native_boss_attack_restore")
    public static void sorcerer_attack_identifiers_and_progress_survive_save_load(GameTestHelper helper){
        var level=helper.getLevel();var target=helper.spawn(EntityType.COW,new BlockPos(8,2,8));target.setNoAi(true);
        for(String clip:SorcererTimeline.attacks()){
            NativeBoss source=Content.SORCERER.get().create(level);source.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
            source.startSorcererAttackForTest(level,target,clip);
            CompoundTag saved=new CompoundTag();source.saveWithoutId(saved);
            int age=17;int expectedRemaining=Math.max(1,saved.getInt("Remaining")-age);
            saved.putInt("AttackAge",age);saved.putInt("AnimationAge",age);saved.putInt("Remaining",expectedRemaining);
            helper.assertTrue(clip.equals(saved.getString("ActiveSorcererAttack"))&&saved.hasUUID("SorcererTarget")
                    &&saved.getUUID("SorcererTarget").equals(target.getUUID()),
                "Save data lost the active Sorcerer attack id or selected target for "+clip+": "+saved);
            NativeBoss restored=Content.SORCERER.get().create(level);restored.load(saved);
            CompoundTag resumed=new CompoundTag();restored.saveWithoutId(resumed);
            helper.assertTrue(clip.equals(resumed.getString("ActiveSorcererAttack"))&&resumed.getInt("AttackAge")==age
                    &&resumed.getInt("Remaining")==expectedRemaining,
                "Sorcerer attack progress did not round-trip for "+clip+": "+resumed);
        }
        NativeBoss haloSource=Content.SORCERER.get().create(level);haloSource.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
        haloSource.startSorcererAttackForTest(level,target,"halobeam");
        try{
            var keyframe=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            keyframe.setAccessible(true);keyframe.invoke(haloSource,level,target,SorcererTimeline.at("halobeam",5).get(0));
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot inspect Halo Beam summon keyframe",exception);}
        CompoundTag haloSave=new CompoundTag();haloSource.saveWithoutId(haloSave);
        NativeBoss haloRestored=Content.SORCERER.get().create(level);haloRestored.load(haloSave);
        CompoundTag haloResumed=new CompoundTag();haloRestored.saveWithoutId(haloResumed);
        helper.assertTrue(haloSave.getCompound("SorcererHalos").contains("Halo0X")
                &&haloSave.getCompound("SorcererHalos").getDouble("Halo0X")==haloResumed.getCompound("SorcererHalos").getDouble("Halo0X")
                &&haloSave.getCompound("SorcererHalos").getDouble("Halo0Y")==haloResumed.getCompound("SorcererHalos").getDouble("Halo0Y")
                &&haloSave.getCompound("SorcererHalos").getDouble("Halo0Z")==haloResumed.getCompound("SorcererHalos").getDouble("Halo0Z"),
            "Halo Beam lost its already-summoned node position during save/reload");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=8,batch="native_boss_attack_restore")
    public static void sorcerer_void_meteor_resumes_at_next_unfired_keyframe(GameTestHelper helper){
        var level=helper.getLevel();var target=helper.makeMockSurvivalPlayer();target.setPos(helper.absolutePos(new BlockPos(9,2,8)).getCenter());level.addFreshEntity(target);
        NativeBoss source=Content.SORCERER.get().create(level);source.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
        source.startSorcererAttackForTest(level,target,"voidmeteor");
        try{
            var keyframe=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            keyframe.setAccessible(true);keyframe.invoke(source,level,target,SorcererTimeline.at("voidmeteor",10).get(0));
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot inspect Void Meteor tear keyframe",exception);}
        CompoundTag saved=new CompoundTag();source.saveWithoutId(saved);
        saved.putInt("AttackAge",29);saved.putInt("AnimationAge",29);saved.putInt("Remaining",38);saved.putUUID("SorcererTarget",target.getUUID());
        NativeBoss restored=Content.SORCERER.get().create(level);restored.load(saved);restored.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
        level.addFreshEntity(restored);keepBossLoadedForTest(helper,restored);
        helper.runAfterDelay(1,()->{
            CompoundTag atTear=new CompoundTag();restored.saveWithoutId(atTear);
            helper.assertTrue(restored.getTarget()==target,
                "Reload did not resolve the Sorcerer's saved target UUID to the live entity");
            helper.assertTrue(restored.animation().equals("voidmeteor")&&atTear.getInt("AttackAge")==30
                    &&atTear.getInt("MeteorTearAge")==0,
                "Reload skipped the source's tick-30 Void Meteor rain keyframe: "+atTear);
        });
        helper.runAfterDelay(2,()->{
            CompoundTag afterTear=new CompoundTag();restored.saveWithoutId(afterTear);
            helper.assertTrue(afterTear.getInt("AttackAge")==31&&afterTear.getInt("MeteorTearAge")==1,
                "Reload replayed the already-fired Void Meteor tear keyframe: "+afterTear);
            helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=8,batch="native_boss_attack_restore")
    public static void sorcerer_shardhail_resumes_without_restarting_persistent_wave(GameTestHelper helper){
        var level=helper.getLevel();var target=helper.makeMockSurvivalPlayer();target.setPos(helper.absolutePos(new BlockPos(9,2,8)).getCenter());level.addFreshEntity(target);
        NativeBoss source=Content.SORCERER.get().create(level);source.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
        source.startSorcererAttackForTest(level,target,"shardhail");
        CompoundTag saved=new CompoundTag();source.saveWithoutId(saved);
        saved.putInt("AttackAge",35);saved.putInt("AnimationAge",35);saved.putInt("Remaining",29);saved.putUUID("SorcererTarget",target.getUUID());
        saved.putInt("HailAge",10);saved.putDouble("HailCenterX",120);saved.putDouble("HailCenterY",90);saved.putDouble("HailCenterZ",80);
        int[] delay=new int[18];java.util.Arrays.fill(delay,100);saved.putIntArray("HailDelay",delay);
        NativeBoss restored=Content.SORCERER.get().create(level);restored.load(saved);restored.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
        level.addFreshEntity(restored);keepBossLoadedForTest(helper,restored);
        helper.runAfterDelay(1,()->{
            CompoundTag resumed=new CompoundTag();restored.saveWithoutId(resumed);
            helper.assertTrue(resumed.getString("ActiveSorcererAttack").equals("shardhail")
                    &&resumed.getInt("AttackAge")==36&&resumed.getInt("HailAge")==11
                    &&resumed.getIntArray("HailDelay").length==18,
                "Reload restarted or discarded the active Shard Hail wave: "+resumed);
            helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=8,batch="native_boss_attack_restore")
    public static void death_walker_slam_resumes_before_damage_frame(GameTestHelper helper){
        var level=helper.getLevel();var player=helper.makeMockSurvivalPlayer();player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(4,2,7)).getCenter());level.addFreshEntity(player);float health=player.getHealth();
        NativeBoss source=Content.DEATH_WALKER.get().create(level);source.setPos(helper.absolutePos(new BlockPos(4,2,3)).getCenter());source.setYRot(0);source.setNoGravity(true);
        CompoundTag saved=new CompoundTag();source.saveWithoutId(saved);saved.putString("Animation","slam");saved.putInt("Remaining",42);
        saved.putInt("AnimationAge",49);saved.putInt("OrdinaryState",3);
        source.discard();
        NativeBoss walker=Content.DEATH_WALKER.get().create(level);walker.load(saved);
        walker.setPos(helper.absolutePos(new BlockPos(4,2,3)).getCenter());walker.setYRot(0);walker.setNoGravity(true);level.addFreshEntity(walker);
        helper.runAfterDelay(1,()->{
            CompoundTag resumed=new CompoundTag();walker.saveWithoutId(resumed);
            helper.assertTrue(walker.animation().equals("slam")&&resumed.getInt("Remaining")==41&&player.getHealth()<health,
                "Death Walker lost the active slam or skipped its next damage frame: "+resumed+", playerHealth="+player.getHealth());
            helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=8,batch="native_boss_attack_restore")
    public static void death_walker_slam_saved_after_impact_does_not_replay_it(GameTestHelper helper){
        var level=helper.getLevel();var player=helper.makeMockSurvivalPlayer();player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(4,2,7)).getCenter());level.addFreshEntity(player);float health=player.getHealth();
        NativeBoss source=Content.DEATH_WALKER.get().create(level);source.setPos(helper.absolutePos(new BlockPos(4,2,3)).getCenter());source.setYRot(0);source.setNoGravity(true);
        CompoundTag saved=new CompoundTag();source.saveWithoutId(saved);saved.putString("Animation","slam");saved.putInt("Remaining",41);
        saved.putInt("AnimationAge",50);saved.putInt("OrdinaryState",3);
        source.discard();
        NativeBoss walker=Content.DEATH_WALKER.get().create(level);walker.load(saved);
        walker.setPos(helper.absolutePos(new BlockPos(4,2,3)).getCenter());walker.setYRot(0);walker.setNoGravity(true);level.addFreshEntity(walker);
        helper.runAfterDelay(1,()->{
            CompoundTag resumed=new CompoundTag();walker.saveWithoutId(resumed);
            helper.assertTrue(walker.animation().equals("slam")&&resumed.getInt("Remaining")==40&&player.getHealth()==health,
                "Death Walker replayed a slam that had already crossed its source damage frame: "+resumed+", playerHealth="+player.getHealth());
            helper.succeed();
        });
    }
    @GameTest(template="empty")
    public static void sorcerer_attack_openers_do_not_debuff_the_locked_target(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(7,2,7));target.setNoAi(true);
        for(String clip:new String[]{"shardhail","frozenblades","singularitycharge","dashstrike","voidwave","javelinswing","airdash","icespikes","seekingsingularities","voidmeteor"}){
            boss.startSorcererAttackForTest(helper.getLevel(),target,clip);
            helper.assertTrue(!target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    &&!target.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),
                "Sorcerer attack opener applied a debuff before a source hit event: "+clip);
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void singularity_charge_spawns_independent_bomb_not_seeking_orb(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=emissaryPlayer(helper,helper.absolutePos(new BlockPos(10,2,10)).getCenter());boss.setTarget(target);
        boss.startSorcererAttackForTest(helper.getLevel(),target,"singularitycharge");
        try{
            var fire=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            fire.setAccessible(true);
            fire.invoke(boss,helper.getLevel(),target,SorcererTimeline.at("singularitycharge",44).get(0));
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot inspect Singularity Charge keyframe",exception);}
        var bolts=helper.getLevel().getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(4));
        helper.assertTrue(bolts.stream().anyMatch(bolt->boss.getUUID().equals(bolt.getOwner()==null?null:bolt.getOwner().getUUID())
            &&bolt.effect()==SpellBolt.Effect.DAMAGE&&Math.abs(bolt.getDeltaMovement().length()-.3)<.001),
            "Frame 44 must spawn an independent 0.3-block-per-tick bomb, not a Seeking Singularities orb");target.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void sorcerer_void_beam_stops_at_nonpassthrough_wall(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,3,2)).getCenter());helper.getLevel().addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(4,3,10));target.setNoAi(true);boss.setTarget(target);
        // Death Walker is twelve blocks tall; cover its entire visible body.
        for(int y=3;y<=16;y++)helper.setBlock(new BlockPos(4,y,7),net.minecraft.world.level.block.Blocks.STONE);
        boss.startSorcererAttackForTest(helper.getLevel(),target,"voidbeam");
        float healthBefore=target.getHealth();
        try{
            var fire=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            fire.setAccessible(true);
            fire.invoke(boss,helper.getLevel(),target,SorcererTimeline.at("voidbeam",30).get(0));
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot inspect Void Beam keyframe",exception);}
        helper.assertTrue(target.getHealth()==healthBefore,"Void Beam damaged a mod creature behind a nonpassthrough solid wall; before="+healthBefore+" after="+target.getHealth()+" boss="+boss.position()+" target="+target.position()+" wall="+helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(4,4,7))));helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=115)
    public static void serpent_death_waits_for_source_100_tick_sequence(GameTestHelper helper){
        var serpent=helper.spawn(Content.SERPENT.get(),new BlockPos(4,3,4));keepBossLoadedForTest(helper,serpent);serpent.setNoAi(true);serpent.setNoGravity(true);serpent.hurt(helper.getLevel().damageSources().genericKill(),2000);
        helper.runAfterDelay(70,()->helper.assertTrue(!serpent.isRemoved(),"Serpent death sequence was cut short"));
        helper.runAfterDelay(105,()->{helper.assertTrue(serpent.isRemoved(),"Serpent remained after its source 100-tick defeat sequence");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void boss_defeat_rewards_participant_lore_and_advancement(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SorcererTester"));helper.getLevel().addNewPlayer(player);
        player.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,2)).getCenter());boss.setHealth(5);helper.getLevel().addFreshEntity(boss);
        helper.assertTrue(Combat.hit(player,boss,10,Combat.Element.VOID),"Participant's killing blow was rejected");
        helper.assertTrue(boss.participantCount()==1,"Sorcerer did not record its participant");
        helper.runAfterDelay(140,()->helper.assertTrue(!boss.isRemoved(),"Participant reward must wait for source death180"));
        helper.runAfterDelay(185,()->{
            helper.assertTrue(player.getInventory().countItem(Content.get("adherent_sorcerer"))==1,"Sorcerer participant did not receive Adherent II");
            var advancement=helper.getLevel().getServer().getAdvancements().getAdvancement(new net.minecraft.resources.ResourceLocation(NativeMod.ID,"bosses/defeat_sorcerer"));
            helper.assertTrue(advancement!=null,"Sorcerer advancement JSON did not load");
            // Forge deliberately suppresses persistent advancement progress for FakePlayer;
            // loading the impossible-trigger advancement plus exercising the same award path is the server-safe regression here.
            player.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void homing_boss_projectile_tracks_registered_mod_entity(GameTestHelper helper){
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(1,2,1));owner.setNoAi(true);owner.setNoGravity(true);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(6,2,6));target.setNoAi(true);target.setNoGravity(true);
        owner.setTarget(target);
        SpellBolt.launch(helper.getLevel(),owner,owner.getEyePosition(),new Vec3(0,0,1),13,Combat.Element.VOID,.55f,1)
            .effect(SpellBolt.Effect.HOMING,70).homeOn(target);
        helper.runAfterDelay(45,()->{helper.assertTrue(target.getHealth()<target.getMaxHealth(),"Homing Sorcerer projectile missed the registered mod entity");helper.succeed();});
    }
    @GameTest(template="empty",batch="spell_collision",timeoutTicks=20)
    public static void source_passthrough_policy_uses_tagged_voxels_instead_of_physical_shapes(GameTestHelper helper){
        var level=helper.getLevel();BlockPos base=helper.absolutePos(new BlockPos(2,240,2));
        var sourcePassable=java.util.List.of(Blocks.AIR,Blocks.CAVE_AIR,Blocks.VOID_AIR,Blocks.WATER,Blocks.LAVA,Blocks.FIRE,
            Blocks.SNOW,Blocks.GRASS,Blocks.TALL_GRASS,Blocks.SCULK_VEIN,Blocks.LIGHT);
        for(int i=0;i<sourcePassable.size();i++){
            BlockPos pos=base.offset(i*2,0,0);level.setBlock(pos,sourcePassable.get(i).defaultBlockState(),2);
            helper.assertTrue(SpellCollisionPolicy.isSourcePassthrough(level.getBlockState(pos)),
                "Imported source passthrough tag did not include "+sourcePassable.get(i)+" state="+level.getBlockState(pos));
            var sweep=SpellCollisionPolicy.firstNonPassthroughVoxel(level,pos.getCenter().add(-.6,0,0),pos.getCenter().add(.6,0,0));
            helper.assertTrue(sweep.hit()==null&&!sweep.unloadedBoundary(),
                "Source-tagged passthrough block collided despite being listed: "+sourcePassable.get(i)+" at "+pos
                    +" hit="+(sweep.hit()==null?"none":sweep.hit().getBlockPos()+" "+level.getBlockState(sweep.hit().getBlockPos())
                    +" tagged="+SpellCollisionPolicy.isSourcePassthrough(level.getBlockState(sweep.hit().getBlockPos())))
                    +" start="+pos.getCenter().add(-.6,0,0));
        }
        BlockPos unlistedPlant=base.offset(0,0,3);level.setBlock(unlistedPlant,Blocks.SEAGRASS.defaultBlockState(),2);
        var plantSweep=SpellCollisionPolicy.firstNonPassthroughVoxel(level,unlistedPlant.getCenter().add(-.6,0,0),unlistedPlant.getCenter().add(.6,0,0));
        helper.assertTrue(plantSweep.hit()!=null&&plantSweep.hit().getBlockPos().equals(unlistedPlant),
            "An unlisted no-collision plant passed through because physical shape was empty");
        BlockPos solid=base.offset(0,0,5);level.setBlock(solid,Blocks.STONE.defaultBlockState(),2);
        var solidSweep=SpellCollisionPolicy.firstNonPassthroughVoxel(level,solid.getCenter().add(-.6,0,0),solid.getCenter().add(.6,0,0));
        helper.assertTrue(solidSweep.hit()!=null&&solidSweep.hit().getBlockPos().equals(solid),"A full untagged block was not a source collision");
        helper.succeed();
    }
    @GameTest(template="empty",batch="spell_collision",timeoutTicks=20)
    public static void source_passthrough_sweep_catches_fast_wall_crossings_and_diagonal_corners(GameTestHelper helper){
        var level=helper.getLevel();BlockPos base=helper.absolutePos(new BlockPos(2,240,2));
        BlockPos wall=base.offset(3,0,0);level.setBlock(wall,Blocks.STONE.defaultBlockState(),2);
        var owner=EntityType.COW.create(level);owner.setNoAi(true);owner.setPos(base.getX()-8,base.getY(),base.getZ());level.addFreshEntity(owner);
        Vec3 start=base.getCenter().add(.1,0,0);
        SpellBolt fast=SpellBolt.launch((net.minecraft.server.level.ServerLevel)level,owner,start,new Vec3(1,0,0),0,Combat.Element.DREAM,5,0)
            .collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        fast.tick();
        helper.assertTrue(fast.isRemoved()&&fast.getX()<wall.getX()+.001,
            "A fast projectile crossed an untagged wall within one tick; removed="+fast.isRemoved()+" position="+fast.position()+" wall="+wall);

        BlockPos cornerBase=base.offset(0,0,8);BlockPos cornerWall=cornerBase.offset(1,0,0);
        level.setBlock(cornerWall,Blocks.STONE.defaultBlockState(),2);
        var corner=SpellCollisionPolicy.firstNonPassthroughVoxel(level,cornerBase.getCenter(),cornerBase.getCenter().add(2,0,2));
        helper.assertTrue(corner.hit()!=null&&corner.hit().getBlockPos().equals(cornerWall),
            "Supercover traversal skipped an untagged block touched at a diagonal corner: "+corner.hit());
        Vec3 topStart=new Vec3(base.getX()+.5,level.getMaxBuildHeight()-.5,base.getZ()+.5);
        var worldTop=SpellCollisionPolicy.firstNonPassthroughVoxel(level,topStart,topStart.add(0,1,0));
        helper.assertTrue(worldTop.hit()!=null&&worldTop.hit().getBlockPos().getY()==level.getMaxBuildHeight(),
            "Voxel traversal did not stop at the dimension build-height boundary: "+worldTop);
        Vec3 boundaryStart=start.add(0,0,16),farEnd=boundaryStart.add(1000,0,0);BlockPos farBlock=BlockPos.containing(farEnd);
        helper.assertTrue(!level.hasChunkAt(farBlock),"Chunk-boundary fixture endpoint was unexpectedly loaded: "+farBlock);
        var unloaded=SpellCollisionPolicy.firstNonPassthroughVoxel(level,boundaryStart,farEnd);
        helper.assertTrue(unloaded.hit()==null&&unloaded.unloadedBoundary(),
            "Voxel traversal continued through an unloaded chunk instead of stopping safely: "+unloaded
                +(unloaded.hit()==null?"":" at "+unloaded.hit().getBlockPos()+" "+level.getBlockState(unloaded.hit().getBlockPos())));
        owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="spell_collision",timeoutTicks=20)
    public static void source_passthrough_policy_keeps_forge_projectile_impact_cancellation(GameTestHelper helper){
        var level=helper.getLevel();BlockPos base=helper.absolutePos(new BlockPos(2,240,2));
        level.setBlock(base.offset(2,0,0),Blocks.STONE.defaultBlockState(),2);
        var owner=EntityType.COW.create(level);owner.setNoAi(true);owner.setPos(base.getX()-8,base.getY(),base.getZ());level.addFreshEntity(owner);
        SpellBolt bolt=SpellBolt.launch((net.minecraft.server.level.ServerLevel)level,owner,base.getCenter(),new Vec3(1,0,0),0,Combat.Element.DREAM,4,0)
            .collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        cancelProjectileImpactForTest=bolt.getUUID();cancelledProjectileImpactsForTest=0;
        try{bolt.tick();}finally{cancelProjectileImpactForTest=null;}
        helper.assertTrue(cancelledProjectileImpactsForTest==1&&!bolt.isRemoved()&&bolt.getX()>base.getX()+2,
            "Forge cancellation was not honored for a source-tag block impact; cancelled="+cancelledProjectileImpactsForTest+" removed="+bolt.isRemoved()+" position="+bolt.position());
        owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="spell_collision",timeoutTicks=20)
    public static void spell_collision_policy_survives_projectile_save_and_load(GameTestHelper helper){
        var level=helper.getLevel();var owner=EntityType.COW.create(level);owner.setNoAi(true);owner.setPos(helper.absolutePos(new BlockPos(1,240,1)).getCenter());level.addFreshEntity(owner);
        SpellBolt original=SpellBolt.launch((net.minecraft.server.level.ServerLevel)level,owner,owner.position().add(0,2,0),new Vec3(1,0,0),1,Combat.Element.DREAM,1,0)
            .collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        CompoundTag saved=new CompoundTag();original.saveWithoutId(saved);
        SpellBolt restored=Content.BOLT.get().create(level);restored.load(saved);
        helper.assertTrue(restored.collisionPolicyForTest()==SpellCollisionPolicy.SOURCE_PASSTHROUGH,
            "A saved source-policy projectile reloaded with a different collision rule");
        original.discard();restored.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="seeking_singularities",timeoutTicks=30)
    public static void seeking_singularities_reselect_nearest_player_after_targets_move(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(30,0,30));level.addFreshEntity(owner);
        var first=encounterTestPlayer(helper,origin.add(0,-1,8),net.minecraft.world.level.GameType.SURVIVAL);
        var nearer=encounterTestPlayer(helper,origin.add(0,-1,14),net.minecraft.world.level.GameType.SURVIVAL);
        SpellBolt bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),13,Combat.Element.VOID,.24f,1)
            .seekingSingularity(60);
        helper.runAfterDelay(1,()->{
            first.moveTo(origin.x,origin.y-1,origin.z+32,0,0);nearer.moveTo(origin.x-4,origin.y-1,origin.z,0,0);
        });
        helper.runAfterDelay(3,()->{
            helper.assertTrue(bolt.getDeltaMovement().x<-.005,"Orb kept its original locked target instead of selecting the nearer player: "+bolt.getDeltaMovement());
            helper.assertTrue(Math.abs(bolt.getDeltaMovement().length()-.24)<.0001,"Seeking orb speed left the source 0.24-block tick step: "+bolt.getDeltaMovement().length());
            bolt.discard();owner.discard();first.discard();nearer.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="seeking_singularities",timeoutTicks=10)
    public static void seeking_singularities_selects_player_at_inclusive_fifty_block_boundary(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-8,0,0));level.addFreshEntity(owner);
        var edge=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SeekingEdge"));
        edge.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);edge.setNoGravity(true);edge.setPos(origin.add(30,0,40));level.addNewPlayer(edge);
        SpellBolt bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),13,Combat.Element.VOID,.24f,1).seekingSingularity(10);
        bolt.tick();
        helper.assertTrue(bolt.getDeltaMovement().x>.01,"Nearest-player selector must include a player exactly 50 blocks away: "+bolt.getDeltaMovement());
        bolt.discard();owner.discard();edge.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="seeking_singularities",timeoutTicks=20)
    public static void seeking_singularities_attack_emits_eight_orbs_with_source_lifetimes(GameTestHelper helper){
        var level=helper.getLevel();NativeBoss boss=Content.SORCERER.get().create(level);boss.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(10,2,10));target.setNoAi(true);boss.setTarget(target);
        boss.startSorcererAttackForTest(level,target,"seekingsingularities");
        try{
            var fire=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            fire.setAccessible(true);fire.invoke(boss,level,target,SorcererTimeline.at("seekingsingularities",30).get(0));
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot inspect Seeking Singularities summon frame",exception);}
        var bolts=level.getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(8),bolt->bolt.getOwner()==boss&&bolt.effect()==SpellBolt.Effect.SEEKING_SINGULARITY);
        helper.assertTrue(bolts.size()==8,"Seeking Singularities must launch exactly eight source orbs; found="+bolts.size());
        helper.assertTrue(bolts.stream().allMatch(bolt->Math.abs(bolt.getDeltaMovement().length()-.24)<.0001&&bolt.remainingLife()>=70&&bolt.remainingLife()<=89),
            "Orb speed/lifetime left the source ranges: "+bolts.stream().map(bolt->bolt.getDeltaMovement().length()+"/"+bolt.remainingLife()).toList());helper.succeed();
    }
    @GameTest(template="empty",batch="seeking_singularities",timeoutTicks=10)
    public static void seeking_singularities_expire_without_damaging_nearby_mobs(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-8,0,0));level.addFreshEntity(owner);
        var nearby=EntityType.COW.create(level);nearby.setNoAi(true);nearby.setPos(origin.add(0,0,1));level.addFreshEntity(nearby);float health=nearby.getHealth();
        SpellBolt bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),13,Combat.Element.VOID,.24f,1).seekingSingularity(1);
        helper.runAfterDelay(3,()->{
            helper.assertTrue(bolt.isRemoved()&&nearby.getHealth()==health,"Lifetime expiry must clean the orb without turning into a blast");owner.discard();nearby.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",batch="seeking_singularities",timeoutTicks=25)
    public static void seeking_singularities_apply_source_slowdown_even_when_damage_is_rejected(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-8,0,0));level.addFreshEntity(owner);
        var target=emissaryPlayer(helper,origin.add(0,0,2.5));target.setInvulnerable(true);float health=target.getHealth();
        SpellBolt bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),13,Combat.Element.VOID,.24f,1).seekingSingularity(30);
        for(int tick=0;tick<25&&!bolt.isRemoved();tick++)bolt.tick();
        var slowdown=target.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(target.getHealth()==health&&slowdown!=null&&slowdown.getAmplifier()==1&&slowdown.getDuration()>=100,
            "Source selector contact must apply hidden Slowness II for five seconds even when target damage is rejected; health="+health+"→"+target.getHealth()+", effect="+slowdown);
        bolt.discard();owner.discard();target.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="seeking_singularities",timeoutTicks=45)
    public static void seeking_singularities_pass_through_solid_blocks_and_hit_the_player_behind_them(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-8,0,0));level.addFreshEntity(owner);
        // Forge FakePlayer is damage-immune; use the GameTest survival player for the impact assertion.
        var target=emissaryPlayer(helper,origin.add(0,-1,3));float health=target.getHealth();
        BlockPos wall=BlockPos.containing(origin.add(0,0,1));level.setBlock(wall,Blocks.OBSIDIAN.defaultBlockState(),3);
        SpellBolt bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),13,Combat.Element.VOID,.24f,1)
            .seekingSingularity(40);
        for(int tick=0;tick<40&&!bolt.isRemoved();tick++)bolt.tick();
        AABB hitBox=new AABB(bolt.getX()-.25,bolt.getY()-.25,bolt.getZ()-.25,bolt.getX()+.25,bolt.getY()+.25,bolt.getZ()+.25);
        helper.assertTrue(target.getHealth()<health,"Orb missed the player behind the obsidian wall: target="+target.position()+", health="+target.getHealth()+"/"+health+", bolt="+bolt.position()+", life="+bolt.remainingLife()+", removed="+bolt.isRemoved()+", hitBoxIntersects="+target.getBoundingBox().intersects(hitBox)+", canHit="+Combat.canHit(owner,target)+", loaded="+level.hasChunkAt(bolt.blockPosition()));
        helper.assertTrue(level.getBlockState(wall).is(Blocks.OBSIDIAN),"Seeking orb must pass through without changing the wall");
        owner.discard();target.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="seeking_singularities",timeoutTicks=35)
    public static void seeking_singularities_reflected_projectile_keeps_ignoring_blocks(GameTestHelper helper){
        var level=helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,4,4)).getCenter();
        var reflector=emissaryPlayer(helper,origin);var target=emissaryPlayer(helper,origin.add(0,0,-3));float health=target.getHealth();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-8,0,0));level.addFreshEntity(owner);
        BlockPos wall=BlockPos.containing(origin.add(0,0,-1));level.setBlock(wall,Blocks.OBSIDIAN.defaultBlockState(),3);
        SpellBolt bolt=SpellBolt.launch(level,owner,origin,new Vec3(0,0,1),13,Combat.Element.VOID,.24f,1).seekingSingularity(40);
        bolt.reflectFrom(reflector,new Vec3(0,0,-1));
        for(int tick=0;tick<30&&!bolt.isRemoved();tick++)bolt.tick();
        helper.assertTrue(bolt.isRemoved()&&target.getHealth()<health&&level.getBlockState(wall).is(Blocks.OBSIDIAN),
            "A reflected seeking orb must become a block-ignoring player projectile and pass the wall; health="+health+"→"+target.getHealth()+", bolt="+bolt.position()+", effect="+bolt.effect());
        reflector.discard();target.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="sorcerer_hit_origins",timeoutTicks=10)
    public static void void_wave_uses_world_y_plus_two_and_player_feet_sphere(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 root=helper.absolutePos(new BlockPos(4,3,4)).getCenter();
        NativeBoss boss=Content.SORCERER.get().create(level);boss.setPos(root);boss.setNoAi(true);boss.setNoGravity(true);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=EntityType.ZOMBIE.create(level);target.setNoAi(true);target.setNoGravity(true);target.setPos(root.add(0,0,32));level.addFreshEntity(target);
        var edge=emissaryPlayer(helper,root.add(0,8,0));float edgeHealth=edge.getHealth();
        var outside=emissaryPlayer(helper,root.add(0,-4.1,0));float outsideHealth=outside.getHealth();
        var creature=EntityType.COW.create(level);creature.setNoAi(true);creature.setNoGravity(true);creature.setPos(root.add(0,2,3));level.addFreshEntity(creature);float creatureHealth=creature.getHealth();
        fireSorcererSourceFrame(helper,boss,target,"voidwave",38);
        helper.assertTrue(edge.getHealth()<edgeHealth,"Void Wave missed a player whose feet are exactly six blocks from the root+world-Y2 origin");
        helper.assertTrue(outside.getHealth()==outsideHealth,"Void Wave included a player's body although their feet are 6.1 blocks outside the source sphere");
        helper.assertTrue(creature.getHealth()==creatureHealth,"Void Wave's @a source selector must not damage non-player creatures");
        boss.discard();target.discard();edge.discard();outside.discard();creature.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="sorcerer_hit_origins",timeoutTicks=10)
    public static void javelin_swing_uses_rotated_local_up_forward_and_player_feet_sphere(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 root=helper.absolutePos(new BlockPos(4,3,4)).getCenter();
        NativeBoss boss=Content.SORCERER.get().create(level);boss.setPos(root);boss.setNoAi(true);boss.setNoGravity(true);boss.setYRot(90);boss.setXRot(30);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=EntityType.ZOMBIE.create(level);target.setNoAi(true);target.setNoGravity(true);target.setPos(root.add(0,0,32));level.addFreshEntity(target);
        double yaw=Math.toRadians(boss.getYRot()),pitch=Math.toRadians(boss.getXRot());
        Vec3 localUp=new Vec3(-Math.sin(yaw)*Math.sin(pitch),Math.cos(pitch),Math.cos(yaw)*Math.sin(pitch));
        Vec3 source=boss.position().add(localUp).add(boss.getLookAngle().scale(2));
        var near=emissaryPlayer(helper,source.add(0,2.9,0));float nearHealth=near.getHealth();
        var edge=emissaryPlayer(helper,source.add(3,0,0));float edgeHealth=edge.getHealth();
        var outside=emissaryPlayer(helper,source.add(3.2,0,0));float outsideHealth=outside.getHealth();
        var creature=EntityType.COW.create(level);creature.setNoAi(true);creature.setNoGravity(true);creature.setPos(source);level.addFreshEntity(creature);float creatureHealth=creature.getHealth();
        fireSorcererSourceFrame(helper,boss,target,"javelinswing",35);
        helper.assertTrue(near.getHealth()<nearHealth,"Javelin Swing missed a player inside the pitch/yaw-rotated local ^ ^1 ^2 sphere");
        helper.assertTrue(edge.getHealth()<edgeHealth,"Javelin Swing must include a player at the inclusive three-block feet boundary");
        helper.assertTrue(outside.getHealth()==outsideHealth,"Javelin Swing included a player's body beyond the three-block feet sphere");
        helper.assertTrue(creature.getHealth()==creatureHealth,"Javelin Swing's @a source selector must not damage non-player creatures");
        boss.discard();target.discard();near.discard();edge.discard();outside.discard();creature.discard();helper.succeed();
    }
    private static void fireSorcererSourceFrame(GameTestHelper helper,NativeBoss boss,net.minecraft.world.entity.LivingEntity target,String clip,int tick){
        boss.startSorcererAttackForTest((net.minecraft.server.level.ServerLevel)helper.getLevel(),target,clip);
        try{
            var fire=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            fire.setAccessible(true);fire.invoke(boss,helper.getLevel(),target,SorcererTimeline.at(clip,tick).get(0));
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot inspect Sorcerer source hit frame "+clip+"/"+tick,exception);}
    }
    @GameTest(template="empty",batch="semblance_swing_samples",timeoutTicks=10)
    public static void semblance_ground_swing_samples_full_ring_not_a_forward_cone(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 root=helper.absolutePos(new BlockPos(4,3,4)).getCenter();
        var attacker=emissaryPlayer(helper,root);attacker.setYRot(90);attacker.setXRot(0);attacker.setOnGround(true);
        var rear=semblanceTestCow(level,root.add(2,1.5,0),200);float rearHealth=rear.getHealth();
        var frontBeyondRing=semblanceTestCow(level,root.add(-3.5,1.5,0),200);float frontHealth=frontBeyondRing.getHealth();
        var sideCancelled=semblanceTestCow(level,root.add(-.13261371670342254,1.5,1.9955985573612005),200);
        sideCancelled.setInvulnerable(true);float cancelledHealth=sideCancelled.getHealth();
        var excludedStand=EntityType.ARMOR_STAND.create(level);excludedStand.setNoGravity(true);excludedStand.setPos(sideCancelled.position());level.addFreshEntity(excludedStand);
        fireSemblanceSwing(attacker,level);
        helper.assertTrue(rear.getHealth()<rearHealth,"Grounded Semblance missed its source radius-two ring behind the attacker");
        helper.assertTrue(frontBeyondRing.getHealth()==frontHealth,"Grounded Semblance hit a target 3.5 blocks forward outside every source sample cube");
        helper.assertTrue(sideCancelled.getHealth()==cancelledHealth&&sideCancelled.getRemainingFireTicks()>0,
            "A source-selected Semblance target must receive Fire=100 ticks even when Forge rejects damage");
        helper.assertTrue(excludedStand.getRemainingFireTicks()<=0,
            "Semblance must honor the source livingthings entity type tag rather than select all LivingEntity types");
        attacker.discard();rear.discard();frontBeyondRing.discard();sideCancelled.discard();excludedStand.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="semblance_swing_samples",timeoutTicks=10)
    public static void semblance_air_swing_uses_rotated_vertical_ring_not_ground_plane(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 root=helper.absolutePos(new BlockPos(4,3,4)).getCenter();
        var attacker=emissaryPlayer(helper,root);attacker.setYRot(45);attacker.setXRot(30);attacker.setOnGround(false);
        double yaw=Math.toRadians(attacker.getYRot()),pitch=Math.toRadians(attacker.getXRot());
        Vec3 up=new Vec3(-Math.sin(yaw)*Math.sin(pitch),Math.cos(pitch),Math.cos(yaw)*Math.sin(pitch));
        Vec3 forward=attacker.getLookAngle();double ringSin=1.9955985573612005,ringForward=.13261371670342254;
        Vec3 airborneSample=root.add(0,.9,0).add(up.scale(ringSin)).add(forward.scale(ringForward));
        var overhead=semblanceTestCow(level,airborneSample.add(0,-.1,0),200);overhead.setInvulnerable(true);float overheadHealth=overhead.getHealth();
        Vec3 groundedOnly=root.add(0,.9,0).add(new Vec3(Math.cos(yaw),0,Math.sin(yaw)).scale(2));
        var lateral=semblanceTestCow(level,groundedOnly,200);float lateralHealth=lateral.getHealth();
        fireSemblanceSwing(attacker,level);
        helper.assertTrue(overhead.getHealth()==overheadHealth&&overhead.getRemainingFireTicks()>0,
            "Airborne Semblance must select the yaw/pitch-rotated vertical radius-two sample and burn despite rejected damage");
        helper.assertTrue(lateral.getHealth()==lateralHealth,
            "Airborne Semblance damaged a ground-plane-only target: before="+lateralHealth+", after="+lateral.getHealth()
                +", attackerYaw="+attacker.getYRot()+", attackerPitch="+attacker.getXRot()+", target="+lateral.position());
        helper.assertTrue(lateral.getRemainingFireTicks()<=0,
            "Airborne Semblance burned a ground-plane-only target: fire="+lateral.getRemainingFireTicks()
                +", attackerYaw="+attacker.getYRot()+", attackerPitch="+attacker.getXRot()+", target="+lateral.position());
        attacker.discard();overhead.discard();lateral.discard();helper.succeed();
    }
    private static net.minecraft.world.entity.animal.Cow semblanceTestCow(net.minecraft.server.level.ServerLevel level,Vec3 at,float health){
        var cow=EntityType.COW.create(level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(at);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);cow.setHealth(health);level.addFreshEntity(cow);return cow;
    }
    private static void fireSemblanceSwing(net.minecraft.world.entity.player.Player attacker,net.minecraft.server.level.ServerLevel level){
        attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Content.get("semblanceofdoom")));
        Content.get("semblanceofdoom").use(level,attacker,net.minecraft.world.InteractionHand.MAIN_HAND);
    }
    @GameTest(template="empty")
    public static void ash_loot_modifier_is_natively_registered(GameTestHelper helper){
        helper.assertTrue(Content.ASH_LOOT.isPresent()&&Content.ASH_LOOT.get()==NyctocosmicAshLoot.CODEC,"Nyctocosmic Ash loot modifier missing");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void sorcerer_death_animation_is_not_cut_off(GameTestHelper helper){
        var boss=helper.spawn(Content.SORCERER.get(),new BlockPos(3,2,3));keepBossLoadedForTest(helper,boss);boss.setNoAi(true);boss.setNoGravity(true);
        boss.hurt(helper.getLevel().damageSources().genericKill(),2000);
        helper.runAfterDelay(70,()->helper.assertTrue(!boss.isRemoved()&&boss.animation().equals("die"),"Sorcerer death clip was cut off at 60 ticks"));
        helper.runAfterDelay(138,()->helper.assertTrue(!boss.isRemoved()&&boss.animation().equals("die"),"Source death hold after frame133 was cut off"));
        helper.runAfterDelay(185,()->{helper.assertTrue(boss.isRemoved(),"Dead sorcerer was not removed after source180");helper.succeed();});
    }
    @GameTest(template="empty")
    public static void bone_item_rotation_happens_after_exported_transform(GameTestHelper helper){
        var bone=new org.joml.Matrix4f().translation(2,5,-3).rotateX(.4f).rotateZ(.7f);
        var source=new org.joml.Matrix4f(bone).rotateY((float)Math.PI);
        helper.assertTrue(source.equals(RigTransforms.itemTransform(bone),.00001f),"ItemDisplay's local half-turn is missing or applied before the bone");helper.succeed();
    }
    @GameTest(template="empty")
    public static void native_rig_orientation_matches_original_display(GameTestHelper helper){
        var display=EntityType.ITEM_DISPLAY.create(helper.getLevel());
        for(float yaw:new float[]{0,45,90,180,-90}){
            display.setYRot(yaw);
            var reference=display.orientation().transform(new org.joml.Vector3f(0,0,1));
            var nativeDirection=RigTransforms.orientation(yaw).transform(new org.joml.Vector3f(0,0,1));
            helper.assertTrue(reference.distance(nativeDirection)<.0001,"Native rig faces opposite to the original display at yaw "+yaw);
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void emissary_rig_pitch_matches_server_locator_transform(GameTestHelper helper){
        Vec3 root=new Vec3(12.25,34.5,-8.75);
        for(float yaw:new float[]{0,90,180,-90})for(float pitch:new float[]{-35,35}){
            var orientation=RigTransforms.orientation(yaw,pitch);
            for(Vec3 local:new Vec3[]{new Vec3(0,0,1),new Vec3(0,1,0),new Vec3(1,0,0),new Vec3(.6,4.2,-2.7)}){
                var transformed=orientation.transform(new org.joml.Vector3f((float)local.x,(float)local.y,(float)local.z));
                Vec3 actual=root.add(transformed.x,transformed.y,transformed.z);
                Vec3 expected=EmissaryPose.localPoint(root,yaw,pitch,local);
                helper.assertTrue(actual.distanceToSqr(expected)<1.0e-6,
                    "Rendered Emissary root basis and server locator transform diverged at yaw="+yaw+", pitch="+pitch+", local="+local+": "+actual+" vs "+expected);
            }
        }
        var before=EmissaryDamagePose.sampleAt("phase2.attack.wing_blast","rightupperwingtip1",11);
        var after=EmissaryDamagePose.sampleAt("phase2.attack.wing_blast","rightupperwingtip1",12);
        var half=EmissaryDamagePose.sampleAt("phase2.attack.wing_blast","rightupperwingtip1",11.5f);
        Vec3 midpoint=before.localPosition().add(after.localPosition()).scale(.5);
        helper.assertTrue(half.localPosition().distanceToSqr(midpoint)<1.0e-10,"Exported Emissary locator did not interpolate at a half frame");
        helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_locators",timeoutTicks=20)
    public static void emissary_launch_nodes_follow_exported_animation_locators(GameTestHelper helper)throws Exception{
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 root=helper.absolutePos(new BlockPos(4,240,4)).getCenter();
        var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(root);boss.setYRot(90);boss.setXRot(28);level.addFreshEntity(boss);keepBossLoadedForTest(helper,boss);
        var target=emissaryPlayer(helper,root.add(0,0,16));
        var attackField=NativeBoss.class.getDeclaredField("attack");attackField.setAccessible(true);
        var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);
        var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);
        attackField.set(boss,new NativeBoss.Attack("phase4.attack.fireball",30,35,NativeBoss.Pattern.VOLLEY,Combat.Element.DISSONANCE));
        for(int frame:new int[]{8,18}){ageField.setInt(boss,frame);perform.invoke(boss,level,target);}
        var bolts=level.getEntitiesOfClass(SpellBolt.class,new net.minecraft.world.phys.AABB(root,root).inflate(40));
        helper.assertTrue(bolts.size()==2,"Fireball source frames 8/18 must launch from both hand locators; got "+bolts.size());
        String[] hands={"righthand","lefthand"};int[] frames={8,18};
        for(int index=0;index<frames.length;index++){
            Vec3 expected=EmissaryDamagePose.worldPosition(boss,EmissaryDamagePose.sampleAt(boss,"phase4.attack.fireball",hands[index],frames[index]));
            Vec3 targetPosition=expected;
            var bolt=bolts.stream().min(java.util.Comparator.comparingDouble(candidate->candidate.position().distanceToSqr(targetPosition))).orElseThrow();
            helper.assertTrue(bolt.position().distanceToSqr(expected)<1.0e-8,"Fireball frame "+frames[index]+" did not start at "+hands[index]+" locator: "+bolt.position()+" vs "+expected);
            helper.assertTrue(bolt.getDeltaMovement().normalize().distanceTo(target.position().subtract(expected).normalize())<1.0e-6,
                "Fireball frame "+frames[index]+" must aim at the selected player's feet rather than the locked target's eyes");
            bolt.discard();bolts.remove(bolt);
        }
        ageField.setInt(boss,20);perform.invoke(boss,level,target);
        helper.assertTrue(level.getEntitiesOfClass(SpellBolt.class,new net.minecraft.world.phys.AABB(root,root).inflate(40)).isEmpty(),
            "Source phase-four attack choice selects a new action; frame 20 must not replay a second Fireball volley");
        attackField.set(boss,new NativeBoss.Attack("phase3.attack.seeking_missiles",51,15,NativeBoss.Pattern.VOLLEY,Combat.Element.DISSONANCE));
        int[] missileFrames={2,4,6,8,10,12,14};
        for(int frame:missileFrames){ageField.setInt(boss,frame);perform.invoke(boss,level,target);}
        var missiles=level.getEntitiesOfClass(EmissarySeekingMissile.class,new net.minecraft.world.phys.AABB(root,root).inflate(40));
        helper.assertTrue(missiles.size()==missileFrames.length,"Source seeking missile animation has seven locator nodes; got "+missiles.size());
        for(int frame:missileFrames){
            Vec3 expected=boss.emissarySeekerMissileOrigin(frame);Vec3 targetPosition=expected;
            var bolt=missiles.stream().min(java.util.Comparator.comparingDouble(candidate->candidate.position().distanceToSqr(targetPosition))).orElseThrow();
            helper.assertTrue(bolt.position().distanceToSqr(expected)<1.0e-8,"Seeking missile source frame "+frame+" did not use its exported locator node: "+bolt.position()+" vs "+expected);
            bolt.discard();missiles.remove(bolt);
        }
        boss.startEmissaryAttackForTest(target,"phase4.attack.grapple_grab");
        ageField.setInt(boss,62);
        Vec3 grapple=EmissaryDamagePose.worldPosition(boss,EmissaryDamagePose.sampleAt(boss,"phase4.attack.grapple_grab","rightmiddlewingtip3",62));
        helper.assertTrue(boss.emissaryGrapplePosition().distanceToSqr(grapple)<1.0e-8,"Grapple seat must ride the exported wing-tip-3 node during the grab animation");
        target.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_fireball",timeoutTicks=20)
    public static void emissary_fireball_random_aim_uses_only_eligible_players_within_one_hundred_blocks(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 origin=helper.absolutePos(new BlockPos(4,240,4)).getCenter();
        var first=emissaryPlayer(helper,origin.add(0,2,12));var second=emissaryPlayer(helper,origin.add(0,-1,-15));
        var debug=emissaryPlayer(helper,origin.add(8,0,0));debug.addTag("emissarydebug");
        var outside=emissaryPlayer(helper,origin.add(101,0,0));
        var spectator=encounterTestPlayer(helper,origin.add(0,0,5),net.minecraft.world.level.GameType.SPECTATOR);
        var candidates=level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,new AABB(origin,origin).inflate(110));
        var eligible=NativeBoss.emissaryFireballTargets(origin,candidates);
        helper.assertTrue(eligible.size()==2&&eligible.contains(first)&&eligible.contains(second),
            "Fireball selector included emissarydebug/spectator/out-of-range players or omitted an eligible player: "+eligible);
        Vec3 firstAim=NativeBoss.emissaryFireballDirection(origin,90,0,eligible,0);
        Vec3 secondAim=NativeBoss.emissaryFireballDirection(origin,90,0,eligible,1);
        helper.assertTrue(firstAim.distanceToSqr(eligible.get(0).position().subtract(origin))<1.0e-8
                &&secondAim.distanceToSqr(eligible.get(1).position().subtract(origin))<1.0e-8
                &&firstAim.distanceToSqr(secondAim)>1.0,
            "Each random selector index must aim at that player's feet: "+firstAim+" and "+secondAim);
        Vec3 fallback=Vec3.directionFromRotation(0,90);
        var noTarget=NativeBoss.emissaryFireballTargets(origin,java.util.List.of(debug,outside,spectator));
        Vec3 noTargetDirection=NativeBoss.emissaryFireballDirection(origin,90,0,noTarget,0);
        helper.assertTrue(noTargetDirection.distanceToSqr(fallback)<1.0e-10,"An empty eligible-player selector must preserve locator facing");
        first.discard();second.discard();debug.discard();outside.discard();spectator.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_fireball",timeoutTicks=20)
    public static void emissary_fireball_advances_two_one_block_substeps(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        var owner=EntityType.COW.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(helper.absolutePos(new BlockPos(2,240,2)).getCenter());level.addFreshEntity(owner);
        Vec3 start=helper.absolutePos(new BlockPos(4,240,2)).getCenter();level.getChunkAt(BlockPos.containing(start));
        var bolt=SpellBolt.launch(level,owner,start,new Vec3(1,0,0),35,Combat.Element.DISSONANCE,1.2f,3)
            .effect(SpellBolt.Effect.FIRE,100).emissaryFireballFlight().collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        bolt.tick();Vec3 afterTick=bolt.position();
        helper.assertTrue(Math.abs(afterTick.distanceTo(start)-2)<1.0E-5&&Math.abs(bolt.getDeltaMovement().length()-2)<1.0E-5,
            "Emissary Fireball must perform two one-block source substeps per tick: position="+afterTick+" start="+start+" speed="+bolt.getDeltaMovement().length());
        helper.assertTrue(bolt.remainingLife()==59&&bolt.flightProfileForTest()==SpellBolt.FlightProfile.EMISSARY_FIREBALL,
            "Fireball source timer/profile was not initialized or decremented once per tick");
        CompoundTag saved=bolt.saveWithoutId(new CompoundTag());var restored=Content.BOLT.get().create(level);restored.load(saved);
        helper.assertTrue(restored.flightProfileForTest()==SpellBolt.FlightProfile.EMISSARY_FIREBALL&&restored.remainingLife()==59,
            "Fireball flight profile and source timer did not survive entity NBT round-trip");
        restored.discard();bolt.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_fireball",timeoutTicks=20)
    public static void emissary_fireball_moves_59_ticks_then_impacts_without_a_final_step(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 start=helper.absolutePos(new BlockPos(4,240,4)).getCenter();
        double terminalX=start.x+118;
        for(int chunkX=Math.floorDiv(BlockPos.containing(start).getX(),16);chunkX<=Math.floorDiv(BlockPos.containing(new Vec3(terminalX,start.y,start.z)).getX(),16);chunkX++)
            level.getChunkAt(new BlockPos(chunkX*16,BlockPos.containing(start).getY(),BlockPos.containing(start).getZ()));
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(start.add(-10,0,0));level.addFreshEntity(owner);
        var bolt=SpellBolt.launch(level,owner,start,new Vec3(1,0,0),35,Combat.Element.DISSONANCE,1,3)
            .effect(SpellBolt.Effect.FIRE,60).emissaryFireballFlight().collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        for(int tick=0;tick<59;tick++)bolt.tick();
        Vec3 after59=bolt.position();
        helper.assertTrue(!bolt.isRemoved()&&bolt.remainingLife()==1&&Math.abs(after59.x-terminalX)<1.0E-4,
            "Source timer 59 must have moved 118 blocks and retain one final timer tick: start="+start+" expectedX="+terminalX+" position="+after59+" distance="+after59.distanceTo(start)+" life="+bolt.remainingLife()+" removed="+bolt.isRemoved());
        CompoundTag saved=bolt.saveWithoutId(new CompoundTag());var restored=Content.BOLT.get().create(level);restored.load(saved);
        helper.assertTrue(restored.flightProfileForTest()==SpellBolt.FlightProfile.EMISSARY_FIREBALL&&restored.remainingLife()==1,
            "Near-expiry Fireball did not restore its source profile/timer");restored.discard();
        Vec3 impact=bolt.position();bolt.tick();
        helper.assertTrue(bolt.isRemoved()&&bolt.remainingLife()==0&&bolt.position().distanceToSqr(impact)<1.0E-8,
            "Timer 60 must impact and remove the Fireball without a 120th-block movement: position="+bolt.position()+" impact="+impact+" life="+bolt.remainingLife());
        helper.assertTrue(bolt.fireballTerminalImpactCountForTest()==1&&"TIMEOUT".equals(bolt.lastFireballImpactReasonForTest()),
            "Timer 60 must invoke exactly one explicit timeout impact: count="+bolt.fireballTerminalImpactCountForTest()+" reason="+bolt.lastFireballImpactReasonForTest());
        owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_fireball",timeoutTicks=20)
    public static void emissary_fireball_entity_collision_runs_one_terminal_impact(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 start=helper.absolutePos(new BlockPos(3,240,3)).getCenter();
        var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(start.add(-4,0,0));level.addFreshEntity(owner);
        var target=emissaryPlayer(helper,start.add(1.2,0,0));
        var bolt=SpellBolt.launch(level,owner,start,new Vec3(1,0,0),35,Combat.Element.DISSONANCE,1,3)
            .effect(SpellBolt.Effect.FIRE,60).emissaryFireballFlight().collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        bolt.tick();
        helper.assertTrue(bolt.isRemoved()&&target.getHealth()<200&&bolt.fireballTerminalImpactCountForTest()==1&&"ENTITY".equals(bolt.lastFireballImpactReasonForTest()),
            "A swept entity hit must resolve the Fireball impact once and remove it: removed="+bolt.isRemoved()+" targetHealth="+target.getHealth()+" position="+bolt.position());
        target.discard();owner.discard();bolt.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_fireball_impact",timeoutTicks=1200)
    public static void emissary_fireball_impact_uses_player_feet_radius_five_without_added_fire_or_push(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,16,()->{
            var level=helper.getLevel();
            var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-8,0,0));level.addFreshEntity(owner);
            var boundary=emissaryPlayer(helper,origin.add(5,0,0));
            var upperBoundary=emissaryPlayer(helper,origin.add(0,5,0));
            boundary.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            upperBoundary.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            var outside=emissaryPlayer(helper,origin.add(5.01,0,0));
            var below=emissaryPlayer(helper,origin.add(0,-5.01,0));
            var debug=emissaryPlayer(helper,origin.add(1,0,0));debug.addTag("emissarydebug");
            var protectedPlayer=emissaryPlayer(helper,origin.add(-1,0,0));protectedPlayer.setInvulnerable(true);
            var npc=EntityType.COW.create(level);npc.setNoAi(true);npc.setNoGravity(true);npc.setPos(origin.add(0,0,1));level.addFreshEntity(npc);float npcHealth=npc.getHealth();
            var bolt=SpellBolt.launch(level,owner,origin,new Vec3(1,0,0),35,Combat.Element.DISSONANCE,1,3)
                .effect(SpellBolt.Effect.FIRE,60).emissaryFireballFlight();
            bolt.onHit(new net.minecraft.world.phys.BlockHitResult(origin,net.minecraft.core.Direction.UP,BlockPos.containing(origin),false));
            // Player.hurt scales damage from a living non-player by the world's difficulty.
            float expectedLoss=switch(level.getDifficulty()){case EASY->18.5f;case NORMAL->35;case HARD->52.5f;case PEACEFUL->0;};
            helper.assertTrue(expectedLoss>0,"Fireball damage fixture needs a non-Peaceful world");
            helper.assertTrue(boundary.getHealth()==200-expectedLoss&&upperBoundary.getHealth()==200-expectedLoss,
                "Fireball missed the source inclusive radius-five feet boundary or changed its base 35 damage: horizontal="+boundary.getHealth()+" vertical="+upperBoundary.getHealth());
            helper.assertTrue(boundary.getDeltaMovement().lengthSqr()==0&&upperBoundary.getDeltaMovement().lengthSqr()==0,
                "Fireball added area push despite normal knockback being resisted");
            helper.assertTrue(outside.getHealth()==200&&below.getHealth()==200,"Fireball used a body AABB instead of the source three-dimensional feet sphere");
            helper.assertTrue(debug.getHealth()==200&&npc.getHealth()==npcHealth,"Fireball impact selected an emissarydebug player or a non-player");
            helper.assertTrue(protectedPlayer.getHealth()==200&&!protectedPlayer.isOnFire(),"Fireball bypassed normal damage protection or added unsourced burning");
            helper.assertTrue(!boundary.isOnFire()&&!debug.isOnFire()&&!npc.isOnFire(),"Fireball impact added unsourced entity burning");
            helper.assertTrue(bolt.isRemoved()&&bolt.fireballTerminalImpactCountForTest()==1,"Fireball impact did not resolve exactly once");
            for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-1,-1,-1)),BlockPos.containing(origin.add(1,1,1))))
                helper.assertTrue(!level.getBlockState(pos).is(Blocks.FIRE),"Unconfirmed Fireball world fire was enabled");
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="emissary_fireball_selector",timeoutTicks=1200)
    public static void emissary_fireball_collision_excludes_nonplayers_and_debug_players(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,16,()->{
            var level=helper.getLevel();
            var owner=EntityType.ZOMBIE.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(origin.add(-8,0,0));level.addFreshEntity(owner);
            var npc=EntityType.COW.create(level);npc.setNoAi(true);npc.setNoGravity(true);npc.setPos(origin.add(1,0,0));level.addFreshEntity(npc);
            var debug=emissaryPlayer(helper,origin.add(1.5,0,0));debug.addTag("emissarydebug");
            var target=emissaryPlayer(helper,origin.add(3.3,0,0));
            var bolt=SpellBolt.launch(level,owner,origin,new Vec3(1,0,0),35,Combat.Element.DISSONANCE,1,3)
                .effect(SpellBolt.Effect.FIRE,60).emissaryFireballFlight().collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
            bolt.tick();helper.assertTrue(!bolt.isRemoved()&&Math.abs(bolt.getX()-origin.x-2)<1.0e-5,
                "Fireball terminated on a non-player or emissarydebug collision");
            bolt.tick();helper.assertTrue(bolt.isRemoved()&&target.getHealth()<200&&"ENTITY".equals(bolt.lastFireballImpactReasonForTest()),
                "Fireball failed to terminate on its next eligible player");
            helper.succeed();
        });
    }
    @GameTest(template="empty",batch="emissary_fireball",timeoutTicks=20)
    public static void reflected_emissary_fireball_keeps_two_one_block_substeps(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 start=helper.absolutePos(new BlockPos(3,240,3)).getCenter();
        var owner=EntityType.COW.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(start.add(-4,0,0));level.addFreshEntity(owner);
        var bolt=SpellBolt.launch(level,owner,start,new Vec3(1,0,0),35,Combat.Element.DISSONANCE,1,3)
            .effect(SpellBolt.Effect.FIRE,60).emissaryFireballFlight().collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        bolt.reflectFrom(owner,new Vec3(0,0,1));bolt.tick();
        helper.assertTrue(Math.abs(bolt.getZ()-(start.z+2))<1.0E-5&&Math.abs(bolt.getX()-start.x)<1.0E-5&&bolt.remainingLife()==59,
            "Reflectance must change the Fireball heading without changing its two one-block substeps or timer: start="+start+" end="+bolt.position()+" life="+bolt.remainingLife());
        bolt.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="emissary_fireball",timeoutTicks=20)
    public static void emissary_fireball_impacts_at_first_block_substep(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 start=helper.absolutePos(new BlockPos(3,240,3)).getCenter();
        BlockPos wall=BlockPos.containing(start.add(1.3,0,0));level.getChunkAt(wall);level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        var owner=EntityType.COW.create(level);owner.setNoAi(true);owner.setNoGravity(true);owner.setPos(start.add(-4,0,0));level.addFreshEntity(owner);
        var bolt=SpellBolt.launch(level,owner,start,new Vec3(1,0,0),35,Combat.Element.DISSONANCE,1,3)
            .effect(SpellBolt.Effect.FIRE,60).emissaryFireballFlight().collisionPolicy(SpellCollisionPolicy.SOURCE_PASSTHROUGH);
        bolt.tick();
        helper.assertTrue(bolt.isRemoved()&&bolt.position().x<=wall.getX()+1.0E-5&&bolt.fireballTerminalImpactCountForTest()==1&&"BLOCK".equals(bolt.lastFireballImpactReasonForTest()),
            "Fireball crossed a non-passthrough wall or failed to terminate at its first colliding substep: x="+bolt.getX()+" wall="+wall);
        bolt.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void emissary_exported_keyframe_is_not_distorted(GameTestHelper helper)throws Exception{
        try(var input=new java.util.zip.GZIPInputStream(NativeGameTests.class.getResourceAsStream("/assets/nyctocosm/native_rigs/ajemissary.json.gz"));var reader=new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)){
            var rig=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var frames=rig.getAsJsonObject("animations").getAsJsonArray("phase3.attack.flipfar");
            var data=frames.get(6).getAsJsonObject().getAsJsonObject("bones").getAsJsonArray("leftlowerwing");
            float[] values=new float[16];for(int n=0;n<16;n++)values[n]=data.get(n).getAsFloat();
            var matrix=new org.joml.Matrix4f().set(values).transpose();
            helper.assertTrue(matrix.equals(RigTransforms.interpolate(matrix,matrix,.5f),.001f),"Original Emissary wing transform changed during playback");
        }
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void gravitance_damages_during_pull_not_on_expiry(GameTestHelper helper){
        var target=helper.spawn(Content.SORCERER.get(),new BlockPos(3,2,3));keepBossLoadedForTest(helper,target);target.setNoAi(true);target.setNoGravity(true);
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(1,2,1));owner.setNoAi(true);owner.setNoGravity(true);
        var bolt=SpellBolt.launch(helper.getLevel(),owner,target.position().add(0,1,0),Vec3.ZERO,20,Combat.Element.VOID,0,0).effect(SpellBolt.Effect.GRAVITY,30);
        helper.runAfterDelay(3,()->helper.assertTrue(target.getHealth()<1000,"Gravitance did not damage during its pull"));
        helper.runAfterDelay(24,()->{
            helper.assertTrue(target.getHealth()<=960,"Gravitance did not repeat damage after hurt cooldown");
            target.setInvulnerable(true);
        });
        helper.runAfterDelay(32,()->{helper.assertTrue(bolt.isRemoved(),"Gravitance did not expire");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=15)
    public static void gravitance_expiry_has_no_damage(GameTestHelper helper){
        var target=helper.spawn(EntityType.COW,new BlockPos(3,2,3));target.setNoAi(true);target.setNoGravity(true);
        var bolt=SpellBolt.launch(helper.getLevel(),target,target.position(),Vec3.ZERO,20,Combat.Element.VOID,0,0).effect(SpellBolt.Effect.GRAVITY,1);
        bolt.setOwner(null);
        helper.runAfterDelay(3,()->{helper.assertTrue(bolt.isRemoved()&&target.getHealth()==target.getMaxHealth(),"Gravitance caused an invented expiry explosion");helper.succeed();});
    }
    @GameTest(template="empty")
    public static void elemental_damage_hits_registered_mod_entity(GameTestHelper helper){
        var mob=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(2,2,2));mob.setNoAi(true);mob.setNoGravity(true);
        for(Combat.Element element:Combat.Element.values()){
            mob.invulnerableTime=0;mob.setHealth(80);
            helper.assertTrue(Combat.hit(null,mob,10,element),"Element was rejected: "+element);
            helper.assertTrue(mob.getHealth()<80,"Registered modded mob was not damaged: "+element);
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void boss_health_is_per_entity(GameTestHelper helper){
        var first=helper.spawn(Content.SORCERER.get(),new BlockPos(2,2,2));
        var second=helper.spawn(Content.SORCERER.get(),new BlockPos(5,2,2));
        keepBossLoadedForTest(helper,first);
        first.setNoAi(true);second.setNoAi(true);first.setNoGravity(true);second.setNoGravity(true);
        helper.assertTrue(first.getMaxHealth()==1000&&second.getMaxHealth()==1000,"Wrong original boss base health");
        first.hurt(helper.getLevel().damageSources().generic(),75);
        helper.assertTrue(first.getHealth()<1000&&second.getHealth()==1000,"Bosses share health");
        helper.assertTrue(helper.getLevel().getScoreboard().getObjectives().isEmpty(),"Native mod created scoreboards");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void native_projectile_collision_damages_mod_entity(GameTestHelper helper){
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(5,2,3));target.setNoAi(true);target.setNoGravity(true);
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(1,2,3));owner.setNoAi(true);owner.setNoGravity(true);
        float before=target.getHealth();
        Vec3 origin=owner.position().add(0,1,0);
        SpellBolt.launch(helper.getLevel(),owner,origin,target.position().add(0,1,0).subtract(origin),12,Combat.Element.VOID,1,0);
        helper.runAfterDelay(12,()->{helper.assertTrue(target.getHealth()<before,"Swept projectile collision did not hurt the mod entity");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void scythe_soul_requires_capture_container(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SoulTester"));
        helper.getLevel().addNewPlayer(player);player.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
        var soul=Content.SOUL.get().create(helper.getLevel());soul.setPos(player.position().add(0,1,0));helper.getLevel().addFreshEntity(soul);
        soul.tick();helper.assertTrue(!soul.isRemoved(),"Unprepared player collected the soul");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Content.get("planar_coil")));
        soul.tick();helper.assertTrue(!soul.isRemoved(),"A Planar Coil in the main hand captured a soul");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
        player.setPos(soul.position().add(4,0,4));soul.tick();helper.assertTrue(!soul.isRemoved(),"Soul capture reached outside its five-block sphere");
        player.setPos(soul.position().add(5,0,0));
        soul.tick();helper.assertTrue(soul.isRemoved(),"Glass bottle failed to capture soul");
        helper.assertTrue(player.getInventory().countItem(Content.get("soul_bottle"))==1,"Captured soul did not produce a filled bottle");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Content.get("planar_coil")));
        var second=Content.SOUL.get().create(helper.getLevel());second.setPos(player.position());helper.getLevel().addFreshEntity(second);second.tick();
        helper.assertTrue(second.isRemoved()&&FocusItem.souls(player.getOffhandItem())==2,"Offhand Planar Coil failed to capture a soul");
        player.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void spending_last_bottled_soul_returns_glass(GameTestHelper helper){
        // GameTestHelper.makeMockPlayer() is always creative in 1.20.1.
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BottleTester"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);helper.getLevel().addNewPlayer(player);
        player.setPos(helper.absolutePos(new BlockPos(3,3,3)).getCenter());
        helper.assertTrue(!player.isCreative(),"Bottle consumption requires a survival player");
        var bottle=new ItemStack(Content.get("soul_bottle"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,bottle);
        var result=((FocusItem)bottle.getItem()).use(helper.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getObject().is(net.minecraft.world.item.Items.GLASS_BOTTLE)&&player.getMainHandItem().is(net.minecraft.world.item.Items.GLASS_BOTTLE),"Empty Soul Bottle did not return the original glass bottle");
        var fields=helper.getLevel().getEntitiesOfClass(SeelicField.class,player.getBoundingBox().inflate(3),field->field.ownedBy(player));
        helper.assertTrue(fields.size()==1,"Spending the last soul failed to create the plane");fields.forEach(SeelicField::discard);
        var coil=new ItemStack(Content.get("planar_coil"));player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,coil);
        var coilResult=((FocusItem)coil.getItem()).use(helper.getLevel(),player,net.minecraft.world.InteractionHand.OFF_HAND);
        helper.assertTrue(coilResult.getObject()==coil&&player.getOffhandItem()==coil&&FocusItem.souls(coil)==0,"Empty Planar Coil must keep its reusable body");
        helper.getLevel().getEntitiesOfClass(SeelicField.class,player.getBoundingBox().inflate(3),field->field.ownedBy(player)).forEach(SeelicField::discard);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var creativeBottle=new ItemStack(Content.get("soul_bottle"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,creativeBottle);
        ((FocusItem)creativeBottle.getItem()).use(helper.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem()==creativeBottle&&FocusItem.souls(creativeBottle)==1,"Creative field creation consumed a bottled soul");
        helper.getLevel().getEntitiesOfClass(SeelicField.class,player.getBoundingBox().inflate(3),field->field.ownedBy(player)).forEach(SeelicField::discard);
        player.discard();helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void submergence_tendril_delays_damage_and_survives_save(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TendrilTester"));
        helper.getLevel().addNewPlayer(player);player.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(5,2,5));target.setNoAi(true);target.setNoGravity(true);
        float before=target.getHealth();var area=SpellArea.createSubmergence(helper.getLevel(),player,target);
        for(int age=0;age<40;age++)area.tick();
        helper.assertTrue(target.getHealth()==before,"Submergence damaged target before source frame 81");
        CompoundTag saved=new CompoundTag();area.saveWithoutId(saved);area.discard();
        var restored=Content.AREA.get().create(helper.getLevel());restored.load(saved);helper.getLevel().addFreshEntity(restored);
        for(int age=40;age<81;age++)restored.tick();
        helper.assertTrue(target.getHealth()<before,"Saved tendril failed to deliver its delayed hit");
        restored.discard();target.discard();player.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void reflectance_consumes_source_twenty_damage_capacity(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ReflectTester"));
        helper.getLevel().addNewPlayer(player);player.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());player.setYRot(0);player.setXRot(0);
        var hostile=helper.spawn(EntityType.ZOMBIE,new BlockPos(3,2,8));hostile.setNoAi(true);
        Vec3 at=player.position().add(0,1,3);var area=SpellArea.create(helper.getLevel(),player,at,Spell.REFLECTANCE);
        var first=SpellBolt.launch(helper.getLevel(),hostile,at,new Vec3(1,0,-1),11,Combat.Element.VOID,.2f,0);
        area.tick();helper.assertTrue(first.getOwner()==player&&!area.isRemoved(),"First incoming spell was not reflected or exhausted the reflector early");
        helper.assertTrue(Math.abs(first.getDeltaMovement().x)<1.0e-5&&first.getDeltaMovement().z>.19,
            "Oblique impact must follow the reflector's facing instead of using specular mirror reflection: "+first.getDeltaMovement());
        CompoundTag saved=new CompoundTag();area.saveWithoutId(saved);helper.assertTrue(Math.abs(saved.getFloat("ReflectanceEnergy")-9)<.001,"Reflectance capacity did not decrease by incoming damage");
        var second=SpellBolt.launch(helper.getLevel(),hostile,at,new Vec3(0,0,-1),9,Combat.Element.VOID,.2f,0);
        area.tick();helper.assertTrue(second.getOwner()==player&&area.isRemoved(),"Reflectance exceeded its twenty damage capacity");
        CompoundTag depleted=new CompoundTag();area.saveWithoutId(depleted);
        helper.assertTrue(Math.abs(depleted.getFloat("ReflectanceEnergy"))<.001,"A final nine-point hit did not consume the remaining capacity exactly");
        first.discard();second.discard();hostile.discard();player.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="reflectance",timeoutTicks=20)
    public static void reflectance_uses_closed_three_block_sphere_without_direction_gate(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        var owner=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ReflectBounds"));
        level.addNewPlayer(owner);
        Vec3 center=helper.absolutePos(new BlockPos(7,3,7)).getCenter();
        owner.setPos(center.add(8,0,0));
        var hostile=EntityType.ZOMBIE.create(level);hostile.setNoAi(true);hostile.setPos(center.add(-8,0,0));level.addFreshEntity(hostile);
        var area=SpellArea.create(level,owner,center,Spell.REFLECTANCE);area.setYRot(0);area.setXRot(0);
        var side=SpellBolt.launch(level,hostile,center.add(2.9,0,0),new Vec3(1,0,0),1,Combat.Element.VOID,.2f,0);
        var boundary=SpellBolt.launch(level,hostile,center.add(0,0,3),new Vec3(0,0,-1),1,Combat.Element.VOID,.2f,0);
        var back=SpellBolt.launch(level,hostile,center.add(0,0,-2.9),new Vec3(0,0,-1),1,Combat.Element.VOID,.2f,0);
        var outside=SpellBolt.launch(level,hostile,center.add(0,0,3.1),new Vec3(0,0,-1),1,Combat.Element.VOID,.2f,0);
        var corner=SpellBolt.launch(level,hostile,center.add(2.8,0,2.8),new Vec3(0,0,-1),1,Combat.Element.VOID,.2f,0);
        area.tick();
        helper.assertTrue(side.getOwner()==owner,"A side entry moving away from the facing normal was not reflected");
        helper.assertTrue(boundary.getOwner()==owner,"A hit at the source's closed 3-block boundary was not reflected");
        helper.assertTrue(back.getOwner()==owner,"A back-side sample inside the source selector sphere was not reflected");
        helper.assertTrue(outside.getOwner()==hostile,"A projectile at distance 3.1 was reflected by the broad-phase box");
        helper.assertTrue(corner.getOwner()==hostile,"A projectile in an AABB corner outside the source sphere was reflected");
        helper.assertTrue(side.getDeltaMovement().z>0&&Math.abs(side.getDeltaMovement().x)<1.0e-5,
            "Reflected projectile did not adopt reflector facing while preserving its speed");
        side.discard();boundary.discard();back.discard();outside.discard();corner.discard();area.discard();hostile.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void repellance_windup_survives_save(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GustTester"));
        helper.getLevel().addNewPlayer(player);Vec3 testPos=helper.absolutePos(new BlockPos(2,2,2)).getCenter();
        player.setPos(testPos.x,helper.getLevel().getMaxBuildHeight()-32,testPos.z);player.setYRot(-90);player.setXRot(0);
        var area=SpellArea.create(helper.getLevel(),player,player.position(),Spell.REPELLANCE);
        double startX=area.getX();for(int tick=0;tick<10;tick++)area.tick();
        CompoundTag saved=new CompoundTag();area.saveWithoutId(saved);area.discard();
        var restored=Content.AREA.get().create(helper.getLevel());restored.load(saved);helper.getLevel().addFreshEntity(restored);
        for(int tick=10;tick<25;tick++)restored.tick();
        helper.assertTrue(Math.abs(restored.getX()-startX)<.01,"Repellance moved during its restored windup");
        restored.tick();helper.assertTrue(Math.abs(restored.getX()-startX-2)<.01,"Repellance failed to launch on source frame 26 after reload: delta="+(restored.getX()-startX)+", removed="+restored.isRemoved()+", block="+helper.getLevel().getBlockState(restored.blockPosition())+", saved="+saved.getInt("GatheringTicks")+", life="+saved.getInt("Life"));
        for(int tick=0;tick<48;tick++)restored.tick();
        helper.assertTrue(!restored.isRemoved()&&Math.abs(restored.getX()-startX-98)<.01,"Windup consumed part of Repellance's fifty movement ticks");
        restored.tick();helper.assertTrue(restored.isRemoved()&&Math.abs(restored.getX()-startX-100)<.01,"Repellance did not finish its full hundred-block flight");
        restored.discard();player.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void wind_spells_use_source_direction_and_spherical_radius(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(3,3,3)).getCenter();
        var owner=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WindTester"));
        level.addNewPlayer(owner);owner.setPos(center);owner.setYRot(0);owner.setXRot(0);
        var target=helper.spawn(EntityType.COW,new BlockPos(6,3,3));target.setNoAi(true);target.setPos(center.add(3,0,0));target.setYRot(90);target.setDeltaMovement(Vec3.ZERO);
        var outside=helper.spawn(EntityType.COW,new BlockPos(8,3,8));outside.setNoAi(true);outside.setPos(center.add(5,0,5));outside.setDeltaMovement(Vec3.ZERO);
        var bubble=SpellArea.create(level,owner,center,Spell.VACANCE);bubble.tick();
        helper.assertTrue(target.getDeltaMovement().x>.99&&Math.abs(target.getDeltaMovement().z)<.01,"Vacance followed the target's facing instead of pushing outward");
        helper.assertTrue(outside.getDeltaMovement().lengthSqr()==0,"Vacance pushed a target outside its six-block sphere");
        bubble.discard();outside.discard();
        target.setPos(center.add(2,0,2));target.setYRot(180);target.setDeltaMovement(Vec3.ZERO);
        var gust=SpellArea.create(level,owner,center,Spell.REPELLANCE);
        for(int i=0;i<26;i++)gust.tick();
        helper.assertTrue(target.getDeltaMovement().z>1.49&&Math.abs(target.getDeltaMovement().x)<.01,"Repellance did not push along the gust heading");
        gust.discard();target.discard();owner.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void protection_drains_one_shared_charge_and_pauses_without_artifacts(GameTestHelper helper){
        var player=helper.makeMockPlayer();NativeEvents.addHypertraceCharge(player,5);
        player.getInventory().add(new ItemStack(Content.get("artifact_transposition")));
        for(int i=0;i<200;i++)NativeEvents.tickArtifacts(player);
        helper.assertTrue(NativeEvents.hypertraceCharges(player)==5,"Transposition incorrectly depleted Hypertrace charge");
        player.getInventory().add(new ItemStack(Content.get("artifact_protection_void")));player.getInventory().add(new ItemStack(Content.get("artifact_protection_dream")));
        for(int i=0;i<199;i++)NativeEvents.tickArtifacts(player);
        helper.assertTrue(NativeEvents.hypertraceCharges(player)==5,"Multiple protections drained charge before two hundred active ticks");
        var replacement=helper.makeMockPlayer();NativeEvents.copyPersistentProgress(player,replacement);
        for(int i=0;i<60;i++)NativeEvents.tickArtifacts(replacement);
        helper.assertTrue(NativeEvents.hypertraceCharges(replacement)==5,"Protection drained while its artifact was absent");
        replacement.getInventory().add(new ItemStack(Content.get("artifact_protection_void")));replacement.getInventory().add(new ItemStack(Content.get("artifact_protection_dream")));
        NativeEvents.tickArtifacts(replacement);
        helper.assertTrue(NativeEvents.hypertraceCharges(replacement)==4,"Protection lost its saved timer or charged once per artifact");helper.succeed();
    }
    @GameTest(template="empty")
    public static void levitation_uses_two_air_blocks_and_one_activation_per_press(GameTestHelper helper){
        var player=helper.makeMockPlayer();player.setPos(helper.absolutePos(new BlockPos(3,5,3)).getCenter());
        player.getInventory().add(new ItemStack(Content.get("artifact_levitation")));NativeEvents.addHypertraceCharge(player,5);
        player.setDeltaMovement(0,.2,0);player.setShiftKeyDown(true);NativeEvents.tickArtifacts(player);
        helper.assertTrue(NativeEvents.hypertraceCharges(player)==2&&player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"Levitation did not activate while rising above two air blocks");
        NativeEvents.addHypertraceCharge(player,3);NativeEvents.tickArtifacts(player);
        helper.assertTrue(NativeEvents.hypertraceCharges(player)==5,"Holding sneak charged for Levitation twice");
        player.setShiftKeyDown(false);NativeEvents.tickArtifacts(player);
        helper.getLevel().setBlockAndUpdate(player.blockPosition().below(2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        player.setShiftKeyDown(true);player.setDeltaMovement(0,-.2,0);NativeEvents.tickArtifacts(player);
        helper.assertTrue(NativeEvents.hypertraceCharges(player)==5,"Levitation activated with a solid block two blocks below");helper.succeed();
    }
    @GameTest(template="empty")
    public static void expulsion_chain_waits_fifteen_ticks_and_survives_reload(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(2,3,2)).getCenter();
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BurstTester"));
        level.addNewPlayer(player);player.setPos(center.add(-2,0,0));player.getInventory().add(new ItemStack(Content.get("artifact_expulsion")));
        var first=helper.spawn(EntityType.COW,new BlockPos(2,3,2));first.setPos(center);first.setNoAi(true);
        var next=helper.spawn(EntityType.COW,new BlockPos(6,3,2));next.setPos(center.add(4,0,0));next.setNoAi(true);next.setHealth(5);
        var last=helper.spawn(EntityType.COW,new BlockPos(11,3,2));last.setPos(center.add(9,0,0));last.setNoAi(true);
        Combat.hit(player,first,20,Combat.Element.DISSONANCE);
        helper.assertTrue(next.getHealth()==5,"Expulsion exploded immediately on a Dissonance kill");
        var bounds=new net.minecraft.world.phys.AABB(center,center).inflate(12);
        var markers=level.getEntitiesOfClass(ExpulsionBurst.class,bounds);
        helper.assertTrue(markers.size()==1,"Kill did not schedule one Expulsion marker");
        var marker=markers.get(0);for(int i=0;i<7;i++)marker.tick();
        CompoundTag saved=new CompoundTag();marker.saveWithoutId(saved);marker.discard();
        var restored=Content.EXPULSION_BURST.get().create(level);restored.load(saved);level.addFreshEntity(restored);
        for(int i=7;i<14;i++)restored.tick();
        helper.assertTrue(next.getHealth()==5,"Reload advanced the Expulsion explosion too early");
        restored.tick();
        helper.assertTrue(!next.isAlive()&&last.getHealth()==last.getMaxHealth(),"Expulsion chain propagated synchronously instead of waiting for the next marker");
        var chained=level.getEntitiesOfClass(ExpulsionBurst.class,bounds);
        helper.assertTrue(chained.size()==1,"Chain kill did not schedule exactly one subsequent explosion");
        for(int i=0;i<15;i++)chained.get(0).tick();
        helper.assertTrue(last.getHealth()<last.getMaxHealth(),"Delayed chain explosion failed to reach the next victim");
        first.discard();next.discard();last.discard();player.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="expulsion_feet_sphere",timeoutTicks=10)
    public static void expulsion_uses_inclusive_horizontal_target_feet_radius(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(4,3,4)).getCenter();
        var owner=emissaryPlayer(helper,center.add(0,0,20));
        var inside=emissaryPlayer(helper,center.add(5.99,0,0));float insideHealth=inside.getHealth();
        var outside=emissaryPlayer(helper,center.add(6.01,0,0));float outsideHealth=outside.getHealth();
        ExpulsionBurst.explode(level,owner,center);
        helper.assertTrue(inside.getHealth()<insideHealth,"Expulsion missed a source livingthing whose feet are 5.99 blocks from its origin");
        helper.assertTrue(outside.getHealth()==outsideHealth,
            "Expulsion included a player's feet at 6.01 blocks although the player's body intersects the radius AABB");
        owner.discard();inside.discard();outside.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="expulsion_feet_sphere",timeoutTicks=10)
    public static void expulsion_uses_three_dimensional_feet_distance_for_large_mobs(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(4,3,4)).getCenter();
        var owner=emissaryPlayer(helper,center.add(0,0,20));
        var above=emissaryPlayer(helper,center.add(0,5.99,0));float aboveHealth=above.getHealth();
        var giant=EntityType.GIANT.create(level);giant.setNoAi(true);giant.setNoGravity(true);giant.setPos(center.add(0,-6.01,0));level.addFreshEntity(giant);float giantHealth=giant.getHealth();
        helper.assertTrue(Combat.distanceToBoxSqr(center,giant.getBoundingBox())<36,
            "Large mob fixture must place its body inside the explosion while its feet remain outside");
        ExpulsionBurst.explode(level,owner,center);
        helper.assertTrue(above.getHealth()<aboveHealth,"Expulsion missed a vertically aligned target whose feet are inside radius six");
        helper.assertTrue(giant.getHealth()==giantHealth,
            "Expulsion included a large source mob whose body intersects the sphere but whose feet are 6.01 blocks below it");
        owner.discard();above.discard();giant.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void area_damage_hits_large_body_at_feet(GameTestHelper helper){
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(4,3,4));target.setNoAi(true);
        var owner=helper.makeMockPlayer();float before=target.getHealth();
        Combat.area(helper.getLevel(),owner,target.position().add(0,.2,0),1,5,Combat.Element.VOID,0);
        helper.assertTrue(target.getHealth()<before,"An explosion touching a large body's feet missed because its center was outside the radius");
        target.discard();helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void reflectance_protects_against_gravity_only_while_active(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(3,3,3)).getCenter();
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ShieldTester"));
        level.addNewPlayer(player);player.setPos(center);player.setNoGravity(true);player.setYRot(0);player.setXRot(0);player.setDeltaMovement(Vec3.ZERO);
        var enemy=helper.spawn(EntityType.ZOMBIE,new BlockPos(7,3,3));enemy.setNoAi(true);
        var shield=SpellArea.create(level,player,center.add(0,0,3),Spell.REFLECTANCE);shield.tick();
        var gravity=SpellBolt.launch(level,enemy,center.add(1,0,0),Vec3.ZERO,20,Combat.Element.VOID,0,0).effect(SpellBolt.Effect.GRAVITY,100);gravity.tick();
        helper.assertTrue(player.getDeltaMovement().lengthSqr()==0,"Gravitance pulled a player protected behind Reflectance");
        shield.discard();gravity.discard();
        helper.runAfterDelay(4,()->{
            player.setPos(center);player.setDeltaMovement(Vec3.ZERO);
            var after=SpellBolt.launch(level,enemy,center.add(1,0,0),Vec3.ZERO,20,Combat.Element.VOID,0,0).effect(SpellBolt.Effect.GRAVITY,100);after.tick();
            helper.assertTrue(player.getDeltaMovement().lengthSqr()>.001,"Reflectance left permanent forced-movement immunity after it disappeared");
            after.discard();enemy.discard();player.discard();helper.succeed();
        });
    }
    @GameTest(template="empty")
    public static void adamance_anchors_owner_until_sneaking(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AnchorTester"));
        helper.getLevel().addNewPlayer(player);player.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());
        var area=SpellArea.create(helper.getLevel(),player,player.position(),Spell.ADAMANCE);
        Vec3 anchor=area.position();player.setShiftKeyDown(false);player.setPos(anchor.add(15,0,0));area.tick();
        helper.assertTrue(player.position().distanceToSqr(anchor)<.01,"Adamance failed to anchor its owner beyond twelve blocks: player="+player.position()+" anchor="+anchor+" removed="+area.isRemoved()+" sneaking="+player.isShiftKeyDown()+" spell="+area.spell());
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)&&player.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION).getAmplifier()==255,"Adamance lost its original levitation lock");
        player.setShiftKeyDown(true);player.setPos(anchor.add(4,0,0));area.tick();
        helper.assertTrue(player.position().distanceToSqr(anchor)>15,"Sneaking failed to suspend Adamance anchoring");
        area.discard();player.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void legacy_book_conversion_keeps_bound_chapter(GameTestHelper helper){
        ItemStack old=new ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);old.getOrCreateTag().putString("title","Entry Four");
        ItemStack converted=LegacyMigration.convert(old);
        helper.assertTrue(converted.getItem()==Content.get("ancient_journals")&&((LoreItem)converted.getItem()).chapter(converted)==4,"Legacy entry lost its bound chapter");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void native_items_and_saved_souls(GameTestHelper helper){
        helper.assertTrue(Content.ALL.size()==37,"Expected 31 original native items, dais and 5 spawn eggs");
        ItemStack bottle=new ItemStack(Content.get("soul_bottle"));
        helper.assertTrue(FocusItem.souls(bottle)==1,"Original starting soul missing");FocusItem.addSoul(bottle,4);
        CompoundTag saved=bottle.save(new CompoundTag());ItemStack loaded=ItemStack.of(saved);
        helper.assertTrue(FocusItem.souls(loaded)==5,"Soul count not saved");
        FocusItem.addSoul(loaded,1000);helper.assertTrue(FocusItem.souls(ItemStack.of(loaded.save(new CompoundTag())))==1005,"Soul count was silently truncated at the invented 999 limit");
        helper.assertTrue(Spell.values().length==17,"Missing spells");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void offhand_weapons_keep_original_particle_profiles(GameTestHelper helper){
        Vec3 eye=new Vec3(0,2,0),end=new Vec3(0,2,6),forward=new Vec3(0,0,1);
        var fissure=WeaponEffects.offhandPlan(2,eye,end,forward);
        var doom=WeaponEffects.offhandPlan(3,eye,end,forward);
        helper.assertTrue(fissure.size()==191,"Nullfissure did not preserve 18 black + 20 purple samples per beam section");
        helper.assertTrue(fissure.get(0).particle().getType()==net.minecraft.core.particles.ParticleTypes.FLASH,"Nullfissure muzzle flash missing");
        helper.assertTrue(fissure.stream().noneMatch(call->call.particle().getType()==net.minecraft.core.particles.ParticleTypes.END_ROD),"Generic end rods replaced the source beam profile");
        Vec3 firstCenter=eye.add(forward.scale(2));
        for(int n=1;n<=38;n++)helper.assertTrue(Math.abs(fissure.get(n).position().subtract(firstCenter).dot(forward))<.00001,"Fissure ring is not perpendicular to aim");
        int delayed=0;for(int tick=0;tick<3;tick++)delayed+=WeaponEffects.dissonanceAfterburstPlan(eye,forward,tick).stream().mapToInt(WeaponEffects.ParticleCall::count).sum();
        helper.assertTrue(doom.size()==58&&doom.stream().mapToInt(WeaponEffects.ParticleCall::count).sum()==86&&delayed==27,"Semblance did not preserve its 86 + 27 particle transmutation sequence");
        for(var type:new net.minecraft.core.particles.ParticleType[]{net.minecraft.core.particles.ParticleTypes.FLASH,net.minecraft.core.particles.ParticleTypes.EXPLOSION,net.minecraft.core.particles.ParticleTypes.FLAME,net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE})
            helper.assertTrue(doom.stream().anyMatch(call->call.particle().getType()==type),"Semblance burst particle missing: "+type);
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void vanilla_mobs_and_owner_exclusion(GameTestHelper helper){
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(2,2,2));
        var target=helper.spawn(EntityType.COW,new BlockPos(4,2,2));
        helper.assertTrue(!Combat.hit(owner,owner,5,Combat.Element.RADIANCE),"Caster damaged itself");
        helper.assertTrue(Combat.hit(owner,target,5,Combat.Element.RADIANCE),"Vanilla mob not damaged");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void dais_consumes_exact_original_cost(GameTestHelper helper){
        var player=helper.makeMockPlayer();var menu=new DaisMenu(1,player.getInventory());
        menu.getSlot(0).set(new ItemStack(Content.get("seelic_essence"),40));
        menu.getSlot(1).set(new ItemStack(Content.get("nightmare_scale"),51));
        helper.assertTrue(!menu.getSlot(2).hasItem(),"Dais accepted too few scales");
        menu.getSlot(1).set(new ItemStack(Content.get("nightmare_scale"),52));
        helper.assertTrue(menu.getSlot(2).getItem().is(Content.get("wyrmsliver")),"Original wyrmsliver recipe missing");
        menu.quickMoveStack(player,2);
        helper.assertTrue(!menu.getSlot(0).hasItem()&&!menu.getSlot(1).hasItem()&&!menu.getSlot(2).hasItem(),"Ingredients not consumed atomically");
        helper.assertTrue(player.getInventory().countItem(Content.get("wyrmsliver"))==1,"Crafting output duplicated or lost");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void dais_does_not_lose_partial_output(GameTestHelper helper){
        var player=helper.makeMockPlayer();var menu=new DaisMenu(1,player.getInventory());
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(net.minecraft.world.item.Items.STONE,64));
        player.getInventory().setItem(0,new ItemStack(Content.get("seelic_essence"),62));
        menu.getSlot(0).set(new ItemStack(net.minecraft.world.item.Items.SOUL_SAND,4));
        menu.getSlot(1).set(new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD));
        helper.assertTrue(menu.quickMoveStack(player,2).isEmpty(),"Partially fitting craft was transferred");
        helper.assertTrue(menu.getSlot(0).getItem().getCount()==4&&menu.getSlot(1).getItem().getCount()==1,"Failed transfer consumed inputs");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void field_consumes_only_owners_reagent(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());var pos=helper.absolutePos(new BlockPos(2,2,2));
        helper.getLevel().addNewPlayer(player);
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.setNoGravity(true);
        var ally=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"LightAlly"));
        var outsider=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"LightOutside"));
        helper.getLevel().addNewPlayer(ally);helper.getLevel().addNewPlayer(outsider);
        ally.setPos(player.position().add(.75,0,0));outsider.setPos(player.position().add(2,0,0));ally.setNoGravity(true);outsider.setNoGravity(true);
        SeelicField field=new SeelicField(Content.FIELD.get(),helper.getLevel());field.configure(player,false,false);field.setPos(player.position());helper.getLevel().addFreshEntity(field);
        var foreign=new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),player.getX(),player.getY(),player.getZ(),new ItemStack(net.minecraft.world.item.Items.GLOWSTONE));
        foreign.setThrower(java.util.UUID.randomUUID());foreign.setNoGravity(true);foreign.setDeltaMovement(Vec3.ZERO);helper.getLevel().addFreshEntity(foreign);
        var own=new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),player.getX(),player.getY(),player.getZ(),new ItemStack(net.minecraft.world.item.Items.GLOWSTONE));
        own.setThrower(player.getUUID());own.setNoGravity(true);own.setDeltaMovement(Vec3.ZERO);own.setPickUpDelay(100);foreign.setPickUpDelay(100);helper.getLevel().addFreshEntity(own);
        helper.runAfterDelay(12,()->{
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)&&ally.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Glowstone must give source Glowing to players on the plane");
            helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Luminescence still gives the invented Night Vision instead of source Glowing");
            helper.assertTrue(!outsider.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Luminescence reached beyond its one-block plane radius");
            helper.assertTrue(own.isRemoved(),"Casting did not consume the reagent");
            helper.assertTrue(!foreign.isRemoved(),"Field stole a foreign reagent");
            player.discard();ally.discard();outsider.discard();field.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=90)
    public static void resistance_reaches_nearby_players_but_not_distant_players(GameTestHelper helper){
        var level=helper.getLevel();var center=helper.absolutePos(new BlockPos(3,2,3)).getCenter();
        var caster=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WardCaster"));
        var ally=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WardAlly"));
        var outsider=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WardOutside"));
        for(var person:new net.minecraft.server.level.ServerPlayer[]{caster,ally,outsider}){level.addNewPlayer(person);person.setNoGravity(true);}
        caster.setPos(center);ally.setPos(center.add(1.5,0,0));outsider.setPos(center.add(3,0,0));
        var field=new SeelicField(Content.FIELD.get(),level);field.configure(caster,false,false);field.setPos(center);level.addFreshEntity(field);
        var reagent=new net.minecraft.world.entity.item.ItemEntity(level,center.x,center.y,center.z,new ItemStack(net.minecraft.world.item.Items.COOKED_BEEF));
        reagent.setThrower(caster.getUUID());reagent.setNoGravity(true);reagent.setDeltaMovement(Vec3.ZERO);reagent.setPickUpDelay(100);level.addFreshEntity(reagent);
        helper.runAfterDelay(55,()->{
            helper.assertTrue(caster.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),"Resistance did not affect caster on the plane");
            helper.assertTrue(ally.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),"Resistance missed an ally within two blocks");
            helper.assertTrue(!outsider.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),"Resistance leaked beyond its two-block radius");
            field.discard();caster.discard();ally.discard();outsider.discard();helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=90)
    public static void stationary_field_fires_from_plane_after_caster_moves(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(3,2,3)).getCenter();
        var caster=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BoltCaster"));
        var aimer=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BoltAimer"));
        level.addNewPlayer(caster);level.addNewPlayer(aimer);caster.setNoGravity(true);aimer.setNoGravity(true);
        caster.setPos(center);aimer.setPos(center.add(1,0,0));aimer.setYRot(0);aimer.setXRot(0);
        var field=new SeelicField(Content.FIELD.get(),level);field.configure(caster,false,false);field.setPos(center);level.addFreshEntity(field);
        var reagent=new net.minecraft.world.entity.item.ItemEntity(level,center.x,center.y,center.z,new ItemStack(net.minecraft.world.item.Items.COAL));
        reagent.setThrower(caster.getUUID());reagent.setNoGravity(true);reagent.setDeltaMovement(Vec3.ZERO);reagent.setPickUpDelay(100);level.addFreshEntity(reagent);
        helper.runAfterDelay(8,()->caster.setPos(center.add(15,0,0)));
        helper.runAfterDelay(45,()->{
            var bolts=level.getEntitiesOfClass(SpellBolt.class,new net.minecraft.world.phys.AABB(center,center).inflate(12));
            helper.assertTrue(bolts.stream().anyMatch(bolt->Math.abs(bolt.getX()-center.x)<1&&bolt.getOwner()==caster),"Abhorrence followed the moved caster instead of launching from its stationary plane");
            field.discard();caster.discard();aimer.discard();for(var bolt:bolts)bolt.discard();helper.succeed();
        });
    }
    @GameTest(template="portal_test",timeoutTicks=260)
    public static void echo_shard_cast_through_open_frame_activates_portal(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));buildPortalFrame(level,corner,net.minecraft.core.Direction.EAST);
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ResonanceTest");
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,profile);level.addNewPlayer(player);
        Vec3 center=Vec3.atLowerCornerOf(corner).add(10.5,3.5,.5);
        player.moveTo(center.x,center.y-player.getEyeHeight(),center.z-12,0,0);player.setNoGravity(true);
        SeelicField field=new SeelicField(Content.FIELD.get(),level);field.configure(player,false,false);field.setPos(player.position());level.addFreshEntity(field);
        var reagent=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),new ItemStack(net.minecraft.world.item.Items.ECHO_SHARD));
        reagent.setThrower(player.getUUID());reagent.setNoGravity(true);reagent.setDeltaMovement(Vec3.ZERO);reagent.setPickUpDelay(200);level.addFreshEntity(reagent);
        helper.runAfterDelay(230,()->{
            helper.assertTrue(reagent.isRemoved(),"Echo shard was not consumed by its owner's field");
            helper.assertTrue(level.getBlockState(corner.offset(10,3,0)).is(Content.PORTAL.get()),"Casting directly through the open frame failed to activate the portal");player.discard();field.discard();helper.succeed();
        });
    }
    @GameTest(template="portal_test")
    public static void resonance_portal_waits_for_opening_animation(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));buildPortalFrame(level,corner,net.minecraft.core.Direction.EAST);
        helper.assertTrue(NativePortalBlock.activate(level,corner.offset(10,3,0)),"Valid frame interior was rejected");
        helper.assertTrue(level.getBlockState(corner.offset(10,3,0)).isAir(),"Portal opened immediately instead of playing the resonance sequence first");helper.succeed();
    }
    @GameTest(template="portal_test")
    public static void resonance_portal_reloads_without_duplicate_or_early_open(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));buildPortalFrame(level,corner,net.minecraft.core.Direction.EAST);
        NativePortalBlock.activate(level,corner.offset(10,3,0));
        var bounds=new net.minecraft.world.phys.AABB(corner,corner.offset(22,8,1));
        var opening=level.getEntitiesOfClass(PortalActivation.class,bounds).get(0);
        for(int tick=0;tick<50;tick++)opening.tick();
        NativePortalBlock.activate(level,corner.offset(15,5,0));
        helper.assertTrue(level.getEntitiesOfClass(PortalActivation.class,bounds).size()==1&&opening.age()==50,"Repeated cast duplicated or restarted the opening");
        var saved=new CompoundTag();opening.saveWithoutId(saved);opening.discard();
        var loaded=Content.PORTAL_ACTIVATION.get().create(level);loaded.load(saved);level.addFreshEntity(loaded);
        for(int tick=50;tick<121;tick++)loaded.tick();
        helper.assertTrue(level.getBlockState(corner.offset(10,3,0)).isAir(),"Portal surface appeared before the third collapse");loaded.tick();
        helper.assertTrue(level.getBlockState(corner.offset(10,3,0)).is(Content.PORTAL.get()),"Saved sequence did not finish on tick 122");
        helper.assertTrue(!NativePortalBlock.activate(level,corner.offset(10,3,0)),"Open portal restarted its opening animation");helper.succeed();
    }
    @GameTest(template="portal_test")
    public static void resonance_portal_aborts_when_frame_breaks(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));buildPortalFrame(level,corner,net.minecraft.core.Direction.EAST);
        NativePortalBlock.activate(level,corner.offset(10,3,0));
        var opening=level.getEntitiesOfClass(PortalActivation.class,new net.minecraft.world.phys.AABB(corner,corner.offset(22,8,1))).get(0);
        for(int tick=0;tick<40;tick++)opening.tick();level.setBlockAndUpdate(corner.offset(0,3,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());opening.tick();
        helper.assertTrue(opening.isRemoved()&&level.getBlockState(corner.offset(10,3,0)).isAir(),"Broken frame continued its opening");
        helper.assertTrue(!NativePortalBlock.activate(level,corner.offset(10,3,0)),"An incomplete frame was accepted");helper.succeed();
    }
    @GameTest(template="portal_test")
    public static void resonance_crosses_z_frame_from_both_sides(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(3,1,1));buildPortalFrame(level,corner,net.minecraft.core.Direction.SOUTH);
        var player=helper.makeMockPlayer();Vec3 center=Vec3.atLowerCornerOf(corner).add(.5,3.5,10.5);
        ResonanceSpell.fire(level,player,center.add(-10,0,0),new Vec3(1,0,0));
        ResonanceSpell.fire(level,player,center.add(10,0,0),new Vec3(-1,0,0));
        var openings=level.getEntitiesOfClass(PortalActivation.class,new net.minecraft.world.phys.AABB(corner,corner.offset(1,8,22)));
        helper.assertTrue(openings.size()==1&&openings.get(0).axis()==net.minecraft.core.Direction.SOUTH,"East/west shots through a Z-axis frame did not share one opening");helper.succeed();
    }
    @GameTest(template="empty")
    public static void resonance_uses_source_particle_geometry_and_portal_timing(GameTestHelper helper){
        var ray=ResonanceEffects.rayPlan(Vec3.ZERO,new Vec3(0,0,1));
        helper.assertTrue(ray.size()==70&&ray.stream().allMatch(p->p.type()==net.minecraft.core.particles.ParticleTypes.SONIC_BOOM)&&ray.get(69).at().z==69,"Resonance lost its original 70 sonic-boom samples");
        for(int age=1;age<=3;age++)helper.assertTrue(ResonanceEffects.geometry("burst",age,Vec3.ZERO,new Vec3(0,0,1)).size()==(age==1?400:144),"Original transmutation geometry was truncated");
        for(int age:new int[]{0,16,32,90,106,122}){
            var plan=ResonanceEffects.portalPlan(Vec3.ZERO,net.minecraft.core.Direction.Axis.X,age);
            helper.assertTrue(plan.stream().anyMatch(p->p.type()==net.minecraft.core.particles.ParticleTypes.FLASH&&p.count()==(age<90?10:100)),"Missing portal flash at source tick "+age);
        }
        for(int wave=0;wave<3;wave++){
            int collapse=90+wave*16;
            helper.assertTrue(ResonanceEffects.portalPlan(Vec3.ZERO,net.minecraft.core.Direction.Axis.X,collapse).stream().anyMatch(p->p.type()==net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER),"Collapse explosion missing");
            for(int tick=0;tick<10;tick++)helper.assertTrue(ResonanceEffects.shakeStrength(collapse+tick,2500)==(wave+1)*.5f,"Shake timing/strength changed");
            helper.assertTrue(ResonanceEffects.shakeStrength(collapse+10,0)==0&&ResonanceEffects.shakeStrength(collapse,2501)==0,"Shake exceeded duration or radius");
        }
        int swirl=0;for(int tick=1;tick<=20;tick++)swirl+=ResonanceEffects.swirlPlan(Vec3.ZERO,tick).size();
        helper.assertTrue(swirl>=1200&&swirl<=3300&&ResonanceEffects.swirlPlan(Vec3.ZERO,21).isEmpty(),"Soul swirl lifetime or strand emission is wrong");
        var plan=ResonanceEffects.portalPlan(Vec3.ZERO,net.minecraft.core.Direction.Axis.X,45);
        helper.assertTrue(plan.size()==432,"Three portal resonators must emit twelve 36-point circles");
        for(var axis:java.util.List.of(net.minecraft.core.Direction.Axis.X,net.minecraft.core.Direction.Axis.Z))for(int age=0;age<=146;age++)ResonanceEffects.portalPlan(Vec3.ZERO,axis,age);
        for(int[] sample:new int[][]{{1,130},{4,460},{6,540}})helper.assertTrue(ResonanceEffects.geometry("shockwave",sample[0],Vec3.ZERO,new Vec3(0,-1,0)).size()==sample[1],"Final source shockwave is missing at frame "+sample[0]);
        helper.succeed();
    }
    @GameTest(template="portal_test")
    public static void resonance_accepts_veins_but_rejects_solid_obstructions(GameTestHelper helper){
        var level=helper.getLevel();var corner=helper.absolutePos(new BlockPos(1,1,3));
        for(var axis:java.util.List.of(net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.SOUTH)){
            buildPortalFrame(level,corner,axis);
            var covered=corner.relative(axis,1).above(3);
            level.setBlock(covered,net.minecraft.world.level.block.Blocks.SCULK_VEIN.defaultBlockState(),2);
            helper.assertTrue(NativePortalBlock.activate(level,corner.relative(axis,10).above(3)),"Vein coverage prevented activation");
            var opening=level.getEntitiesOfClass(PortalActivation.class,new net.minecraft.world.phys.AABB(corner,corner.offset(23,9,23))).stream().filter(e->e.matches(corner,axis)).findFirst().orElseThrow();
            for(int tick=0;tick<PortalActivation.OPEN_TICK;tick++)opening.tick();
            helper.assertTrue(level.getBlockState(covered).is(Content.PORTAL.get()),"Opening did not replace vein coverage");opening.discard();
            level.setBlock(covered,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(!NativePortalBlock.frame(level,corner,axis),"Solid obstruction was accepted");
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_phases_without_suffocation_but_takes_combat_damage(GameTestHelper helper){
        var boss=helper.spawn(Content.SERPENT.get(),new BlockPos(3,3,3));keepBossLoadedForTest(helper,boss);boss.setNoAi(true);
        float health=boss.getHealth();
        helper.assertTrue(!boss.hurt(helper.getLevel().damageSources().inWall(),4)&&boss.getHealth()==health,"Serpent took suffocation damage while phasing");
        helper.assertTrue(boss.hurt(helper.getLevel().damageSources().mobAttack(helper.makeMockPlayer()),4)&&boss.getHealth()<health,"Serpent became immune to combat damage");
        boss.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void emissary_four_phase_attack_pools_keep_source_roles(GameTestHelper helper){
        java.util.Set<String> p1=new java.util.HashSet<>(),p2=new java.util.HashSet<>(),p3Close=new java.util.HashSet<>(),p3Far=new java.util.HashSet<>(),p4Ground=new java.util.HashSet<>(),p4Flying=new java.util.HashSet<>();
        for(int roll=0;roll<100;roll++){
            p1.add(NativeBoss.selectEmissaryAttack(1,10,roll,false).animation());
            p2.add(NativeBoss.selectEmissaryAttack(2,10,roll,false).animation());
            p3Close.add(NativeBoss.selectEmissaryAttack(3,4,roll,false).animation());
            p3Far.add(NativeBoss.selectEmissaryAttack(3,20,roll,false).animation());
            p4Ground.add(NativeBoss.selectEmissaryAttack(4,10,roll,false).animation());
            p4Flying.add(NativeBoss.selectEmissaryAttack(4,10,roll,true).animation());
        }
        helper.assertTrue(p1.equals(java.util.Set.of("phase1.attack.lightning_javelin","phase1.attack.pillars","phase1.attack.meteor_shower","phase1.attack.wingbeat")),"Phase one pool differs from source: "+p1);
        helper.assertTrue(p2.containsAll(java.util.Set.of("phase2.attack.strafeleft","phase2.attack.straferight","phase2.attack.celestial_smite","phase2.attack.flame_geysers","phase2.attack.sword_plunge","phase2.attack.wing_blast")),"Phase two lost movement or attack roles: "+p2);
        helper.assertTrue(p3Close.contains("phase3.attack.frontkick")&&p3Far.contains("phase3.attack.run")&&p3Close.contains("phase3.attack.flipfar"),"Phase three distance split is incomplete");
        helper.assertTrue(p4Ground.equals(java.util.Set.of("phase4_idle_transition_flying","phase4.attack.godrays","phase4.attack.fireball","phase4.attack.divine_blast")),"Ground phase-four pool differs: "+p4Ground);
        helper.assertTrue(p4Flying.equals(java.util.Set.of("phase4_flying_transition_idle","phase4.attack.apocalypse_rain","phase4.attack.corkscrew","phase4.attack.grapple")),"Flying phase-four pool differs: "+p4Flying);
        helper.succeed();
    }
    private static void buildPortalFrame(net.minecraft.server.level.ServerLevel level,BlockPos corner,net.minecraft.core.Direction axis){
        for(int x=0;x<=21;x++)for(int y=0;y<=7;y++)level.setBlockAndUpdate(corner.relative(axis,x).above(y),(x==0||x==21||y==0||y==7)?net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
    private static int isolatedTestAreaIndex;
    private static final net.minecraft.server.level.TicketType<BlockPos> TEST_AREA_TICKET=
        net.minecraft.server.level.TicketType.create("nyctocosm_test_area",java.util.Comparator.comparingLong(BlockPos::asLong));
    private static Vec3 isolatedTestOrigin(GameTestHelper helper,int y){
        // Source selectors reach 300 blocks; each fixture owns a separate arena outside the runner's structures.
        return new Vec3(10000+(isolatedTestAreaIndex++)*1024+.5,y,10000.5);
    }
    private static void inLoadedTestArea(GameTestHelper helper,Vec3 center,int radius,Runnable assertions){
        var level=helper.getLevel();BlockPos anchor=BlockPos.containing(center);
        var chunks=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=Math.floorDiv(anchor.getX()-radius,16);x<=Math.floorDiv(anchor.getX()+radius,16);x++)
            for(int z=Math.floorDiv(anchor.getZ()-radius,16);z<=Math.floorDiv(anchor.getZ()+radius,16);z++){
                var chunk=new net.minecraft.world.level.ChunkPos(x,z);chunks.add(chunk);
                level.getChunkSource().addRegionTicket(TEST_AREA_TICKET,chunk,2,anchor,true);
                level.getChunk(x,z);
            }
        Runnable cleanup=()->{
            AABB owned=new AABB(center.x-radius,level.getMinBuildHeight(),center.z-radius,
                center.x+radius,level.getMaxBuildHeight(),center.z+radius);
            for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,owned))entity.discard();
            for(var chunk:chunks)level.getChunkSource().removeRegionTicket(TEST_AREA_TICKET,chunk,2,anchor,true);
        };
        try{
            var field=GameTestHelper.class.getDeclaredField("testInfo");field.setAccessible(true);
            ((GameTestInfo)field.get(helper)).addListener(new GameTestListener(){
                public void testStructureLoaded(GameTestInfo info){}
                public void testPassed(GameTestInfo info){cleanup.run();}
                public void testFailed(GameTestInfo info){cleanup.run();}
            });
        }catch(ReflectiveOperationException failure){cleanup.run();throw new AssertionError("Could not register isolated arena cleanup",failure);}
        helper.startSequence().thenWaitUntil(()->{
            for(var chunk:chunks){
                BlockPos probe=new BlockPos(chunk.getMinBlockX(),anchor.getY(),chunk.getMinBlockZ());
                boolean loaded=level.areEntitiesLoaded(chunk.toLong()),ticking=level.isPositionEntityTicking(probe);
                helper.assertTrue(loaded&&ticking,
                    "Isolated arena chunk is not loaded and entity-ticking: "+chunk+" loaded="+loaded+" ticking="+ticking+" testTick="+helper.getTick());
            }
        }).thenExecute(assertions);
    }
    private static void prepareGeyserGround(net.minecraft.server.level.ServerLevel level,Vec3 center,int radius){
        int centerX=net.minecraft.util.Mth.floor(center.x),centerZ=net.minecraft.util.Mth.floor(center.z);
        for(int x=centerX-radius;x<=centerX+radius;x++)for(int z=centerZ-radius;z<=centerZ+radius;z++){
            int floorY=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z)-1;
            BlockPos floor=new BlockPos(x,floorY,z);
            if(level.getBlockState(floor).isAir()||!level.getFluidState(floor).isEmpty())level.setBlock(floor,Blocks.STONE.defaultBlockState(),2);
        }
    }
    private static net.minecraft.world.phys.AABB geyserTestBounds(net.minecraft.server.level.ServerLevel level,Vec3 center){
        return new net.minecraft.world.phys.AABB(center.x-48,level.getMinBuildHeight(),center.z-48,center.x+48,level.getMaxBuildHeight(),center.z+48);
    }
    @GameTest(template="portal_test")
    public static void resonance_activates_original_portal_frame(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));
        for(int x=0;x<=21;x++)for(int y=0;y<=7;y++)level.setBlockAndUpdate(corner.offset(x,y,0),(x==0||x==21||y==0||y==7)?net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.assertTrue(NativePortalBlock.activate(level,corner.offset(0,3,0)),"Original 20x6 frame was not activated");
        var opening=level.getEntitiesOfClass(PortalActivation.class,new net.minecraft.world.phys.AABB(corner,corner.offset(22,8,1))).get(0);
        for(int tick=0;tick<PortalActivation.OPEN_TICK;tick++)opening.tick();
        helper.assertTrue(level.getBlockState(corner.offset(10,3,0)).is(Content.PORTAL.get()),"Native portal surface missing");
        level.setBlockAndUpdate(corner.offset(0,3,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.assertTrue(!level.getBlockState(corner.offset(1,3,0)).is(Content.PORTAL.get()),"Broken frame retained active portal");
        helper.succeed();
    }
    @GameTest(template="portal_test")
    public static void forbiddance_cancels_active_portal_surface(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));
        buildPortalFrame(level,corner,net.minecraft.core.Direction.EAST);
        NativePortalBlock.open(level,corner,net.minecraft.core.Direction.EAST);
        BlockPos other=corner.offset(0,0,8);buildPortalFrame(level,other,net.minecraft.core.Direction.EAST);
        NativePortalBlock.open(level,other,net.minecraft.core.Direction.EAST);
        BlockPos center=corner.offset(10,3,0);
        helper.assertTrue(level.getBlockState(center).is(Content.PORTAL.get()),"Portal did not open before counterspell");
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(3,2,8));owner.setNoAi(true);
        var bolt=SpellBolt.launch(level,owner,Vec3.atCenterOf(center),new Vec3(1,0,0),0,Combat.Element.DISSONANCE,1,0).effect(SpellBolt.Effect.COUNTER,35);
        bolt.tick();
        helper.assertTrue(bolt.isRemoved()&&!level.getBlockState(center).is(Content.PORTAL.get()),"Forbiddance failed to close the active portal");
        helper.assertTrue(level.getBlockState(other.offset(10,3,0)).is(Content.PORTAL.get()),"Forbiddance closed a separate portal");
        owner.discard();helper.succeed();
    }
    @GameTest(template="portal_test")
    public static void forbiddance_cancels_portal_opening_before_surface(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));
        buildPortalFrame(level,corner,net.minecraft.core.Direction.EAST);
        var activation=PortalActivation.start(level,corner,net.minecraft.core.Direction.EAST);
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(3,2,8));owner.setNoAi(true);
        var bolt=SpellBolt.launch(level,owner,Vec3.atCenterOf(corner.offset(2,3,0)),new Vec3(1,0,0),0,Combat.Element.DISSONANCE,1,0).effect(SpellBolt.Effect.COUNTER,35);
        bolt.tick();
        helper.assertTrue(bolt.isRemoved()&&activation.isRemoved(),"Forbiddance failed to interrupt the opening portal near its edge");
        owner.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void forbiddance_cancels_enemy_magic_after_grace_and_ignores_arrows(GameTestHelper helper){
        var level=helper.getLevel();Vec3 center=helper.absolutePos(new BlockPos(3,3,3)).getCenter();
        var caster=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CounterCaster"));
        var enemy=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CounterEnemy"));
        level.addNewPlayer(caster);level.addNewPlayer(enemy);caster.setPos(center.add(-2,0,0));enemy.setPos(center.add(2,0,0));
        var field=new SeelicField(Content.FIELD.get(),level);field.configure(enemy,false,false);field.setPos(center.add(2,0,0));
        CompoundTag cast=new CompoundTag();field.saveWithoutId(cast);cast.putInt("Spell",Spell.ABHORRENCE.ordinal());cast.putInt("Cast",20);field.load(cast);level.addFreshEntity(field);
        var area=SpellArea.create(level,enemy,center.add(1,0,0),Spell.REFLECTANCE);
        var hostile=SpellBolt.launch(level,enemy,center.add(2,1,0),Vec3.ZERO,10,Combat.Element.VOID,0,0);
        var friendly=SpellBolt.launch(level,caster,center.add(1,1,0),Vec3.ZERO,10,Combat.Element.VOID,0,0);
        var arrow=new net.minecraft.world.entity.projectile.Arrow(level,enemy);arrow.setPos(center.add(2,0,0));level.addFreshEntity(arrow);
        var counter=SpellBolt.launch(level,caster,center,new Vec3(0,0,1),0,Combat.Element.DISSONANCE,1,0).effect(SpellBolt.Effect.COUNTER,35);
        level.setBlockAndUpdate(BlockPos.containing(center.add(0,0,1)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        counter.tick();counter.tick();
        helper.assertTrue(!counter.isRemoved()&&!hostile.isRemoved()&&field.castTicks()==20,"Forbiddance collided with a block or dispelled before tick three");
        CompoundTag saved=new CompoundTag();counter.saveWithoutId(saved);counter.discard();
        var restored=Content.BOLT.get().create(level);restored.load(saved);level.addFreshEntity(restored);restored.tick();
        helper.assertTrue(restored.isRemoved()&&hostile.isRemoved()&&area.isRemoved(),"Forbiddance did not cancel every enemy spell within the same pulse");
        helper.assertTrue(!field.isRemoved()&&field.castTicks()==0&&field.spell()==null,"Forbiddance destroyed a fixed plane instead of cancelling its cast");
        helper.assertTrue(!friendly.isRemoved()&&!arrow.isRemoved(),"Forbiddance removed the caster's spell or an ordinary arrow");
        field.discard();friendly.discard();arrow.discard();caster.discard();enemy.discard();helper.succeed();
    }
    @GameTest(template="empty")
    public static void original_dais_ritual_checks_lit_candles_and_stairs(GameTestHelper helper){
        var level=helper.getLevel();BlockPos anvil=helper.absolutePos(new BlockPos(4,3,4));var base=anvil.below();
        level.setBlockAndUpdate(anvil,net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState());
        level.setBlockAndUpdate(base,net.minecraft.world.level.block.Blocks.BEACON.defaultBlockState());
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            level.setBlockAndUpdate(base.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            if((Math.abs(x)==3&&Math.abs(z)<=1)||(Math.abs(z)==3&&Math.abs(x)<=1)||(Math.abs(x)==2&&Math.abs(z)==2))
                level.setBlockAndUpdate(base.offset(x,0,z),net.minecraft.world.level.block.Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT,true));
        }
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){
            level.setBlockAndUpdate(base.relative(direction),net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(base.relative(direction,2),net.minecraft.world.level.block.Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING,direction.getOpposite()));
        }
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlockAndUpdate(base.offset(x,0,z),net.minecraft.world.level.block.Blocks.WAXED_CUT_COPPER_SLAB.defaultBlockState());
        helper.assertTrue(DaisRitual.matches(level,anvil),"Original ritual layout rejected");
        level.setBlockAndUpdate(base.offset(3,0,0),net.minecraft.world.level.block.Blocks.CANDLE.defaultBlockState());
        helper.assertTrue(!DaisRitual.activate(level,anvil),"Unlit candle activated ritual");
        helper.assertTrue(level.getBlockState(anvil).is(net.minecraft.world.level.block.Blocks.ANVIL),"Failed ritual consumed anvil");
        level.setBlockAndUpdate(base.offset(3,0,0),net.minecraft.world.level.block.Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT,true));
        var east=base.offset(2,0,0);var stair=level.getBlockState(east);
        level.setBlockAndUpdate(east,stair.setValue(net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.EAST));
        helper.assertTrue(!DaisRitual.matches(level,anvil),"Outward-facing stairs accepted");
        level.setBlockAndUpdate(east,stair);
        helper.assertTrue(DaisRitual.activate(level,anvil)&&level.getBlockState(anvil).is(Content.DAIS.get()),"Native dais not created");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void lore_reveals_original_chapters_and_persists(GameTestHelper helper){
        var player=helper.makeMockPlayer();var lore=(LoreItem)Content.get("ancient_journals");
        ItemStack first=new ItemStack(lore);lore.discover(player,first);
        helper.assertTrue(lore.chapter(first)==1,"First discovery did not bind chapter one");
        helper.assertTrue(lore.book(first).getTag().getList("pages",8).getString(0).contains("journal.1.page1"),"Original chapter text missing");
        lore.discover(player,first);helper.assertTrue(lore.chapter(first)==1,"Reading advanced an already bound volume");
        var replacement=helper.makeMockPlayer();NativeEvents.copyPersistentProgress(player,replacement);
        ItemStack second=new ItemStack(lore);lore.discover(replacement,second);
        helper.assertTrue(lore.chapter(second)==2,"Progress lost on player replacement");
        var loaded=ItemStack.of(second.save(new CompoundTag()));helper.assertTrue(lore.chapter(loaded)==2,"Book chapter not saved");
        int count=0;
        for(String id:new String[]{"ancient_journals","catechism","dreams","axiotopology"}){
            var item=(LoreItem)Content.get(id);count+=item.chapterCount();
            for(int chapter=1;chapter<=item.chapterCount();chapter++)helper.assertTrue(!item.book(item.chapterStack(chapter)).getTag().getList("pages",8).isEmpty(),"Empty chapter: "+id+"/"+chapter);
        }
        helper.assertTrue(count==11,"Not all source chapter variants imported");helper.succeed();
    }
    @GameTest(template="empty")
    public static void chinese_catalog_covers_every_book_component(GameTestHelper helper)throws Exception{
        com.google.gson.JsonObject chinese;
        try(var input=java.util.Objects.requireNonNull(NativeGameTests.class.getResourceAsStream("/assets/nyctocosm/lang/zh_cn.json"));
            var reader=new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)){
            chinese=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
        }
        String[] books={"ancient_journals","ancient_journals_1","ancient_journals_2","ancient_journals_3","ancient_journals_4","ancient_journals_5",
            "catechism","catechism_1","catechism_2","dreams","dreams_1","dreams_2","axiotopology","axiotopology_1","axiotopology_2",
            "adherent_serpent","adherent_sorcerer","adherent_emissary"};
        var pattern=java.util.regex.Pattern.compile("\\\"translate\\\":\\\"([^\\\"]+)\\\"");
        int translated=0,pages=0;
        for(String book:books){
            String path="/data/nyctocosm/lore/"+book+".snbt";
            String source;
            try(var input=java.util.Objects.requireNonNull(NativeGameTests.class.getResourceAsStream(path),path)){
                source=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            }
            var matcher=pattern.matcher(source);
            while(matcher.find()){
                String key=matcher.group(1);
                if(!key.startsWith("item.nyctocosm."))continue;
                helper.assertTrue(chinese.has(key),"Missing Chinese book component: "+key);
                String value=chinese.get(key).getAsString();
                helper.assertTrue(!value.isBlank(),"Blank Chinese book component: "+key);
                translated++;
                if(key.contains(".page")){
                    helper.assertTrue(value.codePoints().anyMatch(c->c>=0x3400&&c<=0x9fff),"Book page did not receive Chinese text: "+key);
                    pages++;
                }
            }
        }
        helper.assertTrue(translated>=70&&pages>=40,"Book translation coverage unexpectedly small");helper.succeed();
    }
    @GameTest(template="empty")
    public static void travel_rejects_hazards_and_blocked_landings(GameTestHelper helper){
        var level=helper.getLevel();var player=helper.makeMockPlayer();BlockPos floor=helper.absolutePos(new BlockPos(3,1,3));
        Vec3 at=Vec3.atBottomCenterOf(floor.above());
        level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.assertTrue(SafeTravel.canStand(level,player,at),"Clear solid landing rejected");
        level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState());
        helper.assertTrue(!SafeTravel.canStand(level,player,at),"Magma landing accepted");
        level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.above(),net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
        helper.assertTrue(!SafeTravel.canStand(level,player,at),"Lava landing accepted");
        level.setBlockAndUpdate(floor.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.assertTrue(!SafeTravel.canStand(level,player,at),"Suffocating landing accepted");
        level.setBlockAndUpdate(floor.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.assertTrue(!SafeTravel.canStand(level,player,at),"Unsupported landing accepted");helper.succeed();
    }
    @GameTest(template="empty")
    public static void absorption_heals_dealer_not_victim(GameTestHelper helper){
        var player=helper.makeMockPlayer();player.setHealth(10);player.getInventory().add(new ItemStack(Content.get("artifact_absorption")));
        var target=helper.spawn(EntityType.COW,new BlockPos(3,2,3));
        helper.assertTrue(Combat.hit(player,target,5,Combat.Element.VOID),"Void hit rejected");
        helper.assertTrue(Math.abs(player.getHealth()-11)<.001,"Absorption did not heal 20 percent of outgoing void damage");
        var events=new NativeEvents();var received=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,Combat.source(helper.getLevel(),target,Combat.Element.VOID),5);
        events.hurt(received);
        helper.assertTrue(received.getAmount()==5&&player.getHealth()==11,"Absorption incorrectly reduced incoming damage or healed victim");helper.succeed();
    }

    @GameTest(template="empty",batch="flame_slash_frames",timeoutTicks=20)
    public static void flame_slash_emits_two_tracked_wavefronts_at_source_frames_2_and_13(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=flameSlashTestOrigin(helper,new BlockPos(4,8,4));
        var boss=emissaryFixture(helper,3);boss.setPos(origin);boss.setYRot(0);
        var target=emissaryPlayer(helper,origin.add(0,0,24));
        boss.startEmissaryAttackForTest(target,"phase3.attack.flame_slash");
        emissaryCelestialFrame(helper,boss,target,1);
        helper.assertTrue(level.getEntitiesOfClass(EmissaryFlameSlashWave.class,new AABB(origin,origin).inflate(8)).isEmpty(),
            "Flame Slash emitted before source animation frame 2");
        emissaryCelestialFrame(helper,boss,target,2);
        var first=level.getEntitiesOfClass(EmissaryFlameSlashWave.class,new AABB(origin,origin).inflate(8));
        helper.assertTrue(first.size()==1&&first.get(0).ownerForTest().equals(boss.getUUID()),
            "Source frame 2 did not create exactly one owned first wavefront; found="+first.size()+", age="+boss.emissaryAttackAge()+", animation="+boss.animation()+", attack="+boss.isPerformingEmissary("phase3.attack.flame_slash")+", boss="+boss.position()+", origin="+origin+", nearbyEntities="+level.getEntitiesOfClass(EmissaryFlameSlashWave.class,new AABB(boss.position(),boss.position()).inflate(16)).size());
        helper.assertTrue(Math.abs(first.get(0).getYRot()-60)<.01&&Math.abs(first.get(0).getXRot())<.01,
            "First wavefront did not inherit source yaw +60 and horizontal pitch");
        emissaryCelestialFrame(helper,boss,target,12);
        helper.assertTrue(level.getEntitiesOfClass(EmissaryFlameSlashWave.class,new AABB(origin,origin).inflate(8)).size()==1,
            "Second wavefront spawned before source animation frame 13");
        emissaryCelestialFrame(helper,boss,target,13);
        var both=level.getEntitiesOfClass(EmissaryFlameSlashWave.class,new AABB(origin,origin).inflate(8));
        helper.assertTrue(both.size()==2,"Source frame 13 did not create the second independent wavefront");
        helper.assertTrue(both.stream().anyMatch(wave->Math.abs(wave.getYRot()+60)<.01),
            "Second wavefront did not inherit source yaw -60");
        target.discard();boss.discard();for(var wave:both)wave.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="flame_slash_lifetime",timeoutTicks=1200)
    public static void flame_slash_advances_point_8_and_keeps_twenty_point_radius_two_surface_for_40_steps(GameTestHelper helper){
        Vec3 origin=isolatedTestOrigin(helper,300);
        inLoadedTestArea(helper,origin,48,()->verifyFlameSlashLifetime(helper,origin));
    }
    private static void verifyFlameSlashLifetime(GameTestHelper helper,Vec3 origin){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-40,-4,-40)),BlockPos.containing(origin.add(40,4,40))))
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        var boss=emissaryFixture(helper,3);boss.setPos(origin);
        var wave=EmissaryFlameSlashWave.launch(level,boss,true);
        Vec3 spawned=wave.position();
        wave.tick();
        helper.assertTrue(wave.ageForTest()==1&&Math.abs(wave.position().distanceTo(spawned)-.8)<.001,
            "Flame Slash must move exactly 0.8 blocks on its first source subtick; delta="+wave.position().distanceTo(spawned)+", position="+wave.position()+", yaw="+wave.getYRot()+", online="+level.players().size());
        for(int sample=0;sample<EmissaryFlameSlashWave.SURFACE_SAMPLES;sample++)
            helper.assertTrue(Math.abs(wave.position().distanceTo(wave.sampleForTest(sample))-2)<1.0e-5,
                "Wavefront sample "+sample+" drifted from the source radius 2 ring");
        CompoundTag saved=new CompoundTag();wave.saveWithoutId(saved);
        var restored=Content.EMISSARY_FLAME_SLASH_WAVE.get().create(level);restored.load(saved);
        helper.assertTrue(restored.ownerForTest().equals(boss.getUUID())&&restored.ageForTest()==1
                &&Math.abs(restored.getYRot()-wave.getYRot())<.01,
            "Flame Slash owner, age, or heading did not survive entity save/load");
        restored.discard();
        for(int tick=1;tick<EmissaryFlameSlashWave.SOURCE_LIFETIME;tick++)wave.tick();
        helper.assertTrue(wave.ageForTest()==40&&wave.endingForTest()&&!wave.isRemoved(),
            "Source wave must perform its fortieth move before entering its one-tick impact phase");
        wave.tick();
        helper.assertTrue(wave.isRemoved(),"Flame Slash wave did not resolve and clean up after its impact phase");
        boss.discard();helper.succeed();
    }

    @GameTest(template="empty",batch="flame_slash_contact",timeoutTicks=20)
    public static void flame_slash_samples_damage_once_per_player_and_wall_contact_only_ends_wave(GameTestHelper helper){
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        Vec3 origin=flameSlashTestOrigin(helper,new BlockPos(4,8,4));
        var boss=emissaryFixture(helper,3);boss.setPos(origin);
        var victim=emissaryPlayer(helper,origin.add(0,2,0));
        victim.setHealth(200);victim.hurtTime=0;victim.invulnerableTime=0;
        var wave=Content.EMISSARY_FLAME_SLASH_WAVE.get().create(level);wave.configureForTest(boss);
        wave.setPos(origin);wave.setYRot(0);wave.setXRot(0);level.addFreshEntity(wave);
        int contacts=wave.captureSurfaceContactsForTest(level);
        helper.assertTrue(contacts==1&&wave.pendingTargetCountForTest()==1,
            "The source ring's upper radius-2 sample did not capture its occupied player cell exactly once");
        float health=victim.getHealth();wave.resolveContactsForTest(level);
        helper.assertTrue(victim.getHealth()<health&&health-victim.getHealth()<=EmissaryFlameSlashWave.SOURCE_DAMAGE,
            "Wave surface contact failed to apply one Dissonance hit through Combat/Forge; health="+health+"→"+victim.getHealth()+", canHit="+Combat.canHit(boss,victim)+", owner="+level.getEntity(boss.getUUID())+", hurt="+victim.hurtTime+", invulnerable="+victim.invulnerableTime);

        victim.addTag("emissarydebug");
        var wallWave=Content.EMISSARY_FLAME_SLASH_WAVE.get().create(level);wallWave.configureForTest(boss);
        wallWave.setPos(origin.add(40,0,0));wallWave.setYRot(0);wallWave.setXRot(0);
        BlockPos wall=BlockPos.containing(wallWave.position().add(0,0,.8));
        level.setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.addFreshEntity(wallWave);wallWave.tick();
        helper.assertTrue(wallWave.endingForTest()&&wallWave.pendingTargetCountForTest()==0,
            "A non-passthrough block must end the wave without inventing a player impact");
        wallWave.tick();
        helper.assertTrue(wallWave.isRemoved(),"Wall-terminated wave did not clean up");
        helper.assertTrue(level.getBlockState(wall).is(net.minecraft.world.level.block.Blocks.STONE),
            "Flame Slash incorrectly replaced a world block with fire");
        level.setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        victim.discard();boss.discard();helper.succeed();
    }
    private static Vec3 flameSlashTestOrigin(GameTestHelper helper,BlockPos local){
        Vec3 origin=celestialTestOrigin(helper,local).add(2048,0,2048);
        var level=(net.minecraft.server.level.ServerLevel)helper.getLevel();
        int chunkX=BlockPos.containing(origin).getX()>>4,chunkZ=BlockPos.containing(origin).getZ()>>4;
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)
            level.getChunkSource().getChunk(chunkX+dx,chunkZ+dz,net.minecraft.world.level.chunk.ChunkStatus.FULL,true);
        return origin;
    }
}
