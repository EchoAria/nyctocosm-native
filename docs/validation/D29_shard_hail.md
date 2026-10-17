# D29 — Shard Hail 独立攻击链

2026-10-02，源码重新沿 leaf_35 → summon → subtick → blocks18–25 核对。

| 事件 | 源参数与原生实现 |
|---|---|
| 35帧 | 根局部(0,2,.6)创建spell-tag冰球；Animated Java先于sorcerer tick，创建世界tick先推进一次 |
| 冰球 | 每tick升1格，timer30孵化18节点；每节点独立初始score0..39；只有冰球可Forbiddance |
| 节点 | 每tick score+1；score41才执行spreadplayers；X/Z方形±15、方块中心、最高安全两格空气地表；上方30格生成冰锥 |
| 多人 | 源选择器sort=nearest但无limit，固定进入位置选出80格内所有非旁观玩家，然后依顺序多次散布；最终落点通常围绕最远者。不能改成只选最近玩家 |
| 减速／预警 | 节点本身1.4格脚部区域Slowness II 10秒；score41的该选择器保持函数进入位置，后续粒子／落冰通过at @s读取新位置。score41..89有36点冰雪圆环 |
| 冰锥 | 与源相同的ArmorStand物理及HEAD模型；落地score2造成玩家脚部半径2、基础Void10，score5删除。命中迟缓仍待D28 |
| 保存 | 新攻击独立保存stage/timer/owner/OnGround/速度/HEAD，不依赖Boss当前动画。score41后重载不会再创建冰锥 |

旧Boss内的HailAge/HailCenter字段仅用于既有原生存档当前攻击的兼容收尾；新施法不再填这些字段，旧NBT恢复回归仍保留。没有扩展数据包存档迁移。

修改：`SorcererHail`、`NativeBoss`、`Content`、`NativeMod`、`SpellBolt`的Forbiddance分支、`ClientEvents`的原版ArmorStandRenderer、内部icicle物品模型。声音新增可指定pitch的masterSound重载，旧调用仍使用pitch1。

红测 `d29-ball-red-20261002.log`：228项，1项失败，35帧没有冰球。绿测 `d29-chain-green-20261002.log`：**232/232 required通过，退出0**。独立原版spreadplayers命令夹具证明多人最终中心，原版ArmorStand逐tick对照证明落冰物理；覆盖18数量、0..39随机分数、分阶段NBT、唯一落冰、节点90寿命、落地2/5计时、2/2.01脚部边界、NPC排除与反制标签差异。

未验收：真实生存／多人战斗、shader模型及粒子、真实chunk卸载／专服重启；D28冰锥命中迟缓取消伤害策略等待答复。本报告不把这些项标为已验收。
