# Nyctocosm Native 项目交接与重构清单

## 1. 项目目标与真实状态

目标是把 `nyctocosm.zip` 中的数据包和资源包完整重构成常规 Forge Mod：

- 运行时不解释旧 `.mcfunction`，不依赖玩法记分板、marker、盔甲架或 display entity 组合体。
- 武器、法术和 Boss 通过原生 `LivingEntity.hurt` / Forge 伤害事件工作，可作用于正常注册的其他模组生物。
- 原模型、纹理、动画、音效、文字和机制仍以旧包为设计规格。
- 所有物品进入独立创造模式标签页。

**当前不是完整等价移植，而是可运行 alpha。** 构建、自动测试和资源审计通过，不代表战斗手感、动画轴向、视觉时序与原包一致。

用户当前明确反馈：

1. Serpent 行为逻辑仍不正确。
2. Sorcerer 身下法环持续旋转的方式不正确。

后续开发必须先修复这两项，不可把 alpha.8 的相关实现视为正确规格。

## 2. 工作区与关键路径

| 内容 | 绝对路径 |
| --- | --- |
| 原始压缩包，只读 | `D:\MinecraftModDev\DEV\nyctocosm.zip` |
| 解压后的原数据/资源，只读 | `D:\MinecraftModDev\DEV\_nyctocosm_source` |
| 当前原生 Mod | `D:\MinecraftModDev\DEV\nyctocosm-native` |
| 原 Forge MDK | `D:\MinecraftModDev\DEV\forge-1.20.1-47.4.13-mdk` |
| 对比资料 | `D:\MinecraftModDev\DEV\_comparison` |
| alpha.8 成品 | `D:\MinecraftModDev\DEV\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.8.jar` |

注意：

- `nyctocosm-native` **目前不是 Git 仓库**。继续修改前建议先备份并初始化版本控制。
- 不要修改 `nyctocosm.zip`。
- 原包与 Mod 使用相同命名空间，测试时不要同时加载。
- 使用新测试世界；没有旧数据包实体、记分板和 CustomModelData 迁移器。
- 美术与原文属于原作者 Flubberschnub；公开发布前需核实许可。

## 3. 新开发者上手顺序

1. 阅读本文件。
2. 阅读 `README.md`：安装、指令、祭坛和玩家操作。
3. 阅读 `docs/MIGRATION.md`：已实现范围、历史修正和验收边界。
4. 打开 `NativeBoss.java`、`ClientEvents.java` 及原包对应函数。
5. 使用 Java 17 构建并运行 GameTest。
6. 在客户端复现 Serpent 和 Sorcerer 法环问题，录制连续视频；单张截图不足以验证运动。

建议下一位开发者使用 `diagnose` 技能，遵循“复现 → 最小化 → 对照原函数 → 加回归 → 修复 → 客户端验证”。

## 4. 构建、测试与审计

~~~powershell
$env:JAVA_HOME='C:\Users\jzy19\.jdks\ms-17.0.19'
$env:JAVA_TOOL_OPTIONS='-Dnet.minecraftforge.gradle.check.certs=false'

.\gradlew.bat build --offline
.\gradlew.bat runGameTestServer --offline
.\gradlew.bat runClient -Psmoke --offline
python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.8.jar
~~~

验证规则：

- `run/logs/latest.log` 必须出现 `All 44 required tests passed`；不能只看 Gradle 的 `BUILD SUCCESSFUL`。
- `ClientSmoke` 和 `NativeGameTests` 在 `src/main/java` 中，但被 JAR 任务排除。
- 审计 `errors` 必须为空。
- alpha.8：2978 个 JAR 条目、1778 模型、10 rig、5607 动画帧、247 声音事件、运行时 mcfunction 为 0。
- alpha.8 SHA-256：`d58c77b7725b579081180a7af7cb54c9f5f3d2383202fd5f63683d43b94bdb9d`。
- Boss 逻辑修改后，必须同时跑 GameTest 和真实客户端连续画面。

## 5. 当前原生代码地图

