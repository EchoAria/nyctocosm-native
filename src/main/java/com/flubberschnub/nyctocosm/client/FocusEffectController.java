package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Timed source effects remain visible after a bottle is consumed or its field disappears. */
@Mod.EventBusSubscriber(modid=NativeMod.ID,value=Dist.CLIENT)
public final class FocusEffectController {
    private static final class Active {
        final NativeNetwork.SourceVisual cue;final long start;int lastAge;
        Active(NativeNetwork.SourceVisual cue,long start){this.cue=cue;this.start=start;}
    }
    private static final List<Active> ACTIVE=new ArrayList<>();
    private static ClientLevel previousLevel;
    public static void play(NativeNetwork.SourceVisual cue){
        var level=Minecraft.getInstance().level;if(level==null)return;
        if(previousLevel!=level){ACTIVE.clear();previousLevel=level;}
        frame(level,cue,0);
        if(cue.clip().equals("release")||cue.clip().equals("plane")||cue.clip().equals("radiance")||cue.clip().startsWith("meteor_ring")||cue.clip().startsWith("repair_")&&duration(cue.clip())>1)ACTIVE.add(new Active(cue,level.getGameTime()));
    }
    private static void frame(ClientLevel level,NativeNetwork.SourceVisual cue,int age){
        if(cue.clip().equals("release"))SoulSwirlEffects.clientExplode(level,cue.at(),cue.seed(),age);
        else if(cue.clip().equals("repair_death_swirl"))EmissarySwirlEffects.clientFrame(level,cue.at(),cue.seed(),age);
        else BossParticleEffects.clientFrame(level,cue.clip().startsWith("repair_")?"combat_repair":"feedback",cue.clip(),cue.frame()+age,cue.at(),cue.yaw(),cue.pitch(),cue.at());
    }
    private static int duration(String clip){return clip.equals("repair_death_swirl")?30:clip.startsWith("repair_")?BossParticleEffects.duration("combat_repair",clip):clip.equals("release")?21:clip.equals("plane")||clip.equals("meteor_ring")?6:clip.equals("meteor_ring_small")?4:5;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var level=mc.level;
        if(previousLevel!=level){ACTIVE.clear();previousLevel=level;}
        if(level==null||mc.isPaused())return;
        ACTIVE.removeIf(active->{int age=(int)(level.getGameTime()-active.start);int duration=duration(active.cue.clip());
            for(int next=active.lastAge+1;next<=Math.min(age,duration-1);next++)frame(level,active.cue,next);
            active.lastAge=Math.max(active.lastAge,age);return age>=duration;});
    }
    private FocusEffectController(){}
}
