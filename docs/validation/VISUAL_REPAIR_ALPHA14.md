# alpha.14：客户端可见性、材质、灵魂演出与法术对照

日期：2026-10-02。响应用户alpha.13实机反馈及“重新检查并编译一版mod”。本报告区分本阶段修复、隔离客户端验证和仍未闭合的逐招行为。

## 本阶段实际修改

| 用户反馈 | 原因／修改 | 已验证范围 |
|---|---|---|
| 蛇头埋入方块后身体消失 | 标准实体通道以头部区段可见性及根位置光照处理整个模型，`shouldRender=true`仍不足。身体改在AFTER_ENTITIES按42段独立视锥、距离和光照绘制 | 新世界实体石块包住头部，露出的整段身体仍在截图780可见；部件伤害规则保持原实现 |
| 使者二阶段提前换材质 | 按用户明确要求新增12个wing_phase2模型：继承原config1的几何／隐藏翼，但可见纹理使用config0调色板。三阶段才使用config2；材质选择先armor再wing再sword，防止armor覆盖武器 | 一／二阶段截图110/220相同翼调色板，二阶段持剑可见；三阶段采用后续变体。该二阶段调色板是**用户覆盖原包config1**的要求，不冒充源码原状 |
| 使者后期飞出视野 | source目标选择不要求视线；飞行中心随有效目标更新并保存，无目标时保留最后中心；96格开始回航，128格硬上限。乘客不拖动飞行中心 | 红测单tick从127.8漂到132.299；新飞行边界GameTest通过。无新永久强加载；真实渲染距离低于8区块仍需用户调设置 |
| 巫师开场前方弧片 | ajsorcerer默认模型包含smear1；原包initial_spawn第0帧staffless会隐藏它，但旧渲染没有执行变体时间线 | 原版apply_variant数值导出，Sorcerer三个rig按动画帧应用；红截图410有弧片，绿截图410无弧片；没有删除后续招式的合法斩击模型 |
| 传送门声音／抖动有，粒子无 | 隔离默认客户端**原本就能显示**门粒子，不能声称已复现用户渲染组合。原粒子依赖不可见载体实体tick；迁至独立客户端tick控制器 | 夹具主动对client PortalActivation设置canUpdate(false)，年龄同步／粒子仍推进，截图585/635可见。用户模组版本／粒子／光影环境仍待提供 |
| 巫师插杖死亡缺灵魂粒子 | 缺少源soul_swirl helper及动画直接粒子；通用紫点不能代替源轨迹 | 补56／116帧各15条独立灵魂轨迹、116帧ink300、20条void vein及三段shockwave。ajloadtick先于effects，因此shockwave实际在动画116／119／121帧。最终15条客户端截图及日志见manifest；shader环境待验 |
| 巨剑只有爆炸没有模型 | .1×.1载体的默认距离限约6.4格，巨大HEAD模型仍被默认裁掉 | 扩展模型包围盒及256格视觉距离，按源HEAD×10、yaw/pitch、sky10/block0显示；红490无剑，绿490有巨剑。D06放火／完整shockwave仍未闭合 |
| 巨蛇大范围粒子掉帧 | 20柱旧服务器每tick产生4800个连续粒子并广播；与伤害计算无须绑定 | 观察端局部生成，共享600个新粒子/tick上限，20柱减少87.5%；1..80高度覆盖与脚部5格伤害保持。没有硬件FPS前后基准，不把数量减少当帧率测量 |

关键文件：`ClientEvents`、`RigLibrary`、`SerpentWorldRenderer`、`WorldEffectController`、`NativeBoss`、`PortalActivation`、`EmissarySwordPlunge`及其renderer、`SerpentBeamHazard`、`BossParticleEffects`、`SoulSwirlEffects`、`VoidSwirlEffects`、`VoidVeinEffects`、`SpellBolt`、`SpellArea`。发布JAR排除ClientSmoke/NativeGameTests。

`export_rig_variants.py`仅给既有rig补变体时间线与12个指定材质模型；没有运行全量import_assets。`export_boss_particles.py`编译8条动画的4100条粒子数值指令及选定helper，运行时没有读取／执行源函数。源摘要及未宣称完成的独立helper见 `rig-variant-import-20261002.json`、`sorcerer-particle-import-20261002.json`。

## Sorcerer十二招逐项对照状态