| 文件 | 责任 |
| --- | --- |
| `NativeMod.java` | Mod 入口、注册总线、指令 |
| `Content.java` | 物品、方块、实体、声音、菜单、创造标签 |
| `NativeEvents.java` | Forge 事件、玩家状态、掉落、全局流程 |
| `Combat.java` | 六元素伤害、范围/射线命中、友军排除 |
| `NativeBoss.java` | 三 Boss 及两普通怪的共用实体和主要状态机 |
| `SorcererTimeline.java` | Sorcerer 12 套攻击关键帧 |
| `NullpointStaff.java` | Nullpoint 独立实体、生命和动画 |
| `IceSpikeWave.java` | Nullpoint 冰刺波 |
| `SerpentBeamHazard.java` | Serpent 延迟光柱 |
| `Spell.java` | 17 法术材料、基础数值、元素 |
| `SpellBolt.java` / `SpellArea.java` | 投射物和持续区域 |
| `SeelicField.java` | 带玩家所有权的灵场和材料扫描 |
| `WeaponItem.java` / `WeaponEffects.java` | 武器入口及服务端特效描述 |
| `client/WeaponEffectController.java` | 客户端副手武器粒子 |
| `ArtifactItem.java` / `FocusItem.java` | 饰品、灵魂瓶和位面线圈 |
| `DaisBlock.java` / `DaisMenu.java` / `DaisRitual.java` | 祭坛 |
| `NativePortalBlock.java` / `SafeTravel.java` | 传送门和安全落点 |
| `LoreItem.java` / `NativeNetwork.java` | 剧情书、客户端消息、效果网络包 |
| `client/ClientEvents.java` | 实体渲染、法环、rig、法术区域 |
| `client/RigLibrary.java` / `RigTransforms.java` | rig 数据和矩阵 |
| `client/GlowingModels.java` | `_e` 发光层 |
| `client/BossMusicController.java` | Boss 分段音乐 |
| `NativeGameTests.java` | Forge GameTest |
| `client/ClientSmoke.java` | 客户端自动场景 |
| `tools/import_assets.py` | 从原包导入资源 |
| `tools/audit_native.py` | 成品审计 |

资源目录：

| 目录 | 内容 |
| --- | --- |
| `assets/nyctocosm` | 纹理、声音、语言、普通模型 |
| `assets/nyctocosm/native_rigs` | 转换后的 rig/动画矩阵 |
| `assets/animated_java`、`assets/*_animated_java_rig` | 保留的 AJ 模型 |
| `data/nyctocosm/damage_type` | 六元素 DamageType |
| `data/nyctocosm/dimension*`、`worldgen` | 三维度和世界生成 |
| `data/nyctocosm/lore` | 剧情书 |
| `data/*/loot_tables` | Boss、结构、方块战利品 |

## 6. 当前完成度

### 已具备

- 37 种注册物品、11 章节变体、48 创造栏条目、独立标签。
- 四武器及主/副手特效。
- 17 基础法术、灵场、投射物、持续区域。
- 六元素原生伤害，可命中普通 `LivingEntity`。
- 三 Boss 实体、血条、基础阶段、模型、动画数据和音乐。
- Sorcerer 六阶段、Nullpoint、12 攻击的主要关键帧。
- Serpent 40 节可视身体、20 受击部位、三攻击框架。
- 三维度、部分结构、传送门、安全返回。
- 祭坛结构扫描、界面和四配方。
- 剧情书中英双语；其他语言回退英文。
- 原模型、纹理、音效、发光贴图。

### 仍未完成或不等价

- Serpent 行为仍被用户判定错误。
- Sorcerer 法环轴向、速度或生命周期错误。
- Emissary 完整战斗和 VFX 大量简化。
- 法术属性、弱点、超迹、耗散、反射/反制未完整等价。
- Submergence 不是原触手。
- 普通怪自然生成、AI、尺寸和攻击只是近似。
- Tony、任务、世界事件、完整生存获取链、结局未完成。
- 部分战利品、字幕、世界文本、仪式演出未迁移。
- 未做第三方大型生物、多玩家长期和跨硬件性能实测。
- 无旧存档迁移。

## 7. 当前最高风险

### 7.1 Serpent

当前 Java 入口：

