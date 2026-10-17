# 三个 Boss 的行动与战斗特效对比

日期：2026-10-03。对比对象：本地原数据包与当前工作区 alpha.17 原生实现。

## 1. 范围与结论

本轮只做对比。覆盖巨蛇的 3 种攻击、巫师的 12 种攻击、使者的 22 种攻击，以及索敌、移动、连招、阶段切换、开场、持续粒子和死亡演出。检查了三个命名空间的全部 1,602 个函数文件、相关模型的动画命令关键帧、被引用的粒子辅助函数及原生服务端和客户端调用链。

这是**源码层面的逐招审计**。没有运行原数据包与 mod 的整场并排录像，也没有新增测试、启动游戏、编译或修改玩法代码。原包的 `execute` 条件、调用顺序和子步计时一起核对；文件中存在一行粒子命令，不等于该条件在游戏中一定执行。历史 262 项测试和客户端截图属于上轮记录，不作为本轮“全部还原”的依据。

当前实现保留了大量原模型、攻击名称和关键帧，但**模型动画相同，不代表攻击运动和粒子演出已经相同**。主要结论：

| Boss | 行动方面的主要差异 | 粒子演出方面的主要差异 |
|---|---|---|
| 巨蛇 | 出生高度、游动中心、首次攻击等待不同；死亡时没有原包的继续上行运动 | 红色须线、尾部五段黑雾、转圈噩梦脉络缺失；激光和光柱密度/比例/发射方式不同；死亡用 Portal 粒子替代原效果 |
| 巫师 | 地面形态增加导航追人并扩大远程攻击距离；转向更直接；死亡 133 tick 提前结束，原包到 180 | 已有较多逐帧粒子，但开场法阵辅助效果、施法元素爆散、陨石裂口展开、Void Wave 冲击波仍缺失或替代；法杖粒子频率不同 |
| 使者 | Seeking Missiles 缺少第 23 帧激活，生成即飞；地面侧移云尘缺失（慢滑回调未解析）；抓取载体允许多人同时乘坐 | 翅/手/脚/光环持续效果缺失；多个技能蓄积、翼波、转阶段和完整死亡演出尚未接入；部分技能只有通用投射物尾迹 |

### 最影响观感或战斗节奏的差异

| 编号 | 项目 | 已确认的差异 |
|---|---|---|
| BC-E01 | 使者追踪飞弹 | 原包 2–14 帧建立 7 枚悬停飞弹，23 帧统一激活；原生生成即移动，寿命和追踪方式也不同 |
| BC-E02 | 使者死亡 | 模型仍播放，但 27 次旋流、57/72 两次十字效果、最终大范围粒子与死亡开始音效没有对应完整调用链 |
| BC-E03 | 使者持续与转阶段效果 | 翼部五类粒子、末期手脚烟火/光环火花、phase3_transition 的 30 行直接粒子和 9 个声音事件缺失 |
| BC-W01 | 巫师地面 AI | 原包近程 ≤7、远程 8–20；原生所有 >7 距离可进入远程池，并在 >10 时导航靠近 |
| BC-W02 | 巫师死亡 | 原包 180 tick 结束；原生 133，后续停留与尚未结束的辅助粒子寿命被截短 |
| BC-W03 | 陨石裂口 | 发射次数/主要节奏相符，裂口由点扩展到半径 5 的过程和激活后的三层大环/旋流不同 |
| BC-E04 | Wing Blast / Celestial Smite | 原生有射线和命中，但光束纹样、法阵/冲击波仍由简单环或螺旋代替 |
| BC-S01 | 巨蛇持续与死亡粒子 | 须线、尾雾、噩梦脉络、随机灵魂爆散不能用已有头部黑雾/Portal 等同 |
| BC-E05 | 雷枪、陨石雨、Wingbeat | 核心发射存在，但充能载体、逐子步尾迹或生成式纹样未完整移植 |
| BC-A01 | 音效与可见范围 | 多数旧调用使用 HOSTILE 和默认粒子发送；原包大量使用 MASTER、显式玩家距离和 force，远处/低粒子设置表现不同 |

以下“主要对应”只指表中所列部分；不表示该招已经逐像素、多人或所有伤害事件验证一致。

## 2. 巨蛇 Serpent

### 2.1 行动、索敌与身体

| 项目 | 原数据包 | 当前 mod | 结论/证据 |
|---|---|---|---|
| 登场 | 施法位置下方 100 格召唤；游动区域中心设在施法位置上方 40；首次攻击 timer=400；播放 incur | 通用 spawn 将头放到位置上方 10；SerpentMotion 以头位置初始化中心，首次等待 100–199；spawn 入口无原 incur 调用 | 明确不同，[S-incur]、[N-spawn]、[N-serpent-motion] |
| 常态游动 | 区域中心附近随机转向；速度、插值状态按原包状态机变化 | 已使用独立 SerpentMotion，保留多状态、目标点与速度档；随机数发生器和初始中心不同 | 主要运动机制对应，出生轨迹与首轮节奏仍不同 |
| 选招/选人 | 攻击范围内三招选择；攻击开始从 200 格内玩家随机选目标 | 三招选择与 200 格随机锁定玩家保留；通用索敌先过滤可攻击目标 | 主结构对应；Creative、NPC 和多 Boss 隔离语义不同 |
| 攻击前转圈 | `twirl` 递归局部移动与旋转，附随 Nightmare Vein 随机单条脉络 | 已有 61 步转圈运动；没有对应噩梦脉络生成 | 运动对应、特效缺失，[S-twirl] |
| 身体链 | 多级头部位置/旋转延迟、40 段身体和尾部；另有须线标记 | SerpentBody 保存 42 个身体位置并分段绘制；没有对应 10 条须线标记的粒子链 | 身体已实现，须线效果缺失 |
| 头埋入方块时身体消失 | 身体各自是独立展示/标记 | 当前 SerpentWorldRenderer 按身体分段剔除和取光照，头部实体不再以普通视锥剔除整条蛇 | 该旧问题在当前代码已有修复；实际头部区块卸载仍不能由此证明，[N-serpent-render] |
| 死亡运动 | 进入状态 5，目标上移并继续运动/身体跟随，100 tick 后销毁 | tickDeath 未继续调用 SerpentMotion，主要保持尸体位置并发 Portal 粒子 | 明确不同，[S-serpent-death]、[N-death] |

