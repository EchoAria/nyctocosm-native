package com.flubberschnub.nyctocosm;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder(NativeMod.ID)
@PrefixGameTestTemplate(false)
public final class NativeGameTests {
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
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);
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
        boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());boss.setHealth(666);helper.getLevel().addFreshEntity(boss);
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
        boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());boss.setHealth(100);helper.getLevel().addFreshEntity(boss);
        helper.runAfterDelay(4,()->{helper.assertTrue(boss.phase()==3&&boss.nullpoint().isPresent(),"Low health skipped the required phase-three Nullpoint break");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void ice_spikes_hit_registered_mod_entity(GameTestHelper helper){
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(2,2,2));owner.setNoAi(true);owner.setNoGravity(true);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(2,2,4));target.setNoAi(true);target.setNoGravity(true);
        IceSpikeWave.spawn(helper.getLevel(),owner,owner.position());
        helper.runAfterDelay(20,()->{helper.assertTrue(target.getHealth()<target.getMaxHealth(),"Nullpoint ice spike missed a registered mod entity");helper.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"Ice spike slowness missing");helper.succeed();});
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
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(serpent);
        helper.assertTrue(serpent.getParts().length==20,"Serpent no longer has the source's 20 damage hitboxes");
        var target=helper.spawn(EntityType.COW,new BlockPos(6,2,6));serpent.startSerpentAttackForTest(helper.getLevel(),target,3);
        long beams=java.util.stream.StreamSupport.stream(helper.getLevel().getAllEntities().spliterator(),false).filter(SerpentBeamHazard.class::isInstance).map(SerpentBeamHazard.class::cast).filter(beam->beam.ownerId().filter(serpent.getUUID()::equals).isPresent()).count();
        helper.assertTrue(beams==20,"Serpent beam attack created "+beams+" columns instead of 20");
        helper.assertTrue(serpent.serpentAttack()==3&&serpent.serpentState()==1,"Serpent beam attack did not enter its twirl state");helper.succeed();
    }
    @GameTest(template="empty")
    public static void serpent_uses_source_speed_tiers_and_segment_spacing(GameTestHelper helper){
        helper.assertTrue(NativeBoss.serpentSpeedForTier(1)==1&&NativeBoss.serpentSpeedForTier(2)==2&&NativeBoss.serpentSpeedForTier(3)==3,
            "Serpent movement no longer uses the source 1/2/3-block speed tiers");
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(serpent);serpent.tick();
        double spacing=serpent.position().distanceTo(serpent.serpentRenderSegment(1,1));
        helper.assertTrue(spacing>=3.9&&spacing<=4.6,"Serpent's first visible segment is "+spacing+" blocks behind the head instead of the source spacing");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void serpent_normally_travels_at_source_two_blocks_per_tick(GameTestHelper helper){
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,5,4)).getCenter());helper.getLevel().addFreshEntity(serpent);
        Vec3 before=serpent.position();
        var target=helper.spawn(EntityType.COW,new BlockPos(8,2,8));target.setNoAi(true);serpent.setTarget(target);
        helper.runAfterDelay(7,()->{
            helper.assertTrue(serpent.position().distanceTo(before)>9,"Serpent did not cover source-speed distance in seven active ticks: "+serpent.position().distanceTo(before));
            double spacing=serpent.serpentRenderSegment(2,1).distanceTo(serpent.serpentRenderSegment(1,1));
            helper.assertTrue(spacing>=3.7&&spacing<=4.8,"A moving body segment snapped out of the source four-block spacing: "+spacing);helper.succeed();
        });
    }
    @GameTest(template="empty")
    public static void serpent_batched_client_samples_match_source_spacing(GameTestHelper helper){
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());serpent.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(serpent);serpent.tick();
        Vec3[] samples=serpent.serpentRenderSegments(.5f);
        helper.assertTrue(samples.length==41,"Serpent must render all 40 original body links in one path sampling pass");
        for(int i=0;i<samples.length;i++)helper.assertTrue(samples[i].distanceToSqr(serpent.serpentRenderSegment(i,.5f))<.0001,
            "Batched render diverged from the source path at segment "+i);
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void serpent_dive_passes_target_instead_of_sticking_to_it(GameTestHelper helper){
        var target=helper.spawn(EntityType.COW,new BlockPos(4,3,10));target.setNoAi(true);target.setNoGravity(true);target.setInvulnerable(true);
        NativeBoss serpent=Content.SERPENT.get().create(helper.getLevel());Vec3 aim=target.getEyePosition();serpent.setPos(aim.x,aim.y,aim.z-8);helper.getLevel().addFreshEntity(serpent);
        serpent.startSerpentDiveForTest(target);
        helper.runAfterDelay(9,()->{
            helper.assertTrue(serpent.distanceTo(target)>5,"Serpent Dive converged onto the target instead of carrying its momentum past them; distance="+serpent.distanceTo(target));
            helper.assertTrue(serpent.getZ()>target.getZ()+3,"Serpent did not overshoot the player along its source marker-guided heading: serpentZ="+serpent.getZ()+" targetZ="+target.getZ());helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=270)
    public static void living_nullpoint_permanently_shields_sorcerer(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);
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
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);
        NullpointStaff staff=boss.spawnNullpointForTest(helper.getLevel());staff.hurt(helper.getLevel().damageSources().generic(),400);
        helper.runAfterDelay(3,()->helper.assertTrue(!boss.hurt(helper.getLevel().damageSources().generic(),20),"Sorcerer shield dropped before falltransition frame 25"));
        helper.runAfterDelay(32,()->{helper.assertTrue(boss.hurt(helper.getLevel().damageSources().generic(),20),"Sorcerer stayed shielded after falltransition frame 25");helper.succeed();});
    }
    @GameTest(template="empty")
    public static void meteor_tear_stays_active_for_six_source_waves(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);
        helper.assertTrue(boss.meteorWaveCount()==6&&boss.meteorBoltsPerWave()==20,"Source meteor tear must release six waves of twenty after the clip finishes");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160)
    public static void meteor_tear_continues_after_sorcerer_animation_ends(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);
        var target=helper.spawn(EntityType.COW,new BlockPos(7,2,7));target.setNoAi(true);boss.setTarget(target);
        boss.startSorcererAttackForTest(helper.getLevel(),target,"voidmeteor");
        helper.runAfterDelay(43,()->helper.assertTrue(boss.meteorWavesEmitted()>=1,"The tear never started its timed meteor waves"));
        helper.runAfterDelay(133,()->{helper.assertTrue(boss.meteorWavesEmitted()==6,"The tear stopped when the boss's 67-frame animation ended: waves="+boss.meteorWavesEmitted());helper.succeed();});
    }
    @GameTest(template="empty")
    public static void sorcerer_attack_openers_apply_source_debuffs_to_mod_entities(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,4)).getCenter());helper.getLevel().addFreshEntity(boss);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(7,2,7));target.setNoAi(true);
        boss.startSorcererAttackForTest(helper.getLevel(),target,"voidwave");
        helper.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)&&target.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),
            "Void wave opener did not impose the source's ten-second slowness and four-second darkness on a modded creature");helper.succeed();
    }
    @GameTest(template="empty")
    public static void singularity_charge_is_straight_not_seeking(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,3,4)).getCenter());helper.getLevel().addFreshEntity(boss);
        var target=helper.spawn(EntityType.COW,new BlockPos(10,2,10));target.setNoAi(true);boss.setTarget(target);
        boss.startSorcererAttackForTest(helper.getLevel(),target,"singularitycharge");
        try{
            var fire=NativeBoss.class.getDeclaredMethod("performSorcerer",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,SorcererTimeline.Event.class);
            fire.setAccessible(true);
            fire.invoke(boss,helper.getLevel(),target,SorcererTimeline.at("singularitycharge",44).get(0));
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot inspect Singularity Charge keyframe",exception);}
        var bolts=helper.getLevel().getEntitiesOfClass(SpellBolt.class,boss.getBoundingBox().inflate(4));
        helper.assertTrue(bolts.stream().anyMatch(bolt->boss.getUUID().equals(bolt.getOwner()==null?null:bolt.getOwner().getUUID())
            &&bolt.effect()==SpellBolt.Effect.DAMAGE&&Math.abs(bolt.getDeltaMovement().length()-.3)<.001),
            "Frame 44 must spawn a straight 0.3-block-per-tick bomb, not a homing seeking orb");helper.succeed();
    }
    @GameTest(template="empty")
    public static void sorcerer_void_beam_stops_at_nonpassthrough_wall(GameTestHelper helper){
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,3,2)).getCenter());helper.getLevel().addFreshEntity(boss);
        var target=helper.spawn(Content.DEATH_WALKER.get(),new BlockPos(4,3,10));target.setNoAi(true);boss.setTarget(target);
        for(int y=3;y<=9;y++)helper.setBlock(new BlockPos(4,y,7),net.minecraft.world.level.block.Blocks.STONE);
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
        var serpent=helper.spawn(Content.SERPENT.get(),new BlockPos(4,3,4));serpent.setNoAi(true);serpent.setNoGravity(true);serpent.hurt(helper.getLevel().damageSources().genericKill(),2000);
        helper.runAfterDelay(70,()->helper.assertTrue(!serpent.isRemoved(),"Serpent death sequence was cut short"));
        helper.runAfterDelay(105,()->{helper.assertTrue(serpent.isRemoved(),"Serpent remained after its source 100-tick defeat sequence");helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=155)
    public static void boss_defeat_rewards_participant_lore_and_advancement(GameTestHelper helper){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SorcererTester"));helper.getLevel().addNewPlayer(player);
        player.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());
        NativeBoss boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absolutePos(new BlockPos(4,2,2)).getCenter());boss.setHealth(5);helper.getLevel().addFreshEntity(boss);
        helper.assertTrue(Combat.hit(player,boss,10,Combat.Element.VOID),"Participant's killing blow was rejected");
        helper.assertTrue(boss.participantCount()==1,"Sorcerer did not record its participant");
        helper.runAfterDelay(140,()->{
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
    @GameTest(template="empty")
    public static void ash_loot_modifier_is_natively_registered(GameTestHelper helper){
        helper.assertTrue(Content.ASH_LOOT.isPresent()&&Content.ASH_LOOT.get()==NyctocosmicAshLoot.CODEC,"Nyctocosmic Ash loot modifier missing");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void sorcerer_death_animation_is_not_cut_off(GameTestHelper helper){
        var boss=helper.spawn(Content.SORCERER.get(),new BlockPos(3,2,3));boss.setNoAi(true);boss.setNoGravity(true);
        boss.hurt(helper.getLevel().damageSources().genericKill(),2000);
        helper.runAfterDelay(70,()->helper.assertTrue(!boss.isRemoved()&&boss.animation().equals("die"),"Sorcerer death clip was cut off at 60 ticks"));
        helper.runAfterDelay(138,()->{helper.assertTrue(boss.isRemoved(),"Dead sorcerer was not removed after full clip");helper.succeed();});
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
        var target=helper.spawn(Content.SORCERER.get(),new BlockPos(3,2,3));target.setNoAi(true);target.setNoGravity(true);
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
        Vec3 origin=owner.position().add(0,1,0);
        SpellBolt.launch(helper.getLevel(),owner,origin,target.position().add(0,1,0).subtract(origin),12,Combat.Element.VOID,1,0);
        helper.runAfterDelay(12,()->{helper.assertTrue(target.getHealth()<80,"Swept projectile collision did not hurt the mod entity");helper.succeed();});
    }
    @GameTest(template="empty")
    public static void native_items_and_saved_souls(GameTestHelper helper){
        helper.assertTrue(Content.ALL.size()==37,"Expected 31 original native items, dais and 5 spawn eggs");
        ItemStack bottle=new ItemStack(Content.get("soul_bottle"));
        helper.assertTrue(FocusItem.souls(bottle)==1,"Original starting soul missing");FocusItem.addSoul(bottle,4);
        CompoundTag saved=bottle.save(new CompoundTag());ItemStack loaded=ItemStack.of(saved);
        helper.assertTrue(FocusItem.souls(loaded)==5,"Soul count not saved");
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
        SeelicField field=new SeelicField(Content.FIELD.get(),helper.getLevel());field.configure(player,false,false);field.setPos(player.position());helper.getLevel().addFreshEntity(field);
        var foreign=new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),player.getX(),player.getY(),player.getZ(),new ItemStack(net.minecraft.world.item.Items.GLOWSTONE));
        foreign.setThrower(java.util.UUID.randomUUID());foreign.setNoGravity(true);foreign.setDeltaMovement(Vec3.ZERO);helper.getLevel().addFreshEntity(foreign);
        var own=new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),player.getX(),player.getY(),player.getZ(),new ItemStack(net.minecraft.world.item.Items.GLOWSTONE));
        own.setThrower(player.getUUID());own.setNoGravity(true);own.setDeltaMovement(Vec3.ZERO);own.setPickUpDelay(100);foreign.setPickUpDelay(100);helper.getLevel().addFreshEntity(own);
        helper.runAfterDelay(12,()->{
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Owned reagent did not cast Luminescence");
            helper.assertTrue(own.isRemoved(),"Casting did not consume the reagent");
            helper.assertTrue(!foreign.isRemoved(),"Field stole a foreign reagent");
            player.discard();field.discard();helper.succeed();
        });
    }
    @GameTest(template="portal_test")
    public static void resonance_activates_original_portal_frame(GameTestHelper helper){
        var level=helper.getLevel();BlockPos corner=helper.absolutePos(new BlockPos(1,1,3));
        for(int x=0;x<=21;x++)for(int y=0;y<=7;y++)level.setBlockAndUpdate(corner.offset(x,y,0),(x==0||x==21||y==0||y==7)?net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.assertTrue(NativePortalBlock.activate(level,corner.offset(0,3,0)),"Original 20x6 frame was not activated");
        helper.assertTrue(level.getBlockState(corner.offset(10,3,0)).is(Content.PORTAL.get()),"Native portal surface missing");
        level.setBlockAndUpdate(corner.offset(0,3,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.assertTrue(!level.getBlockState(corner.offset(1,3,0)).is(Content.PORTAL.get()),"Broken frame retained active portal");
        helper.succeed();
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
}
