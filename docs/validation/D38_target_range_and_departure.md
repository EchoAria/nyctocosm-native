# D38 Serpent / Sorcerer target range and departure

## Source behavior

- `serpent/functions/__generated__/block/193.mcfunction` advances the attack timer only while a player is within 130 blocks. The attack start functions choose from all players within 200 blocks. `serpent/functions/__generated__/block/7.mcfunction` calls removal when no player is within 500 blocks.
- `sorcerer/functions/ai/entitytick.mcfunction` removes the encounter when no player is within 100 blocks. Attack selectors use a maximum 50-block range and exclude spectators. `sorcerer/functions/remove.mcfunction` removes the encounter's rigs, Nullpoint, plane, hitboxes, attacks, bossbar and music state.
- Source target selectors do not require line of sight. The Serpent's registered 200-block follow range and 130-block attack gate were already present in the native code; the target conditions' default sight test and Sorcerer's 80-block follow range were the actual gaps.

## Changes

- Added a player target goal that disables line-of-sight checks during target selection for Serpent and Sorcerer while keeping other boss target rules unchanged.
- Set Sorcerer's follow range to 50. Kept Serpent at 200 and its 130-block attack timer gate.
- Before normal server AI, remove Serpent or Sorcerer only when no current-dimension player remains inside its source radius. Clear its bossbar and owned state. Remove Serpent beam hazards by owner UUID; remove Sorcerer bolts, areas, ice waves and Nullpoint by owner UUID, and clear its plane/staff references. Client boss music observes the removed Boss and stops through the existing music controller.
- GameTest fixtures use separately ordered batches and tagged mock players so a second fixture does not become the first fixture's nearby player.

## Verification

The full command `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d38-verified7-20260930"` completed successfully: **137/137 required GameTests passed**.

Coverage includes the exact 130-block Serpent attack gate and 130.01-block hold, Serpent and Sorcerer target acquisition through solid walls, a Sorcerer target outside the 50-block source selector range, inclusive 500/100-block departure boundaries, owned artifact cleanup, and retention of a second encounter's Boss and owned entity.

`tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar` reported **0 errors**. The JAR has 3,012 entries, 55,377,660 bytes, SHA-256 `dfa69a172ab2d67f8d3181b629844281c3cf3ac114a5b45ea13ece226f258650`; it contains no runtime mcfunctions or GameTest classes.

The GameTests use a dedicated run directory and do not touch real worlds. Client presentation, real survival play, low-simulation-distance chunk loading and multiplayer music/range behavior remain to be checked in game.
