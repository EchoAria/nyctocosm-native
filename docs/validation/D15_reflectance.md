# D15 Reflectance Direction Follow-up

Date: 2026-09-30
Scope: correct SpellBolt's oblique reflection direction to match the source reflector-facing rule.

## Source comparison

The source Emissary fireball slowcast rotates the projectile to the reflector's rotation and advances it in that direction. The native `SpellBolt.reflectFrom` instead used the geometric mirror formula `v - 2(v·normal)normal`; an oblique incoming projectile therefore left on a different path. The native reflector already supplied its look vector to the bolt and already transferred ownership, removed homing, and spent capacity based on spell damage.

## Change

`SpellBolt.reflectFrom` now treats the supplied vector as the reflected direction, preserves the incoming speed magnitude, updates yaw and pitch, transfers ownership, and clears homing state (HOMING becomes DAMAGE). The existing `reflectance_consumes_source_twenty_damage_capacity` GameTest now sends an oblique projectile toward a yaw-zero reflector and asserts that its X component becomes zero while it continues along positive Z. This differentiates reflector-facing behavior from the prior specular formula. The same test still checks the incoming-damage capacity accounting and removal at exhaustion.

The change only closes the SpellBolt direction mismatch. `SpellArea.tickReflectance` still uses an expanded AABB and a dot-product incoming-direction filter, so distance-versus-box boundary behavior remains open. Beam and Emissary hazard paths keep their branch-specific direction logic pending the broader D15 comparison; this report does not claim a unified protocol.

## Verification

- `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d15-bolt-reflect-20260930"`: **172/174 required tests passed**. Failures: `echo_shard_cast_through_open_frame_activates_portal` and `serpent_departure_cleanup_uses_source_500_block_radius`.
- Repeated with `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d15-bolt-reflect-final2-20260930"`: **171/174 required tests passed**. Failures: `wing_blast_player_contact_ends_ray_and_damages_on_cooldown`, `serpent_beam_selects_late_target_after_reload`, and `serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`.
- The Reflectance GameTest was not among the reported failures in either run. These are not full-suite passes; the two complete runs reported different unrelated failures.
- A separate `gradlew.bat build --offline --console=plain` completed successfully.
- The alpha.12 JAR audit reported `errors=[]`, 3,024 entries, 55,716,141 bytes, SHA-256 `17d46fa68b401b10939ee502c48811c3ad945abd4acd661f3ded6d710feeb241`. The pre-change audit snapshot is `native-audit-before-d15-reflect-20260930.json`; current audit is `docs/native-audit.json`.

