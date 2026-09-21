package com.flubberschnub.nyctocosm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class LoreItem extends Item {
    private static final Map<String,Integer> CHAPTERS=loadChapters();
    private final String id;
    public LoreItem(String id){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));this.id=id;}
    private static Map<String,Integer> loadChapters(){
        try(var input=LoreItem.class.getResourceAsStream("/data/nyctocosm/lore/chapters.json")){
            var object=com.google.gson.JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input),StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String,Integer> result=new LinkedHashMap<>();object.entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsInt()));return Map.copyOf(result);
        }catch(IOException e){throw new IllegalStateException("Cannot load chapter catalog",e);}
    }
    public int chapterCount(){return CHAPTERS.getOrDefault(id,0);}
    public int chapter(ItemStack stack){return stack.hasTag()?Math.max(0,Math.min(chapterCount(),stack.getTag().getInt("Chapter"))):0;}
    public ItemStack chapterStack(int chapter){ItemStack stack=new ItemStack(this);stack.getOrCreateTag().putInt("Chapter",Math.max(0,Math.min(chapterCount(),chapter)));return stack;}
    public ItemStack book(ItemStack nativeStack){
        int chapter=chapter(nativeStack);String resource=id+(chapter>0?"_"+chapter:"");
        ItemStack stack=new ItemStack(Items.WRITTEN_BOOK);
        try(InputStream input=LoreItem.class.getResourceAsStream("/data/nyctocosm/lore/"+resource+".snbt")){
            if(input!=null)stack.setTag(TagParser.parseTag(new String(input.readAllBytes(),StandardCharsets.UTF_8)));
        }catch(Exception e){throw new IllegalStateException("Invalid original lore: "+id,e);}
        if(!stack.hasTag()){
            CompoundTag tag=stack.getOrCreateTag();tag.putString("title",id);tag.putString("author","Flubberschnub");
            ListTag pages=new ListTag();pages.add(StringTag.valueOf("{\"text\":\"This chapter has not been migrated yet.\"}"));tag.put("pages",pages);
        }
        return stack;
    }
    /** A newly found unbound volume reveals the next entry; existing volumes stay readable. */
    public void discover(Player player,ItemStack stack){
        if(chapterCount()==0||chapter(stack)>0)return;
        CompoundTag progress=player.getPersistentData().getCompound("NyctocosmLore");
        int next=Math.min(chapterCount(),Math.max(0,progress.getInt(id))+1);
        stack.getOrCreateTag().putInt("Chapter",next);progress.putInt(id,next);
        player.getPersistentData().put("NyctocosmLore",progress);
    }
    @Override public Component getName(ItemStack stack){
        return chapter(stack)>0?Component.translatable("item.nyctocosm.chapter_title",super.getName(stack),chapter(stack)):super.getName(stack);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        if(chapterCount()>0)lines.add(chapter(stack)==0?Component.translatable("tooltip.nyctocosm.lore_unread"):Component.translatable("tooltip.nyctocosm.lore_chapter",chapter(stack),chapterCount()));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer serverPlayer){discover(player,player.getItemInHand(hand));NativeNetwork.openBook(serverPlayer,book(player.getItemInHand(hand)));}
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
