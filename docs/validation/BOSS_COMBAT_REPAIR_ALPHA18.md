# alpha.18：boss 对照后的高影响战斗修复

日期：2026-10-03。用户授权选择此前完整对照中容易修复、影响战斗体验的部分。状态：**实现和276项自动回归、隔离客户端切片检查完成；自然战斗仍待实机验收。**

## 本轮实际修复

| 部分 | 原包可达行为 | 本轮实现与验证范围 |
| --- | --- | --- |
| 使者七枚 Seeking Missiles | 2/4/6/8/10/12/14帧建立悬停节点，23帧统一激活；激活时寿命60–89，每tick计时减1、最多两次.7移动；61以上先随机摆动，60以下找300格内最近非旁观、非debug玩家眼部，以6/.5原版构造转向 | 专用服务端实体，恢复悬停图案44行、激活Flash、四类尾迹、MASTER声音、端点cube命中和15 Dissonance。超时无额外爆炸伤害。保存激活/寿命/旋转/owner；反射消耗容量、保存新伤害owner，继续双子步；禁锢术第三tick反制入口接入新实体，按反射后的owner防止误消除同owner法术。GameTest独立执行vanilla命令比较轨迹，保持Forge伤害路径 |
| 巫师地面选招与位置 | phase2/4/6距离≤7走近招，8–20走远招；(7,8)和>20不选招，不导航追人 | 去除这些阶段的vanilla追逐，保留技能自身移动及既有安全落地。静态边界与真实customServerAiStep入口均测试；没有改悬浮阶段或50格索敌半径 |
| 巫师死亡 | die最终结束回调在180，133后的停留也是演出的一部分 | 从133延长到180；不提前移除或交付胜利奖励。NBT在140保存恢复仍续到180，不重播开头。旧死亡/奖励用例按源结束帧修正；D37致死提交是否等待当前攻击结束、Nullpoint原始伤害问题仍未处理 |
| 巫师法阵与Voidwave | voidplane冲击波1000数值行，Voidwave transmittance冲击波1130行，score1/4/6发射；focus已有原动画直接粒子 | 恢复独立6tick冲击波载体、法阵MASTER音效；保留Voidwave原ExplosionEmitter/Flash。移除focus额外FallingTear/Flash近似效果，继续使用已编译的源关键帧粒子 |
| 法杖Nullpoint | 每tick按20/40…260生命阈值各发一个紫尘，size1，Y+2，spread(.3,1,.3)，speed0，force；召回25tick后SquidInk300 | 从每4tick粗略比例改为原阈值/位置/频率，恢复召回墨汁与MASTER召回声音。没有改法杖伤害结算、保护策略或阶段推进 |
| 使者转阶段 | phase1_transition23行、phase3_transition30行直接粒子，phase1/2/3分别2/4/9个MASTER声音；翼尖3是发射节点 | 导出原值并按实际帧/locator播放；补left/right upper wingtip3为18个locator。保留已恢复的171帧armorless和用户96回航/128范围。第一转阶段的lightning explosion_sparks辅助模型仍未补 |
| 使者地面砸击 | ground_slam7帧，Lava5与Stone100 | 接入原数值两行与局部前2.5位置；原伤害帧和近战筛选保留。未把其它地面连招或翼部常态火焰视为已完成 |
| 使者死亡 | 0帧begin MASTER10/100；27次death_swirl，57右手与72左手crucify，100最终900行粒子和finaldeath MASTER50/300 | 恢复三类旋涡尾迹、双手十字82行和最终爆散；客户端效果独立于Boss实体存活。旋涡保留原运动/随机区间，使用每次事件种子，并未复刻原包全局LCG随机流。最终爆炸网格模型仍缺失 |
| 巨蛇头尾黑雾 | 头Y+1.5/前2，size2/count30；尾Y+1.5/后2/6/10/14/18，size3/各5，speed0/force | 恢复头部参数及五点尾雾。柱状光束600共享预算、160格视距及按身体段绘制保留；没有增加大范围beam粒子数量。音效/出生/死亡和部分激光密度仍属原对照剩余项 |

### 对照报告纠错

原报告把使者地面侧移10–14帧的`.3`慢滑列为可达差异。复查发现动画实际调用`dash_skid`，而只读原包存在的文件名是`dash_slow`；左右两侧相同。`.3`辅助函数并未从动画解析到。本轮撤回拟加入的尾段移动，保留可达的0–9帧移动，新增回归保护此行为，并更正原对照表。无需把原包悬空引用当作已经批准的玩法修复。

## 证据与失败记录

证据目录：[boss-repair-alpha18-20261003](boss-repair-alpha18-20261003)。原完整对照与6404源文件索引保留；其表格仅纠正上述可达性结论，不把alpha.17历史结论冒充alpha.18现状。