Status remains **implementation in progress**. Distance boundary, AABB corners, exact capacity edge, repeat-reflection behavior, cross-hazard rules, and client/multiplayer gameplay remain to be verified. See [D15 source and solution notes](../../../GAMEPLAY_REFACTOR_PLAN.md#d15--reflectance-统一规则-s2).

## Follow-up: shared selector and ownership protocol (2026-10-01)

### Source behavior

- `spells/functions/load.mcfunction` initializes Reflectance damage/capacity to 20. `reflectance/reflect.mcfunction` subtracts the attack's `TEMP s.damage`; generated projectile and beam branches use `distance=..3`, copy the nearest eligible reflector's owner UUID, and set the attack rotation to that reflector's rotation. The selector is a closed spherical distance check on entity/sample origins, not an expanded box or an approach-angle test.
- A Reflectance owned by the attack's current owner is not selected to redirect it. If a different owner is selected, the source applies the capacity cost to all Reflectance nodes inside that sample's 3-block selector. Each source slowcast then advances using the newly copied owner/facing, so the same reflector cannot charge a returning/repeated sample after ownership changes.
- Existing source families represented in this native branch include SpellBolt, Emissary beam samples, Wingbeat, Wing Blast rays, Rain shards, Pillars, and Flame Slash. Their sample timings and attack costs remain caller-defined.

### Implementation

- Added `ReflectableAttack` and `Reflectance.tryReflect` as the shared selector/capacity/owner protocol. A bounded AABB only collects candidates; the final test uses squared center distance `<= 9`. The nearest different-owner reflector supplies the facing and redirected player owner. When a source sample is accepted, all reflectors inside the exact selector spend that attack's cost, matching the generated command branches.
- `SpellArea` now reflects SpellBolts from any side, including a projectile moving away from the facing normal, while excluding same-owner attacks. It no longer uses an AABB corner as a hit or the incoming-velocity dot product as a gate. `SpellBolt` preserves speed, transfers its owner, clears homing, and records the reflection game tick so two reflector entities cannot process the same bolt twice in one tick.
- Emissary beam/Wingbeat, Wing Blast, Rain, Pillar, and Flame Slash call the same protocol. Their encounter parent UUID remains available for lifecycle cleanup while a separate persisted `DamageOwner` follows the reflector player for subsequent hits. Beam/ray sample order and each attack's cost/direction callback stay local to that attack.
- Updated Apocalypse Rain's previous assertion: its first 12-point reflection changes owner, so the second same-tick substep passes the same shield and leaves 8 of its default 20 capacity; it must not charge the same reflector twice.

### Verification

- Added `reflectance_uses_closed_three_block_sphere_without_direction_gate` for a side hit at 2.9, exact boundary at 3.0, back-side entry, outside at 3.1, and an AABB corner outside the sphere. Added `emissary_wingbeat_redirects_once_per_reflector_owner` for facing, owner transfer, and exactly one 5-point capacity spend across three same-tick steps. Extended the 11+9 SpellBolt capacity test to assert exact exhaustion.
- Existing `emissary_beam_reflects_and_consumes_reflectance`, `wing_blast_reflectance_redirects_the_ray_and_marker_expires_at_source_tick_43`, `emissary_pillar_redirects_from_a_reflectance_node`, and the corrected Apocalypse Rain reflection test all passed in the final run.
- Final `gradlew.bat runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d15-reflectance-verified3-20261001"`: **203 tests; 201 passed**. The only failures were the unrelated `flame_geysers_caps_at_two_players_without_cross_spreading_node_groups` and `flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`. Log: `run-d15-reflectance-verified3-20261001/logs/latest.log`.
- `gradlew.bat build --offline --console=plain`: **BUILD SUCCESSFUL**. Alpha.12 audit: `errors=[]`, 3,032 entries, 55,736,204 bytes, SHA-256 `580a30ba94c2947e75a414d7c5baaf19cce134d9cabfbb4660141b015be35118`. Before/after snapshots: `native-audit-before-d15-unified-20261001.json` and `native-audit-after-d15-unified-20261001.json`. The pre-build JAR (SHA-256 `cfe7c04cea23a5281e7002b2f4fafc9a50337c0c18643535b64171100c8255ab`) is preserved at `run-d15-reflectance-verified3-20261001/pre-build-nyctocosm-native-0.3.0-native-alpha.12.jar`.
- Changed native source SHA-256: `ReflectableAttack.java` `BADB92D07555E5A35D920582B7D588C19EDA06F1666487A08F99B4C338328671`; `Reflectance.java` `2AAC2DC6545585A5C95B9842AB14D0BCA589D5090C19C1D3543C4F9598032D3D`; `SpellArea.java` `F2F485084DE347CA2501667C99A24C4D214FB71FCB88457F1433EC6C0C2E1617`; `SpellBolt.java` `3399B152F4D17E60F889897EBA72B751619FC62D6E4FC89C916FB4DFAD8F1A97`; `EmissaryHazard.java` `2A15DFC4B2183A75C5CC8C137C6D46B35840F4AA8377E60DC3AE1F23AF8A0F64`; `EmissaryWingBlastRay.java` `53BC235AFA381D174888FE39E225AFA77500C0EAEACCCBD5819A8259DD2358B0`; `EmissaryFlameSlashWave.java` `6F25B883E33196FA22699F1A2AC62D26D6C3F7E9B997EB2F2A711D9435A46781`; `EmissaryRainShard.java` `A335DA1B663F6CFB709E3150EC4B1982302C320D0DC71557771347BD752E38EB`; `EmissaryPillar.java` `AC2664C59785567A588177E7FF5E87295063F1D8A8E2C25F4A3A0BA7EAFDEDDF`; `NativeGameTests.java` `F17CE3004D6C75F640A975D6FFAB03A864D48868D1AB140BCB4EE7BFF11D4B24`.

D15 remains **implementation in progress**: native Halo/Void beam implementations are tracked under D32; non-default power-dependent Reflectance capacity belongs to the unresolved D22 stat path; client/multiplayer behavior and overlapping-reflector tie cases remain for later verification. No new gameplay choice was needed for the source-defined default behavior. Alpha.12 remains unchanged; no source datapack, saves, adjacent MDK, installed Mods folder, commit, or distribution was touched.