### 2.2 三种攻击逐项

| 技能 | 原包行动和粒子 | 原生实现 | 差异 |
|---|---|---|---|
| Dive | 转圈后俯冲；与目标、区域距离和最长 400 tick 有关；伴随 scream/dive 与转圈脉络 | 已有俯冲状态、区域约束与结束条件 | 大体动作机制对应；转圈脉络缺失，声音发送方式不同；未作同随机种子实机轨迹比较 |
| Laser | 转圈后固定目标扫描；整数 1–150 格射线，碰到非 air 标签方块终止；每格红 dust 2、黑 dust 4，size=1、spread=.5、force；阻挡处召唤隐形火球 | 150 格整数阻挡扫描和玩家脚点球形命中保留，伤害调用传 12；粒子是每格交替一个红/黑，size=1.5/2，spread=0，默认发送 | 密度由每格 6 降为 1，红黑比例由 1:2 变成交替 1:1，形状/大小不同；起射 Nightmare Vein 爆散和原火球阻挡演出没有等价调用。原激光 damageAmt=12000，应按伤害管线理解为 12，[S-laser-ray]、[S-laser-hit]、[S-laser-block]、[N-serpent-tick] |
| Beam | 20 个独立光柱，随机落点；140..279 延迟。预警每柱黑 70/红 30；80 时噩梦脉络爆散和发射声；60..1 时高度 1–80，每层红 1/黑 2、size=4、spread=1、force | 20 个独立 SerpentBeamHazard、延迟与主要伤害窗口保留；预警数量降低；攻击柱以预算采样红/黑各 1；起爆 Flash+ExplosionEmitter | 强度、粗细、红黑比例及脉络演出不同；共享 600/tick 预算和 160 格视觉范围属于已授权的性能调整，但缺少辅助效果仍应单列，[S-beam-ray]、[N-serpent-beam]、[N-world-effects] |

### 2.3 持续特效与声音

| 效果 | 原包 | 原生 | 状态 |
|---|---|---|---|
| 头部黑雾 | Y+1.5、局部前 2，黑 dust size=2、30 个、spread=.7、speed=0、force | Y+1.5，无前 2 位移；size=1.8、30 个、speed=.01、默认发送 | 位置/尺寸/运动/发送方式不同 |
| 尾部黑雾 | Y+1.5，向后 2/6/10/14/18 五处，每处 5 个 size=3 | 无对应五点发射 | 缺失 |
| 红色须线 | 10 个标记按距离和速度追随；红 dust size=3，每点 3 个，force | 没有须线粒子链 | 缺失 |
| 游动呼啸 | 以 40–99 tick 随机 timer 从随机身体部位播放，MASTER、200 格 | 每 tick 1/180 随机概率，HOSTILE | 频率/声道/范围不同 |
| 入块低鸣 | 入块每 8 tick；pitch=0，minVolume=.5，MASTER、200 格 | 入块每 tick 1/80 概率，pitch=.5，HOSTILE；方块判断也从 air 标签变成 isAir | 频率、音高、听感及介质判定不同 |
| 死亡过程中 | 随机身体段 Nightmare Vein | 12 个 Portal 随机身体段 | 替代效果 |
| 最终消散 | 随机 10 个段 Soul Swirl，40 个身体段 Flash，并有原爆炸/defeat | 40 段 Flash 保留，另加 4 个 ExplosionEmitter；没有随机灵魂爆散 | 部分对应、部分缺失 |

参考：[S-serpent-ambient]、[S-serpent-tail]、[S-serpent-whisker]、[N-serpent-tick]、[N-death]。

## 3. 巫师 Sorcerer

### 3.1 阶段与 AI

原包是 6 个逻辑阶段：1/3/5 为法杖支撑的悬浮形态，2/4/6 为法杖破坏后的地面形态。地面 2 降到 666、地面 4 降到 333 后重新升起；法杖健康 300。原生主要阶段结构与这个流程相符。

