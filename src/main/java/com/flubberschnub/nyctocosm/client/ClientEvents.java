package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix3f;

@Mod.EventBusSubscriber(modid=NativeMod.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientEvents {
    private static final String[] EXTRA={"serpent/serpent_head","serpent/serpent_segment","serpent/serpent_tail","mobs/nightmare_apparition/nightmare_apparition","magic/spell_plane","magic/spells/ice_spike","magic/sorcerer_plane"};
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){
        RigLibrary.allModels().forEach(event::register);
        for(String model:EXTRA)event.register(new ResourceLocation("nyctocosm",model));
        for(Spell spell:Spell.values())event.register(new ResourceLocation("nyctocosm","magic/spell_circles/"+spell.id()));
    }
    @SubscribeEvent public static void reloaded(ModelEvent.BakingCompleted event){RigLibrary.clear();}
    @SubscribeEvent public static void glow(ModelEvent.ModifyBakingResult event){
        GlowingModels.clear();event.getModels().replaceAll((id,model)->id.getNamespace().equals("nyctocosm")||id.getNamespace().equals("animated_java")?GlowingModels.wrap(model):model);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        net.minecraft.client.gui.screens.MenuScreens.register(Content.DAIS_MENU.get(),DaisScreen::new);
        for(String id:new String[]{"wyrmsliver","nullfissure","semblanceofdoom","planar_coil"})ItemProperties.register(Content.get(id),new ResourceLocation("nyctocosm","casting"),(stack,level,entity,seed)->entity instanceof net.minecraft.world.entity.player.Player player&&player.getCooldowns().isOnCooldown(stack.getItem())?1:0);
    });}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        for(var type:java.util.List.of(Content.SERPENT,Content.SORCERER,Content.EMISSARY,Content.DEATH_WALKER,Content.APPARITION))event.registerEntityRenderer(type.get(),BossRenderer::new);
        event.registerEntityRenderer(Content.FIELD.get(),FieldRenderer::new);
        event.registerEntityRenderer(Content.BOLT.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.AREA.get(),AreaRenderer::new);
        event.registerEntityRenderer(Content.NULLPOINT.get(),StaffRenderer::new);
        event.registerEntityRenderer(Content.ICE_SPIKE_WAVE.get(),IceSpikeRenderer::new);
        event.registerEntityRenderer(Content.SERPENT_BEAM.get(),EmptyRenderer::new);
    }
    public static void openBook(ItemStack stack){Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(stack)));}
    public static void weaponEffect(int kind,Vec3 eye,Vec3 end,Vec3 forward){WeaponEffectController.play(kind,eye,end,forward);}
    private static void model(ResourceLocation id,PoseStack pose,MultiBufferSource buffers,int light){
        Minecraft mc=Minecraft.getInstance();
        mc.getItemRenderer().render(new ItemStack(Items.STICK),ItemDisplayContext.HEAD,false,pose,buffers,light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel(id));
    }
    private static void legacyHeadModel(ResourceLocation id,PoseStack pose,MultiBufferSource buffers,int light){
        // The artist authored these meshes for a helmet slot. Bake that coordinate transform
        // into our renderer, without creating an ArmorStand or borrowing its entity renderer.
        pose.pushPose();pose.translate(0,1.6875,0);pose.scale(.625f,.625f,.625f);
        model(id,pose,buffers,light);pose.popPose();
    }
    private static void rig(String id,String animation,float time,boolean loop,PoseStack pose,MultiBufferSource buffers,int light,String... variants){
        var rig=RigLibrary.get(id);
        for(var bone:rig.bones()){
            pose.pushPose();var matrix=RigTransforms.itemTransform(RigLibrary.pose(rig,bone,animation,time,loop));pose.mulPoseMatrix(matrix);
            if(Math.abs(matrix.determinant())<1e-8){pose.popPose();continue;}
            pose.last().normal().mul(new Matrix3f(matrix).invert().transpose());
            ResourceLocation selected=bone.model();
            for(String variant:variants)selected=rig.variants().getOrDefault(variant,java.util.Map.of()).getOrDefault(bone.name(),selected);
            model(selected,pose,buffers,light);pose.popPose();
        }
    }
    private static class EmptyRenderer<T extends Entity> extends EntityRenderer<T>{
        EmptyRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(T entity){return TextureAtlas.LOCATION_BLOCKS;}
    }
    private static final class BossRenderer extends EmptyRenderer<NativeBoss>{
        BossRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=1;}
        @Override public void render(NativeBoss boss,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();
            if(boss.kind==0){
                Vec3[] segments=boss.serpentRenderSegments(partial);Vec3 cameraOrigin=segments[0];
                pose.mulPose(Axis.YP.rotationDegrees(180-yaw));legacyHeadModel(new ResourceLocation("nyctocosm",EXTRA[0]),pose,buffers,light);pose.popPose();
                for(int i=1;i<=40;i++){
                    Vec3 at=segments[i],prev=segments[i-1];Vec3 d=prev.subtract(at),offset=at.subtract(cameraOrigin);
                    pose.pushPose();pose.translate(offset.x,offset.y,offset.z);pose.mulPose(Axis.YP.rotationDegrees(180+(float)Math.toDegrees(Math.atan2(d.x,d.z))));
                    pose.mulPose(Axis.XP.rotation((float)-Math.atan2(d.y,d.horizontalDistance())));
                    legacyHeadModel(new ResourceLocation("nyctocosm",i==40?EXTRA[2]:EXTRA[1]),pose,buffers,light);pose.popPose();
                }
            }else{
                if(boss.kind==1&&(boss.phase()&1)==1&&!boss.animation().equals("falltransition")
                    &&(!boss.animation().equals("initial_spawn")||boss.animationTime(partial)>=330)
                    &&(!boss.animation().equals("rise")||boss.animationTime(partial)>=127)){
                    pose.pushPose();pose.translate(0,-.25,0);pose.mulPose(Axis.YP.rotationDegrees(boss.tickCount*2+partial*2));
                    model(new ResourceLocation("nyctocosm",EXTRA[6]),pose,buffers,LightTexture.FULL_BRIGHT);pose.popPose();
                }
                pose.mulPose(boss.kind==4?Axis.YP.rotationDegrees(180-yaw):RigTransforms.orientation(yaw));
                if(boss.kind==4)legacyHeadModel(new ResourceLocation("nyctocosm",EXTRA[3]),pose,buffers,light);
                else if(boss.kind==2)rig(boss.rig(),boss.animation(),boss.animationTime(partial),boss.animation().contains("idle"),pose,buffers,light,
                    boss.animation().equals("spawn")?"wingless":"wing_config_"+(boss.phase()-1),boss.phase()>=2?"swordful":"swordless",boss.phase()>=3?"armorless":"armorful","sweepless");
                else rig(boss.rig(),boss.animation(),boss.animationTime(partial),boss.animation().contains("idle"),pose,buffers,light);
                pose.popPose();
            }
            super.render(boss,yaw,partial,pose,buffers,light);
        }
        @Override public boolean shouldRender(NativeBoss entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){return entity.kind==0||super.shouldRender(entity,frustum,x,y,z);}
    }
    private static final class FieldRenderer extends EmptyRenderer<SeelicField>{
        FieldRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public void render(SeelicField field,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();
            rig(field.isHypertrace()?"ajhypertrace":field.mobile()?"ajmobilespellplane":"ajspellplane","idle",field.tickCount+partial,true,pose,buffers,LightTexture.FULL_BRIGHT);
            if(field.spell()!=null)model(new ResourceLocation("nyctocosm","magic/spell_circles/"+field.spell().id()),pose,buffers,LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
    private static final class StaffRenderer extends EmptyRenderer<NullpointStaff>{
        StaffRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;}
        @Override public void render(NullpointStaff staff,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.mulPose(RigTransforms.orientation(yaw));
            rig("ajnullpoint",staff.animation(),staff.animationTime(partial),staff.animation().equals("idle"),pose,buffers,light);pose.popPose();
            super.render(staff,yaw,partial,pose,buffers,light);
        }
    }
    private static final class IceSpikeRenderer extends EmptyRenderer<IceSpikeWave>{
        IceSpikeRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public void render(IceSpikeWave wave,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            for(int born=5;born<=25;born+=5){
                float rise=wave.spikeRise(born,partial);if(rise<0)continue;double radius=born*.5;
                for(int arm=0;arm<30;arm++){
                    Vec3 point=IceSpikeWave.point(Vec3.ZERO,arm,radius);
                    pose.pushPose();pose.translate(point.x,rise,point.z);pose.mulPose(Axis.YP.rotationDegrees(arm*12));
                    legacyHeadModel(new ResourceLocation("nyctocosm",EXTRA[5]),pose,buffers,LightTexture.FULL_BRIGHT);pose.popPose();
                }
            }
        }
    }
    private static final class AreaRenderer extends EmptyRenderer<SpellArea>{
        AreaRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public void render(SpellArea area,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            model(new ResourceLocation("nyctocosm","magic/spell_circles/"+area.spell().id()),pose,buffers,LightTexture.FULL_BRIGHT);pose.popPose();
        }
    }
}
