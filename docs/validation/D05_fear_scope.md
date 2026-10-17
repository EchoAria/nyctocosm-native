# D05 Fear Processing Scope

Date: 2026-09-30  
Scope: align the native fear tick with the source player-tick dimension, game-mode, and Luminescence gates.

## Source behavior

`nyctocosm:playertick` invokes the fear path only for players matching `predicate=nyctocosm:in_nyctocosm` and `tag=!luminous`. Generated block2 narrows that to `gamemode=survival`. The dimension predicate contains `nyctocosm:nyctocosm`, `nyctocosm:dusk_plane`, and `nyctocosm:emissary_domain`. Within that scope, the fear function accumulates darkness ticks, starts fear at 200, and resets fear/Darkness when the player enters light.

## Native change

`NativeEvents.tick` now enters fear processing only for survival players in those three exact dimensions who do not have active Luminescence. Players outside that context keep their Darkness and stored fear state; another darkness source is no longer removed by the global player tick. Active Luminescence independently clears Darkness in any dimension, preserving its source spell behavior.

## Verification

Added `fear_processing_stays_in_source_realms_and_preserves_external_darkness`, which checks that a survival FakePlayer in the overworld retains both an active Darkness effect and fear state after a player END tick. It also checks that all three source dimensions enable fear for survival and exclude adventure mode.

Command:

```text
gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d05-verified2-20260930"
```

Result: **140/140 required GameTests passed; BUILD SUCCESSFUL**. Log: `run-d05-verified2-20260930/logs/latest.log`.

The alpha.12 JAR audit reported **errors=0**, 3,012 entries, 55,379,636 bytes, SHA-256 `252d5ad57b684f6d7b76ce8eeb15e3bd01475414e963a10e0e8e4f33f75884ce`. The pre-audit report is `docs/validation/native-audit-before-d05.json`; the refreshed audit is `docs/native-audit.json`. No client session or real multiplayer test was run.
