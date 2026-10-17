package com.flubberschnub.nyctocosm;

import java.io.*;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import org.joml.Matrix4f;

/** Regressions for the reported alpha.18 combat and display faults; excluded from jars. */
@GameTestHolder(NativeMod.ID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class NativeGameTestsFeedback19 {
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void sorcerer_player_knockback_cannot_move_model(GameTestHelper helper){
        var boss=Content.SORCERER.get().create(helper.getLevel());boss.setPos(helper.absoluteVec(new Vec3(4,30,4)));boss.setNoAi(true);
        boss.setOnGround(true);boss.setDeltaMovement(Vec3.ZERO);boss.knockback(2,1,1);
        helper.assertTrue(boss.getDeltaMovement().lengthSqr()<1e-12,"Sorcerer moved under player knockback: "+boss.getDeltaMovement());
        boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void serpent_body_snapshot_does_not_inherit_head_packet_drift(GameTestHelper helper){
        var body=new SerpentBody();body.initialize(Vec3.ZERO);
        for(int i=1;i<=40;i++)body.tick(new Vec3(0,0,i),1);
        body.tick(new Vec3(1,0,40),1);
        Vec3 head=new Vec3(.5,0,40);
        for(int i=1;i<=41;i++){
            Vec3 expected=body.position(i,.5f,head);
            helper.assertTrue(expected.distanceToSqr(body.position(i,.5f,head.add(5,0,-5)))<1e-12,
                "Delayed head entity packet dragged remote segment "+i+" sideways");
        }
        helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void emissary_source_idle_keyframes_survive_renderer_decomposition(GameTestHelper helper)throws Exception{
        try(var stream=new GZIPInputStream(NativeGameTestsFeedback19.class.getResourceAsStream("/assets/nyctocosm/native_rigs/ajemissary.json.gz"));var reader=new InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){
            var root=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var frames=root.getAsJsonObject("animations").getAsJsonArray("phase1_idle");
            for(var frame:frames)for(var entry:frame.getAsJsonObject().getAsJsonObject("bones").entrySet()){
                var row=entry.getValue().getAsJsonArray();float[] values=new float[16];for(int i=0;i<16;i++)values[i]=row.get(i).getAsFloat();
                var matrix=new Matrix4f().set(values).transpose();
                helper.assertTrue(matrix.equals(RigTransforms.interpolate(matrix,matrix,.5f),.001f),
                    "Folded idle matrix distorted: frame="+frame.getAsJsonObject().get("tick")+", bone="+entry.getKey());
            }
        }
        helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void emissary_distant_flight_does_not_choose_landing(GameTestHelper helper){
        helper.assertTrue(NativeBoss.selectEmissaryAttack(4,60.01,0,true)==null,"Source decided branch must keep flying beyond60");
        helper.assertTrue(NativeBoss.selectEmissaryAttack(4,60.01,75,false).animation().equals("phase4_idle_transition_flying"),"Distant ground form must take off");
        helper.assertTrue(NativeBoss.selectEmissaryAttack(4,60,0,true).animation().equals("phase4_flying_transition_idle"),"Source inclusive60 boundary lost");
        helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void emissary_flight_retargets_when_source_timer_expires(GameTestHelper helper)throws Exception{
        var level=helper.getLevel();var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(helper.absoluteVec(new Vec3(4,80,4)));
        var state=new CompoundTag();boss.saveWithoutId(state);state.putInt("NativePhase",4);state.putBoolean("EmissaryFlying",true);boss.load(state);
        var flying=NativeBoss.class.getDeclaredField("emissaryFlying");flying.setAccessible(true);flying.setBoolean(boss,true);
        var timer=NativeBoss.class.getDeclaredField("emissaryFlyTimer");timer.setAccessible(true);timer.setInt(boss,1);
        var point=NativeBoss.class.getDeclaredField("emissaryFlyTarget");point.setAccessible(true);point.set(boss,boss.position().add(0,20,50));
        var step=NativeBoss.class.getDeclaredMethod("tickEmissaryFlight",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);step.setAccessible(true);step.invoke(boss,level,null);
        helper.assertTrue(timer.getInt(boss)>=20&&timer.getInt(boss)<=59,"Reachable block737 timer20..59 was not restored: "+timer.getInt(boss));
        boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void sorcerer_sprint_enchanted_player_attack_damages_without_knockback(GameTestHelper helper){
        Vec3 at=helper.absoluteVec(new Vec3(4,30,4));var boss=Content.SORCERER.get().create(helper.getLevel());boss.setNoAi(true);boss.setPos(at);boss.setOnGround(true);
        var attacker=helper.makeMockSurvivalPlayer();attacker.setPos(at.add(0,0,-1));attacker.setSprinting(true);
        var sword=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);sword.enchant(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK,2);attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
        float before=boss.getHealth();boss.setDeltaMovement(Vec3.ZERO);attacker.attack(boss);
        helper.assertTrue(boss.getHealth()<before,"Knockback fix blocked the player's damage");
        helper.assertTrue(boss.getDeltaMovement().lengthSqr()<1e-12,"Sprint/enchantment moved the Sorcerer: "+boss.getDeltaMovement());
        boss.discard();attacker.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void sorcerer_entrance_hides_both_sides_of_smear_only(GameTestHelper helper){
        helper.assertTrue(!BossVisuals.visibleRigBone("ajsorcerer","initial_spawn","smear1"),"Entrance arc still renders");
        helper.assertTrue(BossVisuals.visibleRigBone("ajsorcerer","initial_spawn","group")&&BossVisuals.visibleRigBone("ajsorcerer","initial_spawn","righthand"),"Staff or hand was hidden along with the smear");
        helper.assertTrue(BossVisuals.visibleRigBone("ajsorcererm2","javelinswing","smear1"),"Attack smear was removed");helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void serpent_sharp_turn_uses_one_snapshot_for_head_body_and_angles(GameTestHelper helper){
        var server=new SerpentBody();server.initialize(Vec3.ZERO);server.tick(new Vec3(0,0,20),2,0,0);
        var client=new SerpentBody();client.receive(server.snapshot(),false);
        server.tick(new Vec3(2,1,20),2,-90,-15);client.receive(server.snapshot(),true);
        var before=new SerpentBody();before.load(server.save());
        for(int i=0;i<=41;i++){
            Vec3 expected=i==0?new Vec3(1,.5,20):new Vec3(0,0,0);
            if(i==0)helper.assertTrue(client.position(i,.5f,new Vec3(-4,0,17)).distanceToSqr(expected)<1e-12,"Head uses delayed entity coordinates instead of the body snapshot");
            helper.assertTrue(client.position(i,1,Vec3.ZERO).distanceToSqr(before.position(i,1,Vec3.ZERO))<1e-8,"Snapshot/save mismatch on link "+i);
        }
        helper.assertTrue(Math.abs(client.yaw(0,.5f)+45)<.001&&Math.abs(client.pitch(0,.5f)+7.5)<.001,"Head rotation does not share the body clock");helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void emissary_multiplayer_flytargets_apply_all_vanilla_facing_forks(GameTestHelper helper)throws Exception{
        var level=helper.getLevel();Vec3 at=helper.absoluteVec(new Vec3(4,80,4));var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(at);boss.setYRot(47);boss.setXRot(-12);
        var others=NativeBoss.class.getDeclaredField("emissaryOtherFlyTargets");others.setAccessible(true);
        @SuppressWarnings("unchecked") var points=(java.util.List<Vec3>)others.get(boss);points.add(at.add(12,8,-14));
        var target=NativeBoss.class.getDeclaredField("emissaryFlyTarget");target.setAccessible(true);target.set(boss,at.add(-9,12,17));
        var marker=net.minecraft.world.entity.EntityType.MARKER.create(level);marker.setPos(at);marker.setYRot(47);marker.setXRot(-12);level.addFreshEntity(marker);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withEntity(marker).withSuppressedOutput();
        for(Vec3 point:java.util.List.of(points.get(0),(Vec3)target.get(boss))){
            String command=String.format(java.util.Locale.ROOT,"execute at @s facing %.9f %.9f %.9f positioned ^ ^ ^0.5 rotated as @s positioned ^ ^ ^25 facing entity @s feet facing ^ ^ ^-1 positioned as @s run tp @s ~ ~ ~ ~ ~",point.x,point.y,point.z);
            level.getServer().getCommands().performPrefixedCommand(source,command);
        }
        var method=NativeBoss.class.getDeclaredMethod("turnToEmissaryFlyTargets",double.class);method.setAccessible(true);method.invoke(boss,25d);
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(boss.getYRot()-marker.getYRot()))<.001&&Math.abs(boss.getXRot()-marker.getXRot())<.001,"Only the last multiplayer waypoint affected flight");
        marker.discard();boss.discard();helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void guarded_idle_uses_source_held_arms_and_shield_wings_at_unchanged_body_scale(GameTestHelper helper)throws Exception{
        try(var input=new GZIPInputStream(NativeGameTestsFeedback19.class.getResourceAsStream("/assets/nyctocosm/native_rigs/ajemissary.json.gz"));var reader=new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)){
            var clips=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            var initial=clips.getAsJsonArray("phase1_idle").get(0).getAsJsonObject().getAsJsonObject("bones");
            var held=clips.getAsJsonArray("spawn").get(75).getAsJsonObject().getAsJsonObject("bones");
            var anchor=new Matrix4f(matrix(initial.getAsJsonArray("torso"))).mul(matrix(held.getAsJsonArray("torso")).invert());
            for(var row:clips.getAsJsonArray("phase1_idle_guarded")){
                var poses=row.getAsJsonObject().getAsJsonObject("bones");int frame=row.getAsJsonObject().get("tick").getAsInt();
                var original=clips.getAsJsonArray("phase1_idle").get(frame).getAsJsonObject().getAsJsonObject("bones");
                float bob=original.getAsJsonArray("torso").get(7).getAsFloat()-initial.getAsJsonArray("torso").get(7).getAsFloat();
                for(String arm:new String[]{"rightarm","rightforearm","righthand","leftarm","leftforearm","lefthand"}){
                    var expected=new Matrix4f(anchor).mul(matrix(held.getAsJsonArray(arm)));expected.m31(expected.m31()+bob);
                    helper.assertTrue(expected.equals(matrix(poses.getAsJsonArray(arm)),.00002f),"Held arm lost source pose75: "+arm);
                }
                var expectedWing=matrix(initial.getAsJsonArray("rightmiddlewing"));expectedWing.m31(expectedWing.m31()+bob);
                helper.assertTrue(expectedWing.equals(matrix(poses.getAsJsonArray("rightmiddlewing")),.00002f),"Idle shield wing expanded");
                helper.assertTrue(matrix(original.getAsJsonArray("torso")).equals(matrix(poses.getAsJsonArray("torso")),.00002f),"Guarded pose changed the body scale/position");
            }
            helper.assertTrue(Math.abs(matrix(clips.getAsJsonArray("phase1_idle_guarded").get(0).getAsJsonObject().getAsJsonObject("bones").getAsJsonArray("righthand")).m30())<1,"Idle hands are not held close");
        }
        helper.succeed();
    }
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void distant_natural_flight_chooses_no_attack_and_resets_source_attack_timer(GameTestHelper helper){
        var level=helper.getLevel();Vec3 at=NativeGameTestsBossRepair.origin(helper);var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);boss.setPos(at);
        var player=NativeGameTestsBossRepair.player(helper,at.add(0,0,70));
        var state=new CompoundTag();boss.saveWithoutId(state);state.putInt("NativePhase",4);state.putBoolean("EmissaryFlying",true);state.putInt("Cooldown",0);state.putInt("EmissaryFlyTimer",40);boss.load(state);boss.setTarget(player);level.addFreshEntity(boss);
        boss.customServerAiStep();boss.saveWithoutId(state);
        helper.assertTrue(boss.animation().equals("phase4_idle_flying")&&state.getInt("Remaining")==0,"Distant source AI started a ground/landing attack");
        helper.assertTrue(state.getInt("Cooldown")>=0&&state.getInt("Cooldown")<=29,"Source no-attack branch omitted AttackTimer0..29");
        boss.discard();player.discard();helper.succeed();
    }
    private static Matrix4f matrix(com.google.gson.JsonArray data){float[] values=new float[16];for(int i=0;i<16;i++)values[i]=data.get(i).getAsFloat();return new Matrix4f().set(values).transpose();}
    @GameTest(template="empty",batch="feedback19",timeoutTicks=30)
    public static void emissary_takeoff_and_landing_execute_source_root_displacements(GameTestHelper helper)throws Exception{
        var level=helper.getLevel();var boss=Content.EMISSARY.get().create(level);boss.setNoAi(true);Vec3 base=helper.absoluteVec(new Vec3(4,120,4));
        double surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,net.minecraft.util.Mth.floor(base.x),net.minecraft.util.Mth.floor(base.z));
        Vec3 at=new Vec3(base.x,Math.max(base.y,surface+40),base.z);boss.setPos(at);
        var target=helper.makeMockSurvivalPlayer();target.setPos(at.add(0,0,30));
        var state=new CompoundTag();boss.saveWithoutId(state);state.putInt("NativePhase",4);boss.load(state);
        var gravity=NativeBoss.class.getDeclaredMethod("updateEmissaryGravity");gravity.setAccessible(true);gravity.invoke(boss);
        helper.assertTrue(boss.isNoGravity(),"Source phase-four marker acquired ordinary ground-form gravity");
        var age=NativeBoss.class.getDeclaredField("attackAge");age.setAccessible(true);
        var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);
        boss.startEmissaryAttackForTest(target,"phase4_idle_transition_flying");
        for(int frame=1;frame<=5;frame++){age.setInt(boss,frame);perform.invoke(boss,level,target);helper.assertTrue(Math.abs(boss.getY()-at.y-frame*.80625)<1e-8,"Source takeoff leaf"+frame+" root lift was omitted: "+boss.getY());}
        boss.saveWithoutId(state);state.putInt("EmissaryFlySpeed",10);state.putBoolean("EmissaryFlying",true);boss.load(state);boss.setPos(at);
        boss.startEmissaryAttackForTest(target,"phase4_flying_transition_idle");
        for(int frame=1;frame<=4;frame++){age.setInt(boss,frame);perform.invoke(boss,level,target);}
        System.out.println("FEEDBACK19_LAND_TRACE surface="+surface+" startY="+at.y+" actualY="+boss.getY()+" expectedY="+(at.y-5*.80625));
        helper.assertTrue(Math.abs(boss.getY()-at.y+5*.80625)<1e-8,"Source landing root descent0..4 omitted: y="+boss.getY()+" start="+at.y+" surface="+surface);
        age.setInt(boss,6);perform.invoke(boss,level,target);boss.saveWithoutId(state);int speed=state.getInt("EmissaryFlySpeed");
        age.setInt(boss,7);perform.invoke(boss,level,target);boss.saveWithoutId(state);
        helper.assertTrue(state.getInt("EmissaryFlySpeed")==speed,"Landing decelerates at frame7 absent from source");
        for(int frame=8;frame<=11;frame++){age.setInt(boss,frame);perform.invoke(boss,level,target);}
        boss.saveWithoutId(state);helper.assertTrue(state.getInt("EmissaryFlySpeed")==3&&boss.isNoGravity(),"Landing source deceleration or marker gravity changed");
        boss.setPos(at.x,surface+1,at.z);boss.startEmissaryAttackForTest(target,"phase4_flying_transition_idle");
        for(int frame=1;frame<=4;frame++){age.setInt(boss,frame);perform.invoke(boss,level,target);}
        helper.assertTrue(Math.abs(boss.getY()-surface)<1e-8,"Landing buried native root below the safety surface");
        boss.discard();target.discard();helper.succeed();
    }
    private NativeGameTestsFeedback19(){}
}
