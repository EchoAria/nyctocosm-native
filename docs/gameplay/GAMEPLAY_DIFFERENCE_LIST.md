# 原数据包 → Forge 模组：游戏表现差异清单

更新：2026-10-03。比对对象：`_nyctocosm_source` 与当前工作区 `nyctocosm-native`（alpha.17 开发版，包含既有未提交修改；历史阶段审计记录保留）。

本清单比较**代码执行后产生的行为**，不是比较文件数量、类名、动画资源是否导入。证据沿触发入口、生成的 mcfunction 分支、动画命令关键帧、实体 tick、伤害与退出路径核对。详细证据、重构方案和验收见 [GAMEPLAY_REFACTOR_PLAN.md](GAMEPLAY_REFACTOR_PLAN.md)。本轮复查覆盖表见 [EXTENDED_GAMEPLAY_AUDIT.md](EXTENDED_GAMEPLAY_AUDIT.md)：Serpent 三招、Sorcerer 十二招及六阶段/Nullpoint、两种普通怪、四武器主副手与 13 神器及关键组合。

当前实施阶段 D01、D04、D05、D07、D27、D38 已完成代码实现、自动回归和 alpha.12 发布包审计，仍待客户端实机验收；D02 已修正 Incurrence 的法阵位置上下文，模式子项按用户选择完成单人切片，组队入口和失败补偿仍在实施中并等待玩法选择；D46 已按原包计时器完成主副手独立冷却首片，真实客户端提示与断线重连体验待验收，Scythe 冷却仍由 D41 决定。D13 已区分固定/移动阵寿命，移动阵按创建类型跟随关联玩家并复制 yaw，固定阵不受持有变化影响；两种阵全员离场均清理，取消规则已按源分开。Dream power 加成管线、主人离线时完成施法的产物及实机行为仍未闭合。D14 已完成 17 个法术起手分支、Nightmare/Void 玩家命中分支及 trace 生命周期的首片修复；后续源审计确认 D06–D12 还有 Boss/临时实体发射点，但它们的 chance 分数没有可见的初始化，默认按 0 掷包含式骰会产生 1% 触发，等待用户选择后再接入；D22 非默认属性和其他逐招/武器分支也未闭合。D15 已建立 ReflectableAttack 共用协议并接入当前 SpellBolt 与 Emissary beam/Wingbeat/Wing Blast/Rain/Pillars/Flame Slash 分支：按源 3 格球形选择、扣减容量、转移 damage owner 并沿最近异主 reflector 朝向继续；默认容量、边界及同 tick 单节点耗能有 GameTest。D32 Halo/Void beam、D22 非默认 power 容量和客户端/多人验收未闭合。D17 已增加 Sorcerer 普通攻击 ID/年龄/剩余时间/目标与 HaloBeam 节点位置恢复，并续接 Death Walker slam 帧；自动 NBT 夹具通过，真实卸载/重启仍未验收。D13–D15、D17 均保持实施中；D16 已为审计过的 SpellBolt 分支接入源 passthrough/阻挡策略，自动验证通过待实机；D18 已扩展 16 个命令 locator 并统一服务端采样与客户端 root pitch，定位点切片自动验证通过待实机。D28 已移除开招时强制给锁定目标加状态，但实际命中效果待取消伤害策略确认；D26 已修正 Void Meteor 的发射数、计时与追踪弹道并通过自动回归，黑暗效果与反制验收待 D28/D16；D28、D26 也均保持实施中。D06 已建立独立巨剑、逐步下落/停驻和 105/110 tick 事件；世界火焰选择、generic 伤害扩展、shockwave 完整效果和客户端/生存视觉仍未验收。D07 已建立六路翼尖 marker、预警/扫射时间线、100 格 passthrough 射线、玩家终止/半径伤害、反射和 43 tick 清理，并通过完整 GameTest 与 alpha.12 包审计；客户端射线表现、真实战斗验收及 D15/D16 共用语义仍待闭合。D06、D07、D13、D14、D15、D16、D17、D18、D26、D28 均保持实施中。D03、D19–D25、D29–D37、D39–D51 尚未完成；D08–D12 仍是分项实施中。清单其他行继续代表静态代码审计结果，不是“已在客户端复现全部问题”，也不代表原包所有命令已经穷尽对照。

D19 已完成火球运动 profile、60 tick 终止、源选靶与双手发射时序；本次复查补上终端玩家脚部半径 5 格、debug/旁观排除，并移除通用 FIRE 弹带入的实体燃烧和额外推力。终端 3×3×3 放火等待用户决定，所以仍保持实施中。D02 补上流程最终结算时恢复所管理的模式，并阻止旧使者死亡动画期间开始新流程。最新构建与回归见 [alpha.13 项目复查](nyctocosm-native/docs/validation/PROJECT_RECHECK_ALPHA13.md)，各项状态仍区分已实现切片和整体验收。

## 排序规则

本轮另完成 D08 的首个独立切片：动画帧 33 创建云/火两种持久移动载体；云柱命中/反射/300 tick 生命周期已自动验证。火柱世界方块放置仍待 C03 玩法选择，D08 保持实施中，详见 [D08 阶段报告](nyctocosm-native/docs/validation/D08_pillars_cloud_flame.md)。D09 已接入双目标独立布场、每目标 14 个地表节点、随机延迟/预警/纵向命中和右手动画粒子 cue；完整 GameTest 与包审计通过。D09 的 shockwave 视觉仍是简化表现，原包沿空气写火仍等待 C03 选择，故 D09 保持实施中，详见 [D09 阶段报告](nyctocosm-native/docs/validation/D09_flame_geysers.md)。

按**差异大小**降序，同级再按影响范围与主线体验排序；不是按实现难度或预计工时排序。

