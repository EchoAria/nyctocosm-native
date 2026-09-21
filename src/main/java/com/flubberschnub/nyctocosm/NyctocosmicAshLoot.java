package com.flubberschnub.nyctocosm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;

/** The source gravel table chooses ash or normal gravel loot with equal entry weights. */
public final class NyctocosmicAshLoot extends LootModifier {
    public static final Codec<NyctocosmicAshLoot> CODEC=RecordCodecBuilder.create(instance->codecStart(instance).apply(instance,NyctocosmicAshLoot::new));
    public NyctocosmicAshLoot(LootItemCondition[] conditions){super(conditions);}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        if(context.getRandom().nextBoolean()){loot.clear();loot.add(new ItemStack(Content.get("nyctocosmic_ash")));}
        return loot;
    }
    @Override public Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier> codec(){return CODEC;}
}