- `NativeBoss.java:338`：`tickSerpent`。
- `NativeBoss.java:349`：61 tick 盘旋。
- `NativeBoss.java:358`：Dive。
- `NativeBoss.java:376`：游弋和攻击选择。
- `NativeBoss.java:396`：通用 `steerSerpent`。
- `NativeBoss.java:404`：alpha.8 的 `steerSerpentDive`。
- `NativeBoss.java:425`：身体轨迹采样。
- `ClientEvents.java:79`：身体渲染。

已知风险：

- 当前盘旋围绕玩家计算圆形目标；原包 `attack/twirl.mcfunction` 使用独立 marker 螺旋运动，不等价。
- alpha.8 Dive 从 `lastHead` 推导“前方 30 格”，只是 marker 链近似，不是多个持久 marker 的逐 tick 状态。
- 现有测试只证明固定初始条件下会越过静止目标，不证明完整战斗正确。
- 必须对照原包实机确认攻击选择、盘旋、入 Dive、转弯半径、穿越、绕回和退出。

### 7.2 Sorcerer 法环

当前 Java：

- `ClientEvents.java:89-94` 绘制 `magic/sorcerer_plane`。
- `ClientEvents.java:92` 用 `Axis.YP.rotationDegrees(boss.tickCount * 2 + partial * 2)`，令整个模型绕世界 Y 轴旋转。
- `NativeBoss.java:282-298` 的 `voidPlane` 粒子也加入 `tickCount * .035` 旋转偏移。

原包：

- `sorcerer/functions/summonplane.mcfunction` 创建法环。
- `sorcerer/functions/__generated__/block/5.mcfunction` 生成 `SorcererPlane`、CMD 19 盔甲架。
- `spells/functions/__generated__/block/158.mcfunction` 分派法环 tick。
- `spells/functions/__generated__/block/163.mcfunction` 每 tick 修改 `Pose.Head[2]`，即头部 Z 轴姿态加 1。
- `sorcerer/functions/killplane.mcfunction` 和 `remove.mcfunction` 控制销毁。

所以不能简单把旋转速度 2 改成 1；轴、中心、速度和生命周期都需重查。

## 8. 原数据包文件命名与功能对照

数据根：`D:\MinecraftModDev\DEV\_nyctocosm_source\datapack\data`  
资源根：`D:\MinecraftModDev\DEV\_nyctocosm_source\resources\assets`

### 8.1 命名约定

| 名称 | 含义 |
| --- | --- |
| `load.mcfunction` | 创建记分板、常量、全局状态 |
| `tick.mcfunction` | 命名空间总 tick |
| `playertick.mcfunction` / `entitytick.mcfunction` | 玩家/实体每 tick |
| `subtick.mcfunction` | 攻击或状态存活期间每 tick |
| `start.mcfunction` | 进入攻击/状态 |
| `activate.mcfunction` | 蓄力后激活 |
| `hit.mcfunction` / `hitbox.mcfunction` | 命中与伤害 |
| `raycast.mcfunction` | 命令式射线 |
| `summon*.mcfunction` | 生成 marker、投射物、展示体或模型 |
| `incur.mcfunction` | Boss 受伤入口 |
| `player_dealt_damage.mcfunction` | 记录参战玩家 |
| `defeat.mcfunction` / `finaldeath.mcfunction` | 击败、奖励、死亡收尾 |
| `remove.mcfunction` | 清理从属实体 |
| `__generated__/block/N.mcfunction` | mc-build 展开的匿名逻辑块；编号本身无稳定语义，必须追调用者 |
| `zzz_*_internal` | Animated Java 内部生成代码，不是玩法入口 |
| `aj*` | Animated Java 对外 rig/动画包装 |

### 8.2 顶级命名空间对照

