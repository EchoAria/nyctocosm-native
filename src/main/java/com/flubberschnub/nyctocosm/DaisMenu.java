package com.flubberschnub.nyctocosm;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Native two-input crafting container with the exact four original dais trade costs. */
public final class DaisMenu extends AbstractContainerMenu {
    public record Recipe(Item first,int firstCount,Item second,int secondCount,Item result,int resultCount){
        boolean matches(ItemStack a,ItemStack b){return a.is(first)&&a.getCount()>=firstCount&&b.is(second)&&b.getCount()>=secondCount;}
    }
    public static Recipe[] recipes(){return new Recipe[]{
        new Recipe(Items.SOUL_SAND,4,Items.BLAZE_ROD,1,Content.get("seelic_essence"),4),
        new Recipe(Content.get("seelic_essence"),20,Items.GOLD_INGOT,4,Content.get("planar_coil"),1),
        new Recipe(Content.get("seelic_essence"),40,Content.get("nightmare_scale"),52,Content.get("wyrmsliver"),1),
        new Recipe(Content.get("seelic_essence"),40,Content.get("null_splinter"),6,Content.get("nullfissure"),1)};}
    private final SimpleContainer inputs=new SimpleContainer(2);
    private final ResultContainer result=new ResultContainer();
    private final ContainerLevelAccess access;
    private boolean crafting;
    public DaisMenu(int id,Inventory inventory){this(id,inventory,ContainerLevelAccess.NULL);}
    public DaisMenu(int id,Inventory inventory,ContainerLevelAccess access){
        super(Content.DAIS_MENU.get(),id);this.access=access;
        inputs.addListener(container->slotsChanged(container));
        addSlot(new Slot(inputs,0,35,40));addSlot(new Slot(inputs,1,71,40));
        addSlot(new Slot(result,0,129,40){
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return current()!=null;}
            @Override public void onTake(Player player,ItemStack stack){
                Recipe recipe=current();
                if(recipe!=null){crafting=true;inputs.removeItem(0,recipe.firstCount);inputs.removeItem(1,recipe.secondCount);crafting=false;slotsChanged(inputs);}
                super.onTake(player,stack);
            }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    private Recipe current(){for(Recipe r:recipes())if(r.matches(inputs.getItem(0),inputs.getItem(1)))return r;return null;}
    @Override public void slotsChanged(Container container){
        if(crafting)return;
        Recipe recipe=current();result.setItem(0,recipe==null?ItemStack.EMPTY:new ItemStack(recipe.result,recipe.resultCount));broadcastChanges();
    }
    @Override public boolean stillValid(Player player){return stillValid(access,player,Content.DAIS.get());}
    @Override public ItemStack quickMoveStack(Player player,int index){
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index==2){
            int capacity=0;
            for(int n=3;n<39;n++){
                Slot destination=slots.get(n);ItemStack present=destination.getItem();
                if(present.isEmpty())capacity+=Math.min(destination.getMaxStackSize(stack),stack.getMaxStackSize());
                else if(ItemStack.isSameItemSameTags(present,stack))capacity+=Math.max(0,Math.min(destination.getMaxStackSize(stack),stack.getMaxStackSize())-present.getCount());
            }
            if(capacity<stack.getCount()||!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;
            slot.onQuickCraft(stack,copy);
        }
        else if(index<2){if(!moveItemStackTo(stack,3,39,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,2,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);return copy;
    }
    @Override public void removed(Player player){super.removed(player);access.execute((level,pos)->clearContainer(player,inputs));}
}
