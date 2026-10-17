# D33 — 固定落点与逐帧根位移

2026-10-02。源码dashstrike/airdash实际调用，与原版facing/LocalCoordinates命令夹具对照。

| 招式 | 保存／位移 | 玩家脚部伤害 | 粒子原点 |
| --- | --- | --- | --- |
| Dash Strike | 37水平面向最近30格非旁观玩家，保存前15格安全地表点；41..48每帧向固定点tp1.5；49再走1.5后到保存点；50不移动 | 41..48进入位置1.5；49进入位置按根rotation前1.5半径2及最终点1.5两次；50进入位置1.5；Void15 | 骨骼/root命令位置使用进入位置，49蓝尘使用最终位置 |
| Air Dash | 1..4升.3、5..12升.6、13..16升.3、17..19升.1，总7.5；20保存目标安全地表点；26..39每帧向固定点tp1.5；40到保存点 | 飞行段不伤害；40函数进入位置4格，Void15；至多一次 | 升空／移动／slam仍使用函数进入位置；20双dashfly2 |

源Air Dash接近分支写`aj.frame=39`，但ajsorcererm2新版引擎只增加`aj.ajsorcererm2.animation.airdash.local_anim_time`并复制到`aj.anim_time`，apply_frame只读后者；rig中没有aj.frame引用。此前“能提前跳slam”方案错误，没有添加源外的提前落地玩法。沿固定点移动可能越过／折返，40仍正常结算。

新增SorcererDash保存世界点、锁定yaw、finished、原gravity；持有期间NoGravity/零velocity/停止navigation、避开staff hover双重根位移。结束、换招、死亡恢复原gravity；新NBT恢复继续原点而非跟随新目标。安全落点保留精确X/Z，受阻才有界8格地表搜索，不清块，不退回地下传送。旧原生在途攻击缺锁点不捏造新的目标点，未做旧数据包存档迁移。

源facing角度＋lookup局部坐标使视觉“正前方”有微小横向偏移。初绿夹具错误使用理想Z轴，独立原版tp证明真实落点，修预期夹具而非放宽1e-6边界。客户端每帧只收固定大小位置/cue包，按源数值表生成粒子，避免跟随插值漂移或每点服务器广播；Air20新增320点双dashfly2＋1flash，26..39每帧35尘，40正常3粒子。

红`d33-motion-red-20261002.log`：244项2失败（升空无实际位移、Dash追实时玩家）。初绿244项2失败，第二绿248项3失败，均为理想坐标预期；保留日志，使用原版facing/tp代替理想值。`d33-motion-green3-20261002.log`：**247/248通过，六项D33全部通过**；唯一既有Celestial Smite共享测试区射线在6步提前终止，未直接记录阻挡块／旁观玩家，不能据此认定生产偏差。移到独立arena后首次复验因加载用旁观者站在起点而1步终止（源码选择器包含旁观者）。移除加载玩家，以测试region ticket加载，保持20/150原边界与断言，最终全套复验单独记录。

D28附带Slowness II取消伤害策略未批准，未外推D34选择。真实生存/多人/卸载重启/光影仍待验；手动客户端source-frame烟测不等于自然AI生存战斗验收。

最终 `sorcerer-repair-alpha15-final2-20261002.log`：**248/248 required通过、BUILD SUCCESSFUL，退出0**，build/reobf成功。六招与全部原反馈场景在两个独立新世界截图核对，发布包证据见[SORCERER_REPAIR_ALPHA15](SORCERER_REPAIR_ALPHA15.md)。
