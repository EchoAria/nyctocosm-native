package com.flubberschnub.nyctocosm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import net.minecraft.core.registries.Registries;

public final class Content {
    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,NativeMod.ID);
    public static final RegistryObject<com.mojang.serialization.Codec<NyctocosmicAshLoot>> ASH_LOOT=LOOT_MODIFIERS.register("nyctocosmic_ash",()->NyctocosmicAshLoot.CODEC);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NativeMod.ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NativeMod.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NativeMod.ID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,NativeMod.ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,NativeMod.ID);
    public static final RegistryObject<net.minecraft.world.level.block.Block> DAIS=BLOCKS.register("dais",DaisBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.Block> PORTAL=BLOCKS.register("portal",NativePortalBlock::new);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<DaisMenu>> DAIS_MENU=MENUS.register("dais",()->new net.minecraft.world.inventory.MenuType<>(DaisMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final Map<String, RegistryObject<Item>> ALL = new LinkedHashMap<>();
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NativeMod.ID);
    public static final Map<String,RegistryObject<net.minecraft.sounds.SoundEvent>> SOUND_MAP=new LinkedHashMap<>();
    public static final String[] ARTIFACTS = {"transposition","amelioration","levitation","absorption","accumulation",
        "protection_dream","protection_radiance","protection_resonance","protection_nightmare","protection_void","protection_dissonance","expulsion","extraction"};
    public static final RegistryObject<EntityType<NativeBoss>> SERPENT = boss("serpent", 0, 3, 3);
    public static final RegistryObject<EntityType<NativeBoss>> SORCERER = boss("sorcerer", 1, 1.2f, 3.6f);
    public static final RegistryObject<EntityType<NativeBoss>> EMISSARY = boss("emissary", 2, 2, 5);
    public static final RegistryObject<EntityType<NativeBoss>> DEATH_WALKER = boss("death_walker", 3, 1.2f, 3);
    public static final RegistryObject<EntityType<NativeBoss>> APPARITION = boss("nightmare_apparition", 4, 1, 2.5f);
    public static final RegistryObject<EntityType<SpellBolt>> BOLT = ENTITIES.register("spell_bolt", () ->
        EntityType.Builder.<SpellBolt>of(SpellBolt::new, MobCategory.MISC).sized(.35f,.35f).clientTrackingRange(96).updateInterval(1).build("nyctocosm:spell_bolt"));
    public static final RegistryObject<EntityType<SeelicField>> FIELD = ENTITIES.register("seelic_field", () ->
        EntityType.Builder.<SeelicField>of(SeelicField::new, MobCategory.MISC).sized(1,.2f).clientTrackingRange(64).updateInterval(1).build("nyctocosm:seelic_field"));
    public static final RegistryObject<EntityType<SpellArea>> AREA = ENTITIES.register("spell_area", () ->
        EntityType.Builder.<SpellArea>of(SpellArea::new,MobCategory.MISC).sized(1,.2f).clientTrackingRange(64).updateInterval(1).build("nyctocosm:spell_area"));
    public static final RegistryObject<EntityType<NullpointStaff>> NULLPOINT = ENTITIES.register("nullpoint_staff", () ->
        EntityType.Builder.<NullpointStaff>of(NullpointStaff::new,MobCategory.MONSTER).sized(1.1f,3.2f).fireImmune().clientTrackingRange(128).updateInterval(1).build("nyctocosm:nullpoint_staff"));
    public static final RegistryObject<EntityType<IceSpikeWave>> ICE_SPIKE_WAVE = ENTITIES.register("ice_spike_wave", () ->
        EntityType.Builder.<IceSpikeWave>of(IceSpikeWave::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(96).updateInterval(1).build("nyctocosm:ice_spike_wave"));
    public static final RegistryObject<EntityType<SerpentBeamHazard>> SERPENT_BEAM = ENTITIES.register("serpent_beam_hazard", () ->
        EntityType.Builder.<SerpentBeamHazard>of(SerpentBeamHazard::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:serpent_beam_hazard"));
    static {
        try(var input=Content.class.getResourceAsStream("/assets/nyctocosm/sounds.json")) {
            var sounds=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(String id:sounds.keySet())SOUND_MAP.put(id,SOUNDS.register(id,()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(NativeMod.ID,id))));
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot load original sound catalog",e);}
        for (String id : new String[]{"nyctocosmic_ash","seelic_essence","null_splinter","void_shard","nightmare_scale"}) item(id, () -> new Item(new Item.Properties()));
        item("dais",()->new BlockItem(DAIS.get(),new Item.Properties().rarity(Rarity.RARE)));
        item("soul_bottle", () -> new FocusItem(false));
        item("planar_coil", () -> new FocusItem(true));
        item("seelic_scythe", () -> new WeaponItem(0, 12, -2.4f));
        item("wyrmsliver", () -> new WeaponItem(1, 15, -2.7f));
        item("nullfissure", () -> new WeaponItem(2, 16.5f, -3));
        item("semblanceofdoom", () -> new WeaponItem(3, 20, -3.3f));
        for (String id : ARTIFACTS) item("artifact_"+id, () -> new ArtifactItem(id));
        for (String id : new String[]{"ancient_journals","catechism","dreams","axiotopology","adherent_serpent","adherent_sorcerer","adherent_emissary"})
            item(id, () -> new LoreItem(id));
        egg("serpent", SERPENT, 0x12051e, 0x7c27a8);
        egg("sorcerer", SORCERER, 0x171625, 0x00b2dd);
        egg("emissary", EMISSARY, 0xefd38b, 0xa74209);
        egg("death_walker", DEATH_WALKER, 0x18101f, 0x9157bd);
        egg("nightmare_apparition", APPARITION, 0x170520, 0xa40cc6);
    }
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("nyctocosm", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.nyctocosm")).icon(() -> new ItemStack(get("seelic_scythe")))
        .displayItems((parameters, output) -> ALL.values().forEach(item -> {
            output.accept(item.get());
            if(item.get() instanceof LoreItem lore)for(int chapter=1;chapter<=lore.chapterCount();chapter++)output.accept(lore.chapterStack(chapter));
        })).build());
    private static RegistryObject<EntityType<NativeBoss>> boss(String id, int kind, float width, float height) {
        return ENTITIES.register(id, () -> EntityType.Builder.<NativeBoss>of((type, level) -> new NativeBoss(type,level,kind), MobCategory.MONSTER)
            .sized(width,height).fireImmune().clientTrackingRange(128).updateInterval(1).build("nyctocosm:"+id));
    }
    private static void egg(String id, RegistryObject<EntityType<NativeBoss>> entity, int first, int second) {
        item(id+"_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(entity, first, second, new Item.Properties()));
    }
    private static void item(String id, Supplier<Item> factory) { ALL.put(id, ITEMS.register(id,factory)); }
    public static Item get(String id) { return ALL.get(id).get(); }
    private Content() {}
}
