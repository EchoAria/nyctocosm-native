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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fresh-world feedback captures, deliberately absent from release jars. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class ClientSmokeFeedback19 {
    @Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class ReferenceModels {
        @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event){
            if(!Boolean.getBoolean("nyctocosm.feedback19IdleOnly"))return;
            var key=new net.minecraft.client.resources.model.ModelResourceLocation(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","orange_dye"),"inventory");
            var base=event.getModels().get(key);if(base==null)throw new IllegalStateException("Missing reference dye model");
            event.getModels().put(key,new net.minecraftforge.client.model.BakedModelWrapper<net.minecraft.client.resources.model.BakedModel>(base){
                @Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides(){
                    return new net.minecraft.client.renderer.block.model.ItemOverrides(){
                        @Override public net.minecraft.client.resources.model.BakedModel resolve(net.minecraft.client.resources.model.BakedModel model,net.minecraft.world.item.ItemStack stack,net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.world.entity.LivingEntity entity,int seed){
                            if(stack.hasTag()&&stack.getTag().contains("FeedbackRigModel"))return Minecraft.getInstance().getModelManager().getModel(new net.minecraft.resources.ResourceLocation(stack.getTag().getString("FeedbackRigModel")));
                            return base.getOverrides().resolve(base,stack,level,entity,seed);
                        }
                    };
                }
            });
        }
    }
    private static boolean creating;
    private static int tick,boot;
    private static NativeBoss boss;
    private static Entity flightCamera;
    private static String lastFlightClip="";
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("nyctocosm.feedback19Smoke")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null){
            if(mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button b&&Set.of("Proceed","I know what I'm doing!","Continue","Done").contains(b.getMessage().getString()))b.onPress();
            if(!creating&&mc.screen instanceof TitleScreen){
                creating=true;if(Files.exists(mc.gameDirectory.toPath().resolve("saves/Feedback19")))throw new IllegalStateException("Fresh feedback19RunDir required");
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("Feedback19",new LevelSettings("Feedback19",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(81904L,false,false),WorldPresets::createNormalWorldDimensions);
            }
            if(++boot>3000)throw new IllegalStateException("No fresh feedback world");return;
        }
        tick++;mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);mc.player.getAbilities().flying=true;mc.player.setDeltaMovement(Vec3.ZERO);
        int frame=tick;mc.getSingleplayerServer().execute(()->scene(mc,frame));
        if(frame>=730&&boss!=null&&mc.level.getEntity(boss.getId()) instanceof NativeBoss visible){
            if(flightCamera==null)flightCamera=net.minecraft.world.entity.EntityType.ARMOR_STAND.create(mc.level);
            Vec3 at=visible.position().add(24,12,24);flightCamera.setPos(at);
            flightCamera.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,visible.position().add(0,1.5,0));
            flightCamera.xo=at.x;flightCamera.yo=at.y;flightCamera.zo=at.z;
            flightCamera.yRotO=flightCamera.getYRot();flightCamera.xRotO=flightCamera.getXRot();mc.setCameraEntity(flightCamera);
            if(visible.animation().endsWith("apocalypse_rain")&&Set.of(12,22,36,44).contains((int)visible.animationTime(0)))
                Screenshot.grab(mc.gameDirectory,"feedback19-rain-"+tick+".png",mc.getMainRenderTarget(),m->{});
        }
        if(tick==60)for(Entity e:mc.level.entitiesForRendering())if(e instanceof NativeBoss b&&b.kind==2){
            var rig=com.flubberschnub.nyctocosm.client.RigLibrary.get("ajemissary");
            for(var bone:rig.bones())if(Set.of("rightupperwing","righthand").contains(bone.name())){
                var selected=rig.variants().getOrDefault(b.emissaryModelVariant(bone.name()),Map.of()).getOrDefault(bone.name(),bone.model());
                var model=mc.getModelManager().getModel(selected);var trs=model.getTransforms().getTransform(net.minecraft.world.item.ItemDisplayContext.HEAD);
                var sampled=com.flubberschnub.nyctocosm.client.RigLibrary.pose(rig,bone,b.animation(),b.animationTime(0),true);
                var probe=new com.mojang.blaze3d.vertex.PoseStack();probe.mulPoseMatrix(RigTransforms.itemTransform(sampled));model.applyTransform(net.minecraft.world.item.ItemDisplayContext.HEAD,probe,false);
                System.out.println("FEEDBACK19_HEAD_APPLIED "+probe.last().pose());
                System.out.println("FEEDBACK19_MATRIX bone="+bone.name()+" pose="+sampled+" item="+RigTransforms.itemTransform(sampled));
                System.out.println("FEEDBACK19_MODEL bone="+bone.name()+" model="+selected+" head="+trs.rotation+" scale="+trs.scale+" missing="+(model==mc.getModelManager().getMissingModel()));
                for(var q:model.getQuads(null,null,net.minecraft.util.RandomSource.create(1)))System.out.println("FEEDBACK19_SPRITE "+q.getSprite().contents().name());
            }
        }
        if(tick%20==0)for(Entity e:mc.level.entitiesForRendering())if(e instanceof NativeBoss b)System.out.println("FEEDBACK19_CLIENT tick="+tick+" kind="+b.kind+" clip="+b.animation()+" time="+b.animationTime(0)+" pos="+b.position());
        if(Set.of(65,90,115,370,390,394,404,450,635,650,665,680,700,760,860,1000,1100,1200,1400,1600,1800,2000,2200,2315,2330,2345).contains(tick))Screenshot.grab(mc.gameDirectory,"feedback19-"+tick+".png",mc.getMainRenderTarget(),m->{});
        if(tick==2400||tick==120&&Boolean.getBoolean("nyctocosm.feedback19IdleOnly")){System.out.println("FEEDBACK19_CAPTURE_COMPLETE");mc.stop();}
    }
    private static void scene(Minecraft mc,int frame){
        var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        if(frame==1){level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);for(int x=-60;x<=60;x++)for(int z=-60;z<=60;z++)level.setBlock(new BlockPos(x,200,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);}
        if(Set.of(30,160,570,730).contains(frame)){
            var old=new ArrayList<Entity>();level.getAllEntities().forEach(old::add);for(Entity e:old)if(e instanceof NativeBoss||e instanceof NullpointStaff||e instanceof EmissaryRainShard||e instanceof SpellBolt)e.discard();
            player.setGameMode(frame==730?GameType.SURVIVAL:GameType.CREATIVE);player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.getAbilities().invulnerable=true;player.onUpdateAbilities();
            player.teleportTo(level,0,frame==570?232:frame==160?203:206,frame==160?-8:-28,0,frame==570?30:0);
            boss=(frame==160?Content.SORCERER:frame==570?Content.SERPENT:Content.EMISSARY).get().create(level);boss.moveTo(0,201,4,180,0);boss.setNoAi(frame!=730);boss.setNoGravity(true);level.addFreshEntity(boss);
            if(frame==30&&Boolean.getBoolean("nyctocosm.feedback19IdleOnly")){
                boss.setPos(-15,201,4);player.teleportTo(level,0,206,-38,0,0);
                try(var stream=new java.util.zip.GZIPInputStream(ClientSmokeFeedback19.class.getResourceAsStream("/assets/nyctocosm/native_rigs/ajemissary.json.gz"));var reader=new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){
                    var rig=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var poses=rig.getAsJsonObject("animations").getAsJsonArray("phase1_idle").get(0).getAsJsonObject().getAsJsonObject("bones");
                    for(var entry:poses.entrySet()){
                        String bone=entry.getKey(),variant=boss.emissaryModelVariant(bone);
                        var models=rig.getAsJsonObject("variants").getAsJsonObject(variant);String model=models!=null&&models.has(bone)?models.get(bone).getAsString():"animated_java:item/ajemissary/"+bone;
                        var tag=new CompoundTag();var matrix=new net.minecraft.nbt.ListTag();for(var number:entry.getValue().getAsJsonArray())matrix.add(net.minecraft.nbt.FloatTag.valueOf(number.getAsFloat()));tag.put("transformation",matrix);tag.putString("item_display","head");tag.putFloat("view_range",100);tag.putFloat("width",100);tag.putFloat("height",100);
                        var item=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ORANGE_DYE);item.getOrCreateTag().putString("FeedbackRigModel",model);tag.put("item",item.save(new CompoundTag()));
                        var display=net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(level);display.load(tag);display.moveTo(15,201,4,180,0);level.addFreshEntity(display);
                    }
                }catch(Exception error){throw new IllegalStateException(error);}
            }
            if(frame==160)boss.beginEntrance();
            if(frame==730){var state=new CompoundTag();boss.saveWithoutId(state);state.putInt("NativePhase",4);state.putInt("Cooldown",0);boss.load(state);boss.getRandom().setSeed(19L);boss.setTarget(player);boss.startEmissaryAttackForTest(player,"phase4_idle_transition_flying");}
            System.out.println("FEEDBACK19_SCENE frame="+frame+" kind="+boss.kind);
        }
        if(boss==null||boss.isRemoved())return;
        if(frame==2300){
            // Independent high-altitude Rain presentation fixture follows the natural AI scene.
            player.stopRiding();player.teleportTo(level,0,201,-28,0,0);boss.discard();
            boss=Content.EMISSARY.get().create(level);boss.moveTo(0,231,4,180,0);boss.setNoAi(true);level.addFreshEntity(boss);
            var state=new CompoundTag();boss.saveWithoutId(state);state.putInt("NativePhase",4);state.putBoolean("EmissaryFlying",true);state.putInt("EmissaryFlyTimer",40);boss.load(state);boss.setTarget(player);
            boss.startEmissaryAttackForTest(player,"phase4.attack.apocalypse_rain");
            System.out.println("FEEDBACK19_HIGH_RAIN_FIXTURE root="+boss.position()+" player="+player.position());
        }
        if(frame>2300)boss.customServerAiStep();
        if(frame>160&&frame<570)boss.customServerAiStep();
        if(frame>=570&&frame<730){
            int age=frame-570;Vec3 p=age<70?new Vec3(0,201,-30+age*.7):new Vec3((age-70)*.7,201,19);
            boss.setPos(p);boss.setYRot(age<70?0:-90);boss.setXRot(0);
        }
        if(frame>=730&&frame%10==0){var saved=new CompoundTag();boss.saveWithoutId(saved);int rain=0;double low=1000,high=-1000;for(Entity e:level.getAllEntities())if(e instanceof EmissaryRainShard){rain++;low=Math.min(low,e.getY());high=Math.max(high,e.getY());}
            System.out.println("FEEDBACK19_FLIGHT frame="+frame+" y="+boss.getY()+" playerY="+player.getY()+" distance="+boss.distanceTo(player)+" clip="+boss.animation()+" timer="+saved.getInt("EmissaryFlyTimer")+" speed="+saved.getInt("EmissaryFlySpeed")+" shards="+rain+" low="+low+" high="+high);
        }
        if(frame>=730&&!lastFlightClip.equals(boss.animation())){lastFlightClip=boss.animation();System.out.println("FEEDBACK19_FLIGHT_ATTACK frame="+frame+" clip="+lastFlightClip+" rootY="+boss.getY()+" playerY="+player.getY()+" distance="+boss.distanceTo(player));}
        if(frame>=730&&boss.animation().endsWith("apocalypse_rain")&&boss.emissaryAttackAge()>=10&&boss.emissaryAttackAge()<=44)
            System.out.println("FEEDBACK19_RAIN_FIRE frame="+frame+" age="+boss.emissaryAttackAge()+" rootY="+boss.getY()+" playerY="+player.getY());
    }
    private ClientSmokeFeedback19(){}
}
