# D09 Flame Geysers 验证记录

日期：2026-09-30。源为 `_nyctocosm_source`，实现目标为 `nyctocosm-native` alpha.12。

## 源行为与已实现切片

`phase2.attack.flame_geysers` 的动画 frame 0 播放 `charge`；frame 40 调用 activate。activate 在 300 格内随机选择最多两名非 spectator/debug 玩家。每个选中玩家的位置执行 generated block/405：生成 14 个 marker，执行 `spreadplayers ~ ~ 5 25 false`，再播放该目标位置的 `start`（MASTER、音量 50、300 格受众）。这里的 5 是节点间最低距离，25 是离玩家中心的最远散布半径；节点不是固定半径圆环。原包第二个目标的命令会再次用全局 geyser selector 重散布已有 marker，原生实现将节点保留在各自玩家 group 下，避免组间串场。

节点 timer 由 `20 + rng % 130` 初始化为 20–149。每 tick 先减计时；剩余计时 20 时在 marker 位置播放 `erupt`（MASTER、音量 1.5、300 格受众）。剩余 40 tick 内每 tick 生成半径 5 的 36 点红尘警示环，计时归零的喷发 tick 不再多发最后一环。spray 沿垂直 y=0..49 的 50 个采样点查找 5 格内合格玩家，每个 geyser 节点经一次 Dissonance 40 基础伤害；普通 Forge 伤害事件仍生效。源还生成 flame/lava/爆炸粒子和 `effects:shockwave/inculcate` emitter。

新增 `EmissaryFlameGeyser` 持久服务端实体，并接入注册和空 renderer。第 40 帧按组创建最多 28 个节点；每组独立记录 owner/group/target UUID 与随机 timer。采用均匀圆盘点位、WORLD_SURFACE 高度和最小 5 格水平节点间距，候选搜索上限为每组 14×500 次，不强制加载区块。加载/可用地表不足时可少于 14 个节点，这是有界寻点的限制。

动画 frame 0 播 `charge`，frame 40 启动双目标节点。原生右手 locator 导出从 9 个扩到 10 个；D09 在源 frame 10、20、25、30、33、36、38 从 `righthand` 播 flame（5）、flash（1）、lava（10）粒子，NativeBoss 现按对应动画帧和世界手部位置发送。locator bundle 包含 44 个动画、2601 帧、2640 个源动画文件，`sourceSha256=9856F853246135E80DF0C635F93A9A1B5C1D131EA11855ED652A4E4305C2AF82`。

测试初期出现的 0 节点结果是 GameTest 的实体查询框只覆盖模板地下高度，而 `WORLD_SURFACE` 在 y=63；节点生成在真实表面后落在查询框之外。现已按维度 build height 扫描测试实体，并在孤立夹具准备地表。测试还曾错误地把 spreadPlayers 的 5 当作目标中心最小半径；按原命令校正为节点间距 5、中心最大半径 25 后通过。

## 尚未完成

- 源 `effects:shockwave/inculcate` 的 emitter 和三组环形粒子尚未接入；当前喷发 plume 是受控粒子聚合近似。
- 源 `shoot` 中对空气执行的 `fill fire` 未实现。C03 世界放火范围选择仍等待用户确认，因此本阶段不写世界火块，差异项继续保持实施中。
- 40 基础伤害已通过 `Combat.hit`，但 Emissary 专用动态伤害属性/D22 非默认属性加值还未接入。
- 客户端粒子/声音视距、真实多人和战斗场景未实测。

## 自动验证

3 项 `flame_geysers` GameTest 覆盖：frame39/40 事件、1/2 目标各 14 个节点、group 独立散布、5–25 格径向范围及 5 格最小水平间距、世界表面定位、随机 20–149 timer 与 NBT 恢复、倒计时 40 警示起点、timer 归零不重复警示、50 格纵向样点命中，以及待用户决策时没有世界火块写入。右手 cue 七帧均取到有限的源动画 locator 并经过 NativeBoss 事件分支。

最终隔离全套命令：

```powershell
.\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d09-geysers-final10-20260930"
```

结果：**161/161 required GameTests 通过，BUILD SUCCESSFUL**。日志：`run-d09-geysers-final10-20260930/logs/latest.log`。此前数轮完整回归中两项既有 Serpent 远距 beam 测试曾间歇失败；本次最终完整回归两项均通过。

alpha.12 JAR audit：**errors=0**；3020 项、55,694,818 bytes，SHA-256 `798949bc18520d237d2524f89259b2f0aee6f9dc49ffb9f098b7013a4521a2e8`。locator bundle 10 个 locator；无 `NativeGameTests`/`ClientSmoke` 类或 runtime mcfunction。审计快照：[native-audit-after-d09-prefire.json](native-audit-after-d09-prefire.json)；前态：[native-audit-before-d09.json](native-audit-before-d09.json)。

关键源函数 SHA-256：

| 源文件 | SHA-256 |
|---|---|
| `flame_geysers/activate.mcfunction` | `052D708DB9CFFC2BB2E4FF7F91739173E47F5F847D7EE026B280AD4643AA7DF7` |
| `flame_geysers/__generated__/block/405.mcfunction` | `20EA2D53AC190776BDC50D1BA91F9F5735365F94888134497F0E5D6C703E0C38` |
| `flame_geysers/summon.mcfunction` | `55B9D87A5FC1BC48C94773CFB3B9FAA531BF48FD4300DC88465ED594CE8BAF7D` |
| `flame_geysers/subtick.mcfunction` | `4DBFA6CAA830E93D0D7DC1FF95BDB63F2323DF29F37FCA493C2F39EA3166CEB4` |
| `flame_geysers/shoot.mcfunction` | `329EE957C2B15E95B3644A548A51BE658486D24446D104EB15FF806D4C5E2414` |
| `flame_geysers/impact.mcfunction` | `D24959831FE22C71A8F446670D3689695E6412E413FA53D96680928A4DF963A0` |
| `flame_geysers/__generated__/block/404.mcfunction` | `D2C9833BBC6D08D240E4B5AA73D0C1AC2B14D2344C49E55D7E380243F12D083C` |
| `phase2.attack.flame_geysers/tick_animation.mcfunction` | `BF65688E2B0B0ACCACADD5F0C2044D52B356C7708ED3BD8177D24E4A3C26CB53` |

未改 `_nyctocosm_source` 或真实存档；alpha.12 版本不变，未交付/安装 JAR。下一优先项 D10 Apocalypse Rain。

## alpha.13 复查补正（2026-10-01）

两项数量/布场测试的反复失败来自区块实体可见性和相邻夹具串场，已改为独立场地、显式加载条件、owner查询和成功/失败清理。两个不同种子的完整219/219回归均通过；详见 [项目复查](PROJECT_RECHECK_ALPHA13.md)。

本次同时更正旧规格：Minecraft `SpreadPlayersCommand.Position.randomize` 分别在X/Z的最小/最大值间采样；`spreadplayers 5 25` 的25是方形区域半宽，不是圆形最大半径。当前 `spreadOnSurface` 仍是半径25圆盘，分布/坐标量化未对齐；本轮保留原生布场断言，数量/时序通过不能证明源散布等价。此代码缺口与世界火方块C03决定及shockwave视觉一起保留，D09仍实施中。
