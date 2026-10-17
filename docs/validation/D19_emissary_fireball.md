# D19 Fireball 子步、选靶与超时验证

日期：2026-10-01。此报告记录 D19 的已完成切片；D19 仍在实施中。

## 原包核对与实现

- `phase4.attack.fireball` 的第 8、18 帧分别调用 `shoot1`、`shoot2`，从右手、左手 locator 发弹。生成函数 711/712 对每发弹独立选择以发射点为中心 100 格内、非旁观且没有 `emissarydebug` 标签的随机玩家，朝其脚部瞄准。原生发射点仍使用 D18 导出的手部 locator；选靶过滤和脚部方向已对齐，空选择时保持 locator 朝向。
- 原包火球动画结束后由第四阶段选招再次抽签；地面池的 `AttackChoice=66..85` 才会选择 Fireball，飞行池没有 Fireball。移除了原生在第 20 帧以 50% 概率直接重播同一招的额外逻辑。左右手两发的源帧仍为 8、18。
- `SpellBolt.FlightProfile.EMISSARY_FIREBALL` 每 tick 检查两个一格子步，并以实体扫掠及 D16 的 `#nyctocosm:passthrough` 方块规则处理碰撞。life=60：前 59 tick 最远前进 118 格；第 60 tick 不再移动，产生一个 `TIMEOUT` 终端 impact。方块、实体、超时三条路径保留显式原因和一次性终端保护；反射改变方向但不改变子步/计时。
- 尚未实现原包 `impact.mcfunction` 中的 3×3×3 `fire replace air` 方块放置。它会改变世界；在用户选择是否实施及 Forge 保护处理方式前，不能当作已批准。终端视觉/实体伤害继续沿原生 impact 通路处理。

## 自动验证

专用 GameTest 包含速度/双子步、59 tick 距离与第 60 tick 零位移、实体扫掠、首个阻挡体素、反射后子步，以及 100 格随机选靶过滤与脚部瞄准；火球 locator 测试也确认第 20 帧不会生成同招额外齐射。D19 用例均通过。

完整 GameTest `run-d19-fireball-targeting-final2-20261001`：**215 项，211 项通过**。剩余 4 项失败为 `flame_geysers_caps_at_two_players_without_cross_spreading_node_groups`、`flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`、`ice_spikes_first_ring_hits_on_source_tick_seven`、`flame_slash_advances_point_8_and_keeps_twenty_point_radius_two_surface_for_40_steps`；均不经过 Fireball 代码。日志：[`latest.log`](../../run-d19-fireball-targeting-final2-20261001/logs/latest.log)。

`compileJava` 和 `build --offline` 均成功。Alpha.12 JAR 审计 `errors=[]`，3039 项，55,936,809 bytes，SHA-256 `e6d49d3838582607255c789ac7fbb41124a531071bd276b519b87a2377df832f`；未包含 GameTest、ClientSmoke 或运行时 mcfunction。审计快照：[`before`](../native-audit-before-d19-targeting-20261001.json)、[`after`](../native-audit-after-d19-targeting-20261001.json)。覆盖前的 alpha.12 JAR 已备份到 [`pre-build-backup`](../../run-d19-fireball-targeting-final2-20261001/pre-build-backup/)。

## 尚未验收

- 火焰方块策略等待用户选择；在该决定落地前，D19 不能标为完成。
- 当前验证是服务端 GameTest；客户端视觉、多人战斗、服务器保护插件交互和真实存档体验尚未进行。
- 独立 GameTest 有四项非 D19 失败；其错误在完整日志中可追溯，不能声称全套通过。

主要实现文件：`NativeBoss.java`、`SpellBolt.java`、`NativeGameTests.java`。构建保持 alpha.12；未修改源数据包、真实存档、相邻 MDK 或已安装模组，未提交或分发 JAR。

## alpha.13 后续复查（2026-10-01）

上面的alpha.12数字保留为历史。本次重新核对终端源代码，修正此前通用impact遗留的半径3、NPC命中、实体燃烧和额外推力。Fireball现在仅伤害脚部半径5内的非旁观、非debug玩家，保留正常Forge及难度管线；碰撞候选也排除NPC/debug。新增水平/垂直5格闭边界、5.01格反例、无敌、抗击退和碰撞候选回归。两个不同种子的隔离世界均219/219通过，alpha.13构建审计无错误；完整证据、源/实现SHA-256和产物见 [项目复查报告](PROJECT_RECHECK_ALPHA13.md)。

世界放火选择仍未确认，客户端/多人/保护插件、真实重启和逐子步源选择箱对照仍待验；D19保持实施中。