| 原命名空间 | 原功能 | 当前原生位置 |
| --- | --- | --- |
| `serpent` | Serpent AI、攻击、伤害、死亡 | `NativeBoss`、`SerpentBeamHazard`、`ClientEvents` |
| `sorcerer` | Sorcerer 阶段、攻击、法环、死亡 | `NativeBoss`、`SorcererTimeline`、`NullpointStaff`、`IceSpikeWave` |
| `emissary` | Emissary 四阶段、攻击、过场 | `NativeBoss`，尚未完整 |
| `spells` | 17 法术、灵场、灵魂 | `Spell`、`SeelicField`、`SpellBolt`、`SpellArea` |
| `magic` | 属性、六元素粒子、超迹 | `Combat`、客户端粒子；部分未实现 |
| `damage` / `ncdamage` | 旧记分板伤害 API | `Combat` + DamageType JSON |
| `effects` | 闪电、冲击波、元素、Boss VFX | 多个原生类；未全部迁移 |
| `items` | 物品检测、武器、获取 | `Content`、`WeaponItem`、`FocusItem` |
| `artifacts` | 13 饰品 | `ArtifactItem`、`NativeEvents` |
| `crafting` / `inculcate` | 祭坛和仪式 rig | `DaisRitual`、`DaisBlock`、`DaisMenu` |
| `nyctocosm` | 主 tick、传送门、普通怪、Tony | `NativeEvents`、`NativePortalBlock`、`SafeTravel`；Tony 未实现 |
| `lore` | 剧情书和章节进度 | `LoreItem`、`data/nyctocosm/lore` |
| `music` | Boss/结构音乐 | `BossMusicController` |
| `structures` | 结构辅助 | 原生 worldgen JSON/NBT |
| `death_walker` / `ajdeathwalker` | Death Walker rig | `NativeBoss(kind=3)`、`native_rigs/ajdeathwalker` |
| `ajsorcerer*` / `ajnullpoint` | Sorcerer/Nullpoint 动画 | `RigLibrary`、对应 native rig |
| `ajspellplane` / `ajmobilespellplane` | 法阵 rig | `SeelicField`、`ClientEvents`、`RigLibrary` |

### 8.3 Serpent 核心文件完整表

| 原文件 | 功能 |
| --- | --- |
| `serpent/functions/load.mcfunction` | 初始化记分板 |
| `serpent/functions/summon.mcfunction` | 召唤 Boss 和从属实体 |
| `serpent/functions/tick.mcfunction` | 总 tick |
| `serpent/functions/ai.mcfunction` | 状态选择、攻击入口 |
| `serpent/functions/headai.mcfunction` | 蛇头朝向 marker 链 |
| `serpent/functions/move.mcfunction` | 按速度档移动蛇头 |
| `serpent/functions/attack/twirl.mcfunction` | 所有攻击前的 61 tick 螺旋盘旋 |
| `serpent/functions/attack/dive/start.mcfunction` | 选择目标、设置 Dive |
| `serpent/functions/attack/dive/subtick.mcfunction` | Dive 持续追踪和退出 |
| `serpent/functions/attack/laser/start.mcfunction` | 激光初始化 |
| `serpent/functions/attack/laser/subtick.mcfunction` | 激光持续阶段 |
| `serpent/functions/attack/laser/raycast.mcfunction` | 激光射线 |
| `serpent/functions/attack/beam/start.mcfunction` | 20 柱攻击初始化 |
| `serpent/functions/attack/beam/subtick.mcfunction` | 光柱预警、伤害、消失 |
| `serpent/functions/attack/beam/raycast.mcfunction` | 定位光柱表面 |
| `serpent/functions/incur.mcfunction` | 受击 |
| `serpent/functions/player_dealt_damage.mcfunction` | 记录参战玩家 |
| `serpent/functions/defeat.mcfunction` | 击败流程 |
| `serpent/functions/remove.mcfunction` | 清理 |
| `serpent/functions/__generated__/block/200.mcfunction` | `SerpentInterp=2` 的 currentfacing 修正 |
| `serpent/functions/__generated__/block/201.mcfunction` | 实时目标 marker |
| `serpent/functions/__generated__/block/203.mcfunction` | 盘旋结束后的速度/插值切换 |
| `serpent/functions/__generated__/block/204.mcfunction` | 进入 Dive 和播放音效 |

### 8.4 Sorcerer 文件对照

#### 状态、法杖和死亡

