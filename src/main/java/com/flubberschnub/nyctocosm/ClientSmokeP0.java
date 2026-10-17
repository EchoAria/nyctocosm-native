package com.flubberschnub.nyctocosm;

import java.nio.file.*;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in continuous P0 capture. Vanilla reference entities exist ONLY in this excluded test class. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class ClientSmokeP0 {
    private static int ticks,bootTicks,frames;
    private static NativeBoss boss;
    private static Cow target;
    private static ArmorStand reference;
    private static long referenceStart;
    private static boolean transformChecked;
    private static final Path OUTPUT=Path.of("screenshots","p0");
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("nyctocosm.p0Smoke")||event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null){
            if(++bootTicks>320&&mc.screen!=null)for(var widget:mc.screen.children())
                if(widget instanceof net.minecraft.client.gui.components.Button button&&
                    (button.getMessage().getString().equals("Proceed")||button.getMessage().getString().equals("I know what I'm doing!")))button.onPress();
            if(bootTicks>1800)throw new IllegalStateException("P0 smoke world did not open");return;
        }
        ticks++;mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);
        if(ticks==1){
            try{Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("trace.csv"),"tick,kind,x,y,z,phase,animation,motion\n");}
            catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        final int tick=ticks;
        if(!transformChecked)for(Entity entity:mc.level.entitiesForRendering())if(entity instanceof ArmorStand stand&&stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.GLOWSTONE_DUST)){
            var renderer=(net.minecraft.client.renderer.entity.ArmorStandRenderer)mc.getEntityRenderDispatcher().getRenderer(stand);
            var model=renderer.getModel();model.setupAnim(stand,0,0,0,0,0);
            var vanilla=new com.mojang.blaze3d.vertex.PoseStack();
            vanilla.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-stand.getYRot()));
            vanilla.scale(-1,-1,1);vanilla.translate(0,-1.501f,0);
            model.getHead().translateAndRotate(vanilla);
            net.minecraft.client.renderer.entity.layers.CustomHeadLayer.translateToHead(vanilla,false);
            var nativeMatrix=SorcererPlane.headTransform(stand.getYRot(),stand.getHeadPose().getZ());
            if(!nativeMatrix.equals(vanilla.last().pose(),.00001f))throw new IllegalStateException("Native plane transform differs from vanilla armor stand renderer");
            Rotations original=stand.getHeadPose();
            for(float pitch:new float[]{-80,-30,30,80}){
                stand.setHeadPose(new Rotations(pitch,.001f,.001f));model.setupAnim(stand,0,0,0,0,0);
                var tilted=new com.mojang.blaze3d.vertex.PoseStack();tilted.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-stand.getYRot()));
                tilted.scale(-1,-1,1);tilted.translate(0,-1.501f,0);model.getHead().translateAndRotate(tilted);
                net.minecraft.client.renderer.entity.layers.CustomHeadLayer.translateToHead(tilted,false);
                if(!RigTransforms.armorStandHead(stand.getYRot(),pitch,.001f,.001f).equals(tilted.last().pose(),.00001f))throw new IllegalStateException("Serpent helmet pitch pivot differs from vanilla");
            }
            stand.setHeadPose(original);System.out.println("P0_SERPENT_PITCH_TRANSFORM_MATCH");
            transformChecked=true;System.out.println("P0_PLANE_TRANSFORM_MATCH");break;
        }
        mc.getSingleplayerServer().execute(()->serverTick(mc,tick));
        if(ticks>=20&&ticks<=1640&&ticks%2==0){
            Screenshot.grab(mc.gameDirectory,String.format(Locale.ROOT,"p0/frame-%05d.png",frames++),mc.getMainRenderTarget(),message->{});
        }
        if(ticks==1670){
            if(!transformChecked)throw new IllegalStateException("Vanilla plane reference did not render");
            mc.options.hideGui=false;System.out.println("P0_SMOKE_COMPLETE frames="+frames);mc.stop();
        }
    }
    private static void serverTick(Minecraft mc,int tick){
        var server=mc.getSingleplayerServer();ServerLevel level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(tick==1){
            clear(level);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            for(int x=-32;x<=64;x++)for(int z=-32;z<=64;z++)level.setBlockAndUpdate(new BlockPos(x,200,z),Blocks.SMOOTH_STONE.defaultBlockState());
            level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            player.teleportTo(level,17,208,-14,0,0);
            boss=Content.SORCERER.get().create(level);boss.moveTo(12,201,8,180,0);
            var saved=new CompoundTag();boss.saveWithoutId(saved);saved.putString("Animation","rise");saved.putInt("AnimationAge",0);saved.putInt("Remaining",133);boss.load(saved);
            boss.spawnNullpointForTest(level);level.addFreshEntity(boss);
        }
        if(tick<600&&boss!=null&&boss.hasSorcererPlane()&&reference==null){
            reference=EntityType.ARMOR_STAND.create(level);Vec3 p=boss.sorcererPlanePosition().add(10,0,0);
            reference.moveTo(p.x,p.y,p.z,boss.sorcererPlaneYaw(),0);reference.setInvisible(true);reference.setNoGravity(true);
            CompoundTag standTag=new CompoundTag();reference.saveWithoutId(standTag);standTag.putBoolean("Marker",true);reference.load(standTag);
            ItemStack item=new ItemStack(Items.GLOWSTONE_DUST);item.getOrCreateTag().putInt("CustomModelData",19);reference.setItemSlot(EquipmentSlot.HEAD,item);
            referenceStart=level.getGameTime()-(long)boss.sorcererPlaneRoll(0);level.addFreshEntity(reference);
        }
        if(reference!=null){
            int angle=(int)((level.getGameTime()-referenceStart)%360);reference.setHeadPose(new Rotations(.01f,.01f,angle==0?360:angle));
        }
        if(tick==510){boss.nullpoint().orElseThrow().hurt(level.damageSources().generic(),400);}
        if(tick==540&&boss.hasSorcererPlane())throw new IllegalStateException("P0 plane did not disappear after staff break");
        if(tick==600||tick==1100||tick==1350){
            clear(level);int type=tick==600?1:tick==1100?2:3;
            player.teleportTo(level,80,255,-55,48,20);
            target=EntityType.COW.create(level);target.moveTo(16,210,8,0,0);target.setNoAi(true);target.setNoGravity(true);target.setInvulnerable(true);target.addTag("p0_smoke");level.addFreshEntity(target);
            boss=Content.SERPENT.get().create(level);boss.moveTo(16,214,-10,0,0);level.addFreshEntity(boss);
            boss.startSerpentAttackForTest(level,target,type);
        }
        if(tick>=600&&target!=null){
            double angle=(tick-600)*.035;target.setPos(16+Math.sin(angle)*12,210+Math.sin(angle*.5)*6,8+Math.cos(angle)*12);
            if(tick==850){CompoundTag saved=new CompoundTag();boss.saveWithoutId(saved);boss.discard();boss=Content.SERPENT.get().create(level);boss.load(saved);level.addFreshEntity(boss);boss.setTarget(target);}
        }
        if(tick>=1100&&tick<1350&&boss!=null&&boss.serpentState()==SerpentMotion.LASER){
            // Keep the observer within particle delivery range while recording the actual ray.
            Vec3 camera=boss.position().add(18,8,-18),direction=boss.getEyePosition().subtract(camera.add(0,player.getEyeHeight(),0));
            player.teleportTo(level,camera.x,camera.y,camera.z,SerpentMotion.yaw(direction),(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        }
        if(tick==1350)player.teleportTo(level,38,217,-14,45,16);
        if(boss!=null&&!boss.isRemoved()){
            String motion=boss.kind==0?boss.serpentMotionForTrace().toString().replace('"','\''):"plane="+boss.hasSorcererPlane()+";roll="+boss.sorcererPlaneRoll(0);
            String row=String.format(Locale.ROOT,"%d,%d,%.6f,%.6f,%.6f,%d,%s,\"%s\"%n",tick,boss.kind,boss.getX(),boss.getY(),boss.getZ(),boss.phase(),boss.animation(),motion);
            try{Files.writeString(OUTPUT.resolve("trace.csv"),row,StandardOpenOption.APPEND);}catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
    }
    private static void clear(ServerLevel level){
        for(Entity e:level.getAllEntities())if(e instanceof NativeBoss||e instanceof NullpointStaff||e instanceof SerpentBeamHazard||e instanceof IceSpikeWave||e instanceof ArmorStand||e.getTags().contains("p0_smoke")||e.getTags().contains("native_smoke_target"))e.discard();
        reference=null;target=null;
    }
}
