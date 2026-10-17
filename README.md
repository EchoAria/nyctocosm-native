> 最新编译交付为alpha.20（2026-10-04），与alpha.19生产class/资源逐字节一致；本轮强制重新编译并通过JAR审计，沿用288/288回归。见[编译交付与关机记录](docs/validation/SHUTDOWN_BUILD_ALPHA20.md)。

> 当前开发版为alpha.19，见[三Boss反馈修复](docs/validation/FEEDBACK_REPAIR_ALPHA19.md)：288/288回归、编译和包审计通过，恢复使者护身待机及源飞行时序/位移/重力，修巫师抗击退/开场弧片和巨蛇头身快照同步。协议6，两端同版；自然生存、多人及剩余效果仍待验收。旧版本/报告与关机历史保留。

# Nyctocosm Native — 原生重写开发版

目标平台：Minecraft Java 1.20.1 / Forge 47.4.13 / Java 17。

**这是独立目录中的原生重写 alpha，不是原数据包的完整等价移植。** 当前版本用于测试原生架构、战斗、资源和部分生存交互。Sorcerer 与 Serpent 的关键阶段、移动和持续攻击已进一步恢复，但逐粒子的美术、所有特殊碰撞边界及全流程仍需继续比对；Emissary、剧情流程等也仍未完成。

新开发者或接手者请先阅读 `docs/PROJECT_HANDOFF_CHECKLIST.md`，其中包含项目地图、已知问题、后续任务，以及原数据包文件与原生实现的功能对照表。

## alpha.13：项目复查与战斗边界修正

- 使者最终死亡动画完成后，恢复本流程实际改动且仍由流程管理的游戏模式；旧使者的死亡动画结束前阻止新流程重叠。
- 使者 Fireball 终止伤害按玩家脚部半径 5 格筛选，排除旁观和 `emissarydebug`；移除通用 FIRE 弹带入的实体燃烧和额外推力，保留正常 Forge 伤害流程。
- 为喷泉、翼部射线、冰刺和 Flame Slash 等回归测试隔离场地，按真实区块加载状态开始验证，保留原有伤害边界和时序断言。
- 世界放火、祭坛延迟交付及其他待确认玩法仍保留待定；客户端、多人和真实重启验收范围见本次验证报告。

## alpha.12：地表传送与使者入场

- 跨维度按原 `execute in` 换算 X/Z，再寻找有支撑、净空且无危险的地表；返程仍保留安全的原洞穴高度。
- 修正未加载区块跳过碰撞检测，导致首次进入使者领域落在地下的问题；召唤前分别验证玩家和 Boss 落点。
- 恢复 95 tick 远方陨石入场、落地展开翅膀、后撤升空，以及一阶段悬空远程战斗；索敌范围改为原包的 300 格。
- 入场/悬空状态支持读档，旧版卡地下的一、二阶段实体可恢复；二转三阶段重新对齐地表。Wingbeat 恢复向玩家推进的 4×4 波面与短时漂浮。
- 行为依据、验证边界与实测步骤见 `docs/EMISSARY_ARRIVAL_BEHAVIOR.md`；仍不是完整等价移植。客户端与服务端都应换为此版本。

## alpha.10：回响碎片与传送门

- 将回响碎片交给自己的法阵后，直接瞄准远古城市门框内部即可启动传送门，不再要求命中强化深板岩。
- 恢复灵魂火蓄力、70 段声波束、三阶段共振环与原音效；约 6.1 秒的开门演出结束后才出现门面。
- 重复施法不重置动画，读档继续进度，中途破坏门框会取消；两种方向均有客户端验证。
- 67 项 GameTest 通过；详见 `docs/RESONANCE_PORTAL_BEHAVIOR.md`。
- 法阵须由背包内 Transposition artifact 启用跟随的规则已记入 `docs/FOLLOW_UP_FIXES.md`，本轮尚未修改。

## alpha.9 历史修正