| 原文件 | 功能 |
| --- | --- |
| `sorcerer/functions/tick.mcfunction` | 总 tick |
| `sorcerer/functions/ai/entitytick.mcfunction` | 本体 tick |
| `sorcerer/functions/ai/subtick.mcfunction` | 阶段/攻击 AI |
| `sorcerer/functions/ai/hurt.mcfunction` | 本体受击 |
| `sorcerer/functions/ai/staffhurt.mcfunction` | Nullpoint 受击 |
| `sorcerer/functions/rise.mcfunction` | 插杖后升空 |
| `sorcerer/functions/risetransition1.mcfunction` | 升空过渡 1 |
| `sorcerer/functions/risetransition2.mcfunction` | 升空过渡 2 |
| `sorcerer/functions/plungespear.mcfunction` | Nullpoint 插地 |
| `sorcerer/functions/falltransition.mcfunction` | 破杖下落过渡 |
| `sorcerer/functions/fall.mcfunction` | 落地阶段 |
| `sorcerer/functions/summonplane.mcfunction` | 创建身下法环 |
| `sorcerer/functions/killplane.mcfunction` | 删除法环 |
| `sorcerer/functions/ai/die.mcfunction` | 死亡状态 |
| `sorcerer/functions/deathpoof.mcfunction` | 死亡爆散 |
| `sorcerer/functions/finaldeath.mcfunction` | 最终死亡 |
| `sorcerer/functions/defeat.mcfunction` | 奖励/击败 |

#### 12 套攻击目录

| 原目录 | 攻击 |
| --- | --- |
| `sorcerer/functions/attacks/halobeam/` | 8 光环依次生成、激活、射击 |
| `.../voidbeam/` | 持续 Void 射线 |
| `.../voidmeteor/` | 裂隙和多波陨石 |
| `.../shardhail/` | 聚能球和延迟冰雹 |
| `.../singularitycharge/` | 直行奇点弹 |
| `.../seekingsingularities/` | 追踪奇点弹 |
| `.../frozenblades/` | 冰刃召唤、挥击、投掷 |
| `.../javelinswing/` | 长枪挥击 |
| `.../dashstrike/` | 地面突进斩 |
| `.../airdash/` | 空中突进/砸地 |
| `.../voidwave/` | Void 波 |
| `.../icespikes/` | Nullpoint 冰刺波 |

每个攻击目录按 `start → subtick → activate/throw/shoot → hit/raycast` 阅读。阶段攻击权重位于：

- `attacks/phase3/shortpool.mcfunction`、`farpool.mcfunction`
- `attacks/phase4/shortpool.mcfunction`、`farpool.mcfunction`
- `attacks/phase5/shortpool.mcfunction`、`farpool.mcfunction`
- `attacks/phase6/shortpool.mcfunction`、`farpool.mcfunction`

### 8.5 Emissary 文件对照

| 原目录/文件 | 功能 |
| --- | --- |
| `emissary/functions/tick.mcfunction` / `entitytick.mcfunction` | 总 tick/实体 tick |
| `ai/phase/start1..start4.mcfunction` | 四个主要形态入口 |
| `ai/phase/1..4/idle.mcfunction` | 各形态空闲 AI |
| `ai/phase/transition.mcfunction` | 阶段切换 |
| `ai/hitbox/mode/phase1..phase4.mcfunction` | 阶段受击箱 |
| `attacks/perform.mcfunction` / `end.mcfunction` | 攻击总调度 |
| `attacks/phase1/choose.mcfunction` | 一阶段选择 |
| `phase1/lightning_javelin/` | 雷霆标枪 |
| `phase1/meteor_shower/` | 流星雨 |
| `phase1/pillars/` | 火焰/云柱 |
| `phase1/wingbeat/` | 翼击 |
| `attacks/phase2/choose.mcfunction` | 二阶段选择 |
| `phase2/celestial_smite/` | 天界惩击 |
| `phase2/flame_geysers/` | 火焰喷泉 |
| `phase2/sword_plunge/` | 插剑爆发 |
| `phase2/wing_blast/` | 翼部射线 |
| `phase2/strafeleft/`、`straferight/` | 横移 |
| `attacks/phase3/choose.mcfunction` | 三阶段选择 |
| `phase3/cross/`、`frontkick/`、`ground_slam/`、`swordspin/`、`swordswing/` | 近战组合 |
| `phase3/flame_slash/`、`lightning_field/`、`seeking_missiles/` | 三阶段法术 |
| `phase3/parry/`、`runclose/`、`strafe*`、`flipfar/` | 移动/反击 |
| `attacks/phase4/choose.mcfunction` | 四阶段选择 |
| `phase4/apocalypse_rain/`、`divine_blast/`、`fireball/`、`godrays/` | 四阶段远程 |
| `phase4/corkscrew/`、`grapple/`、`flying/` | 四阶段飞行 |
| `cutscene/start.mcfunction`、`subtick.mcfunction`、`end.mcfunction` | 过场 |
| `die.mcfunction`、`finaldeath.mcfunction`、`defeat.mcfunction` | 死亡/奖励 |