- S4：整段流程/关联内容缺失，或整场战斗的交互模型不同。
- S3：一整招、一个持续机制或核心资源循环明显不同。
- S2：局部轨迹、边界、时序、恢复状态或有条件触发的规则不同。
- S1：主要是声音、音乐编排与表现事件差异。

目前共 **51 项**：保留 D01–D25，新增 D26–D51，并扩充 D17 的 Death Walker 恢复问题。旧数据包存档迁移按用户要求排除，不计入 51 项；原生存档读档仍在范围内。编号保持稳定，下面的“顺序”才是本次优先级。

所有 D 项均已找到原包和模组的具体代码依据；其中 D22 的影响依赖非默认属性值，不能推断普通新档所有法术数值都错。V 项只有验证缺口，不混入“确认缺失”排序。C02–C04 属于需要先决定的设计边界，C01 已排除，不自动视作应照搬的缺陷。

## 已确认差异，按大小排序

2026-10-03实机反馈修复：用户“二阶段地面近战”对应内部phase3，“三阶段高速飞行”对应phase4，纠正此前解释。持剑逐骨骼变体10显示/45隐藏并跨idle/NBT保存；巨剑移除非本招随机Rotation；近战护甲保留，171转场才armorless。飞行、Corkscrew、Divine/Godrays、焦点/灵魂粒子与MASTER声音、亮光和部分玩家弹道切片已修改；262/262回归、三轮隔离客户端、alpha.17包审计通过，详见[本轮交付](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA17.md)。完整51项、D22属性管线、炽光取消燃烧等待选项不据此完成；逐玩家法术剩余项见报告。

2026-10-02 客户端反馈补充：巨蛇按身体段独立可见性／光照绘制；使者二阶段按用户要求保留一阶段调色板、后期飞行96格回航／128格上限；巫师开场变体、传送门独立客户端粒子、死亡灵魂／消散、巨剑模型及蛇柱粒子预算已实际修改并有隔离客户端证据。D06/D18/D25/D27关联切片保持待用户战斗实机；不据此勾选整项。十二招逐项未闭合范围见 [alpha.14客户端修复](nyctocosm-native/docs/validation/VISUAL_REPAIR_ALPHA14.md)。D28全局取消伤害附带效果仍未批准。

每行“实施状态”是该 D 项的执行记录；D01、D04、D05、D07、D27、D38 自动验证通过、待实机；D02、D06–D15、D17、D26、D28、D46 有已实施切片但仍在实施中，其余项目待实施或待决策。按 [实施补充](GAMEPLAY_IMPLEMENTATION_GUIDE.md) 第 6 节区分实现、自动验证和实机验收，状态后附验证记录链接。分组复选框只有全部成员验收后才勾选。

