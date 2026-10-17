# D02：Incurrence 法阵位置上下文

日期：2026-09-30。状态：**部分实现；模式子项已按用户选择实现，多人入口与失败补偿仍待决策。**

## 已实施

- 原始调用链在 `spells/functions/incurrence/cast.mcfunction` 以法术原点执行生成函数；生成的 spellcircle 位于该上下文。`spells/functions/incurrence/transmutate.mcfunction` 在当前位置匹配 `night_wastes`、`desert_of_rime`、`infernal_chasms` 三个 biome predicate，再调用对应 Boss 的 Incurrence。
- 当前原生流程此前使用完成施法玩家的位置查询 biome。现由 `SeelicField.finish` 将法阵位置传给 `NativeEvents.incur(player, position())`；biome 路由从传入位置查询，并只接受 `nyctocosm` 命名空间对应的三种 biome。使者领域的安全地表落点也以法阵位置作为 X/Z 请求点，再保留现有安全落点验证。
- 保留 `incur(Player)` 兼容入口，转调新方法并以玩家当前位置为默认上下文。
- 用户确认后，已把源中的生存→冒险规则接到使者成功入域路径。只在原模式为 Survival 时保存先前模式并切到 Adventure；离开使者领域或死亡重生时，仅当当前仍是此流程设置的 Adventure 才恢复，并清除流程记录。若玩家本来是 Creative/Spectator，或管理员/其他系统在流程中改过模式，不覆盖该模式。流程标记随玩家 PersistentData 复制/保存。

## 自动验证

新增 `incurrence_biome_routing_matches_source_predicates`，覆盖三种源路由和异命名空间同路径不得误触发。运行：

```powershell
.\gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d02-context-20260930"
```

结果：**122/122 required GameTest 通过，BUILD SUCCESSFUL**。随后对当前 alpha.12 JAR 运行 `tools/audit_native.py`，**errors=0**；D01 locator bundle 仍正确打包。JAR 3008 项，55,368,487 字节，SHA-256：`0657da272935d4bf7e4e53994bc860acbcadf32861a2457829eed19f8c35202f`。

## 已确认模式选择后的回归（2026-09-30）

- 全套 GameTest 的 `incurrence_mode` 批次运行 `emissary_entry_restores_only_survival_mode_changed_by_the_encounter` 并通过：覆盖 Survival→Adventure→Survival、流程中外部改为 Creative 时不覆盖、原本 Creative 不切换。
- 同次全套共 185 项，181 项通过；其余四项为两项 Flame Geysers、使者翼部 SpellBolt 命中及 Serpent Beam 高目标边界，未包含 D02 模式断言失败。该夹具直接验证模式状态方法，不等同于真实死亡/跨维度事件和多人入口实机验收。

## 待完成与待决策

- 法阵周围 2 格玩家集合、多人各自落点与返程记录仍未实现。
- 任一队员无安全落点时的全队取消/局部进入/补偿策略尚待用户选择；确认前不做半队传送实现。
- 需增加跨 biome 施法和多玩家真实入口测试；当前回归只验证路由映射、完整旧套件和构建，不宣称 D02 完成。

## 用户决策记录

- D02 模式：用户回复“按建议恢复仅由流程改动的模式”，确认问题包含流程结束、死亡或退出。据此只恢复本流程确实切换且仍保持流程所设 Adventure 的玩家；既有非生存模式和流程中的外部模式修改不被覆盖。多人传送失败的扣费/回滚/补偿没有在该回复中确认。

## alpha.13 结算路径补正（2026-10-01）

复查发现旧代码只覆盖离开和死亡重生，没有覆盖使者最终流程结算。现在第100死亡tick的最终结算调用同一模式恢复方法，幂等保护保留；新测试验证99时不提前恢复、100时恢复、外部Spectator/原Creative不覆盖及重复调用安全。入口拒绝尚未移除的使者，包括死亡动画期间，以防旧流程结算覆盖新流程标记。两个不同种子全套219/219通过，alpha.13构建审计通过；见 [本次项目复查](PROJECT_RECHECK_ALPHA13.md)。组队入口、补偿、完整encounter ID、离线关联和真实重启验收仍未闭合。
