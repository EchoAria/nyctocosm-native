package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Render-only shake: source timing/radius/strength without changing player aim. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class PortalCameraEffects {
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;
        float strength=0;
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof PortalActivation opening)
            strength=Math.max(strength,ResonanceEffects.shakeStrength(opening.age(),opening.distanceToSqr(mc.player)));
        if(strength==0)return;
        double time=mc.level.getGameTime()+event.getPartialTick();
        event.setYaw(event.getYaw()+(float)(Math.sin(time*Math.PI*.8)*strength));
        event.setPitch(event.getPitch()+(float)(Math.cos(time*Math.PI*.7)*strength));
    }
    private PortalCameraEffects(){}
}