| 顺序 / 编号 | 大小与差异项 | 原数据包代码产生的效果 | 当前模组代码产生的效果 | 实施状态 |
|---|---|---|---|---|
| 01 / D01 | S4：使者分阶段受击部位 | 一阶段四个翼侧/上翼 slime hitbox，后续阶段切换受击实体和位置。 | 已有四个随动画 locator 更新的 multipart 部位：一阶段四翼命中，二阶段 Giant，三/四阶段 Wither Skeleton；Size:4 代理已按原版实际 2.0808×2.0808 尺寸修正；投射物、范围和线性法术将伤害转给父 Boss。 | [自动验证通过待实机](nyctocosm-native/docs/validation/D01_emissary_hitboxes.md) |
| 02 / D02 | S4：召唤位置、多人入域及战场规则 | Incurrence 在法阵所在生物群系判断；使者入口把距入口 2 格内玩家送入领域，生存玩家转冒险。 | biome 与使者落点从法阵位置计算；安全单人入域后只把 Survival 切至 Adventure，最终结算/离开/死亡时仅恢复流程仍管理的模式；旧使者死亡动画结束前不开始新流程。仍未完成队伍入口、逐玩家返程与失败处理。 | [实施中：法阵上下文与模式切片](nyctocosm-native/docs/validation/D02_incurrence_context.md) |
| 03 / D03 | S4：使者首次击败结局 | 首次成就奖励启动约 1300 tick（65 秒）对白，分时播放、限制移动，末尾才显示逃离；重复击败走快捷出口。 | 发书、成就、标题后立即显示出口；没有接入首次结局对白状态机。**不是缺奖励或缺返程命令。** | 待实施 |
| 04 / D04 | S4：crypt → 地表 Slatehenge 关联生成 | crypt 末端 marker 触发地表结构放置，并用 200 格内标记防重；Slatehenge 标记还维持表现。 | 原生 marker 事件实体已替换两处已审计标签；触发器按海底高度图、200 格已加载实体范围和源结构池处理器放置 Slatehenge，特效实体复现持续粒子与空气清理。 | [自动验证通过待实机](nyctocosm-native/docs/validation/D04_crypt_slatehenge.md) |
| 05 / D27 | S3：Serpent：躯干受击体与原生法术命中 | 20个沿身体偶数段分布的Size:8 slime是livingthings，可被原包武器/法术选择器命中；原身体41节逐节追逐。 | 原multipart受击切片保留；alpha.19修客户端头/身网络双时钟拖动整链：统一BODY位置/角度快照，服务端追逐链及17220源采样对照不变。 | [受击及新增身体同步切片：验收范围见alpha.19](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md) |
| 06 / D38 | S3：Serpent / Sorcerer 索敌门槛与离场 | 蛇在130格内触发选招、开招从200格内选目标，无视线门槛；术士目标最多50格，两Boss分别500/100格无人移除；巫师模型marker独立于受击代理。 | 原索敌/离场及alpha.18地面≤7／8–20门槛保留；alpha.19补巫师抗击退1及kind1 knockback屏蔽，玩家冲刺/击退附魔仍正常造成伤害，自己的技能位移保留。开场smear按用户要求隐藏。 | [新增巫师反馈切片：验收范围见alpha.19](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md) |
| 07 / D28 | S3：Sorcerer：负面效果的施加对象与时机 | 多招在 hit 分支给真正命中者减速/黑暗；Shard Hail 另有地面节点近距减速。 | 开招时对锁定目标的强制减速/黑暗已移除；实际命中者的效果配置与 Hail 地面节点减速尚未接入，Forge 取消伤害时的附带效果规则等待用户确认。 | [实施中](nyctocosm-native/docs/validation/D28_D26_sorcerer_debuffs_and_meteor.md) |
| 08 / D26 | S3：Sorcerer：Void Meteor 数量、弹道与计时 | tear 激活后六个计分时点各发一枚，共 6 枚；从裂隙散射并追踪玩家，弹体 25 tick 进入爆炸分支。 | 激活计分器由 block109 与激活后的 block112 每世界 tick 各加 1，因此 20/40/60/80/100/120 计分点实际相隔 10 个世界 tick；现每 10 个世界 tick 各发一枚，共 6 枚。每 tick 三个 0.5 格子步进，追踪最近 50 格玩家，实块或 25 tick 触发 5 格玩家范围伤害。D28 黑暗效果与 D16 反制交互仍待完成。 | [实施中](nyctocosm-native/docs/validation/D28_D26_sorcerer_debuffs_and_meteor.md) |
| 09 / D05 | S3：恐惧系统影响到夜域之外 | 恐惧处理只对 `in_nyctocosm` 三个维度中非 luminous 的生存玩家调用。 | 恐惧逻辑已限制到 Nyctocosm、Dusk Plane、Emissary Domain 的 survival 玩家；其它维度保留 DARKNESS 和恐惧计时状态，Luminescence 仍按其独立效果清除 Darkness。 | [自动验证通过待实机](nyctocosm-native/docs/validation/D05_fear_scope.md) |
| 10 / D13 | S3：固定/移动法阵生命周期 | 固定/移动阵都在 3 格内无人时消失；移动阵默认 500 空闲 tick 加 Dream power 修正，施法期间不扣时。移动 tick 让 marker 跟随匹配玩家位置与 yaw、pitch 归零；取消施法只令最近移动阵寿命归零。 | 固定/移动类型按创建时确定；固定阵不跟随持有变化，移动阵跟随关联在线玩家、复制 yaw/marker pitch 0；施法暂停寿命，两种阵全员离场清理，取消时仅移动阵下次 tick 过期。Dream power 依赖 D22；主人离线后施法效果待源上下文厘清。 | [实施中；5 项 field_lifecycle 通过](nyctocosm-native/docs/validation/D13_field_lifecycle.md) |
| 11 / D14 | S3：Hypertrace 生成与空间行为 | 全 17 个法术 cast 开始各抽一次 global tracechance 并在朝向前方 3 格生成；Nightmare/Void 玩家攻击伤害分支另用元素+global chance 在受击者处生成。trace 每 tick 旋转 1 度、离开 1 格内其他 trace 0.2 格，400 tick 到期。 | 新增独立 service：全法术在起手抽样，Combat.hit 的玩家 Nightmare/Void 分支在伤害调用后抽样并保留攻击者归属/受击位置；15 分基础 inclusive roll、Accumulation 默认每个本人球形 6 格内 trace 加 10。trace 旋转、近距避让、400 tick、owner 离线继续、Transposition 不跟随已对齐；拾取/摧毁仍限本人并补多人断言。D22 非默认 power/element 属性加成及其它逐招/武器源触发仍待接入。 | [实施中](nyctocosm-native/docs/validation/D14_hypertrace_lifecycle.md) |
| 12 / D06 | S3：Sword Plunge 的攻击载体 | 在目标地表上方 220 格生成独立巨剑，前 200 子步每步 -1 格、后 50 子步每步 -0.2 格；每 tick 三子步，105 tick shockwave、110 tick 地表爆炸，最后在 21×21×7 空气范围放火。 | 独立实体现按地表 +220、3 子步、200/50 速度阶段和 105/110 计时运行，保存 one-shot 状态，generic 60 点伤害接入 Forge；源45帧手中剑隐藏并持久、巨剑无错误root倾斜，隔离客户端已见。放火待选择；完整shockwave与D14/D22仍未闭环。 | [实施中；alpha.17持剑/竖剑自动与客户端切片通过](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA17.md) |
| 13 / D07 | S3：Wing Blast 六路扫射 | 第 12 帧按六个 wingtip1 locator 生成 marker 并独立瞄向随机合格玩家；第 35 帧转为 blasting。每 tick 上/中/下翼按标签偏转 yaw/pitch，预警与主动射线均逐格检查 passthrough、最多 100 格；主动射线还检查玩家接触和 Reflectance，末端半径 4 爆炸，计时达到 43 清理。 | 新增六个有 owner/wing/age/active 保存状态的射线实体，从导出 locator 取点，逐 tick 扫描、按源偏转和时序结算；主动命中使用 radiance 40、hurtTime=0，反射消耗 Reflectance 储量并改向。 | [自动验证通过待实机；D15/D16 共用语义继续闭合](nyctocosm-native/docs/validation/D07_wing_blast_rays.md) |
| 14 / D08 | S3：Pillars 云柱/火柱 | 第 33 帧生成两个独立 marker（火/云分别从 boss yaw +60/-60 起步）；每 tick 寻最近合格玩家并以 0.6/0.4 格推进，最多 300 tick。云柱命中检查 hurtTime，给 2 秒失明与 Slowness III 并造成 Dissonance 伤害；火柱每 tick沿路对空气填火。 | 新增两种独立持久载体、寻敌/移动、粒子/声音、接触伤害与状态、Reflectance 转向、owner/kind/age 保存和 300 tick 清理。火柱当前不放置世界方块，等待 C03 选择。 | [云/载体自动验证通过；火焰规则待确认，实施中](nyctocosm-native/docs/validation/D08_pillars_cloud_flame.md) |
| 15 / D09 | S3：Flame Geysers 布场 | 最多选两名玩家，每名生成 14 个节点；`spreadplayers 5 25` 以 5 格最小节点间距、X/Z ±25 格方形区域地表散布；节点随机 20–149 tick，20 tick 处有音效，最后 40 tick 每 tick 预警后喷发。喷发沿 0–49 格纵向范围检查 5 格距离，并执行冲击波/粒子/放火效果。 | 第 40 帧随机选最多两名合格玩家并按组创建独立地表点；每组 14 个、5 格最小水平间距、半径25圆盘（源为方形，待对齐）、持久倒数/预警与 Dissonance 命中；右手 cue locator 和七个粒子帧已接入。组间不重散布。源冲击波视觉目前简化，世界火焰填充待 C03 选择；状态仍为实施中。 | [自动验证通过首片；实施中，待散布对齐、C03 与冲击波视觉](nyctocosm-native/docs/validation/D09_flame_geysers.md) |
| 16 / D10 | S3：Apocalypse Rain 弹幕形态 | fire 一次生成四个朝左右、上下分组的弹体；逐 tick 调整俯仰，每 tick 两次前进，60 tick 到期。 | 新增独立 RainShard，动画第 10–44 帧每帧四枚、从使者根部按源角度/扰动发射；顺序俯仰变化、两段子步、passthrough/玩家终止、反射与半径 5 Dissonance 冲击均已接入。基础伤害 12 走 Forge，但 D22 动态属性/统计链和更广义 D15/D16 语义仍未闭合。 | [实施中；四项 D10 GameTest 与 165/165 全套通过，待 D22/实机](nyctocosm-native/docs/validation/D10_apocalypse_rain.md) |
| 17 / D11 | S3：Celestial Smite 裂隙射击 | 起手同步执行一次 tear→circles→activate；动画第 26/50/93 帧再发一枚 tear。Raycast 最多检查 150 个位置，沿途 5 格玩家/阻挡方块结束射线，impact 后伤害终点 15 格内目标。 | 原生现已实现两次射击、持久 tear、raycast 和终点伤害基础片段；未写火方块（C03 未确认），D22 修正与源模型/beamwave 仍未闭合。 | 实施中；自动全套通过，待 C03/D22/视觉/实机 [报告](nyctocosm-native/docs/validation/D11_celestial_smite.md) |
| 18 / D12 | S3：Flame Slash 波面 | 第 2 帧 +60° 和第 13 帧 -60° 各生成波面；每 tick 朝最近目标追踪、步进 0.8，扫描半径 2 圆周 20 点，40 步后延迟 1 tick impact。 | 原生已移除五枚直线 FIRE 弹，接入两枚持久波面、表面采样、墙体/超时终止与基础 Dissonance 命中。源 facing 精确式、D22 和模型粒子仍待对齐。 | 实施中；三项 D12 GameTest 通过，完整源等价仍未闭合 [报告](nyctocosm-native/docs/validation/D12_flame_slash.md) |
| 19 / D29 | S3：Sorcerer：Shard Hail 独立落点、下坠与反制 | 冰球每tick升1格，timer30孵化18节点；节点score41依无limit的多人选择器散布，生成上方30格物理冰锥，落地score2半径2 Void10。 | 已改为独立Ball/Node/Icicle，源方形散布／函数进入坐标／物理／2与5落地计时／分阶段保存；仅Ball可反制。旧原生存档当前旧式wave兼容收尾保留。 | [实施中：232项回归通过，D28命中迟缓待选择／实机待验](nyctocosm-native/docs/validation/D29_shard_hail.md) |
| 20 / D30 | S3：Sorcerer：Frozen Blades 追踪波面 | 根坐标手骨显示实体以Y+3／yaw±90／pitch+20发射；每tick两次12点纵弧采样、一次.4移动、末次5/.5追踪转向。 | 已用双FrozenBlade独立载体，源Void14／脚部2格／块检查／14容量反射／Forbiddance／100tick清理／NBT；命令坐标与转向经原版夹具补正。 | [实施中：236项回归通过，D28迟缓待选择／实机待验](nyctocosm-native/docs/validation/D30_frozen_blades.md) |
| 21 / D31 | S3：Sorcerer：Singularity Charge 追踪、近炸与超时 | .3移动后从实体新位置按5/.5转向40格最近非旁观玩家脚部；函数进入位置检查3格近炸／passthrough／100tick，爆炸半径5；反射在检查后从函数位置重定位。 | 独立SINGULARITY_CHARGE profile已实现源44帧发射、移动／转向、三种爆炸、20容量反射、one-shot及原生NBT恢复，原版命令夹具确认进入位置不会随tp移动；源充能／reverse portal环已恢复。Slowness II 10秒等待D28确认。 | [实施中：弹道／终止自动验证通过，状态待选择](nyctocosm-native/docs/validation/D31_singularity_charge.md) |
| 22 / D32 | S3：Sorcerer：Halobeam / Void Beam 的锁定与射线交互 | Halo独立score65锁定/70发射（出生score1）；Void20帧每名玩家一个marker，30..57发射、58清理。双dx=0选箱，遇第一玩家或阻挡终止，最大60半格样点；反射，Halo可反制。 | 已接独立持久化节点、源多人execute fork转向、双选箱与first-player终止、反射10/3、客户端源粒子路径包；出生时点与旧“贯穿玩家”方案已更正。 | [自动验证242/242通过·待实机](nyctocosm-native/docs/validation/D32_sorcerer_beams.md) |
| 23 / D33 | S3：Sorcerer：Dash Strike / Air Dash 锁点与位移 | 固定世界点，根tp每帧1.5；Air升7.5、40正常slam。源aj.frame39不被新版动画读取；命中按函数进入坐标，Dash49另有最终点命中。 | 已保存固定点/yaw/finished/gravity，恢复逐帧根移动及源玩家脚部窗口，安全地表搜索保持有界；cue包还原进入粒子坐标与Air双dashfly2。 | [自动验证248/248通过，D28待选／实机待验](nyctocosm-native/docs/validation/D33_sorcerer_dash.md) |
| 24 / D44 | S3：Semblance of Doom 副手火弹及燃烧 | 每 tick 三个 1 格子步，50 tick 到期；对实际标记命中者伤害并给 Fire:100，终止分支在空气中放置火方块。 | 1.1 速度、100 tick、半径 2 的普通 DAMAGE 弹；weaponVisual(3) 只控制外观，没有 FIRE 效果，既不点燃受害者也不放火。 | 待实施 |
| 25 / D43 | S3：Nullfissure 双模式采样与副手反射 | 主手按地面/空中弧线采样并补近身覆盖，命中减速；副手从眼前 2 格开始步进，按 passthrough 阻挡并可反射。 | 主手是前向 4 格锥体；副手是从眼睛开始、宽 0.7 的物理碰撞直线，无反射。主手命中减速缺失。 | 待实施 |
| 26 / D42 | S3：Wyrmsliver 波面、运动与状态效果 | 地面横向/空中纵向弧形切面，逐 tick 前进 1，50 tick 清理；施用者减速 1 秒，命中者致盲 5 秒。 | 速度 1.8、半径 0.8 的通用弹，默认寿命 100；条状粒子不决定命中面，也无上述双方效果。 | 待实施 |
| 27 / D41 | S3：Scythe 穿越、落点、速度与冷却 | dash 仅将 impenetrable 标签当阻挡；沿步进路径传送并伤害，给自己 1 秒速度，入口无独立 dash 冷却。 | 遇任何实体物理碰撞方块停止，改用眼部直线伤害；不给速度，并统一加 20 tick 冷却。 | 待实施 |
| 28 / D39 | S3：Death Walker 追逐、地形与砸击区域 | 优先筛生存玩家；行走是命令推进/探测台阶和前方落差。slam 第 50 帧以怪物根为圆心取脚部 10 格，前方 3 格只是爆炸粒子。 | 先选最近玩家再排除创造/旁观，可能遮蔽更远生存玩家；追逐用原版导航。伤害也移到前方 3 格并加推力。 | 待实施 |
| 29 / D40 | S3：Nightmare Apparition 出生、警戒与多人接触 | 替换出生点上方 10 格生成；固定随机浮动周期，附近任意满足条件的生存玩家可惊动，警戒后换模型，接触范围内所有玩家中招。 | 在原替换 Y 生成；浮动周期不同。只检查最近玩家，缺少近距互斥；仅伤一个最近玩家，客户端始终绘制普通模型。 | 待实施 |
| 30 / D37 | S2：Nullpoint 受伤及 Sorcerer 死亡提交时机 | staff hitbox 以实际 Health 差额扣 Nullpoint；Sorcerer 血量归零还需等当前 attacking 结束才调用 defeat。 | Nullpoint.hurt 在 super.hurt 前以原始 amount>=health 强制回收/通知；术士本体没有像使者那样的攻击结束后致死提交。 | 待实施 |
| 31 / D49 | S2：神器带自定义 NBT 后识别失败 | clear ...{artifact:1,具体标志:1} 0 使用局部 NBT 条件，不因改名称/增加其他字段而失去神器身份。 | hasArtifact 用 contains(new ItemStack(item))；本地同版本 Inventory.contains 调用 isSameItemSameTags，改名后的 stack 与无 NBT 新 stack 不等。 | 待实施 |
| 32 / D47 | S2：普通打击被误归类为元素伤害 | 蛇贴身碰撞、Death Walker slam、Nullfissure 主手分别走 damage_generic；后者副手才走 Void。 | 前两项统一 NIGHTMARE，Nullfissure 主手统一 VOID，改变保护神器、吸收及元素伤害事件的触发。 | 待实施 |
| 33 / D50 | S2：Absorption 与 Protection Void 组合运算 | 弱点增伤和抵抗减伤均按同一基础伤害算出，再加减并截断非正结果。 | LivingHurt 先乘 1.2，再按增加后的值乘 (1−0.03×充能)，变成连续乘算；即使 power=0 也有差异。 | 待实施 |
| 34 / D17 | S2：Sorcerer / Death Walker 攻击读档 | 原活动攻击依赖持久的动画/计分状态。 | Sorcerer 活动攻击已按 ID、age、remaining、目标 UUID 和 HaloBeam 已生成节点位置恢复；Death Walker 的活动 slam 续接剩余帧。隔离专服重启确认 slam NBT 保留在实体 region 文件；玩家区块激活、客户端动画与逐招战斗仍未验收。 | [实施中](nyctocosm-native/docs/validation/D17_attack_restore.md) |
| 35 / D35 | S2：Sorcerer：Ice Spikes 波前、命中高度与恢复 | 30 个径向波前从小半径开始，首次推进即生刺，之后每 5 tick；刺从地下升起，6–10 tick 按根位置检查半径 2。 | 同样 30 路，但第 5 tick 才生第一圈，出生半径不同；刺的视觉/命中高度偏高，地表扫描范围不同；只保存 Owner，年龄依赖 tickCount。 | 实施中：[D35 验证记录](nyctocosm-native/docs/validation/D35_ice_spikes.md)；四项 D35 GameTest 与构建/审计通过，客户端晚加入及真实卸载/重载待验。 |
| 36 / D46 | S2：武器主副手入口及独立冷却 | Scythe/Wyrmsliver 主手谓词触发；Nullfissure 与 Semblance 各有 swing/cast 两套独立计时。 | 前两者副手入口返回 PASS；后两者按玩家和能力分别保存 20/25、30/30 tick 计时，切手、换同类物品不共用冷却。Scythe 的 20 tick 原生冷却暂留，待 D41 选择。 | [实施中：自动 GameTest、构建与包审计通过](nyctocosm-native/docs/validation/D46_weapon_hands_cooldowns.md)；客户端及真实断线重连待验 |
| 37 / D34 | S2：Sorcerer：Seeking Singularities 追踪与穿墙 | 8 枚，70–89 tick、每 tick 0.24；每枚找 50 格内最近非旁观玩家眼部并按命令转向；subtick 没有方块终止检查。 | 数量/速度/寿命已有，但锁定原目标、向身体中心混合转向；通用 ProjectileUtil 会撞墙，命中筛选也受锁定 UUID 限制。 | 自动验证通过·待实机：[D34 报告](nyctocosm-native/docs/validation/D34_seeking_singularities.md)；追踪/穿墙/期限/反射/迟缓均有 GameTest，动态伤害记账及客户端/区块重载待验 |
| 38 / D48 | S2：Serpent Laser 命中体积与受伤间隔 | 沿射线每 1 格取脚部半径 3 的球形选择器，并要求 HurtTime=0 才造成 12 Nightmare。 | 使用连续线段与放大目标 AABB 相交，无同等 HurtTime=0 前置条件；端点/边缘和可接受伤害时刻不同。 | 自动验证通过·待实机：[D48 报告](nyctocosm-native/docs/validation/D48_serpent_laser_sampling.md)；脚部半径/HurtTime/player-only 有回归测试，真实扫射全程待验 |
| 39 / D36 | S2：Sorcerer：Void Wave / Javelin Swing 命中原点 | Void Wave 在根 +世界 Y2 处取半径 6；Javelin 在局部 ^ ^1 ^2 处取半径 3，距离选择器按目标脚部。 | Void Wave 世界 Y+2 与 Javelin yaw/pitch 局部坐标现走玩家脚部球筛选和 Forge 伤害流程；无源固定推力已删。 | [自动验证通过·待实机](nyctocosm-native/docs/validation/D36_sorcerer_melee_origins.md)；旋转局部原点、内外脚部边界和仅玩家各有 GameTest；D28 状态另待处理 |
| 40 / D45 | S2：Semblance of Doom 主手横斩/纵斩命中面 | grounded 与空中分别以 Y1.5/Y0.9 为原点做半径 2 的局部圆周样点，收集目标后每目标结算一次。 | 主手按 onGround 选择对应局部圆面，72 点各取 1×1×1 样本，按源 livingthings 类型标签和 UUID 去重后每目标尝试一次 19 Dissonance，并在伤害尝试后无条件燃烧 5 秒。 | [自动验证通过·待实机](nyctocosm-native/docs/validation/D45_semblance_swing_samples.md)：ground/air、旋转、标签排除、环外和取消伤害仍燃烧均有 GameTest；多人客户端待验 |
| 41 / D51 | S2：Expulsion 爆炸的目标距离边界 | 在爆炸点按目标脚部 distance=..6 筛选；基础伤害 7 和击杀后延迟 15 tick 是另两项规则。 | Expulsion 使用独立 3D 脚部半径6筛选，源 livingthings 标签、7 Dissonance 与15 tick延迟链保留；通用 Combat.area 不变。 | [自动验证通过·待实机](nyctocosm-native/docs/validation/D51_expulsion_feet_sphere.md)：水平/垂直5.99与6.01、巨大实体脚部边界和延迟连锁均有 GameTest；多人客户端待验 |
| 42 / D15 | S2：Reflectance 出射方向/边界 | 源分支按 `distance=..3` 球形样点触发；改用反射器朝向、复制 owner，并按攻击 `TEMP s.damage` 扣容量。 | `ReflectableAttack` 共用协议让 SpellBolt、已实现的 Emissary beam/Wingbeat/Wing Blast/Rain/Pillars/Flame Slash 使用精确 3 格球体、排除同 owner redirect、按源 cost 消耗所有同样命中的节点并沿最近异主 shield 改方向和归属；Bolt 保速、清 homing、同 tick 不重复。默认20容量与正好耗尽已测。Sorcerer Halo/Void beam 待 D32 原生实现，非默认 power 容量待 D22，实机多人待验。 | [实施中：当前原生调用点自动验证通过](nyctocosm-native/docs/validation/D15_reflectance.md) |
| 43 / D16 | S2：弹体方块碰撞语义 | 许多 slowcast 按 `#nyctocosm:passthrough` 判断能否穿过，例如标签含 snow，不含多数其他植物。 | 给审计过的 Abhorrence/Incandescence/Fulminance/Gravitance 与 Emissary lightning_javelin/seeking_missiles/fireball 配置源 tag 体素扫描或阻挡体素停留；保留独立实体扫掠、Forge impact 取消和未加载区块边界。其它未审计 SpellBolt 分支仍按原规则。 | [自动验证通过·待实机](nyctocosm-native/docs/validation/D16_spell_collision.md) |
| 44 / D18 | S2：使者发射节点与飞行姿态 | 攻击从locator发射；飞行25/.5及quick10/.5、FlyTimer20..59可达，任一60格玩家选招门槛；起落根位移、phase4 marker无普通重力。 | 原locator/材质/七飞弹切片保留；alpha.19纠正alpha.17误删计时器、恢复多航点转向/60格门槛/起落位移/精确减速/无重力；Rain按+30航点和10..44四弹。按用户要求以源姿态生成护身idle，保留原clip，明确为表现调整。96/128范围保留，完整37招/多人仍未闭合。 | [实施中：alpha.19反馈切片，自动/实机范围见报告](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md) |
| 45 / D19 | S2：Fireball 速度和存续 | 每 tick 两个前进 1 格子步，60 tick 终止，并按 marker 终止路径 impact。 | 两个一格子步、59 tick 前进/第 60 tick 终止已按源行为验证；双手 locator、随机玩家 feet selector 与地面选招池已核对。终端伤害改为玩家脚部半径5，排除旁观/debug，去除实体燃烧及额外推力。世界放火尚待用户选择；客户端和多人实战待验。 | [部分实施·自动验证通过](nyctocosm-native/docs/validation/D19_emissary_fireball.md) |
| 46 / D20 | S2：Dais 灌注过程 | 交易后播放 inculcate rig；110 帧效果节点调用实际产物生成，产物还带拾取延迟。 | 四项材料配方已一致，但菜单直接取得产物，当场扣料、一次性粒子；没有等待、悬浮材料和完成出物过程。 | 待实施 |
| 47 / D21 | S2：Transmittance 乘客筛选 | 仅传送 `in_plane` 且距法阵不超过 3 格的玩家，各自按视线推进。 | 已支持多人各自视线，但收集乘客只用放大 3 格 AABB，无球形距离和 in_plane 等价判定，角落玩家可能被额外传走。 | 待实施 |
| 48 / D22 | S2（条件性）：元素属性计算 | 全局/各元素 power、speed、tracechance、resistance、weakness 由 base + modifier 计算，施法按顺序修正时间和强度。 | 17招默认值审计一致；alpha.17修Glowing/Resistance/治疗及部分默认发射/运动，非默认属性快照/修正/统计管线仍缺。炽光源命中载体及取消伤害燃烧待确认，不能将默认修复视为属性链完成。 | [实施中：默认玩家法术关联切片；属性链待实施](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA17.md) |
| 49 / D23 | S2：Tony 冷却计时范围 | `nyctocosm:playertick` 对玩家扣 tonytimer 不限当前维度。 | TonyManager 在非夜域直接返回，冷却只累计夜域时间。返回主世界等候不会推进冷却，影响 Dreams 获取节奏。 | 待实施 |
| 50 / D24 | S1：使者音乐推进 | 独立 music queue 按片段推进，生命值 1000/800/600/400/200 排入后续乐段，不只看四个战斗阶段。 | 音乐仅按四阶段分组循环；800、400 阈值没有对应排队状态，暂停还会清状态、恢复后重播 intro。 | 待实施 |
| 51 / D25 | S1：使者死亡及声音/效果事件 | die 第 0 帧播放 `emissary.death.begin`，最终死亡有专门粒子；原包各事件指定声道、距离、音量和受众。 | alpha.18补使者三个转阶段数值粒子/MASTER声音、死亡27次旋涡/双手十字/最终900行爆散、巫师法阵/Voidwave冲击波及180tick死亡、杖召回墨汁和蛇尾雾；使者电火花辅助/最终爆炸网格及其余常态/技能效果仍未补。 | [实施中：alpha.18所选战斗表现切片自动通过待实机](nyctocosm-native/docs/validation/BOSS_COMBAT_REPAIR_ALPHA18.md) |

