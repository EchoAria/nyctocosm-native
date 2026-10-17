# D32 — Halo / Void Beam 独立 marker 与源射线

2026-10-02。source subtick/raycast/block132 与原版 execute/selectors 对照。

- Halo 八个根坐标节点在5..40帧错开生成，出生即score1；自身65停止瞄准、70发射，67帧统一激活，因此第一发在动画74帧、末发109帧。旧方案75..110遗漏出生tick。独立位置/朝向/age/active/fired/owner保存，动画结束不删除未完成节点。旧原生数组字段仅兼容旧在途攻击。
- Void20帧的aim选择器无limit：40格非旁观玩家分别生成marker，root水平朝向通常最后的最远玩家；30..57逐tick发射，58清理。每次block132的execute forks从同一进入rotation计算，不能把多名玩家的turn依次累加。原版命令夹具验证。
- 两种射线60个半格样点，0..29.5，源passthrough阻挡点仍显示。两个连续dx=0选择箱按玩家AABB相交判定，遇第一玩家即停止，即使Forge取消也终止；旧方案“贯穿第二玩家”有误。目标消失不缩短最大射程。
- Reflectance成本Halo10/Void3，转移damageOwner、沿shield方向继续，marker本身不因ray反射转向；Forbiddance只清除有源spell标签的Halo。玩家伤害仍通过Forge与原版无敌帧。
- 服务端计算命中/反射/路径，单路径网络包传最多60样点；客户端还原halobeaminitial35、halobeamfinal70、halobeambeam38条源数值粒子。纯Java/JSON，未执行mcfunction，未改原包。

红：`d32-marker-red-20261002.log` 237项1失败，暴露数组载体。首次绿：`d32-ray-green-20261002.log` 241项1失败，原版多人execute夹具暴露错误累积turn。最终 `d32-ray-green2-20261002.log`：**242/242 required GameTests通过，BUILD SUCCESSFUL，退出0**，包含D29/30与其余回归。双dx=0边界.54/.56、脚部低1.7、墙样点、反射成本、独立NBT、65锁定/70发射均验证。

实体/渲染注册、NativeBoss、SpellBolt、NativeNetwork、WorldEffectController已接入。网络协议升4，服务端与客户端必须安装同一版本。实际生存/多人/光影/卸载重启仍待验。D28未确认的其他招附带状态未外推为已批准。
