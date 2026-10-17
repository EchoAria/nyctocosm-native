package com.flubberschnub.nyctocosm;

import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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

/** Disposable client reproduction. Never opens or copies an existing save. Excluded from the JAR. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class ClientSmokeVisualRepair {
    private static boolean creating;
    private static int boot,tick;
    private static NativeBoss boss;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("nyctocosm.visualRepairSmoke")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null){
            if(boot%100==0){
                System.out.println("VISUAL_BOOT screen="+(mc.screen==null?"none":mc.screen.getClass().getName()));
                Screenshot.grab(mc.gameDirectory,"visual-boot-"+boot+".png",mc.getMainRenderTarget(),message->{});
            }
            if(mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button button){
                String label=button.getMessage().getString();
                if(label.equals("Proceed")||label.equals("I know what I'm doing!")||label.equals("Continue")||label.equals("Done"))button.onPress();
            }
            if(!creating&&mc.screen instanceof TitleScreen){
                creating=true;
                if(Files.exists(mc.gameDirectory.toPath().resolve("saves/VisualRepair")))throw new IllegalStateException("Use a fresh visualRepairRunDir");
                var rules=new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("VisualRepair",new LevelSettings("VisualRepair",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(72602L,false,false),WorldPresets::createNormalWorldDimensions);
            }
            if(++boot>2400)throw new IllegalStateException("Client did not enter its disposable world");
            return;
        }
        tick++;mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);
        if(tick==1058||tick==1202||tick==1498||tick==1558||tick==1638)mc.particleEngine.setLevel(mc.level); // Remove long-lived particles from the previous disposable scene.
        mc.player.getAbilities().flying=true;mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(tick==5){
            var rig=com.flubberschnub.nyctocosm.client.RigLibrary.get("ajsorcerer");
            var smear=rig.bones().stream().filter(bone->bone.name().equals("smear1")).findFirst().orElseThrow();
            var model=com.flubberschnub.nyctocosm.client.RigLibrary.animatedModel(rig,"smear1","initial_spawn",30,smear.model());
            if(!model.getPath().equals("item/ajsorcerer/staffless/smear1"))throw new IllegalStateException("Opening arc is still visible: "+model);
            System.out.println("VISUAL_VARIANT_ASSERTIONS_PASS");
        }
        int frame=tick;mc.getSingleplayerServer().execute(()->scene(mc,frame));
        if(tick%20==0){
            int portalAge=-1;int swords=0;
            for(Entity entity:mc.level.entitiesForRendering()){
                if(entity instanceof PortalActivation activation){activation.canUpdate(false);portalAge=activation.age();}
                if(entity instanceof EmissarySwordPlunge)swords++;
            }
            String appearance="";for(Entity entity:mc.level.entitiesForRendering())if(entity instanceof NativeBoss boss&&boss.kind==2)appearance=" phase="+boss.phase()+" head="+boss.emissaryModelVariant("head")+" sword="+boss.emissaryModelVariant("sword");
            System.out.println("VISUAL_TRACE tick="+tick+" portalAge="+portalAge+" swords="+swords+" particles="+mc.particleEngine.countParticles()+appearance+" glowing="+mc.player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING));
        }
        if(java.util.Set.of(70,110,180,220,290,330,410,440,490,535,585,635,690,730,780,840,895,910,920,937,941,960,1020,1080,1100,1205,1215).contains(tick))
            Screenshot.grab(mc.gameDirectory,"visual-repair-"+tick+".png",mc.getMainRenderTarget(),message->{});
        if(java.util.Set.of(1335,1370,1390,1440,1480,1503,1510,1563,1570,1650,1717,1735,1745,1865,1880).contains(tick))Screenshot.grab(mc.gameDirectory,"feedback-repair-"+tick+".png",mc.getMainRenderTarget(),message->{});
        if(tick==2000){System.out.println("VISUAL_REPAIR_CAPTURE_COMPLETE");mc.stop();}
    }
    private static void scene(Minecraft mc,int frame){
        var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        if(frame==1){
            level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);
            for(int x=-35;x<=35;x++)for(int z=-35;z<=45;z++)level.setBlock(new BlockPos(x,200,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
        }
        if(frame==20||frame==140||frame==260){
            clear(level);int phase=frame==20?1:frame==140?2:3;
            player.teleportTo(level,0,219,-26,0,0);
            boss=Content.EMISSARY.get().create(level);boss.moveTo(0,219,0,180,0);boss.setNoAi(true);boss.setNoGravity(true);
            var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",phase);tag.putString("Animation","phase"+phase+"_idle");tag.putInt("Remaining",0);tag.putBoolean("EmissaryHoverEstablished",true);boss.load(tag);level.addFreshEntity(boss);
        }
        if(frame==380){
            clear(level);player.teleportTo(level,0,201,-7,0,-4);
            boss=Content.SORCERER.get().create(level);boss.moveTo(0,201,4,180,0);level.addFreshEntity(boss);boss.beginEntrance();
        }
        if(frame==465){
            clear(level);player.teleportTo(level,0,214,-32,0,12);
            boss=Content.EMISSARY.get().create(level);boss.moveTo(20,201,0,180,0);boss.setNoAi(true);level.addFreshEntity(boss);
            var sword=Content.EMISSARY_SWORD_PLUNGE.get().create(level);
            var tag=new CompoundTag();sword.saveWithoutId(tag);tag.putInt("Age",65);tag.putInt("Substeps",250);sword.load(tag);sword.moveTo(0,210,0,0,0);level.addFreshEntity(sword);
        }
        if(frame==520){
            clear(level);player.teleportTo(level,11,212,-12,0,12);BlockPos corner=new BlockPos(0,201,20);
            for(int x=0;x<=21;x++)for(int y=0;y<=7;y++)level.setBlock(corner.relative(Direction.EAST,x).above(y),(x==0||x==21||y==0||y==7)?Blocks.REINFORCED_DEEPSLATE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            for(int y=1;y<=6;y++)level.setBlock(corner.relative(Direction.EAST,1).above(y),Blocks.SCULK_VEIN.defaultBlockState().setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.WEST),true),2);
            PortalActivation.start(level,corner,Direction.EAST);
        }
        if(frame==670){
            clear(level);player.teleportTo(level,-18,216,-15,-45,15);
            boss=Content.SERPENT.get().create(level);boss.moveTo(0,210,0,90,0);boss.setNoAi(true);boss.setNoGravity(true);level.addFreshEntity(boss);
        }
        if(frame==710){
            clear(level);player.teleportTo(level,-25,219,-42,0,8);
            boss=Content.SERPENT.get().create(level);boss.moveTo(48,214,10,-90,0);boss.setNoAi(true);boss.setNoGravity(true);
            var tag=new CompoundTag();boss.saveWithoutId(tag);var body=new CompoundTag();body.putDouble("X",48);body.putDouble("Y",214);body.putDouble("Z",10);
            var links=new net.minecraft.nbt.ListTag();
            for(int i=1;i<=41;i++){var link=new CompoundTag();link.putDouble("X",48-i*4);link.putDouble("Y",214);link.putDouble("Z",10);link.putFloat("Yaw",-90);links.add(link);}
            body.put("Links",links);tag.put("SerpentBody",body);boss.load(tag);level.addFreshEntity(boss);
        }
        if(frame==755)for(BlockPos at:BlockPos.betweenClosed(32,208,0,63,223,15))level.setBlock(at,Blocks.STONE.defaultBlockState(),2);
        if(frame==820){
            clear(level);player.teleportTo(level,0,206,-12,0,-10);
            boss=Content.SORCERER.get().create(level);boss.moveTo(0,201,4,180,0);boss.setNoAi(true);boss.setNoGravity(true);
            var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",2);boss.load(tag);level.addFreshEntity(boss);
            boss.hurt(level.damageSources().genericKill(),10000);
        }
        if(frame==980){
            clear(level);player.teleportTo(level,0,225,-35,0,15);
            boss=Content.SERPENT.get().create(level);boss.moveTo(20,250,30,90,0);boss.setNoAi(true);boss.setNoGravity(true);level.addFreshEntity(boss);
            for(int i=0;i<20;i++)SerpentBeamHazard.spawn(level,boss,new net.minecraft.world.phys.Vec3(i%5*7-14,201,i/5*7),65);
        }
        if(frame==1050||frame==1200){
            clear(level);player.teleportTo(level,0,206,-12,0,-10);
            boss=Content.SORCERER.get().create(level);boss.moveTo(0,201,4,180,0);boss.setNoAi(true);boss.setNoGravity(true);
            var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",frame==1050?1:2);boss.load(tag);level.addFreshEntity(boss);
            boss.startSorcererAttackForTest(level,player,frame==1050?"singularitycharge":"airdash");
        }
        if(frame>1200&&frame<=1266&&boss!=null){
            try{var age=NativeBoss.class.getDeclaredField("attackAge");age.setAccessible(true);age.setInt(boss,frame-1200);var perform=NativeBoss.class.getDeclaredMethod("perform",ServerLevel.class,net.minecraft.world.entity.LivingEntity.class);perform.setAccessible(true);perform.invoke(boss,level,player);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        if(frame==1320){
            clear(level);player.teleportTo(level,0,219,-26,0,0);
            player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=true;player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();
            boss=Content.EMISSARY.get().create(level);boss.moveTo(0,219,5,180,0);boss.setNoAi(true);boss.setNoGravity(true);
            var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",2);tag.putBoolean("EmissaryHoverEstablished",true);boss.load(tag);boss.setHealth(900);level.addFreshEntity(boss);
            boss.startEmissaryAttackForTest(player,"phase2.attack.sword_plunge");
        }
        if(frame>1320&&frame<=1381&&boss!=null)boss.customServerAiStep();
        if(frame==1420||frame==1460){
            clear(level);player.setGameMode(GameType.CREATIVE);player.teleportTo(level,0,204,-18,0,-6);
            boss=Content.EMISSARY.get().create(level);boss.moveTo(0,201,5,180,0);boss.setNoAi(true);boss.setNoGravity(true);
            var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",frame==1420?3:4);boss.load(tag);level.addFreshEntity(boss);
        }
        if(frame==1500||frame==1560){
            clear(level);player.teleportTo(level,0,201,4,0,65);
            var hand=frame==1500?net.minecraft.world.InteractionHand.MAIN_HAND:net.minecraft.world.InteractionHand.OFF_HAND;
            var item=new net.minecraft.world.item.ItemStack(Content.get(frame==1500?"soul_bottle":"planar_coil"));player.setItemInHand(hand,item);
            ((FocusItem)item.getItem()).use(level,player,hand);
            System.out.println("FOCUS_SOURCE_USE "+(frame==1500?"bottle":"coil"));
        }
        if(frame==1640){
            clear(level);player.teleportTo(level,0,201,4,0,65);
            var field=new SeelicField(Content.FIELD.get(),level);field.configure(player,false,false);field.setPos(player.position());level.addFreshEntity(field);
            var item=new net.minecraft.world.entity.item.ItemEntity(level,0,201,4,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLOWSTONE));item.setThrower(player.getUUID());item.setNoGravity(true);item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);item.setPickUpDelay(100);level.addFreshEntity(item);
        }
        if(frame==1700||frame==1850){
            clear(level);player.teleportTo(level,0,207,-38,0,6);player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable=true;player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();
            boss=Content.EMISSARY.get().create(level);boss.moveTo(0,201,5,180,0);boss.setNoAi(true);
            var tag=new CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",4);boss.load(tag);level.addFreshEntity(boss);
            boss.startEmissaryAttackForTest(player,frame==1700?"phase4.attack.godrays":"phase4.attack.divine_blast");
            System.out.println("FEEDBACK_RAY_SCENE "+(frame==1700?"godrays":"divine_blast"));
        }
        if((frame>1700&&frame<=1760||frame>1850&&frame<=1910)&&boss!=null)boss.customServerAiStep();
    }
    private static void clear(ServerLevel level){
        var entities=new java.util.ArrayList<Entity>();level.getAllEntities().forEach(entities::add);
        for(Entity entity:entities)if(entity instanceof NativeBoss||entity instanceof PortalActivation||entity instanceof EmissarySwordPlunge||entity instanceof NullpointStaff||entity instanceof SerpentBeamHazard||entity instanceof SpellBolt||entity instanceof SeelicField||entity instanceof SoulEntity||entity instanceof EmissaryHazard)entity.discard();
    }
}
