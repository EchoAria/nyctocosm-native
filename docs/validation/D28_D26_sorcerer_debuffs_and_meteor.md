# D28 / D26 Sorcerer debuffs and Void Meteor

Date: 2026-09-30  
Scope: native implementation slices only; D28 remains open pending the Forge-cancellation choice.

## D28 — negative effects

Source `hit.mcfunction` commands apply slowness to the actual hit target after the damage function call. The source durations/amplifiers are 200 ticks / amplifier 1 for Air Dash, Dash Strike, Frozen Blades, Void Wave, Shard Hail, Singularity Charge, Ice Spikes, and Javelin Swing; Seeking Singularities uses 100 ticks / amplifier 1. Void Wave also gives darkness for 80 ticks / amplifier 1; Void Meteor gives darkness for 100 ticks / amplifier 1. Shard Hail has a separate ground-node rule: players within 1.4 blocks of each live node receive 200 ticks / amplifier 1 slowness.

Removed the native attack-start status effects from `beginAttack`, Hail setup, Frozen Blades launch, and Void Meteor tear creation. Starting an attack no longer debuffs a locked player who avoids every hit. A GameTest covers all ten affected attack openers.

The hit profiles and Hail ground-node effect are not implemented yet. The source executes its effect command after attempting damage, even if a Forge damage event would be cancelled in the native game. The implementation guide leaves that cancellation policy for the user; no policy has been selected or assumed here. Therefore D28 remains **in progress**, and the current native implementation does not yet reproduce hit debuffs.

## D26 — Void Meteor

Source path: `voidmeteor/subtick.mcfunction` → blocks 109/112–118. The tear activation resets its timer; blocks 112–118 emit one meteor at scores 20, 40, 60, 80, 100, and 120. Both block109 and block112 increment that timer in each activated world tick, so those score points occur every 10 world ticks (through world tick 60), not every 20 world ticks. Each meteor starts at the tear, gets independent yaw and pitch offsets in `[-40, 39]`, then follows the nearest non-spectator player within 50 blocks. Its projectile advances in three 0.5-block substeps per tick. A non-`passthrough` block or the 25-tick expiry invokes the impact function. Impact affects players within 5 blocks and calls the Void damage function; its darkness effect is recorded under D28.

Native changes:

- Tear launch and activation are separate events. The timer starts at activation; it emits one projectile every 10 world ticks, for six total shots through tick 60, continuing after the attack animation ends.
- The dedicated `SpellBolt` meteor stores its movement direction, owner, effect kind, and remaining life. Each server tick it updates its aim toward the nearest eligible player within 50 blocks and advances three half-block substeps. It passes through the source block tag `nyctocosm:passthrough`; another block triggers a five-block player-only impact. A clear flight ends and impacts after 25 ticks.
- All impact damage uses `Combat.hit`, preserving Forge events and ordinary damage handling. Attack-owned projectiles remain covered by existing departure cleanup.

GameTests verify no pre-activation shots, no shot at active world tick 9, one shot at 10, two at 20, five at 59, six at 60, retargeting after the player moves, passage through water tagged as `passthrough`, stopping at a solid block, five-block player damage, the 25-tick expiry, and NBT round-trip of owner/type/lifetime. The old assertion for six waves of 20 was replaced with assertions over actual emitted projectiles and timing.

D26 remains **in progress** because its darkness hit effect is part of the unresolved D28 policy and Reflectance/Forbiddance interaction has not received its own encounter test. No client visual or live multiplayer test was run.

## Verification

Command:

```text
gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d28-d26-final-20260930"
```

Result: **139/139 required GameTests passed; BUILD SUCCESSFUL**. Full log: `run-d28-d26-final-20260930/logs/latest.log`. It includes the D01, D02, D04, D27, and D38 regression cases.

`python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`: **errors=0**, 3,012 JAR entries, 55,379,331 bytes, SHA-256 `4628c1556ee4a4ff9b2503e9af8032ab36b58d5c8da70afbf5a09ea5a36cc7a4`. The pre-audit report is `docs/validation/native-audit-before-d28-d26-final.json`; the refreshed report is `docs/native-audit.json`.