> 面向第一次接触本项目的开发者。  
> 最后整理：2026-09-21  
> 当前版本：`0.3.0-native-alpha.8`  
> 平台：Minecraft Java 1.20.1、Forge 47.4.13、Java 17

### 8.6 17 种法术对照

原路径统一为 `spells/functions/<法术名>/`；`cast.mcfunction` 是释放逻辑，`transmutate.mcfunction` 是材料转化/法术创建。

| 法术 | 原材料 | 当前枚举 |
| --- | --- | --- |
| Luminescence | 荧石 | `Spell.LUMINESCENCE` |
| Resonance | 回响碎片 | `Spell.RESONANCE` |
| Evanescence | 末影之眼 | `Spell.EVANESCENCE` |
| Incurrence | 凋零玫瑰 | `Spell.INCURRENCE` |
| Abhorrence | 煤炭 | `Spell.ABHORRENCE` |
| Forbiddance | 恶魂之泪 | `Spell.FORBIDDANCE` |
| Gravitance | 哭泣的黑曜石 | `Spell.GRAVITANCE` |
| Subsistence | 苹果 | `Spell.SUBSISTENCE` |
| Reflectance | 铁锭 | `Spell.REFLECTANCE` |
| Submergence | 灵魂沙 | `Spell.SUBMERGENCE` |
| Adamance | 圆石 | `Spell.ADAMANCE` |
| Incandescence | 火药 | `Spell.INCANDESCENCE` |
| Resistance | 熟牛肉 | `Spell.RESISTANCE` |
| Transmittance | 末影珍珠 | `Spell.TRANSMITTANCE` |
| Vacance | 玻璃瓶 | `Spell.VACANCE` |
| Repellance | 羽毛 | `Spell.REPELLANCE` |
| Fulminance | 下界石英 | `Spell.FULMINANCE` |

通用原入口：

- `spells/functions/cast_plane.mcfunction`：创建灵场。
- `spells/functions/casting/subtick.mcfunction`：施法计时。
- `spells/functions/cancel_cast.mcfunction`：取消。
- `spells/functions/ring/*`：位面线圈。
- `spells/functions/bottle/*`：灵魂瓶。
- `effects/functions/spells/circles/*`：法术圆粒子。
- `magic/functions/calcstatsspell.mcfunction`：原属性计算，尚未完整移植。

### 8.7 武器、饰品、祭坛、传送门、剧情

| 功能 | 原文件/目录 | 当前原生 |
| --- | --- | --- |
| 武器/物品 tick | `items/functions/*` | `WeaponItem`、`NativeEvents` |
| 元素伤害 | `damage/functions/api/*damage.mcfunction` | `Combat`、DamageType |
| 元素粒子 | `magic/functions/particles/*.mcfunction` | `WeaponEffects`、实体特效 |
| 饰品 | `artifacts/functions/<artifact>/` | `ArtifactItem` |
| 祭坛扫描 | `crafting/functions/dais/scan.mcfunction` | `DaisRitual` |
| 祭坛构建 | `crafting/functions/dais/construct.mcfunction` | `DaisRitual` |
| 祭坛演出 | `crafting/functions/dais/effects/*`、`inculcate/functions/*` | 当前未等价 |
| 传送门扫描 | `nyctocosm/functions/portal/scan*.mcfunction` | `NativePortalBlock` |
| 传送门激活 | `portal/activate*.mcfunction`、`portal/activation/*` | `NativePortalBlock` |
| 普通怪 | `nyctocosm/functions/mobs/*` | `NativeBoss(kind=3/4)`，近似 |
| Tony | `nyctocosm/functions/tony/*` | 未实现 |
| 剧情书 | `lore/functions/storylines/*` | `LoreItem` + lore SNBT |
| Boss 音乐 | `music/functions/{serpent,sorcerer,emissary}/*` | `BossMusicController` |