- Serpent 改为持久标记状态控制器：独立上升螺旋、延迟 Dive、固定方向 Laser；保存攻击目标、标记链和逐节身体状态，修复 Laser 读档崩溃与读档长尾拉直。
- Serpent 身体按原三档速度逐节跟随；直接执行原函数核对 420 tick / 17,220 个位置，最大误差 0。光柱在 delay=140 才选点，恢复 80 格射线与圆形判伤。
- Sorcerer 首次出场接完整 rise，按原关键帧上升 5.83 格；法环按原头部 Z 轴及 HEAD 模型变换旋转，127 帧创建、破杖过渡第 5 帧删除，位置和朝向独立保存。
- 61 项 GameTest 包含 Minecraft 原生命令逐刻对照；开发客户端提供原生法环与盔甲架参考的并排连续录制。
- 本轮尚未完成原数据包完整战斗录像对照；Serpent 整体调度、多人战斗和其余 P1–P4 仍需继续核验。详见 `docs/SERPENT_BEHAVIOR.md`、`docs/SORCERER_PLANE_BEHAVIOR.md`。

## 安装与存档

- 构建产物：`build/libs/nyctocosm-native-0.3.0-native-alpha.20.jar`。
- 替换 `mods` 中旧的 nyctocosm-native JAR；不要同时放入多个版本。多人游戏的客户端和服务器均需安装。
- 材质、模型、音效包含在 JAR 内，不需要额外安装原资源包或 OptiFine。
- **不要同时安装旧版 `nyctocosm-0.2.0.jar` 或原 Nyctocosm 数据包**，它们使用相同命名空间，会发生冲突。
- 使用新的测试存档。已能导入可识别的旧 CustomModelData 背包物品、灵魂数量与部分玩家记分板；旧 marker 实体、进行中的 Boss 战和已开启的传送门不可迁移。不要直接用重要旧存档测试；先备份，完整主线使用新世界。
- 不修改旁边的旧 MDK 项目，也不修改源压缩包。

## 已可测试

- 37 个注册物品类型（31 个已映射的原包物品、祭坛、5 个刷怪蛋），独立创造标签。另列出 11 个剧情书章节变体，共 48 个创造栏条目。
- 四种武器、17 种法术的原生基础行为，灵魂瓶/位面线圈、玩家归属灵场、原生投射物和持续法术区域。
- 六类元素伤害使用 `LivingEntity.hurt` 和 Forge 伤害事件，不再使用原包的生物白名单或改写生命记分板。
- 三个 Boss 的独立生命值、Boss 血条、阶段状态、原美术动画数据与分段战斗音乐。Sorcerer 已恢复 6 阶段、独立 300 生命 Nullpoint 与 12 种招式；Serpent 已恢复俯冲、固定方向扫射激光、20 柱延迟光束、40 节躯体、独立尾巴与 20 个受击部位。Emissary 招式仍有简化，见迁移状态文档。
- 原包三个维度的生成 JSON、21 个结构的几何数据（去除实体/命令执行载体），以及自定义战利品的初步转换。
- 原尺寸强化深板岩传送门，Resonance 激活；跨维度安全落点搜索、逐层返回记录。
- 原排列祭坛激活，原生合成界面及四种原配方。
- 原剧情书文字、章节变体、服务器决定的阅读进度；死亡后保留进度。
- 原模型、纹理、音效；将 `_e` 发光贴图接入原生模型渲染。尚未完成与原包逐帧对照。
- 简体中文和英文提供原生物品名称、用途说明与操作提示；选择其他语言时，本模组文字回退为英文。
- Nyctocosmic Ash（夜域灰烬）是原包在夜域挖掘沙砾时获得的基础材料；原生版以全局战利品修改器恢复了该来源，当前没有专用配方。

## 测试指令

需要作弊权限 / 服务器管理员权限。原生版没有原来的 `/function`：

```mcfunction
/nyctocosm summon serpent
/nyctocosm summon sorcerer
/nyctocosm summon emissary

/nyctocosm travel nyctocosm
/nyctocosm travel dusk_plane
/nyctocosm travel emissary_domain
/nyctocosm return
```

也可以使用原生实体指令：

```mcfunction
/summon nyctocosm:serpent ~ ~10 ~
/summon nyctocosm:sorcerer ~ ~ ~
/summon nyctocosm:emissary ~ ~ ~
/give @s nyctocosm:soul_bottle
/give @s nyctocosm:seelic_scythe
```

三个 Boss 基础生命分别为 1000 / 1000 / 1200。建议在空旷的新测试世界召唤。

## 灵场与武器