实施记录（D01、D04、D05、D07、D16、D18、D27、D38 自动验证通过待实机；D02、D06–D15、D17、D26、D28、D46 有已实施切片但仍在实施中；D03、D19–D25、D29–D37、D39–D51 尚待实施/验收；D08–D12 各有已实施切片）：

- [ ] D01–D04：整场交互与主线流程。
- [x] D05：恐惧处理范围（自动验证通过待实机）。
- [ ] D06–D14：使者招式与法阵资源循环（D06–D14 均有实施中条目，详见各行状态）。
- [ ] D15–D23：通用规则、恢复与时序（D15 出射方向首片、D17 攻击恢复首片均仍实施中）。
- [ ] D24–D25：音乐和表现。
- [ ] D26–D38：蛇受击、术士全招/杖与两 Boss 索敌。
- [ ] D39–D40：普通怪完整行为。
- [ ] D41–D46：四武器的技能与主副手规则。
- [ ] D47–D51：伤害类型、蛇射线、神器识别与组合/空间边界。

自动验证通过只覆盖该编号验证记录中的服务器断言；客户端、生存流程、多人和低模拟距离仍按记录所列范围验收。

## 不应再次误报为“未实现”的内容

- alpha.12 已有跨维度 X/Z 缩放、地表安全搜索、玩家/Boss 分别检查净空、使者 95 tick 远方入场、后撤升空、一二阶段悬空、300 格索敌和相关读档修正；仍要 V01 实测，不重新写成“代码完全没做”。
- Wingbeat 已是 4×4 独立推进波面，能漂浮/反射/反制；Godrays、Divine Blast、Lightning Field 已有独立 hazard。剩余节点和采样近似归 D18 / V02，不笼统标成瞬时范围攻击。
- 普通怪自然生成、恐惧主体、Tony 事件、使者书/成就/出口、章节书、神器充能/保护均已有原生代码。
- 碎石的 Nyctocosmic Ash 掉落有 Forge loot modifier，不因缺少同名原版 loot table 就判遗漏。
- 本轮对照的 `nyctocosm` 命名空间中 3 个 dimension、2 个 dimension_type、54 个 worldgen JSON 与原包逐文件哈希相同；结构模板的动态 marker 则另有 D04。静态地形资源相同不证明所有世界行为相同。
- Dais 四项配方成本、掉落物品和动态灌注表现是不同问题；D20 不要求重改已对齐的成本。
- Serpent 的游走/盘旋/俯冲/扫射目标运动与 41 节身体链已有专用逻辑；Sorcerer 六阶段/Nullpoint 和十二招关键帧不是空壳。D26–D38 分别指出事件执行效果的差异，不能据此说动画/阶段全部没做。
- 13 神器中，默认治疗、充能、保护共用扣费时钟、提取和逐次延迟连锁已有实现；D49/D50/D51 与既有 D14/D22 是具体缺口，不要求将所有神器重写。

