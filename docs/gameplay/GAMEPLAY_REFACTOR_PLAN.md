# 游戏表现差异：逐项重构方案与验收

> 最新实施（2026-10-04）：[alpha.19三Boss反馈修复](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md)，288/288回归及build/JAR审计通过，护身待机、源飞行时序/位移/重力、巫师抗击退与巨蛇头身同步切片完成，实机仍待验收。原阶段说明保留。

日期：2026-09-30。对应 [差异大小排序清单](GAMEPLAY_DIFFERENCE_LIST.md) 的 D01–D51。新增 D26–D51 来自本轮扩展复查；[覆盖矩阵](EXTENDED_GAMEPLAY_AUDIT.md) 列明已查模块及仍需实测部分。编号是稳定引用，不是优先级，优先级以清单排序为准。

这是一份**待审查的实施设计**，本轮不实施以下代码改动。方案中的新增类名是建议名，不表示当前仓库已有这些类；对原包有副作用的行为须明确选择，不擅自扩大改动/破坏范围。按用户要求，旧数据包存档迁移不纳入本次差异、方案或验收；模组自身保存/恢复仍在范围内。

## 实施交接补充（2026-09-30）

后续执行入口：[AI_GAMEPLAY_REPAIR_PROMPT.md](AI_GAMEPLAY_REPAIR_PROMPT.md)。必须配套阅读 [GAMEPLAY_IMPLEMENTATION_GUIDE.md](GAMEPLAY_IMPLEMENTATION_GUIDE.md)：补充目录/事实优先级、按排序前置最小依赖、逐项参数取证、待决策清单、状态定义、隔离测试/构建和续接记录。

本文件中“本轮不实施”“尚未实施”均是审计/交接轮次的状态记录，不是对未来实施任务的永久限制；真正开工以最新用户授权和清单逐项状态为准。方案中的建议类名不是必须新建的架构，源码尚能确定的参数应继续取证，不把它们当作需要用户猜的玩法选择。明确待用户确认的模式、退款、放火等选项，也不能因获得一般修复授权就视为逐项已批准。

## 对照方法和公共约束

1. 原包规范不是单个 `start` 文件：继续追踪 `__generated__` 分支、动画 `leaf_*` 命令、tick 入口和结束清理。原动画帧号、攻击实体自身 timer、世界 tick 是不同时间轴，不直接混用。
2. 原生规范看服务端实际的实体生成、tick、受击、销毁、保存/恢复，以及客户端事件调用。仅有相同 JSON、模型、声音、动画名称不证明行为已执行。
3. 记录“条件 → 时间 → 位置/朝向 → 目标选择 → 碰撞 → 效果 → 结束”。目前静态可确定的差异见各项；未逐招追完的参数不以猜测填数。
4. 服务端决定命中、随机序列、传送和物品产出；客户端插值/显示。独立攻击不能随着 Boss 动画结束而提前消失，也不能卸载重载后重新全额发射。
5. 共享持久化字段建议包含：schema 版本、owner/Boss UUID、目标 UUID、起点/朝向、阶段、实体 age/子步、随机 seed、已触发事件、终止原因。只保存真正需要续接的状态，不恢复原包全局 marker 混用。
6. 伤害保留 Forge 事件流程；不通过直接修改 Health 绕过其他模组。对原包 `distance` 球形、`dx/dy/dz` 选择箱、方块标签、HurtTime 分别建立测试，不能统一改成随意放大的 AABB。
7. 公共验证：每个改动先有一个能在旧原生代码上失败的行为用例，再实现、跑回归；动画/声效另做客户端双端录屏。保存重载、目标死亡/掉线、Boss 消失、区块卸载均纳入有状态功能验收。
8. 原包素材和源代码仍作为只读参照；不把 mcfunction、命令方块或永久强加载重新塞回运行时。

以下路径均相对当前项目根目录，链接可用于逐项核查。

## D01 — 使者分阶段受击部位（S4）

**代码对照。** [原包一阶段入口](_nyctocosm_source/datapack/data/emissary/functions/ai/hitbox/mode/phase1.mcfunction) 调用 [block/745](_nyctocosm_source/datapack/data/emissary/functions/ai/hitbox/mode/__generated__/block/745.mcfunction) 生成四个 Size:4 slime；[二阶段入口](_nyctocosm_source/datapack/data/emissary/functions/ai/hitbox/mode/phase2.mcfunction) 换另一种受击实体。[Content.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Content.java) 注册使者为 2×5；[NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java) 的 `isMultipartEntity/getParts` 只支持 kind 0。`EmissaryHitRegion` 是 Boss 的攻击/格挡区域，不能替代 Boss 被玩家打的部位。

**方案。** 基于 D18 的公共姿态/locator 计算，定义每阶段的受击部位表；用原生 multipart 或独立代理受击体转发给父 Boss。按原包 hitbox tick 实际跟随点确定位置，不能直接把整片可见模型都设为实心。统一生命值、无敌帧、阶段免伤和死亡；同一箭/爆炸跨多个 part 只结算一次。切阶段原子替换 part，客户端同步实体 ID 和包围盒。

**验收。** 四阶段分别从正面、背面、侧面向翼/躯干/模型空隙射击；该命中处命中、空隙不误伤。箭、近战、范围法术不重复扣血；与镜头旋转、阶段转换、重载/重连后的可见部位一致。

## D02 — Incurrence 上下文、多人入口与模式（S4）

**代码对照。** [incurrence/transmutate](_nyctocosm_source/datapack/data/spells/functions/incurrence/transmutate.mcfunction) 在法阵执行上下文判断 biome；[使者 block/21](_nyctocosm_source/datapack/data/emissary/functions/__generated__/block/21.mcfunction) 选距入口 2 格玩家，[block/22](_nyctocosm_source/datapack/data/emissary/functions/__generated__/block/22.mcfunction) 转维度、地表、冒险模式。[SeelicField.finish](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SeelicField.java) 只传 owner 给 [NativeEvents.incur](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java)，后者使用玩家当前位置、只传自己，不改模式。

**方案。** 引入 `CastContext` 区分法阵原点、创建者、瞄准者与参与者；生物群系/结构条件固定从法阵执行位置取得。使者入口一次收集符合原包距离的队伍，先验证所有玩家和 Boss 的地表净空，再提交传送和逐玩家返程记录。记录独立 encounter ID；失败时不遗留半支队伍或没有 Boss 的领域。模式子项已获用户选择：只在生存玩家成功入域时切至冒险，保存原模式；流程结束/离开/死亡时仅当当前仍是本流程所设模式才恢复，不覆盖流程中由管理员或其他系统改动的模式。组队入口与无安全落点失败处理仍待用户选择。

**验收。** 两人站在法阵 2 格内同时进入，2 格外不进入；施法中 owner 越过 biome 边界仍按法阵判断。任一必要落点失败时结果一致、提示明确；成功时每人可返回各自入口且无重复 Boss。检查创造/旁观、已有战斗、死亡和掉线重进。

**实施进展（2026-09-30）。** 当前单人使者入口在安全落点通过且 Boss 成功创建后，按上述已确认模式规则写入玩家持久数据并切换 Survival→Adventure；离开 `emissary_domain` 和死亡重生路径按流程标记恢复。2026-10-01 复查另补最终结算路径的恢复，以及旧使者死亡动画期间的新入口排重。测试覆盖生存恢复、管理员改 Creative 不被覆盖、Creative 入口不变。多玩家入口/返程及失败补偿没有实现，D02 仍实施中；选择记录见 [D02 验证记录](nyctocosm-native/docs/validation/D02_incurrence_context.md)。

## D03 — 使者首次胜利结局（S4）

**代码对照。** [原成就](_nyctocosm_source/datapack/data/nyctocosm/advancements/bosses/defeat_emissary.json) 的 reward 调用 [cutscene/start](_nyctocosm_source/datapack/data/emissary/functions/cutscene/start.mcfunction)；[cutscene/subtick](_nyctocosm_source/datapack/data/emissary/functions/cutscene/subtick.mcfunction) 在 150、230、310、390、470、550、630、710、790、840、1000、1080、1160、1300 tick 分段执行。首次与重复击败由 [block/33](_nyctocosm_source/datapack/data/emissary/functions/__generated__/block/33.mcfunction) 区分。[原生 completeDefeat](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java) 则统一立即发逃离提示；[原生成就](nyctocosm-native/src/main/resources/data/nyctocosm/advancements/bosses/defeat_emissary.json) 也无上述 reward。

**方案。** 新建服务端逐玩家 `EmissaryEndingSequence`，复用原翻译文本、身份参数和声音。状态包括 encounter、首次资格、age、已发事件位图、临时限制与出口解锁。发成就前快照首次资格；书/经验/标题仍只发一次。首次按时间轴播放，重复胜利直接开放出口。异常离场必须解除本流程自己的限制。离线期间暂停对白并重连续播是建议策略，须与用户确认，不能将全服时间流逝当成玩家已看过。

**验收。** 新玩家完整 65 秒顺序、文本/声音/限制和最后逃离均正确；已有成就者不重播。中途重载、不足 65 秒掉线重连、多名首次/老玩家混合参战都不重复奖品或永久禁跳。禁止用再次授予成就来反复触发奖励。

## D04 — crypt 与 Slatehenge 关联世界事件（S4）

**代码对照。** [crypt hallways end 模板](_nyctocosm_source/datapack/data/structures/structures/crypt/hallways/generic/end.nbt) 中的 `slatehengesummon` marker，经 [structures/tick](_nyctocosm_source/datapack/data/structures/functions/tick.mcfunction) 和生成分支到 [generateslatehenge](_nyctocosm_source/datapack/data/structures/functions/crypt/generateslatehenge.mcfunction)：在 ocean_floor 上方、200 格内无匹配标记时 place structure，然后杀掉触发 marker。[导入报告](nyctocosm-native/docs/structure-import.json) 显示 crypt end 与 slatehenge 模板各删除一个 entity；当前 Java 没有这一关联放置逻辑。**不是所有 worldgen 或 Slatehenge 静态生成都缺失。**

**方案。** 导入时提取这两个 marker 的角色/局部坐标为纯数据，不恢复可执行 marker。结构 piece 落地后把事件排到服务端安全阶段，转换旋转/镜像后的世界坐标，按原高度图和防重范围创建关联地标。`SavedData` 记录已处理 piece/位置和已生成地标；分步放置成功后提交状态，重载不重建。表面持续粒子由轻量客户端表现事件负责。已生成的原生区块补生成可能覆盖玩家建筑，默认只对新生成结构生效；若以后要求追溯补生成，另行审查，不能静默覆盖建筑。

**验收。** 新档生成指定 crypt end 后出现对应地表地标；相邻触发点遵守 200 格条件。区块卸载、重载、服务端重启、旋转模板均不重复放置；没有新增常驻 marker/长期区块票；确认结构 loot、原地形高度与安全失败提示。

## D05 — 恐惧清除其他来源的黑暗（S3）

**代码对照。** [原 playertick](_nyctocosm_source/datapack/data/nyctocosm/functions/playertick.mcfunction) 与 [block/2](_nyctocosm_source/datapack/data/nyctocosm/functions/__generated__/block/2.mcfunction) 限制维度、luminous、survival。[NativeEvents.tickFear](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java) 中 `light = !inDimension || ...` 后直接 `removeEffect(DARKNESS)`，且所有玩家每 tick 调用。

**方案。** 将“本模组恐惧退出”与“玩家具有黑暗效果”分开。未进入本模组恐惧的玩家不做移除；匹配原包的维度/模式入口。退出时清理自身计时/声效，不无条件删除来源未知的同名药水。仅用一个布尔标记不能解决黑暗效果叠加归属，建议采用短期续施、自然到期，或独立恐惧状态驱动客户端遮暗；最终方案需验证监守者效果合并行为。

**验收。** 主世界被监守者施加的 Darkness 不被下一 tick 移除；夜域亮/暗、Luminescence、创造/旁观、冒险模式分别按原条件处理。恐惧退出、维度切换与外部黑暗叠加时均不残留心跳/永久遮黑。

## D06 — Sword Plunge 独立巨剑（S3）

**代码对照。** [activate](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/activate.mcfunction) 从 300 格内随机在线玩家的 `world_surface + 220` 生成巨剑；[subtick](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/subtick.mcfunction) 每实体 tick 加一次攻击年龄、最多三次 slowcast，并在 105/110 tick 调后效/爆炸；[slowcast](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/slowcast.mcfunction) 为 200×1 格、50×0.2 格的垂直子步。[NativeBoss.performEmissary](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java) 原先把位移施加给 Boss 并在自身位置爆炸，与源独立 item-display 攻击载体不同。

**方案。** 新建 `EmissarySwordPlunge` 攻击实体，状态为下落、近地减速、插地/后效、爆发、清理。按源 slowcast 和命令关键帧移植每步速度/碰撞，不用一条匀速插值代替。保留 Boss 悬空锚点，不把落剑运动施加给 Boss；剑模型、预警、落点、伤害共用该实体状态。源 105/110 是攻击实体 timer，不硬套到 Boss attackAge。