| 项目 | 原包 | 原生/差异 |
|---|---|---|
| 悬浮形态攻击池 | ≤15 近程，其他为远程；按阶段加招和加权 | 主要分池对应；phase3 二选一通过 nextInt(35)%2，产生 18/35 与 17/35 的轻微偏差 |
| 地面形态攻击池 | ≤7 近程；8–20 远程；(7,8) 与 >20 没有该分支攻击 | 原生所有 >7 都进入远程池；>10 时还有 vanilla navigation 追人，源常态 AI 无等价导航 |
| 转向 | 空闲按 nearest 玩家 40 格的脚点，以局部 5/.5 平滑转向 | 通用 LookControl/直接 yaw 更快，目标选择可以包含生物 |
| 等待 | 攻击结束后 10–59 tick 随机间隔 | 主要间隔对应；独立投射物常继续存在，不能用 Boss 动画结束等同技能结束 |
| 法杖碎裂 | falltransition 第 25 帧移除法杖并 squid_ink 300 | 移除法杖时点已对应；墨汁爆散缺失 |
| 插杖升起 | 初始 253 / rise 51 的 Flash 和冰碎粒子在 Boss 根位置；法杖按局部偏移建立 | 原生用安全地面扫描，粒子/伤害中心部分在法杖位置而非 Boss 根位置；这是可见的偏移差异 |
| 法杖持续紫雾 | 按法杖健康分档，逐 tick 1–13，Y+2、dust size=1 | 每 4 tick 发一次，Y+1.7、size=1.2，数量与偏移/速度不同 |
| 开场“前方弧片”旧问题 | 原位面模型与法阵效果独立 | 当前 SourcePose 用真实 plane locator/矩阵定位；旧错位模型已处理。原包 initial_spawn 330 转 rise，rise 127 才建法阵，当前代码也按此分支；仍缺 summonplane 对应的短时 shockwave。不能把法阵粒子缺失与模型错位合并成同一问题 |

证据：[W-ai]、[N-boss-ai]、[N-staff]、[W-fall]、[W-plane]。距离条件值得优先关注，它会直接改变地面阶段的压迫感与移动节奏。

### 3.2 十二种攻击

所有悬浮形态开招函数里的 `effects:elements/void` 没有完整对应的原生入口；`CHARGE` 的粒子处理为空。以下多招已有局部特效，但共同缺少这个元素施法爆散。[W-element]、[N-sorcerer-events]

| 技能 | 原包关键行为/效果 | 当前实现及差异 |
|---|---|---|
| Halo Beam | 5–40 每 5 帧生成 8 节点，67 激活；节点各自计时、锁定/发射；halobeam glyph 与射线环纹 | 独立 SorcererBeamNode 已还原各节点计时和编译数值图案；正常新生成走独立节点，不能把旧保存兼容数组的 75..110 当作所有新节点的错误时点。共同元素开招爆散和音效发送仍不同 |
| Shard Hail | 35 建立抬升冰球，30 tick 抬升，18 个随机延迟节点，扩散冰屑/碎冰/电弧、雪/烟花爆发 | SorcererHail 已移植球、18 节点、扩散计时和主要粒子/玻璃声音。新实际节点用 Void 伤害，不能拿旧攻击表的 DREAM 字段断言所有冰雹仍错元素；命中附加效果的 Forge 取消规则仍受 D28 待决策限制 |
| Frozen Blades | 34 双刃建立，32 电弧扫掠；12 点纵向弧线、5/.5 转向、100 寿命、14 伤害 | FrozenBlade 独立实体主要恢复了弧线、转向、逐步粒子和音效；共同施法元素效果与待确认命中附效仍有差异 |
| Singularity Charge | 20–41 扁平 Void Swirl/紫粒子；44 发射，先 .3 位移再按 40/.5 转向，100 寿命、近距终结 | 行为 profile 和 VoidSwirl 已接入；部分黑粒子 force/参数不同；不应继续记成“完全没有旋流” |
| Void Meteor | 10 在世界 Y+12 建立裂口；激活前紫圈从半径 0 展开到 5；30 激活，三层环、Void Swirl，大约每 10 tick 发一次，共 6 次 | 高度、六次发射及主要节奏对应。原包激活计时由 block109+112 每 tick 增两次，不能误判成 120 tick 六次。原生激活前是一次半径 2.5 静态环/黑粒子；激活后每两 tick 一个 24 点小环，三层大环/旋流和 transmittance_first 没有对应。陨石三子步的 campfire_signal_smoke 等完整尾迹和逐次 shoot 音效也未完整还原 |
| Dash Strike | 37 锁定前方目标，41–49 位移，49 双次命中，50 视觉尾帧；dash 数值粒子 | SorcererDash 已保存移动前/后位置，主要关键帧和 20 行直接粒子对应；附加迟缓及声音规则仍有差异 |
| Air Dash | 1–19 抬升 7.5；20 双份 160 行环形爆发；26–39 冲刺，40 到达与命中 | 已有 3,201 行编译粒子和位移前/后位置，不再是早期简单电弧替代。伤害取消后的附效、远距 force 与场景观感仍未整包实机对照 |
| Void Beam | 20 可为 40 格内所有玩家建立目标；30–57 发射，58 收尾；源 38 行图案和子步截断 | 独立节点已实现多玩家建立、阻挡/反射、主要数值射线。普通新节点与旧兼容 halo 数组应分开；多数外层音效仍用 HOSTILE |
| Void Wave | 充能直接粒子/辅助效果，38 在 Y+2 打出 transmittance_first 冲击波 | 已导入直接粒子部分，但额外加 Falling Obsidian Tear 和周期 Flash；释放用 48 点半径 6 紫圈代替冲击波。原效果形状不同 |
| Javelin Swing | 34 声音/模型斩击，35 局部前 2/Y+1 的范围攻击与 Sweep | 原生 BLADE_SWING 在 34 先发 Sweep7，35 JAVELIN 再发 Sweep7，比源多一次提前扫击粒子；主要攻击位置已对应 |
| Seeking Singularities | 30 从局部 (.7,2.4,1.5) 发 8 枚；均匀 yaw/pitch 随机；70–89 寿命；黑粒子 spread=.1，逐 tick 低音 gravitation hum | 原生从眼部朝目标偏移 1.5，随机 XYZ 归一化，角分布不同；黑粒子 spread=.3；没有原逐 tick hum。主转向/反射与用户已确认“取消伤害仍 Slowness”已实现，不能将这项再列为未批准 |
| Ice Spikes | 法杖中心 30 个方向，连续五圈，共 150 个尖刺；源波形绝对 yaw 0..348 | IceSpikeWave 主要波次、缩退、模型/接触/玻璃声已实现；原生基准 yaw 取法杖旋转，而源明确用绝对方向，方向整体有旋转差异 |

