package com.flubberschnub.nyctocosm;

import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Disposable visual check for selected alpha.18 repairs, excluded from the release jar. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class ClientSmokeBossRepair {
    private static boolean creating;
    private static int boot,tick,birth;
    private static NativeBoss boss;
    private static String scene="";
    private static final Set<Integer> SCENES=Set.of(30,200,410,560,760,960,1060,1200,1370);
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("nyctocosm.bossRepairSmoke")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null){
            if(mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button button&&Set.of("Proceed","I know what I'm doing!","Continue","Done").contains(button.getMessage().getString()))button.onPress();
            if(!creating&&mc.screen instanceof TitleScreen){
                creating=true;if(Files.exists(mc.gameDirectory.toPath().resolve("saves/BossRepair18")))throw new IllegalStateException("Fresh bossRepairRunDir required");
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("BossRepair18",new LevelSettings("BossRepair18",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(72604L,false,false),WorldPresets::createNormalWorldDimensions);
            }
            if(++boot>2400)throw new IllegalStateException("Client did not enter disposable boss world");return;
        }
        tick++;mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);mc.player.getAbilities().flying=true;mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(SCENES.contains(tick-2))mc.particleEngine.setLevel(mc.level);
        int frame=tick;mc.getSingleplayerServer().execute(()->scene(mc,frame));
        if(tick%10==0){int missiles=0,bosses=0;String clip="";for(Entity entity:mc.level.entitiesForRendering()){if(entity instanceof EmissarySeekingMissile)missiles++;if(entity instanceof NativeBoss b){bosses++;clip=b.animation();}}
            System.out.println("BOSS_REPAIR18_TRACE tick="+tick+" missiles="+missiles+" bosses="+bosses+" clip="+clip+" particles="+mc.particleEngine.countParticles());}
        if(Set.of(48,52,58,75,99,265,279,303,337,344,357,455,468,483,509,512,520,598,602,608,635,795,820,895,930,974,983,998,1080,1110,1140,1203,1206,1210,1390,1399,1410).contains(tick))Screenshot.grab(mc.gameDirectory,"boss-repair18-"+tick+".png",mc.getMainRenderTarget(),message->{});
        if(tick==1480){System.out.println("BOSS_REPAIR18_CAPTURE_COMPLETE");mc.stop();}
    }
    private static void scene(Minecraft mc,int frame){
        var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        if(frame==1){level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);for(int x=-40;x<=40;x++)for(int z=-40;z<=45;z++)level.setBlock(new BlockPos(x,200,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);}
        if(SCENES.contains(frame)){
            var entities=new ArrayList<Entity>();level.getAllEntities().forEach(entities::add);
            for(Entity entity:entities)if(entity instanceof NativeBoss||entity instanceof EmissarySeekingMissile||entity instanceof NullpointStaff||entity instanceof SpellBolt)entity.discard();
            player.setGameMode(GameType.SURVIVAL);player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.getAbilities().invulnerable=true;player.onUpdateAbilities();
            player.teleportTo(level,0,frame==410?205:203,-22,0,frame==410?-5:0);
            scene=switch(frame){case 30->"missiles";case 200->"transition";case 410->"emissary_death";case 560->"voidwave";case 760->"sorcerer_death";case 960->"ground_slam";case 1060->"serpent_mist";case 1200->"voidplane";default->"staff_recall";};birth=frame;
            boss=(frame==1060?Content.SERPENT:frame==560||frame==760||frame>=1200?Content.SORCERER:Content.EMISSARY).get().create(level);boss.moveTo(0,201,4,180,0);boss.setNoAi(true);boss.setNoGravity(true);
            var state=new CompoundTag();boss.saveWithoutId(state);state.putInt("NativePhase",frame==560?2:frame==760?1:3);state.putInt("Remaining",0);boss.load(state);level.addFreshEntity(boss);
            if(scene.equals("missiles")||scene.equals("ground_slam"))boss.startEmissaryAttackForTest(player,"phase3.attack."+(scene.equals("missiles")?"seeking_missiles":"ground_slam"));
            if(scene.equals("transition")){boss.saveWithoutId(state);state.putString("Animation","phase3_transition");state.putInt("Remaining",181);state.putInt("AnimationAge",0);boss.load(state);}
            if(scene.equals("voidwave")){boss.startSorcererAttackForTest(level,player,"voidwave");boss.spawnNullpointForTest(level);}
            if(scene.equals("serpent_mist"))player.teleportTo(level,-45,235,-45,-45,25);
            if(scene.equals("voidplane")){
                boss.spawnNullpointForTest(level);
                try{var summon=NativeBoss.class.getDeclaredMethod("summonSorcererPlane",net.minecraft.server.level.ServerLevel.class);summon.setAccessible(true);summon.invoke(boss,level);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            }
            if(scene.equals("staff_recall"))boss.spawnNullpointForTest(level);
            if(scene.endsWith("death"))boss.hurt(level.damageSources().genericKill(),2000);
            System.out.println("BOSS_REPAIR18_SCENE "+scene);
        }
        if(boss==null||boss.isRemoved()||frame<=birth)return;int age=frame-birth;
        try{
            if(scene.equals("transition")&&age<=181)boss.customServerAiStep();
            else if(scene.equals("serpent_mist")&&age<=120)boss.customServerAiStep();
            else if(scene.equals("staff_recall")&&age==5)boss.nullpoint().orElseThrow().recall();
            else if((scene.equals("missiles")&&boss.animation().equals("phase3.attack.seeking_missiles")||scene.equals("ground_slam")&&boss.animation().equals("phase3.attack.ground_slam"))&&age<=50){
                var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);ageField.setInt(boss,age);
                var perform=NativeBoss.class.getDeclaredMethod("performEmissary",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);perform.invoke(boss,level,player);
            }else if(scene.equals("voidwave")&&age<=65){
                var ageField=NativeBoss.class.getDeclaredField("attackAge");ageField.setAccessible(true);ageField.setInt(boss,age);
                var perform=NativeBoss.class.getDeclaredMethod("perform",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);perform.invoke(boss,level,player);
            }
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        if(scene.equals("sorcerer_death")&&(age==138||age==179))System.out.println("BOSS_REPAIR18_DEATH_HOLD age="+age+" removed="+boss.isRemoved()+" clip="+boss.animation());
    }
    private ClientSmokeBossRepair(){}
}