**验收。** 目标高低地形对应 +220 起点；Boss 不替巨剑冲向玩家。对照 200/50 子步速度、105/110 tick 后效；空中躲避/离开落点有效。50/104/109 tick 存档恢复不重放落剑/重复爆炸；清理不遗留模型。原包爆炸后有 21×21×7 空气放火，必须先确认 C03 放火策略；generic damage 的属性扩展与 trace 按 D22/D14 验收，不以当前 60 点基础值代表全部管线已等价。当前自动验证和缺口见 [D06 验证记录](nyctocosm-native/docs/validation/D06_sword_plunge_lifecycle.md)。

## D07 — Wing Blast 六路射线（S3）

**代码对照。** [eyes](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/wing_blast/eyes.mcfunction) 在第 12 帧从六个翼尖 locator 创建 marker 并播放 MASTER 频道预警音，[activate](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/wing_blast/activate.mcfunction) 在第 35 帧添加 `blasting`。[subtick](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/wing_blast/subtick.mcfunction) 区分预警/主动阶段；[block/348](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/wing_blast/__generated__/block/348.mcfunction) 按六翼标签逐 tick 转向、射线并在 timer 43 清理，主动射线另在 100 步边界检查玩家、Reflectance 与末端爆炸。原生实现原先只有 `Combat.line` 的一次攻击。

**实现与剩余方案。** 已新增 `EmissaryWingBlastRay` 独立实体，按 phase2 `wing_blast` 第 12 帧的六个 `wingtip1` locator 发射，随机挑选 300 格内合格玩家独立瞄准；第 35 帧切换主动阶段，逐翼 yaw/pitch 每 tick 偏转，timer 43 终结。警告和主动射线均最多 100 格并使用源 passthrough tag；主动阶段按玩家格、hurtTime 和半径 4 终端范围伤害执行，Reflectance 会消耗储量并重定向。frame 0 rise、frame 12 eyes 音效按源 MASTER/50/300 受众范围触发。D15/D16 的标签/资源语义仍待抽到公共服务中核验；客户端 beam 表现和高空对地真实视觉仍须实机确认，不能用 GameTest 代替。

**验收。** 自动 GameTest 已覆盖 12/35 帧、六 locator 位置、保存恢复、墙体、玩家接触、半径伤害、Reflectance 重定向及第 43 tick 清理；完整 155/155 与 alpha.12 JAR 审计通过。未完成六线客户端可见性、高空地形实战、声音听感和多人联机验收；43 是 marker 生命周期值，不表示主动伤害持续 43 tick。见 [D07 验证记录](nyctocosm-native/docs/validation/D07_wing_blast_rays.md)。

## D08 — Pillars 双类型追击柱（S3）

**代码对照。** [activate](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase1/pillars/activate.mcfunction) 在动画帧 33 创建 yaw +60° 火柱和 yaw -60° 云柱；[subtick](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase1/pillars/subtick.mcfunction) 对各自实体推进独立 300 tick timer。[slowcast_cloud](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase1/pillars/slowcast_cloud.mcfunction) 每 tick 寻最近合格玩家并前进 0.4 格，[slowcast_flame](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase1/pillars/slowcast_flame.mcfunction) 前进 0.6 格且对沿路空气填火；[impact_cloud](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase1/pillars/impact_cloud.mcfunction) 给 2 秒失明、2 秒缓慢（放大值 2，即缓慢 III）。

**已实现切片与剩余方案。** 新增 `EmissaryPillar` 云/火两种持久实体，NativeBoss 在第 33 帧生成成对载体；各自独立计时、寻最近非 spectator/debug 玩家、以 0.4/0.6 格每 tick 推进、逐 tick 发 ambient 声音/粒子，并保存 owner/kind/age。玩家 selector 按源脚部方块矩形和 hurtTime 检查；云柱命中给 40 tick Blindness I、Slowness III 并按 35 基础 Dissonance 伤害，Reflectance 能耗并转向已接入。火柱粒子与接触/状态行为已接入，但世界 fire block 放置被明确留空，等待 C03 用户决策后按选择实现和测试；没有默认照搬源 `fill fire`。持续音效由 marker tick 驱动。

**验收。** 已自动验证第 33 帧双 carrier、云柱移动与命中效果、Reflectance 转向、owner/kind/age 保存恢复、299→300 tick 清理，并断言等待选择期间没有火方块改变。完整 158/158 GameTest 与 alpha.12 JAR audit 通过。未完成放火决策与选定规则后的火方块验收、客户端柱体表现及多人实战；Boss 换招后持续活动的全 300 tick 手工验收也待做。见 [D08 阶段报告](nyctocosm-native/docs/validation/D08_pillars_cloud_flame.md)。

## D09 — Flame Geysers 随机延迟布场（S3）

**代码对照。** [activate](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/flame_geysers/activate.mcfunction) 对 300 格内随机最多两名非 spectator/debug 玩家执行；[block/405](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/flame_geysers/__generated__/block/405.mcfunction) 每目标生成 14 marker 并执行 `spreadplayers 5 25`（5 是点间最小距离，25 是中心最大半径）；[summon](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/flame_geysers/summon.mcfunction) timer 为 `20 + rng % 130`。`subtick` 先减计时，再于剩余 20 播声音，剩余 40 tick 内发 36 点半径 5 红尘；`shoot` 逐格处理 y=0..49、5 格玩家范围与 shockwave emitter，然后 impact 伤害/销毁。每 tick 最后的 warning 不应覆盖计时归零喷发的同一 tick。原命令再次选取全局 geyser marker 会重散布先前玩家的组，因此原生按 group UUID 隔离，这是明确的串场安全修正。

**已实现切片。** 新增 `EmissaryFlameGeyser` 持久服务端实体：第 40 帧选最多两名玩家；每组最多 14 个点，以均匀圆盘采样实现 5–25 spreadPlayers 空间语义、局部 WORLD_SURFACE 落点、5 格最小水平间距和有界采样；每点分别保存 owner/group/target UUID 与随机 20–149 计时。按 20 tick 声音、最后 40 tick 预警尘环、归零喷发与 50 个纵向样点求交；基础 Dissonance 伤害经 Forge 管线。NativeBoss 第 0 帧 charge 音、第 40 帧发射；右手 locator 资源增加到 10 个 locator，7 个 cue 帧播放 flame/flash/lava。此前全局 selector 的跨组重散布不复刻。

**待完成及边界。** 原 `effects:shockwave/inculcate` emitter 与三组环形粒子还没等价接入，爆发 plume 仅为受控聚合近似。`shoot` 中 `fill fire` 依赖 C03 世界放火玩法选择，因此当前代码和测试均不写世界火。扩散地形找点有 `14×500` 次界限；加载或安全地表不足时可少于 14，避免无界循环和强制加载。D22 非默认伤害属性外，客户端画面/音效覆盖和真实多人验收仍待做。

**验收。** `flame_geysers` 三项 GameTest 验证第 39/40 帧、单目标 14/双目标 28 点、组 UUID 隔离、地表坐标、均匀圆盘边界与最小点间距、timer/NBT、40 tick 预警开始、归零当 tick 不多发预警环、纵向命中及不放火。全套 161/161 与 alpha.12 JAR 审计通过。见 [D09 阶段报告](nyctocosm-native/docs/validation/D09_flame_geysers.md)。

## D10 — Apocalypse Rain 曲线弹幕（S3）

**代码对照。** [fire](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/apocalypse_rain/fire.mcfunction) 每次四枚左右/上下弹体；动画 leaf_10–44 连续 35 帧各调用一次 fire，共 140 枚；block/692–695 设置初始朝向并分别抽取 yaw/pitch `[-8,+7]` 度扰动。[block/689](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/apocalypse_rain/__generated__/block/689.mcfunction) 的俯仰条件顺序执行并可能累计，每 tick 两次 slowcast；[subtick](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/apocalypse_rain/subtick.mcfunction) 在 timer 到 60 时先结束、不移动。终止 impact 对半径 5 内玩家使用基础 Dissonance 12。原生此前为 19–44 帧在玩家点上空随机生成直落 FIRE bolt。

**实现。** 新增 [EmissaryRainShard](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryRainShard.java)，保存 owner、upper、age、子步和 resolved。NativeBoss frame 0 触发 charge，10–44 每帧发四枚；使用顺序俯仰更新、单子步一格移动、每 tick 最多两步、timer60 终止。接入 passthrough 方块检查、同方块玩家触碰、半径 5 玩家冲击、五种 source trail 粒子和 Reflectance 储量/转向。玩家目标消失不会中止剩余动画发射。D22 动态伤害属性/统计、D15/D16 共用语义及实机视觉仍未闭合。

**自动验收。** 四项 `emissary` GameTest 覆盖动画触发边界/140 枚/四类角度随机范围与根部原点、逐序俯仰与双子步、NBT 和第 60 tick 无移动结算、实块/玩家碰撞与半径伤害、Reflectance 转向和储量。隔离完整 GameTest **165/165** 通过。还需按源每步旋转/移动对照独立坐标采样、客户端粒子与多人/生存实机验收。详见 [D10 阶段报告](nyctocosm-native/docs/validation/D10_apocalypse_rain.md)。

## D11 — Celestial Smite 的 tear → raycast（S3）

**代码对照。** [start](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/celestial_smite/start.mcfunction) 除播放动画外还同步调用 generated block/26、50、93，直接完成一次 tear→circles→activate；动画树随后在帧 26、50、93 再完成第二次延迟射击。[tear](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/celestial_smite/tear.mcfunction) 从随机目标脚下 +100 建点，[raycast](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/celestial_smite/raycast.mcfunction) 逐步检查 150 个位置；沿途 5 格玩家和非 passthrough 方块仅作为射线终止条件，命中后伤害终点 15 格内玩家。generated block/391、394–399 包含射线、终点、伤害和效果链。[原生实现](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryCelestialTear.java) 已复现两次射击的核心事件，但 9×5×9 fire、D22 属性与完整源视觉仍待闭合。

**方案。** 用专用可存档 tear hazard 驱动两次独立射击；分开处理 5 格射线终止、150 步上限与终点半径 15 的伤害。保留 owner/朝向/激活状态。来源调用链确认包含 impact 后 9×5×9 火方块 fill；该写世界行为等 C03 用户选择后再实现，不预设已批准。D22 的基础 Radiance 80、非默认统计和 weakness/resistance 分开验收。

**验收与状态。** 三项 D11 GameTest 覆盖同步起手/第 26、50、93 帧、存档、墙/玩家命中、终点范围与 150 次检查上限；隔离全套 **168/168** 通过，build 和 alpha.12 JAR audit errors=0。报告：[D11 阶段验证](nyctocosm-native/docs/validation/D11_celestial_smite.md)。D11 仍为实施中：C03 火方块、D22、源模型/beamwave 精确表现和多人/客户端实机待完成。

## D12 — Flame Slash 单一波面（S3）

**代码对照。** 动画 [leaf_2](_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/phase3.attack.flame_slash/tree/leaf_2.mcfunction) 调用 [slash1](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase3/flame_slash/slash1.mcfunction)，第 2 帧以 yaw +60° 和局部偏移创建第一波面；[leaf_13](_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/phase3.attack.flame_slash/tree/leaf_13.mcfunction) 调用 [slash2](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase3/flame_slash/slash2.mcfunction)，第 13 帧以 yaw -60° 创建第二波面。[slowcast](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase3/flame_slash/slowcast.mcfunction) 每 tick 最近目标追踪、Reflectance、步进 0.8 和半径 2 的 20 点表面采样；block/542 年龄到 40 的 tick 仍先推进，下一 tick 结算 impact。源基础 Dissonance 为 50。[原生实现](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryFlameSlashWave.java) 已用单一持久波面实体替换五枚直线 FIRE 弹。

**方案与已实现。** 两个动画触发点分别生成 owner 隔离的持久实体；保存年龄、方向、结束状态和待命中目标。每 tick 转向最近合格玩家，步进 0.8，扫描 20 个半径 2 局部截面点；玩家仅在源采样方块内被命中，同一玩家在一次波面结算中去重。源 passthrough 阻挡和第 40 步生命周期分开处理；墙体本身不产生伤害。接入 Dissonance 50 基础 Combat/Forge 管线与当前 Reflectance API。逐 tick facing 命令尚未坐标复现：原生以目标方向直接重算 yaw/pitch；需对照源连续 facing/position 公式。D22 非默认属性/weakness/resistance/记账及精确动画表现继续单列，不视为完成。

**验收与状态。** 三项 GameTest 覆盖第 2/13 帧、±60°、0.8 步长、20 点半径 2、NBT、40 步及下一 tick 清理、玩家命中和仅终止的墙体。final4 完整 171/171 通过；final6 的三项 D12 测试通过，但全套有 6 项既有 Wing Blast/Flame Geysers/Serpent 失败。build 成功，alpha.12 JAR audit errors=0。报告：[D12 阶段验证](nyctocosm-native/docs/validation/D12_flame_slash.md)。仍待源转向坐标对照、D22、完整音画及客户端/多人/生存实机。

## D13 — 法阵类型、持续和跟随（S3）

