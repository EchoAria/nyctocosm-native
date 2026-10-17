# 扩展游戏表现复查：覆盖矩阵与修复路线

日期：2026-09-30。对象为当前工作区 alpha.12 代码（含既有未提交修改）与 `_nyctocosm_source`。本轮只做代码效果对照、设计和文档更新，没有实施修复、编译或启动游戏。

主清单：[按差异大小排序的 51 项](GAMEPLAY_DIFFERENCE_LIST.md)。逐项设计：[D01–D51 重构方案与验收](GAMEPLAY_REFACTOR_PLAN.md)。本轮新增 **D26–D51，共 26 项**；D17 补充 Death Walker 读档问题。旧数据包存档迁移明确排除，模组自身保存/恢复不排除。

实施交接另见 [修复提示词](AI_GAMEPLAY_REPAIR_PROMPT.md) 和 [实施补充](GAMEPLAY_IMPLEMENTATION_GUIDE.md)。本覆盖表记录静态审计事实，不能替代逐项实施状态或新测试证据。

## 1. 覆盖标准与结论边界

“已复查”指沿原包入口、动画命令 leaf、生成分支、攻击 tick/命中/终止路径，对照原生实际执行，不只看类名和动画资源。本轮覆盖下列模块的核心行为链；不表示已经穷尽每一个粒子采样点、每种随机输入、全部 8192 种神器子集或第三方模组组合。

- **确认差异：** 双侧代码已经能解释不同结果，进入 D 编号，有设计和验收。
- **有对应实现：** 检查的那条规则已有实现，不重复报为“缺失”；不等于该模块所有边界完全等价。
- **需实测：** 源/原生执行轨迹、引擎碰撞与联机结果未实跑；继续留在 V01–V04，不伪造已复现。
- 同一生成分支被复制到不同动画内部命名空间，不按文件数量重复计事件。未从运行入口到达的旧/残留函数也不当成必需玩法。
- 全部引用指向本地代码；本轮没有借网上攻略替代当前版本源码，也没有用旧测试通过来证明新增差异已修复。

## 2. Serpent：三种攻击和整个身体链

证据入口：[tick](_nyctocosm_source/datapack/data/serpent/functions/tick.mcfunction)、[headai](_nyctocosm_source/datapack/data/serpent/functions/headai.mcfunction)、[原召唤/受击体](_nyctocosm_source/datapack/data/serpent/functions/summon.mcfunction)；原生 [SerpentMotion](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SerpentMotion.java)、[SerpentBody](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SerpentBody.java)、[NativeBoss](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)。

| 分支 | 已检查效果 | 结论与方案编号 |
|---|---|---|
| 游走、攻击前盘旋、追逐链 | 区域/朝向/目标 marker 的更新顺序；盘旋即时首步；按速度层级推进头、40 身体段和尾部 | 有专用原生状态和身体追逐逻辑，不是“一条直线的简化蛇”。仍需双端轨迹回放；外层索敌影响见 D38。 |
| Dive | 盘旋后进入俯冲，目标更新顺序、400 tick 上限及离区域重置 | 核心状态已实现；与目标获取/贴身伤害相关的差距归 D38/D47，不重报整招缺失。 |
| Laser | 固定移动目标 marker、100 tick 扫射、最长 150 步、逐点受击条件 | 目标轨迹已有；D48 已修为每格脚部半径3、HurtTime=0 的玩家命中。脚部边界/HurtTime/player-only 自动回归通过，真实100 tick扫射中的伤害间隔待实测。 |
| Beam 柱阵 | 20 个独立柱、随机等待、预警/伤害/黑暗/结束 | 有独立原生 hazard，不能再称“一次瞬时 AoE”；当前脚部范围修正应保留。多人/不同地形与保存仍需实测。 |
| 接触与接受伤害 | 19.5/10 两档贴身伤害；20 个沿偶数段分布的受击体；箭/近战与模组法术的目标类型 | generic 被改 Nightmare：D47；LivingEntity-only 的弹体/范围/射线漏身体部件：D27。 |
| 遭遇退出 | 选招 130 格、无人 500 格移除；区分追逐锁定与发现目标 | 原生外层 80 格/视线与持久化门槛不同：D38。 |

现有 [SERPENT_BEHAVIOR.md](nyctocosm-native/docs/SERPENT_BEHAVIOR.md) 的历史运动测试可以作为底线，但并未覆盖上表所有新问题。

