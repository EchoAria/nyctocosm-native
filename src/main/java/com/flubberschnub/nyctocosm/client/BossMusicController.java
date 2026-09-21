package com.flubberschnub.nyctocosm.client;

import com.flubberschnub.nyctocosm.Content;
import com.flubberschnub.nyctocosm.NativeBoss;
import com.flubberschnub.nyctocosm.NativeMod;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Plays the original streamed boss score as client music.  The data pack kept
 * per-player scoreboard timers; the native version keeps the same timing local
 * to each client, which also prevents one player's fight affecting another's.
 */
@Mod.EventBusSubscriber(modid = NativeMod.ID, value = Dist.CLIENT)
public final class BossMusicController {
    private static final int SERPENT_LENGTH = 1900;
    private static final int SORCERER_INTRO = 609;
    private static final int SORCERER_PART = 412;
    private static final int EMISSARY_INTRO = 119;
    private static final int EMISSARY_PART = 240;
    private static final String[][] EMISSARY_TRACKS = {
        {"phase1.part0", "phase1.part1", "phase1.part2", "phase1.part3", "phase1.part4"},
        {"phase2.part1", "phase2.part2", "phase2.part3", "phase2.part4"},
        {"phase3.part1", "phase3.part2", "phase3.part3", "phase3.part4"},
        {"phase4.part1", "phase4.part2"}
    };

    private static UUID bossId;
    private static int kind = -1;
    private static int group;
    private static int part;
    private static int remaining;
    private static boolean intro;
    private static boolean ending;
    private static SoundInstance playing;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            stop(minecraft);
            return;
        }

        NativeBoss active = bossId == null ? null : findById(minecraft, bossId);
        if (active != null && !active.isAlive()) {
            if (!ending) playEnding(minecraft);
            if (remaining > 0) remaining--;
            if (remaining <= 0) clearState();
            return;
        }

        if (active == null || !inMusicRange(minecraft, active)) {
            NativeBoss nearest = nearestBoss(minecraft);
            if (nearest == null) {
                stop(minecraft);
                return;
            }
            if (active == null || !nearest.getUUID().equals(active.getUUID())) start(minecraft, nearest);
            active = nearest;
        }

        // Keep vanilla ambient music from starting over the streamed boss score.
        if ((minecraft.level.getGameTime() & 31L) == 0L) minecraft.getMusicManager().stopPlaying();
        if (remaining > 0) remaining--;
        if (remaining <= 0) advance(minecraft, active);
    }

    private static NativeBoss nearestBoss(Minecraft minecraft) {
        AABB search = minecraft.player.getBoundingBox().inflate(300);
        List<NativeBoss> bosses = minecraft.level.getEntitiesOfClass(NativeBoss.class, search,
            boss -> boss.kind < 3 && boss.isAlive() && inMusicRange(minecraft, boss));
        return bosses.stream().min(Comparator.comparingDouble(minecraft.player::distanceToSqr)).orElse(null);
    }

    private static NativeBoss findById(Minecraft minecraft, UUID id) {
        for (var entity : minecraft.level.entitiesForRendering())
            if (entity instanceof NativeBoss boss && boss.getUUID().equals(id)) return boss;
        return null;
    }

    private static boolean inMusicRange(Minecraft minecraft, NativeBoss boss) {
        double range = boss.kind == 2 ? 300 : boss.kind == 1 ? 40 : 200;
        return minecraft.player.distanceToSqr(boss) <= range * range;
    }

    private static void start(Minecraft minecraft, NativeBoss boss) {
        stopSound(minecraft);
        bossId = boss.getUUID();
        kind = boss.kind;
        group = musicGroup(boss);
        part = 0;
        ending = false;
        minecraft.getMusicManager().stopPlaying();
        if (kind == 0) {
            intro = false;
            play(minecraft, "boss.music.serpent.loop", SERPENT_LENGTH);
        } else {
            intro = true;
            play(minecraft, kind == 1 ? "boss.music.sorcerer.intro" : "boss.music.emissary.intro",
                kind == 1 ? SORCERER_INTRO : EMISSARY_INTRO);
        }
    }

    private static void advance(Minecraft minecraft, NativeBoss boss) {
        if (kind == 0) {
            play(minecraft, "boss.music.serpent.loop", SERPENT_LENGTH);
            return;
        }
        int nextGroup = musicGroup(boss);
        if (intro || nextGroup != group) {
            group = nextGroup;
            part = 0;
            intro = false;
        }
        if (kind == 1) {
            String id = "boss.music.sorcerer.phase" + (group + 1) + ".part" + (part + 1);
            part = (part + 1) & 1;
            play(minecraft, id, SORCERER_PART);
        } else {
            String[] tracks = EMISSARY_TRACKS[group];
            play(minecraft, "boss.music.emissary." + tracks[part], EMISSARY_PART);
            // The original score settles into the final two-part phrase of each
            // phase until the next health threshold queues a new section.
            part++;
            if (part >= tracks.length) part = Math.max(0, tracks.length - 2);
        }
    }

    private static int musicGroup(NativeBoss boss) {
        float ratio = boss.getHealth() / boss.getMaxHealth();
        if (boss.kind == 1) return Math.min(5, Math.max(0, (int)((1F - ratio) * 6F)));
        if (boss.kind == 2) return Math.max(0, Math.min(3, boss.phase() - 1));
        return 0;
    }

    private static void playEnding(Minecraft minecraft) {
        ending = true;
        stopSound(minecraft);
        String id = switch (kind) {
            case 0 -> "boss.music.serpent.end";
            case 1 -> "boss.music.sorcerer.end";
            case 2 -> "boss.music.emissary.end";
            default -> null;
        };
        if (id == null) {
            clearState();
            return;
        }
        play(minecraft, id, kind == 2 ? EMISSARY_PART : kind == 1 ? SORCERER_PART : 200);
    }

    private static void play(Minecraft minecraft, String id, int ticks) {
        var registered = Content.SOUND_MAP.get(id);
        if (registered == null) {
            clearState();
            return;
        }
        SoundEvent sound = registered.get();
        playing = SimpleSoundInstance.forMusic(sound);
        minecraft.getSoundManager().play(playing);
        remaining = ticks;
    }

    private static void stop(Minecraft minecraft) {
        if (bossId == null) return;
        stopSound(minecraft);
        clearState();
    }

    private static void stopSound(Minecraft minecraft) {
        if (playing != null) minecraft.getSoundManager().stop(playing);
        playing = null;
    }

    private static void clearState() {
        bossId = null;
        kind = -1;
        remaining = 0;
        intro = false;
        ending = false;
        playing = null;
    }

    static boolean isPlaying(Minecraft minecraft) {
        return bossId != null && playing != null && minecraft.getSoundManager().isActive(playing);
    }

    private BossMusicController() {}
}
