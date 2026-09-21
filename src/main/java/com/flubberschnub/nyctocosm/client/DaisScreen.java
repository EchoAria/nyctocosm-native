package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.DaisMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DaisScreen extends AbstractContainerScreen<DaisMenu>{
    public DaisScreen(DaisMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        int x=leftPos,y=topPos;graphics.fill(x-2,y-2,x+178,y+168,0xff27343e);graphics.fill(x,y,x+176,y+166,0xffc7c7c7);
        for(var slot:menu.slots){graphics.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xff383838);graphics.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xff929292);}
        graphics.drawString(font,"+",x+58,y+44,0xff3b4350,false);graphics.drawString(font,"→",x+105,y+44,0xff3b4350,false);
        graphics.drawString(font,Component.translatable("gui.nyctocosm.dais_hint"),x+15,y+65,0xff344052,false);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