对应原生证据：[N-sorcerer-events]、[N-sorcerer-fields]、[N-halo]、[N-hail]、[N-frozen]、[N-dash]、[N-ice]、[N-bolt]。原包各招函数与命令关键帧的逐行索引见证据 JSON。

### 3.3 死亡

原包 die 在 11 浮起、56 自刺并释放灵魂、115/116 大爆散与消失、126/130 法杖落地，180 才进入最后清理。原生已有 56 的 Soul Swirl、116 的 Soul Swirl、115 的 Void Vein，以及导出的直接粒子和多数声音时点，所以“自刺完全没有灵魂特效”已不符合当前代码。

仍有三项差异：

- `deathDuration()` 是 133，源到 180，死亡停留短 47 tick；依赖 Boss 仍被跟踪的辅助演出尾部也会随移除停止。
- 骨骼定位的粒子转成根/局部定位的编译数据时，不能由行数相同证明与 torso/head 运动完全重合。
- 原包在 AI 可进入死亡动画的空闲分支判断死亡；原生 LivingEntity 死亡生命周期能在攻击途中切入。与独立技能的清理顺序有区别。

证据：[W-death-end]、[N-death]、[N-world-effects]。

## 4. 使者 Emissary

### 4.1 阶段名称与材质

用户描述的“二阶段地面近战 / 三阶段高速飞行”，分别对应原数据包的 **phase3 / phase4**。原包 phase1 与 phase2 都是悬浮攻击阶段。比较必须按动作定位，不能只按中文阶段编号匹配源码数字。

| 源阶段 | 血量与行为 | 当前实现 |
|---|---|---|
| phase1 | 初始 1200，第一套悬浮四招 | 攻击集合对应 |
| phase2 | ≤1000，第一转阶段后第二套悬浮四招/侧移 | 主要转阶段时点、技能集合对应；翼部持续效果与转阶段粒子不完整 |
| phase3 | ≤600，第二转阶段落地，地面近战、格挡、追近与连招 | 主要集合/连招已实现；此时仍应 armorful，当前 EmissaryAppearance 已如此保存 |
| phase4 | ≤200，第三转阶段后高速飞行六招 | 171 帧才 armorless，180 进入末期；当前逐骨骼持久变体机制已对应 |
| 转阶段触发 | 等当前攻击结束再检查阈值 | 主要逻辑对应；Forge 伤害/死亡安全机制和源记分板生命值系统不同 |

当前手持大剑在 Sword Plunge 的 10 帧显示、45 帧消失，并跨 idle 保持；独立巨剑 root Rotation 的错误随机倾斜已去除。地面近战提前换甲、落剑仍一直持剑、巨剑倾斜三项，在当前 alpha.17 代码都有修复。它们的历史截图不能证明用户安装目录一定已经用了该包，本轮没有检查安装目录。[N-appearance]、[N-sword]

### 4.2 常态行动、飞行、格挡与连招

