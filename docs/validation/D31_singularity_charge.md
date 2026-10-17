# D31：Singularity Charge 独立弹道与命令上下文

日期：2026-10-02。状态：弹道／终止自动验证通过；命中减速待 D28 用户选择；真实生存战斗待验收。

## 可达链与独立规格

只读源：`datapack/data/animated_java/functions/ajsorcererm1/zzzzzzzz/animations/singularitycharge/tree/leaf_44.mcfunction` → `sorcerer/functions/attacks/singularitycharge/summon.mcfunction` → `subtick.mcfunction` → generated block 63 → `explode.mcfunction` → `hit.mcfunction`。`sorcerer/functions/load.mcfunction` 的基础伤害为 20 Void。

| 事件 | 源行为／本次实现 |
|---|---|
| 发射 | 动画44帧，根局部 `(0,2,.5)`，最近50格非旁观玩家脚部；不是锁定的NPC眼睛 |
| 世界tick | 独立寿命100，先增加计时，仍执行本tick的0.3格移动 |
| 转向 | 在移动后的实体位置选40格内最近非旁观玩家脚部；`normalize(5×前向+.5×指向脚部)` |
| 函数坐标 | `tp @s` 不改变外层 CommandSourceStack。近炸、体素、hum、爆炸、反射选择仍基于本tick**进入函数时的位置** |
| 终止 | 进入位置3格内玩家／非passthrough体素／第100tick均进入爆炸，不能静默discard |
| 爆炸 | 进入位置脚部半径5闭球、玩家、20 Void；保留Combat／Forge、无敌帧和友伤边界；NPC不作为爆炸伤害目标 |
| 反射 | 在爆炸检查后才执行，容量成本20、转移伤害归属；反射tp从函数进入位置沿盾方向0.3格，不能叠加在已移动位置上 |
| 命中状态 | 原包 Slowness II 10秒仍未启用，等待 D28 取消伤害策略确认；D34的单招确认不能扩展为D28全部已批准 |
| 视觉 | 充能xflat portal速度环90样点、飞行reverse portal90样点／5黑尘、爆炸emitter与源音效；静态数值和客户端数学，不执行mcfunction |
| 保存 | flight profile、剩余寿命、owner／方向／反射时间和爆炸one-shot；旧**原生**WeaponVisual=4存档获得新源策略。没有数据包存档迁移 |

## 反例与验证

- `d31-visual-red-20261002.log`：223项中仅 timeout 反例失败，旧原生到期丢弃而没有5格爆炸。
- 第一版D31测试误把新位置当所有命令的上下文。重新读取block63/explode并加原版命令夹具，不保留错误预期。
- `d31-context-red-20261002.log`：226项中2项失败。位于前方3.15格的玩家在实体移动后只有2.85格，但原版函数上下文仍在旧位置，不近炸；另有2.99格横向边界反例。
- `singularity_charge_selectors_keep_function_entry_position` 用同版本原版 `tp` 和选择器命令共享CommandSourceStack，独立确认源语义，再与原生弹体比较；覆盖前方3.15／后方-2.9、到期爆炸后方4.99、20容量反射的重定位、下一tick才检查进入石块。命令只在开发测试运行。
- `singularity_charge_moves_then_turns_and_restores` 覆盖先移动后转向、独立5/.5方向预期、NBT与旧原生profile恢复；其余测试覆盖3.01/2.99、99/100计时和发射独立载体。
- 完整 `run-d31-context-green-20261002`：**226／226 required通过，退出0**。此前 `run-visual-repair-green3-20261002`：225／225通过。

## 范围

不声明D31整项验收。待D28状态、D22非默认属性、多人反射／保护模组事件、真实区块卸载和客户端生存战斗。NBT往返不是专服重启。该修复为用户新增“逐招对照”请求的一个实际切片，D29/D30/D32/D33独立攻击仍继续跟踪。
