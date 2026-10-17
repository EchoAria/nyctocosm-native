package com.flubberschnub.nyctocosm;

import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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

/** Fresh disposable world, manually driven source frames; never packaged or installed. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class ClientSmokeSorcererRepair {
    private static boolean creating;
    private static int boot,tick,birth;
    private static NativeBoss boss;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("nyctocosm.sorcererRepairSmoke")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null){
            if(mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button button){String label=button.getMessage().getString();if(label.equals("Proceed")||label.equals("I know what I'm doing!")||label.equals("Continue")||label.equals("Done"))button.onPress();}
            if(!creating&&mc.screen instanceof TitleScreen){creating=true;if(Files.exists(mc.gameDirectory.toPath().resolve("saves/SorcererRepair")))throw new IllegalStateException("Fresh sorcererRepairRunDir required");var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);mc.createWorldOpenFlows().createFreshLevel("SorcererRepair",new LevelSettings("SorcererRepair",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(72603L,false,false),WorldPresets::createNormalWorldDimensions);}
            if(++boot>2400)throw new IllegalStateException("Client did not enter disposable world");return;
        }
        tick++;mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);mc.player.getAbilities().flying=true;mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(java.util.Set.of(22,172,362,442,542,652).contains(tick))mc.particleEngine.setLevel(mc.level);
        int frame=tick;mc.getSingleplayerServer().execute(()->scene(mc,frame));
        if(tick%20==0){int hail=0,blades=0,beams=0;for(Entity entity:mc.level.entitiesForRendering()){if(entity instanceof SorcererHail)hail++;if(entity instanceof FrozenBlade)blades++;if(entity instanceof SorcererBeamNode)beams++;}System.out.println("SORCERER_REPAIR_TRACE tick="+tick+" hail="+hail+" blades="+blades+" beams="+beams+" particles="+mc.particleEngine.countParticles());}
        if(java.util.Set.of(55,85,100,125,211,232,270,292,310,385,393,398,472,483,492,577,583,590,660,670,675,686,691).contains(tick))Screenshot.grab(mc.gameDirectory,"sorcerer-repair-"+tick+".png",mc.getMainRenderTarget(),message->{});
        if(tick==760){System.out.println("SORCERER_REPAIR_CAPTURE_COMPLETE");mc.stop();}
    }
    private static void scene(Minecraft mc,int frame){
        var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        if(frame==1){level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);for(int x=-36;x<=36;x++)for(int z=-36;z<=45;z++)level.setBlock(new BlockPos(x,200,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();}
        if(java.util.Set.of(20,170,360,440,540,650).contains(frame)){
            var entities=new java.util.ArrayList<Entity>();level.getAllEntities().forEach(entities::add);for(Entity entity:entities)if(entity instanceof NativeBoss||entity instanceof SorcererHail||entity instanceof FrozenBlade||entity instanceof SorcererBeamNode||entity instanceof NullpointStaff||entity instanceof SpellBolt)entity.discard();
            String clip=switch(frame){case 20->"halobeam";case 170->"shardhail";case 360->"frozenblades";case 440->"voidbeam";case 540->"dashstrike";default->"airdash";};
            player.teleportTo(level,0,frame==170?220:frame>=540?204:206,-18,0,frame==170?30:0);
            boss=Content.SORCERER.get().create(level);boss.moveTo(0,201,4,180,0);boss.setNoAi(true);boss.setNoGravity(true);var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",frame>=440?2:1);boss.load(tag);level.addFreshEntity(boss);boss.startSorcererAttackForTest(level,player,clip);birth=frame;
            System.out.println("SORCERER_REPAIR_SCENE "+clip);
        }
        if(boss!=null&&frame>birth&&frame-birth<=132){
            try{var age=NativeBoss.class.getDeclaredField("attackAge");age.setAccessible(true);age.setInt(boss,frame-birth);var perform=NativeBoss.class.getDeclaredMethod("perform",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);perform.invoke(boss,level,player);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        if(frame==579)player.teleportTo(level,8,204,-18,-20,0);
    }
}
