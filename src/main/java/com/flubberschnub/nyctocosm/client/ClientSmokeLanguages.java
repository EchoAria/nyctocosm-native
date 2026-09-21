package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

/** Development-only real client resource fallback checks. Never shipped. */
public final class ClientSmokeLanguages {
    private static Language original;
    public static void verify(){
        Minecraft mc=Minecraft.getInstance();original=Language.getInstance();Map<String,Integer> passed=new LinkedHashMap<>();
        for(String locale:new String[]{"en_us","zh_cn","fr_fr"}){
            var language=ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us",locale),false);Language.inject(language);
            var expected=ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us",locale.equals("zh_cn")?"zh_cn":"en_us"),false);
            int count=0;
            for(var entry:Content.ALL.entrySet()){
                ItemStack stack=new ItemStack(entry.getValue().get());String name=expected.getOrDefault(stack.getDescriptionId(),"MISSING");
                if(!stack.getHoverName().getString().equals(name)||name.equals("MISSING"))throw new IllegalStateException("Name locale mismatch: "+locale+"/"+entry.getKey());
                String key="tooltip.nyctocosm.item."+entry.getKey(),description=expected.getOrDefault(key,"MISSING");
                if(description.equals("MISSING")||stack.getTooltipLines(mc.player,TooltipFlag.Default.NORMAL).stream().noneMatch(line->line.getString().equals(description)))
                    throw new IllegalStateException("Description locale mismatch: "+locale+"/"+entry.getKey());
                count++;
            }
            passed.put(locale,count);
        }
        Language.inject(original);
        try{java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("native-language-smoke.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(passed));}
        catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public static void show(String locale){
        var mc=Minecraft.getInstance();use(locale);
        mc.setScreen(new Screen(Component.literal("Language check: "+locale)){
            @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
                renderBackground(graphics);graphics.fill(8,8,width-8,height-8,0xee18212b);graphics.drawString(font,title,16,16,0xffffff,false);
                int y=34;
                for(String id:new String[]{"nyctocosmic_ash","soul_bottle","artifact_absorption","nullfissure"}){
                    var stack=new ItemStack(Content.get(id));
                    graphics.drawString(font,stack.getHoverName(),16,y,0x77ddff,false);y+=11;
                    for(var line:stack.getTooltipLines(mc.player,TooltipFlag.Default.NORMAL).stream().skip(1).toList()){
                        // Keep vanilla combat stats off this focused custom-description capture.
                        if(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents&&!contents.getKey().startsWith("tooltip.nyctocosm."))continue;
                        if(line.getString().isBlank())continue;
                        for(var wrapped:font.split(line,width-40)){graphics.drawString(font,wrapped,20,y,0xdddddd,false);y+=9;}
                    }
                    y+=5;
                }
            }
        });
    }
    public static void use(String locale){
        var mc=Minecraft.getInstance();
        Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us",locale),false));
    }
    public static void restore(){if(original!=null)Language.inject(original);}
}