## 9. 美术与动画对照

| 原资源 | 含义 | 当前方式 |
| --- | --- | --- |
| `resources/assets/nyctocosm/textures` | 主要纹理 | 复制/转换到 Mod assets |
| `resources/assets/nyctocosm/sounds`、`sounds.json` | 音频和事件 | 注册 SoundEvent |
| `ajsorcerer_animated_java_rig` | Sorcerer 初始 rig | `native_rigs/ajsorcerer.json.gz` |
| `ajsorcererm1_animated_java_rig` | Sorcerer 形态 1 | `native_rigs/ajsorcererm1.json.gz` |
| `ajsorcererm2_animated_java_rig` | Sorcerer 形态 2 | `native_rigs/ajsorcererm2.json.gz` |
| `ajnullpoint_animated_java_rig` | Nullpoint | `native_rigs/ajnullpoint.json.gz` |
| `ajemissary_animated_java_rig` | Emissary | `native_rigs/ajemissary.json.gz` |
| `ajspellplane_animated_java_rig` | 固定法阵 | `native_rigs/ajspellplane.json.gz` |
| `ajmobilespellplane_animated_java_rig` | 移动法阵 | `native_rigs/ajmobilespellplane.json.gz` |
| `zzz_<rig>_internal/functions/animations/<clip>/` | AJ 逐帧矩阵命令 | 仅导入源，不在运行时执行 |

`tools/import_assets.py` 输入 `../_nyctocosm_source` 和 `../nyctocosm.zip`，输出 assets、`native_rigs/*.json.gz`、语言和声音映射。

注意：

- 重新导入前备份手工汉化和修正。
- 动画帧已导入不等于动画选择、根朝向、轴向、循环和切换时机正确。
- `_e.png` 是发光层，由 `GlowingModels` 处理。

## 10. 后续工作清单

### P0：用户已确认的问题

- [ ] 在原数据包环境录制 Serpent 完整战斗，覆盖游弋、盘旋、Dive、Laser、Beam 和退出。
- [ ] 每 tick 记录原包 Attack、State、Interp、Speed、目标 marker、currentfacing、towardfacing、蛇头位置/朝向。
- [ ] 画出 marker 生命周期，确认持久实体与临时实体。
- [ ] 用持久 Java 状态重写 marker 链，不再用单一 steering lerp 近似。
- [ ] 精确还原 `attack/twirl.mcfunction` 的独立螺旋 marker。
- [ ] 验证移动玩家、高低差、玩家急转、多人、目标丢失、方块碰撞、穿体绕回。
- [ ] 核对 Serpent 状态结束、重新选目标、冷却、三攻击等权选择。
- [ ] 对照 `summonplane → block/5 → block/158 → block/163 → killplane` 全生命周期。
- [ ] 明确 Sorcerer 法环的轴、速度、中心、偏移和出现/消失帧。
- [ ] 删除或修正 `ClientEvents.java:92` 的世界 Y 轴旋转。
- [ ] 单独判断 `NativeBoss.voidPlane` 粒子是否应旋转。
- [ ] 增加法环状态测试和连续画面验证。

### P1：Boss 完整度

- [ ] 将 Serpent、Sorcerer、Emissary 从 `NativeBoss.java` 拆为各自实体或控制器。
- [ ] 逐项核对 Sorcerer 12 攻击的运动、伤害体积、粒子、音效、帧。
- [ ] 完成 Emissary 四阶段攻击、移动、反击、过场、受击箱、死亡、奖励。
- [ ] 核对三个 Boss 掉落、概率、经验、章节奖励。
- [ ] 增加存档重载、区块卸载、多人目标切换测试。

### P2：法术和武器