**代码对照。** [固定创建 block/193](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/193.mcfunction)、[移动创建 block/196](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/196.mcfunction)、[plane tick block/2](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/2.mcfunction)、[移动 tick block/4](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/4.mcfunction)、[取消 spells/cancel_cast](_nyctocosm_source/datapack/data/spells/functions/cancel_cast.mcfunction)。移动时长 = 500 + Dream power×100，仅非 `s.casting` 时扣；固定/移动阵都在无人 3 格内移除。block/4 的 `execute at <linked player> run tp @s ~ ~ ~ ~ 0` 保留 marker 为执行者，因此移动阵每 tick 跟随玩家位置、复制 yaw 且自身 pitch 为 0。`cancel_cast` 清施法并将最近移动阵的 duration 设为 0，固定阵不因此删除。已有 D13 首片已实现固定/移动寿命与施法暂停；本轮 follow-up 发现其移动跟随错误地由当前 Transposition 库存决定，且三格无人清理只作用于固定阵，已按上述源语义修正。

**方案。** 显式区分 fixed plane / mobile plane / trace 和创建时的类型；持有神器变化不重分类已创建法阵。移动阵按其创建时的 mobile 类型持续跟随关联在线玩家，不能因持有物变化停止跟随；固定阵位置不随 Transposition 动态变化。两种 plane 都按源在半径 3 无人时清理；移动阵施法期间暂停时长，取消后走正常寿命清理；固定阵取消只清施法。主人离线与附近其他玩家仍在的行为不靠 `player == null` 一刀切。Dream power 加成继续依赖 D22 属性管线；未猜测补造属性系统。

**验收。** 覆盖固定阵主人离开但另一人留在范围内、任何 plane 在全员离开时清理、移动阵默认 500 空闲 tick、施法暂停、保存恢复、移动跟随与固定不跟随在神器变化后保持、marker pitch/yaw 规则和取消固定/移动施法差异。法阵与 trace 分开后不改变拾取/充能用例。2026-09-30 follow-up 的 5 项 `field_lifecycle` 均通过；两次完整 174 项运行分别有 1 和 2 项既有 Serpent 测试失败，详见 [D13 阶段报告](nyctocosm-native/docs/validation/D13_field_lifecycle.md)。

## D14 — Hypertrace 触发、位置和移动（S3）

**代码对照。** 经扫描所有 `spells/functions/*/cast.mcfunction`，原包 **17 种法术全部**在各自 cast 入口按 `m.global.tracechance` 掷骰，并在玩家朝向前方 3 格调用 summon；施法类型/构成只参与法阵状态，并不把抽样推迟到命中/施法完成。[spells block/58](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/58.mcfunction) 与 [block/81](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/81.mcfunction) 分别在 Nightmare/Void 伤害应用后，对玩家攻击来源掷“对应元素 tracechance + global tracechance”，以攻击玩家为归属、保留受击实体的位置上下文生成 trace。默认元素 chance 初始化为 0，全球 chance 默认初始化为 15。[magic block/0](_nyctocosm_source/datapack/data/magic/functions/__generated__/block/0.mcfunction) 使 trace 每 tick 旋转 1 度；其他 trace 在 1 格内时沿远离方向移动 0.2 格；400 tick 后移除。当前 [SeelicField.finish/tick](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SeelicField.java) 原在完成时统一抽样，且 Transposition 跟随先于 trace 早退。后续 source-audit 还找到 D06–D12 的 Boss 攻击与临时实体发射点；它们的 tracechance 依赖分数未找到可靠初始化。若把缺省分数设 0，源的包含式 `roll <= chance` 会令 chance 0 仍有 1% 触发，故这些分支保持未接入，等用户确认处理规则。

**方案。** 已引入 `HypertraceService`，为 17 个玩家法术起手和玩家来源 Nightmare/Void 命中建立不同入口，归属、元素和随机骰不混用。按源上下文取位置，不把所有 trace 都绑在 owner 或阵脚；保留 400 tick、角度旋转、近邻排斥。修正 Accumulation 的 6 格球形邻居统计，并与 D22 属性接合。其它实体发射分支待厘清其 chance score 的源初始化，再按用户决定接入；不能将未初始化分数静默解释为 0，也不能把命中触发一概扩展到全部伤害或递归 Expulsion 无限生成。

**验收。** 固定随机源验证 inclusive `0..99 <= chance` 边界，基础 15 对应 16 个成功结果；起手和命中分别计数、可中断施法不推迟既定触发。Transposition 下远处 trace 不被吸到脚下；400 tick、互斥、拾取/摧毁、多人所有权、反射后归属均独立测。

## D15 — Reflectance 统一规则（S2）

**代码对照。** [原 fireball/slowcast](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/fireball/slowcast.mcfunction) 将弹体旋转设为附近 reflector 的旋转；[SpellBolt.reflectFrom](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java) 为 `v - 2(v·normal)normal`。[SpellArea.tickReflectance](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellArea.java) 还使用 AABB 和迎面速度条件。正面弹可能看似一致，斜向弹不一致。

**方案。** 设计 `ReflectableAttack` 协议，统一允许反射的攻击、检测体积、容量扣减、反射后方向/速度/owner 和同一节点的重复反射冷却。Nyctocosm 已证实分支采用“沿反射器朝向”，不是镜面算法；其他模组弹体只有显式兼容适配才接入。为各源分支列明例外，不借通用化改大反射范围。

**验收。** 正面/斜面/侧后入射、距离 2.9/3.1、AABB 角落、反射容量正好耗尽；bolt/beam/wingbeat 的共同部分一致、源差异显式保留。反射后不会继续追原 owner 或一帧多次耗能。

**已实施切片（2026-09-30）。** `SpellBolt.reflectFrom` 已改为采用反射器朝向作为出射方向，同时保留入射速度大小、转移 owner、清除 homing 目标并将 HOMING 降为 DAMAGE；GameTest 使用斜向来弹区分旧镜面公式与源朝向规则。当前检测仍使用扩大的 AABB、迎面速度点积过滤和既有 energy 扣减；距离/角落边界、统一协议、跨 beam/hazard 例外与重复反射规则尚未完成，见 [D15 阶段报告](nyctocosm-native/docs/validation/D15_reflectance.md)。

**统一协议阶段（2026-10-01）。** 新增 `ReflectableAttack`/`Reflectance.tryReflect`，按源 `distance=..3` 球体选择，排除同归属的 redirect 候选，沿最近反射器方向并将伤害归属交给其玩家；有有效 foreign reflector 时，所有处于该源 selector 的 reflector 都按攻击 cost 扣容量。SpellBolt 不再有迎面方向门槛，按中心距离精确筛选；同时把现有 Emissary beam、Wingbeat、Wing Blast、Rain、Pillars、Flame Slash 分支接入共用协议，遇到同一 shield 后以新归属避免重复扣能。两项 D15 专项测试和已有 bolt/beam/ray/hazard 反射用例在最终 203 项运行中均通过；全套另有两项既有 Flame Geysers 用例失败。仍待 D32 的 Sorcerer Halo/Void Beam 分支、D22 非默认 power 容量、重叠反射器 tie 顺序与客户端/多人实机验收。

## D16 — passthrough 与物理碰撞（S2）

**代码对照。** [passthrough.json](_nyctocosm_source/datapack/data/nyctocosm/tags/blocks/passthrough.json) 定义命令 slowcast 可穿方块，[fireball/slowcast](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/fireball/slowcast.mcfunction)、lightning_javelin、seeking_missiles 与法术移动分支也逐体素使用此标签。原生通用 [SpellBolt.tick](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java) 原使用物理碰撞；标签中的草、水、火、雪与标签外的无碰撞植物可产生相反结论。

**方案。** 抽取可配置 `SpellCollisionPolicy`：指定源分支使用标签体素 DDA 和原子步，实体命中另做扫掠，Forge projectile impact 事件继续保留；其余 projectile 仍使用原版物理碰撞。光束、普通弹、传送寻路不共用一个默认规则，原版箭保持原版。

**实现（2026-10-01）。** 新增 `SpellCollisionPolicy`，按源攻击设置 passthrough、blocked-voxel hold 或 ignore-blocks。接入 SeelicField 的 Abhorrence、Incandescence、Fulminance、Gravitance，以及 Emissary 的 lightning_javelin、seeking_missiles、fireball；Gravitance 在阻挡体素停留而保留其攻击 tick。逐体素 DDA 检查源 tag，斜穿角点检查同时相交的侧体素；实体扫掠仍与方块轨迹分开并按最近命中结算，Forge impact event 仍可取消。越过世界高度边界视为阻挡，跨入未加载区块即停止扫描，过长/非有限扫掠不继续推进。策略通过 NBT 保存；旧 `IgnoreBlocks` 字段仍兼容。其它未审计 SpellBolt 调用和 vanilla projectile 不改。

**验收状态。** 四项 `spell_collision` GameTest 均通过，覆盖源 tag 与物理形状相反的方块、快弹穿墙、斜角边界、build-height、未加载 chunk、Forge impact 取消和 NBT 往返。最终 207 项完整套件中 204 项通过；两项既有 Flame Geysers 和 `serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step` 未通过，不经过 D16 路径。独立 build 成功，alpha.12 audit `errors=[]`。客户端轨迹/视觉与多人实机仍待验收，详见 [D16 阶段报告](nyctocosm-native/docs/validation/D16_spell_collision.md)。

## D17 — Sorcerer / Death Walker 攻击恢复（S2）

**代码对照。** [原 sorcerer/load](_nyctocosm_source/datapack/data/sorcerer/functions/load.mcfunction) 注册攻击 timer/objective；相关 marker/score 是持久世界状态。D17 修改前，[NativeBoss.readAdditionalSaveData](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java) 先 idle/attack=null，kind 1 只恢复 initial_spawn、rise/fall 等五种 clip；hailAge、meteorTearAge 等又单独恢复。这是普通攻击被取消的明确分支。补查 Death Walker：kind>=3 只读回 OrdinaryState 等字段，不恢复 slam 的 Remaining/AnimationAge，也会打断已开始的砸击；[原怪物 tick](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/death_walker/death_walker_tick.mcfunction) 的攻击依赖原持久动画状态。原生成动画仅在 [slam leaf_50](_nyctocosm_source/datapack/data/zzz_death_walker_internal/functions/animations/slam/tree/leaf_50.mcfunction) 调用伤害函数，映射到原生 `AnimationAge=50` / 下一 tick `Remaining=41`；相邻 leaf_49/leaf_51 不调用伤害。D17 已增加攻击 ID 与计时恢复；隔离专服重启 smoke 确认 entity region 文件保留 slam 帧字段，但无玩家时无法确认实体被激活到运行时选择器，后续仍需玩家加载该区块并验收客户端表现。

**方案。** 逐个 Sorcerer attack ID 与 Death Walker 的 slam 建可恢复映射，保存普通攻击 age、剩余时间、选择目标、已生成攻击/效果标志和过渡待办。独立 hazard 保留独立寿命，但和同一 cast ID 关联，不重放已执行发射事件。未知旧存档 clip 安全退空闲并清理/续接相关状态应有版本化规则，不能静默同时开启两招。

**验收。** 每类攻击取准备中、发射前一帧、发射后一帧、持续中、收招前各保存恢复；对比不读档版本的剩余命中次数/波次。Nullpoint 护盾、六阶段、升降不回退；Death Walker 第 49/50/51 帧恢复不丢砸击、不重复伤害；跨区块和重启都测。

**已实施切片（2026-09-30）。** `NativeBoss` 在 Sorcerer NBT 中保存/恢复活动攻击 ID、`attackAge`、remaining、动画 age 与目标 UUID；ICE_SPIKES 用独立 ID，因为显示 clip 是 idle。HaloBeam 保存已经生成的节点位置，避免读档后后续射线改变起点。原本独立保存的 Hail、Meteor 状态继续按现有字段恢复，keyframe age 不重置，从而不重复发射/生成。Death Walker kind 3 只对有效 `slam` clip 恢复 `remaining` 与动画 age，并规范化普通状态；未知 clip 安全回空闲。5 项新增 GameTest 覆盖全部 Sorcerer attack ID、HaloBeam 节点、Hail/Meteor keyframe 续接以及 Death Walker 砸击伤害帧两侧；Death Walker 用例已改为新建实体读取保存的 NBT。完整记录见 [D17 阶段报告](nyctocosm-native/docs/validation/D17_attack_restore.md)。D17 仍实施中，自动 NBT fixture 不代替真实 chunk unload、服务端重启和战斗逐招验收。

## D18 — 共享节点计算与飞行根姿态（S2）

**代码对照。** [sword_plunge/activate](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/activate.mcfunction)、meteor shower、lightning javelin、seeking missiles、fireball、divine blast、godrays 与 grapple 均从 Animated Java locator/root-local 坐标取点；[phase4/movement](_nyctocosm_source/datapack/data/emissary/functions/ai/phase/4/movement.mcfunction) 还会更新使者根部 yaw/pitch。原生 [NativeBoss](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java) 多处使用眼睛/包围盒中心加偏移，[ClientEvents](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/client/ClientEvents.java) 的使者 rig 根原只应用 yaw。

**方案。** 扩展现有 rig 数据导出，保存需要的命令 locator、局部点/方向、阶段 variant 和动画采样。`EmissaryPose` 在客户端和服务端共享纯数学变换：坐标单位、镜像、yaw/pitch、插值一致；不在服务端加载渲染类。先以已修 Wingbeat 作基准，再逐招替换占位起点；飞行 XRot 接到根姿态时避免与动画自身倾角重复。

