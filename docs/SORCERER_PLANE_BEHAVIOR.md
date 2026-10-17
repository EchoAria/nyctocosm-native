# Sorcerer 法环规格与验证边界（alpha.9）

## 原调用链

`initial_spawn/tree/leaf_330_effects_0` 切换到 m1 的 `rise`；不是直接进入 idle。
`rise/tree/leaf_127` 调用 `sorcerer:summonplane`，再调用 `sorcerer:__generated__/block/5,6`。
`spells:__generated__/block/158` 每 tick 分派到 block/163。
`falltransition/tree/leaf_5_effects_0` 调用 `sorcerer:killplane`；`remove` 也清理法环。

## 位置与姿态

- 创建位置是调用者局部坐标 `^ ^1 ^-2`，创建时继承 yaw。
- 原盔甲架此后不跟随本体的移动或转向。原生版用 Boss 的同步状态保存独立世界坐标和固定 yaw。
- `Pose.Head` 初始为 `[0.01, 0.01, 0.01]`；block/163 取 Z 分量的整数值，每 tick 加 1，360 后回绕。原生版以创建时间计算角度，并对渲染分数帧插值。
- `magic/sorcerer_plane.json` 的 HEAD 变换包含 X=-90°、平移 `[0,-4.5,-31.75]/16`、缩放 4。因此圆盘是**竖直**的；头部 Z 轴让它在圆盘平面内转动，不能改成绕世界 Y 轴旋转。
- `SorcererPlane.headTransform` 还原 `LivingEntityRenderer → ArmorStandModel.head → CustomHeadLayer.translateToHead` 的先后顺序，包括头部支点和 0.625 倍缩放，随后交给正常物品 HEAD 变换。

## 生命周期

| 帧 | 行为 |
| --- | --- |
| initial_spawn 253 | 插入 Nullpoint；此时没有法环，也不提前把本体抬高 |
| initial_spawn 330 | 接上 rise |
| rise 44–58 | 每 tick 上升 0.05 格 |
| rise 59–114 | 每 tick 上升 0.08 格 |
| rise 115–126 | 每 tick 上升 0.05 格；总位移 5.83 格 |
| rise 127 | 创建法环、播放原创建音效 |
| falltransition 1–4 | 法环仍存在 |
| falltransition 5 | 删除本 Boss 的法环 |
| 死亡/移除 | 法环消失，不影响另一个 Boss |

法环数据通过 `SynchedEntityData` 只在创建、销毁、恢复时同步。存档保存位置、yaw 和已旋转时间；加载恢复时间基准，避免旋转重置或直接跳过整个未加载时段。升起/落下过渡动画的播放位置一并恢复。

原 `effects:shockwave/voidplane` 是创建时的短暂效果，不是永久旋转粒子圈。本轮删除旧 `voidPlane` 的持续粒子，保留创建闪光与音效；完整原三段冲击波粒子仍待单独迁移。

## 验证

- GameTest：首次出场接 rise、127 帧前后、破杖后第 5 帧销毁、固定锚点、存档、两 Boss 隔离、死亡清理、竖直圆盘法线、5.83 格位移。
- 连续客户端场景：右侧原生法环，左侧原资源模型戴在原版盔甲架上，`Pose.Head.Z` 每 tick 增加 1。开发资源包只为 glowstone_dust 的 CMD 19 添加原模型引用，没有改动发行资源。
- 录制覆盖出现、超过一圈旋转和破杖消失。参考盔甲架与开发类均不进入发行 JAR。

上述参考验证了渲染变换，不等于整个 Sorcerer 战斗的原包完整录像。12 种攻击的其余运动、粒子、受击体积与完整原包时序仍在交接清单中。