## 需实测/扩大取样，暂不判定为确定缺陷

| 编号 | 尚不能直接下结论的范围 | 下一步证据与验收 |
|---|---|---|
| V01 | 地下古城门 → 夜域地表 → 使者领域 → 战后返程全链 | 同版本客户端/服务端、新旧原生存档、多种种子、低模拟距离与冷区块；记录坐标/维度/碰撞盒/返程栈。不要把旧 alpha.11 体验当作 alpha.12 实测。 |
| V02 | 静态复查后的逐帧实测与剩余使者分支 | Serpent 三招/Sorcerer 十二招已查核心代码链，差异列入 D26–D38/D47/D48；仍需逐帧轨迹/命中、随机分布和联机录屏。使者其余近战/架势/抓取继续取样。 |
| V03 | 多人、旁观/创造、掉线重连、末击奖励 | 2–4 人同时进行法阵、传送、反射、Boss 死亡；检查归属、重复奖励、离线奖励策略与状态清理。D02 已确认的单人入口不必等待此项才立项。 |
| V04 | Forge 生态与性能 | 有护甲/抗性/友伤/伤害事件取消的测试模组；低 TPS、大量弹幕、区块卸载、远程客户端录屏。当前测试并未证明所有第三方组合兼容。 |

V01–V04 的具体验证方案在方案文件末尾；不靠“先全勾选”宣布还原完成。

