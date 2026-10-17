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
    public static final RegistryObject<EntityType<NativeBoss>> DEATH_WALKER = boss("death_walker", 3, 3.6f, 12);
    public static final RegistryObject<EntityType<NativeBoss>> APPARITION = boss("nightmare_apparition", 4, 1, 2.5f);
    public static final RegistryObject<EntityType<SpellBolt>> BOLT = ENTITIES.register("spell_bolt", () ->
        EntityType.Builder.<SpellBolt>of(SpellBolt::new, MobCategory.MISC).sized(.35f,.35f).clientTrackingRange(96).updateInterval(1).build("nyctocosm:spell_bolt"));
    public static final RegistryObject<EntityType<SeelicField>> FIELD = ENTITIES.register("seelic_field", () ->
        EntityType.Builder.<SeelicField>of(SeelicField::new, MobCategory.MISC).sized(1,.2f).clientTrackingRange(64).updateInterval(1).build("nyctocosm:seelic_field"));
    public static final RegistryObject<EntityType<PortalActivation>> PORTAL_ACTIVATION = ENTITIES.register("portal_activation", () ->
        EntityType.Builder.<PortalActivation>of(PortalActivation::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:portal_activation"));
    public static final RegistryObject<EntityType<SpellArea>> AREA = ENTITIES.register("spell_area", () ->
        EntityType.Builder.<SpellArea>of(SpellArea::new,MobCategory.MISC).sized(1,.2f).clientTrackingRange(64).updateInterval(1).build("nyctocosm:spell_area"));
    public static final RegistryObject<EntityType<NullpointStaff>> NULLPOINT = ENTITIES.register("nullpoint_staff", () ->
        EntityType.Builder.<NullpointStaff>of(NullpointStaff::new,MobCategory.MONSTER).sized(1.1f,3.2f).fireImmune().clientTrackingRange(128).updateInterval(1).build("nyctocosm:nullpoint_staff"));
    public static final RegistryObject<EntityType<IceSpikeWave>> ICE_SPIKE_WAVE = ENTITIES.register("ice_spike_wave", () ->
        EntityType.Builder.<IceSpikeWave>of(IceSpikeWave::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(96).updateInterval(1).build("nyctocosm:ice_spike_wave"));
    public static final RegistryObject<EntityType<SerpentBeamHazard>> SERPENT_BEAM = ENTITIES.register("serpent_beam_hazard", () ->
        EntityType.Builder.<SerpentBeamHazard>of(SerpentBeamHazard::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:serpent_beam_hazard"));
    public static final RegistryObject<EntityType<SoulEntity>> SOUL = ENTITIES.register("soul", () ->
        EntityType.Builder.<SoulEntity>of(SoulEntity::new,MobCategory.MISC).sized(.2f,.2f).clientTrackingRange(32).updateInterval(5).build("nyctocosm:soul"));
    public static final RegistryObject<EntityType<ExpulsionBurst>> EXPULSION_BURST = ENTITIES.register("expulsion_burst", () ->
        EntityType.Builder.<ExpulsionBurst>of(ExpulsionBurst::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(32).updateInterval(20).build("nyctocosm:expulsion_burst"));
    public static final RegistryObject<EntityType<EmissaryGrappleSeat>> EMISSARY_GRAPPLE = ENTITIES.register("emissary_grapple", () ->
        EntityType.Builder.<EmissaryGrappleSeat>of(EmissaryGrappleSeat::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_grapple"));
    public static final RegistryObject<EntityType<EmissaryHitRegion>> EMISSARY_HIT_REGION = ENTITIES.register("emissary_hit_region", () ->
        EntityType.Builder.<EmissaryHitRegion>of(EmissaryHitRegion::new,MobCategory.MISC).sized(1,1).clientTrackingRange(96).updateInterval(1).build("nyctocosm:emissary_hit_region"));
    public static final RegistryObject<EntityType<EmissaryHazard>> EMISSARY_HAZARD = ENTITIES.register("emissary_hazard", () ->
        EntityType.Builder.<EmissaryHazard>of(EmissaryHazard::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_hazard"));
    public static final RegistryObject<EntityType<EmissarySeekingMissile>> EMISSARY_SEEKING_MISSILE = ENTITIES.register("emissary_seeking_missile", () ->
        EntityType.Builder.<EmissarySeekingMissile>of(EmissarySeekingMissile::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_seeking_missile"));
    public static final RegistryObject<EntityType<EmissarySwordPlunge>> EMISSARY_SWORD_PLUNGE = ENTITIES.register("emissary_sword_plunge", () ->
        EntityType.Builder.<EmissarySwordPlunge>of(EmissarySwordPlunge::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_sword_plunge"));
    public static final RegistryObject<EntityType<EmissaryWingBlastRay>> EMISSARY_WING_BLAST_RAY = ENTITIES.register("emissary_wing_blast_ray", () ->
        EntityType.Builder.<EmissaryWingBlastRay>of(EmissaryWingBlastRay::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_wing_blast_ray"));
    public static final RegistryObject<EntityType<EmissaryPillar>> EMISSARY_PILLAR = ENTITIES.register("emissary_pillar", () ->
        EntityType.Builder.<EmissaryPillar>of(EmissaryPillar::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_pillar"));
    public static final RegistryObject<EntityType<EmissaryFlameGeyser>> EMISSARY_FLAME_GEYSER = ENTITIES.register("emissary_flame_geyser", () ->
        EntityType.Builder.<EmissaryFlameGeyser>of(EmissaryFlameGeyser::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_flame_geyser"));
    public static final RegistryObject<EntityType<EmissaryRainShard>> EMISSARY_RAIN_SHARD = ENTITIES.register("emissary_rain_shard", () ->
        EntityType.Builder.<EmissaryRainShard>of(EmissaryRainShard::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_rain_shard"));
    public static final RegistryObject<EntityType<EmissaryCelestialTear>> EMISSARY_CELESTIAL_TEAR = ENTITIES.register("emissary_celestial_tear", () ->
        EntityType.Builder.<EmissaryCelestialTear>of(EmissaryCelestialTear::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_celestial_tear"));
    public static final RegistryObject<EntityType<EmissaryFlameSlashWave>> EMISSARY_FLAME_SLASH_WAVE = ENTITIES.register("emissary_flame_slash_wave", () ->
        EntityType.Builder.<EmissaryFlameSlashWave>of(EmissaryFlameSlashWave::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:emissary_flame_slash_wave"));
    /** Internal render carrier for the source giant-sword item model; deliberately omitted from the creative tab. */
    public static final RegistryObject<Item> EMISSARY_SWORD_VISUAL = ITEMS.register("emissary_sword_visual", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SORCERER_VOID_PARTICLE_VISUAL = ITEMS.register("sorcerer_void_particle_visual", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SORCERER_ICICLE_VISUAL = ITEMS.register("sorcerer_icicle_visual", () -> new Item(new Item.Properties()));
    public static final RegistryObject<EntityType<SorcererHail>> SORCERER_HAIL = ENTITIES.register("sorcerer_hail", () ->
        EntityType.Builder.<SorcererHail>of(SorcererHail::new,MobCategory.MISC).sized(.5f,1.975f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:sorcerer_hail"));
    public static final RegistryObject<EntityType<FrozenBlade>> FROZEN_BLADE = ENTITIES.register("frozen_blade", () ->
        EntityType.Builder.<FrozenBlade>of(FrozenBlade::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:frozen_blade"));
    public static final RegistryObject<EntityType<SorcererBeamNode>> SORCERER_BEAM_NODE = ENTITIES.register("sorcerer_beam_node", () ->
        EntityType.Builder.<SorcererBeamNode>of(SorcererBeamNode::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(128).updateInterval(1).build("nyctocosm:sorcerer_beam_node"));
    public static final RegistryObject<EntityType<CryptSlatehengeTrigger>> CRYPT_SLATEHENGE_TRIGGER = ENTITIES.register("crypt_slatehenge_trigger", () ->
        EntityType.Builder.<CryptSlatehengeTrigger>of(CryptSlatehengeTrigger::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(32).updateInterval(20).build("nyctocosm:crypt_slatehenge_trigger"));
    public static final RegistryObject<EntityType<SlatehengeEffect>> SLATEHENGE_EFFECT = ENTITIES.register("slatehenge_effect", () ->
        EntityType.Builder.<SlatehengeEffect>of(SlatehengeEffect::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(32).updateInterval(20).build("nyctocosm:slatehenge_effect"));
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