- [ ] 对照 `magic/calcstatsdamage.mcfunction`、`calcstatsspell.mcfunction` 完成属性、弱点、超迹、耗散。
- [ ] 完成 Submergence 原触手。
- [ ] 精确恢复 Reflectance、Forbiddance、Gravitance 等几何和时序。
- [ ] 每种法术对原版、本模组、第三方大型生物测试。
- [ ] 核对四武器主/副手在第一人称、第三人称、多人网络下的粒子方向。

### P3：世界、生存、普通生物

- [ ] 还原 Death Walker、Apparition 自然生成、碰撞箱、AI、攻击。
- [ ] 实现 Tony、任务、世界事件、完整材料链、结局。
- [ ] 核对结构、战利品、维度生成、传送门演出。
- [ ] 完成原祭坛交易/灌注动画。
- [ ] 制作旧存档迁移方案，或明确不兼容。

### P4：交付质量

- [ ] 初始化 Git，先提交 alpha.8 基线。
- [ ] 为三个 Boss 写独立行为规格和可验证时间轴。
- [ ] 建立原包与 Mod 同场景视频对照。
- [ ] 进行第三方模组、多玩家、服务器性能、低配渲染测试。
- [ ] 核对全部中英文本、字幕、Boss 标题和世界文本。
- [ ] 发布前确认许可。

## 11. 推荐移植流程

1. 找到原命名空间 `tick/start`。
2. 递归追踪所有 `function namespace:path`。
3. 为 `__generated__/block/N` 记录调用者和条件。
4. 列出记分板、tag、marker/display 实体及生命周期。
5. 写出逐 tick 状态、位置、旋转、伤害、粒子、声音表。
6. 在原包录制参考画面和关键数值。
7. 设计原生状态对象；实现方式可原生化，结果必须可对照。
8. 先写失败回归，再实现。
9. 跑 GameTest。
10. 跑客户端连续场景；高速运动不能只看截图。
11. 更新本文件、`docs/MIGRATION.md`、版本号和审计。

## 12. 完成验收清单

- [ ] 原函数调用链追完。
- [ ] 状态、时序、数值、伤害对象、退出条件已记录。
- [ ] 原包与 Mod 在相同初始条件下对照。
- [ ] 原版、本模组和普通第三方 `LivingEntity` 均正确受伤。
- [ ] 不绕过其他模组主动取消的伤害、无敌和护盾。
- [ ] 服务端权威，客户端只负责视觉/声音。
- [ ] 存档重载和区块卸载不损坏状态。
- [ ] 自动回归通过。
- [ ] 客户端连续画面、声音、性能通过。
- [ ] 文档更新；未用“测试通过”冒充“体验等价”。

## 13. 已知陷阱

- 关键逻辑常藏在 `__generated__/block/N`；只读顶层会漏行为。
- AJ 内部函数是生成结果，动作语义需结合调用时机、记分板和帧。
- 盔甲架 `Pose.Head[2]` 与渲染器绕世界 Y 轴不是同一变换。
- marker 链延迟不能简单改成“朝目标插值”，会改变转弯半径和惯性。
- 高速 `setPos` 会绕过普通寻路/碰撞直觉，需验证穿块和大实体碰撞。
- 粒子数量、网络发送和视觉结果不同；服务端逐粒子包会造成负载。
- 照搬旧实体标签过滤会再次导致无法伤害其他模组生物。
- GameTest 成功只证明断言，不证明动画和手感。
- `docs/gametest-latest.log` 是旧记录，以 `run/logs/latest.log` 为准。

## 14. 现有参考

- `README.md`：玩家安装、指令、祭坛、书籍、构建。
- `docs/MIGRATION.md`：实现边界、历次补齐、测试覆盖。
- `docs/native-audit.json`：最近 JAR 审计。
- `docs/sound-import.json`：声音导入、缺失和修正。
- `run/screenshots/`：静态 smoke 截图，只作辅助。

## 15. 交接结论

当前工程已有可运行的原生 Forge 架构，但“可运行”和“完整还原”仍有明显距离。下一阶段不要继续堆近似功能：先把 Serpent 与 Sorcerer 法环做成有原包运行证据、逐 tick 状态记录和连续画面对照的精确实现，再扩展 Emissary 和其余系统。

