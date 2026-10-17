package com.flubberschnub.nyctocosm;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Server-owned sound/helper events compiled from selected source command keyframes. */
final class BossEffectTimeline {
    private static final JsonObject EVENTS=load();
    private static JsonObject load(){
        try(var stream=Objects.requireNonNull(BossEffectTimeline.class.getResourceAsStream("/assets/nyctocosm/effects/combat_repair_events.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            return JsonParser.parseReader(reader).getAsJsonObject();
        }catch(Exception e){throw new IllegalStateException("Invalid combat cue timeline",e);}
    }
    static int soundCount(String clip,int frame){var event=event(clip,frame);return event==null?0:event.getAsJsonArray("sounds").size();}
    static int helperCount(String clip,int frame){var event=event(clip,frame);return event==null?0:event.getAsJsonArray("helpers").size();}
    private static JsonObject event(String clip,int frame){
        var frames=EVENTS.getAsJsonObject(clip);return frames==null||!frames.has(Integer.toString(frame))?null:frames.getAsJsonObject(Integer.toString(frame));
    }
    private static Vec3 origin(NativeBoss boss,String clip,int frame,JsonObject cue){
        String name=cue.get("locator").getAsString();return name.isEmpty()?boss.position():EmissaryDamagePose.worldPosition(boss,EmissaryDamagePose.sampleAt(boss,clip,name,frame));
    }
    static void serverFrame(ServerLevel server,NativeBoss boss,String clip,int frame){
        var event=event(clip,frame);if(event==null)return;
        for(var element:event.getAsJsonArray("sounds")){
            var cue=element.getAsJsonObject();NativeEvents.resonanceSound(server,origin(boss,clip,frame,cue),cue.get("id").getAsString(),cue.get("volume").getAsFloat(),cue.get("radius").getAsDouble());
        }
        for(var element:event.getAsJsonArray("helpers")){
            var cue=element.getAsJsonObject();Vec3 at=origin(boss,clip,frame,cue);String id=cue.get("clip").getAsString();
            NativeNetwork.sourceVisual(server,id,at,boss.getYRot(),cue.get("pitch").getAsFloat());
        }
    }
    private BossEffectTimeline(){}
}
