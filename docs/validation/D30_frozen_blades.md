# D30 — Frozen Blades 双追踪波面

2026-10-02。34帧throw → summon → block78 → procast两次 → hitbox12分支 → 79清理；source frozenbladesdamage=14，实际Void通道。

源骨骼为乘坐root的ItemDisplay，tick_as_root先把所有passenger tp到root并复制根rotation；leaf_as_bone只改显示矩阵，不能改变execute at的实体坐标。throw内部at重置外层leaf局部(0,2,1)：真正出生是root世界Y+3，yaw±90／pitch+20后局部前.5。充能是两侧世界Y+3.5、前1.7，各10个源冰点；32帧sweep两侧各2点。

每tick两次procast，第一次以**函数进入位置／朝向**采样；12个pitch−60..+50、步10度、前1.5的纵向弧点，每点玩家脚部半径2。每个hitbox内bladehit去重，递归第二hitbox重新允许尝试，原版无敌帧仍保留。遇人／阻挡只是标kill，仍完成当前两次procast；100tick也完成最后一次攻击后玻璃声／删除。

一次.4移动发生在两次采样之间。首次转向tp随后被继承进入rotation的移动tp重置，第二次endcast转向才保留。最近50格非旁观玩家脚部，5×当前方向＋.5×瞄准方向，经原版两次facing计算；用Mth lookup和LocalCoordinates公式，不用理想三角函数制造源外的转向精度。反射在首次移动之前，成本14，转移owner／复制shield朝向；原spell标签允许Forbiddance。NBT存age／owner／damageOwner／朝向／位置，动画结束后波面独立续行。

红 `d30-wave-red-20261002.log`：233项2失败（双波面缺失；另一个既有Emissary加载夹具失败，下一隔离运行通过，未删除）。`d30-context-red-20261002.log`：234项1失败，暴露首次转向被错误累加两次。`d30-wave-green-20261002.log`：236项2失败，暴露Mth/LocalCoordinates与理想角度的差别；未放宽边界，换成独立原版局部坐标和tp夹具。`d30-wave-green2-20261002.log`：**236/236 required通过，退出0**。

新类FrozenBlade/SorcererCastMath，注册／Boss发射／Forbiddance／客户端空载体。真实生存／多人／shader／卸载重启未验，D28命中Slowness II 10秒取消伤害策略待用户；没有当作已批准。
