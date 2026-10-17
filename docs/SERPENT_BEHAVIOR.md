# Serpent 行为规格与验证边界（alpha.9）

本文件描述本轮已移植的头部运动、逐节身体和光柱调度，不宣称整个 Boss 与原包完全等价。
设计来源为工作区旁边只读的 `_nyctocosm_source/datapack/data/serpent/functions`。

## 持久状态

`SerpentMotion` 替代原包四个 AI marker，运行时不创建 marker 或记分板。

| 原 marker/状态 | Java 状态 | 生命周期 |
| --- | --- | --- |
| `positionarea` | `area` | 首次活动时固定，以此为游弋中心；存档保留 |
| `currentfacing` | `current` | 跨 tick 保留，蛇头先读取旧值，再重置和修正 |
| `towardfacing` | `toward` | 每 tick 从移动前的蛇头朝 `target` 投射 30 格 |
| `target` | `target`、`targetYaw` | 游弋随机点、盘旋螺旋、Dive 延迟目标或 Laser 扫描点 |
| `serpenttarget` | `targetId` | 一次攻击锁定一个目标；丢失目标后安全回到游弋 |
| State/Attack/Interp/Speed/Timer | 对应整数字段 | 每条蛇独立，并随 NBT 保存 |
| `twirling` 与定时函数 | `twirling`、`age` | 创建攻击时立即执行第一个盘旋步；随后逐 tick 执行 |

没有临时辅助 marker/盔甲架。`SerpentBody` 保存 40 节身体和独立尾巴各自的坐标、yaw/pitch，首次召唤与原包一样重叠。每 tick 发送约 0.85 KiB 的相对坐标/角度快照，客户端插值；存档保留 double 坐标，避免读档或重新进入视距后变成笔直长尾。

头部和身体模型使用 `RigTransforms.armorStandHead` 还原原头部变换；俯仰绕头部支点旋转，不绕实体脚底旋转。客户端以原版渲染器在 -80°、-30°、30°、80° 四角度核对矩阵。

## 逐 tick 顺序

对照 `headai`、`move`、`ai`、`__generated__/block/200,201`：

1. 蛇头朝上一 tick 的 `currentfacing` 转向。
2. 在移动前的位置重建前方 30 格的 `currentfacing`，并重建指向旧 `target` 的 `towardfacing`。
3. 更新攻击状态与目标 marker 的攻击运动。
4. 蛇头按速度档固定移动 1、2 或 3 格，可穿过方块。
5. 修正 `currentfacing`：Interp 1 朝 `towardfacing` 前进 4 格，距离至少 4.1；Interp 2 朝旧 `target` 前进 3 格，距离至少 3.1；Interp 3 朝 `towardfacing` 前进 6 格，距离至少 6.1。
6. Dive 才将 `target` 更新为被锁定目标的最新脚底坐标。因此修正保留一 tick 延迟。
7. 执行已排程的盘旋步。角度使用 Minecraft 的 float 角度、环绕规则与三角函数查表。

## 时间轴

| 状态 | 来源与行为 |
| --- | --- |
| 游弋 | block/193–196；每 20–49 tick 更新一次相对固定区域中心的随机点，XZ 约 ±30 格、Y 约 ±3 格。目标在 130 格内时扣攻击冷却；冷却为 100–199 tick |
| 选招 | 三种攻击 `1 + random.nextInt(3)` 等权。每次攻击从 200 格内可攻击玩家中随机锁定一个；仍允许正常原生生物作为受击/反击目标 |
| 盘旋 | `attack/twirl` 与 while/0–2、block/202–203。目标由蛇头当前位置向上 10 格、沿当前朝向前进 10 格，yaw 加 90°；每步再上升 1 格、yaw 加 9°、前进 3 格，共 61 步。不是围绕玩家旋转 |
| Dive | block/204、197–198。Interp 2、速度 2；进入后最多跟随 400 tick；离固定区域中心 200 格也退出。正对目标时可直接穿越并继续飞出，而不必人为强制掉头 |
| Laser | block/205–207。目标先置于玩家朝蛇头水平方向 20 格处，朝向反转；之后每 tick 沿固定方向前进 0.5 格，持续 100 tick。并非平滑追踪玩家眼睛。蛇头速度 1，最后一刻恢复速度 2；射线按原 `air` 标签每格检查，最长 150 格 |
| Beam | 盘旋后恢复游弋，攻击冷却置 20 tick；开始时创建 20 个延迟 140–279 tick 的光柱，后续独立运行 |

