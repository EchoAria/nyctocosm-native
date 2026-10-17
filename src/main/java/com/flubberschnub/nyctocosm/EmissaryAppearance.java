package com.flubberschnub.nyctocosm;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.CompoundTag;

/** Source variants change individual display items and persist across animation changes. */
public final class EmissaryAppearance {
    private record Frame(int tick,List<String> variants){}
    private static final Map<String,List<String>> BONES=new HashMap<>();
    private static final Map<String,List<Frame>> FRAMES=new HashMap<>();
    private static final Map<String,Integer> DURATIONS=new HashMap<>();
    static {
        try(var stream=new GZIPInputStream(Objects.requireNonNull(EmissaryAppearance.class.getResourceAsStream("/assets/nyctocosm/native_rigs/ajemissary.json.gz")));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();
            root.getAsJsonObject("variants").entrySet().forEach(e->BONES.put(e.getKey(),List.copyOf(e.getValue().getAsJsonObject().keySet())));
            root.getAsJsonObject("animations").entrySet().forEach(e->{var rows=e.getValue().getAsJsonArray();DURATIONS.put(e.getKey(),rows.get(rows.size()-1).getAsJsonObject().get("tick").getAsInt()+1);});
            root.getAsJsonObject("variant_keyframes").entrySet().forEach(e->{
                var frames=new ArrayList<Frame>();
                for(var row:e.getValue().getAsJsonArray()){
                    var names=new ArrayList<String>();row.getAsJsonObject().getAsJsonArray("variants").forEach(v->names.add(v.getAsString()));
                    frames.add(new Frame(row.getAsJsonObject().get("tick").getAsInt(),List.copyOf(names)));
                }
                FRAMES.put(e.getKey(),List.copyOf(frames));
            });
        }catch(Exception failure){throw new IllegalStateException("Invalid source Emissary variant timeline",failure);}
    }
    public static CompoundTag initial(int phase){
        var state=new CompoundTag();apply(state,"default");apply(state,phase>=4?"armorless":"armorful");
        apply(state,phase==2?"wing_config_1":"wing_config_"+Math.max(0,phase-1));
        apply(state,"swordless");apply(state,"sweepless");return state;
    }
    private static void apply(CompoundTag state,String variant){for(String bone:BONES.getOrDefault(variant,List.of()))state.putString(bone,variant);}
    public static CompoundTag at(CompoundTag before,String clip,int frame){
        var state=before.copy();for(var cue:FRAMES.getOrDefault(clip,List.of()))if(cue.tick==frame)for(String variant:cue.variants)apply(state,variant);return state;
    }
    public static int duration(String clip){return DURATIONS.getOrDefault(clip,1);}
    private EmissaryAppearance(){}
}
