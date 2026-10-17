# D16 — SpellBolt 方块碰撞语义

日期：2026-10-01  
状态：自动验证通过，待客户端/多人实机验收；本项只覆盖下列已审计原生调用点。

## 原包行为与映射

原包 `nyctocosm:passthrough` 标签不是“无物理碰撞方块”列表。标签中的水、火、雪、草等可穿越，而某些不在标签中的无碰撞植物仍会阻挡 slowcast。对照源 tag、`phase4/fireball/slowcast`、`phase1/lightning_javelin/slowcast`、`phase3/seeking_missiles/slowcast` 和法术移动函数后，原生只在对应调用点启用源碰撞策略：

- SeelicField 的 Abhorrence、Incandescence、Fulminance：按 passthrough tag 穿越。
- SeelicField 的 Gravitance：遇非 passthrough 体素时停在原位，但保留攻击 tick。
- Emissary lightning_javelin、seeking_missiles、fireball：按 passthrough tag 穿越。
- 其它未审计 SpellBolt 调用与 vanilla 箭：继续使用原生物理碰撞。
- seeking singularity 使用 ignore-blocks policy；其它自带碰撞逻辑的光束/载体未被统一替换。

`SpellCollisionPolicy` 通过 NBT 保存，旧 `IgnoreBlocks` boolean 仍读写兼容；客户端同步 held 状态，避免 Gravitance 在阻挡方块处发生姿态漂移。passthrough 扫描使用有界 DDA：逐体素检查 tag，同时跨越多个轴时检查侧向相交体素；实体单独做扫掠并与方块命中按距离比较。Forge projectile impact 事件仍执行，取消事件仍会阻止原生 impact 结果。越过维度 build height 视为阻挡；检测到未加载 chunk 或无效/超长扫掠时停止推进。

## 自动验证

以下四项 GameTest 在 `run-d16-collision-verified3-20261001` 运行中通过：

1. `source_passthrough_policy_uses_tagged_voxels_instead_of_physical_shapes`：源标签方块、未标记的无碰撞海草、完整方块。
2. `source_passthrough_sweep_catches_fast_wall_crossings_and_diagonal_corners`：高速穿墙、对角角点、build-height 与未加载 chunk 边界。
3. `source_passthrough_policy_keeps_forge_projectile_impact_cancellation`：Forge impact 监听器取消后不应用 projectile impact。
4. `spell_collision_policy_survives_projectile_save_and_load`：策略 NBT 往返。

最终完整 GameTest 套件：207 项完成，204 项通过，3 项失败：

- `flame_geysers_caps_at_two_players_without_cross_spreading_node_groups`
- `flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`
- `serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`

失败日志位于 `nyctocosm-native/run-d16-collision-verified3-20261001/logs/latest.log`。这些失败不经过 D16 policy 调用点；本轮四项 D16 GameTest 全部通过。

## 构建与包审计

- `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- 版本保持 `0.3.0-native-alpha.12`。审计 `errors=[]`，3,036 个 JAR entries，55,744,877 bytes，SHA-256 `c06535ea76f3c1027c3332b51bdcca8b207e03c810763b204fc39f20ff2c96ee`。
- 审计前后快照：`docs/native-audit-before-d16-collision-20261001.json`、`docs/native-audit-after-d16-collision-20261001.json`。重建前 JAR SHA-256 `580a30ba94c2947e75a414d7c5baaf19cce134d9cabfbb4660141b015be35118` 已备份到本次运行目录 `pre-build-backup/`。
- 无 GameTest/ClientSmoke/runtime mcfunction 被打入 JAR；未升版本、未改原包、真实存档、相邻 MDK 或安装 Mods，无提交/分发。

## 实现文件 SHA-256

- `SpellCollisionPolicy.java`: `018f79c466dec1ff46636ae9b2c1a06d1bc0d42b1a9f61d263ec57b2ec8429bb`
- `SpellBolt.java`: `40669c8b7f47abacd288d657ff1f8f0524f876533fd0bae4b5631f8624ec30f0`
- `SeelicField.java`: `9b86f5374d257bf0c71af94d1f996d70afc3cccb931160f77aad100e79d34de7`
- `NativeBoss.java`: `4cd8b1fe3353e9220bf7c498e5c75c2e6103c29a5c7523a5d8308c96e07be9d`
- `NativeGameTests.java`: `08afb0b185ef35dc63dd76cbe21190df48c160f2c741ab30abb7daf84d12a1d1`

客户端弹体终点与动画观感、多人同步以及真实战斗中的第三方事件交互仍待手动验收。此报告不表示所有 SpellBolt 调用点都已与原包逐招核完。