右键灵魂瓶或位面线圈展开灵场，非创造模式消耗一个灵魂；重复右键关闭。潜行并向正下方看也可取消。使用镰刀击杀生物补充灵魂。

站在自己的灵场内丢出材料施法，例如：回响碎片 → Resonance，荧石 → Luminescence，末影之眼 → Evanescence，凋零玫瑰 → Incurrence。必须是自己刚丢出的材料；其他玩家的掉落物不会被消耗。

镰刀右键短距穿行并消耗生命；Wyrmsliver 右键发射 Nightmare 攻击；Nullfissure 和 Semblance of Doom 区分主手挥击、副手施法。Nullfissure 副手射线使用原包的紫黑双环截面，Semblance 副手弹体包含原发射爆发及火焰/末地烛/烟雾/熔岩弹道。法术元素、施法时间和材料映射见 `Spell.java`；当前不是原包所有数值联动的等价实现。

## 灌注祭坛

以铁砧为 `(0,0,0)`。其下一层 `y=-1` 的俯视排列如下（上为北）：

```text
..CCC..
.C.v.C.
C.SQS.C
C>QBQ<C
C.SQS.C
.C.^.C.
..CCC..
```

- `B`：信标，正上方放完整铁砧（不是开裂或损坏铁砧）。
- `Q`：石英块。
- `S`：涂蜡切制铜台阶，下半。
- 箭头：石英楼梯，下半、直形，朝向中心。
- `C`：点燃的任意颜色蜡烛，下面放支撑方块。
- `.`：不检查，可留空。

站在铁砧相邻位置，与铁砧同一高度，面向铁砧，用灵魂瓶**对空气右键**展开灵场（避免打开铁砧自身界面）。前方一格的排列符合时，铁砧变为原生祭坛，底座保留。之后右键祭坛打开界面。创造栏也有直接放置的祭坛物品；本开发版额外保留了便捷祭坛/灵魂瓶合成配方，并非原包仪式。

| 左侧材料 | 右侧材料 | 产物 |
| --- | --- | --- |
| 4 灵魂沙 | 1 烈焰棒 | 4 Seelic Essence |
| 20 Seelic Essence | 4 金锭 | 1 Planar Coil |
| 40 Seelic Essence | 52 Nightmare Scale | 1 Wyrmsliver |
| 40 Seelic Essence | 6 Null Splinter | 1 Nullfissure |

## 剧情书

未解读书首次右键会根据玩家进度绑定下一章；已经绑定的书重复打开不会推进。再获得一本未解读书才会揭示下一章。创造栏提供全部 11 个章节，便于查看。英文客户端显示原文，简体中文客户端显示完整汉化，其他语言回退英文；章节标题、作者、正文及术语悬浮解释都会随客户端语言切换。原包 Journals 第 5 章自身只有标题和占位内容，未擅自补写。

## 兼容性与验证边界

本模组的注册生物与原版生物均已进入伤害回归测试，但**尚未安装第三方生物模组逐个实测**。第三方模组主动取消伤害、无敌阶段、特殊护盾仍会被正常尊重；这里没有绕过所有防御强制扣血的逻辑。

已进行真实 Forge GameTest 服务端测试、客户端模型/物品栏/祭坛/书籍界面截图检查。测试详情和未完成项见 `docs/MIGRATION.md`。编译成功、自动测试通过不代表全流程或美术完全一致。

## 构建与维护

```powershell
$env:JAVA_HOME='C:\Users\jzy19\.jdks\ms-17.0.19'
.\gradlew.bat build --offline
.\gradlew.bat runGameTestServer --offline
python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.13.jar
# 连续画面，使用已有的可丢弃 NativeSmoke 测试世界
.\tools\run_p0_smoke.ps1 -JavaHome $env:JAVA_HOME
```

离线构建需要本机已缓存的 Forge/Gradle 依赖；其他机器首次构建应去掉 `--offline` 并设置自己的 Java 17 路径。

`tools/import_assets.py` 是**开发时**资源转换器，输入旁边的 `_nyctocosm_source` 和 `nyctocosm.zip`。它提取模型/动画矩阵/原文，不生成游戏内命令解释器。资源导入后应重新测试和构建。美术与原文属于原作者 Flubberschnub；本项目未获得额外再发布授权，公开发布前需核实原许可。
