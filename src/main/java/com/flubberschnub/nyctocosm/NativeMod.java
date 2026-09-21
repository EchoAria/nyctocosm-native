package com.flubberschnub.nyctocosm;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NativeMod.ID)
public final class NativeMod {
    public static final String ID = "nyctocosm";
    public NativeMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        Content.ITEMS.register(bus);
        Content.ENTITIES.register(bus);
        Content.TABS.register(bus);
        Content.SOUNDS.register(bus);
        Content.BLOCKS.register(bus);
        Content.MENUS.register(bus);
        Content.LOOT_MODIFIERS.register(bus);
        NativeNetwork.register();
        bus.addListener(this::attributes);
        MinecraftForge.EVENT_BUS.register(new NativeEvents());
    }
    private void attributes(EntityAttributeCreationEvent event) {
        event.put(Content.SERPENT.get(), NativeBoss.attributes(1000, 20,200).build());
        event.put(Content.SORCERER.get(), NativeBoss.attributes(1000, 15).build());
        event.put(Content.EMISSARY.get(), NativeBoss.attributes(1200, 35).build());
        event.put(Content.DEATH_WALKER.get(), NativeBoss.attributes(80, 12).build());
        event.put(Content.APPARITION.get(), NativeBoss.attributes(30, 8).build());
        event.put(Content.NULLPOINT.get(), NullpointStaff.attributes().build());
    }
}
