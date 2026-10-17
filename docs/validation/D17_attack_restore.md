# D17 Sorcerer and Death Walker Attack Restore

Date: 2026-09-30  
Scope: resume native Sorcerer attacks and the Death Walker slam from serialized entity state without resetting attack progress.

## Source evidence

- `_nyctocosm_source/datapack/data/sorcerer/functions/load.mcfunction` registers per-attack timer objectives and global Sorcerer attack state. SHA-256: `08419a9dd64faee949e409e97a101a176744c507921cd394cdad6df12d105299`.
- `_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/death_walker/death_walker_tick.mcfunction` dispatches the persistent attack state; SHA-256: `7397a43a81f5611c560b9791a3b6302501141f5233ab5464872a7bfecc0d1293`. In the generated slam animation, `leaf_49` and `leaf_51` do not call the hit function, while `leaf_50` does; their SHA-256 values are respectively `c38853c8dad1db27e0ca28d40a42eed2e869c8e8a42b56fa2bc6b0cec5c54d74`, `e7462e2b9edc108b4446ab34f3ef7fab9890c54937bb656529806558f8d977ac`, and `ac9507c72d6b11110a62777d12d5f67fdee3784095cf0247789f8a4d0a117b86`. The invoked `nyctocosm:mobs/death_walker/slam.mcfunction` SHA-256 is `e7274b56ee0096859c765880e45657c7d6b2d879ac905f105abab000b92eed6b`.
- Native timeline and persistence code: `NativeBoss.java` SHA-256 `1b76cb634d09929b25429bf25ef8dda3c5c11dd24743feef71651faa13c3f5dc`; `SorcererTimeline.java` SHA-256 `e69690ba88efb20fed8183a9022717f8e468f999e6116bdb68fc9011f3ad8f8c`; `NativeGameTests.java` SHA-256 `2ce2cba4bd35eb94474617ca6a09a647c3b0cecfcd9dd3498fe9cc399575747f`.

## Change

Sorcerer saves now include the active attack ID, attack age, remaining duration, animation age, and the current target UUID. Read restores those fields and resumes the mapped attack instead of forcing it to idle. ICE_SPIKES retains its distinct attack ID although its presentation clip is `idle`. HaloBeam saves already-generated node positions so later beams keep their original origins. Hail and Meteor remain independently age-tracked; resuming their parent attack uses the saved attack age and does not replay an earlier timeline event. Unknown or expired attack IDs safely remain idle.

Death Walker kind 3 restores `slam` only when its saved clip and positive remaining duration identify an active slam. It restores the animation age and keeps state 3 until the existing final frame; invalid state combinations fall back to an ordinary state. The source animation event is age/frame 50 (`leaf_50`); in the native countdown this is the tick where remaining 42 advances to 41 and deals damage. A save at remaining 41 / animation age 50 advances to 40 without replaying impact.

## Verification

Five new GameTests cover every `SorcererTimeline` attack ID, target and timer round-trip, HaloBeam node positions, Shard Hail wave continuation, Void Meteor's unfired frame 30 plus the following no-replay tick, and Death Walker slam state on both sides of impact. Both Death Walker cases now save one entity, remove it, and load a newly created entity instance before ticking the resumed attack.

- `gradlew.bat compileJava --offline --console=plain`: **BUILD SUCCESSFUL**.
- Latest `gradlew.bat runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d17-target-restore-verified-20260930"`: **175/179 required tests passed**; all five D17 tests passed, including resolution of the saved Sorcerer target UUID to the live player after load. Four failures: `serpent_beam_selects_late_target_after_reload`, `flame_geysers_caps_at_two_players_without_cross_spreading_node_groups`, `flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`, and `serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`.
- `gradlew.bat build --offline --console=plain` after the UUID assertion: **BUILD SUCCESSFUL**. Earlier isolated runs also showed a transient D17 Meteor assertion error: the test treated the source's `-2` pre-rain sentinel as advancing to `-1`. The fixture now checks the actual frame-30 transition `0 → 1` after resuming at frame 29.
- An isolated dedicated-server smoke used `run-d17-server-restart-smoke-20260930` (new test world, local-only RCON): summoned Death Walker at animation age 49 / remaining 42, ran `save-all flush`, stopped the server normally, then restarted the same world. The saved entity region `d17-restart-world/entities/r.0.0.mca` retained `id=nyctocosm:death_walker`, `Animation=slam`, `Remaining=41`, `AnimationAge=50`, `OrdinaryState=3`, matching the source impact boundary. After restart, the headless server had no connected player and `@e` returned no loaded Death Walker even with chunk 0,0 force-loaded; this confirms the on-disk record survives restart but does not confirm runtime entity activation in a player-ticking chunk.
- `git diff --check` on the edited Java files reported no whitespace errors; Git emitted only its existing LF/CRLF normalization notices.
- The alpha.12 audit reported `errors=[]`, 3,024 JAR entries, 55,716,915 bytes, SHA-256 `2cf4574920fe35ab870da8403cd65758a7d2acee796995b2a31ca68c91f7a92c`. The prior audit is preserved at `native-audit-before-d17-attack-restore-20260930.json`; the refreshed report is `docs/native-audit.json`. The audit checks that GameTest/client smoke classes and runtime mcfunctions are absent from the JAR.

Status remains **implementation in progress**. New-entity NBT fixtures pass, and a real dedicated-server process restart preserved the slam entity record on disk. Runtime entity activation on a player-loaded chunk and client-visible animation continuity remain unverified. Existing Nullpoint and six-phase regression cases ran in the same complete suite but that suite had the unrelated failures listed above. The smoke used only a new isolated test world; no existing save, source pack, MDK, or installed JAR was modified; version remains alpha.12.

See [D17 source and solution notes](../../../GAMEPLAY_REFACTOR_PLAN.md#d17--sorcerer--death-walker-攻击恢复-s2) and [the parity TODO](../../../MAINLINE_GAMEPLAY_PARITY_TODO.md).