| 行动 | 原数据包 | 原生差异/对应 |
|---|---|---|
| 出生飞行 | 95 tick 接近，逐 tick 七类粒子 Flame20/Lava10/CampfireCosy5/Flash1/Smoke10/Wax5/EndRod5，再播放出生模型 | 95 tick 接近段主要对应，七类粒子数量也对应；后续 spawn 62–99 的局部方向/辅助演出未完整对应 |
| 悬浮左右侧移 | 3–16 每帧侧移 4，并有脚/翅定位的云尘 | 主要移动时窗对应；源脚/翅辅助效果缺失 |
| 地面左右侧移 | 0–9 每帧侧移1；10–14动画调用`dash_skid`，但原包只有`dash_slow`文件，回调不解析；.3慢滑辅助未从动画调用 | 0–9移动已存在；云尘仍缺失。2026-10-03实施复查纠正此前“.3尾段可达”的结论，不凭辅助文件恢复不可达移动 |
| Run Close | 固定 .7 移动，持续循环到 5 格内玩家；源转向脚点/全体选择器 | 原生向目标向量归一化后 XZ×.7，基本水平追近对应；固定单一目标与高度差时的水平速度/多人到达条件不同。没有“把整个距离乘 .7”的失控追近错误 |
| Flip Far | 4–16 后退 .7，带翼音/云尘，可继续连招 | 主要移动时窗对应，附带定位效果缺失 |
| 格挡/破防 | parry 与 parry.break 模型、攻击条件、格挡受击与后续连招 | 已有独立 parry 与命中区域机制；伤害事件/状态处理遵循原生生命周期，不能用播放模型作为全部格挡判据已一致的证据 |
| 近战连招 | 特定关键帧后按概率继续近战攻击或位移 | 当前有 comboFrame 和 70% 分支，主要结构对应；Java RNG 与源全局 LCG 不保证逐次相同 |
| 起飞/降落 | idle/transition 动作、FlySpeed=5、降落减速，指定帧修改飞行状态 | 已有对应关键帧和状态；源飞行 idle 每两帧 fly 音为 MASTER10/100格，原生每 tick 调 MASTER3/200格，频率/音量/范围不同 |
| 转向/步长 | 25/.5，quickturn 再 10/.5；速度 N 对应通常 N−1 次 .5 移动 | alpha.17已对应基本构造与flight/flight_step尾迹。2026-10-04复查更正：ai/subtick→736/737的20..59tick巡航计时器可达，alpha.17删除它是错误，alpha.19恢复；多航点依创建顺序全部参与正常/quick转向 |
| 航点更新 | 玩家周围 XZ±15、Y+20±15；达到任一5格航点、特定事件或FlyTimer到期更新；corkscrewing/grappletry/grapplegrabbed暂停计时递减 | alpha.19恢复源定时换路、>60无玩家时维持飞行/地面起飞与AttackTimer0..29。Java RNG与源LCG、连续多人顺序仍需实机；未加载区块原生暂停移动 |
| 起落根位移/重力 | 起飞1..5每帧+.80625、14启飞；降落0..4每帧−.80625、4停飞，减速0/3/6/8/9/10/11；root是marker，没有普通重力 | 2026-10-04连续复查发现此前根位移遗漏且phase4非Flying用了原生自由落体；alpha.19恢复位移/减速/无重力，保留降落防埋地表夹紧。Godrays升高不再被普通重力抹掉，Rain仍按玩家+30航点与10..44发射，不新增最低高度门槛 |
| 飞行范围 | 源攻击/航点选人常为 100/300，无统一 8 区块球形硬边界 | 当前 96 格回航、128 格硬限制是用户已授权调整；越界转向会影响轨迹，但不能记成未获批准的误实现 |
| 自动加速分支 | 源距离 >41 加速判断使用 `@a[tag=emissary,tag=flytarget]`，而正常航点是 marker | 该源条件通常无法命中。不能仅看到加速语句，就要求原生无条件实现“远离航点必加速” |
| Grapple 抓取 | 尝试抓人、切 grab 动画、翼尖 locator 挂点；源每 tick 强制 ride 的选择器 limit=1 | 原生独立座位 `canAddPassenger=true`，可容纳多个玩家；挂点/抓后飞行有实现，但不是原单乘客状态语义 |

证据：[E-flight]、[N-emissary-flight]、[N-emissary-movement]、[E-grapple]、[E-grapple-ride]、[N-grapple]。

### 4.3 全部攻击

#### phase1：悬浮第一套

| 技能 | 原包 | 当前差异/对应 |
|---|---|---|
| Lightning Javelin | 雷枪先附着手部充能，再发射；每 tick 五个 1 格 slowcast 子步；充能与飞行有 lightning sparks/draw_spark | 原生到发射帧生成通用 SpellBolt，速度 5，没有完整附手充能载体及逐子步电纹；通常显示通用 EndRod 尾迹，不能等同源雷枪。声量/开始帧也不同 |
| Meteor Shower | 独立 shooter，每 15 tick 发射，共 10 枚；首发 shockwave/meteor_shower，手部蓄积；每子步 Lava3/Flame7/Smoke5/CampfireCosy2/Wax5 | 十枚/主要间隔对应；原生用通用投射物，手部蓄积、首发冲击波和五类逐子步尾迹未完整还原；转向/碰撞采用通用 profile，轨迹存在区别 |
| Pillars | 云柱/火柱两个独立载体，追踪与反射，300 寿命；高 6 的云/火烟柱及三层火花/火环 | 两柱类型、寿命、运动、反射与主要粒子已有实现。云速度 .05 被统一 .025，部分火/烟/环的参数不同，force 多变默认；不能列为完全无效果 |
| Wingbeat | 16 个网格载体，三次子步，接触浮空；另有按计数展开的渐变 dust/火花纹样 | 16 载体和主要接触浮空对应；当前 ElectricSpark/Flame 简单散点不包含源数百行渐变纹样，充能/翼端效果不完整 |

#### phase2：悬浮第二套

| 技能 | 原包 | 当前差异/对应 |
|---|---|---|
| Celestial Smite | start 立即执行 tear→circles→activate；26 建新 tear、50 激活、93 射击；射线/终点与多级环形法阵冲击波 | 原生上述核心建立顺序已对应，不能继续说“多生成三次裂口”；150 格射线、15 格终点范围与伤害主机制有实现。法阵/激活/冲击环用少量 Cloud/EndRod/SoulFire 环和简单终点爆散，未照搬源数值纹样 |
| Flame Geysers | 两组各 14；随机点在 ±25 方形，timer20–149，末段预警/50 格喷发，多层 shockwave 与粒子 | 两组/数量和主要计时保留；原生落点采圆形而非方形；喷发用大范围随机 plume 替代多层原冲击波。手部逐帧效果也不完整 |
| Sword Plunge | 45 手剑隐藏，独立展示剑 HEAD×10/sky10 竖直落下；独立 timer105 六层 shockwave，120 爆炸 | 剑模型、45 帧隐藏及竖直姿态已修复。105 的六层波目前是六处 Flash，形状不同；120 爆炸有粒子但不等同全部原多层图案。世界火焰规则仍待 C03 |
| Wing Blast | 六个翼尖节点，35 开始光束、43 结束；70 步射线，源翼波/Radiance 辅助纹样 | 六节点和主要窗口有实现；客户端 `emissaryBeam` 仍用通用 EndRod + Flame/Wax 螺旋，每段采样限量，与原纹样不同。Godrays/Divine 的新图案不能当作此技能已修复 |