## 身段与光柱

- 身段：`block/35–75,86–126,137–177`。第 1 节在三速度档的启动距离分别为 4.5/4.2/4，其他 39 节均为 4.5，尾巴为 0.8。每次按对应 1.05/2.05/3.05 格朝上一节移动，不把步长截断到剩余距离。无移动时保留旧朝向；尾巴允许越过上一节后再追回。
- 20 个受击部位在相应偶数身段移动前定位，保留原包一 tick 的位置延迟。逐节计算使用已经更新的上一节位置。`GameTest` 直接读取上述 123 个原文件，仅替换实体身份选择器，保留距离门槛和传送表达式，执行 420 tick 并中途存档恢复。
- 光柱：`block/24–27`、`attack/beam`。每 tick 先减 delay；等于 140 时按目标的当前位置选择落点；139–81 为黑 70/红 30 的预警，80 爆发，60–1 发射，0 删除。保留原始 delay=140 先减到 139 因而不散布的边界行为。
- 单光柱 `spreadplayers ~ ~ 3 20 false @s` 的分布为 XZ ±20 的方形，落在方块中心，要求安全地面上连续两格空气；并不是半径 3–20 的圆环。随机失败后保留原点。
- 射线在上方 1–80 格逐格生成黑/红粒子，可穿越方块；以目标脚底到每个采样点的距离 ≤5 判伤，18 Nightmare 与 300 tick Darkness。接入原生生物伤害管线，不限原包玩家选择器。

## 可复现证据

- `NativeGameTests.serpent_dive_matches_vanilla_command_marker_chain`：真正执行 Minecraft 的 `execute/facing/tp` 命令，与原生链比较 80 tick；目标持续移动并突变高度，中途重载控制器，头部误差小于 0.01 格。
- `serpent_twirl_matches_vanilla_local_coordinates`：非零初始 yaw/pitch 下执行原盘旋坐标命令，比较全部 61 个目标点，误差小于 0.002 格。
- `serpent_body_matches_original_function_files`：420 tick、17,220 个位置最大误差 0；原始结果在 `build/validation/serpent-body-source-trace.csv`，归档摘要见 `docs/validation/serpent-source-comparison.json`。
- 光柱回归覆盖高空目标、圆形边界、延迟选点、读档目标保留和初始 delay=140 边界。
- 回归覆盖三攻击状态保存、目标丢失、Laser 固定扫描方向、400 tick 上限、200 格边界、路径存档和观察者首次同步。
- `runClient -Pp0Smoke` 连续场景：移动目标、三种招式、Dive 中重建实体；输出 `run/screenshots/p0/trace.csv` 和连续帧。

这些命令对照是在独立开发测试中执行选定的原命令，不是运行整个原数据包。测试类被排除出 JAR。

## 仍需验证/移植

- 尚无完整原数据包环境的同场景战斗录像、全量逐 tick trace 或真实多人录像；源包实体遍历和定时函数在复杂场景中的执行顺序仍应实机核验。
- 原包共享 `serpenttarget` 标签；原生版按每条蛇保存最近一次选招目标，旧光柱到 delay=140 时也使用该最新目标，存档保留。多条蛇之间仍隔离，避免原包全局标签互相干扰。独立 RNG 保留取值范围，不复刻原包全局 LCG 随机序列及模运算分布。
- 身段位置逐节对照通过，但原包 `@e` 跨区块的实体遍历顺序仍未做完整运行对照。胡须/尾部粒子、多人长时间表现与第三方大型生物尚未全面验收。
- 新生成实体的初始化方向采用稳定前向；不模仿原包首次召唤时所有 marker 重叠造成的未定义朝向。