## 3. Sorcerer：十二种攻击逐一对照

时间轴入口：[原 sorcerer tick](_nyctocosm_source/datapack/data/sorcerer/functions/tick.mcfunction) 与动画 `ajsorcererm1/m2` 的命令 leaf；原生 [SorcererTimeline](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SorcererTimeline.java)、[NativeBoss.performSorcerer/tickSorcererFields](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)、[IceSpikeWave](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/IceSpikeWave.java)。下表中的 12 种包含由 Nullpoint 执行的 Ice Spikes。

| 攻击 | 原包 → 当前原生的主要结论 | 修复设计 |
|---|---|---|
| Halobeam | 八节点与发射关键帧已有；独立瞄准锁定、目标后方完整射程、反射/反制不同 | D32，共用 D15/D16，恢复 D17 |
| Shard Hail | 18 数量存在；由迟选地表节点/重力冰锥改成固定中心高空直落弹 | D29/D28 |
| Frozen Blades | 双刃数量存在；追踪弧形切面改为两枚普通直线弹 | D30/D28 |
| Singularity Charge | 名字、慢速弹与爆炸视觉存在；近炸、追踪、到期爆炸不等价 | D31/D28 |
| Void Meteor | 六个发射时点各一枚改为六波各二十枚；裂隙散射/追踪改为天空直落 | D26/D28 |
| Dash Strike | 起手/冲刺事件存在；固定 marker 终点改实时跟人、物理速度与连续范围伤害 | D33/D28 |
| Javelin Swing | 第 35 帧已按局部 `^ ^1 ^2` 与脚部半径球命中；D28 附加状态和实机表现待验 | D36/D28 |
| Seeking Singularities | 8 枚、0.24、70–89 tick 已有；最近玩家眼部追踪、穿墙、小命中区不同 | D34/D28 |
| Void Beam | 持续发射窗口存在；独立平滑追踪 marker 改成每帧直瞄，反射缺失 | D32 |
| Void Wave | 第 38 帧现用根部世界 Y+2 与玩家脚部半径球命中；D28 附加状态和完整 shockwave 表现待验 | D36/D28 |
| Air Dash | 起飞/冲锋/slam 已有；锁点、接近后跳帧与最终落点不同 | D33/D28 |
| Ice Spikes | 30 路波前已做；首次生刺相位、半径、高度、地形搜索、读档/晚加入年龄不同 | D35/D28 |

额外反制核查：[Forbiddance tick](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/61.mcfunction) 按 `spell` 标签筛选；[Halo summon](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/halobeam/summon.mcfunction)、[Hail ball summon](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/shardhail/summon.mcfunction) 有该标签，应能被消除。不能因为同属术士攻击，就把没有该标签的 tear/hail 节点全部设成可反制。原生 Boss 内数组字段不是世界实体，当前 Forbiddance 的实体扫描抓不到它们。

### 六阶段与 Nullpoint

证据：[原 AI subtick](_nyctocosm_source/datapack/data/sorcerer/functions/ai/subtick.mcfunction)、[实际 hitbox 扣血](_nyctocosm_source/datapack/data/sorcerer/functions/ai/__generated__/block/178.mcfunction)；原生 `selectSorcererAttack/customServerAiStep/tickSorcererAnimation` 与 [NullpointStaff](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NullpointStaff.java)。

| 阶段 | 已核对的流程/候选招式 | 对照结论 |
|---|---|---|
| 1 | 空中/杖目标；近 Hail、远 Frozen | 阶段及候选已有；真实招式效果 D29/D30，杖伤害 D37 |
| 2 | 杖破后落地；近 Javelin、远 Dash；本体血量到 666 后升段 | 骨架已有；D33/D36，等待当前攻击与恢复需验收 |
| 3 | 空中；近 Hail/Ice Spikes，远 Frozen/Halo | 候选已有；D29/D30/D32/D35 |
| 4 | 地面；近 Javelin/Seeking，远 Dash/Air Dash；333 阈值 | 候选已有；D33/D34/D36 |
| 5 | 空中；近 Hail/Ice Spikes/Meteor，远 Frozen/Halo/Singularity | 候选已有；D26/D29–D32/D35 |
| 6 | 地面；近 Javelin/Seeking/Void Wave，远 Dash/Air Dash/Void Beam；最终死亡 | 候选已有；招式见上；攻击中归零的提交时机 D37 |