#### phase3：地面近战

| 技能 | 原包 | 当前差异/对应 |
|---|---|---|
| Cross | 5–7 前冲，6 三处前方命中区域；剑的变体残影 | 主要前冲/命中区域/持久模型变体已有对应；原招关键在模型残影，没有额外大粒子辅助函数，不能因粒子少判为缺失 |
| Flame Slash | 2/13 两次火焰波，沿面推进并以固定圆形采样/反射 | EmissaryFlameWave 独立实现主要两波/采样/反射；粒子速度、分布及默认发送仍与源存在参数差异 |
| Ground Slam | 1–4 前冲，5 扁平接触区域；7 在局部前 2.5 发 Lava5、Stone100 | 原生前冲/命中实现；第 7 帧没有这组地面粒子，只有攻击/声音，明确缺失 |
| Lightning Field | 充能手部随机 lightning 图案；34 生 32 节点，四条半径 6..34；节点预警 Wax5/Spark5 在 Y+3、纵 spread4，21 触发闪电/Flash/Explosion | 32 节点与伤害窗口保留；充能手部电纹缺失，预警只有 Y+.15 的 Spark8 扁平散点，缺 Wax、出生 Flash、原预警竖向体积和部分触发爆散。真实闪电世界副作用待 C03 |
| Seeking Missiles | 2–14 七枚建立并悬停，23 激活；寿命 60–89，两个 .7 子步，每子步追当前最近玩家，cloud/firework/spark/flame 尾迹和悬停十字 glyph | 七个 locator 出生位置已修，但建立即发射、寿命120、锁定单目标/随机高斯方向；无 23 激活、悬停 glyph 和四类原尾迹，是明确的动作与演出缺口 |
| Sword Spin | 6–8 前冲，7 范围接触，剑残影/连招 | 主要窗口和模型变体对应；通用受击/音效规则仍有差异 |
| Sword Swing | 1 命中区域，2/3 前冲，残影和继续连招 | 主要窗口/区域/变体对应；同上 |
| Front Kick | 4–6 前冲，4 多段接触区域；踢击/连招 | 该招由近战池单独选择，不在七招数组里但实际存在；主要窗口/区域对应。源载体与原生碰撞/事件过滤仍有共通差异 |

#### phase4：末期高速飞行

| 技能 | 原包 | 当前差异/对应 |
|---|---|---|
| Apocalypse Rain | 10–44 每 tick 四枚，共 140；扇区俯仰、双子步、超时/碰撞爆炸；五类 force 尾迹 | EmissaryRainShard 已恢复数量、主路径、五类粒子与 MASTER 声音；此技能不再是早期的通用落弹。世界火焰/真实自然飞行观感仍需分别处理 |
| Corkscrew | 13 激活、64 收回；quickturn/持续接触/score 纹样；14–63 每帧 spin 音 | 运动状态、接触与 72 行数值尾迹已有实现；spin 声原生主要只在 13 播放一次，连续声音节奏不同 |
| Divine Blast | 建立独立 shooter，24 激活，26 tick 光束；充能静态图案/Flash，27 行射线、3 行终点、1,130 行冲击环 | alpha.17 已导入射线/终点/冲击环，玩家 cube 截断和脚点冲击恢复；充能仍是 EndRod6 普通散点，缺原生成式充能图案。外层 charge 声道/声量不同 |
| Fireball | 8/18 双手发射，两个 1 格子步，60 计数寿命；蜡/火环、尾迹和爆炸 | 手部起点、源通行标签及主要伤害规则有实现；尾迹通过通用 weapon visual 路径近似，采样次数/图案与源不完全一致。源无额外实体燃烧/推开，原生已移除旧添加项；世界方块火焰仍待 C03 |
| Godrays | 15 生载体；充能四路红标，15..30 活跃，三次子步旋转+1 yaw，四路 70 步射线/终点/400 行冲击环 | alpha.17 已恢复红标、三个路径包、射线与冲击环；不是完全缺特效。充能外层声音与实际多人路径执行仍有差异，不能拿孤立射线截图证明整场高速形态一致 |
| Grapple | 15–70 抓取尝试；grab 后翼端挂点，60 改俯冲目标，160 释放；落地对乘客有 Lava1/Flame3/Smoke3/Stone6/Dirt6 和 slam 声 | 原生抓取/悬挂/俯冲/释放流程已实现，但座位可多乘客；乘客主要只有 Flame3，原落地烟土组合不全；原生部分事件追加选航点，与源仅取消标签的事件不同 |

原生入口：[N-emissary-attacks]、[N-hazard]、[N-celestial]、[N-pillar]、[N-rain]、[N-beam-client]、[N-sword]、[N-bolt]。各招完整源命令索引均保存在证据 JSON；表中“粒子存在”与“原数值图案已导入”分开判断。

### 4.4 持续、转阶段与死亡演出