## 排除范围与需要用户审查的设计边界

| 编号 | 当前差异/边界 | 建议处理，不代表已获实现批准 |
|---|---|---|
| C01 | 旧数据包存档迁移：按用户要求排除 | 不计差异、不设计转换、不执行迁移。D17/D35/D37 原生读档和 D49 新物品改名不属于这一排除项。 |
| C02 | 原生安全落点拒绝危险地形；原包 spreadplayers/地表命令不保证完整 Boss 净空，且有长期 forceload | 保留安全检查和短期票。若要求原包随机散布距离，单独对齐分布，不退回地下卡死或永久强加载。 |
| C03 | 原生实体/GUI/伤害事件替代 armor stand、村民交易和自定义扣血；有友伤/宠物保护 | 保留 Forge 原生结构；只对齐可观察结果。原包全局 selector 串场等缺陷不直接照搬。 |
| C04 | 原资源中缺失的声音已有占位；粒子量与长距离广播也受客户端性能约束 | 区分“资源本身缺失”和“调用没接”。新增替代素材需审查；性能降级应只改表现密度，不改伤害与预警时间。 |

## 阅读与执行顺序

交给后续 AI 实施时，使用 [可复制修复提示词](AI_GAMEPLAY_REPAIR_PROMPT.md)，并同时阅读 [执行/决策/验收补充](GAMEPLAY_IMPLEMENTATION_GUIDE.md)。提示词准备轮只改文档，具体实现须由新的实施任务授权。