- `before.json`保存2695个src/main/build.gradle基线文件、只读源29657文件及11个旧JAR摘要。
- `red.log`首次因新测试模板前缀错误失败，属于夹具错误；修正后`red2.log`中原262项通过，3个新反例按预期失败：飞弹提前移动、7–8格选招、180前移除。
- `green1.log`三个失败：局部角度精度、反射夹具碰入玩家选择箱、旧奖励测试按133结束。移动改用原版局部坐标计算，保留独立命令对照；修正夹具与源时长，不降低边界。
- `green2.log`两个飞弹夹具失败；原最近玩家范围300，测试场地彼此过近。改为每场1024格间隔，并在生产查询排除已removed玩家；未减小300格范围。
- `green3.log`274/274通过。最后补左右侧移悬空回调用例，`final.log` **275/275 required通过，BUILD SUCCESSFUL，退出0，3m57s**。
- 交付前补查源spell/projectile标签，接入新飞弹的禁锢术第三tick反制与反射owner识别；新增第14项回归，最后`final-counter.log` **276/276**通过，3m51s。此前275项结果保留为阶段记录。
- `particle-import.json`记录选中源文件逐文件SHA和粒子/声音数量。选中直接粒子55行、helper3156行；使者locator完整44动画/2601帧/18节点重新导出。运行时没有mcfunction依赖。
- `static-audit2.log`错误列表为空。发布审计核对资源字节、Java17 class major61、排除全部测试/烟测类及执行式mcfunction；包摘要最终另写下方与manifest。

可复现命令：

```powershell
.\gradlew.bat runGameTestServer build --offline --console=plain -PgameTestRunDir=<新的隔离目录>
.\gradlew.bat runClient --offline --console=plain -PbossRepairSmoke -PbossRepairRunDir=<新的隔离目录>
python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.18.jar
python tools/package_boss_repair.py
```

GameTest与客户端使用缓存Java17工具链，未修改系统Java。烟测仅新建指定目录下的BossRepair18世界，拒绝复用已有同名世界；未安装至用户mods，未触及真实存档。

## 客户端切片

首轮`client.log`运行退出0，1m31s。悬停期七个节点、激活轨迹、转阶段粒子、双手十字、最终死亡爆散、Voidwave和巫师死亡停留已实际截图。510/520 tick时Boss数0但粒子仍约1.25/1.30万，说明死亡粒子在移除后延续。138/179死亡年龄日志均为`removed=false, clip=die`。这些是可见演出与事件链证据，不是FPS测试。

扩展轮`client-final.log`虽退出0且有头尾雾、voidplane/召回墨汁截图，但夹具在ground_slam已结束后仍反射调用攻击方法，出现attack为空的Server task错误；该轮不能作无错误验收。修正夹具为仅在原招式动画仍活动时驱动关键帧，保留全部失败输出与截图，使用第三个新世界重新检查，第三轮`client-verified.log`在新建世界无Server task错误、无渲染异常地完成，退出0，1m53s；37张截图覆盖九个场景。客户端场景使用NoAI/免伤飞行玩家及手动关键帧；并未证明自然AI、多人数目标竞争或整场原包视频完全一致。

## 实机验收与剩余项

1. 同版alpha.18客户端/服务端，在使者地面近战阶段等其完整使用Seeking Missiles：应看到七节点蓄积再齐发，玩家移动后飞弹追踪；用Reflectance核验反射/容量。
2. 与巫师地面形态保持6–7格、7–8格、8–20格、20格外，观察其固定位置与选招差别；移动技能仍应执行。击败后等满180tick演出再确认奖励。
3. 观察巫师法杖紫尘随血量减少、回收墨汁、Voidwave冲击波，以及使者三个转阶段和死亡的旋涡/双手十字/爆散。
4. 巨蛇身体和尾雾应持续可见；多条柱状光束仍受原先共享预算限制。记录实际FPS/粒子设置并反馈，没有真实FPS基准前不声称完全解决掉帧。

仍未闭合：使者常态翼火和phase4眼/手/脚/halo、Wing Blast/Celestial Strike等剩余效果，第一转阶段电火花和最终死亡爆炸网格；巫师其余共同元素特效、法杖与致死提交D37；巨蛇其余出生/死亡/音效与激光密度。D28/C03等取消伤害附效与世界火焰、D20事务、D02组队/离线、D14 chance、D41成本等待选规则未自动批准。D34与D45旧确认不外推到其它法术。

没有实测：多人自然生存、保护模组、光影、晚加入、真实专服重启/区块卸载和FPS。NBT及卸载原因用例通过，不将其等同真实重启集成验收。D18/D25保持分项实施中，D38新增切片自动验证通过待实机；51项没有整体勾选。

## 开发包

客户端结束后补查专用飞弹的禁锢术反制集成，增加第三tick/反射owner回归；`final-counter.log`以最新生产代码 **276/276 required通过，BUILD SUCCESSFUL，退出0，3m51s**。客户端演出资产没有因反制入口补充而改动，客户端截图不当作这项反制的验收证据。最终构建、包审计及SHA由manifest与阶段87记录。版本`0.3.0-native-alpha.18`，协议6，两端需同版；alpha.17及更早JAR保留。

- 路径：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.18.jar`
- 大小：**56,163,014 bytes**；3091 entries；Java class major61。
- SHA-256：`060b9cd423500d2bddbb0175e5a98f0fbe4f65f02742238c7b7f7b019bdb137e`。
- `release-audit.log` / `jar-audit.json`：errors=[]，无测试/烟测类、无runtime mcfunction，新增资源字节与源工作区一致。
- `manifest.json`：只读源29657文件总摘要及11个旧JAR均未变，13个既有基线文件改变、6个生产文件新增，0删除；开发夹具摘要单列。最终客户端37张截图与各次失败日志均保留。本轮没有安装、提交、推送或关机动作。
