package com.flubberschnub.nyctocosm;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Development-only, real item -> reagent -> spell -> portal -> client sound/particle capture. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class ClientSmokeResonance {
    private static int ticks,boot,frames;
    private static final Set<String> SHAKES=java.util.concurrent.ConcurrentHashMap.newKeySet();
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void camera(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event){
        if(!Boolean.getBoolean("nyctocosm.resonanceSmoke"))return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof PortalActivation opening){
            float strength=ResonanceEffects.shakeStrength(opening.age(),opening.distanceToSqr(mc.player));
            if(strength>0&&(Math.abs(event.getYaw()-event.getCamera().getYRot())>.001||Math.abs(event.getPitch()-event.getCamera().getXRot())>.001))SHAKES.add(opening.getId()+":"+strength);
        }
    }
    private static final Path OUTPUT=Path.of("screenshots","resonance");
    private static final ConcurrentLinkedQueue<String> SOUNDS=new ConcurrentLinkedQueue<>();
    private static final BlockPos FIRST=new BlockPos(0,201,20), SECOND=new BlockPos(55,201,15);
    @SubscribeEvent public static void sound(PlaySoundEvent event){
        if(!Boolean.getBoolean("nyctocosm.resonanceSmoke")||event.getSound()==null)return;
        String id=event.getSound().getLocation().toString();
        if(id.equals("nyctocosm:spells.cast.resonance")||id.startsWith("nyctocosm:portal.external_resonance"))SOUNDS.add(ticks+","+id);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("nyctocosm.resonanceSmoke")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null){
            if(++boot>320&&mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button button&&(button.getMessage().getString().equals("Proceed")||button.getMessage().getString().equals("I know what I'm doing!")))button.onPress();
            if(boot>1800)throw new IllegalStateException("Resonance test world did not open");return;
        }
        ticks++;mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);
        if(ticks==1)try{Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("trace.csv"),"tick,opening_count,opening_age,x_portal,z_portal\n");}catch(Exception e){throw new IllegalStateException(e);}
        int tick=ticks;mc.getSingleplayerServer().execute(()->serverTick(mc,tick));
        if(ticks>=20&&ticks<=700&&ticks%2==0){Screenshot.grab(mc.gameDirectory,String.format(Locale.ROOT,"resonance/frame-%05d.png",frames++),mc.getMainRenderTarget(),message->{});}
    }
    private static void serverTick(Minecraft mc,int tick){
        var server=mc.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(tick==1){
            for(Entity entity:level.getAllEntities())if(entity instanceof NativeBoss||entity instanceof SeelicField||entity instanceof PortalActivation||entity instanceof SerpentBeamHazard||entity.getTags().contains("p0_smoke"))entity.discard();
            level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);
            for(BlockPos p:BlockPos.betweenClosed(-10,200,-15,70,225,45))level.setBlock(p,p.getY()==200?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            frame(level,FIRST,Direction.EAST);frame(level,SECOND,Direction.SOUTH);
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            player.teleportTo(level,11,203.375,3,0,0);
        }
        if(tick==25||tick==325){
            for(var field:level.getEntitiesOfClass(SeelicField.class,player.getBoundingBox().inflate(128)))field.discard();
            if(tick==325)player.teleportTo(level,38,203.375,26,-90,0);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.get("soul_bottle")));
            Content.get("soul_bottle").use(level,player,InteractionHand.MAIN_HAND);
        }
        if(tick==30||tick==330){
            var reagent=new ItemEntity(level,player.getX(),player.getY(),player.getZ(),new ItemStack(Items.ECHO_SHARD));reagent.setThrower(player.getUUID());reagent.setPickUpDelay(200);reagent.setNoGravity(true);reagent.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(reagent);
        }
        if(tick==120)player.teleportTo(level,32,214,-12,34,13);
        if(tick==420)player.teleportTo(level,24,214,0,-50,13);
        boolean first=level.getBlockState(FIRST.offset(10,3,0)).is(Content.PORTAL.get()),second=level.getBlockState(SECOND.offset(0,3,10)).is(Content.PORTAL.get());
        if(tick==115&&first)throw new IllegalStateException("Portal skipped opening animation");
        if(tick==270&&!first||tick==570&&!second)throw new IllegalStateException("Cast through empty frame did not open portal");
        var effects=level.getEntitiesOfClass(PortalActivation.class,new net.minecraft.world.phys.AABB(-20,190,-30,90,240,70));
        try{Files.writeString(OUTPUT.resolve("trace.csv"),tick+","+effects.size()+","+(effects.isEmpty()?-1:effects.get(0).age())+","+first+","+second+"\n",StandardOpenOption.APPEND);}catch(Exception e){throw new IllegalStateException(e);}
        if(tick==720){
            if(SHAKES.size()!=6)throw new IllegalStateException("Expected six rendered shake windows: "+SHAKES);
            System.out.println("RESONANCE_SHAKE_COMPLETE windows="+SHAKES.size());
            for(String suffix:List.of("first","second","third","collapse.first","collapse.second","collapse.third"))if(SOUNDS.stream().filter(s->s.endsWith("nyctocosm:portal.external_resonance."+suffix)).count()!=2)throw new IllegalStateException("Missing/duplicated client sound "+suffix+": "+SOUNDS);
            if(SOUNDS.stream().filter(s->s.endsWith("nyctocosm:spells.cast.resonance")).count()!=2)throw new IllegalStateException("Cast sound missing");
            try{Files.writeString(OUTPUT.resolve("sound-events.csv"),"client_tick,sound\n"+String.join("\n",SOUNDS)+"\n");}catch(Exception e){throw new IllegalStateException(e);}
            System.out.println("RESONANCE_SMOKE_COMPLETE frames="+frames+" sounds="+SOUNDS.size());mc.execute(mc::stop);
        }
    }
    private static void frame(ServerLevel level,BlockPos corner,Direction axis){
        for(int x=0;x<=21;x++)for(int y=0;y<=7;y++)level.setBlock(corner.relative(axis,x).above(y),(x==0||x==21||y==0||y==7)?Blocks.REINFORCED_DEEPSLATE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        for(int y=1;y<=6;y++)level.setBlock(corner.relative(axis,1).above(y),Blocks.SCULK_VEIN.defaultBlockState().setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(axis.getOpposite()),true),2);
    }
}
