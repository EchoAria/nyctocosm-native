# D14 Hypertrace Triggers and Lifecycle

Date: 2026-09-30  
Scope: align source cast-start and Nightmare/Void hit creation paths, then restore trace position, movement, expiry, and owner interactions.

## Source behavior

All 17 source spell `cast.mcfunction` files calculate global `tracechance` and independently roll before the spell resolves. A successful roll summons the trace three blocks along the player's local look direction; the shared summon function offsets it one block up. The source compares a 0–99 integer roll with an inclusive score (`roll <= chance`); the default global score is 15.

Generated spell block/58 and block/81 perform separate trace rolls after Nightmare/Void damage application when the damage source is a player. They add the attacker's element-specific tracechance to global tracechance, summon as the attacker while keeping the victim's location context, and therefore produce an attacker-owned trace one block above the victim origin. Element-specific tracechance defaults to zero. The source trace manager rotates one degree each tick, moves 0.2 blocks away when another root is within one block, and expires at 400 ticks. Its tick is entity-driven and does not require the owner to stay online.

Accumulation counts only traces with the same owner identity inside a six-block spherical selector. At default power, each such trace adds 10 global chance points; the additional power multiplier remains part of D22.

## Native change

`HypertraceService` now handles the independent cast-start and player Nightmare/Void hit triggers. `SeelicField` invokes the cast trigger as soon as any spell starts and no longer rolls a generic trace chance when a spell finishes. `Combat.hit` invokes the N/V trace branch after the hurt call, matching the source function's ordering even if hurt returns false. The trigger preserves the player attacker as owner and the spell's victim context as the spawn location.

Hypertraces now keep a 400-tick lifetime, rotate one degree per tick, move away from the nearest trace within one block, ignore Transposition following, and remain alive after the owner leaves. `NativeEvents.traces` now applies the source's spherical six-block distance check. The existing pickup/destroy behavior still accepts the owner only and is now covered by a multiplayer GameTest.

Non-default global/element power-based tracechance modifiers remain open under D22. Other entity-specific and weapon-specific source trace emitters remain coupled to their separately prioritized attack fixes; this implementation covers all spell cast starts and the validated player-owned Nightmare/Void spell damage branches.

## Verification

Added six GameTests:

- Inclusive 0–99 boundary, with score 15 accepting exactly 16 results.
- All 17 spells generating at cast start before resolution, with cancel not retracting a created trace.
- Nightmare/Void hit ownership and victim position; unrelated Radiance does not trigger, while the source post-damage branch still rolls when hurt is rejected.
- Accumulation counts only the owner's traces inside the spherical six-block radius.
- Rotation, 0.2-block repulsion, 400-tick expiry, owner disconnect, and Transposition isolation.
- Owner-only pickup and owner-only destruction.

The first full runs caught two test/implementation issues: the hit test reused a target during invulnerability, and the spell-start origin received its one-block Y offset twice. Both were corrected. Several unrelated Serpent target/beam GameTests failed intermittently in intermediate complete runs; the final complete run passed every required test.

Final isolated command (ForgeGradle's network certificate preflight had begun failing before task execution; the quoted property skips that preflight and `--offline` prevents dependency downloads):

```text
gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d14-verified10-20260930"
```

Result: **148/148 required GameTests passed; BUILD SUCCESSFUL**. Log: `run-d14-verified10-20260930/logs/latest.log`.

The alpha.12 JAR audit reported **errors=0**, 3,013 entries, 55,383,320 bytes, SHA-256 `aad6fd2de045dcded5e15e9d2314c8b3a578c2289758137033df4d6d1fb4698d`. The previous audit is preserved as `docs/validation/native-audit-before-d14.json`; the refreshed audit is `docs/native-audit.json`. No GameTest classes, runtime mcfunctions, or client smoke classes entered the JAR. `git diff --check` found no whitespace errors; it printed existing LF/CRLF normalization notices. No client or real-multiplayer session was run.

Status remains **implementation in progress** pending the D22 non-default stat modifiers and the remaining source-specific trace emitters. See [D14 source and solution notes](../../../GAMEPLAY_REFACTOR_PLAN.md#d14--hypertrace-触发位置和移动-s3).

## Follow-up source audit (2026-09-30)

The D06–D12 source attacks also contain trace calls: Sword Plunge emits from its attack entity; Wing Blast, Pillars, Flame Geysers, Apocalypse Rain, and Celestial Smite emit from the Emissary model; Flame Slash emits from a temporary impact entity. These are distinct from the already implemented player cast-start and player-owned Nightmare/Void hit paths. The full source reset-stats function was not found on the runtime call path, while player tick initialization only establishes the player's default global chance. No reliable initialization was found for the Boss/temporary entities' element/global chance scores. Because the source uses an inclusive 0–99 comparison, assigning missing scores a default of zero would create a 1% chance; no such gameplay default was invented. These non-player emitters are held pending the user's choice.

The audit also rechecked the player hit context: the source copies the attacker's UUID from the damage marker, then executes the summon as that attacker while preserving the victim's position context. The native callback already follows that owner/location split, so no code change was needed for this follow-up.

Follow-up verification used `run-d14-source-audit-20260930`: **174/174 required GameTests passed; BUILD SUCCESSFUL**. The audit retained the existing D14 alpha.12 JAR digest, with 3,024 entries and SHA-256 `69fa132508f65f6074326c66fa47fe1aa72573851b8e6dbd8b634581819c2fee`. The non-player hooks were deliberately not added, so this run verifies the existing implementation while the gameplay choice remains open. See the appended Stage 53 in [the parity TODO](../../../MAINLINE_GAMEPLAY_PARITY_TODO.md).
