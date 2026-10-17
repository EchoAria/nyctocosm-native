package com.flubberschnub.nyctocosm;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Lossless conversion for the identifiable CustomModelData-era inventory items. */
public final class LegacyMigration {
    private static final Map<String,String> TAG_ITEMS=new LinkedHashMap<>();
    static{
        TAG_ITEMS.put("soul_bottle","soul_bottle");TAG_ITEMS.put("planar_circlet","planar_coil");
        TAG_ITEMS.put("scythe","seelic_scythe");TAG_ITEMS.put("wyrmsliver","wyrmsliver");TAG_ITEMS.put("nullfissure","nullfissure");TAG_ITEMS.put("semblanceofdoom","semblanceofdoom");
        TAG_ITEMS.put("seelicessence","seelic_essence");TAG_ITEMS.put("nullsplinter","null_splinter");TAG_ITEMS.put("voidshard","void_shard");TAG_ITEMS.put("nightmarescale","nightmare_scale");
        for(String artifact:Content.ARTIFACTS)TAG_ITEMS.put(artifact,"artifact_"+artifact);
    }

    public static int migrate(Player player){
        int converted=0;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            ItemStack old=player.getInventory().getItem(slot),replacement=convert(old);
            if(replacement!=old){player.getInventory().setItem(slot,replacement);converted++;}
        }
        migrateScores(player);
        return converted;
    }

    public static boolean hasLegacyScores(Player player){
        var board=player.getScoreboard();
        return board.getObjective("m.hypertrace.charge")!=null||board.getObjective("l.storyline0")!=null||board.getObjective("tonytimer")!=null;
    }

    private static void migrateScores(Player player){
        CompoundTag data=player.getPersistentData();
        if(data.getBoolean("NyctocosmLegacyScoresMigrated"))return;
        var scoreboard=player.getScoreboard();String name=player.getScoreboardName();
        int charge=score(scoreboard,name,"m.hypertrace.charge");
        if(charge>0)data.putInt("NyctocosmHypertraceCharge",Math.min(15,charge));
        int maxCharge=score(scoreboard,name,"m.hypertrace.maxcharge");
        if(maxCharge>0)data.putInt("NyctocosmHypertraceMax",Math.min(15,maxCharge));
        int fear=score(scoreboard,name,"darknessmeter");
        if(fear>0)data.putInt("NyctocosmFearDark",Math.min(200,fear));
        int tony=score(scoreboard,name,"tonytimer");
        if(tony>0)data.putInt("NyctocosmTonyCooldown",tony);
        CompoundTag lore=data.getCompound("NyctocosmLore");
        String[] ids={"ancient_journals","catechism","dreams","axiotopology"};
        for(int i=0;i<ids.length;i++){
            int chapter=score(scoreboard,name,"l.storyline"+i);
            if(chapter>0)lore.putInt(ids[i],chapter);
        }
        if(!lore.isEmpty())data.put("NyctocosmLore",lore);
        data.putBoolean("NyctocosmLegacyScoresMigrated",true);
    }

    private static int score(net.minecraft.world.scores.Scoreboard scoreboard,String player,String objective){
        var found=scoreboard.getObjective(objective);
        return found==null?0:scoreboard.getOrCreatePlayerScore(player,found).getScore();
    }

    static ItemStack convert(ItemStack old){
        if(old.isEmpty()||old.getItem().builtInRegistryHolder().key().location().getNamespace().equals(NativeMod.ID)||!old.hasTag())return old;
        CompoundTag tag=old.getTag();String id=null;
        for(var entry:TAG_ITEMS.entrySet())if(tag.contains(entry.getKey())){id=entry.getValue();break;}
        int chapter=0;
        if(id==null&&old.is(Items.WRITTEN_BOOK)){
            if(tag.contains("storyline"))id=switch(tag.getInt("storyline")){case 0->"ancient_journals";case 1->"catechism";case 2->"dreams";case 3->"axiotopology";default->null;};
            if(id==null){
                String title=tag.getString("title");
                if(title.startsWith("Entry ")){id="ancient_journals";chapter=wordNumber(title.substring(6));}
                else if(title.startsWith("Precept ")){id="catechism";chapter=wordNumber(title.substring(8));}
                else if(title.startsWith("Insight ")){id="dreams";chapter=wordNumber(title.substring(8));}
                else if(title.startsWith("Theorem ")){id="axiotopology";chapter=wordNumber(title.substring(8));}
                else id=title.equals("Adherent I")?"adherent_serpent":title.equals("Adherent II")?"adherent_sorcerer":title.equals("Adherent III")?"adherent_emissary":null;
            }
        }
        if(id==null)return old;
        ItemStack result=new ItemStack(Content.get(id),old.getCount());
        if(result.getItem() instanceof FocusItem)result.getOrCreateTag().putInt("Souls",Math.max(0,tag.contains("souls_held")?tag.getInt("souls_held"):1));
        if(result.getItem() instanceof LoreItem&&chapter>0)result.getOrCreateTag().putInt("Chapter",chapter);
        return result;
    }
    private static int wordNumber(String word){return switch(word){case "One"->1;case "Two"->2;case "Three"->3;case "Four"->4;case "Five"->5;default->0;};}
    private LegacyMigration(){}
}
