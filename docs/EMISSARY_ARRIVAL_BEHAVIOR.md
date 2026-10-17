# Emissary 入场、悬空与地表传送

日期：2026-09-29。针对用户实测的“进维度/领域卡地下、缺少飞入落地、一阶段贴地”修正。以 `../../_nyctocosm_source/datapack` 的命令和导出动画为依据，不修改源包。

## 原包行为与原生实现

| 环节 | 原包依据（相对 `datapack/data`） | 本轮处理 |
| --- | --- | --- |
| 远古城市门进入夜域 | `nyctocosm/functions/portal/__generated__/block/49.mcfunction`：先 `execute in`，后 `spreadplayers` | 按维度比例换算 X/Z，舍弃原地下 Y，选择附近可站立地表 |
| 进入使者领域 | `emissary/functions/__generated__/block/21..24.mcfunction`：换维度、`positioned over world_surface`、附近独立召唤点 | 玩家与 Boss 落点分开预检；领域自然地表为 Y=193，不沿用来源高度 |
| 远方入场 | `emissary/functions/__generated__/block/26.mcfunction` 与 `block/0.mcfunction` | 从落点反向 100 格、倾角 50° 的空中起点，沿直线每 tick 1 格，95 tick 后接实体 `spawn`；这一段是粒子陨石，尚不显示模型 |
| 落地动作 | `emissary/functions/summon.mcfunction`；`animated_java/functions/ajemissary/zzzzzzzz/animations/spawn/tree/leaf_*.mcfunction` | 剩余 5 格在帧 0/2/4/6/8 完成；保留原模型动画，帧 20/60 音效，恢复帧 0 的 `wing_config_0`，不再全程隐藏翅膀 |
| 后撤升空 | `spawn` 帧 62 开始、99 停止；`emissary/functions/__generated__/block/11.mcfunction` | 37 次、每次 1.5 格、倾角 35°，水平后撤约 45.46 格、升高约 31.83 格 |
| 一阶段常态 | `emissary/functions/ai/phase/start1.mcfunction`、原 root marker 与攻击调度 | 根节点保持悬空，转向目标并使用一阶段远程招式；不是地面导航，也不是四阶段巡航。修正 80 格索敌上限为 300 格 |
| 二转三落地 | `phase2_transition` 导出帧；`emissary/functions/ai/phase/2/slide.mcfunction` | 帧 0–4 后撤、41–59 前进下降，帧 60 从 Boss 当时位置朝远离玩家方向后移 20 格，再找安全地表；随后进入三阶段 |
| Wingbeat | `emissary/functions/attacks/phase1/wingbeat/` | 第 20 帧扇翼音效、23 帧从导出 center locator 发出 4×4 平行波面。每 tick 三次一步移动；接近未受免位移保护的玩家时给 20 tick、等级参数 30 的漂浮，不再凭空造成 5 点直接伤害 |

## 地表判定与读档

- 高度图只提供候选点；完整身体碰撞、液体、危险方块、世界边界和脚下支撑仍必须通过。跳过树冠/草的高度，避免把树叶当出生地。
- 先加载实体碰撞箱覆盖的区块，再检测碰撞。旧逻辑在第一次来到领域时先检查未知区块，曾错误接受 Y=-30 的深板岩内部。
- 搜索最多扩展到附近 32 格，不建平台、不清方块。无安全落点时拒绝传送，返程栈不改变。
- 入场落点、动作进度、已建立的悬空状态持久化；过场跨区块使用短期自动过期区块票，读档继续，不永久强加载世界。
- 旧原生版地下/地面的一、二阶段使者在首次恢复 AI 时上移到地表之上并建立悬空锚点。已正确建立锚点的不重复抬高；原生旧存档中 80 格默认索敌属性同步迁移。
- 不迁移原数据包旧 marker 战斗；不把玩家强制切换成冒险模式，不自动部署到用户存档。

## 验证与限制

本轮在原 103 项服务端回归基础上新增 13 项，覆盖地表/洞穴返程区别、真实领域高度、首次未加载碰撞、完整 Boss 净空、树冠、95 tick 入场与升空、实际 Mob 物理 tick、实体重建读档、旧存档恢复、二转三落地、真实 AI 索敌、原版 `CommandSourceStack.withLevel` 坐标比例、Wingbeat 从空中命中与保护/读档。完整结果以根目录 todolist 尾部为准。

尚未进行本轮客户端完整生存录像对照。Pillars、Flame Geysers、Sword Plunge、Wing Blast、Flame Slash、Apocalypse Rain 等仍有简化；其他招式 locator、完整翅膀受击部位、粒子画面及多人细节不能宣称等价。Wingbeat 本轮对齐运动/触发/反射与反制通路，但粒子采样仍为原生近似。

## 实测建议

1. 用备份或新测试存档，从地下远古城市门进入夜域：应出现在地表、有头顶净空，不继承原地下高度；返回仍应回到原门附近安全位置。
2. 在 infernal_chasms 触发 Incurrence：玩家应落在使者领域草地（自然地形 Y=193），Boss 落点与玩家分开。
3. 朝召唤方向观察：先看到远处粒子陨石约 4.75 秒飞近，再接落地、展开翅膀、后撤升空；一阶段留在空中施放远程攻击。
4. 入场中和悬空战斗中分别保存退出、重进，确认不重播整个流程、不掉地下。旧版本的地下使者会恢复高度，但不会为已开战实体强制重播开场。
5. 二转三阶段应回到地表；Wingbeat 应有从空中逼近的波面，接触后漂浮。客户端与服务器均使用 alpha.12，避免旧客户端误画新入场状态。
