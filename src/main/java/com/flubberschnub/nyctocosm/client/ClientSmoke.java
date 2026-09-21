package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in developer visual smoke test. Explicitly excluded from release JARs. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class ClientSmoke {
    private static int ticks;
    private static int bootTicks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("nyctocosm.clientSmoke")||event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null){
            bootTicks++;
            if(bootTicks==300){
                System.out.println("NATIVE_SMOKE_SCREEN: "+(mc.screen==null?"null":mc.screen.getClass().getName()));
                Screenshot.grab(mc.gameDirectory,"native-smoke-startup.png",mc.getMainRenderTarget(),message->{});
            }
            if(bootTicks>320&&mc.screen!=null){
                for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button button){
                    String label=button.getMessage().getString();
                    if(label.equals("Proceed")||label.equals("I know what I'm doing!")){button.onPress();break;}
                }
            }
            if(bootTicks>1800)mc.stop();
            return;
        }
        ticks++;
        if(ticks==5){mc.options.pauseOnLostFocus=false;ClientSmokeLanguages.verify();}
        if(ticks==10||ticks==210||ticks==410){
            int kind=ticks==10?1:ticks==210?2:0;
            mc.setScreen(null);mc.options.hideGui=true;
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var level=server.overworld();
                var player=server.getPlayerList().getPlayers().get(0);player.setGameMode(GameType.CREATIVE);
                for(Entity entity:level.getAllEntities())if(entity instanceof NativeBoss||entity instanceof SeelicField)entity.discard();
                for(int x=0;x<=32;x++)for(int z=-16;z<=32;z++)level.setBlockAndUpdate(new BlockPos(x,200,z),Blocks.SMOOTH_STONE.defaultBlockState());
                player.teleportTo(level,kind==0?0:16.5,kind==0?209:kind==2?204:201,kind==0?-2:kind==2?-10:0.5,kind==0?-65:0,kind==0?12:-4);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Content.get("seelic_scythe")));
                var type=kind==1?Content.SORCERER:kind==2?Content.EMISSARY:Content.SERPENT;
                NativeBoss boss=type.get().create(level);boss.moveTo(16.5,201,8.5,180,0);boss.setNoAi(true);boss.setNoGravity(true);level.addFreshEntity(boss);
                if(kind==2){var tag=new net.minecraft.nbt.CompoundTag();boss.saveWithoutId(tag);tag.putInt("NativePhase",2);boss.load(tag);boss.setHealth(900);}
                if(kind==1){var field=new SeelicField(Content.FIELD.get(),level);field.configure(player,false,false);field.setPos(12,201,6);level.addFreshEntity(field);}
                level.setDayTime(6000);
            });
        }
        if(ticks==180||ticks==380||ticks==580){
            String name=ticks==180?"sorcerer":ticks==380?"emissary":"serpent";
            if(!BossMusicController.isPlaying(mc))throw new IllegalStateException("Native boss music did not start: "+name);
            Screenshot.grab(mc.gameDirectory,"native-smoke-"+name+".png",mc.getMainRenderTarget(),message->{});
        }
        if(ticks==610){
            mc.options.hideGui=false;
            try{
                var selected=net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");selected.setAccessible(true);selected.set(null,Content.TAB.get());
            }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot select native tab in development smoke",e);}
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,mc.level.enabledFeatures(),true));
            System.out.println("NATIVE_SMOKE_TAB_ITEMS: "+Content.TAB.get().getDisplayItems().size());
        }
        if(ticks==650)Screenshot.grab(mc.gameDirectory,"native-smoke-creative.png",mc.getMainRenderTarget(),message->{});
        if(ticks==670&&mc.screen!=null)mc.screen.mouseScrolled(0,0,-10);
        if(ticks==695)Screenshot.grab(mc.gameDirectory,"native-smoke-creative-bottom.png",mc.getMainRenderTarget(),message->{});
        if(ticks==710){
            mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var pos=player.blockPosition().below();
                player.level().setBlockAndUpdate(pos,Content.DAIS.get().defaultBlockState());
                Content.DAIS.get().use(player.level().getBlockState(pos),player.level(),pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,null);
                if(player.containerMenu instanceof DaisMenu menu){menu.getSlot(0).set(new ItemStack(Content.get("seelic_essence"),40));menu.getSlot(1).set(new ItemStack(Content.get("nightmare_scale"),52));}
            });
        }
        if(ticks==750){System.out.println("NATIVE_SMOKE_DAIS: "+mc.screen);Screenshot.grab(mc.gameDirectory,"native-smoke-dais.png",mc.getMainRenderTarget(),message->{});}
        if(ticks==768)ClientSmokeLanguages.use("zh_cn");
        if(ticks==770){
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);player.closeContainer();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,((LoreItem)Content.get("ancient_journals")).chapterStack(1));
                Content.get("ancient_journals").use(player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            });
        }
        if(ticks==805){System.out.println("NATIVE_SMOKE_BOOK: "+mc.screen);Screenshot.grab(mc.gameDirectory,"native-smoke-book.png",mc.getMainRenderTarget(),message->{});}
        if(ticks==820)ClientSmokeLanguages.restore();
        if(ticks==840)ClientSmokeLanguages.show("zh_cn");
        if(ticks==870)Screenshot.grab(mc.gameDirectory,"native-smoke-language-zh.png",mc.getMainRenderTarget(),message->{});
        if(ticks==880)ClientSmokeLanguages.show("en_us");
        if(ticks==910)Screenshot.grab(mc.gameDirectory,"native-smoke-language-en.png",mc.getMainRenderTarget(),message->{});
        if(ticks==920)ClientSmokeLanguages.show("fr_fr");
        if(ticks==950)Screenshot.grab(mc.gameDirectory,"native-smoke-language-fallback.png",mc.getMainRenderTarget(),message->{});
        if(ticks==960){
            ClientSmokeLanguages.restore();mc.setScreen(null);mc.options.hideGui=false;
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                var level=(net.minecraft.server.level.ServerLevel)player.level();
                for(Entity entity:level.getAllEntities())if(entity instanceof NativeBoss||entity instanceof SpellBolt)entity.discard();
                player.teleportTo(level,16.5,202,0.5,0,0);
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Content.get("nullfissure")));
                Content.get("nullfissure").use(player.level(),player,net.minecraft.world.InteractionHand.OFF_HAND);
            });
        }
        if(ticks==965)Screenshot.grab(mc.gameDirectory,"native-smoke-offhand-nullfissure.png",mc.getMainRenderTarget(),message->{});
        if(ticks==985)mc.getSingleplayerServer().execute(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Content.get("semblanceofdoom")));
            Content.get("semblanceofdoom").use(player.level(),player,net.minecraft.world.InteractionHand.OFF_HAND);
        });
        if(ticks==995)Screenshot.grab(mc.gameDirectory,"native-smoke-offhand-semblance.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1020){
            ClientSmokeLanguages.restore();mc.setScreen(null);mc.options.hideGui=true;
            mc.getSingleplayerServer().execute(()->{
                try{
                    var server=mc.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
                    for(Entity entity:level.getAllEntities())if(entity instanceof NativeBoss||entity.getTags().contains("native_smoke_target"))entity.discard();
                    player.teleportTo(level,16.5,202,.5,0,-4);
                    NativeBoss boss=Content.SORCERER.get().create(level);boss.moveTo(16.5,201,9.5,180,0);boss.setNoGravity(true);level.addFreshEntity(boss);
                    var target=net.minecraft.world.entity.EntityType.COW.create(level);target.moveTo(16.5,201,-2.5,0,0);target.setInvulnerable(true);target.setNoAi(true);target.setNoGravity(true);target.addTag("native_smoke_target");level.addFreshEntity(target);boss.setTarget(target);
                    var attacks=NativeBoss.class.getDeclaredField("SORCERER_A");attacks.setAccessible(true);
                    var plant=NativeBoss.class.getDeclaredMethod("spawnNullpointForTest",net.minecraft.server.level.ServerLevel.class);plant.setAccessible(true);plant.invoke(boss,level);
                    var begin=NativeBoss.class.getDeclaredMethod("beginAttack",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,NativeBoss.Attack.class);begin.setAccessible(true);
                    begin.invoke(boss,level,target,((NativeBoss.Attack[])attacks.get(null))[0]);
                }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot start Sorcerer keyframe smoke",exception);}
            });
        }
        if(ticks==1051)Screenshot.grab(mc.gameDirectory,"native-smoke-sorcerer-nullpoint-hover.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1061)Screenshot.grab(mc.gameDirectory,"native-smoke-sorcerer-halo-charge.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1096)Screenshot.grab(mc.gameDirectory,"native-smoke-sorcerer-halo-fire.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1120){
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                for(Entity entity:level.getAllEntities())if(entity instanceof NativeBoss||entity instanceof NullpointStaff||entity instanceof IceSpikeWave)entity.discard();
                player.teleportTo(level,16.5,202,-8.5,0,-3);
                NativeBoss boss=Content.SORCERER.get().create(level);boss.moveTo(16.5,201,8.5,180,0);boss.setNoAi(true);boss.setNoGravity(true);level.addFreshEntity(boss);
                NullpointStaff staff=Content.NULLPOINT.get().create(level);staff.bind(boss);staff.moveTo(15.8,201,7.6,180,0);level.addFreshEntity(staff);staff.emit();
                IceSpikeWave.spawn(level,boss,staff.position().add(0,2,0));
            });
        }
        if(ticks==1140)Screenshot.grab(mc.gameDirectory,"native-smoke-sorcerer-nullpoint-icespikes.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1180){
            mc.getSingleplayerServer().execute(()->{
                try{
                    var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    for(Entity entity:level.getAllEntities())if(entity instanceof NativeBoss||entity instanceof NullpointStaff||entity instanceof IceSpikeWave||entity instanceof SerpentBeamHazard||entity.getTags().contains("native_smoke_target"))entity.discard();
                    player.teleportTo(level,35.5,213,-14.5,45,12);
                    NativeBoss serpent=Content.SERPENT.get().create(level);serpent.moveTo(16.5,205,17.5,180,0);level.addFreshEntity(serpent);
                    var target=net.minecraft.world.entity.EntityType.COW.create(level);target.moveTo(16.5,202,1.5,0,0);target.setInvulnerable(true);target.setNoAi(true);target.setNoGravity(true);target.addTag("native_smoke_target");level.addFreshEntity(target);serpent.setTarget(target);
                    var start=NativeBoss.class.getDeclaredMethod("startSerpentAttackForTest",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.entity.LivingEntity.class,int.class);start.setAccessible(true);start.invoke(serpent,level,target,2);
                }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot start Serpent laser smoke",exception);}
            });
        }
        if(ticks==1255)Screenshot.grab(mc.gameDirectory,"native-smoke-serpent-laser.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1280){
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                for(Entity entity:level.getAllEntities())if(entity instanceof NativeBoss||entity instanceof SerpentBeamHazard)entity.discard();
                player.teleportTo(level,35.5,213,-14.5,45,12);
                NativeBoss serpent=Content.SERPENT.get().create(level);serpent.moveTo(16.5,205,18.5,180,0);serpent.setNoAi(true);level.addFreshEntity(serpent);
                for(int n=0;n<20;n++){double angle=n*Math.PI*2/20,radius=3+(n%5)*2.5;SerpentBeamHazard.spawn(level,serpent,new net.minecraft.world.phys.Vec3(16.5+Math.cos(angle)*radius,201,7.5+Math.sin(angle)*radius),100);}
            });
        }
        if(ticks==1290)Screenshot.grab(mc.gameDirectory,"native-smoke-serpent-beam-charge.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1300)Screenshot.grab(mc.gameDirectory,"native-smoke-serpent-beam-fire.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1320){
            mc.getSingleplayerServer().execute(()->{
                try{
                    var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    for(Entity entity:level.getAllEntities())if(entity!=null&&(entity instanceof NativeBoss||entity instanceof SerpentBeamHazard||entity.getTags().contains("native_smoke_target")))entity.discard();
                    player.teleportTo(level,35.5,211,7.5,90,8);
                    var target=net.minecraft.world.entity.EntityType.COW.create(level);target.moveTo(16.5,205,7.5,0,0);target.setInvulnerable(true);target.setNoAi(true);target.setNoGravity(true);target.addTag("native_smoke_target");level.addFreshEntity(target);
                    NativeBoss serpent=Content.SERPENT.get().create(level);net.minecraft.world.phys.Vec3 aim=target.getEyePosition();serpent.moveTo(aim.x,aim.y,aim.z-8,0,0);level.addFreshEntity(serpent);
                    var start=NativeBoss.class.getDeclaredMethod("startSerpentDiveForTest",net.minecraft.world.entity.LivingEntity.class);start.setAccessible(true);start.invoke(serpent,target);
                }catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot start Serpent dive smoke",exception);}
            });
        }
        if(ticks==1324)Screenshot.grab(mc.gameDirectory,"native-smoke-serpent-dive-contact.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1332)Screenshot.grab(mc.gameDirectory,"native-smoke-serpent-dive-overshoot.png",mc.getMainRenderTarget(),message->{});
        if(ticks==1360){ClientSmokeLanguages.restore();mc.options.hideGui=false;mc.stop();}
    }
}