| 演出 | 原包实际命令 | 原生状态 |
|---|---|---|
| phase1/2 翼部持续火焰 | 各 wing locator 的 Flame/Wax/Lava/FallingLava/Smoke，每类2、force | 没有对应的常态逐 locator 粒子分发 |
| phase4 眼/手/脚/光环 | 眼 Spark；手脚 FallingLava/Smoke/Flame/Ash；光环倾斜圆环 18 点 Spark | 模型 locator 可采样，但这些常态粒子未接入。不能由普通射线/火球粒子代替 |
| phase1_transition | 32/46 等 23 行直接粒子，加 lightning explosion_sparks；两次关键声音 | 原生主要声音和一次 ExplosionEmitter，缺原多类逐帧粒子和电火花 |
| phase2_transition | 0/16/36/60 四个声音；后撤、冲刺、落地 | 原生主要 0 音和移动，16/36/60 原声音未完整调用；落地安全位置扫描改变碰墙/穿地观感 |
| phase3_transition | 30 行 Flash/Flame/Lava 等直接粒子；18/64/79/89/100/110/137/144/146 共9个声音；171 换甲 | 材质/模型变体关键帧已恢复；30 行粒子和9个声音事件未恢复 |
| Death | 0 death.begin；30–98 共27次 death_swirl；57右手、72左手 crucify；100 最终900行粒子与爆炸/清理辅助 | 原生 deathDuration=100 和模型保留，服务端 tickDeath 没有 kind2 专属这些事件；客户端自动编译粒子播放仅 kind1，因此这些完整使者效果缺失 |

证据：[E-ambient]、[E-transition1]、[E-transition2]、[E-transition3]、[E-death]、[E-death-final]、[N-death]、[N-particle-loader]、[N-world-effects]。

## 5. 三个 Boss 共通差异

### 粒子发送和观察者

- 原包的 `force` 通常允许远距离观察。原生很多 `server.sendParticles(...)` 用默认模式，受常规发送距离/客户端粒子设置影响。少数新实体已经逐玩家 true 发送至 512 格，新射线路径也有独立客户端效果；不能将全项目概括为“全部 force 已恢复”或“全部粒子都只有 32 格”。
- `BossParticleEffects.clientFrame(NativeBoss, frame)` 只允许 kind1，载入 sorcerer_keyframes 和 feedback_keyframes。导入模型 JSON 的命令关键帧，并没有自动执行 mcfunction，也没有自动给 kind2 补齐使者粒子。
- WorldEffectController 对已观察到的巫师有最多 4 帧补偿；首次出现从当前 age 开始，不补所有过去效果。独立 Soul/Void 辅助演出仍随 Boss 被观察/移除而受影响，与源独立 marker 生命周期不同。
- 巨蛇共享预算已明确保留。原来光柱最大约 20×80×3=4,800 个新 dust/tick，不能直接恢复全部数量而不考虑用户已要求的掉帧优化；形状、颜色比例和辅助脉络可以分别讨论，本轮没有改动。

### 音效/音乐

- NativeEvents.sound 默认 HOSTILE；源大量 MASTER，并用不同的 @a 距离、pitch/minVolume。音量滑块、范围和临场感可能不同。resonanceSound/masterSound 的新调用已显式 MASTER，须逐招判断。
- 巫师 BossMusicController 仍按血量比例分六段；源按法杖/六阶段流程换曲。法杖打碎、Boss 血量仍满时，原生音乐可能仍停在上一组。
- 使者音乐按四个 phase；源也有 1000/800/600/400/200 的独立音乐阈值/队列逻辑，不能用四个模型阶段完全替代。
- 暂停/范围变化/重新观察时，原生清队列并可能重放 intro；源玩家记分板队列与循环 timer 的行为不同。

### 目标、伤害与多 Boss

源普遍用玩家选择器、脚点距离和 HurtTime；原生部分通用 Combat 还影响 LivingEntity/NPC，排除 Creative，并按实体盒与 Forge 事件过滤。已经专项改成玩家脚点/源 cube 的新载体应分别认可。多个同类 Boss 使用 UUID 隔离，与源全局 tag/记分板互相干扰也不同；随机 LCG 改 Java RNG 会改变具体顺序。

这些差异并不都适合直接照搬。当前比较仅记录事实；不会据此擅自改变 Forge 取消事件、多人资源事务、世界爆炸/着火等政策。

## 6. 已授权调整与仍待选择的边界

| 类型 | 项目 | 本轮处理 |
|---|---|---|
| 已授权 | 使者约 8 区块范围，96 回航/128 硬边界 | 标为有意偏离源无限制航线，保留 |
| 已授权 | 巨蛇大范围粒子数量优化 | 数量降采样单列；不把优化许可推成缺失所有细节的许可 |
| 已确认 | D34 Seeking 命中后，Forge 取消伤害仍迟缓 | 当前该规则已实现，不再次询问、不外推到所有巫师技能 |
| 已确认 | D45 主手 Semblance 取消伤害仍燃烧；D02 仅恢复流程实际改动且仍管理的模式 | 不扩大到本轮其它 Boss/法术 |
| 待确认 | D28 其它命中附加状态在取消伤害后是否继续 | 仅记录当前差异，不自动批准 |
| 待确认 | C03 方块着火、真实闪电、爆炸世界副作用 | 区别视觉粒子与世界副作用，不自动恢复 |
| 其它历史待决策 | D20 事务、D02 组队/离线、D14 chance、D41 消耗等 | 不在本轮 Boss 演出审计中闭合 |