先按上表顺序审查差异是否应还原，再看方案文件同编号。实施时可先建立 D18 的公共节点计算、D15/D16 的碰撞/反射规则，支持前面的大项；**依赖顺序不改变差异排序**。旧 [MAINLINE_GAMEPLAY_PARITY_TODO.md](MAINLINE_GAMEPLAY_PARITY_TODO.md) 保留历史，本清单是本次复核的独立剩余项，不覆盖或伪造此前完成记录。


## 2026-10-04 alpha.19 三Boss反馈修复补充

本轮用户确认使者默认问题为出场后的悬浮形态。最终288/288 required回归、build/reobf及42张隔离客户端截图完成，详见 [alpha.19报告](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md)和TODO阶段88–90。

- D18：更正alpha.17误删源FlyTimer的结论，ai/subtick→736/737可达，20..59定时换路已恢复；保留通常不可达的@a自动加速分支判定。恢复>60无玩家的选招/AttackTimer、全部航点转向、起飞1..5升高、降落0..4下降及精确减速，phase4 marker无普通重力，Godrays升高保留；Rain维持玩家+30航点/10..44每帧四弹，源码没有最低高度门槛。96/128范围及防埋地夹紧保留。护身idle以源spawn75 arms/idle0 wings生成，只改客户端待机采样，属于用户要求的表现调整。
- D27关联客户端：头/身位置和角度统一BODY快照，去掉滞后实体头坐标拖动全链；服务端原41节追逐算法和17220命令采样保留。
- D38关联巫师反馈：抗击退1及kind1 knockback屏蔽，保留玩家伤害及技能位移；仅开场smear1按用户要求隐藏，法杖/战斗弧片保留。
- 自然AI免伤玩家夹具捕获三次高空Rain，共105发射帧，root高于玩家18.77..21.17格；随后手动高空场景单独标注。完整37招、原包/mod并排整场录像、真实自然生存/多人/保护模组/shader/重启卸载/晚加入/FPS仍待验收。未选玩法不因回归构建自动闭合。协议6，两端同版；本轮没有关机。
