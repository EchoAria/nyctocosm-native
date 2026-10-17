# D51：Expulsion 爆炸目标脚部距离边界

日期：2026-10-01

状态：自动验证通过·待实机。D51 的 3D 脚部距离、源目标类型与延迟连锁已完成 GameTest；实现保留原伤害所有者并继续走 Forge 伤害管线。多人客户端与第三方伤害拦截仍待验。

## 原包调用链

- `artifacts:expulsion/explosion` 对爆炸实体位置执行 `@e[type=#nyctocosm:livingthings,distance=..6,tag=!damagetemp]`，再运行生成块32。Minecraft `distance` 是以实体位置（脚部）到爆炸原点的三维距离，不是到包围盒最近点的距离。
- `explosion` 中先给爆炸来源添加 `damagetemp`，所以来源不会被其自身爆炸再次选中。生成块32以 Dissonance 能力计算伤害，再进入生成块33的普通伤害处理。另一路 `subtick → block28` 递增爆炸标记计时器，到15 tick 后执行爆炸；连锁击杀创建各自的后续标记。
- 源 `livingthings` 类型范围是原生资源 `#nyctocosm:mobs` 加所有玩家。D45 已导入这组实体类型标签，D51 复用同一标签。

## 实现

- `Combat.livingFeetSphere` 以半径 AABB 加 0.0001 作候选粗筛，再按 `LivingEntity.position()` 到源原点的三维距离平方精确筛选；边界含 radius² 与极小浮点容差。仅处理 `Combat.LIVING_THINGS` 标签实体并跳过 `damagetemp` 标记目标。
- `ExpulsionBurst.explode` 从 `Combat.area` 切换到专用脚部球查询：原点、半径6、7 Dissonance、所有者、粒子声音及零推力均不变；通用 `Combat.area` 保持现状，避免改变其他 AABB 命中技能。
- ExpulsionBurst 保存结构、15 tick 延迟以及逐次链式生成逻辑没有改动。原生仍通过 Forge `Combat.hit` 处理正常伤害拦截与伤害源。

## 验证

- 新增两项 `expulsion_feet_sphere` GameTest。修复前均红：水平玩家脚部 6.01 格虽身体包围盒进入半径 AABB 仍被误伤；巨大 Giant 的身体进入球体但脚部在爆炸下方6.01格仍被误伤。
- 修复后两项全绿：脚部5.99格和上方5.99格目标可受击；水平6.01格玩家与下方6.01格 Giant 不受击。第二项还断言 Giant 包围盒本身确实进入半径6范围。
- `expulsion_chain_waits_fifteen_ticks_and_survives_reload` 及通用 `area_damage_hits_large_body_at_feet` 回归通过；D45 两个 `semblance_swing_samples` 在同一轮也全绿。
- 最终 `run-d51-feet-verified-20261001` 完成 **201 项中195项通过**。未通过的6项为 `serpent_beam_selects_late_target_after_reload`、两项 `flame_geysers_*`、`emissary_wing_parts_take_ranged_spell_damage`、`serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`、`celestial_smite_player_contact_stops_early_and_open_ray_checks_at_most_150_cells`；均未调用 D51 路径。日志：[GameTest latest.log](../../run-d51-feet-verified-20261001/logs/latest.log)。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 审计 errors=0、3,029 JAR entries、0 runtime mcfunction；无 `NativeGameTests`/`ClientSmoke` 类。快照：[before](../native-audit-before-d51-20261001.json)、[after](../native-audit-after-d51-20261001.json)。

## 摘要与待验

| 文件 | SHA-256 |
|---|---|
| 原包 `expulsion/explosion.mcfunction` | `A73DAEC38CEA3F7C3BE555AA554D2D8DECAB19131DD29E58BBBDEBE94DA699C7` |
| 原包 `expulsion/subtick.mcfunction` | `04DA40ABA6BD09834480565247DC9415A43518E60C960639DC7A7ACB45B4A7CC` |
| 原包生成块28 | `93087261ACCDE5D0984C859E668C2B9911C6A442969E468D86CFFBB41E97614E` |
| 原包生成块29 | `57E2D88A8C2FCEFFEA6E84D667BB05783B3DBAC54B77392A3A271687C1DFA5DB` |
| 原包生成块32 | `4684676F31427B96882EAC29CE0DD737DD9121A3A1A8E3DD56C525346A625B47` |
| 原包生成块33 | `146D1E960C11E9B8637376F445FA6161AAF0C60CF82CE74E6A88152BB5A1B65B` |
| `Combat.java` | `BFD94C7ECC2A98C1D83AAF17D2B0520BA88D280404384B54A320C3E9FC4AED1D` |
| `ExpulsionBurst.java` | `904633702BEDE369079872B78FBDF82A3362B38C0566CBE6CFB2C555BB08DE9A` |
| `WeaponItem.java` | `D5FCF768F7091C154FD3981C4312EAC7FB4601BED5FF70A87D051E191FA3B3B9` |
| `NativeGameTests.java` | `202AB10E85EFB99BEA4285B67DFDC0C7A5743A77A4BA78045F0B97A56BA68E25` |
| alpha.12 JAR | `CFE7C04CEA23A5281E7002B2F4FAFC9A50337C0C18643535B64171100C8255AB` |

构建前 alpha.12（`BD6271B24D07EB2975225D556DAA8ED1395A22A32E4557277CC08BD57BA1A35B`）已备份到 `run-d51-feet-verified-20261001/pre-build-nyctocosm-native-0.3.0-native-alpha.12.jar`。原生多人客户端、不同实体标签配置与第三方伤害取消仍待实机；未修改源包/真实存档/邻接 MDK/安装目录，不升 alpha 版本、无提交或分发。
