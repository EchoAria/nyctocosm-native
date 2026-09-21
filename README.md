# Nyctocosm Native — 原生重写开发版

目标平台：Minecraft Java 1.20.1 / Forge 47.4.13 / Java 17。

**这是独立目录中的原生重写 alpha，不是原数据包的完整等价移植。** 当前版本用于测试原生架构、战斗、资源和部分生存交互。Sorcerer 与 Serpent 的关键阶段、移动和持续攻击已进一步恢复，但逐粒子的美术、所有特殊碰撞边界及全流程仍需继续比对；Emissary、剧情流程等也仍未完成。

新开发者或接手者请先阅读 `docs/PROJECT_HANDOFF_CHECKLIST.md`，其中包含项目地图、已知问题、后续任务，以及原数据包文件与原生实现的功能对照表。

## 安装与存档

- 构建产物：`build/libs/nyctocosm-native-0.3.0-native-alpha.8.jar`。
- 放入 Forge 1.20.1 实例的 `mods` 文件夹；多人游戏的客户端和服务器均需安装。
- 材质、模型、音效包含在 JAR 内，不需要额外安装原资源包或 OptiFine。
- **不要同时安装旧版 `nyctocosm-0.2.0.jar` 或原 Nyctocosm 数据包**，它们使用相同命名空间，会发生冲突。
- 使用新的测试存档。尚无旧数据包实体、记分板、CustomModelData 物品到原生实体/物品的存档迁移器。不要直接用重要旧存档测试。
- 不修改旁边的旧 MDK 项目，也不修改源压缩包。

## 已可测试

- 37 个注册物品类型（31 个已映射的原包物品、祭坛、5 个刷怪蛋），独立创造标签。另列出 11 个剧情书章节变体，共 48 个创造栏条目。
- 四种武器、17 种法术的原生基础行为，灵魂瓶/位面线圈、玩家归属灵场、原生投射物和持续法术区域。
- 六类元素伤害使用 `LivingEntity.hurt` 和 Forge 伤害事件，不再使用原包的生物白名单或改写生命记分板。
- 三个 Boss 的独立生命值、Boss 血条、阶段状态、原美术动画数据与分段战斗音乐。Sorcerer 已恢复 6 阶段、独立 300 生命 Nullpoint 与 12 种招式；Serpent 已恢复俯冲、追踪激光、20 柱延迟光束、40 节躯体与 20 个受击部位。Emissary 招式仍有简化，见迁移状态文档。
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
python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.8.jar
```

离线构建需要本机已缓存的 Forge/Gradle 依赖；其他机器首次构建应去掉 `--offline` 并设置自己的 Java 17 路径。

`tools/import_assets.py` 是**开发时**资源转换器，输入旁边的 `_nyctocosm_source` 和 `nyctocosm.zip`。它提取模型/动画矩阵/原文，不生成游戏内命令解释器。资源导入后应重新测试和构建。美术与原文属于原作者 Flubberschnub；本项目未获得额外再发布授权，公开发布前需核实原许可。
