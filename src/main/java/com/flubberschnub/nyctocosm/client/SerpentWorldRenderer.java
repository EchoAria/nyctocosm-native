package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.NativeBoss;
import com.flubberschnub.nyctocosm.NativeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The visible tail must not inherit the head's section visibility or its underground lighting. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class SerpentWorldRenderer {
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();Vec3 camera=event.getCamera().getPosition();
        float partial=event.getPartialTick();boolean drawn=false;
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof NativeBoss boss&&boss.kind==0&&!boss.isRemoved()){
            var segments=boss.serpentRenderSegments(partial);
            for(int part=0;part<segments.length;part++){
                Vec3 at=segments[part];AABB bounds=new AABB(at,at).inflate(8);
                if(!event.getFrustum().isVisible(bounds)||at.distanceToSqr(camera)>256*256)continue;
                pose.pushPose();Vec3 offset=at.subtract(camera);pose.translate(offset.x,offset.y,offset.z);
                String model=part==0?"serpent_head":part==41?"serpent_tail":"serpent_segment";
                float yaw=boss.serpentSegmentYaw(part,partial);
                float pitch=boss.serpentSegmentPitch(part,partial);
                int light=LevelRenderer.getLightColor(mc.level,BlockPos.containing(at.add(0,1.6,0)));
                ClientEvents.serpentModel(new ResourceLocation(NativeMod.ID,"serpent/"+model),yaw,pitch,pose,buffers,light);
                pose.popPose();drawn=true;
            }
        }
        if(drawn)buffers.endBatch();
    }
}
