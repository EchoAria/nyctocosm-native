package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.NativeMod;
import com.flubberschnub.nyctocosm.WeaponEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-local continuation for the three delayed rays in Dissonance transmutation. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class WeaponEffectController {
    private record Pending(Vec3 eye,Vec3 forward,int age){}
    private static final List<Pending> PENDING=new ArrayList<>();

    public static void play(int kind,Vec3 eye,Vec3 end,Vec3 forward){
        var level=Minecraft.getInstance().level;if(level==null)return;
        WeaponEffects.clientOffhand(level,kind,eye,end,forward);
        if(kind==3)PENDING.add(new Pending(eye,forward,0));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var level=Minecraft.getInstance().level;
        if(level==null){PENDING.clear();return;}
        for(int index=PENDING.size()-1;index>=0;index--){
            Pending pending=PENDING.get(index);
            WeaponEffects.clientDissonanceAfterburst(level,pending.eye,pending.forward,pending.age);
            if(pending.age>=2)PENDING.remove(index);else PENDING.set(index,new Pending(pending.eye,pending.forward,pending.age+1));
        }
    }
    private WeaponEffectController(){}
}