本轮确认的是“候选集/阈值和状态骨架已有”，不是六阶段整体通过等价验收。仍需测试近远边界、不同随机输入的选招分布、动画切换和并行 hazard：原生使用一个 `nextInt(35)` 再取模，不能只看几个枚举分支就宣称所有概率严格相同。普通攻击读档取消归 D17；独立冰刺进度归 D35；杖 raw amount 直接破坏与术士终结时机归 D37。没有把“不需要旧包迁移”理解为“不用保存原生战斗”。

## 4. 普通怪：从自然替换入口到 AI/攻击/退出

入口：[原 spawning](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/spawning.mcfunction) → 实际生成分支；原生 [NativeEvents.joined](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java)。AI 原生均落在 [NativeBoss.tickOrdinary/tickApparition](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)。

| 怪物 | 已沿调用链检查 | 确认差异及保留项 |
|---|---|---|
| Death Walker | 生物群系/替换概率/数量上限；闲逛和追逐；台阶/悬崖；slam 的 leaf50、目标、数值、类型；远距计时及读取 | 自然生成与闲逛计时、91 tick/第 50 帧并非缺失。筛选顺序、原点、地形策略 D39；generic 类型 D47；中途读档 D17。 |
| Nightmare Apparition | 出生 +10Y/净空/偏移；浮动周期；近距互斥；潜行/行走/冲刺惊动；受光退避；模型切换；追击/多人接触/消散 | 出生、最近玩家遮蔽、警戒模型、近距群体中招等 D40。正常/攻击两个原模型已存在，不需要另画替代素材。 |

警戒模型证据：[原 diamond 两个模型映射](_nyctocosm_source/resources/assets/minecraft/models/item/diamond.json) 与 [原警戒分支](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/nightmare_apparition/__generated__/block/21.mcfunction)；[原生 ClientEvents](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/client/ClientEvents.java) 的 kind 4 固定选普通模型。服务端保存 `apparitionAlerted` 不等于客户端能读到并切模型。

边界：普通怪的原命令离远计时，与原生 Monster 自带消失检查可能交叠，需 V03/V04 运行验证；不在未验证引擎调用结果时新增“某一距离必然消失”的断言。原包长时间离远计分、多个全局 selector 的副作用不默认照搬。

## 5. 四武器：主副手、弹道、命中体积与触发效果

