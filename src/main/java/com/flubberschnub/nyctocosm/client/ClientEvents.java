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
    private static java.util.UUID cooldownPlayer;
    private static final int[] weaponCooldowns=new int[4];
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
        for(String id:new String[]{"seelic_scythe","wyrmsliver","nullfissure","semblanceofdoom"})ItemProperties.register(Content.get(id),new ResourceLocation("nyctocosm","casting"),(stack,level,entity,seed)->weaponCoolingDown(stack,entity)?1:0);
        ItemProperties.register(Content.get("planar_coil"),new ResourceLocation("nyctocosm","casting"),(stack,level,entity,seed)->entity instanceof net.minecraft.world.entity.player.Player player&&player.getCooldowns().isOnCooldown(stack.getItem())?1:0);
    });}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        for(var type:java.util.List.of(Content.SERPENT,Content.SORCERER,Content.EMISSARY,Content.DEATH_WALKER,Content.APPARITION))event.registerEntityRenderer(type.get(),BossRenderer::new);
        event.registerEntityRenderer(Content.FIELD.get(),FieldRenderer::new);
        event.registerEntityRenderer(Content.BOLT.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.AREA.get(),AreaRenderer::new);
        event.registerEntityRenderer(Content.PORTAL_ACTIVATION.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.NULLPOINT.get(),StaffRenderer::new);
        event.registerEntityRenderer(Content.SORCERER_HAIL.get(),ArmorStandRenderer::new);
        event.registerEntityRenderer(Content.FROZEN_BLADE.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.SORCERER_BEAM_NODE.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.ICE_SPIKE_WAVE.get(),IceSpikeRenderer::new);
        event.registerEntityRenderer(Content.SERPENT_BEAM.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.SOUL.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EXPULSION_BURST.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_GRAPPLE.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_HIT_REGION.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_HAZARD.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_SEEKING_MISSILE.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_SWORD_PLUNGE.get(),EmissarySwordPlungeRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_WING_BLAST_RAY.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_PILLAR.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_FLAME_GEYSER.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_RAIN_SHARD.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_CELESTIAL_TEAR.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.EMISSARY_FLAME_SLASH_WAVE.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.CRYPT_SLATEHENGE_TRIGGER.get(),EmptyRenderer::new);
        event.registerEntityRenderer(Content.SLATEHENGE_EFFECT.get(),EmptyRenderer::new);
    }
    public static void openBook(ItemStack stack){Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(stack)));}
    public static void weaponCooldowns(java.util.UUID playerId,int nullSwing,int nullCast,int doomSwing,int doomCast){
        cooldownPlayer=playerId;weaponCooldowns[0]=nullSwing;weaponCooldowns[1]=nullCast;weaponCooldowns[2]=doomSwing;weaponCooldowns[3]=doomCast;
    }
    private static boolean weaponCoolingDown(ItemStack stack,Entity entity){
        if(!(entity instanceof net.minecraft.world.entity.player.Player player)||!(stack.getItem() instanceof WeaponItem weapon))return false;
        if(weapon.kind<=1)return player.getCooldowns().isOnCooldown(stack.getItem());
        net.minecraft.world.InteractionHand hand=player.getMainHandItem()==stack?net.minecraft.world.InteractionHand.MAIN_HAND:
            player.getOffhandItem()==stack?net.minecraft.world.InteractionHand.OFF_HAND:null;
        if(hand==null||!player.getUUID().equals(cooldownPlayer))return false;
        int index=weapon.kind==2?(hand==net.minecraft.world.InteractionHand.MAIN_HAND?0:1):(hand==net.minecraft.world.InteractionHand.MAIN_HAND?2:3);
        return weaponCooldowns[index]>0;
    }
    public static void weaponEffect(int kind,Vec3 eye,Vec3 end,Vec3 forward){WeaponEffectController.play(kind,eye,end,forward);}
    public static void sorcererRay(java.util.List<SorcererBeamNode.Sample> samples,boolean halo){SorcererBeamEffects.ray(Minecraft.getInstance().level,samples,halo);}
    public static void sorcererCue(NativeNetwork.SorcererCue cue){var level=Minecraft.getInstance().level;if(level!=null&&(cue.clip().equals("dashstrike")||cue.clip().equals("airdash"))&&cue.frame()>=0&&cue.frame()<=71)BossParticleEffects.clientFrame(level,"ajsorcererm2",cue.clip(),cue.frame(),cue.entry(),cue.yaw(),cue.pitch(),cue.after());}
    public static void emissaryRay(NativeNetwork.EmissaryRay cue){
        var level=Minecraft.getInstance().level;if(level==null)return;int range=0;
        for(var sample:cue.samples()){
            ++range;BossParticleEffects.clientFrame(level,"feedback",cue.signal()?"god_signal":cue.wide()?"divine_ray":"god_ray",0,sample.position(),sample.yaw(),sample.pitch(),sample.position());
            if(cue.wide()&&!cue.signal()&&(range==10||range==30||range==40))FocusEffectController.play(new NativeNetwork.SourceVisual(range==10?"meteor_ring":"meteor_ring_small",0,sample.position(),sample.yaw(),sample.pitch()-90,level.random.nextLong()));
        }
        if(!cue.signal())BossParticleEffects.clientFrame(level,"feedback","ray_impact",0,cue.end(),0,0,cue.end());
    }
    public static void emissaryBeam(Vec3 start,Vec3 end,boolean wide){
        var level=Minecraft.getInstance().level;if(level==null)return;
        Vec3 delta=end.subtract(start),forward=delta.normalize();
        Vec3 side=forward.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);side=side.normalize();
        Vec3 up=side.cross(forward).normalize();
        // One segment packet, bounded client particles; no per-sample server broadcast.
        int count=Math.min(140,(int)Math.ceil(delta.length()*2));
        for(int i=0;i<count;i++){
            Vec3 center=start.add(forward.scale(i*.5));double angle=i*2.4+level.getGameTime()*.5,radius=wide?1.5:.25;
            Vec3 point=center.add(side.scale(Math.cos(angle)*radius)).add(up.scale(Math.sin(angle)*radius));
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,center.x,center.y,center.z,0,0,0);
            level.addParticle(i%2==0?net.minecraft.core.particles.ParticleTypes.FLAME:net.minecraft.core.particles.ParticleTypes.WAX_ON,point.x,point.y,point.z,0,0,0);
        }
    }
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
    static void serpentModel(ResourceLocation id,float yaw,float pitch,PoseStack pose,MultiBufferSource buffers,int light){
        // Pitch belongs at the original helmet pivot, not at the entity's feet.
        pose.pushPose();var matrix=RigTransforms.armorStandHead(yaw,pitch,.001f,.001f);
        pose.mulPoseMatrix(matrix);pose.last().normal().mul(new Matrix3f(matrix).invert().transpose());
        model(id,pose,buffers,light);pose.popPose();
    }
    private static void rig(String id,String animation,float time,boolean loop,PoseStack pose,MultiBufferSource buffers,int light,String... variants){rig(id,animation,time,loop,pose,buffers,light,null,variants);}
    private static void rig(String id,String animation,float time,boolean loop,PoseStack pose,MultiBufferSource buffers,int light,NativeBoss emissary,String... variants){
        var rig=RigLibrary.get(id);
        for(var bone:rig.bones()){
            if(!BossVisuals.visibleRigBone(id,animation,bone.name()))continue;
            pose.pushPose();var matrix=RigTransforms.itemTransform(RigLibrary.pose(rig,bone,animation,time,loop));pose.mulPoseMatrix(matrix);
            if(Math.abs(matrix.determinant())<1e-8){pose.popPose();continue;}
            pose.last().normal().mul(new Matrix3f(matrix).invert().transpose());
            ResourceLocation selected=bone.model();
            if(id.startsWith("ajsorcerer"))selected=RigLibrary.animatedModel(rig,bone.name(),animation,time,selected);
            for(String variant:variants)selected=rig.variants().getOrDefault(variant,java.util.Map.of()).getOrDefault(bone.name(),selected);
            if(emissary!=null)selected=rig.variants().getOrDefault(emissary.emissaryModelVariant(bone.name()),java.util.Map.of()).getOrDefault(bone.name(),selected);
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
            if(boss.emissaryApproaching())return; // The original 95-tick approach is a particle meteor, not the rig yet.
            pose.pushPose();
            if(boss.kind==0){
                // Each visible segment is drawn by SerpentWorldRenderer, independently of the head's section.
                pose.popPose();
            }else{
                if(boss.hasSorcererPlane()){
                    Vec3 offset=boss.sorcererPlanePosition().subtract(boss.getPosition(partial));
                    pose.pushPose();pose.translate(offset.x,offset.y,offset.z);
                    var matrix=SorcererPlane.headTransform(boss.sorcererPlaneYaw(),boss.sorcererPlaneRoll(partial));
                    pose.mulPoseMatrix(matrix);pose.last().normal().mul(new Matrix3f(matrix).invert().transpose());
                    model(new ResourceLocation("nyctocosm",EXTRA[6]),pose,buffers,LightTexture.FULL_BRIGHT);pose.popPose();
                }
                float emissaryPitch=net.minecraft.util.Mth.rotLerp(partial,boss.xRotO,boss.getXRot());
                pose.mulPose(boss.kind==4?Axis.YP.rotationDegrees(180-yaw):boss.kind==2
                    ?RigTransforms.orientation(yaw,emissaryPitch):RigTransforms.orientation(yaw));
                if(boss.kind==4)legacyHeadModel(new ResourceLocation("nyctocosm",EXTRA[3]),pose,buffers,light);
                else if(boss.kind==2)rig(boss.rig(),boss.animation(),boss.animationTime(partial),boss.animation().contains("idle"),pose,buffers,light,boss);
                else rig(boss.rig(),boss.animation(),boss.animationTime(partial),boss.animation().contains("idle"),pose,buffers,light);
                pose.popPose();
            }
            super.render(boss,yaw,partial,pose,buffers,light);
        }
        @Override public boolean shouldRender(NativeBoss entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){return entity.kind==0||entity.kind==2||super.shouldRender(entity,frustum,x,y,z);}
    }
    private static final class FieldRenderer extends EmptyRenderer<SeelicField>{
        FieldRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public void render(SeelicField field,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();
            rig(field.isHypertrace()?"ajhypertrace":field.mobile()?"ajmobilespellplane":"ajspellplane","idle",field.tickCount+partial,true,pose,buffers,LightTexture.FULL_BRIGHT);
            if(field.spell()!=null)model(new ResourceLocation("nyctocosm","magic/spell_circles/"+field.spell().id()),pose,buffers,LightTexture.FULL_BRIGHT);
            var circle=field.luminousCircle();
            if(!circle.isEmpty()){
                pose.pushPose();pose.translate(circle.getDouble("X")-field.getX(),circle.getDouble("Y")-field.getY(),circle.getDouble("Z")-field.getZ());
                pose.mulPose(Axis.YP.rotationDegrees(180-circle.getFloat("Yaw")-(20-circle.getInt("Left")+partial)*.5f));
                legacyHeadModel(new ResourceLocation("nyctocosm","magic/spell_circles/luminescence"),pose,buffers,LightTexture.FULL_BRIGHT);pose.popPose();
            }
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
            for(int ring=0;ring<5;ring++){
                float rise=wave.spikeVisualOffset(ring,partial);if(rise<0)continue;
                for(int arm=0;arm<30;arm++){
                    int surface=wave.spikeSurfaceY(ring,arm);if(!IceSpikeWave.hasSurface(surface))continue;
                    int born=1+ring*5;double radius=.4+born*.5;
                    Vec3 point=IceSpikeWave.point(Vec3.ZERO,arm,radius,wave.baseYaw());
                    pose.pushPose();pose.translate(point.x,surface-wave.getY()+rise,point.z);pose.mulPose(Axis.YP.rotationDegrees(wave.baseYaw()+arm*12));
                    legacyHeadModel(new ResourceLocation("nyctocosm",EXTRA[5]),pose,buffers,LightTexture.FULL_BRIGHT);pose.popPose();
                }
            }
        }
    }
    private static final class AreaRenderer extends EmptyRenderer<SpellArea>{
        AreaRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public void render(SpellArea area,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            if(area.spell()==Spell.SUBMERGENCE)return;
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            if(area.spell()==Spell.REFLECTANCE)pose.mulPose(Axis.XP.rotationDegrees(90));
            model(new ResourceLocation("nyctocosm","magic/spell_circles/"+area.spell().id()),pose,buffers,LightTexture.FULL_BRIGHT);pose.popPose();
        }
    }
}