**验收。** 四个正交 yaw、正负 pitch、关键帧/半帧插值下记录 locator 世界坐标；发射物从手/翼/对应节点出现。录屏比对爬升、俯冲、转向、起落和暂停读档，命中体不脱离模型。该公共工作可前置实施，但不改变差异大小排序。

**已实施切片（2026-10-01）。** 新增服务端安全的 `EmissaryPose.localPoint`，使 `EmissaryDamagePose` locator 与客户端 `RigTransforms.orientation(yaw,pitch)` 共用 Minecraft yaw/pitch 坐标基底；使者 renderer 按 `xRotO → getXRot()` 插值应用 root pitch。locator 导出扩为 16 个命令 locator、44 动画、2601 帧，包括左右手、seeking missile 的 wingtip2 节点和 grapple wingtip3；审计验证整组 locator、每帧完整性和变换有限值。原生接入火球左右手、Lightning Javelin 右手、7 个 Seeking Missiles 源发射点、Meteor Shower/Wingbeat center、Divine Blast/Godrays center、动态 Grapple seat 与 Sword Plunge 手部轨迹粒子；Wing Blast/Flame Geysers 继续用既有 locator 轨迹。新 GameTest 覆盖四方 yaw、正负 pitch、局部基向量、半帧 locator 插值及火球/Seeking/抓取起点。209 项全套中 205 项通过；4 项失败均不经过 D18 新接入分支。独立 build 成功、alpha.12 审计 `errors=[]`。客户端录屏、多人生存战斗、所有未审计招式仍待逐项验收，见 [D18 阶段报告](nyctocosm-native/docs/validation/D18_emissary_pose_and_locators.md)。

## D19 — Fireball 子步和超时（S2）

**代码对照。** [block/703](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/fireball/__generated__/block/703.mcfunction) 每 tick 两个 slowcast、timer 60 标记 end，再 impact；[slowcast](_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/fireball/slowcast.mcfunction) 一步一格。[原生分支](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java) 速度 1.2、寿命 100；[SpellBolt](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java) 普通 FIRE 超时直接 discard。

**方案。** 给使者 Fireball 配独立运动/终止 profile，按源每 tick 两子步推进；碰墙/碰人/到期均走各自明确 impact 原因。D15/D16 负责方向和方块规则。继续沿源发射帧/连发选择器核查，不能只改 speed 数字就宣称整招完成。

**验收。** 同距离命中耗时、60 tick 最远距离、末 tick 是否还前进/impact 都有边界测试；离开射程不会在 100 tick 时才消失。斜向反射、击中实体、超时均只有一次终端事件。

**已实施切片（2026-10-01）。** `SpellBolt` 增加 Emissary Fireball 专属 profile：每 tick 两个一格子步、60 tick life、碰实体/阻挡体素/到期分别以 `ENTITY` / `BLOCK` / `TIMEOUT` 原因终止并仅结算一次。保留 D16 passthrough 和 Forge impact 事件；反射后维持两子步。沿原包补查了第 8/18 帧右/左手发射、block/711/712 的随机玩家 feet selector（100 格、排除旁观者与 `emissarydebug`）、地面 phase4 的 66..85 Fireball 选招范围；原生改为相同目标过滤/脚部方向，并移除源流程没有的第 20 帧 50% 同招重播。215 项全套中 211 项通过；4 项非 Fireball 失败见 [D19 验证报告](nyctocosm-native/docs/validation/D19_emissary_fireball.md)。构建与 alpha.12 包审计成功。原包终端的 3×3×3 世界放火仍等用户选择，故 D19 保持实施中；客户端、多人和保护插件实机验收未做。

**alpha.13 复查补充（2026-10-01）。** 重新追踪 `impact` 与 block/705–707：终端按脚部半径5筛玩家，排除 spectator/debug，基础35 Dissonance 走普通 Forge/难度管线，没有源外的实体燃烧或额外范围推力；碰撞候选也排除 NPC/debug。新增水平/垂直5格闭边界、5.01格反例、无敌保护、抗击退和一次终止回归。世界放火继续等待选择；本次219项回归和产物信息见 [alpha.13复查记录](nyctocosm-native/docs/validation/PROJECT_RECHECK_ALPHA13.md)。

## D20 — Dais 有状态灌注（S2）

**代码对照。** [crafting/dais/inculcate](_nyctocosm_source/datapack/data/crafting/functions/dais/inculcate.mcfunction) 播放动画，[110 帧效果](_nyctocosm_source/datapack/data/zzz_inculcate_internal/functions/animations/inculcate/tree/leaf_110_effects_0.mcfunction) 调用 [inculcate2](_nyctocosm_source/datapack/data/crafting/functions/dais/effects/inculcate2.mcfunction) 再生成物品。[DaisMenu](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/DaisMenu.java) 是立即给产物、onTake 扣料、一次性粒子；成本已经对齐。

**方案。** 保留原生菜单，增加方块实体中的灌注事务：recipe、锁定材料、age、完成标记、产物。开始时只扣一次，110 事件才提交产物并播放完整 rig/悬浮材料表现；是否以世界掉落还是结果槽领取需审查，前者更接近原包。拆除/爆炸/卸载的取消与返还规则必须先确定，不能复制物品或吞材料；多用户打开时共用同一事务。

**验收。** 四配方成本/数量不变；110 前不能获得成品，完成只发一次，拾取延迟符合所选模式。满背包、Shift 连点、同时取物、完成前重启/拆除、完成后未取走都无复制/丢失。

## D21 — Transmittance 球形乘客边界（S2）

**代码对照。** [transmutate](_nyctocosm_source/datapack/data/spells/functions/transmittance/transmutate.mcfunction) 筛 `@a[tag=in_plane,distance=..3]`。[SeelicField.finish](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SeelicField.java) 收集 `getBoundingBox().inflate(3)` 内所有玩家，虽然之后各自按方向传送，但前置筛选不等价。

**方案。** 使用 D02 CastContext 和源 `in_plane` 进入/退出条件定义原生参与者集合，AABB 只做粗筛，另以指定原点做真实距离筛选。此项不应顺手重写已对齐的逐玩家朝向或扩展最大距离。

**验收。** 2.99/3.01 格、平面内角落/平面外角落、上下高度、相邻两个法阵交叠、不同朝向各有测试；范围外队友不被意外带走，合法乘客保留缓降/落点安全。

## D22 — 元素属性管线（条件性 S2）

**代码对照。** [calcstatsspell](_nyctocosm_source/datapack/data/magic/functions/calcstatsspell.mcfunction)、[calcstatsdamage](_nyctocosm_source/datapack/data/magic/functions/calcstatsdamage.mcfunction)、[Abhorrence block/296](_nyctocosm_source/datapack/data/spells/functions/abhorrence/__generated__/block/296.mcfunction)、[block/297](_nyctocosm_source/datapack/data/spells/functions/abhorrence/__generated__/block/297.mcfunction) 展示 base/modifier、全局后元素的顺序运算与整数除法；[Accumulation block/10](_nyctocosm_source/datapack/data/artifacts/functions/accumulation/__generated__/block/10.mcfunction) 也使用 power。当前 [Combat](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)、[Spell](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Spell.java)、[NativeEvents](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java) 主要固定值加个别神器处理。

**影响边界。** 原包 reset 默认 power/speed 多为零，已移植的保护/吸收也覆盖部分实际玩法；这是属性扩展和特定修正状态不等价，不足以证明所有普通新档战斗数值全面偏离。不要因此虚构新的装备成长路线。

**方案。** 先枚举原包真正写入各属性的来源与可达路径，再设计 `MagicStats`（全局+六元素）及施法开始快照。按源顺序、整数舍入、上下限计算 cast time/power/概率；防御在明确的一次伤害阶段处理，避免 Protection/Absorption 重复计算。外部修改接口另行审查；本次不设计旧 scoreboard 值迁移，也不默认自动接收任意旧值。

**验收。** 零修正与现有基础行为相同；单全局修正、单元素、多元素混合、负值/上限及 100% 边界有确定性例子。舍入和连续乘算对照原命令，不用一次浮点求和近似；Forge 护甲/事件仍生效且不双算。

## D23 — Tony 冷却跨维度计时（S2）

**代码对照。** [原 playertick](_nyctocosm_source/datapack/data/nyctocosm/functions/playertick.mcfunction) 在维度条件之外移除 tonytimer；[TonyManager.tickPlayer](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/TonyManager.java) 先检查夜域，非夜域即返回。Tony 的实体、掉 Dreams、黑暗/crypt 条件已有实现，不重写成整段缺失。

**方案。** 把冷却推进移到通用玩家 tick，保留在夜域特定结构/亮度条件下才尝试生成；原包是在线 tick，不自动改成离线现实时间。保存的剩余冷却可沿用；每秒触发采样和落点失败后是否重置冷却需另取源行为测试，不在此次未经验证地改掉。

**验收。** 剩余 100 tick 时去主世界在线等 100 tick 再回夜域，冷却已结束；离线 100 tick 不冒充在线计时。结构外/有光时不生成，跨维度反复往返不重复猫或重置冷却。

## D24 — 音乐片段队列（S1）

**代码对照。** [ai/subtick](_nyctocosm_source/datapack/data/emissary/functions/ai/subtick.mcfunction) 在 1000/800/600/400/200 阈值排队；[music/nextpart](_nyctocosm_source/datapack/data/music/functions/emissary/nextpart.mcfunction) 有独立片段状态。[BossMusicController](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/client/BossMusicController.java) 则 `phase()-1` 四组，`isPaused()` 调 stop 清状态。

**方案。** 导出原 intro/start/continue/queue 组成的有限状态机和各片段时长；把战斗阶段与音乐阶段解耦。服务端提供 encounter 和必要阈值状态，客户端在片段边界切换；不因暂停清掉游标。离开范围/死亡/换 Boss/重连的音乐生命周期显式处理；不能同时播放多条流或恢复错误的另一个玩家战斗音乐。

**验收。** 在 1001→1000、801→800、601→600、401→400、201→200 分别录制，下一首与原队列相符；慢打/跳血阈值也不丢队列。单机暂停恢复不重播 intro，胜利尾曲正常，退出世界无残音。

## D25 — 死亡与声效/效果命令事件（S1）

**代码对照。** [die/leaf_0](_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/die/tree/leaf_0.mcfunction) 播放 `emissary.death.begin`；[finaldeath](_nyctocosm_source/datapack/data/emissary/functions/finaldeath.mcfunction) 调终结粒子和奖励。当前 [NativeBoss.tickDeath](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java) 无 kind 2 专用事件分支；[NativeEvents.sound](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java) 的通用声道与源 MASTER/受众筛选也不同。

**方案。** 建每动画/攻击的语义事件表：帧、locator、粒子方案、sound ID、类别、音量、音调、受众距离。优先补已确定的死亡开始和最终爆发，再逐招核对遗漏，不能见到声音文件就全局播放。语义伤害/时序与表现分离，低粒子设置可以降密度，不能缩短预警。奖励继续使用已存在的单次结算路径，与 D03 对话时序衔接。

**验收。** 死亡 clip 开始、100 tick 终结、D03 开始三处事件各一次；不同声音滑块、距离和旁观玩家听到预期内容。重载不重放已触发事件，低粒子设置仍能辨识危险区；截图/录屏与源关键帧对照，资源缺失单列 C04。

## 扩展复查新增方案（D26–D51）

以下 26 项只完成分析设计，均未实施。共用能力是实现建议，不要求一次性重写所有战斗代码。

## D26 — Sorcerer：Void Meteor 数量、弹道与计时（S3）

**代码对照。** 原包：tear 激活后六个计分时点各发一枚，共 6 枚；从裂隙散射并追踪玩家，弹体 25 tick 进入爆炸分支。 模组：六波各 20 枚，共 120 枚；从保存的目标点附近上空垂直落下，普通弹默认寿命 100。