本轮没有提出新的玩法批准请求，因为任务是比较；差异报告不构成批准记录。

## 7. 证据、覆盖与限制

[逐行证据 JSON](<D:/MinecraftModDev/DEV - 副本/nyctocosm-native/docs/validation/boss-comparison-20261003/source-and-native-evidence.json>)保存：

- 三个命名空间所有函数文件，相关动画 leaf 命令和 effects 引用闭包，共 **6,404** 个源文件记录；**78** 个原生 Java 文件记录。
- 文件 SHA-256、源行号、粒子/声音/移动/状态命令，以及动画目录清单；effects 引用解析无缺失。
- 原始粒子行数仅用于追踪覆盖；wrapper/effects 文件可能重复、分支可能互斥，不将这些行数当每 tick 实际粒子数。调用图是范围上界，人工对条件和关键路径作了上述判断。

[不可变检查记录](<D:/MinecraftModDev/DEV - 副本/nyctocosm-native/docs/validation/boss-comparison-20261003/immutability.json>)记录比较前后源文件、生产代码/资源、工具与 alpha.17 JAR 的摘要。只新增本报告/证据，并将比较成果追加到 TODO。

本轮不宣称自然 AI、生存多人、光影、真实 FPS、区块卸载/晚加入完全一致。表中源码可确认的差异与这些运行表现的待验收项分开。逐像素颗粒运动还涉及原随机数与 vanilla 客户端粒子模拟，需要未来整包实机录像补充。

### 原包证据链接

[S-incur]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/29.mcfunction:3>
[S-twirl]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/attack/twirl.mcfunction:3>
[S-laser-ray]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/attack/laser/__generated__/while/4.mcfunction:4>
[S-laser-hit]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/attack/laser/__generated__/block/209.mcfunction:3>
[S-laser-block]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/attack/laser/__generated__/block/215.mcfunction:4>
[S-beam-ray]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/attack/beam/__generated__/while/7.mcfunction:4>
[S-serpent-death]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/0.mcfunction:3>
[S-serpent-ambient]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/7.mcfunction:3>
[S-serpent-tail]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/28.mcfunction:3>
[S-serpent-whisker]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/serpent/functions/__generated__/block/76.mcfunction:3>
[W-ai]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/sorcerer/functions/ai/entitytick.mcfunction:3>
[W-fall]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/animated_java/functions/ajsorcererm1/zzzzzzzz/animations/falltransition/tree/leaf_25.mcfunction:2>
[W-plane]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/animated_java/functions/ajsorcererm1/zzzzzzzz/animations/rise/tree/leaf_127.mcfunction:2>
[W-element]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/sorcerer/functions/attacks/voidmeteor/__generated__/block/108.mcfunction:6>
[W-death-end]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/animated_java/functions/ajsorcererm2/zzzzzzzz/animations/die/tree/leaf_180.mcfunction:2>
[E-flight]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/emissary/functions/ai/phase/4/fly.mcfunction:3>
[E-grapple]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/grapple/grab.mcfunction:3>
[E-grapple-ride]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/emissary/functions/attacks/phase4/grapple/__generated__/block/728.mcfunction:3>
[E-ambient]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/emissary/functions/__generated__/block/3.mcfunction:3>
[E-transition1]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/phase1_transition/tree/leaf_46_effects_0.mcfunction:1>
[E-transition2]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/phase2_transition/tree/leaf_60_effects_0.mcfunction:1>
[E-transition3]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/phase3_transition/tree/leaf_64_effects_0.mcfunction:1>
[E-death]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/die/tree/leaf_57_effects_0.mcfunction:1>
[E-death-final]: <D:/MinecraftModDev/DEV - 副本/_nyctocosm_source/datapack/data/emissary/functions/__generated__/block/34.mcfunction:3>

### 原生证据链接

[N-spawn]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java:293>
[N-serpent-motion]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SerpentMotion.java:21>
[N-serpent-tick]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:616>
[N-serpent-render]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/client/SerpentWorldRenderer.java:21>
[N-serpent-beam]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SerpentBeamHazard.java:84>
[N-death]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:1196>
[N-boss-ai]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:250>
[N-staff]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NullpointStaff.java:68>
[N-sorcerer-events]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:1029>
[N-sorcerer-fields]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:572>
[N-halo]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SorcererBeamNode.java:1>
[N-hail]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SorcererHail.java:1>
[N-frozen]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/FrozenBlade.java:1>
[N-dash]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SorcererDash.java:1>
[N-ice]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/IceSpikeWave.java:1>
[N-bolt]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java:1>
[N-world-effects]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/client/WorldEffectController.java:28>
[N-particle-loader]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/BossParticleEffects.java:42>
[N-appearance]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryAppearance.java:1>
[N-sword]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissarySwordPlunge.java:1>
[N-emissary-flight]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:792>
[N-emissary-movement]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:858>
[N-emissary-attacks]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java:844>
[N-grapple]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryGrappleSeat.java:44>
[N-hazard]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryHazard.java:1>
[N-celestial]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryCelestialTear.java:1>
[N-pillar]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryPillar.java:108>
[N-rain]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/EmissaryRainShard.java:1>
[N-beam-client]: <D:/MinecraftModDev/DEV - 副本/nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/client/ClientEvents.java:93>
