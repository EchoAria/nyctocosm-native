package com.flubberschnub.nyctocosm;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent one-shot bookkeeping for native events embedded in structures. */
final class StructureEventSavedData extends SavedData {
    private static final String STORAGE_KEY = NativeMod.ID + "_structure_events";
    private static final String PROCESSED_TRIGGERS = "ProcessedCryptSlatehengeTriggers";
    private final Set<Long> processed = new HashSet<>();

    static StructureEventSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(StructureEventSavedData::load,
            StructureEventSavedData::new, STORAGE_KEY);
    }

    static StructureEventSavedData load(CompoundTag tag) {
        var data = new StructureEventSavedData();
        for (long position : tag.getLongArray(PROCESSED_TRIGGERS)) data.processed.add(position);
        return data;
    }

    boolean processed(net.minecraft.core.BlockPos position) {
        return processed.contains(position.asLong());
    }

    void markProcessed(net.minecraft.core.BlockPos position) {
        if (processed.add(position.asLong())) setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLongArray(PROCESSED_TRIGGERS, processed.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
