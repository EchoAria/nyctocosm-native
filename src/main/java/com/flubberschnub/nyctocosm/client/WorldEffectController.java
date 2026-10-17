package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Pure visual effects advance independently of entity visibility and entity tick culling. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class WorldEffectController {
    private record AnimationState(String clip,int age,long epoch){}
    private static final Map<UUID,AnimationState> ANIMATIONS=new HashMap<>();
    private static ClientLevel previousLevel;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var level=mc.level;
        if(previousLevel!=level){ANIMATIONS.clear();previousLevel=level;}
        if(level==null||mc.isPaused())return;
        Set<UUID> live=new HashSet<>();List<SerpentBeamHazard> beams=new ArrayList<>();
        for(var entity:level.entitiesForRendering()){
            if(entity instanceof PortalActivation opening)opening.clientVisualTick();
            if(entity instanceof SorcererBeamNode node)node.clientVisualTick();
            if(entity instanceof SerpentBeamHazard beam&&beam.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())<=160*160)beams.add(beam);
            if(entity instanceof NativeBoss boss&&(boss.kind==1||boss.kind==2)){
                live.add(boss.getUUID());int age=(int)boss.animationTime(0);long epoch=level.getGameTime()-age;
                var previous=ANIMATIONS.get(boss.getUUID());
                int first=previous==null||!previous.clip.equals(boss.animation())||previous.epoch!=epoch?age:Math.max(previous.age+1,age-4);
                if(!boss.animation().equals("dashstrike")&&!boss.animation().equals("airdash"))for(int frame=first;frame<=age;frame++)BossParticleEffects.clientFrame(boss,frame);
                if(boss.kind==1&&boss.animation().equals("die")){
                    Vec3 at=EmissaryPose.localPoint(boss.position(),boss.getYRot(),boss.getXRot(),new Vec3(0,3,-.6));
                    for(int frame=first;frame<=age;frame++)SoulSwirlEffects.clientExplode(level,at,boss.getUUID().getLeastSignificantBits()^epoch,frame-56);
                    for(int frame=first;frame<=age;frame++){
                        SoulSwirlEffects.clientExplode(level,at,boss.getUUID().getLeastSignificantBits()^epoch^116,frame-116);
                        VoidVeinEffects.clientExplode(level,at,boss.getUUID().getMostSignificantBits()^epoch,frame-115);
                    }
                }else if(boss.animation().equals("singularitycharge")){
                    Vec3 at=EmissaryPose.localPoint(boss.position(),boss.getYRot(),boss.getXRot(),new Vec3(0,2,.5));
                    for(int frame=first;frame<=age;frame++)if(frame>=20&&frame<=41)
                        VoidSwirlEffects.clientSingle(level,at,boss.getUUID().getLeastSignificantBits()^epoch^frame,false,true);
                }
                ANIMATIONS.put(boss.getUUID(),new AnimationState(boss.animation(),age,epoch));
            }
        }
        ANIMATIONS.keySet().retainAll(live);
        int allocation=beams.isEmpty()?0:600/beams.size();
        for(var beam:beams)beam.clientVisualTick(allocation);
    }
    private WorldEffectController(){}
}
