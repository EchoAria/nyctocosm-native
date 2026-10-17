# D35 Sorcerer Ice Spikes

Date: 2026-09-30
Status: **implemented; server GameTests and build/audit pass; client join and real chunk unload/reload remain unverified**.

## Source call chain and timing

- Sorcerer attack `icespikes` dispatches event age 40 from `SorcererTimeline`; the source animation leaf `animated_java/.../emit/tree/leaf_40_effects_0.mcfunction` calls `sorcerer:attacks/icespikes/wave` at the staff model position plus world Y 2. The source start function emits the Nullpoint animation; its independent attack markers advance through `sorcerer:attacks/icespikes/subtick`.
- `wave.mcfunction` summons 30 radial markers at local forward 0.4, rotated every 12 degrees. Generated block 51 increments `ns.icespikestimer`, moves each marker 0.5, removes one point from `value`, then triggers block 52 at score `..0`. No source assignment initializes `value` on the new marker. A GameTest ran the vanilla scoreboard command `scoreboard players remove D35Missing d35v 1` on an unset score and read back **-1**, confirming the first advancement triggers immediately; block 52 resets it to 5. Birth ages are therefore **1, 6, 11, 16, 21** and radii are **0.9, 3.4, 5.9, 8.4, 10.9**. At wave age 25, the wave marker is killed after the last ring.
- Block 52 re-runs the source surface search and summons each armor-stand spike two blocks below that point. The spike entity has not been visited yet in that subtick: its first timer step is on the following world tick. Generated blocks 48–50 move the root up 0.4 on timer ages 1–5, check players at the root on ages 6–10, move down 0.4 on ages 20–24, and kill at age 25. `hit.mcfunction` uses `@a[distance=..2]`, so its origin is the spike root and its distance is a player-feet sphere.
- The source `passthrough` block tag skips air, fluids, vegetation, snow and other listed blocks. Initial marker placement scans from world height 320; later spike placements scan from the marker plus five. The native search is bounded to dimension height limits, checks the same block tag, and skips unavailable chunks without loading them.

## Native change

`IceSpikeWave` now owns a persisted wave age, base yaw, 30 moving-marker surface anchors and 150 per-ring/per-arm spike surface anchors. The ring schedule is fixed from the source event sequence; each spike's rise, root-height impact window and descent are derived from that absolute age. The parent remains alive until the final spike reaches source timer age 25. A fresh entity loaded from NBT resumes the saved age and all surface anchors without replaying old rings.

The same authoritative state is published in a synchronized compound tag for tracked clients and late joiners to receive the current age, orientation and existing spike anchors. `ClientEvents` renders only the spikes that remain alive and offsets each by its saved surface and source rise/descent phase. Live client late-join animation has not been exercised yet.

Damage broad-phase queries players only, then applies the exact inclusive feet-distance check `<= 2`; it keeps the Forge damage path and current status application behavior. This change does not decide the pending D28/D42 behavior for canceled damage. Non-player mobs no longer receive this source player-only hit.

## Red/green verification

Before the implementation, `run-d35-red-20260930` added two red assertions: first-ring impact at wave tick 7 and no damage to non-player mobs. Both failed on the prior native behavior. The run covered 180 required tests and also reported six unrelated existing failures.

After the implementation, the following D35 tests passed in the final full run `run-d35-source-score-final-20260930`:

- `ice_spikes_wavefront_uses_source_first_advance_and_five_tick_rings`: unset-score command behavior and ages 1/5/6/10/11/25, ring counts, radii and staff yaw.
- `ice_spikes_first_ring_hits_on_source_tick_seven`: first-ring impact, elevated block plus passthrough snow surface anchor, and the exact feet-sphere edge helper.
- `ice_spikes_do_not_hit_nonplayers`: source `@a` player-only target scope.
- `ice_spikes_age_and_surface_anchors_survive_reload_without_replaying_impacts`: new entity instance loads age 32 and all 150 surface values; no completed inner-ring impact replays, age 45 remains alive for the final spike and age 46 cleans up.

Full-suite command:

```text
gradlew.bat runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d35-source-score-final-20260930"
```