**证据入口。** 原包 [sorcerer/attacks/voidmeteor/__generated__/block/109.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidmeteor/__generated__/block/109.mcfunction)、[sorcerer/attacks/voidmeteor/__generated__/block/112.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidmeteor/__generated__/block/112.mcfunction)、[sorcerer/attacks/voidmeteor/__generated__/block/113.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidmeteor/__generated__/block/113.mcfunction)、[sorcerer/attacks/voidmeteor/__generated__/block/119.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidmeteor/__generated__/block/119.mcfunction)、[sorcerer/attacks/voidmeteor/procast.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidmeteor/procast.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 以独立 tear 状态保存创建、激活、计分器、发射序号及朝向。六个事件每次只生一枚，从 tear 朝最近合法玩家方向叠加源角度散布；独立 meteor 使用源追踪子步、25 tick 终止/爆炸。特别注意 block/109 和已激活 block/112 同 tick 都加计分器，不能把源码的 20 分直接读成 20 游戏 tick；先导出逐 tick 事件序列。保留服务器决定命中、客户端渲染，接 D15/D16/D28。

**验收。** 单次施法、单个 Boss 应恰好生成 6 枚而不是 120 枚；分别在激活前后移动目标，比较发射原点、事件 tick、轨迹与超时爆炸。现有 meteorBoltsPerWave=20 相关断言必须按源证据改写，不能把旧测试当规格。

## D27 — Serpent：躯干受击体与原生法术命中（S3）

**代码对照。** 原包：20 个沿身体偶数段分布的 Size:8 slime 是 livingthings，可被原包武器/法术选择器命中。 模组：有 20 个 3×3 PartEntity，但 SpellBolt 与 Combat.area/line 只接受 LivingEntity；普通弹体/范围法术没有对应的躯干命中路径。

**证据入口。** 原包 [serpent/summon.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/summon.mcfunction)、[nyctocosm/tags/entity_types/livingthings.json](_nyctocosm_source/datapack/data/nyctocosm/tags/entity_types/livingthings.json)、[nyctocosm/tags/entity_types/mobs.json](_nyctocosm_source/datapack/data/nyctocosm/tags/entity_types/mobs.json)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)、[SerpentBody.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SerpentBody.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 保留已对齐的 41 节身体追逐链，独立建立 DamageTarget 解析器，将 part 命中转给父 Boss，并保留命中位置。原始 Slime NBT 的实际尺寸通过同版本实体夹具取得，不把 Size 数字当方块宽度。弹体、射线、范围攻击都查询部件；同一次攻击按父实体去重，防止横穿十节造成十倍伤害。与 D01 共用能力但分别验收。

**验收。** 只瞄身体中段而不碰主实体，用普通近战、箭、Wyrmsliver、Nullfissure 射线及范围法术各测一次；应命中父 Boss、归属正确且每个允许伤害窗口只结算一次。移动/旋转/远端客户端部件与可见身体一致。

## D28 — Sorcerer：负面效果的施加对象与时机（S3）

**代码对照。** 原包：多招在 hit 分支给真正命中者减速/黑暗；Shard Hail 另有地面节点近距减速。 模组：beginAttack 直接给当前目标减速/黑暗，部分发射事件再次施加；通用弹命中其他玩家却没有对应效果。

**证据入口。** 原包 [sorcerer/attacks/frozenblades/hit.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/frozenblades/hit.mcfunction)、[sorcerer/attacks/singularitycharge/hit.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/singularitycharge/hit.mcfunction)、[sorcerer/attacks/voidwave/hit.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidwave/hit.mcfunction)、[sorcerer/attacks/voidmeteor/hit.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidmeteor/hit.mcfunction)、[sorcerer/attacks/shardhail/__generated__/block/21.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/shardhail/__generated__/block/21.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 移除无源依据的起手强制效果，逐招定义 HitProfile（目标、持续时间、放大值、伤害与效果的调用先后）以及 Hail 的接近效果。不要只给被锁定玩家发状态，也不要把所有效果硬绑到成功扣血之后：先保留原命令语义，再明确 Forge 取消伤害是否连同效果取消。Void Wave/Meteor 原命令的 darkness 放大值也单独保留记录。

**验收。** A 被锁定但全躲开，B 进入真实命中区：A 不凭起手中招，B 获得源规定效果；Hail 的地面减速单独测。覆盖减速 10 秒/5 秒、黑暗 4 秒/5 秒及免疫/事件取消策略。

## D29 — Sorcerer：Shard Hail 独立落点、下坠与反制（S3）

2026-10-02实施补正：独立三stage攻击及232项回归见 [D29报告](nyctocosm-native/docs/validation/D29_shard_hail.md)。原选择器sort=nearest无limit，实际按所有80格合格玩家依次散布，通常最终最远者；节点score41的直接减速还保持spread前函数进入位置。以下旧方案的“按附近玩家”不能解释成只选最近一人。

**代码对照。** 原包：冰球逐 tick 上升，之后生成 18 个带随机等待的节点；节点稍后按附近玩家散布地表、给局部减速，从其上方 30 格落冰锥，落地后触发。 模组：施法事件保存目标中心，延迟后在周围随机天空点发 18 枚匀速直落弹；没有相应地面节点、晚选目标或落地计时。

**证据入口。** 原包 [sorcerer/attacks/shardhail/__generated__/block/18.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/shardhail/__generated__/block/18.mcfunction)、[sorcerer/attacks/shardhail/__generated__/block/19.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/shardhail/__generated__/block/19.mcfunction)、[sorcerer/attacks/shardhail/__generated__/block/21.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/shardhail/__generated__/block/21.mcfunction)、[sorcerer/attacks/shardhail/__generated__/block/24.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/shardhail/__generated__/block/24.mcfunction)、[spells/__generated__/block/61.mcfunction](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/61.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 拆分 HailBall→18 个 HailNode→FallingIcicle：分别保存年龄、等待、落点和落地状态；依源实际调用时刻选择玩家并做地表散布，不在起手冻结整片区域。冰锥按原重力/落地后第 2 tick 触发，地面节点维持预警及近距减速。带 spell 标签的冰球必须可被 Forbiddance 消除；不把无该标签的后续节点一律改成可反制。

**验收。** 移动目标分别在球升起、节点等待、落下阶段转弯；测 18 个节点、不同高度地形、预警与伤害间隔，计时不因多玩家加速。上升冰球被反制后不能再孵化；中途卸载只恢复未完成事件。

## D30 — Sorcerer：Frozen Blades 追踪波面（S3）

2026-10-02实施补正：双波面及236项回归见 [D30报告](nyctocosm-native/docs/validation/D30_frozen_blades.md)。手骨ItemDisplay命令坐标在root，动画矩阵不是locator；两次procast只移动一次.4，首次转向被移动tp的进入rotation重置，第二次转向保留。下文“首帧动画手部locator”设计已经由原版命令夹具否定，不按该错误前提实现。伤害实际Void14，命中状态待D28。

**代码对照。** 原包：左右手节点带初始旋转发射，两片冰刃持续转向最近玩家；局部弧形采样伤害，递归子步推进。 模组：从眼前左右偏移发两枚速度 1.25 的普通直线弹，半径 1；冰粒子不代表冰刃的弧形命中面。

**证据入口。** 原包 [sorcerer/attacks/frozenblades/throw.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/frozenblades/throw.mcfunction)、[sorcerer/attacks/frozenblades/procast.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/frozenblades/procast.mcfunction)、[sorcerer/attacks/frozenblades/__generated__/block/78.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/frozenblades/__generated__/block/78.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 做双 BladeWave，记录发射手、朝向、子步计数和命中历史；按源局部坐标推进/追踪与弧形采样，不用扩大弹丸半径代替。首帧用动画手部 locator，按 D15/D16 接反射/阻挡，命中状态归 D28。粒子可抽样，服务端采样集合不缩水。

**验收。** 左右两刃分开验证；玩家横移/绕后/变高后曲线应随源转向。比较弧形边缘内外、同 tick 多采样去重、反射与 100 tick 清理，晚加入客户端不重启轨迹。

## D31 — Sorcerer：Singularity Charge 追踪、近炸与超时（S3）

2026-10-02 实施补充：已经实现独立弹道／三种终止／源反射并验证，详见 [D31报告](nyctocosm-native/docs/validation/D31_singularity_charge.md)。原版命令夹具更正空间上下文：tp后的转向通过`at @s`使用新位置，而近炸、体素、爆炸和反射选择保持函数进入位置；反射tp也从该进入位置重定位。原生旧“直线到期discard”说明仅作为历史反例保留。D28状态待用户选择，不标整项完成。

**代码对照。** 原包：以 0.3 子步并转向近处玩家；玩家接近 3 格、遇不透过方块或达到 100 tick 时走爆炸分支，爆炸半径 5。 模组：0.3 速度直线 DAMAGE 弹；只有碰实体/方块才结算，100 tick 到期直接丢弃，无 3 格近炸。

**证据入口。** 原包 [sorcerer/attacks/singularitycharge/__generated__/block/63.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/singularitycharge/__generated__/block/63.mcfunction)、[sorcerer/attacks/singularitycharge/hit.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/singularitycharge/hit.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 单独定义追踪策略与到期策略，统一进入幂等 explode(reason)；按最近玩家脚部/源距离更新方向，近炸检查不能用实际弹体碰撞替代。保留半径 5、源伤害和 D28 命中效果，不因名字 Singularity 给它添加原包没有的吸引力。反射后按原规则继续，而不是通用 HOMING 自动锁回旧目标。

**验收。** 在 2.99/3.01 格、移动目标、墙前、空旷 100 tick 三类条件测爆炸次数与范围；永远至多爆一次，超时不得静默消失，Forbiddance 清除则不冒充自然爆炸。

## D32 — Sorcerer：Halobeam / Void Beam 的锁定与射线交互（S3）

**代码对照。** 原包：Halo 有各自年龄与停止瞄准时点，射线步进至长度上限/阻挡，并支持 reflector；Void Beam 用独立 marker 平滑转向。Halo 自身可被反制。 模组：Halo 每次开火瞄实时目标，射线还截到目标距离；Void Beam 每帧直接指向目标。两种射线没有源反射分支，halo 数组节点无法被 Forbiddance 消除。

**证据入口。** 原包 [sorcerer/attacks/halobeam/subtick.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/halobeam/subtick.mcfunction)、[sorcerer/attacks/halobeam/raycast.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/halobeam/raycast.mcfunction)、[sorcerer/attacks/voidbeam/__generated__/block/132.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidbeam/__generated__/block/132.mcfunction)、[sorcerer/attacks/voidbeam/raycast.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidbeam/raycast.mcfunction)、[spells/__generated__/block/61.mcfunction](_nyctocosm_source/datapack/data/spells/functions/__generated__/block/61.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 维护八个 Halo 的 birthAge/aim/locked/fired/countered 和 VoidBeam 的独立发射点/朝向。按源 Halo 自身 65/70 计分界限锁定并发射，不把错开出生的节点同步开火；射线完整走 60 个 0.5 步的检测序列，目标近不缩短射程。抽取带反射的射线采样器和可反制节点接口，记录所有权与容量；D15/D16 共用但不混淆各招的标签。

**验收。** 锁定后最后几tick横移可躲Halo；射线命中第一个玩家就终止，后方第二玩家不被同射线贯穿。测八个错开时点、源双dx=0选箱边缘、Void Beam转向延迟、反射与Halo被反制后只取消该节点。

**实施核对（2026-10-02）。** 出生即score1，Halo首发74/末发109；Void aim无limit，多玩家创建多个marker，block132的execute forks均从进入rotation计算。原版命令/selector夹具验证并修正累计turn和单选箱误读；242/242 required回归通过。详细参数/源数值helper见[D32报告](nyctocosm-native/docs/validation/D32_sorcerer_beams.md)，真实客户端/多人/光影/卸载重启待验。

## D33 — Sorcerer：Dash Strike / Air Dash 锁点与位移（S3）

**代码对照。** 原包：Dash Strike先放前方落点marker，结束回到该点；Air Dash保存目标点并逐帧tp，40帧slam。接近分支写aj.frame39，但新版ajsorcererm2只读local_anim_time/aj.anim_time，该写入不能提前跳slam。旧模组dashToward追实时玩家并用物理速度，扩大持续伤害窗口。

**证据入口。** 原包 [sorcerer/attacks/dashstrike/aim.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/dashstrike/aim.mcfunction)、[sorcerer/attacks/dashstrike/enddash.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/dashstrike/enddash.mcfunction)、[sorcerer/attacks/airdash/aim.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/airdash/aim.mcfunction)、[sorcerer/attacks/airdash/dashtotarget.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/airdash/dashtotarget.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SorcererTimeline.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SorcererTimeline.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** AIM保存世界坐标/朝向/净空，后续不跟随玩家重算。Dash37锁点、41..49根tp1.5、49到点、50无移动；Air1..19总升7.5、20锁点、26..39根tp1.5、40正常slam。源码tp后旧命令context仍在进入位置：Air40半径4结算在进入坐标；Dash49另有最终点1.5命中。不加未被实际动画读取的提前跳帧。安全搜索有界，保存锁点/finished/gravity，纳入D17。

**验收。** 玩家在瞄准后侧移，Boss 应冲向旧点；比较源每帧位置与结束位置，而非只看总位移。高低地形、途中墙、早到终点、中途保存、玩家换目标均不二次 slam。

**实施核对（2026-10-02）。** 六项D33通过，预期坐标独立由原版facing/tp生成，保留1e-6边界。既有Celestial Smite共享场地早停改独立arena复验，源终止选择器包含旁观者，加载由ticket承担；最终全套248/248 required通过，alpha.15 build/reobf成功，两个独立新世界客户端截图通过。逐项参数见[D33报告](nyctocosm-native/docs/validation/D33_sorcerer_dash.md)。D28状态待选择、真实生存/多人/重启/光影验收待完成。

## D34 — Sorcerer：Seeking Singularities 追踪与穿墙（S2）

**代码对照。** 原包：8 枚，70–89 tick、每 tick 0.24；每枚找 50 格内最近非旁观玩家眼部并按命令转向；subtick 没有方块终止检查。 模组：数量/速度/寿命已有，但锁定原目标、向身体中心混合转向；通用 ProjectileUtil 会撞墙，命中筛选也受锁定 UUID 限制。

**证据入口。** 原包 [sorcerer/attacks/seekingsingularities/summon.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/seekingsingularities/summon.mcfunction)、[sorcerer/attacks/seekingsingularities/subtick.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/seekingsingularities/subtick.mcfunction)、[sorcerer/attacks/seekingsingularities/__generated__/block/146.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/seekingsingularities/__generated__/block/146.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 为此招定义动态最近玩家、眼部瞄准、源局部坐标转向与 IgnoreBlocks 碰撞策略，不能简单启用 D16 passthrough（源码此处连该标签也不查）。保留 8 枚和已有寿命范围；伤害按源小范围选择器，状态归 D28。反射和允许命中者按当前弹体状态处理，不让旧 homingTarget 永久排除其他玩家。

**验收。** 两人交叉移动应切换最近目标；实心墙两边验证穿行、墙后玩家仍可受击，过期清理不伤人。对照 0.24 位移与急转轨迹，反射后不得立即错误重锁。

**实施进展（2026-09-30）。** `SpellBolt` 按原命令方向链实现每 tick 最近目标（含精确 50 格）、眼部向量、0.24 前进与无方块终止；命中选择器按源玩家盒触发 Void 与用户确认的 Slowness II（即使伤害被拒），倒计时清理不爆炸。寻星弹独立三色粒子与 Gravitance hum；反射清目标并保留 IgnoreBlocks，且空结果不会使通用弹道碰撞空指针。7 项 D34 GameTest 全通过，完整 192 项通过 189 项、3 项其他攻击测试失败；build/audit 通过。D22 伤害记账、client 视觉/声音、真实区块续接仍未验收，D34 维持自动验证通过·待实机，详见 [阶段报告](nyctocosm-native/docs/validation/D34_seeking_singularities.md)。

## D35 — Sorcerer：Ice Spikes 波前、命中高度与恢复（S2）

**代码对照。** 原包：30 个径向波前从小半径开始，首次推进即生刺，之后每 5 tick；刺从地下升起，6–10 tick 按根位置检查半径 2。 模组：同样 30 路，但第 5 tick 才生第一圈，出生半径不同；刺的视觉/命中高度偏高，地表扫描范围不同；只保存 Owner，年龄依赖 tickCount。

**证据入口。** 原包 [sorcerer/attacks/icespikes/__generated__/block/48.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/icespikes/__generated__/block/48.mcfunction)、[sorcerer/attacks/icespikes/__generated__/block/51.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/icespikes/__generated__/block/51.mcfunction)、[sorcerer/attacks/icespikes/__generated__/block/52.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/icespikes/__generated__/block/52.mcfunction)、[sorcerer/attacks/icespikes/__generated__/block/59.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/attacks/icespikes/__generated__/block/59.mcfunction)；模组 [IceSpikeWave.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/IceSpikeWave.java)、[NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 独立 WaveClock 和 SpikeRecord（arm、birthAge、地表点、上升/伤害/下降进度）。复现源首次生成相位与每 5 tick 波前，分别计算根高度和可视模型高度；地表扫描使用世界高度界限和已验证候选，不无界递归。保存绝对进度与地表锚点，同步给晚加入客户端；年龄不能只用新建实体 tickCount。

**验收。** 对照首次推进及第 5/6/10/11/25 tick 的圈数、半径，站立/跳跃/上下层命中边界；在已有圈损伤完毕后卸载再载入不得重生内圈或重复伤害。晚加入客户端只看剩余阶段。

**实施进展（2026-09-30）。** 已沿 leaf_40、独立 `subtick`、block51→52、block48→50 和 `hit` 核对事件顺序。运行时 GameTest 确认无分数的首次 `scoreboard players remove` 生成 -1；因此生成年龄为 1/6/11/16/21，半径为 .9/3.4/5.9/8.4/10.9。原生改为保存 WaveAge、yaw 和每圈每臂地表锚点，根高度/命中窗/视觉高度分别计算，并按 `@a` 玩家脚部球形距离受击。四项 D35 GameTest 通过；全套 182 项有两项 Flame Geysers 既有失败，构建和 alpha.12 包审计通过。客户端晚加入与真实区块卸载/专服重启仍未验证，D35 保持实施中，详见 [验证记录](nyctocosm-native/docs/validation/D35_ice_spikes.md)。

## D36 — Sorcerer：Void Wave / Javelin Swing 命中原点（S2）

**代码对照。** 原包：Void Wave 在根 +世界 Y2 处取半径 6；Javelin 在局部 ^ ^1 ^2 处取半径 3，距离选择器按目标脚部。 模组：分别用根位置、朝目标中心方向前 2 格做 Combat.area；按包围盒距离，且额外传入推力。

**证据入口。** 原包 [animated_java/ajsorcererm2/zzzzzzzz/animations/voidwave/tree/leaf_38.mcfunction](_nyctocosm_source/datapack/data/animated_java/functions/ajsorcererm2/zzzzzzzz/animations/voidwave/tree/leaf_38.mcfunction)、[animated_java/ajsorcererm2/zzzzzzzz/animations/javelinswing/tree/leaf_35.mcfunction](_nyctocosm_source/datapack/data/animated_java/functions/ajsorcererm2/zzzzzzzz/animations/javelinswing/tree/leaf_35.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 给伤害事件明确 SourceFrame（世界偏移/局部偏移）与 FeetSphere 规则。渲染粒子位置不得代替命中原点，删除无源依据的额外固定推力或作为经审查的原生化选项，普通伤害的引擎击退仍保留。D28 负责命中者状态，不改已对齐的 38/35 帧。

**验收。** 目标分别站在 Boss 同高、低两格、高两格，以及水平半径边缘；比较原脚部球形选中集合。以目标在左右/后方的夹具检查局部坐标，不能仅平地正面测一次。

**实施进展（2026-10-01）。** Void Wave tick38 的命中原点改为根部世界 Y+2，Javelin Swing tick35 按 vanilla `LocalCoordinates` 的 yaw/pitch 基向量还原 `^ ^1 ^2`。两者现走玩家脚部闭边界球形筛选、保留 Forge 伤害路径，删除源函数没有的固定推力；Sweep 与 Void Wave 直接爆发粒子回到对应源原点。修复前两个范围反例按预期失败，修复后 `sorcerer_hit_origins` 两项全绿；完整197项为194通过、3项既有 Flame Geysers/Serpent 回归失败。独立 build 与 alpha.12 JAR 审计通过。D28 命中状态及客户端/多人实机仍待验收，详情见 [D36 阶段报告](nyctocosm-native/docs/validation/D36_sorcerer_melee_origins.md)。

## D37 — Nullpoint 受伤及 Sorcerer 死亡提交时机（S2）

**代码对照。** 原包：staff hitbox 以实际 Health 差额扣 Nullpoint；Sorcerer 血量归零还需等当前 attacking 结束才调用 defeat。 模组：Nullpoint.hurt 在 super.hurt 前以原始 amount>=health 强制回收/通知；术士本体没有像使者那样的攻击结束后致死提交。

**证据入口。** 原包 [sorcerer/ai/__generated__/block/176.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/ai/__generated__/block/176.mcfunction)、[sorcerer/ai/__generated__/block/178.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/ai/__generated__/block/178.mcfunction)、[sorcerer/ai/staffhurt.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/ai/staffhurt.mcfunction)、[sorcerer/ai/subtick.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/ai/subtick.mcfunction)；模组 [NullpointStaff.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NullpointStaff.java)、[NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 先让伤害进入 Forge 事件、无敌帧、护甲/抗性与吸收流程，再基于实际扣血提交杖破坏。借鉴已存在的使者延迟死亡机制，但为术士定义独立 PendingDefeat/已通知状态：不能在事件取消后破杖，也不能直接把使者四阶段阈值复制过来。回收、转阶段、死亡和奖励各幂等，保存待提交状态。

**验收。** 取消一次名义致死伤害、给杖抗性/吸收、连续伤害碰无敌帧，均不得提前破杖；实际致死只通知一次。术士正在招式中归零时完成源要求的窗口再死亡；保存/恢复不重复回收、转阶段或奖励。

## D38 — Serpent / Sorcerer 索敌门槛与离场（S3）

**代码对照。** 原包：蛇在 130 格内触发选招，攻击开始时再从 200 格内随机选人，均不依赖视线；术士各阶段攻击选择半径不超过 50 格的非旁观玩家。蛇/术士分别在 500/100 格内没有任何玩家时进入移除链。模组：Serpent 的 FOLLOW_RANGE=200 与 130 格选招门槛已对齐，但 vanilla `NearestAttackableTargetGoal` 的选人条件仍要求视线；Sorcerer 的范围原为默认 80，且选人也要求视线。两 Boss 均持久化，之前未按源距离清理。

**证据入口。** 原包 [serpent/headai.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/headai.mcfunction)、[serpent/__generated__/block/193.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/193.mcfunction)、[serpent/__generated__/block/6.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/6.mcfunction)、[sorcerer/ai/entitytick.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/ai/entitytick.mcfunction)、[sorcerer/ai/subtick.mcfunction](_nyctocosm_source/datapack/data/sorcerer/functions/ai/subtick.mcfunction)、[serpent/__generated__/block/7.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/7.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[SerpentMotion.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SerpentMotion.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案/实施。** 按源行为保留 Serpent 200 格追踪范围及 `SerpentMotion` 的 130 格计时门槛；Serpent/Sorcerer 的目标条件关闭视线检测，普通 Boss 保持原规则；Sorcerer `FOLLOW_RANGE` 收到其攻击选择上限 50。Boss 每 tick 先按源的 500/100 格范围检查当前维度全部玩家；无人时仅清理该 Boss UUID 拥有的弹体、范围法术、冰波、Nullpoint 与本地法阵状态，再移除 bossbar 与 Boss，不跨遭遇清理。客户端音乐控制器随 Boss 移除停止播放。无需旧包迁移。

**自动验证完成，客户端验收待做。** GameTest 覆盖 Serpent 130 格攻击门槛、120 格隔墙索敌，Sorcerer 40 格隔墙索敌与 55 格不选人；两 Boss 的 500/100 格离场边界、拥有物清理和另一场 Boss/弹体保留。完整服务器套件通过 137 项。仍需真实客户端检查地下/隔墙战斗、客户端音乐停止、低模拟距离及多人区块加载。

## D39 — Death Walker 追逐、地形与砸击区域（S3）

**代码对照。** 原包：优先筛生存玩家；行走是命令推进/探测台阶和前方落差。slam 第 50 帧以怪物根为圆心取脚部 10 格，前方 3 格只是爆炸粒子。 模组：先选最近玩家再排除创造/旁观，可能遮蔽更远生存玩家；追逐用原版导航。伤害也移到前方 3 格并加推力。

**证据入口。** 原包 [nyctocosm/mobs/death_walker/death_walker_tick.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/death_walker/death_walker_tick.mcfunction)、[nyctocosm/mobs/death_walker/slam.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/death_walker/slam.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 先筛合法生存目标再求最近值；提取源台阶/悬崖规则作为 WalkerMovementProfile，保留有界碰撞和服务器权威，明确导航可用的范围。伤害圆心回到根，粒子维持前 3 格，脚部球形筛选；伤害类型归 D47、读档归 D17。闲逛时长和已对齐的 91 tick/第 50 帧无需推倒重写。

**验收。** 创造玩家贴身、生存玩家 20 格时仍追生存玩家；测台阶、五格前悬崖及绕障。Boss 后方 9 格、前方 12 格分别检验不漏打/不超范围；砸击前后保存不跳过或重复伤害。

## D40 — Nightmare Apparition 出生、警戒与多人接触（S3）

**代码对照。** 原包：替换出生点上方 10 格生成；固定随机浮动周期，附近任意满足条件的生存玩家可惊动，警戒后换模型，接触范围内所有玩家中招。 模组：在原替换 Y 生成；浮动周期不同。只检查最近玩家，缺少近距互斥；仅伤一个最近玩家，客户端始终绘制普通模型。

**证据入口。** 原包 [nyctocosm/mobs/nightmare_apparition/summon.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/nightmare_apparition/summon.mcfunction)、[nyctocosm/mobs/nightmare_apparition/nightmare_apparition_tick.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/nightmare_apparition/nightmare_apparition_tick.mcfunction)、[nyctocosm/mobs/nightmare_apparition/__generated__/block/20.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/nightmare_apparition/__generated__/block/20.mcfunction)、[nyctocosm/mobs/nightmare_apparition/__generated__/block/21.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/nightmare_apparition/__generated__/block/21.mcfunction)、[nyctocosm/mobs/nightmare_apparition/__generated__/block/23.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/nightmare_apparition/__generated__/block/23.mcfunction)；模组 [NativeEvents.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java)、[NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[client/ClientEvents.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/client/ClientEvents.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 把 spawnOffset、随机但固定的浮动半周期、接近/避让/受光退避、警戒/消散列成状态表；源行走统计与原生运动量阈值建立可测试映射，选取全部合法触发者而非先最近再过滤。警戒状态同步客户端并绑定已存在 attacking 模型。接触按该 tick 全部合法玩家快照结算，再消散一次；保留创造/友伤保护的设计边界。

**验收。** 自然替换时记录原坐标和 +10Y 位置并做净空拒绝；潜行、慢走、冲刺、旁有创造玩家、两个生存玩家同时接触均有用例。原/警戒模型可见切换，重连保持状态；多只近距离不无条件堆叠，浮动周期按源范围。

## D41 — Scythe 穿越、落点、速度与冷却（S3）

**代码对照。** 原包：dash 仅将 impenetrable 标签当阻挡；沿步进路径传送并伤害，给自己 1 秒速度，入口无独立 dash 冷却。 模组：遇任何实体物理碰撞方块停止，改用眼部直线伤害；不给速度，并统一加 20 tick 冷却。

**证据入口。** 原包 [items/scythe/dash.mcfunction](_nyctocosm_source/datapack/data/items/functions/scythe/dash.mcfunction)、[items/scythe/raycast.mcfunction](_nyctocosm_source/datapack/data/items/functions/scythe/raycast.mcfunction)、[items/__generated__/block/14.mcfunction](_nyctocosm_source/datapack/data/items/functions/__generated__/block/14.mcfunction)、[nyctocosm/tags/blocks/impenetrable.json](_nyctocosm_source/datapack/data/nyctocosm/tags/blocks/impenetrable.json)；模组 [WeaponItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 将可穿越路径与最终可站立落点分离：普通方块可穿、bedrock/barrier 等 impenetrable 不可穿；最终要求完整净空并有界回退，不能按原包潜在卡墙结果硬传。源递归的 7 次计数不等于必然前进 7 格，逐步导出位置与每目标一次伤害；恢复速度状态，并审查去掉额外冷却。4 点自损继续经过原生伤害管线。

**验收。** 薄普通墙可穿到安全另一侧，厚墙无安全终点则拒绝/回退，基岩与屏障始终阻断；对比每步受击集合、自损、速度、连用频率。安全落点是 C02 保留项，失败是否仍扣自损在实施前冻结规则。

## D42 — Wyrmsliver 波面、运动与状态效果（S3）

**代码对照。** 原包：地面横向/空中纵向弧形切面，逐 tick 前进 1，50 tick 清理；施用者减速 1 秒，命中者致盲 5 秒。 模组：速度 1.8、半径 0.8 的通用弹，默认寿命 100；条状粒子不决定命中面，也无上述双方效果。

**证据入口。** 原包 [items/wyrmsliver/slash.mcfunction](_nyctocosm_source/datapack/data/items/functions/wyrmsliver/slash.mcfunction)、[items/wyrmsliver/slashhitbox.mcfunction](_nyctocosm_source/datapack/data/items/functions/wyrmsliver/slashhitbox.mcfunction)、[items/wyrmsliver/__generated__/block/39.mcfunction](_nyctocosm_source/datapack/data/items/functions/wyrmsliver/__generated__/block/39.mcfunction)、[items/__generated__/block/5.mcfunction](_nyctocosm_source/datapack/data/items/functions/__generated__/block/5.mcfunction)、[items/__generated__/block/7.mcfunction](_nyctocosm_source/datapack/data/items/functions/__generated__/block/7.mcfunction)、[items/__generated__/block/8.mcfunction](_nyctocosm_source/datapack/data/items/functions/__generated__/block/8.mcfunction)；模组 [WeaponItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 专用 SlashWave 保存 grounded、朝向、age、已命中集合，沿源弧线样点检测阻挡和实体，并在源命中/终止时结束；伤害 14 Nightmare 保留。所有权、反射能力只按源可达分支确定，不因为用了通用 SpellBolt 就额外获得能力。分别补施用者减速与真正命中者致盲，客户端显示同一波面。

**验收。** 站立和跳跃各发一刀，同一组左右/上方靶标命中集合不同；逐 tick 位置和 50 tick 清理一致。施用后减速、擦过未命中者不致盲、真正命中者致盲；主/副手入口归 D46。

## D43 — Nullfissure 双模式采样与副手反射（S3）

**代码对照。** 原包：主手按地面/空中弧线采样并补近身覆盖，命中减速；副手从眼前 2 格开始步进，按 passthrough 阻挡并可反射。 模组：主手是前向 4 格锥体；副手是从眼睛开始、宽 0.7 的物理碰撞直线，无反射。主手命中减速缺失。

**证据入口。** 原包 [items/nullfissure/swing.mcfunction](_nyctocosm_source/datapack/data/items/functions/nullfissure/swing.mcfunction)、[items/nullfissure/swinghit.mcfunction](_nyctocosm_source/datapack/data/items/functions/nullfissure/swinghit.mcfunction)、[items/nullfissure/cast.mcfunction](_nyctocosm_source/datapack/data/items/functions/nullfissure/cast.mcfunction)、[items/nullfissure/raycast.mcfunction](_nyctocosm_source/datapack/data/items/functions/nullfissure/raycast.mcfunction)；模组 [WeaponItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 区分 SweepSampleSet 与 RaySampleSet：主手恢复局部弧线及近身补偿，不把已有 WeaponEffects 视觉当伤害实现；副手复用 D32 的步进/反射框架但使用本武器参数、原点与 owner。主手减速及施用动作状态独立补齐。主手 generic、副手 Void 的类型修复归 D47，双手计时归 D46。

**验收。** 地面/空中、贴身后侧、范围边缘及不同朝向比较命中集合；副手射线从正确偏移开始，源 40 次采样覆盖与墙/雪层/反射一致。主手命中者获得 10 秒减速，副手不误套该状态。

## D44 — Semblance of Doom 副手火弹及燃烧（S3）

**代码对照。** 原包：每 tick 三个 1 格子步，50 tick 到期；对实际标记命中者伤害并给 Fire:100，终止分支在空气中放置火方块。 模组：1.1 速度、100 tick、半径 2 的普通 DAMAGE 弹；weaponVisual(3) 只控制外观，没有 FIRE 效果，既不点燃受害者也不放火。

**证据入口。** 原包 [items/semblanceofdoom/shoot.mcfunction](_nyctocosm_source/datapack/data/items/functions/semblanceofdoom/shoot.mcfunction)、[items/semblanceofdoom/slowcast.mcfunction](_nyctocosm_source/datapack/data/items/functions/semblanceofdoom/slowcast.mcfunction)、[items/semblanceofdoom/__generated__/block/80.mcfunction](_nyctocosm_source/datapack/data/items/functions/semblanceofdoom/__generated__/block/80.mcfunction)、[items/__generated__/block/9.mcfunction](_nyctocosm_source/datapack/data/items/functions/__generated__/block/9.mcfunction)、[items/__generated__/block/11.mcfunction](_nyctocosm_source/datapack/data/items/functions/__generated__/block/11.mcfunction)；模组 [WeaponItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)、[SpellBolt.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 独立 DoomFireball 的三子步和终止原因，按源小采样标记目标而非额外半径 2 群伤；命中给 100 tick 燃烧。恢复终止视觉与反射 D15。世界放火与燃烧实体分开：前者是会改地形的设计项，遵守服务器规则/保护事件并经用户审查后决定默认，不自动允许绕过保护插件。

**验收。** 空中每 tick 3 格、50 tick 到期；近弹但没进入源采样者不被误伤，命中者燃烧 5 秒。不同 FireTick/保护取消设置、反射和多人测试；即使选择不放火也要在验收报告明确它是保留差异，而非声称完全一致。

## D45 — Semblance of Doom 主手横斩/纵斩命中面（S2）

**代码对照。** 原包：grounded 与空中分别以 Y1.5/Y0.9 为原点做半径 2 的局部圆周样点，收集目标后每目标结算一次。 模组：服务端统一前向 4 格锥体；地面/空中视觉已有区别，但命中条件不随之变化。

**证据入口。** 原包 [items/semblanceofdoom/swing.mcfunction](_nyctocosm_source/datapack/data/items/functions/semblanceofdoom/swing.mcfunction)、[items/semblanceofdoom/__generated__/block/62.mcfunction](_nyctocosm_source/datapack/data/items/functions/semblanceofdoom/__generated__/block/62.mcfunction)、[items/semblanceofdoom/__generated__/block/63.mcfunction](_nyctocosm_source/datapack/data/items/functions/semblanceofdoom/__generated__/block/63.mcfunction)、[items/semblanceofdoom/swing_hitbox.mcfunction](_nyctocosm_source/datapack/data/items/functions/semblanceofdoom/swing_hitbox.mcfunction)；模组 [WeaponItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)、[WeaponEffects.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponEffects.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 复用 D43 的局部采样能力但使用本武器自己的原点、角度及半径。保持 19 Dissonance 和已有主手 5 秒燃烧；先采集 UUID 集再结算，避免多个样点造成重复伤害。绘制调试采样轮廓供验收，不改变普通左键攻击属性。

**验收。** 在水平两侧/身后、上方、前方 3–4 格布靶，对照站立和腾空两套命中集合；视觉扫到而不在伤害面与视觉外却被打中的问题应有对应边界断言。

**实施进展（2026-10-01）。** Semblance 主手改为复用视觉轮廓对应的 72 个 `i*5` 半径 2 样点；grounded 使用 local left/forward、空中使用 yaw/pitch 旋转 local up/forward。每样点使用 1 格实体选择盒，候选按原包 `livingthings` 类型标签和 UUID 去重后每目标仅尝试一次 19 Dissonance；依用户 D45 确认，无论 Forge 是否接受伤害，选择目标随后均燃烧 5 秒。两项测试先红后绿，含 Armor Stand 标签排除；全套199项中193项通过，6 项失败为两个 Wing Blast、两个 Serpent 与两个 Flame Geysers；独立构建和 alpha.12 审计通过。真实多人客户端待验，详见 [D45 验证记录](nyctocosm-native/docs/validation/D45_semblance_swing_samples.md)。

## D46 — 武器主副手入口及独立冷却（S2）

**代码对照。** 原包：Scythe/Wyrmsliver 主手谓词触发；Nullfissure 与 Semblance 各有 swing/cast 两套独立计时。 模组：WeaponItem.use 也接受前两者副手；后两者使用按 Item 类型的统一冷却，主手技能会锁副手技能。

**证据入口。** 原包 [items/playertick.mcfunction](_nyctocosm_source/datapack/data/items/functions/playertick.mcfunction)、[items/predicates/scythe/holding_scythe.json](_nyctocosm_source/datapack/data/items/predicates/scythe/holding_scythe.json)、[items/predicates/wyrmsliver/holding_wyrmsliver.json](_nyctocosm_source/datapack/data/items/predicates/wyrmsliver/holding_wyrmsliver.json)；模组 [WeaponItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 建立服务器验证的每玩家 AbilityCooldown（武器种类+能力，而非物品实例），主副手技能分别倒计时；无源副手入口默认返回 PASS。客户端冷却提示分别显示，不能通过换同类物品/换槽绕过。Scythe 无额外冷却的恢复由 D41 决定，不与本项重复计数。

**验收。** 主手使用后立即切副手，与源允许的 swing→cast 组合一致；换槽、丢弃再拾取、两把同类武器、断线重连不能重置同一能力冷却。空手主手持副手 Scythe/Wyrmsliver 不额外产生技能。

**实施进展（2026-09-30）。** 原包 20/25/30 tick 计时已改为玩家持久数据中的能力级独立计数器；Nullfissure 主手 swing 与副手 cast、Semblance 主手 swing 与副手 shoot 可独立启动。Scythe/Wyrmsliver 副手返回 PASS。登录时恢复客户端提示，计时只随在线玩家 tick 推进。自动覆盖交叉冷却、计时递减、玩家数据复制和实际副手 use 返回；断线重连、真实客户端手持显示待验收。Scythe 的原生冷却差异属于 D41，未因 D46 改动。

## D47 — 普通打击被误归类为元素伤害（S2）

**代码对照。** 原包：蛇贴身碰撞、Death Walker slam、Nullfissure 主手分别走 damage_generic；后者副手才走 Void。 模组：前两项统一 NIGHTMARE，Nullfissure 主手统一 VOID，改变保护神器、吸收及元素伤害事件的触发。

**证据入口。** 原包 [serpent/__generated__/block/10.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/10.mcfunction)、[nyctocosm/mobs/death_walker/__generated__/block/37.mcfunction](_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/death_walker/__generated__/block/37.mcfunction)、[items/nullfissure/__generated__/block/46.mcfunction](_nyctocosm_source/datapack/data/items/functions/nullfissure/__generated__/block/46.mcfunction)、[items/nullfissure/__generated__/block/54.mcfunction](_nyctocosm_source/datapack/data/items/functions/nullfissure/__generated__/block/54.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[WeaponItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/WeaponItem.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)、[NativeEvents.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 将 DamageKind 与六元素枚举分开，增加明确的 generic 原生伤害源和归属；逐个调用点使用源类型，别把 generic 随意映射成 Nightmare。元素保护/吸收只看真实元素，通用护甲/抗性/事件照常工作。不要根据粒子颜色或武器 lore 反推伤害元素。

**验收。** 无神器时基础 19.5/10、15、15.5 等值不因修类型而随意变；装备 Nightmare/Void 保护后 generic 打击不受对应元素保护，Nullfissure 副手仍受 Void 保护且可触发吸收。

## D48 — Serpent Laser 命中体积与受伤间隔（S2）

**代码对照。** 原包：沿射线每 1 格取脚部半径 3 的球形选择器，并要求 HurtTime=0 才造成 12 Nightmare。 模组：使用连续线段与放大目标 AABB 相交，无同等 HurtTime=0 前置条件；端点/边缘和可接受伤害时刻不同。

**证据入口。** 原包 [serpent/attack/laser/__generated__/while/4.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/attack/laser/__generated__/while/4.mcfunction)、[serpent/attack/laser/__generated__/block/208.mcfunction](_nyctocosm_source/datapack/data/serpent/functions/attack/laser/__generated__/block/208.mcfunction)；模组 [NativeBoss.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 保留已还原的固定朝向扫射运动；服务端使用原 150 次上限的样点序列、FeetSphere 与 hurtTime 条件，按源次序处理墙体/伤害。不要把其他柱状 hazard 已采用的正确脚部规则又统一改成 AABB；源接触伤害与 laser 的冷却条件分别定义。

**验收。** 在相邻样点的侧边/对角边界、头碰线脚不在球内、被其他强弱攻击后 0–20 tick 内逐 tick 测；比较可命中 tick 集合。总扫射 100 tick 与蛇运动不回归。

**实施进展（2026-09-30）。** 按 start→block205/206→raycast→while3/4→block208 追到完整调用；while4 每次先移 1 格，查询 `@a[distance=..3]`，最多150点，随后才检查终点方块；block208 要求 NBT `HurtTime=0` 才执行12点Nightmare伤害。`Combat.serpentLaser` 现在按整格样点、玩家脚位置球半径3与 hurtTime=0 逐点调用 Forge/Combat 流程；Serpent Laser 实际分支已切换到该路径，其他连续 beam 保持不变。三个差异 GameTest 均先对旧路径红、再绿。全套195项193通过，两个失败为 Flame Geysers；build/audit通过。真实攻击移动中逐tick目标集合与联机体验仍未验收，详见[阶段报告](nyctocosm-native/docs/validation/D48_serpent_laser_sampling.md)。

## D49 — 神器带自定义 NBT 后识别失败（S2）

**代码对照。** 原包：clear ...{artifact:1,具体标志:1} 0 使用局部 NBT 条件，不因改名称/增加其他字段而失去神器身份。 模组：hasArtifact 用 contains(new ItemStack(item))；本地同版本 Inventory.contains 调用 isSameItemSameTags，改名后的 stack 与无 NBT 新 stack 不等。

**证据入口。** 原包 [artifacts/playertick.mcfunction](_nyctocosm_source/datapack/data/artifacts/functions/playertick.mcfunction)；模组 [NativeEvents.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java)、[ArtifactItem.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/ArtifactItem.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 按注册 Item 身份/明确神器类型识别，忽略名称、Lore 等不参与能力身份的 NBT；扫描范围保持原生已支持的玩家背包/副手，不能擅自扩成潜影盒或外部饰品槽。统一所有 13 种神器调用此入口，同类重复携带仍是布尔持有，不叠成多份效果。这是新原生物品的改名问题，与旧包存档迁移无关。

**验收。** 每种神器原样、铁砧改名、恢复名字、附加非能力 NBT、副手持有各测一次；相同名称的普通物品不算神器，重复神器不重复充能/减伤。用本地 1.20.1 字节码确认 contains 语义，最终仍需游戏内铁砧用例。

## D50 — Absorption 与 Protection Void 组合运算（S2）

**代码对照。** 原包：弱点增伤和抵抗减伤均按同一基础伤害算出，再加减并截断非正结果。 模组：LivingHurt 先乘 1.2，再按增加后的值乘 (1−0.03×充能)，变成连续乘算；即使 power=0 也有差异。

**证据入口。** 原包 [artifacts/absorption/__generated__/block/7.mcfunction](_nyctocosm_source/datapack/data/artifacts/functions/absorption/__generated__/block/7.mcfunction)、[items/nullfissure/__generated__/block/55.mcfunction](_nyctocosm_source/datapack/data/items/functions/nullfissure/__generated__/block/55.mcfunction)、[artifacts/protection/subtick.mcfunction](_nyctocosm_source/datapack/data/artifacts/functions/protection/subtick.mcfunction)；模组 [NativeEvents.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 在进入明确的原生伤害阶段时保存基础值，分别求来源可达的 weakness/resistance，再按源整数精度计算 base+extra−less，最后进入后续护甲/吸收阶段；避免 Protection 和 Absorption 各自再次修改同一数值。普通增伤/减伤与通用第三方乘区需明确接口。此项基础组合即可复现，不能完全藏进 D22 非默认属性条件项。

**验收。** 例如输入 20、默认 20% 弱点、5 充能 15% 抵抗，源元素阶段为 21，当前为 20.4（均指护甲前这一阶段）。测 0/1/5/15 充能、两条 trace 重叠不叠弱点、两名吸收拥有者、退出范围及事件取消；并验证实际伤害后的吸血不重复。

## D51 — Expulsion 爆炸的目标距离边界（S2）

**代码对照。** 原包：在爆炸点按目标脚部 distance=..6 筛选；基础伤害 7 和击杀后延迟 15 tick 是另两项规则。 模组：基础值和延迟已有，但 Combat.area 按目标包围盒最近点距离 6 筛选，体型大的实体或边缘玩家会提前进入范围。

**证据入口。** 原包 [artifacts/expulsion/explosion.mcfunction](_nyctocosm_source/datapack/data/artifacts/functions/expulsion/explosion.mcfunction)、[artifacts/expulsion/subtick.mcfunction](_nyctocosm_source/datapack/data/artifacts/functions/expulsion/subtick.mcfunction)；模组 [ExpulsionBurst.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/ExpulsionBurst.java)、[Combat.java](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java)。以上均沿生成分支/实际调用继续核对，不以方法名认定效果。

**方案。** 给 Expulsion 显式 FeetSphere 查询策略，AABB 只做粗筛；保持源爆炸原点、归属、命中集合和连锁产生下一次延迟的规则。不是全局把所有 Combat.area 改成脚部球：D36/D39/D48 与本项按各自源选择器逐一接入。默认伤害 7 不改，属性扩展归 D22；多人全局计时加速的原包副作用按 C03 不照搬。

**验收。** 脚部在 5.99/6.01 格、大型实体身体进入但脚部在外、上下层目标分别测；一次连锁按各独立 15 tick 延迟推进，不因人数或多个样点倍增。已经完成的延迟/保存功能继续跑回归。

**实施进展（2026-10-01）。** Expulsion 从 `Combat.area` 切换为专用 `livingFeetSphere`：复用源 livingthings 类型标签，AABB只粗筛，按实体脚部位置执行三维半径6闭边界判定；7 Dissonance、伤害所有者、原点和零推力不变。没有全局改动 Combat.area；ExpulsionBurst 延迟/保存/15 tick 连锁路径保持原状。两个新范围反例先红后绿；201项完整回归中195项通过，6项为非D51的 Serpent、Flame Geysers、Emissary wing-part 和 Celestial Smite 失败。独立 build 和 alpha.12 JAR 审计通过。真实多人客户端待验，详见 [D51 验证记录](nyctocosm-native/docs/validation/D51_expulsion_feet_sphere.md)。

## V01–V04 — 不把未测范围当成已经确认的缺陷

- **V01 全流程复验：** 用隔离测试存档，双方同版本 alpha.12、同维度配置。覆盖冷区块古城地下入口、洞穴/山地/树冠/液体附近落点、低模拟距离、领域入场/一阶段悬空/二转三/胜利返回。记录源/目标 XYZ、缩放、实体净空、门和返程记录。复用现有 [入场验收说明](nyctocosm-native/docs/EMISSARY_ARRIVAL_BEHAVIOR.md)；旧测试通过只覆盖其所写断言。
- **V02 全 Boss 行为取样：** 本轮 Serpent 三招与 Sorcerer 十二招/六阶段已做核心调用链静态对照，确认项见 D26–D38/D47/D48，不再整体列成“还没看”。下一步固定目标与随机输入，记录 20 TPS 轨迹、命中和双端录屏；使者其余近战 locator/架势/抓取及全 Boss 跨招连锁仍需扩大取样。现有测试只作回归底线，先纠正与源不符的断言。
- **V03 多人/恢复：** 2–4 人、不同首杀资格、轮流死亡/断线/旁观/跨维度。记录每次伤害归属、奖励事务、模式/返程栈、boss/attack UUID。先审查“离线是否给奖励/是否暂停对白”等策略，再写断言；不能将未决定的策略包装为源码 bug。
- **V04 兼容/性能：** 建最小测试适配验证护甲/抗性/友伤/宠物和 Forge 事件取消，再加指定第三方模组，不宣称任意兼容。对高密度弹幕量、网络包量、长 tick、卸载清理、实体残留给出测量；降级只影响表现，服务器伤害保持确定性。

## C01–C04 — 排除范围与需审查的边界方案

- **C01 旧数据包存档迁移：已排除。** 按用户明确要求，不列入差异排名、不设计转换器、不扫描或修改旧世界。历史阶段 33 保留原记录，不代表该方案仍待执行。D17/D35/D37 的原生实体重载与 D49 的原生物品铁砧改名不属于旧包迁移，继续处理。
- **C02 安全传送：** 保留 [SafeTravel](nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SafeTravel.java) 的完整身体检查、有界搜索和短期票。如果要恢复原包 spreadplayers 的随机分布，先取原参数建立分布测试，再在可安全落地候选中实现；不牺牲已解决的地下卡死问题。
- **C03 原生化边界：** 保留实体/GUI/Forge 伤害管线，并以逐项效果验收保证还原。源全局 selector、永久 forceload、跨队串场和允许破坏地形不是默认必须复刻的功能；对冒险模式、放火、所有权差异形成单独决策记录。
- **C04 素材/性能：** 依据 [声音导入报告](nyctocosm-native/docs/sound-import.json) 区分原资源缺失与调用缺失。已有素材复用；新替代声音先由用户批准。粒子预算、声音受众、远距离广播分别配置，记录默认设置与原包视觉的差异。

## 建议实施批次（不是差异排序）

以下是按依赖汇总的参考分组，不是“先把 D01–D25 做完才轮到 D26–D51”的顺序。实际始终从差异清单最高未完成项开始，允许前置其最小依赖，并按实施补充记录依赖服务的目标。

1. 先复核并冻结每项源事件表，建立 D05 的独立回归；同步完成 D18 公共变换、D15/D16 碰撞/反射底座的最小切片。
2. D01 受击部位；D06–D12、D19 按单招完成纵向闭环：发射、移动、命中、反射、销毁、保存、显示。每招完成再合入下一招，避免一次改整场 Boss 却无法定位回归。
3. D02 入口/参与者与 D03 结局，配 V01/V03 完整战役验证。
4. D04 结构事件；D13/D14/D21/D22 法阵与资源机制；D17 恢复、D20 仪式、D23 Tony。
5. 扩展项：先 D27 部件解析、D38 索敌、D28 命中状态与 D47/D50 伤害语义；D26/D29–D36 按单招纵向完成；D37 受伤提交与 D17/D35 原生恢复贯穿每招。
6. D41–D46 四武器；D39/D40 普通怪；D49 神器身份和 D51 爆炸边界。可依靠共用采样器减少重复，但每个调用点独立验收。
7. D24/D25 音画收口，V02/V04 扩大取样；C02–C04 先获设计确认再实施相关破坏性改动，C01 不执行。

每完成一项，将实现文件、原包对照、失败→通过测试、尚未实测部分追加到 `MAINLINE_GAMEPLAY_PARITY_TODO.md` 尾部，同时更新独立清单该项状态。不要用“编译通过”代替行为验收，也不要覆盖历史结论。


## 2026-10-03 alpha.18 选中战斗切片补充

最新用户授权从完整boss对照中选择容易修复/体验影响大的部分；最终276/276隔离回归及客户端切片通过，详细行为、失败记录及实机边界见 [alpha.18报告](nyctocosm-native/docs/validation/BOSS_COMBAT_REPAIR_ALPHA18.md)。使者飞弹专用载体、转阶段/死亡与部分技能效果、巫师地面门槛/180tick死亡/法阵/杖粒子、蛇尾雾均有实际代码；D18/D25保留分项实施中，D37伤害/致死提交未闭合。原报告地面侧移.3尾段结论纠正：dash_skid不存在，不能从未调用的dash_slow推断可达动作。待选规则继续保留。协议6。


## 2026-10-04 alpha.19 三Boss反馈修复补充

本轮用户确认使者默认问题为出场后的悬浮形态。最终288/288 required回归、build/reobf及42张隔离客户端截图完成，详见 [alpha.19报告](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md)和TODO阶段88–90。

- D18：更正alpha.17误删源FlyTimer的结论，ai/subtick→736/737可达，20..59定时换路已恢复；保留通常不可达的@a自动加速分支判定。恢复>60无玩家的选招/AttackTimer、全部航点转向、起飞1..5升高、降落0..4下降及精确减速，phase4 marker无普通重力，Godrays升高保留；Rain维持玩家+30航点/10..44每帧四弹，源码没有最低高度门槛。96/128范围及防埋地夹紧保留。护身idle以源spawn75 arms/idle0 wings生成，只改客户端待机采样，属于用户要求的表现调整。
- D27关联客户端：头/身位置和角度统一BODY快照，去掉滞后实体头坐标拖动全链；服务端原41节追逐算法和17220命令采样保留。
- D38关联巫师反馈：抗击退1及kind1 knockback屏蔽，保留玩家伤害及技能位移；仅开场smear1按用户要求隐藏，法杖/战斗弧片保留。
- 自然AI免伤玩家夹具捕获三次高空Rain，共105发射帧，root高于玩家18.77..21.17格；随后手动高空场景单独标注。完整37招、原包/mod并排整场录像、真实自然生存/多人/保护模组/shader/重启卸载/晚加入/FPS仍待验收。未选玩法不因回归构建自动闭合。协议6，两端同版；本轮没有关机。