| 招式 | 本阶段或已有正确实现 | 尚未闭合 |
|---|---|---|
| Halo Beam | 已有动画、节点及NBT | D32：各节点年龄／停止瞄准、源长度、反射／Forbiddance；不是补一个环就完成 |
| Shard Hail | 本阶段恢复原动画冰块／Void物品充能粒子，替换旧通用充能点 | D29：独立上升球→18地面节点→物理冰锥、晚选玩家／spread、触地2tick；D28状态 |
| Frozen Blades | 既有发射框架 | D30：两手源节点、持续追踪弧面、递归子步及源充能helper；当前普通直线弹不等价 |
| Singularity Charge | **本阶段实际修复D31**：源44帧发射、.3移动、5/.5转向、3格近炸／100tick／5格爆炸、命令进入坐标、20容量反射、充能／飞行速度环 | D28 Slowness II、D22非默认属性、多人实机 |
| Void Meteor | 既有D26六发／25tick／三子步追踪 | D28黑暗、完整独立tear视觉及反制验收 |
| Dash Strike | 本阶段恢复源直接动画粒子／模型变体 | D33固定世界落点、结束回点、源位移／命中；骨骼定位点粒子需独立对照 |
| Void Wave | 既有D36世界Y+2／6格玩家脚部球 | 本阶段补直接keyframe粒子；D28状态及源其它helper待验 |
| Javelin Swing | 既有D36局部(0,1,2)／3格脚部球；本阶段恢复slash/staff变体 | 原完整演出与D28状态待验 |
| Air Dash | **本阶段补150点dashfly图形×19动画帧**，删除重复通用上升紫尘 | D33锁点／沿源路径／提前跳slam帧；生存战斗待验 |
| Void Beam | 既有逐帧射线表现 | D32独立平滑marker、源长射线／反射与生命周期，旧实时瞄准不等价 |
| Seeking Singularities | 既有D34八发／.24追踪／70–89tick；用户已明确取消伤害仍给该招5秒迟缓 | 真实战斗及D22动态属性；不能扩展该批准到D28全部 |
| Ice Spikes | 既有D35独立波面、30臂×5环及模型 | 真实卸载／重载／晚加入、D28状态；本轮修了会自然消失的测试施法者夹具 |

以上表格是明确续接清单，**不宣称十二招或51项已全部还原**。玩家17个法术的其余D13/D14/D20–D23既有缺口也没有借视觉修复标记完成。

## 隔离验证与失败记录

所有世界均新建，禁止打开／复制用户存档。客户端固定新世界VisualRepair、原版渲染、无外置shader，自动脚本最后退出；该脚本仅在开发property启用，发布包排除。

- 红客户端 `run-visual-repair-red2-20261002`：真实发现开场弧片与巨剑缺图；传送门基线粒子已有显示。
- 飞行红测 `visual-repair-flight-red2-20261002.log`：221项中边界反例失败132.299；此前red首轮错误动画夹具日志保留，不算生产反例。
- `visual-repair-gametest-green-20261002.log`：222项，死亡116粒子预期失败；按实际deathpoof调用补编译helper，保留失败，没有删测试。
- `d31-visual-green-20261002.log`：225项，两项测试远处玩家所在chunk尚未entity-ticking；等待加载并在独立arena复验。
- `d31-visual-green2-20261002.log`：225项，新D31全部通过，2项既有夹具失败（蛇120格目标chunk未准备、Ice施法Zombie自然despawn）。仅修夹具准备／生命周期，保留时序、距离、命中断言。
- `visual-repair-gametest-green3-20261002.log`：**225／225通过**。
- `d31-context-red-20261002.log`：226项，2项源命令上下文反例失败；修正后 `d31-context-green-20261002.log`：**226／226通过**。
- 多轮客户端 `visual-repair-green2`、`green3`、`alpha14-client` 的完成标记／截图均保留；第一轮有前一个场景的長寿命蛇柱粒子遮挡充能，最终夹具在指定场景切换后清除旧粒子。不能拿遮挡截图当充能效果验收。

封包复核发现初版死亡helper仅接入single，源block628实际15次single调用，已纠正。`soul-count-red2-20261002.log`：227项，数量反例失败，总轨迹点70而原15条的合法范围600–1650。并补正半径每tick30%增长（包括延迟期）、先减延迟／再激活、emit保持move函数进入位置；VoidVein同样在进入位置发粒子，第30tick kill后仍有直接粒子命令。`soul-count-red-20261002.log`保留该坐标／寿命反例。Portal与Sorcerer共享正确15条计划，去除两份公式漂移。第一次生成的alpha.14仅作为`pre-soul-count-alpha14-20261002.jar`证据备份，不是交付包。

最终 `visual-repair-alpha14-soul-final-20261002.log`：**227／227 required通过，build/reobfJar成功**。最终客户端 `visual-repair-alpha14-soul-client-20261002.log`：1320tick捕获完成、正常退出，895截图确认15条灵魂大范围演出。最终JAR **55,983,723 bytes／3063 entries**，SHA-256 `d1ac9901cfde0b4b19dda39cdfdd4e72cdc0fd0bccfba1704ca0c12b66e4c55f`；审计errors=[]，无开发测试／runtime函数，class major61。完整字段／源和实现哈希见 `visual-repair-20261002/manifest.json`及TODO阶段74。

## 待用户与手工验收

1. 待用户：D28 Forge取消其他术士法术伤害时的附带效果选择。保持D34/D45已确认策略，不自动外推。世界火块、组队补偿、祭坛事务等历史待决项继续保留。
2. 待环境信息：传送门问题时的版本、粒子设置、渲染距离、shader／渲染优化模组；默认隔离客户端可见并不证明用户组合已修好。
3. 用新JAR测试：蛇头进入实体山体而身体在外；使者二→三阶段调色板，飞行开招／失去视线后是否留在战场；巫师完整开场／插杖／消散；巨剑下落、驻地105/110tick爆炸；开门三波粒子／最终灵魂；20蛇柱多人战斗帧率。
4. NBT夹具没有替代真实存档重启、实际区块卸载／重载、多人、保护插件及shader验收。新JAR没有自动安装进mods。
