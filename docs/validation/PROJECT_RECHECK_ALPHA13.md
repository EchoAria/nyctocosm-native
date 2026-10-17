# alpha.13 项目复查与构建交付

日期：2026-10-01。平台：Minecraft 1.20.1 / Forge 47.4.13 / Microsoft Java 17.0.19。

本轮按用户“重新检查一遍项目，完成后编译出一版 mod”的要求，复查当前实现、排查既有回归失败并交付新的开发版。D01–D51 的整体还原任务仍按根目录差异清单继续；全套自动测试通过不代表全部玩法或客户端已经验收。

## 本轮生产代码修正

| 项目 | 修正 | 状态 |
|---|---|---|
| D02 模式恢复 | 使者最终死亡动画第100 tick结算时，恢复流程实际改动且仍由流程管理的模式。外部改成 Creative/Spectator 的模式不被覆盖。结算幂等保护保留。 | 模式切片自动通过；多人入口/补偿仍实施中 |
| D02 流程重叠 | 管理入口和召唤入口检查尚未移除的使者，包括死亡动画期间的实体，避免旧结算恢复新流程的模式。 | 自动通过；完整 encounter ID/离线关联未闭合 |
| D19 Fireball 终止 | 独立 impact 按玩家脚部三维半径5筛选，排除 spectator 和 `emissarydebug`；基础35 Dissonance 经普通 Forge 伤害及难度缩放。移除通用 FIRE 弹带入的实体燃烧及额外范围推力。碰撞候选也排除 NPC/debug。保留双子步、60 tick、passthrough、反射和一次终止。 | 已实现切片自动通过；世界放火待决定，整项仍实施中 |

D19 新回归覆盖水平/垂直恰好5格、5.01格之外（身体仍进入粗筛框）、NPC/debug、无敌保护、抗击退、一次终止及沿途碰撞选择器。查询框只为候选筛选增加极小余量，实际距离判定仍是 `distanceSquared <= 25`。测试保留正常难度缩放：Easy 对非玩家来源的基础35伤害变为18.5，Normal为35，Hard为52.5；没有为了断言35而绕过引擎管线。

原包依据为 `emissary/attacks/phase4/fireball/impact.mcfunction`、`slowcast.mcfunction`、generated block/705–707。五个源文件及四个 Java 文件的完整 SHA-256 收录在 [manifest.json](project-recheck-20261001/manifest.json)。源 `impact` 的世界火方块行为仍等待用户选择。

## 既有失败的诊断与夹具修正

- **Flame Geysers：** `created=28` 却查询为空/只有14个，说明同步加载地形并不保证实体 section 可见；此前同批测试的300格玩家选择器和48格节点查询也会串场。现在各自拥有相距1024格的场地，等待所有相关区块 `areEntitiesLoaded` 与 `isPositionEntityTicking`，查询按 owner 过滤。14/28数量、5格间距、39/40帧和20–149计时断言保留。
- **Ice Spikes：** 绝对Y=90的石板可能位于生成地形下方，地表扫描会命中Y=100–141的天然方块；非玩家测试还会受并行攻击污染。首环改用独立高空平台，保留雪 passthrough、半径2和第7 tick断言；非玩家测试隔离攻击来源。
- **Flame Slash：** 旧夹具固定Y=120，40×0.8格路径可能穿入天然地形，提前终止。新场地拥有清空的高空路径，原40步、20点半径2、延迟一tick结算与NBT断言保留。
- **Wing Blast：** 同批marker/reflector和100格射线可以串场。四项测试独立场地、覆盖112格加载范围，并按 owner 查询，保留6 locator、11/12/34/35帧、反射伤害和43 tick清理。
- **Serpent Beam：** 两片失败世界的1681列目标区域均为Y=62的水，原石板在Y=-58海底，source spreadplayers选地必失败。改为独立可用台地，保留delay140、保存/重选及block center断言。身体滞后一拍测试恢复原有旁观者夹具，以满足500格遭遇存活规则；临时票据本身不代表附近存在玩家。

场地使用开发测试类中的临时 region tickets；通过/失败监听器清理自身实体和票据。1200 tick上限仅允许区块准备完成；准备条件满足后马上执行，攻击时序断言没有放宽。开发类整体排除于 JAR。所有旧失败日志、测试世界和产物保留。

## 执行与结果

命令均从 `nyctocosm-native` 执行，Gradle运行使用离线缓存。