源入口：[items/playertick](_nyctocosm_source/datapack/data/items/functions/playertick.mcfunction)；原生 [WeaponItem](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)、[WeaponEffects](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponEffects.java)、[Combat](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)、[SpellBolt](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。

| 武器 | 主手原效果 → 原生差异 | 副手/切换 | 设计 |
|---|---|---|---|
| Scythe | 4 自损/路径打击存在；普通墙可穿与安全终点分离未实现，速度与额外冷却不同 | 原入口只主手，原生 use 未限制副手 | D41/D46；安全性 C02 |
| Wyrmsliver | 横/纵弧形前进波面改直线弹；速度、寿命、施用者减速及命中者致盲不同 | 原入口只主手，原生副手也可调用 | D42/D46 |
| Nullfissure | 原地面/空中采样和近身补偿改锥体；generic 错成 Void、命中减速遗漏 | 原眼前偏移步进/反射改成眼部物理射线；独立 swing/cast 计时变共享 | D43/D46/D47 |
| Semblance of Doom | 横/纵圆周采样改统一锥体；主手伤害/燃烧基础已有 | 副手三子步、命中燃烧和放火缺口，且与主手共享冷却 | D44/D45/D46；放火边界 C03 |

普通左键属性与主动技能分开：检查 give 的 attribute 及原生总伤害时要加上玩家基础值，不能把 AttributeModifiers 的增量直接和总伤害比较。已有主手基础属性、Semblance 普通近战燃烧不构成“整把武器未实现”。本轮未把全套原版附魔/攻击扫击/第三方战斗模组相互作用穷尽验收，留 V04。

## 6. 全部 13 种已注册神器

共同证据：[原持有入口](_nyctocosm_source/datapack/data/artifacts/functions/playertick.mcfunction)、[基础值](_nyctocosm_source/datapack/data/artifacts/functions/load.mcfunction)、[摧毁 trace 三效果链](_nyctocosm_source/datapack/data/artifacts/functions/hypertrace/destroy.mcfunction)；原生 [NativeEvents](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java)、[SeelicField](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SeelicField.java)、[ExpulsionBurst](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/ExpulsionBurst.java)。

**以下所有神器都受 D49 的带 NBT 身份识别问题影响。** “基础已实现”只指普通无附加 NBT、默认属性条件。原包多余的 locomotion 函数没有在本轮发现可达的给物品/持有标签入口，不虚构第 14 件；也不凭残留 activate/deactivate 函数要求重做一个无可达调用的右键开关。

| # | 神器 | 检查的可观察规则 | 结论/方案 |
|---|---|---|---|
| 1 | Transposition | 固定/移动法阵、持续时间、与 trace 的实体分类 | D13/D14；trace 被一起拖到脚下不是正常移动法阵 |
| 2 | Amelioration | 摧毁附近己方 trace 时治疗 6、饱和 3 秒及 power 修正 | 默认基础存在；D14/D22/D49 |
| 3 | Levitation | 潜行触发、脚下两层 AIR、至少 3 充能、消耗 3、缓降 5 秒 | 基础存在；与保护共用资源的边沿时序需验收，D49 |
| 4 | Absorption | 自己 trace 周围非主人获得 Void 弱点；Void 伤害吸血，多个 trace 不重复弱点 | 默认 20% 基础存在；组合顺序 D50，属性扩展 D22，身份 D49 |
| 5 | Accumulation | 附近自己的 trace 数影响生成率，每条基础 +10 与 power 修正 | 基础增量存在；trace 球形距离/触发入口 D14，属性 D22，身份 D49 |
| 6 | Protection Dream | 每充能对应元素抗性 3%，共用消耗时钟 | 基础存在；D22/D49 |
| 7 | Protection Radiance | 同上，仅作用于 Radiance | 基础存在；D22/D49 |
| 8 | Protection Resonance | 同上，仅作用于 Resonance | 基础存在；D22/D49 |
| 9 | Protection Nightmare | 同上，仅作用于 Nightmare | 基础存在；错误 generic 分类的连带影响 D47；D22/D49 |
| 10 | Protection Void | 同上，仅作用于 Void | 与弱点同时存在的运算 D50，错误 generic 分类 D47；D22/D49 |
| 11 | Protection Dissonance | 同上，仅作用于 Dissonance | 基础存在；D22/D49 |
| 12 | Expulsion | trace 摧毁爆炸、Dissonance 击杀延迟 15 tick 连锁、基础 7 | 延迟/连锁已有；脚部/包围盒边界 D51，属性 D22，身份 D49 |
| 13 | Extraction | 摧毁 trace 生成 Soul，归属/拾取 | 产物路径已有；依赖正确 trace 生成 D14，身份 D49；多人拾取继续 V03 |

六种 Protection 是“一份充能/一条 200 tick 扣费时钟”，不是每带一种多扣一次；[原 protection subtick](_nyctocosm_source/datapack/data/artifacts/functions/protection/subtick.mcfunction) 与原生 shared timer 均已检查。修复时要保留这点。

D49 的方法语义还用本机 Forge 1.20.1 映射 JAR 做了只读字节码核查：`Inventory.contains(ItemStack)` 调用 `ItemStack.isSameItemSameTags`。因此新增名称 NBT 会改变匹配结果；这不是在讨论旧数据包物品转换。

### 关键组合（静态对照与计划用例，不声称全部游戏实测）

| 组合 | 确认结果/风险 | 方案与验收关注点 |
|---|---|---|
| Absorption + Protection Void | 同一基础伤害增/减改成顺次乘算，默认属性也有差异 | D50；输入 20、弱点 20%、5 充能时，对比 21 与 20.4 的元素阶段结果 |
| Transposition + 任一 trace 依赖神器 | trace 错误跟随改变布置、距离和回收空间 | 先 D13/D14，再验证治疗/弱点/积累/爆炸，不逐件重复修坐标 |
| Amelioration + Extraction + Expulsion | 原调用链三者同时执行，原生也分别执行，不能互斥成只触发一种 | D49 后一次摧毁只治疗一次、产魂一次、爆炸一次；检查爆炸连锁不重新摧毁已失效 trace |
| Levitation + 六 Protection | 共用充能池；一键消耗 3 与每 200 tick 消耗 1 可能同 tick | 保持共享时钟；在 2/3/4 充能、保护计时 199/200 与潜行边沿组合测试，禁止负充能/重复触发 |
| Accumulation + Expulsion/其他命中产 trace | 不只结束施法时生成，后续命中/连锁会改变资源数量 | D14/D22；核对掷骰次数/时点、主人和 trace 近距判定 |
| 两个吸收持有者/多条 trace 重叠 | 原 weakened 标签防止重复叠弱点；原生 bool 也不是逐 trace 叠加 | D50/V03；保留一次弱点，独立验证所有者选择和友伤边界 |
| 多人同时携带 Expulsion | 原全局 subtick 可能在每个玩家入口重复推进全体 marker | C03 保留原生每世界 tick 一次的合理化，不为“还原”引入随在线人数加速的计时缺陷 |

## 7. 实施切片、验收与仍未覆盖的部分

这里只设计，不实施。各 D 项已在方案文件给出源码依据、改动点与独立验收，可按排序清单逐个审查。

1. **公共最小切片：** D27 部件→父实体命中；D47 伤害类型；D28 状态效果；D15/D16 方向/方块策略。目标脚部球形、局部采样箱、连续碰撞和 IgnoreBlocks 应是显式策略，不用一个放大 AABB 覆盖全部。
2. **单招纵向完成：** 优先 D26 六枚 Meteor，再逐个完成 Hail/Frozen/Singularity/Beam/Dash 等。每招闭合“创建→预警→移动→命中/反制→终止→保存→显示”；不是先导入完粒子才检查伤害。
3. **主线武器和探索：** D41–D46、D39/D40；恢复逻辑 D17/D35/D37 和多人目标 D38 穿插回归。
4. **神器收口：** D49 身份识别、D50 组合计算、D51 距离；D13/D14/D22 的旧差异仍是这些组合的前置条件。
5. **实机验收：** 同版本对照世界、固定随机输入、原/新两端录屏；所有新增用例先证明旧原生不满足断言，再进行修复。此次没有新的通过率；以前 116/116 不是这 26 项的验收结果。

仍需继续实测或扩大源码取样：使者剩余近战/架势/抓取的所有 locator 和跨招交互、原版附魔与其他模组伤害事件、低 TPS/区块边界、多场 Boss 同时战斗、完整生存路线。已静态复查的 Serpent/Sorcerer 不再列成“整个没看”，也不因本轮查过就宣布逐帧完全一致。安全落点、物理保护、放火和全局选择器副作用明确列为 C02–C04 边界。

完成记录追加在 [MAINLINE_GAMEPLAY_PARITY_TODO.md](MAINLINE_GAMEPLAY_PARITY_TODO.md) 阶段 34；不回写覆盖历史阶段的测试或完成结论。


## 2026-10-04 alpha.19 三Boss反馈修复补充

本轮用户确认使者默认问题为出场后的悬浮形态。最终288/288 required回归、build/reobf及42张隔离客户端截图完成，详见 [alpha.19报告](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md)和TODO阶段88–90。

- D18：更正alpha.17误删源FlyTimer的结论，ai/subtick→736/737可达，20..59定时换路已恢复；保留通常不可达的@a自动加速分支判定。恢复>60无玩家的选招/AttackTimer、全部航点转向、起飞1..5升高、降落0..4下降及精确减速，phase4 marker无普通重力，Godrays升高保留；Rain维持玩家+30航点/10..44每帧四弹，源码没有最低高度门槛。96/128范围及防埋地夹紧保留。护身idle以源spawn75 arms/idle0 wings生成，只改客户端待机采样，属于用户要求的表现调整。
- D27关联客户端：头/身位置和角度统一BODY快照，去掉滞后实体头坐标拖动全链；服务端原41节追逐算法和17220命令采样保留。
- D38关联巫师反馈：抗击退1及kind1 knockback屏蔽，保留玩家伤害及技能位移；仅开场smear1按用户要求隐藏，法杖/战斗弧片保留。
- 自然AI免伤玩家夹具捕获三次高空Rain，共105发射帧，root高于玩家18.77..21.17格；随后手动高空场景单独标注。完整37招、原包/mod并排整场录像、真实自然生存/多人/保护模组/shader/重启卸载/晚加入/FPS仍待验收。未选玩法不因回归构建自动闭合。协议6，两端同版；本轮没有关机。
