# D13 Fixed and Mobile Field Lifecycle

Date: 2026-09-30  
Scope: separate fixed field presence from mobile field idle lifetime and preserve mobile cast state.

## Source behavior

The fixed-field creation branch does not set a duration. The shared plane tick removes it when no player is within three blocks. The mobile creation branch sets `s.duration` to `500 + m.dream.power * 100`; its idle duration decrements only while `s.casting` is absent. The stat reset initializes Dream power base and modifier to zero, while the broader attribute pipeline is tracked separately as D22. Plane ticking and cast subticks are entity-driven, so their countdown does not require an owner player entity to be present.

## Native change

`SeelicField` now distinguishes fixed (`life=-1`), mobile (`life=500` default), and trace lifecycles. A fixed field persists while any player is within a spherical three-block radius and is removed after the area empties. A mobile field decrements its remaining lifetime only while idle. NBT preserves mobile type, cast progress, cooldown, and remaining lifetime; legacy mobile lifetimes are clamped to the current source base default. Existing creation-time mobile classification remains immutable.

The source Dream power bonus is not implemented because the D22 native stat pipeline remains open. If an owner disappears during a cast, the native plane continues its cast countdown; on completion it clears cast state and enters cooldown without executing owner-dependent spell effects. This avoids invoking a spell with a missing owner, but is not confirmed source parity and remains an explicit D13 gap. Holding-item transitions also need an in-game behavior check. Hypertrace movement and Transposition interactions are tracked under D14.

## Verification

Added GameTests for fixed fields surviving their original owner's removal while a second player remains nearby, removal after the last nearby player leaves, mobile default lifetime, lifetime pause while casting, and save/load of mobile/cast/lifetime state followed by cast countdown with the owner absent.

The first D13 run exposed a fixture issue: a non-registered mock player was not available through the server UUID lookup. The fixture now registers a `FakePlayerFactory` player in the level. An intermediate run stopped without a GameTest summary and cannot be counted; a fresh isolated run completed:

```text
gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d13-verified4-20260930"
```

Result: **142/142 required GameTests passed; BUILD SUCCESSFUL**. The run log is `run-d13-verified4-20260930/logs/latest.log`.

The alpha.12 JAR audit reported **errors=0**, 3,012 entries, 55,379,970 bytes, SHA-256 `736baecdc949336407efdedc799ce431d435aad99e345bc0507cc0b6904b067d`. The previous audit report is preserved as `docs/validation/native-audit-before-d13.json`; the refreshed report is `docs/native-audit.json`. No client session or real multiplayer test was run.

Status remains **implementation in progress** pending D22 Dream power support, owner-absent cast outcome evidence, item-transition verification, and in-game lifecycle checks.

## Follow-up: source anchor, pitch, and cancellation rules

Date: 2026-09-30

Rechecked the D13 lifecycle against the source after the later Flame Slash stage. In `block/4`, `execute at <matched player>` changes command position/rotation but leaves the plane marker as `@s`; `tp @s ~ ~ ~ ~ 0` therefore moves the mobile marker to the linked player's location, copies yaw, and sets the marker pitch to zero. Shared `block/2` removes either plane type when no player is within three blocks. Source `cancel_cast` clears casting state and sets the nearest mobile plane's duration to zero; a fixed plane is not expired by that path. The field type comes from its creation-time `mobile` tag, so later inventory changes do not retag it.

Updated `SeelicField` so only creation-time mobile fields follow their linked online owner and copy yaw with marker pitch zero; fixed fields remain at their position regardless of later Transposition changes. Both types now use the source three-block no-player cleanup. A canceled mobile field reaches zero duration and is removed by its normal lifecycle tick; a canceled fixed field only clears cast state. Added three GameTests covering the creation-time type/inventory boundary and transform, mobile expiry with no nearby player during a cast, and the distinct fixed/mobile cancellation outcomes. The existing offline-owner cast test now keeps another player nearby, matching the source condition that the plane disappears only when the area empties.

Verification: `gradlew.bat compileJava --offline --console=plain` succeeded. Two isolated full runs each executed **174 required GameTests**; all five `field_lifecycle` tests passed in both. `run-d13-followup2-20260930` had one unrelated failure, `serpent_beam_hits_high_targets_and_keeps_round_boundary`; `run-d13-followup3-20260930` had two unrelated Serpent retarget tests fail. Neither run is claimed as a full-suite pass. The exact logs are under each run's `logs/latest.log`.

Final code build `gradlew.bat build --offline --console=plain` succeeded. Alpha.12 JAR audit: **errors=[]**, 3,024 entries, 55,716,022 bytes, SHA-256 `69fa132508f65f6074326c66fa47fe1aa72573851b8e6dbd8b634581819c2fee`; it contains no runtime mcfunction, GameTest or ClientSmoke class. Before/after audit snapshots are `native-audit-before-d13-followup-20260930.json` and `native-audit-after-d13-followup-20260930.json`; current audit is `docs/native-audit.json`. `git diff --check` passed.

D13 remains **implementation in progress**: Dream power bonus needs the D22 stat pipeline; when the linked owner disconnects but another player remains nearby, source spell executor behavior still needs per-spell context review before resolving owner-dependent native effects. Item-transition and client/real-multiplayer behavior are not tested in-game. No C03 fire or D28/D42 status-on-cancel choice is involved in this change. No source pack or real save was modified, alpha.12 was not bumped, and the JAR was not installed or distributed.