| 运行目录 | 结果 | Gradle退出码 |
|---|---|---|
| `run-project-recheck-baseline-20261001` | 215项，212通过，2项Geysers与Ice非玩家失败 | 1 |
| `run-project-recheck-regressions-red-20261001` | 218项，209通过；新模式结算回归失败，其他失败包含新场地准备问题 | 1 |
| `run-project-recheck-regressions-red2-20261001` | 218项，209通过；模式结算及Fireball半径回归暴露旧实现，另外7项场地准备失败 | 1 |
| `run-project-recheck-green-20261001` | 218项，214通过；2项Wing、Serpent缺存活玩家、Fireball预期未计Easy缩放，继续修正后复验 | 1 |
| `run-project-recheck-alpha13-final-20261001` | **219/219 required通过** | **0** |
| `run-project-recheck-alpha13-confirm-20261001` | **219/219 required通过，build/reobfJar成功** | **0** |

最终命令：

```powershell
.\gradlew.bat runGameTestServer --offline --console=plain '-PgameTestRunDir=run-project-recheck-alpha13-final-20261001'
.\gradlew.bat runGameTestServer build --offline --console=plain '-PgameTestRunDir=run-project-recheck-alpha13-confirm-20261001'
python .\tools\audit_native.py .\build\libs\nyctocosm-native-0.3.0-native-alpha.13.jar
```

两次最终世界种子分别为 `8305437981795680883`、`-6212535458866628845`。最终日志：[首轮](../../run-project-recheck-alpha13-final-20261001/logs/latest.log)、[复验](../../run-project-recheck-alpha13-confirm-20261001/logs/latest.log)。完整控制台日志、审计前后快照和 manifest 在 [本次证据目录](project-recheck-20261001)。

项目子目录实际存在 Git 仓库，HEAD=`193a7d8`，本轮用单次 `safe.directory` 参数读取，未改全局信任设置。此前TODO中“没有Git元数据”的判断不准确，此处更正，历史原文保留。`git diff --check` 使用 `cr-at-eol` 识别Windows行尾，退出0；当前大量既有修改/未跟踪文件保留。未自动提交。

## 构建产物

- 路径：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.13.jar`
- 版本：`0.3.0-native-alpha.13`
- 大小：**55,937,267 bytes**；3039个ZIP条目。
- SHA-256：`1fd0e3caaac1cdcc3b7647bc9e93f92b88c0f3e4875af58024cfabfe124c604c`
- 审计：`errors=[]`；包内manifest版本匹配，1779模型、10 rigs、5607动画帧、2601 damage locator帧、247声音事件，0 runtime mcfunction。GameTest/ClientSmoke和测试结构未打包。
- 旧alpha.12 JAR仍为55,936,809 bytes，SHA-256 `e6d49d3838582607255c789ac7fbb41124a531071bd276b519b87a2377df832f`，未覆盖，并另存于本次证据目录。

## 保留的缺口与用户实机检查

- D19/C03的世界放火未确认；D20产物领取方式、拆除退款、卸载事务处理未确认。本轮没有采用推荐项作为默认批准。D02组队/失败补偿、D03离线剧情、D14非玩家概率、D28状态策略、D41成本/冷却等仍按实施补充第5节及历史记录待定。
- D09另复核到既有规格误读：原版 `spreadplayers 5 25` 的25是X/Z方形区域半宽，当前原生实现使用半径25圆盘，采样和量化仍未对齐。本轮保留数量/时序回归，但这些通过不能证明散布分布等价；差异清单和D09报告已补正，D09仍实施中。
- 客户端方向/粒子/声音、多人、第三方保护取消、真实区块卸载、服务端重启和晚加入未在本轮执行。NBT对象重建和GameTest专服启动不替代这些验收。Fireball继续使用D16的实体扫掠，尚未与原包逐子步1格选择箱做完整坐标对照。
- 实机优先：在新测试世界使用1.20.1/Forge47.4.13，客户端与服务端同时安装alpha.13；检查生存入域、最终结算恢复模式及外部改模式不被覆盖；对照Fireball5格边界、反射与双手发射；然后中途保存/重启、客户端重连和多人晚加入验证。原数据包、旧MDK、真实存档和安装目录未由本轮改动。

本轮复查、修正和自动验证完成；以上玩法决策、已列代码缺口和实机项继续待处理，不宣称完整等价移植。
