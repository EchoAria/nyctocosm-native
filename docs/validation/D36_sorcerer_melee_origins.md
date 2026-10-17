# D36：Sorcerer Void Wave / Javelin Swing 命中原点

日期：2026-10-01

状态：自动验证通过·待实机。D36 的第 38/35 帧、范围与命中原点已完成 GameTest；D28 的命中附加状态及完整客户端表现仍独立待验。

## 原包调用链

- Void Wave：Animated Java `leaf_38` 在实体根部上移世界 Y 2 格后调用 `voidwave/discharge`。Discharge 启动 transmittance shockwave，在该位置发 explosion/flash，并执行 `@a[distance=..6]`；hit 函数处理被选玩家。
- Javelin Swing：`leaf_35` 在源实体局部坐标 `^ ^1 ^2` 运行 `javelinswing/swing`。Swing 对 `@a[distance=..3]` 调用 hit，并在相同局部原点发 sweep 粒子。
- 原生事件仍在 `SorcererTimeline` 的 tick 38/35 触发，没有移动关键帧或变更攻击伤害配置。

## 实现

- `Combat.playerFeetSphere` 以半径 AABB 做候选筛选，再按玩家脚部 `position()` 到源原点的三维距离平方执行闭边界过滤；只查询玩家、保留 `Combat.canHit` 与 `Combat.hit`/Forge 伤害流程，不追加固定 push。
- Void Wave 的攻击原点改为 `position() + (0, 2, 0)`、半径 6；爆发与 flash 的直接原点以及原生环形 cue 都放在该高度。
- Javelin Swing 使用实体 yaw/pitch 计算 Minecraft 局部 up 基向量 `(-sin(yaw)sin(pitch), cos(pitch), cos(yaw)sin(pitch))`，并加 `getLookAngle() * 2`，即源 `^ ^1 ^2`。公式已对照本机 Minecraft 1.20.1 缓存映射 `LocalCoordinates.getPosition` 字节码核验。伤害半径为 3；在同源局部原点补回 7 个 Sweep 粒子。
- 两招无源的 `1.3`/`1.0` 固定推力已移除；基础引擎伤害处理不变。未在 D36 里实现 D28 的 Slowness/Darkness 等命中状态。

## 验证

- 新增 2 项 `sorcerer_hit_origins` GameTest。Void Wave 检查世界 Y+2、脚部恰好半径 6 命中、脚部 6.1 格但身体进入粗筛 AABB 不命中，以及 Cow 不受玩家选择器攻击。Javelin 以 yaw 90°/pitch 30° 验证旋转局部原点，检查脚部内点、恰好半径 3、脚部 3.2 格但身体进入候选框、以及 Cow 免受攻击。
- 修复前两项测试均在旧 `Combat.area` 路径按预期失败；修复后的 `run-d36-source-origins-verified-20260930` 两项均通过。
- 同次全套 **197 项中 194 项通过**。仅三项失败：`flame_geysers_caps_at_two_players_without_cross_spreading_node_groups`、`flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`、`serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`。均未调用 D36 代码。日志：[GameTest latest.log](../../run-d36-source-origins-verified-20260930/logs/latest.log)。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 审计 errors=0、3,026 JAR entries、0 runtime mcfunction；无 `NativeGameTests`/`ClientSmoke` 类。审计前后快照：[before](../native-audit-before-d36-20261001.json)、[after](../native-audit-after-d36-20261001.json)。

## 摘要与待验

| 文件 | SHA-256 |
|---|---|
| 原包 `voidwave/tree/leaf_38.mcfunction` | `DC39ED225965D6A94225C342D0BCA90BCACCDC7314F4BA77C0B6450BE97954A9` |
| 原包 `voidwave/discharge.mcfunction` | `404A46F9790733E2D24AC19FC726DEDECAF69F9A0597FD739E72AB42317DC7FE` |
| 原包 `voidwave/hit.mcfunction` | `3E044DDB87682CBA3E2F9CB730ECEC710F0DE8C6F2263B5525478E8C4D441399` |
| 原包 `javelinswing/tree/leaf_35.mcfunction` | `8FADCC3DA5FA7C41150C7B66D9B7E2F623972490816514B413F23F47B32E47CC` |
| 原包 `javelinswing/swing.mcfunction` | `B4B77A0B3F769B82B67DA13F8861BFFEE974FBFAEDEABCB9C941AB0C9E24D75F` |
| 原包 `javelinswing/hit.mcfunction` | `BAB8099EDD4D4730F6ACF7262131443CFB7E024E3555CA3F4CECC189CA0FA939` |
| `Combat.java` | `07C2AB9537664C1A245A14DDD533D27733561B8F680203F890F7CE822AB01955` |
| `NativeBoss.java` | `11A1B9C4E953A32C0E2A8E2ACA31DE83D245D699B81E07C449548B6CFF397419` |
| `NativeGameTests.java` | `815D85E71E65375829586B0CD2698D045D82DD8B66499533BCDD98FBECA2A217` |
| alpha.12 JAR | `E099C311396850FF5B5F0964F00158B62EA886AB6F61C33D3A45D483BD22F298` |

构建前 alpha.12（`0918620d7f7bb60e10604fea3d2e0c26a5ae4a1ad50d6eb5dc75b301dafc0f87`）已备份至 `run-d36-source-origins-verified-20260930/pre-build-nyctocosm-native-0.3.0-native-alpha.12.jar`。真实玩家对战、多人边缘点、客户端粒子、完整 shockwave 轨迹和攻击中途区块卸载/重载仍待实机；D28 状态选择继续保持未决。