Result: **180/182 required GameTests passed**. Both failures were the existing unrelated Flame Geysers tests `flame_geysers_caps_at_two_players_without_cross_spreading_node_groups` and `flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`. The D35 tests all passed. Log: `nyctocosm-native/run-d35-source-score-final-20260930/logs/latest.log`.

`gradlew.bat build --offline --console=plain`: **BUILD SUCCESSFUL**. `tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`: **errors=[]**, 3,024 entries, 55,719,531 bytes, SHA-256 `48aef11ef9b55bbecb2b1209c158d78c76b03168098d7a30ea6c92bde7bb509b`. The package check found no `NativeGameTests` or `ClientSmoke` entries. The previous alpha.12 JAR was copied to the run directory before rebuilding; its SHA-256 is `2cf4574920fe35ab870da8403cd65758a7d2acee796995b2a31ca68c91f7a92c`.

## Source digests

| Source file | SHA-256 |
|---|---|
| `sorcerer/functions/attacks/icespikes/start.mcfunction` | `25c25a599323015e6806a63f9d629ef5b3941d2fa402f50082b2f9fa7721e2b6` |
| `sorcerer/functions/attacks/icespikes/subtick.mcfunction` | `39dfeb13291b29776f8f9e3555a9923be5a89992fbd4dade948ec25bcb6617fd` |
| `sorcerer/functions/attacks/icespikes/wave.mcfunction` | `be31caabcf3f41debfc227271ae29b108473012f000f867f274b14f69b7f80ad` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/48.mcfunction` | `1b05cf2948f89b9cca2a09999c975a36885a49e739c0fef08189ba86507a537c` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/49.mcfunction` | `b668c72cdda12ede26e6e07d9b830f83a70c8071cc6b6d67b9ffc9da70ae9328` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/50.mcfunction` | `79e48d5378792d7c0f5afce4746653e19d3a419358a9d37df588d9db2b863b14` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/51.mcfunction` | `7e9c4239de09392cb199f98d873933de545889cbf709b9731c7368c73552cde6` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/52.mcfunction` | `12afaa5ce962c6039dc17d1a5eeed1b8b6b31a1e94633cec1e40f49b51bba6f9` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/59.mcfunction` | `d8d9ff8d63ffcb8bb1710604cfa51ab226f1d8602456fbfed29e37632dfe813c` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/60.mcfunction` | `af8ebd0590ae1e201995743eeb2256b1cb702df88bd307bb833a7e8a5e178232` |
| `sorcerer/functions/attacks/icespikes/__generated__/block/61.mcfunction` | `7892be29b0d41823c248364c5d00695ca334e318de04a2150db05442eaeca461` |
| `sorcerer/functions/attacks/icespikes/hit.mcfunction` | `1189d503229a0e764c985acd61286da94b9a7d1725131c673488e74a28e0e496` |
| `animated_java/.../emit/tree/leaf_40_effects_0.mcfunction` | `4334b5b1f0e7dd29da19d33c2c0772cdf3edbdf7aa6e4d54252b32b35f4d74e5` |
| `nyctocosm/tags/blocks/passthrough.json` | `4946344d080010ce306e85dab7535ed310b66d0e4c544a2b452801bfd6d46806` |

Native source digests: `IceSpikeWave.java` `9548f29162939e6dd817c222d7f6f7b0e9e1e87beba066a4ba49da8a8fbeb116`; `NativeBoss.java` `3f73c0b8c32b8436202785a6bbc50d6ee0ab5bd8e3769decc62b4729fe350b7a`; `NativeGameTests.java` `b79b221273e7f085c48c041e777b080b61a2004cee5e7f7601f69be7af8b402a`; `client/ClientEvents.java` `77431069c64fd8813035baaa03612befc0a1ddbffad8fe62c541b1daa057ecb0`.

## Remaining checks and boundaries

- D35 remains **in progress** until a real client confirms mid-wave tracking / late join rendering and a real chunk unload/reload or dedicated-server restart confirms persistent surface anchors in gameplay.
- No source datapack, real save, adjacent Forge MDK, installed mods, or JAR installation was modified.
- This workspace copy has no `.git` metadata, so Git status and `git diff --check` are unavailable. A separate whitespace scan is recorded in the parity TODO.
