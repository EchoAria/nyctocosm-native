# Nyctocosm 主线体验差距审查 TODO

用途：按对主线流程体验的影响从高到低，逐项对照 `_nyctocosm_source`（旧数据包）与 `nyctocosm-native`（Forge 模组），确认、修改并验收。

本清单只记录“玩家实际能否按主线游玩、遭遇和推进”的差距。代码架构拆分、纯开发便利性和不影响流程的内部实现不优先。

## 使用方式

每项依次经过三个状态：

- [ ] **审查**：在旧数据包和模组中用相同条件复现，确认预期行为与差距。
- [ ] **修改**：差距确认后实施修复。
- [ ] **验收**：自动测试和实际客户端连续游玩均通过。

处理顺序原则：上一项若会阻断下一项的正常获取或触发，应先完成上一项。

---

## P0：主线阻断项

### 1. 建立并跑通一条明确的完整主线路线

- [ ] 审查
- [ ] 修改
- [ ] 验收

**目标路线**：新存档起步 → 获得镰刀/灵魂容器 → 收集灵魂 → 建造祭坛 → 制作位面线圈和进阶武器 → 激活远古城市传送门 → 探索夜域与结构 → 获取剧情书/饰品/材料 → 触发三个 Boss → 击败 Emissary → 逃离领域并完成结局。

**当前差距**：模组有大量可单独测试的功能，但没有一条经过验证、无需创造模式和管理员指令的完整路线。

**验收标准**：从全新生存存档开始，不使用 `/give`、刷怪蛋或 `/nyctocosm` 测试指令，能够完成上述路线；记录每一步的触发条件、获取来源和失败恢复方式。

### 2. 修复开局物品与镰刀获取链

- [ ] 审查
- [ ] 修改
- [ ] 验收

**旧包行为**：3 个回响碎片加 2 根木棍合成的占位物会被替换为 Seelic Scythe。

**当前模组**：没有镰刀配方；额外加入了旧包没有的 Soul Bottle 和 Dais 便捷配方。Death Walker 有 10% 镰刀掉落，但普通怪自然生成尚未工作，因此不能作为可靠开局来源。

**检查入口**：

- `_nyctocosm_source/datapack/data/items/recipes/scythe.json`
- `_nyctocosm_source/datapack/data/items/functions/scythe/replacehand.mcfunction`
- `nyctocosm-native/src/main/resources/data/nyctocosm/recipes/`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java`

**验收标准**：明确并恢复正式开局获取方式；移除或正式定义便捷配方的定位；配方书、物品提示和多人服务端结果一致。

### 3. 恢复灵魂生成、拾取和容器循环

- [ ] 审查
- [ ] 修改
- [ ] 验收

**旧包行为**：镰刀击杀后生成短时存在的灵魂；附近玩家必须拿玻璃瓶、已有 Soul Bottle，或在副手佩戴 Planar Coil 才能捕获。

**当前模组**：主手镰刀击杀后，直接给背包中第一个 FocusItem 增加 1 个灵魂；没有灵魂实体、捕获窗口、抢夺关系或明确的容器选择。

**检查入口**：

- `_nyctocosm_source/datapack/data/items/functions/__generated__/block/0.mcfunction`
- `_nyctocosm_source/datapack/data/items/functions/__generated__/block/1.mcfunction`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/FocusItem.java`

**验收标准**：灵魂出现、寿命、可见性、捕获距离、容器优先级、多人竞争及满容量行为与旧包一致；死亡、掉线和区块卸载不复制灵魂。

### 4. 恢复普通怪物自然生成与替换流程

- [ ] 审查
- [ ] 修改
- [ ] 验收

**旧包行为**：夜域群系自然生成的 illusioner 会被转换成 Death Walker 或 Nightmare Apparition，并受各自数量上限、群系条件和状态机控制。

**当前模组**：群系 JSON 仍生成原版 illusioner，但没有对应转换事件，因此实际会出现原版幻术师；注册的两个自定义怪主要只能通过刷怪蛋生成。

**检查入口**：

- `_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/spawning.mcfunction`
- `_nyctocosm_source/datapack/data/nyctocosm/functions/mobs/`
- `nyctocosm-native/src/main/resources/data/nyctocosm/worldgen/biome/`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java`

**验收标准**：正确群系、亮度、地面和数量条件下自然生成正确怪物；不残留 illusioner；生成频率、上限、清理和多人区块加载行为可对照。

### 5. 还原 Death Walker 与 Nightmare Apparition 的实战行为

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：血量、碰撞箱、受击位置、移动、追踪、攻击、动画、死亡和掉落仍是近似实现；共用了 `NativeBoss` 的简化寻路与攻击模板。

**验收标准**：分别录制旧包与模组的生成、闲置、发现玩家、追踪、攻击、受伤、丢失目标和死亡全过程；逐项核对尺寸、速度、距离、伤害、冷却、音效、粒子和掉落。

### 6. 修复夜域亮度、恐惧与 Luminescence 循环

- [ ] 审查
- [ ] 修改
- [ ] 验收

**旧包行为**：玩家在亮度低于 5 的位置累计 200 tick 后进入恐惧；回到光下立即清除；恐惧期间具有 Darkness、Slowness、心跳和随机事件。

**当前模组**：只要位于夜域且没有 Luminescence，每 100 tick 就无视环境亮度给予 120 tick Darkness；没有原恐惧积累、光照解除、缓慢、心跳及随机事件。

**检查入口**：

- `_nyctocosm_source/datapack/data/nyctocosm/functions/player/fear.mcfunction`
- `_nyctocosm_source/datapack/data/nyctocosm/predicates/in_light.json`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeEvents.java`

**验收标准**：火把等光源可阻止和解除恐惧；200 tick 门槛、状态效果、心跳节奏、随机事件及 Luminescence 免疫均与旧包一致。

### 7. 恢复 Tony、任务提示和世界事件

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：Tony 的随机出现、猫实体表现、音效、计时和相关进度未实现；依赖 Tony 或世界事件提供的主线引导、物品和剧情节点中断。

**检查入口**：

- `_nyctocosm_source/datapack/data/nyctocosm/functions/tony/`
- `_nyctocosm_source/datapack/data/nyctocosm/advancements/tony/tonyspawn.json`

**验收标准**：明确 Tony 在主线中的全部职责，并恢复生成条件、交互、奖励、冷却、多人归属和存档行为；玩家不查外部攻略也能获得下一步方向。

### 8. 核对结构生成、箱子内容与剧情书推进

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：结构几何和主要战利品已导入，但结构中的实体/命令载体被移除，尚未证明所有结构事件仍会发生；书籍内容已导入，但完整结构发现和章节推进链未跑通。

**已知错误**：Slatehenge 原战利品表最后一项是 Extraction artifact，模组表中却出现两个 Transposition，导致该位置无法抽到 Extraction。

**检查入口**：

- `nyctocosm-native/docs/structure-import.json`
- `_nyctocosm_source/datapack/data/nyctocosm/loot_tables/structures/`
- `nyctocosm-native/src/main/resources/data/nyctocosm/loot_tables/structures/`
- `nyctocosm-native/src/main/resources/data/structures/loot_tables/crypt/`

**验收标准**：所有结构能在预期群系和频率生成；箱子、剧情书、饰品和关键材料的权重及数量正确；结构中被移除的实体都有原生替代行为。

### 9. 完成祭坛建造、交易和灌注演出

- [ ] 审查
- [ ] 修改
- [ ] 验收

**已恢复**：方块排列扫描和四种主要兑换比例。

**当前差距**：旧包的交易载体、投料、环形模型、物品悬浮和完整灌注动画被普通容器界面及即时扣料替代。

**检查入口**：

- `_nyctocosm_source/datapack/data/crafting/functions/dais/`
- `_nyctocosm_source/datapack/data/inculcate/`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/DaisRitual.java`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/DaisMenu.java`

**验收标准**：建造失败不消耗材料；四配方、材料计数、背包已满、多人同时使用、动画、声音和最终产物全部正确。

### 10. 跑通远古城市传送门与往返流程

- [ ] 审查
- [ ] 修改
- [ ] 验收

**已恢复**：Resonance 射入门框内部、80 tick 蓄力、70 格射线、三波共振、122 tick 后开门、双轴向和状态保存。

**剩余差距**：门面仍是原生方块表现；缺常驻 `portal.hum` 和完整表面细节；爆炸不破坏地形；镜头抖动不改变玩家瞄准；传送使用新的安全落点和返回栈。

**验收标准**：从正常生存获取回响碎片开始，完整激活、穿越、返回、破框失效、区块卸载和多人同时穿越均可靠；明确哪些安全性差异决定保留，不再误标为等价。

---

## P1：Boss 主线与结局

### 11. 核对 Incurrence 的群系触发和 Boss 入口

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前模组**：按玩家所在群系直接选择 Serpent、Sorcerer 或 Emissary；Emissary 先传送到专用领域。

**验收标准**：材料消耗、施法时间、群系判断、失败提示、Boss 已存在时的处理、多人目标归属及领域传送与旧包一致。

### 12. 完成 Serpent 全战斗实机等价审查

- [ ] 审查
- [ ] 修改
- [ ] 验收

**已恢复**：游弋、61 tick 盘旋、Dive、Laser、20 柱 Beam、40 节身体、20 个受击部位、死亡时序和主要掉落。

**待核对**：完整原包调度、目标切换、跨区块实体顺序、胡须/尾部粒子、接触判定、多人长时间战斗、声音逐拍及第三方大型实体。

**检查入口**：

- `nyctocosm-native/docs/SERPENT_BEHAVIOR.md`
- `_nyctocosm_source/datapack/data/serpent/functions/`

**验收标准**：相同场景双版本连续录像和逐 tick trace 对齐；三种攻击的选择、进入、持续、退出、读档恢复和死亡奖励均通过。

### 13. 完成 Sorcerer 六阶段与 12 招全量核对

- [ ] 审查
- [ ] 修改
- [ ] 验收

**已恢复**：六阶段、Nullpoint、升降杖、法环生命周期、12 招主要关键帧及 Boss 音乐。

**待核对**：每招完整运动、伤害体积、方块阻挡、粒子几何、音效帧、目标状态效果、阶段权重和多人行为；法环创建冲击波仍简化。

**检查入口**：

- `nyctocosm-native/docs/SORCERER_PLANE_BEHAVIOR.md`
- `_nyctocosm_source/datapack/data/sorcerer/functions/`

**验收标准**：12 招逐项建立时间轴；旧包与模组同场景录像和伤害 trace 对齐；Nullpoint、阶段切换、读档与死亡奖励全部通过。

### 14. 重做 Emissary 四阶段完整战斗

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：虽然保留了四阶段和招式名称，但每招被压缩为 Beam、Volley、Rain、Ring、Dash、Pillars、Pull、Melee 等通用模板；旧包的独立移动、连段、格挡、飞行、抓取、反击、预警和专属 VFX 大量缺失，选招权重也不等价。

**检查入口**：

- `_nyctocosm_source/datapack/data/emissary/functions/attacks/`
- `_nyctocosm_source/datapack/data/emissary/functions/ai/`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java`

**验收标准**：四阶段分别形成行为规格；每个招式按距离、状态和权重正确选择；动作、命中、VFX、音效、转阶段、飞行和存档恢复均通过实机对照。

### 15. 恢复 Emissary 击败奖励、领域逃生和结局

- [ ] 审查
- [ ] 修改
- [ ] 验收

**旧包行为**：向参战者播放击败音效和标题，发放 Adherent Emissary，授予成就，向领域内玩家提供逃生入口，并生成总计 12000 经验。

**当前模组**：完整奖励函数直接跳过 Emissary；仅保留普通掉落，经验值设为 100，缺少 Adherent III、成就、逃生提示和结局收束。

**检查入口**：

- `_nyctocosm_source/datapack/data/emissary/functions/finaldeath.mcfunction`
- `_nyctocosm_source/datapack/data/emissary/functions/escape.mcfunction`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java`

**验收标准**：所有在线参战者获得正确奖励；未参战者不获得；领域内玩家可安全离开；经验、掉落、章节和成就只结算一次；重登和区块卸载不重复结算。

---

## P2：构筑、法术与战斗手感

### 16. 完成六元素属性、弱点、加成与耗散公式

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：法术和武器大量使用固定伤害、半径和持续时间；旧包的 global power、六元素 power/weakness、伤害追踪和耗散尚未等价迁移。

**检查入口**：

- `_nyctocosm_source/datapack/data/magic/functions/calcstatsdamage.mcfunction`
- `_nyctocosm_source/datapack/data/magic/functions/calcstatsspell.mcfunction`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Combat.java`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/Spell.java`

**验收标准**：为每个公式建立输入/输出样例；基础值、全局加成、元素加成、弱点、治疗和状态持续时间逐项对齐。

### 17. 完成 Hypertrace 的生成、容量、消耗和饰品联动

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：模组使用固定 15% 基础概率、最多 8 个实体和固定衰减；旧包拥有最大 charge、概率修正、可视电荷、玩家绑定和更多公式联动。

**验收标准**：生成概率、最大数量、charge 上限、自然耗散、被玩家击破、粒子显示及所有饰品联动与旧包一致。

### 18. 修复 Planar Coil 与 Transposition 跟随规则

- [ ] 审查
- [ ] 修改
- [ ] 验收

**已知差距**：Planar Coil 创建法阵时直接设置 `mobile=true`，会绕过“背包中必须有 Transposition artifact”的旧规则。

**检查入口**：

- `nyctocosm-native/docs/FOLLOW_UP_FIXES.md`
- `_nyctocosm_source/datapack/data/artifacts/functions/transposition/`
- `_nyctocosm_source/datapack/data/spells/functions/ring/`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/FocusItem.java`
- `nyctocosm-native/src/main/java/com/flubberschnub/nyctocosm/SeelicField.java`

**验收标准**：无 artifact 时固定，有 artifact 时跟随；覆盖放入/移出背包、普通法阵、Hypertrace、死亡、重载和多人归属。

### 19. 逐一还原 17 种法术

- [ ] 审查
- [ ] 修改
- [ ] 验收

优先顺序：

1. Submergence：恢复原触手，而非直接选取六个目标伤害和缓慢。
2. Reflectance：恢复原反射器的几何、法术/武器交互和持续时间。
3. Forbiddance：核对反制对象、半径、触发时机和消耗。
4. Gravitance：核对落点、吸引曲线、持续伤害、碰撞箱距离和消散。
5. Transmittance、Vacance、Repellance、Adamance：核对方块、实体、投射物和传送边界。
6. 其余伤害、治疗、照明、抗性和 Boss 召唤法术。

**验收标准**：每种法术都有旧包/模组行为表，覆盖施法、取消、材料消耗、命中、失效、读档、多人和第三方生物。

### 20. 逐一核对 13 种 artifact

- [ ] 审查
- [ ] 修改
- [ ] 验收

重点差距：Levitation 当前给予 Levitation II，而旧包给予 Slow Falling；Protection、Accumulation、Absorption、Amelioration、Expulsion、Extraction 的公式、半径、粒子和 Hypertrace 消耗仍需逐项对照。

**验收标准**：每种 artifact 的持有检测、触发条件、数值、消耗、冷却、视觉、移出背包和多人行为均有测试。

### 21. 核对四种武器的命中体积、位移和副手能力

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：模组已恢复基础攻击和主要粒子，但部分旧包使用密集命令采样形成特殊挥击/射线体积；模组改成原生包围盒、锥形筛选或投射物，边缘命中和方块交互会不同。

**验收标准**：第一/第三人称、地面/空中、主手/副手、多人观察、方块边缘、大型实体和反射器交互全部对照。

---

## P3：掉落、叙事与表现

### 22. 核对所有 Boss、普通怪和结构掉落

- [ ] 审查
- [ ] 修改
- [ ] 验收

重点：

- Serpent 旧包是两次独立 `12–20` Nightmare Scale，模组是一次均匀 `24–40`，范围相同但概率分布不同。
- Sorcerer 三轮二选一、Seelic Essence 和经验值。
- Emissary 的 30% Semblance、Seelic Essence、12000 经验和章节奖励。
- Death Walker / Apparition 的掉落及模组额外加入的 10% 镰刀是否保留。
- Slatehenge 的重复 Transposition / 缺失 Extraction。

**验收标准**：轮数、权重、数量分布、Looting、经验和参战者奖励均一致。

### 23. 补全成就、Boss 标题、字幕、世界文本和剧情触发

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：模组只有 Serpent 与 Sorcerer 的击败成就；旧包的 root、Tony、Emissary 和若干检测型 advancement 未完整迁移。部分字幕、世界文本和剧情触发缺失。

**验收标准**：所有玩家可见文本和音效字幕有中英文；章节只推进一次；Boss 标题、作者、悬浮解释和剧情顺序正确。

### 24. 完成 Boss、法术、祭坛和传送门的逐帧/逐拍表现对照

- [ ] 审查
- [ ] 修改
- [ ] 验收

**当前差距**：导入模型、贴图、声音和动画矩阵不等于动作选择、轴向、根变换、粒子密度及声音帧已经等价。

**验收标准**：相同初始场景下分别录制旧包和模组；对齐动画帧、粒子、声音、镜头、发光层、第一/第三人称和低帧率补帧行为。

### 25. 明确并处理法语及其他语言回退

- [ ] 审查
- [ ] 修改
- [ ] 验收

**旧包**：包含 `fr_fr`、`fr_ca`。

**当前模组**：只提供 `en_us` 与 `zh_cn`，其他语言回退英文。

**验收标准**：决定是否迁移旧法语；所有已支持语言中不显示裸翻译键，剧情书和术语悬浮文本完整。

---

## P4：存档、多人和兼容性验收

### 26. 制定旧存档迁移或正式不兼容方案

- [ ] 审查
- [ ] 修改
- [ ] 验收

需处理：旧 CustomModelData 物品、灵魂数量、剧情记分板、Boss/法阵 marker、传送门状态、返回位置和已生成结构。

**验收标准**：提供可重复的迁移测试；若决定不兼容，游戏内和发布说明明确阻止用户直接覆盖重要旧存档。

### 27. 完成多人主线验证

- [ ] 审查
- [ ] 修改
- [ ] 验收

覆盖：物品归属、灵魂争夺、法阵归属、祭坛并发、Boss 目标切换、参战奖励、剧情章节、传送门、领域逃生、玩家中途上下线和跨维度死亡。

### 28. 完成第三方生物与特殊防御兼容验证

- [ ] 审查
- [ ] 修改
- [ ] 验收

覆盖：大型碰撞箱、多部位实体、主动取消伤害、无敌阶段、护盾、特殊投射物和非标准移动；不得为了还原旧包而绕过 Forge 伤害事件。

### 29. 完成完整主线最终回归

- [ ] 审查
- [ ] 修改
- [ ] 验收

最终要求：

- 全新单人生存从开局到结局连续完成。
- 两名以上玩家从开局到结局连续完成。
- 中途多次退出、重载和卸载区块后仍可继续。
- 不使用测试指令、刷怪蛋或创造栏补齐缺失物品。
- 每个关键节点保存旧包与模组的对照证据。
- 已决定保留的原生化差异单独列出，不再标记为遗漏。

---

## 已决定可能保留、但必须显式验收的原生化差异

- [ ] 多个 Boss 状态互相独立，不复刻旧包全局记分板互相干扰。
- [ ] 伤害走 Forge 事件，可命中普通第三方 `LivingEntity`，并尊重取消伤害、护盾和无敌。
- [ ] 跨维度传送使用安全落点搜索和最多 32 层返回记录。
- [ ] 传送门爆炸只保留视觉，不破坏地形。
- [ ] 开门镜头抖动不永久改变玩家瞄准方向。
- [ ] 客户端本地批量生成大量粒子，视觉结果对齐但不复刻旧包网络包数量。
- [ ] 原生注册物品、实体、方块和界面取代 CustomModelData、marker、盔甲架及隐藏村民载体。

---

## 实施日志（按阶段追加）

### 阶段 1：开局、灵魂、自然生成与夜域事件（2026-09-23）

- 恢复 Seelic Scythe 生存合成配方（3 个回响碎片 + 2 根木棍）；移除了旧包不存在的 Dais 和 Soul Bottle 便捷配方。
- 新增可存档、100 tick 寿命的灵魂实体；镐刀击杀改为生成灵魂，玩家需在 5 格内用主手玻璃瓶、主手 Soul Bottle 或副手 Planar Coil 捕获；多人时最近的合法捕获者获得该灵魂，避免同 tick 复制。
- 新增夜域 illusioner 原生替换：Night Wastes 按 50% 转为 Nightmare Apparition（上限 30），Cimmerian Gorge 按 20% 转为 Death Walker（上限 3），其余未转换 illusioner 不留在世界中。
- 重建恐惧循环：亮度低于 5 累计 200 tick 后触发，包含 Darkness、Slowness II、逐渐加速的心跳与三种随机听觉事件；光照、Luminescence、创造/旁观会立即清除。
- 恢复 Dream Crypt 中的 Tony 事件：结构+零亮度触发、12000–23999 tick 个人冷却、黑猫生成与 200–399 tick 停留、出现/消失表现、掉落 Dreams 剧情书。
- 本阶段涉及：`Content.java`、`NativeEvents.java`、`SoulEntity.java`、`TonyManager.java`、`client/ClientEvents.java`、配方数据。尚需在后续统一编译和测试阶段验证 Forge API 与服务端行为。

### 阶段 2：普通怪、结构、仪式、传送门与结局链（2026-09-23）

- Death Walker 不再复用通用 Boss 追击模板：恢复巨人级 3.6×12 碰撞箱、100 生命、闲置/漫步/追踪/砸击状态、30/7 格触发距离、第 50 帧的 15 点范围伤害、远离玩家后的延时清理；移除了旧包不存在的 10% 镐刀额外掉落。
- Nightmare Apparition 恢复上下漂浮、呢喃、面向玩家、静默逼近、跑动/非潜行警觉、200–249 tick 追杀窗口、近身瞬间伤害+凋零、遇光后退、消散表现与远距离清理；还原为不可常规攻击。
- 修正 Slatehenge 战利品表：第二个重复 Transposition 已替换为 Extraction；其他两张主要结构表的原生物品替换项保持不变。
- Dais 成品取出时新增原版 `dais.inculcate` 声音和灵魂火/附魔粒子反馈；原子扣料和背包容量保护保留。
- 开启后的门面恢复随机周期 `portal.hum`；现有轴向、框体失效、返回栈与安全落点继续保留。
- Incurrence 增加每种 Boss 的世界级唯一性检查；Emissary 在传送前会检查专用领域，避免反复创建战斗。
- 补全 Emissary 结算：12000 经验、参战者标题/音效/Adherent III、成就、领域内玩家可点击的逃生提示；`/nyctocosm return` 改为生存玩家可用，而 summon/travel 测试指令仍需 2 级权限。
- 重建进度树 root，首次进入夜域授予；三个 Boss 成就改回 root 子项。

### 阶段 3：Hypertrace、artifact、掉落、语言与旧物品兼容（2026-09-23）

- Hypertrace 重新分离为“场上可拾取实体”和“玩家电荷”：右键自己的 Hypertrace 拾取 1 电荷，默认上限 5，攻击则走 artifact 销毁联动；场上寿命修正为旧包的 400 tick。
- Transposition 成为法阵跟随的唯一条件；Planar Coil 只决定环形模型，不再绕过 artifact；持有 Transposition 时每 200 tick 消耗 1 电荷。
- Levitation 修正为每次按下潜行消耗 3 电荷并给予 5 秒 Slow Falling I，长按不会重复扣除；Protection 改用玩家电荷按每点 3% 计算。
- Accumulation 修正为每个附近 Hypertrace +10% 生成率；Expulsion 修正为 6 格、7 基础伤害；Extraction 改为生成可捕获灵魂，不再直接写入背包容器。
- Serpent 的 Nightmare Scale 数量改为两次独立 12–20 抽样的三角分布；Sorcerer 和 Emissary 的现有轮次/权重保留；普通怪 XP 回调。
- 迁移旧包 `fr_fr` 和 `fr_ca` 语言文件，未新增的原生模组键由英文回退。
- 新增登录时旧物品迁移：根据旧 NBT 标记转换 Soul Bottle、Planar Coil、四把武器、13 种 artifact、四种 Boss 材料及七系列剧情书，并保留灵魂数。
- 本阶段后 `compileJava` 已通过；仅有项目原有的 deprecated API 警告。

### 阶段 4：Emissary 四阶段战斗与选招（2026-09-23）

- 移除 Emissary 共用的 Beam/Volley/Rain/Ring/Dash 压缩模板，按旧包恢复四阶段独立选招池、距离分支、左右横移、近身/远离调整以及第四阶段地面—飞行轮换。
- 恢复 Lightning Javelin、Meteor Shower、Pillar Javelins、Wingbeat、Celestial Smite、Flame Geysers、Sword Plunge、Wing Blast、Cross、Flame Slash、Ground Slam、Lightning Field、Seeking Missiles、Sword Spin/Swing、Front Kick、Apocalypse Rain、Corkscrew、Divine Blast、Fireball、Godrays 和 Grapple 的独立时间线与命中逻辑。
- 新增飞行状态及关键攻击点的 NBT 存档，避免区块卸载后回到错误选招池或丢失落点。
- 新增 GameTest 选招池边界检查，覆盖四阶段、近远距离和地面/飞行状态。`compileJava` 已通过。

### 阶段 5：高优先级法术与存档迁移补强（2026-09-23）

- Submergence 不再瞬时对六个目标造成伤害；改为每个目标生成可存档的 200 tick 触手效果，前 80 tick 拘束与上浮，第 81 tick 结算伤害，随后拖向地下锚点，并恢复灵魂火触手轨迹。
- Transmittance 恢复法阵上 3 格内的多玩家运输；每位乘员按自身视线进行旧包基础 100 格的 passthrough/实体阻挡射线，正确处理潜行落点、碰撞检查和 6 秒 Slow Falling，移除了旧包实际未使用的落点范围伤害。
- 旧存档迁移扩展到 `m.hypertrace.charge`、`darknessmeter`、`tonytimer` 和四条 `l.storyline*` 记分板；仅首次登录导入，不会反复覆盖模组进度。
- 已经被旧包替换成成品的 Written Book 现可根据 Entry/Precept/Insight/Theorem 标题识别系列和章节，而不只能识别尚未处理的 `storyline` 占位书。
- 本阶段后 `compileJava` 已通过；实机视觉、多人运输和存档样本仍留给最终验收。

### 阶段 6：Forge GameTest 回归（2026-09-23）

- 首次运行 `runGameTestServer` 发现 3/70 失败。法术投射物用例仍以 Death Walker 旧 80 血为断言，新尺寸/血量为 100；修成“相对于发射前扣血”。
- Sorcerer Void Beam 阻挡用例的石墙只到 9 格高，而 Death Walker 高 12 格；结果光线合法越过墙顶命中。将墙覆盖整个目标身体，保留旧包 passthrough 阻挡断言。
- Emissary 选招新测试的期望动画名与真实资源名不同，已对齐导入的 clip ID。再跑 `runGameTestServer`：70/70 通过。

### 阶段 7：Hypertrace 边界值与新机制回归（2026-09-23）

- 旧包的 Hypertrace 生成是对 0–99 的整数随机值做包含上界的 `<= tracechance` 比较；基础分数 15 实际覆盖 16 个随机值。原生版改为相同的整数抽样，Accumulation 每个附近超迹继续加 10 个分数点。
- Hypertrace 容量默认 5，可保留旧存档 `m.hypertrace.maxcharge` 的 0–15 上限；每 tick 按上限校正当前电荷，补入旧包橙色电荷颗粒显示。
- 新增灵魂必须使用玻璃瓶捕获、Submergence 第 81 tick 才造成伤害且中途存档可恢复、旧剧情书第四章转换三项 GameTest。`runGameTestServer` 最新结果为 73/73 通过。

### 阶段 8：Reflectance、Vacance 和 Repellance（2026-09-23）

- Reflectance 立起旧包竖向法圈，仅对迎面而来的本模组法术/武器弹体反射；每次反射按入射伤害扣除初始 20 点反射容量，耗尽即消失。反射后清除追踪目标，容量随实体存档。
- Submergence 触手只使用粒子轨迹，不再绘制额外的静态法圈。Reflectance 20 点容量的回归 GameTest 通过；连同前述用例，最新服务端结果 74/74 通过。
- Vacance 恢复每 tick 清理半径 6 格立方内 `vacable` 方块，并按受影响实体自身朝向推动；Repellance 恢复 26 tick 聚集、随后每 tick 前进 2 格、碰到非 passthrough 方块消失与 6 格影响范围。本步 `compileJava` 通过。

### 阶段 9：旧存档风险提示与法术读档回归（2026-09-23）

- 旧存档登录时，若检测到旧版记分板或发生旧物品转换，向玩家展示一次红色迁移风险提示；README 和迁移说明明确区分“玩家物品/部分分数可导入”与“世界级 marker、Boss、传送门状态不兼容”，避免把直接覆盖旧世界误认为完整迁移。
- Repellance 的 26 tick 聚集阶段改用已存档的剩余寿命计算，避免区块卸载、重载后重新蓄力；新增读档后按第 27 tick 发射的 GameTest。
- Adamance 对照旧包改为不受 12 格距离限制地锚定施法者，并恢复每 tick 的 Levitation 255；潜行暂停位置锚定。新增远距与潜行 GameTest。
- 本轮完整 `runGameTestServer` 最终为 76/76 通过。自动测试无法替代客户端逐帧表现、多人和完整生存主线验收；这些验收框仍保留未勾选。

### 阶段 10：Forbiddance 对传送门的反制（2026-09-23）

- 对照旧包确认 Forbiddance 除清除其他法术投射物外，还会中断附近正在开启或已经开启的传送门；此前模组遗漏这一交互。
- 为已开启的原生门面补充连通面清理，并让反制弹体可中断尚未生成门面的 PortalActivation。清理仅作用于被命中的那一座门，避免把旧包的全局 marker 清理副作用带入多人世界。
- 增加“关闭门面且相邻另一座门保持开启”和“开门过程中靠近边缘时中断”两项 GameTest；完整 `runGameTestServer` 为 78/78 通过。

### 阶段 11：交付前构建与资源审计（2026-09-23）

- `gradlew build` 完整通过，生成 `0.3.0-native-alpha.11` 的 Forge JAR；不是仅通过 Java 编译。
- 对生成的 JAR 运行项目现有 `tools/audit_native.py`：1778 个模型、10 个 rig、5607 帧、247 个声音事件均通过检查；未打包运行时 mcfunction 或开发测试内容，审计错误为 0。
- 资源审计结果写入模组项目的 `docs/native-audit.json`。中英语言 JSON 均可解析，`git diff --check` 无空白错误（Git 仅提示 Windows 换行符转换）。

### 阶段 12：群体增益法术（2026-09-23）

- 核对旧包 `resistance/transmutate` 后发现 Resistance 应覆盖法阵周围 2 格内所有玩家，而非仅施法者；已改成多人范围增益，并精确按半径筛选，不让包围盒角落的玩家越界获益。
- Subsistence 原本已遍历附近玩家，但只有立方盒筛选；补充旧包 `distance=..3` 的球形距离限制。
- 新增 Resistance 的施法者、近处队友、远处玩家三方 GameTest；完整 `runGameTestServer` 为 79/79 通过。该阶段修改发生在阶段 11 的打包审计之后，需在最终交付前重跑 JAR 构建与审计。

### 阶段 13：Luminescence 的多人覆盖（2026-09-23）

- 旧包在施法时给法阵 1 格内的所有 `in_plane` 玩家赋予 luminous，模组此前只给施法者；现在为范围内每位玩家分别记录发光免恐惧期限和夜视效果。
- 扩充原有“只消耗自己投下的材料”测试，同时验证近处另一名玩家得到 Luminescence、2 格外玩家没有得到；完整 `runGameTestServer` 仍为 79/79 通过。

### 阶段 14：投射法术的法阵原点与朝向（2026-09-23）

- Abhorrence、Incandescence、Fulminance、Forbiddance、Gravitance 旧包从法阵上方 1.625 格发射、按附近玩家的视角定向；模组原来从施法者当前眼位发射。现已改回法阵原点，Gravitance 的水平前移 3 格也相对于法阵计算。
- Reflectance 改为从法阵上方沿附近玩家朝向前移 4 格放置反射器；Repellance 的聚集位置改回法阵上方，不再跟着施法者当前眼位移动。法术归属依旧属于原施法者。
- 新增“施法者离开固定法阵、另一名玩家在法阵旁瞄准”GameTest；投射物仍从法阵发出。完整 `runGameTestServer` 为 80/80 通过。

### 阶段 15：本轮最终自动验证（2026-09-23）

- 重新执行 `gradlew build`，Forge JAR 构建成功；重新审计该最新 JAR，模型、动画、声音和禁用的运行时数据包内容检查均通过，错误 0。
- `runGameTestServer` 结果为 80/80。尚未把 TODO 项的“验收”勾选为完成，因为旧包/模组双客户端逐帧对照、无指令完整生存路线、两人以上连续联机与第三方模组兼容性必须经过实际游戏测试。

### 阶段 16：重新核对风系法术，修正前次判断（2026-09-28）

- 重新追踪 `spells:__generated__/block/115 → 118 → 119` 的命令上下文：Vacance 的 `facing entity @s` 从气泡位置看向受影响实体，实际是径向排斥；Repellance 的 `block/125 → 135 → 136` 继承风团朝向。阶段 8 将两者记录为“实体自身朝向”不正确，本阶段改正实现与结论。
- Vacance 改为从中心向外推动，Repellance 改为沿风团方向推动；两者按 6 格球形距离筛选，修掉立方范围角落越界受影响的问题。
- Repellance 的 26 tick 聚集独立计时并保存；旧包第 26 tick 移除 gathering 标签后会在同 tick 开始运动，随后有完整 50 次、每次 2 格的运动。此前实现把蓄力算进 50 tick 寿命且第 27 tick 才发射，射程明显缩水；现已更正。
- 重新读取完整 `repellance/transmutate.mcfunction` 后确认生成点应为法阵上方 1.625 格、沿附近玩家朝向再前移 3 格；已更正阶段 14 的遗漏，并恢复沿途清除 `vacable` 方块。
- 增加方向、球形边界和完整 100 格运动/存档回归。首次回归发现原测试高空偏移仍位于 GameTest 世界地下岩层，已改用世界最高高度下方的空中走廊，最终结果见后续验证日志。

### 阶段 17：Protection 耗电与 Levitation 触发条件（2026-09-28）

- `artifacts:__generated__/block/0–2` 明确由六类 Protection 共同触发一次耗电计时，而非 Transposition。修掉错误的 Transposition 每 200 tick 扣电；Protection 持有累计 200 tick 扣 1 电荷，多种保护不重复扣费，移除时暂停、重新持有后续计，进度随玩家存档保留。
- `levitation/subtick` 要求脚下两格为普通空气，没有“必须已开始下落”的条件。恢复该条件，可在上升阶段提前按潜行获得 Slow Falling；长按仍只消耗一次 3 电荷。
- 补充共享耗电计时、移除/恢复、玩家数据复制，以及上升时触发/脚下遮挡/长按不重复扣费回归。

### 阶段 18：Expulsion 延迟连锁与大型目标范围受击（2026-09-28）

- 原包 `expulsion/__generated__/block/27–31` 的击杀标记等待 15 tick 才爆炸；此前模组在死亡事件内立即爆炸并同步递归触发连锁。新增可存档的 ExpulsionBurst 实体，恢复逐波延迟，避免密集生物死亡时同一调用栈递归爆炸。
- 击破 Hypertrace 的 Expulsion 仍即时爆炸，与旧包独立路径一致；恢复爆炸/火焰/熔岩粒子和爆炸声音。
- 通用范围伤害改为检测球体是否接触目标碰撞箱，修掉爆炸接触 Death Walker 脚部却因为身体中心过高而未命中的情况；投射物直接命中后备分支同步更新，避免重复提交同一次伤害。
- 新增延迟连锁中途读档和大型目标脚部受击回归；首轮 85 项中这些新增用例已通过，另两项失败继续定位中。

### 阶段 19：Forbiddance 的反制对象和时序（2026-09-28）

- 对照 `spells:__generated__/block/61、65、66`：Forbiddance 穿过方块，第 3 tick 开始反制 5 格内非同归属的法术；一次脉冲处理范围内全部符合对象，固定法阵取消施法后保留。此前模组会撞墙、立即反制、只删除第一个投射物，也会误删原版箭。
- 已将反制对象限定为本模组法术弹体、持续法术和施法法阵；不删除普通箭或己方法术。固定法阵清空施法进度与 Resonance 蓄力，Transposition 移动法阵按旧包取消逻辑消散；第三帧计时随弹体保存。
- 增加“墙内穿过、两帧宽限、读档后第三帧反制、同时处理多个敌方法术、保留己方法术/普通箭/固定法阵”的完整用例。该阶段后的第一次完整回归为 86/86 通过。

### 阶段 20：保护法术与强制位移联动（2026-09-28）

- 旧包 `block/84、92` 每 tick 为 Reflectance 后方 3 格区域内玩家及 Adamance 施法者设置 `s.immoveable`；Gravitance、Repellance、Submergence 均检查该标记。模组之前遗漏了这一整条联动。
- 恢复短时刷新、自动到期的免强制位移状态：Reflectance 后方保护与 Adamance 均可阻止上述三种法术的拉拽/选取，效果消散后不会残留永久免疫；Vacance 按原包没有该排除条件。
- Submergence 同时补上 7 格上界的球形筛选，避免立方盒角落的 7 格外目标被选中；增加 Reflectance 阻止 Gravitance、消散后恢复受影响的回归。

### 阶段 21：开局灵魂容器循环复核（2026-09-28）

- `items:__generated__/block/0–4` 要求 5 格球形范围，捕获容器按主手玻璃瓶、主手 Soul Bottle、副手 Planar Coil 依次选择；修掉立方盒角落超距捕获，以及把主手 Planar Coil 误当作 Soul Bottle 的问题。
- `spells:bottle/subtract` 在用完最后一魂时把容器替换为玻璃瓶；模组此前留下无法开启法阵的空 Soul Bottle。现恢复玻璃瓶并保持最后一次法阵创建成功；Planar Coil 继续保留本体。
- `bottle/add` 与 `ring/add` 没有 999 上限，移除模组额外的 999 截断，避免大量库存或旧物品迁移后继续收魂时损失已有灵魂；使用 long 中间运算避免整数加法溢出。
- 扩充捕获回归，覆盖主副手、5 格边界与立方角落；新增最后一魂返回玻璃瓶用例，并检查超过 999 的数量保存。

### 阶段 22：回归失败定位与本轮测试收敛（2026-09-28）

- Serpent 的原版命令轨迹对照测试在测试场地布局变化后出现偶发失败。逐条检查命令返回值后确认：对照 marker 跨到未进入实体 ticking 状态的区块时，原版 `tp` 命令返回 0，并非已经验证的 SerpentMotion 再次偏离。测试现临时强制加载轨迹周围区块、等待其就绪，并断言每条对照命令执行成功；在四种原点偏移下比较，结束后只解除本测试新增的强制加载。未据此修改生产用 SerpentMotion。
- 最后一魂的测试首次失败，按 `diagnose` 流程检查本机 Minecraft 1.20.1 类实现后确认 `GameTestHelper.makeMockPlayer()` 的 `isCreative()` 固定返回 true，恰好跳过生存模式消耗分支。改用明确设置为生存模式的独立 FakePlayer，避免把测试前提错误当作生产代码错误。
- 同一测试补充空 Planar Coil 保留本体、创造模式不消耗 Soul Bottle 两个边界，玻璃瓶返还和最后一次法阵生成均通过。
- 最终完整执行 `gradlew runGameTestServer --offline`：**88/88 必需测试通过，BUILD SUCCESSFUL**。相较阶段 15 的 80 项增加 8 项，并扩充了既有灵魂、物品存档和 Serpent 轨迹用例。

### 本轮对照结论与实机复测顺序（2026-09-28）

以下是本轮已确认并落实到代码的差距；原 TODO 正文保留的是首次审查基线，最新进度以尾部阶段日志为准。验收框不代替用户实机结果，仍不提前勾选。

1. **开局灵魂循环**：恢复容器主副手限制、5 格球形收魂范围、最后一魂返还玻璃瓶，移除额外的 999 灵魂截断；实机优先检查从镰刀击杀到连续开启法阵的流程。
2. **Protection / Levitation 资源与触发**：恢复 Protection 共享耗电计时、移除错误的 Transposition 周期耗电；Levitation 根据脚下两格空气与潜行触发，可在上升时使用。
3. **风系法术位移与射程**：纠正 Vacance 径向推离、Repellance 沿风团朝向推动，恢复施法偏移、26 tick 聚集后完整 100 格运动及沿途清除对象；需要客户端比较实际受推手感和粒子。
4. **Expulsion 战斗连锁**：击杀爆炸恢复 15 tick 延迟和分波连锁，中途存档保留倒计时；击破 Hypertrace 仍即时触发。爆炸接触大型目标身体边缘也能受击。
5. **Forbiddance 战术反制**：穿墙、第 3 tick 起在 5 格内清除多个敌方法术，保留普通箭与己方法术；固定法阵取消施法后仍保留，移动法阵按取消规则消散。
6. **保护与强制位移联动**：Reflectance 后方范围、Adamance 持续提供免强制位移，Gravitance / Repellance / Submergence 遵守该状态；保护结束后恢复正常，Submergence 选取遵守 7 格球形上界。

本轮验证属于旧包命令语义对照、原生服务端 GameTest 和构建检查，尚不能宣称整包游戏表现完全一致。原清单中的完整生存主线、Boss 全招式逐帧观感（尤其 Emissary）、客户端视听、连续多人游玩与第三方模组兼容，仍需实机对照验收。

### 阶段 23：最终构建与打包审计（2026-09-28）

- 在 88/88 GameTest 通过后执行 `gradlew build --offline`，结果 **BUILD SUCCESSFUL**。保留当前版本号 `0.3.0-native-alpha.11`，本次生成的 JAR 已包含阶段 16–22 的代码。
- 对最新 JAR 运行 `tools/audit_native.py`：1778 个模型、10 个 rig、5607 帧动画、247 个声音事件均通过；没有运行时 mcfunction、开发 GameTest 或客户端烟测类混入发行包，审计错误 **0**。
- 产物：`nyctocosm-native/build/libs/nyctocosm-native-0.3.0-native-alpha.11.jar`；SHA-256：`d86ab98b7d920e80272064c598281ec8dbc8e7021202538bd19c00fc44035091`。完整审计记录位于 `nyctocosm-native/docs/native-audit.json`。
- `git diff --check` 无空白错误；编译仅有现有 `ResourceLocation(String,String)` 弃用警告。未改动 `_nyctocosm_source`，未启动客户端代替用户作实机验收，未部署到用户存档或服务器。

### 阶段 24：Emissary 战斗重新审查与失败回归（2026-09-28）

- 用户指定先修 Emissary。按 `diagnose` 流程，新增真实攻击入口的三项回归，首次运行均复现：大幅掉血跳过中间阶段、Grapple 抓空仍对远处目标扣血、读档取消已开始的攻击。首轮 91 项另有既有 Sorcerer 陨石持续测试失败，记录在后续回归中跟踪，未据此修改 Sorcerer 生产逻辑。
- 对照 `emissary:ai/subtick` 与 `ai/phase/transition`：改为当前招式结束后逐阶段转换，转换动作期间无敌，阶段号到转换结束才推进；转换与攻击计时可保存恢复。致命伤害在 Forge 伤害事件、护甲和吸收结算后才判断，前阶段或招式未结束时暂缓最终死亡，避免跳过整个 Boss 流程。
- 对照原动画命令帧：Lightning Javelin 不再拖到第 50 帧才投出；Fireball 恢复第 8/18 帧双发与第 20 帧 50% 接续；Apocalypse Rain 发射窗口改为第 19–44 帧；Seeking Missiles 补上第一枚。
- 第三阶段不再错误使用第二阶段横移。近战恢复各自前冲与导出交互区域的五 tick 反制窗口，玩家攻击该区域可打断招式；累计 7 次反制进入 60 帧破防动作。恢复原连招节点的 70% 再选招，而非每次等完整收招。
- 首轮修正后完整回归 **91/91 通过**。抓取挂载、飞行轨迹、反制/破防与伤害取消边界仍在补充测试，不以此作为最终交付结论。

### 阶段 25：Emissary 抓取、飞行、近战验证（2026-09-28）

- Grapple 改为第 15 帧开始尝试实际接触抓取，未接触不扣血、不强制转入抓住动作；抓住后使用原生座位挂载，可同时携带附近玩家。仅碰到非穿透方块时产生撞击伤害，空中不再凭计时扣血，第 160 帧释放，Boss 消失时座位兜底释放乘客。
- 恢复源函数的飞行转向构造、半格子步进、抓取/螺旋突进快速转向、起飞第 14 帧与落地第 4 帧的状态切换；飞行目标、抓取归属和攻击阶段均可保存。
- 补测五 tick 近战延迟命中、七次反制破防、真正接触后挂载、释放帧、座位失主释放、飞行转向/起降、吸收伤害不误触发延迟死亡。使用真实生存 Player 测试替身，避免 Forge FakePlayer 永久免伤掩盖命中错误。
- 本阶段完整回归 **96/96 通过**。无客户端实机验收；后续继续检查远程持续技能，不宣称 Emissary 全招式已等价。

### 阶段 26：Emissary 持续技能重建中（2026-09-28）

- 新增四项实际 Boss 攻击入口回归，首次均失败：陨石没有独立发射器、雷场第 34 帧前已经产生伤害弹、Divine Blast 无可反制的蓄力实体、Godrays 被替换为落弹。完整 100 项首轮另有既有 Serpent 命令轨迹测试失败，单独跟踪，不据此改动 Serpent 生产逻辑。
- 新增持久化攻击实体：Meteor Shower 第 10 帧建立发射器，之后每 15 tick 一枚、共 10 枚，第 160 tick 清理；Lightning Field 恢复 4 条对角射线、每条 8 个预警点，点计时 20 tick 后第 21 tick 落雷；Divine Blast 改为固定朝向持续射线；Godrays 改为四束旋转射线。
- 持续光束使用专用客户端段消息，避免每个采样点逐包广播。网络协议号升为 2，客户端与服务端需使用同一构建。伤害仍通过 Forge 普通伤害事件和无敌帧结算，不直接扣血。
- 已修正 Javelin、Seeking Missiles 的元素归属，以及近战/螺旋突进错误使用元素伤害的问题。当前验证仍在进行，下一阶段记录最终通过数和剩余边界。

### 阶段 27：Emissary 本轮回归收口与尚未完成项（2026-09-28）

- 持续技能验证完成：Divine Blast 第 24 tick 开始，连续 26 tick 射线判定；Godrays 起手第 5–9 帧上升，第 15 帧生成原点，原点计时第 15–30 tick 每 tick 三次旋转、四束发射。射线遇非穿透方块或有效目标终止，在终点按原伤害半径结算，保留普通受伤无敌帧。
- Forbiddance 第三 tick 可以消除蓄力光束和雷场点；不误删未标记为法术的陨石发射器。Godrays 原点不直接被删除，仅消除五格内对应的射线节点，已消除节点读档后不会复活。Reflectance 消耗容量并沿反射器朝向重新导向光束。
- 新增七项持续技能回归与上一阶段八项 Emissary 回归共同验证，本轮累计新增 **15 项**；完整 **103/103 GameTest 通过**。包含实体重建读档后的陨石发射时刻、反制和反射、孤立效果清理。
- 排查测试污染：旧运行世界中，失败测试的高空 Boss 残留在原地，会挡住后来测试的光束。新增独立 `emissary` 批次及带标签的测试对象清理；可用 `-PgameTestRunDir=run-emissary-gametest` 选择隔离目录，未删除、覆盖或搬迁原 `run/world`。
- 同步修正测试夹具而非其他 Boss 的生产行为：Serpent 延迟落点测试先加载目标跨越到的区块；参考命令轨迹的加载范围从 96 格扩大为能覆盖完整 80 步及前向标记的 224 格，并分批执行；Sorcerer 持续陨石测试单独分批，避免其他同时运行的战斗干扰。临时诊断日志已移除。

**第 14 项仍不勾选“全部修改/验收”；已知后续差距按战斗影响排序：**

1. **Pillars**：仍是短窗口内的瞬时区域命中，未重建原包最长 300 tick 的云/火双柱与云柱致盲、减速等独立生命周期。
2. **Flame Geysers**：仍是八处即时爆发；原包对最多两名玩家分别布置 14 处、20–149 tick 随机延迟的高火柱，需继续迁移预警、分布和高度判定。
3. **Wingbeat**：仍是瞬时范围伤害/击退；原包是 4×4 前进波面，逐步碰撞后给范围内非免位移玩家短时高等级漂浮，不能只当数值偏差处理。
4. **其他招式轨迹**：Sword Plunge、Wing Blast、Flame Slash、Apocalypse Rain 等还需逐招对齐轨迹、持续时间、命中区域和独立特效，不能凭名称和关键帧接近就宣称等价。
5. **本轮保留的近似与实机边界**：发射点暂用原生实体中心而非原导出 locator 的逐帧位置；Meteor Shower 使用低转向率原生追踪弹，尚非原命令两次子步的逐点数值复制；雷场落雷使用可视闪电，避免叠加原版闪电的额外伤害/点火；粒子、飞行模型俯仰、多人延迟与其他模组伤害交互仍需客户端对照。全套元素增幅/抗性体系不在本轮内。

### 阶段 28：本轮最终构建与复验（2026-09-28）

- 隔离目录中的完整 GameTest 连续两次 **103/103 通过**；`gradlew.bat runGameTestServer build --offline -PgameTestRunDir=run-emissary-gametest` 构建成功，再次单独执行 GameTest 亦成功。
- `python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.11.jar` 审计错误 **0**，发行包未包含运行时 mcfunction、GameTest 或客户端烟测类。
- 本轮产物仍为 `nyctocosm-native/build/libs/nyctocosm-native-0.3.0-native-alpha.11.jar`，大小 55,225,130 字节，SHA-256：`a8a9b3e01b1e9eb1cfd634449200c33b01a1e851e848f77a2b051bc8d9c09b70`。客户端/服务端请一并换成同一构建，不能与旧协议 1 的 alpha.11 混用。
- `git diff --check` 无空白错误；已移除所有本轮临时诊断输出。未改动原始数据包，未部署到用户存档或服务器，未代替用户完成客户端实机验收。后续从阶段 27 的未完成招式继续。

### 阶段 29：按用户要求编译测试版本（2026-09-28）

- 执行 `gradlew.bat build --offline`，构建及 Forge 重混淆成功；未修改战斗代码或版本号。
- 交付 `nyctocosm-native/build/libs/nyctocosm-native-0.3.0-native-alpha.11.jar`（55,225,130 字节），重新运行发行包审计，错误 0。SHA-256 与阶段 28 一致：`a8a9b3e01b1e9eb1cfd634449200c33b01a1e851e848f77a2b051bc8d9c09b70`。
- 本次仅构建与审计，未重新启动 GameTest 或客户端；游戏逻辑回归沿用上一轮连续两次 103/103 通过的结果。请用新 JAR 替换旧包，联机客户端与服务端须使用同一构建；未自动部署或修改用户存档。

### 阶段 30：按实测反馈重解传送与 Emissary 入场（2026-09-29）

- 原包依据：远古城市门 `nyctocosm:portal/__generated__/block/49` 在换维度后执行 `spreadplayers`，重新取得地表 Y；使者领域入口 `emissary:__generated__/block/22` 明确 `positioned over world_surface`，不是保留地下原高度。领域的 257 层平坦地形从 Y=-64 起，实际站立面为 Y=193。
- 新增四项真实逻辑入口回归，首次 107 项运行中四项均失败，原 103 项通过。首次进入未加载区块时，旧碰撞检查竟接受领域 `(240.5,-30,240.5)`：`noCollision` 跳过未加载方块，随后查询脚下方块才加载区块，导致地下位置被误判安全。
- `SafeTravel` 改为碰撞前加载完整实体占用区块；新增只选地表的入口搜索，忽略草与树冠，但检查液体、危险方块、世界边界、完整身体净空及脚下支撑。返程/召回继续保留原安全洞穴高度；找不到落点时不传送、不造平台、不清除方块。
- Incurrence 先验证玩家和 2×5 Boss 的两个落点，再修改维度/返程记录；使者在玩家附近独立选择地表，并面向玩家。开发召唤入口同样验证地表。
- 原包入场：100 格斜向距离的陨石以每 tick 1 格飞行 95 tick，随后 `spawn` 帧 0/2/4/6/8 补完落地；帧 62–98 每 tick 后退 1.5 格、上仰 35°，累计升高约 31.83 格。一、二阶段维持悬空根节点，不是地面追逐，也不是四阶段的巡航飞行。
- 恢复上述根位移、入场音效/远距离可见粒子、展开翅膀与阶段转换位移。修复渲染器整段 `spawn` 强制隐藏翅膀的问题；入场期间使用自动过期区块票，避免低模拟距离下远处陨石停住，不永久强加载存档。阶段二转三按原帧 60 的当前位置后移 20 格并重新找地表。
- 第一轮修正后 **107/107 GameTest 通过**。又补五项读档、旧存档、树冠落点、完整 Boss 净空和二转三落地回归，其中旧存档恢复暴露另一处未加载高度图问题，已补齐加载前置；后续继续验证。未改动原数据包。

### 阶段 31：悬空关联问题与回归补齐（2026-09-29）

- 旧原生版地下使者恢复测试已由失败转为通过；入场 37 tick 后重建实体可以继续剩余 58 tick，悬空锚点再次重建后仍保持高度。二转三过渡验证后撤、下降、从当前位置后移 20 格与领域 Y=193 的安全落地。
- 新增真实 AI tick 索敌回归，初次失败：80 格 Follow Range 会使后撤升空的 Boss 脱离玩家；改为原包 300 格后通过。保存了旧默认 80 格属性的实体读档时同步升级，不覆盖其他自定义值。
- 发现并复现维度 X/Z 缩放遗漏。使用原版 `CommandSourceStack.withLevel` 做对照，夜域 `(160.5,-30,160.5)` 进入领域应映射为 `(2568,-30,2568)`，而旧原生结果仍为 X/Z=160.5；现按原维度比例换算，再重新寻找地表 Y。不是只修改一个固定传送高度。
- Wingbeat 高空对地回归初次失败，已从 Boss 身边 10 格瞬时伤害改为第 23 帧的 4×4 平行波面，每 tick 前进三格、各节点最多 200 个子步（初值随机 0–49）。触碰玩家给予 20 tick、放大值 30 的漂浮，遵守免强制位移保护；源包此处没有直接 5 点伤害，该数值用于反射容量消耗。节点碰墙/超时消散，可被反射、Forbiddance 反制，读档保留子步计数。
- 13 项本轮新回归加原 103 项，完整 **116/116 GameTest 通过**。测试夹具修正为真实领域或高空空域，临时搭建的洞穴/树冠在 `finally` 恢复，避免把生成地形或前次测试留下的高空方块误当成代码失败。
- 新增 `nyctocosm-native/docs/EMISSARY_ARRIVAL_BEHAVIOR.md`，记录原包命令路径、阶段时序、修正范围与手工验收步骤。版本升为 **0.3.0-native-alpha.12**，与前次用户测试的 alpha.11 区分。
- 阶段 27 第 3 项 Wingbeat 的运动/效果问题已处理，但其粒子采样仍近似；Pillars、Flame Geysers、Sword Plunge 等其他招式、完整翅膀受击部位和多人/客户端视觉仍未全量等价。第 14 项依然不标记“全部完成”。

### 阶段 32：alpha.12 最终复验与交付（2026-09-29）

- 最终再执行 `gradlew.bat runGameTestServer build --offline -PgameTestRunDir=run-emissary-gametest`：**116/116 required GameTest 通过，BUILD SUCCESSFUL**；与上一轮相同用例集连续两次通过。最后一轮包含客户端不自行覆盖服务端无重力同步标记的收口修正。
- `tools/audit_native.py` 对新 JAR 审计错误 **0**：1778 模型、10 rig、5607 动画帧、247 声音事件；没有运行时 mcfunction、GameTest 或客户端烟测类混入发布包。`git diff --check` 无空白错误。
- 产物：`nyctocosm-native/build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`，**55,230,528 字节**。SHA-256：`5131d6e5d125ffd0c0d51b9d99618fe2aa8a82141eddd98c1204e0423071340c`。
- 不自动替换用户 `mods`、服务器或存档；客户端与服务端均需换为 alpha.12，并移走旧版本以免同时加载。原 alpha.11 产物保留，未删除。未修改 `_nyctocosm_source`。
- 本轮已完成代码、原命令语义对照、服务端回归与打包；**未运行本轮客户端视觉/完整生存验收**。请按 `docs/EMISSARY_ARRIVAL_BEHAVIOR.md` 的步骤重点测试地下门进入、领域地表落点、远方飞入、展开/后撤升空、一阶段远程攻击和中途读档。其余未对齐招式仍按阶段 31 保留。

### 阶段 33：按代码效果重新审计，独立列出剩余差异与方案（2026-09-29）

- 按用户最新要求，本轮**只修改文档，不实施游戏代码修复，不编译、不启动游戏**。以 `_nyctocosm_source` 原函数调用链、生成分支、动画命令关键帧和 NBT 标记，对照当前 alpha.12 工作区的实体 tick、伤害、传送、保存与客户端事件；未把历史文档的旧“未实现”描述直接复用。
- 根目录新增 [GAMEPLAY_DIFFERENCE_LIST.md](GAMEPLAY_DIFFERENCE_LIST.md)：**25 项有双侧代码依据的差异**，按 S4 整段流程/整场交互 → S3 整招/机制 → S2 局部规则 → S1 音画事件的差异大小排序。另列 **4 项验证缺口、4 项待审查设计边界**，不把未实测范围或有意原生化自动当作确定缺陷。
- 根目录新增 [GAMEPLAY_REFACTOR_PLAN.md](GAMEPLAY_REFACTOR_PLAN.md)：与 D01–D25 一一对应，每项写明原包和原生证据、实现设计、依赖与可观察验收。实施批次与差异排序分开，公共 locator/碰撞能力可先做，不因容易修就把小问题排到大流程前面。
- 排序前列包括：使者分阶段受击部位、法阵上下文和多人入域、首次胜利约 1300 tick 的结局、crypt 到地表 Slatehenge 的关联生成。新核实的恐惧逻辑会在夜域外清除玩家 DARKNESS；另外列出 Sword Plunge、Wing Blast、Pillars、Flame Geysers、Apocalypse Rain、Celestial Smite、Flame Slash 等实际攻击形态差异。
- 补充法阵持续时间、Hypertrace 起手/命中触发及 Transposition 跟随、反射方向、passthrough 语义、Sorcerer 普通攻击读档、Tony 跨维度冷却、Dais 延迟灌注、音乐队列与死亡事件等。完整元素属性管线标为**条件性差异**：默认 power/speed 多为零，不据此声称所有新档基础数值都错。
- 排除误报：alpha.12 地表落点/远方入场/悬空/索敌修复、已有的 Wingbeat/持续 hazard、Tony、普通怪、奖励/出口、书籍、碎石 loot modifier 和四项 Dais 成本，不再笼统写成未实现。静态世界生成 JSON 相同与结构 marker 动态逻辑缺失分开记录。
- 两份新文档中的代码证据链接均经本地存在性核对，编号与顺序一致。原生仓库 `git diff --check` 通过；本轮无新增行为测试结果，上一阶段的 116/116 不可用于证明这些新列差异已修复。保留原 TODO 历史、原数据包与所有已有代码改动；25 项仍待用户审查后实施和验收。

### 阶段 34：补完非使者模块的核心代码链复查与修复设计（2026-09-30）

- 延续用户“把尚未完整复查部分看完差距并设计方案”的要求，本轮**只更新文档，不修改游戏代码、不编译、不启动游戏**；保留原数据包及所有既有未提交代码。按最新范围明确排除旧数据包存档迁移，阶段 33 中 C01 的迁移建议不再属于待办；原生实体保存/恢复和新物品铁砧改名仍在范围内。
- 扩大复查：Serpent 三种攻击、游走/盘旋/身体链/受击/离场；Sorcerer 十二种攻击、六阶段/Nullpoint；Death Walker 与 Nightmare Apparition 的生成/AI/攻击/退出；四把武器主副手、弹道、命中体积与冷却；全部 13 种注册神器及关键组合。按入口→生成分支/动画命令→tick→命中→结束核对，不将残留不可达函数或重复动画导出文件当成新的玩法。
- [GAMEPLAY_DIFFERENCE_LIST.md](GAMEPLAY_DIFFERENCE_LIST.md) 从 25 项扩展到 **51 项**：新增 D26–D51 共 26 项，D17 增补 Death Walker 攻击读档。重新按差异大小排序，4 项 S4、25 项 S3、20 项 S2、2 项 S1；稳定编号不重排，顺序栏反映新优先级。所有项目仍待实施/验收，没有因完成审计而勾选“修复”。
- [GAMEPLAY_REFACTOR_PLAN.md](GAMEPLAY_REFACTOR_PLAN.md) 为 51 项一一提供原包/原生证据、具体方案及可观察验收。新设计涵盖 multipart 命中解析、明确的目标/距离/方块策略、独立攻击生命周期、命中状态效果、主副手能力计时、原生恢复和神器伤害组合；公共能力只作为建议，不在本轮顺手重构。
- 主要新发现：原包 Void Meteor 六次各一枚，原生六波各二十枚；Frozen Blades/Singularity/Hail/两种 Beam/Dash 的实际移动、命中或终止不同；术士多招在起手直接对锁定目标给负面效果。蛇已有身体部件，但原生通用法术只收 LivingEntity，无法以相同方式打中 PartEntity 躯干。
- 武器/探索问题：Scythe 把普通方块也当阻挡并新增冷却；Wyrmsliver 波面与双方状态不等价；Nullfissure 主手 generic 被改成 Void、副手反射缺失；Semblance 副手仅有火焰外观却未设置燃烧效果。Death Walker 把前方粒子位置当伤害圆心；Apparition 出生高度、警戒模型及多人接触不同。
- 神器问题：hasArtifact 使用带 NBT 全匹配；只读核查本地同版本 Inventory.contains 字节码确认调用 isSameItemSameTags，原生神器改名可失去被动识别。Absorption 与 Protection Void 以先增后乘减伤替代源同基数加减，默认属性也能产生差异；Expulsion 脚部距离改成包围盒距离。默认治疗/缓降/共享保护计时/产魂/延迟连锁等已有规则未被误报为整体缺失。
- 新增 [EXTENDED_GAMEPLAY_AUDIT.md](EXTENDED_GAMEPLAY_AUDIT.md) 的覆盖矩阵：逐列 Serpent 三招、Sorcerer 十二招和六阶段、普通怪、四武器主副手、13 神器及七组重点组合；明确“有对应实现”“确认差异”“尚需实测”。旧测试不能证明新差异已经修复，Meteor 原有 20 枚断言本身需要重新校准。
- 修复边界：保留完整身体安全落点、服务器权威、Forge 事件/友伤/保护；Scythe 穿越和最终安全落点分开，放火需单独审查。不为还原全局 selector 副作用而引入多场串场、多人加速计时或永久强加载；不自动移除/转换旧世界内容。
- 文档核验：三份主审计文档 **297 个本地链接全部存在**；清单/方案各 **51 个唯一 D 编号**且集合一致；原生仓库 `git diff --check` 通过。本轮没有新增行为测试通过记录，未把历史 116/116 用作这 26 项的新验收；客户端逐帧、低 TPS/多人/第三方组合继续留在 V01–V04。

### 阶段 35：补齐 AI 修复提示词与实施交接约束（2026-09-30）

- 按用户要求编写可直接交给后续 AI 的 [AI_GAMEPLAY_REPAIR_PROMPT.md](AI_GAMEPLAY_REPAIR_PROMPT.md)。提示词明确下一轮是实际实现/验证任务，而不是再次只做审计；本轮自身**只改文档，未改游戏代码、未编译、未运行行为测试**。
- 采用 handoff 交接结构组织信息：引用现有差异/方案而不复制 51 项全文；写清当前 dirty 工作区、只读原包、开发目录、证据优先级、下一步和中断续接要求。另按技能要求保存临时续接索引；项目内上述提示词和实施补充是正式可交付文件，不依赖临时索引。
- 新增 [GAMEPLAY_IMPLEMENTATION_GUIDE.md](GAMEPLAY_IMPLEMENTATION_GUIDE.md)，补齐原方案尚欠明确的：排序与最小依赖关系、逐项可执行参数/事件表、选择器/计时/伤害语义、去重/保存/同步边界、测试夹具、隔离运行目录、JAR 测试类排除、动态版本产物审计、阶段日志模板与交付门槛。
- 集中列出需要确认的玩法选择及推荐：生存转冒险/模式恢复、组队传送失败补偿、掉线剧情与奖励、Dais 交付/退款、世界放火、Scythe 失败成本/冷却、伤害取消与附带状态、新接口/替代素材。**推荐不等于用户已批准**；另区分由源码即可回答、应由实施者继续查明的技术问题。
- 解决交接歧义：方案“本轮不实施”仅指此前审计轮次，不是未来永久禁止修复；但一般修复授权也不代表待决策选项全部通过。D 编号保持稳定，优先级以排序列为准，不能做完全部旧编号后才处理新增高优先级项。
- [GAMEPLAY_DIFFERENCE_LIST.md](GAMEPLAY_DIFFERENCE_LIST.md) 新增每项“实施状态”列，51 项均保持“待实施”；实施补充区分实施中、代码完成待验证、自动验证通过待实机、已验收、待决策/阻塞及证伪/批准保留差异。分组复选框不能替代逐项验收，不得把“已写方案”或“已编译”标成已修复。
- 补充构建验证入口依据当前 build.gradle 和 audit_native.py：使用原生目录 wrapper/Java 17、独立 gameTestRunDir、当前版本准确 JAR，避免照抄旧 README 的 alpha.11 审计路径；审计脚本会写 native-audit.json，后续执行前需保存原报告。新增开发测试类必须符合发布包排除与审计规则。
- 核验：五份交接/审计文档的 **318 个本地链接全部存在**；排序清单/方案均 51 个 D 编号且集合一致，51 项待实施；构建示例仅做 PowerShell AST 语法检查，**0 语法错误，不等于运行成功**。原生仓库 git diff --check 通过。本轮未运行 Gradle 或资产审计脚本，没有新的游戏测试结果。



### 阶段 36：D01 使者分阶段受击部位（2026-09-30）

- 按最高优先级先完成 D01。导出 44 个动画共 2601 帧的 `center`/左右翼 locator，聚合 2640 个源函数；重新导出后资源哈希不变。`NativeBoss` 建立四个 multipart 受击部位并按动画与阶段更新：阶段一四翼 Size:4 slime 对应 1.02×1.02，阶段二切 Giant，阶段三/四切 Wither Skeleton。`SpellBolt`、范围与线性攻击均能解析部位并转入父 Boss 正常伤害结算。
- 新增五项 `emissary_damage_parts` GameTest，覆盖源 locator/偏移、部位尺寸与阶段切换、伤害转发、面积法术、SpellBolt 和原版箭。完整隔离运行 **121/121 required GameTest 通过，BUILD SUCCESSFUL**；Serpent 源传送对照 17,220 点最大误差 0。修正其测试夹具因手动跨区块移 marker 后实体 section 查询失效的问题，测试说明已同步更新。
- 扩展 `audit_native.py` 以分别校验骨骼 rig 和 locator bundle，并检查 D01 locator 文件存在于 JAR 且与资源源文件字节一致。当前 alpha.12 包审计 **errors=0**：10 个骨骼 rig / 5607 帧、1 个 locator bundle / 2601 帧、1778 模型、247 音效事件；JAR 3008 项，55,368,331 字节，SHA-256 `919ab45e3d648f4b134c2eb74bee1eba6767abcabd884f85fdef93035d24a77e`。
- 验证详情见 [D01 受击部位记录](nyctocosm-native/docs/validation/D01_emissary_hitboxes.md)。清单将 D01 标为**自动验证通过待实机**；尚未做客户端视觉/真实生存验收，D27 仍独立待办。按实施补充继续处理优先级下一项 D02；其模式恢复、组队失败补偿等玩法选择已向用户征求确认，收到答复前不实现相关选择。
### 阶段 37：D02 Incurrence 法阵位置上下文（2026-09-30）

- 依据 `spells/functions/incurrence/cast.mcfunction` 与 `transmutate.mcfunction`，原始 biome predicates 在法阵/法术原点执行；改为将 `SeelicField` 位置传入 `NativeEvents.incur`，按法阵位置匹配三种 Nyctocosm biome，使者的安全地表落点请求也以法阵位置为基准。未改变玩家模式或队伍失败规则。
- 新增 Incurrence 三条 biome 路由 GameTest；隔离全套 **122/122 required GameTest 通过，BUILD SUCCESSFUL**。alpha.12 当前产物审计 **errors=0**，SHA-256 `0657da272935d4bf7e4e53994bc860acbcadf32861a2457829eed19f8c35202f`，大小 55,368,487 字节。详情见 [D02 法阵上下文记录](nyctocosm-native/docs/validation/D02_incurrence_context.md)。
- D02 仍为**实施中**：2 格多人参与、逐玩家返程，以及冒险模式与部分队伍失败补偿待用户选择；确认前不实现这些关联规则。下一项优先级按序继续。

### 阶段 38：D04 crypt → 地表 Slatehenge 关联生成（2026-09-30）

- 按差异清单优先级完成 D04。源函数重新核实：crypt 末端 marker 执行 `positioned over ocean_floor`，若 200 格内没有 Slatehenge marker 就放置结构，随后无条件移除触发 marker；Slatehenge marker 在所在方块非空气时每 tick 产生 2 个 firework 粒子，空气时移除但本 tick 粒子命令仍执行。静态 Slatehenge worldgen JSON 原样保留。
- 白名单 NBT 转换只将 crypt hallway 末端和 `nyctocosm:slatehenge` 两个精确 tag marker 改为 `crypt_slatehenge_trigger` / `slatehenge_effect`，仅保留实体 ID。新建定向导入脚本避免全量重新导入；`StructureEventSavedData` 保存已处理位置。触发实体按海底高度图检查半径 200 格内已加载原生特效实体，使用源模板池的随机旋转与同名单方块 0.75 BlockRotProcessor 放置唯一 Slatehenge 模板；效果实体按源参数持续发粒子并在标记块为空气后结束。没有命令执行器或运行时 mcfunction。
- 新增 GameTest 覆盖两模板实体 ID、200 格边界、附近地标防重、SavedData 往返、空气/非空气特效生命周期，以及无附近地标时动态放置模板并生成原生效果实体。
- 全套隔离验证命令 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d04-final2-20260930"`：**128/128 required GameTest 通过，BUILD SUCCESSFUL**。期间修正既有远程 beam 测试夹具：临时加载远距离目标区块并铺设/还原安全地面，保留全部原命中断言。验证目录是新建的 GameTest 世界，不触及真实存档。
- `python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`：**errors=0**，2 个原生结构事件、无运行时 mcfunction、3011 个 JAR 条目。alpha.12 当前产物 55,375,621 bytes，SHA-256 `9fc3d7671f00ec51b1162b23987960d46af43050198d8af3761650877d5a34c`。D04 记录见 [D04 crypt/Slatehenge 验证](nyctocosm-native/docs/validation/D04_crypt_slatehenge.md)，覆盖 JAR 的更新审计报告见 `nyctocosm-native/docs/native-audit.json`；执行前保存的旧审计报告为 `nyctocosm-native/docs/validation/native-audit-before-d04.json`。
- 差异清单 D04 状态更新为**自动验证通过待实机**。尚未通过客户端/生存流程确认新 crypt 的触发体验、地标视觉朝向与残损分布；旧世界不迁移、不追溯注入 marker。D02 的冒险模式、队伍和失败补偿选择仍等待用户确认；D03 及其后排序项继续推进。

### 阶段 39：D27 Serpent 躯干命中与 D01 Size:4 尺寸更正（2026-09-30）

- 继续按差异清单排序。D03 的首次胜利对白离线/重复奖励选择仍待用户确认，故未实现该依赖策略；先推进无关的 D27。读取 `serpent/summon.mcfunction` 与 `livingthings`/`mobs` tags：原包 20 个身体偶数段 `Size:8` Slime 为可命中实体。原生 `SerpentPart` 现通过同版本 Slime 维度公式取得 Size:8 的真实尺寸，GameTest 直接与原版 Slime 对照，20 个部件宽高差最大 **0**（4.1615996×4.1615996）。
- `Combat.area` / `Combat.line` 现将命中范围扩展到 Serpent 与 Emissary multipart 部件，并以父实体去重；`SpellBolt` 命中部件时转发父 Boss，超出 AoE 的直接命中仍计一次，FIRE 附加效果也纳入直接目标。新增 Size8 对照、远端部件面积/线段单次伤害、原版箭与 SpellBolt 直接命中测试。
- 验证过程中先发现旧的 3×3 Serpent 部件与原版 Size:8 尺寸不符，修正后面积/线段测试通过。直接箭初始失败是测试夹具把箭放在实体方块内：确认 Forge 查询已返回身体部件后，将夹具移到空气区块并断言发射位置为空气；箭和 SpellBolt 均能命中并扣除父 Boss 生命。修复前的失败被作为夹具校正记录，没有误报为生产碰撞缺陷。
- 同版原版 Slime Size:4 夹具复核发现，阶段 36 报告中的 Emissary 1.02×1.02 漏乘基础尺寸；现代理尺寸改为 **2.0808×2.0808**，同阶段测试按逐轴比较核验。保留阶段 36 历史记录，本阶段附上更正与新的完整复验，不覆盖旧结论。
- 隔离完整命令 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d27-verified6-20260930"`：**131/131 required GameTest 通过，BUILD SUCCESSFUL**，包含 D01、D02、D04 回归；Serpent 源运动对照 17,220 个位置最大误差 **0**。未触碰真实世界存档。
- alpha.12 JAR 审计 `errors=0`：3011 项，1778 模型、10 rig / 5607 动画帧、1 locator bundle / 2601 帧、247 音效事件、2 个原生结构事件；无运行时 mcfunction、GameTest 或客户端烟测类。产物 55,375,927 bytes，SHA-256 `c03ca13facd558963e4fae06aa3f6e65bdd9f305c1420e5e96884d2b0fea10ad`；更新报告 [D27 验证记录](nyctocosm-native/docs/validation/D27_serpent_multipart_damage.md)、[D01 更正记录](nyctocosm-native/docs/validation/D01_emissary_hitboxes.md) 与 `nyctocosm-native/docs/native-audit.json`。
- 清单将 D27 改为**自动验证通过待实机**，并保留 D01、D04 同状态。尚未做客户端视觉/真实生存、多人与区块边界验收；近战、Wyrmsliver 和 Nullfissure 的实战手感仍需实机。D02 的模式/队伍/失败补偿、D03 离线对白与奖励幂等，以及 D08/D44/C03 放火、D20 祭坛交付、D41 Scythe 失败成本、D28/D42/D43 取消伤害后的附加效果、D22/C04 统计 API/音效选择仍按此前向用户提出的问题等待答复；不当作已批准。

### 阶段 40：D38 Serpent / Sorcerer 索敌与离场（2026-09-30）

- 按差异清单优先级核对 D38 的原函数链。Serpent 的 200 格 `FOLLOW_RANGE` 与 `SerpentMotion` 内 130 格选招门槛原已存在；缺口是 vanilla 目标筛选默认要求视线及 500 格无人清理。Sorcerer 源攻击选择最多 50 格，离场半径为 100；原生则默认追踪 80 格、要求视线且缺少无人清理。原包 Serpent 每次开招随机选择 200 格内玩家，术士的招式查询不要求视线。
- 新增 `SourcePlayerTargetGoal`，通过 `TargetingConditions.ignoreLineOfSight()` 修复选人阶段；`mustSee=false` 本身只控制持续保留目标，并不能关闭选人检查。Sorcerer FOLLOW_RANGE 改为 50，Serpent 的 200/130 行为保持不变。
- Serpent/Sorcerer 服务端 tick 在正常 AI 前按源的 500/100 格检查同维度全部玩家；范围外无人时只移除本遭遇拥有的 beam、SpellBolt、SpellArea、IceSpikeWave、Nullpoint、法阵状态和 bossbar。新增两场遭遇并存测试确认移除一只 Boss 不会清掉另一只 Boss 或其 beam/冰波。客户端音乐随现有 BossMusicController 检测实体移除停止。
- 新增并稳定化目标/离场 GameTests：隔墙 120/40 格索敌、Serpent 130/130.01 格攻击门槛、Sorcerer 50 格上限及 55 格拒绝、500/100 格精确离场边界、所属实体回收和跨遭遇隔离。测试采用不同顺序批次，避免并行模拟玩家互相影响。最终完整隔离命令 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d38-verified7-20260930"`：**137/137 required GameTest 通过，BUILD SUCCESSFUL**。
- 最终 alpha.12 JAR 审计 errors=0：3012 项，55,377,660 bytes，SHA-256 `dfa69a172ab2d67f8d3181b629844281c3cf3ac114a5b45ea13ece226f258650`；无运行时 mcfunction 与 GameTest 类。详情见 [D38 索敌/离场验证](nyctocosm-native/docs/validation/D38_target_range_and_departure.md)。清单状态改为**自动验证通过待实机**，方案同步纠正原范围记录。客户端隔墙/地下战斗、低模拟距离和多人实际环境尚未验收；没有触及原版来源备份或真实存档。
- 下一排序项 D28 的“伤害事件被 Forge 取消时是否仍施加附带效果”仍等待用户确认；未把选择当作已批准，继续推进不依赖该策略的工作。

### 阶段 41：D28 起手负面效果与 D26 Void Meteor 第一修复片（2026-09-30）

- 按排序继续核对 D28 源 `hit.mcfunction` 与原生调用链：减速/黑暗命令由命中函数在调用伤害后发给实际目标；Void Wave darkness 为 80 tick、amp 1，Void Meteor 为 100 tick、amp 1；Shard Hail 地面节点在 1.4 格内另发 200 tick、amp 1 slowness。移除原生 `beginAttack`、Hail 设置、Frozen Blades 发射和 Meteor tear 创建时的锁定目标强制效果，添加十种术士招式开场无状态回归。D28 尚未完成：真实命中附带效果和 Hail 地面节点效果仍缺，Forge 取消伤害是否同时取消状态的选择此前已向用户提出，收到确认前不假定策略。状态更新见 [D28/D26 阶段验证](nyctocosm-native/docs/validation/D28_D26_sorcerer_debuffs_and_meteor.md)。
- 对照实施补充纠正 D26 的世界 tick 换算：block109 和激活后的 block112 每 tick 都给计分器加 1，所以计分器 20/40/60/80/100/120 点实际分别在激活后第 10/20/30/40/50/60 个世界 tick 到达，而不是每 20 个世界 tick。原生计时与 GameTest 已按此修正；边界断言核实第 9 tick 尚未发射、第 10 tick 发射首枚、第 20 tick 第二枚、第 59 tick 共五枚、第 60 tick 六枚。
- 测试夹具覆盖目标改变时追踪方向更新、正确的 10 tick 发射间隔、passthrough 水方块和实块、范围伤害、25 tick 超时、飞弹所有者/效果/寿命 NBT 往返；改写旧的“六波各二十枚”断言。最终全套隔离运行 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d28-d26-final-20260930"`：**139/139 required GameTest 通过，BUILD SUCCESSFUL**，包含第 9/10、20、59/60 tick 发射边界及 D01、D02、D04、D27、D38 回归。前序两次全套运行出现过未复现的 Serpent 目标追踪用例失败，保留各次隔离日志。
- 最终 D26 alpha.12 JAR 审计 errors=0：3012 项，55,379,331 bytes，SHA-256 `4628c1556ee4a4ff9b2503e9af8032ab36b58d5c8da70afbf5a09ea5a36cc7a4`。更新报告见 [D28/D26 阶段验证](nyctocosm-native/docs/validation/D28_D26_sorcerer_debuffs_and_meteor.md)，JAR 报告快照为 `nyctocosm-native/docs/validation/native-audit-before-d28-d26-final.json`。D28 命中附带状态仍因 Forge 取消行为的待确认选择而未实现，D26 还需补 D28 darkness 和 D16 反制互动；两项保持实施中。没有改原始数据包或真实存档。

### 阶段 42：D05 恐惧处理范围（2026-09-30）

- 依差异清单顺序完成 D05。回读 `nyctocosm:playertick`、block2 和 `in_nyctocosm` 谓词：fear 仅运行于 `nyctocosm:nyctocosm`、`nyctocosm:dusk_plane`、`nyctocosm:emissary_domain` 三个维度，且只对 survival、非 luminous 玩家调用。原生事件此前每个服务端玩家都跑恐惧处理，维度外被当作“亮处”并全局清除 Darkness/恐惧状态。
- 新增 `NativeEvents.isFearDimension` / `shouldTickFear` 门控；恐惧处理只针对源范围。其他维度的 Darkness 与恐惧计时留存；活动中的 Luminescence 仍独立清除 Darkness，不依赖恐惧系统。新增 GameTest 核实主世界 Darkness/恐惧状态不被 END tick 改动，三个原包维度均接受 survival 并排除 adventure。
- 全套隔离验证 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d05-verified2-20260930"`：**140/140 required GameTests 通过，BUILD SUCCESSFUL**，含上一阶段 D26 计时边界及已有 D01、D02、D04、D27、D38 回归。D05 JAR 审计 `errors=0`：3012 项、55,379,636 bytes，SHA-256 `252d5ad57b684f6d7b76ce8eeb15e3bd01475414e963a10e0e8e4f33f75884ce`；报告见 [D05 恐惧范围验证](nyctocosm-native/docs/validation/D05_fear_scope.md)，审计快照见 `nyctocosm-native/docs/validation/native-audit-before-d05.json`。
- D05 更新为**自动验证通过待实机**。仍未运行客户端/多人现场验收；D28 的取消伤害附带效果选择仍待用户确认，D28/D26 尚有依赖该选择的剩余工作。原包备份与真实存档未更改。下一排序项为 D13。

### 阶段 43：D13 固定/移动法阵生命周期（2026-09-30）

- 按清单顺序复核 D13 源调用链：固定分支无持续时间，block2 在 3 格内找不到任意玩家时移除；移动分支初始寿命为 `500 + m.dream.power * 100`，且只有 `s.casting` 不存在时递减。`resetstats` 将 Dream power base 与 modifier 设为 0；非零属性加成依赖仍在开放中的 D22 管线，没有凭空新增一套属性实现。
- 原生 `SeelicField` 将固定阵设为无独立寿命、移动阵默认 500 tick，并按任意玩家的球形 3 格范围清除固定阵。移动阵仅在空闲时递减；NBT 往返保留类型、当前施法、冷却与剩余寿命。移动类型沿用创建时标记。
- 新增固定阵其他玩家留守/全员离开测试，以及移动阵 500 tick 初值、施法期间冻结寿命和施法状态/寿命保存恢复测试。保存恢复后移除主人，验证施法倒数仍可推进。发现主人离线时原生会清施法并进入冷却而不执行依赖玩家的法术效果；源 entity tick 虽证明倒数由法阵继续推进，但具体离线完成效果还未闭环，因此该项标为 **实施中**。换持神器行为也未实机确认，Dream power 修正待 D22。
- 首轮新测试因未注册 Mock 玩家无法经服务器 UUID 查找而失败；修正测试夹具为注册到 level 的 FakePlayer 后，全套通过。随后一次运行只留下不完整 server log、没有测试汇总，未计成功；新隔离运行 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d13-verified4-20260930"`：**142/142 required GameTests 通过，BUILD SUCCESSFUL**。日志 `nyctocosm-native/run-d13-verified4-20260930/logs/latest.log`。
- alpha.12 JAR audit：**errors=0**、3,012 项、55,379,970 bytes，SHA-256 `736baecdc949336407efdedc799ce431d435aad99e345bc0507cc0b6904b067d`。审计前快照 `nyctocosm-native/docs/validation/native-audit-before-d13.json`，当前结果 `nyctocosm-native/docs/native-audit.json`；验证说明见 [D13 法阵生命周期](nyctocosm-native/docs/validation/D13_field_lifecycle.md)。无客户端/真实多人测试；没有修改原包或真实存档。
- 下一优先项为 D14 Hypertrace 生成与空间行为。D28 的 Forge 取消伤害时是否仍施加附带效果选择继续等待用户确认；不依赖该选择的项目照常推进。

### 阶段 44：D14 Hypertrace 起手/命中生成与法阵空间行为（2026-09-30）

- 按源分支扫描更正原方案的单点描述：**全部 17 个** `spells/functions/*/cast.mcfunction` 都在 spell cast 入口按 global tracechance 各掷一次骰，并在玩家 local look 前方 3 格触发 summon；不是只有 Abhorrence。Nightmare/Void 的 spell block58/81 在伤害调用后分别按元素 tracechance + global chance 再抽一次，攻击玩家作为 trace owner，受击实体保持生成位置上下文。源比较使用 0–99 整数 roll 的包含上界 `roll <= chance`；默认 global 分为 15、元素分为 0。已将修正证据写回 [GAMEPLAY_REFACTOR_PLAN.md](GAMEPLAY_REFACTOR_PLAN.md) D14 节。
- 新增原生 `HypertraceService`：17 种 spell 均在 `SeelicField` cast 开始处独立掷骰，删除施法结束时对所有法术统一抽样；`Combat.hit` 的玩家 Nightmare/Void 分支在 hurt 调用之后按源顺序掷骰，即便 hurt 返回 false 仍保留 block58/81 的命中后调用语义。trace 使用攻击者 UUID 与元素，cast trace 出现在玩家向前 3 格、hit trace 出现在受击位置上方 1 格。
- 对齐 trace 本身生命周期：400 tick、每 tick yaw +1 度、任一其他 trace 距离不超过 1 格时沿远离方向挪 0.2 格；不再追随 Transposition，也不因 owner 不在线而提前消失。Accumulation 查找现在用 source 的球形 6 格，并按默认每个本人附近 trace +10 chance；其 power multiplier 和 spell 元素 chance 的非默认属性仍属 D22。现有本人拾取/本人攻击摧毁行为保持，并补了陌生玩家不能拾取/摧毁的断言。
- 新增六项 Hypertrace GameTest：100 次 roll 中 score15 正好 16 次成功；覆盖 17 种 spell 的起手时机及中断、N/V owner/坐标与无敌目标行为、Accumulation 球形边界/归属、避让/旋转/离线/Transposition/400 tick 生命周期、owner-only pickup/destroy。两项首次新增测试缺陷已修：Radiance 夹具不再重复命中仍处无敌帧的同一目标；cast 起手位置不再被重复增加 1 格 Y。中间多个完整回归曾出现非 D14 的 Serpent target/beam 用例间歇失败，未放宽断言；最终完整隔离运行通过。
- 最终命令 `gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d14-verified10-20260930"`：**148/148 required GameTests 通过，BUILD SUCCESSFUL**。最初 ForgeGradle 的 `libraries.minecraft.net` 证书预检在启动阶段失败；引号包住它报出的属性后预检跳过，命令保持 `--offline`，未下载依赖。日志 `nyctocosm-native/run-d14-verified10-20260930/logs/latest.log`。
- alpha.12 JAR audit：**errors=0**、3,013 项、55,383,320 bytes，SHA-256 `aad6fd2de045dcded5e15e9d2314c8b3a578c2289758137033df4d6d1fb4698d`；包含新增生产 service，无 GameTest/客户端烟测类、无 runtime mcfunction。审计前快照 `nyctocosm-native/docs/validation/native-audit-before-d14.json`，当前 `nyctocosm-native/docs/native-audit.json`；D14 报告见 [Hypertrace 验证](nyctocosm-native/docs/validation/D14_hypertrace_lifecycle.md)。`git diff --check` 无 whitespace 错误，只有已有 LF/CRLF 提示。无客户端/真实多人实机验收。
- D14 保持**实施中**：D22 非默认全局/元素属性及其它逐招、武器 source trace emitters 尚未接入。D28 取消伤害时是否附加状态的玩法选择仍待用户确认，不影响独立条目继续推进。下一排序项为 D06 Sword Plunge 独立巨剑攻击载体。

### 阶段 45：D06 Sword Plunge 独立巨剑首轮实现（2026-09-30）

- 按优先级对照 `sword_plunge/activate`、`subtick`、`slowcast`、`explode`、generated block 334/344 与第 5/45 动画效果帧。源行为为随机选取 300 格内合格在线玩家，在该列 WORLD_SURFACE +220 生成剑；每 tick 最多三次下落子步，200 步每次 1 格、50 步每次 0.2 格，最终停于地表 +10；攻击实体 timer 105 触发六层 shockwave，110 在地表 15 格范围执行 generic 60 点伤害与爆炸，然后执行 21×21×7 空气放火。
- 新增 `EmissarySwordPlunge` 服务端实体和隐藏剑视觉物品/客户端 renderer。Boss conjure/drop/落剑时机修到动画第 5/45 帧；删除旧的 30–44 帧 Boss 冲刺。攻击实体独立执行并保存 owner、攻击年龄、子步数与 105/110 one-shot 状态；Boss 攻击切换或移除不取消巨剑。当前 shockwave 效果只是粒子近似，通用 generic 属性/trace 子管线不承诺等价。
- 新增三项 Sword Plunge GameTest，覆盖 Boss 锚定、目标地表 +220、逐步速度阶段、实体独立 timer 存档恢复、105/110 精确事件与地表半径伤害。红测 `run-d06-red-boss-immobile-20260930` 使旧 Boss 速度断言失败（148 个用例通过），证明旧行为不满足锚定；此前两个高度断言为夹具错误，查明 GameTest 玩家 Y=-55 而该列 WORLD_SURFACE=67，修正查询范围/爆炸受害者位置后生产代码未变。
- 首轮还发现 1.20.1 Heightmap 映射只接受 X/Z，调用改为 `getHeight(type,x,z)`。最终完整隔离命令：`gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d06-core-verified5-20260930"`，**151/151 required GameTest 通过，BUILD SUCCESSFUL**。日志在 `nyctocosm-native/run-d06-core-verified5-20260930/logs/latest.log`。
- JAR 审计 `errors=0`：3,016 项、55,391,733 bytes、SHA-256 `be7e8b78ffe354b15a6c514bd45a0595574b91e3beb8a33463856151d436cd83`；包含 Sword Plunge 实体/renderer 与模型，未包含运行时 mcfunction/GameTest 类。快照 `nyctocosm-native/docs/validation/native-audit-before-d06.json`，D06 记录见 [独立巨剑验证](nyctocosm-native/docs/validation/D06_sword_plunge_lifecycle.md)。
- D06 仍为**实施中**：D08/D44/C03 放火策略已向用户询问，本项等待明确答复；不在确认前落火。shockwave 完整粒子/效果实体、generic damage 的 D14 trace 与 D22 非默认属性、客户端模型及多人实机仍未验收。没有修改原数据包或真实存档；alpha.12 版本号不变，不交付中间 JAR。下一排序项 D07 Wing Blast 与这些决策无依赖，继续推进。

### 阶段 46：D07 Wing Blast 六路射线与动画 locator（2026-09-30）

- 按排序项 D07 对照 `wing_blast/eyes`、`activate`、`subtick`、`raycast_signal`、`raycast`、`impact` 与 generated block 349/350/348/353/381。第 12 帧从六个 `wingtip1` locator 建 marker，随机选择 300 格内非 spectator/debug 玩家并独立瞄准；第 35 帧开始 blasting。预警/主动 raycast 均检查 passthrough，最多 100 格；主动阶段每翼按 yaw ±0.3°、上翼 pitch -0.3°、下翼 +0.3° 推进，玩家格或阻挡结束射线并在终端 4 格处理 radiance 40。计时达到 43 执行末 tick 后移除，不能解释成 43 tick 连续伤害。frame 0 rise 和 frame 12 eyes 音效现均按源 MASTER、音量 50、300 格受众触发。
- `EmissaryDamagePose` 增加任意 animation/locator/tick 采样。`tools/export_emissary_damage_locators.py` 将 damage locator bundle 从 3 个扩到 9 个，原来的 D01 三个 locator 保持；增加六翼尖的逐帧变换，当前 44 个 animation、2,601 帧、2640 个源文件，bundle source SHA-256 `9856F853246135E80DF0C635F93A9A1B5C1D131EA11855ED652A4E4305C2AF82`。审计工具现在对九个 locator 的每帧完整性做断言。
- 新增 `EmissaryWingBlastRay` 和实体注册/空 renderer，并将 NativeBoss 第 12/35 帧事件接入。实体保存 boss UUID、翼编号、年龄和预警/主动状态；每 tick 用 source 角度变化、passthrough 终止、玩家接触、hurtTime、半径伤害、Reflectance 储量消耗/重定向，并通过网络 beam 包表现当前射线。D15/D16 的更广义 Reflectance/passthrough 共用语义仍待核验。
- 新增四项 Wing Blast GameTest：直接驱动 boss 动画第 11/12/34/35 帧并核六射线及 locator 坐标、保存续接和 blasting 状态；实体墙体阻挡与远近受害者；玩家接触终止和伤害；Reflectance 消耗/转向以及 age 42→43 的末 tick 清理。
- 调试期间首轮测试的三个失败原因分别是 yaw 0 在 Minecraft 坐标朝 +Z（测试改用 180° 对准 -Z）、实体恢复测试未先 discard 旧 UUID（改成删除旧实体后再 add restored entity）、D06 测试假设固定目标但源规则会在合格玩家中随机抽取（改为基于实际抽中玩家列验证，不改生产行为）。之后增强的 frame/saving/reflectance/43 边界回归通过。
- 最终完整命令 `gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d07-wingblast-final-20260930"`：**155/155 required GameTests 通过，BUILD SUCCESSFUL**；日志 `nyctocosm-native/run-d07-wingblast-final-20260930/logs/latest.log`。文档更新后 `git diff --check` 复核。
- alpha.12 JAR `nyctocosm-native/build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar` audit：**errors=0**，3017 entries、55,625,155 bytes，SHA-256 `58f9b493eea94384db4f0545fe680cec0232ea8e726ea9af7ba11dbbb24c371d`；包括 `EmissaryWingBlastRay.class` 和 9-locator bundle，没有 GameTest/ClientSmoke 类或 runtime mcfunction。快照：`nyctocosm-native/docs/validation/native-audit-after-d07.json`；报告：[D07 Wing Blast 验证](nyctocosm-native/docs/validation/D07_wing_blast_rays.md)。
- 状态：D07 **自动验证通过、待客户端/实机，仍实施中**。六条客户端 beam 是否按动画稳定显示、高空对地实战空间、多人听声范围尚未验收；D15/D16 公共反射/tag 语义未完全闭合。已确认的新玩法决策：无；D06/D08/D44 涉及的世界放火范围选择继续等用户答复，未在确认前落火。没有修改 `_nyctocosm_source` 或真实世界；alpha.12 号不变，不发布/交付中间 JAR。下一排序项 D08 Pillars：先追 cloud/flame 的节点召唤、移动与 300 tick 生命周期；任何放火分支先等用户确认，先实现和验证不产生世界火焰的 cloud 独立行为；当前无运行中进程。

### 阶段 47：D08 Pillars 云柱/火柱持续载体首片（2026-09-30）

- 对照 `phase1/pillars/activate`、`subtick`、`slowcast_cloud`、`slowcast_flame`、`impact_cloud` 和 `impact_flame`：第 33 帧创建 yaw +60° flame、-60° cloud；各自最多 300 tick，nearest eligible player 跟向，cloud/flame 分别移动 0.4/0.6 格，selector 按 marker 原点脚部方块坐标 x/z -2..+1、y -3..+17 并检查 hurtTime=0。云 impact 有 Blindness I + Slowness III、dissonance damage 与专属粒子/音效；火 impact 无上述 potion effects，另按原包往空气放火。
- 新增 `EmissaryPillar` 实体及 NativeBoss age10 throw 音效、age33 双实体生成。实体保存 owner/kind/age，推进粒子/ambient、nearest target 和 source cuboid 接触；Reflectance 能耗/转向接入。状态效果只在 cloud mode；flame 的世界 fire block 写入仍因 C03 未决定而留空，代码和测试都不把火焰规则当作已批准。
- 新增 3 项 `emissary_pillars` GameTest：frame32/33 成对生成，云移动/伤害/云状态/存档恢复/等待选择期间无 fire block/299→300 清理，Reflectance 耗能转向。初次 `getNearestPlayer(Entity,...)` 重载不适配 Forge/Minecraft mapping，改用坐标式谓词重载；最终 source 校对发现 flame 不应继承 cloud 负面效果，已收窄并做最终完整复验。
- 最终命令 `gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d08-pillar-final2-20260930"`：**158/158 required GameTests 通过，BUILD SUCCESSFUL**。日志 `nyctocosm-native/run-d08-pillar-final2-20260930/logs/latest.log`。
- alpha.12 JAR audit：**errors=0**，3019 entries、55,631,849 bytes，SHA-256 `02c4a496c83dc21029acd3b7f2828219070d654bacc15f7f742b27737c5c6fd9`；含 `EmissaryPillar.class`，无 GameTest/ClientSmoke 和 runtime mcfunction。快照 `nyctocosm-native/docs/validation/native-audit-after-d08-cloud.json`；报告 [D08 Pillars 阶段验证](nyctocosm-native/docs/validation/D08_pillars_cloud_flame.md)。
- `git diff --check` 待所有本轮文档追加后执行。状态：D08 **云/载体自动验证通过；火方块玩法选择待用户确认，整体实施中**。客户端粒子/声音、多人实战、Boss 换招后持续与完整 300 tick表现未测。未改 source pack/真实世界；alpha.12 不变，无中间 JAR交付。下一排序 D09 Flame Geysers：先核实最多双目标、14 nodes/target、地表 spread 与 20–149 tick 预警，追踪任何 fire placement 并只做不依赖未决 C03 的部分；当前无后台进程。

### 阶段 48：D09 Flame Geysers 双组布场与独立计时首片（2026-09-30）

- 按清单继续到 D09；复核 `activate`/generated block 405/`summon`/`subtick`/`shoot`/`impact` 和 animated_java 帧函数。校正 `spreadplayers 5 25` 语义：5 是同组节点之间最小距离，25 是玩家中心最大半径，不是 5–25 环带；最多两个目标，每目标 14 个，节点 20–149 tick 倒数。Frame 0 charge、frame 40 activation、timer20 erupt 声音、最后40 tick 警示均接入。
- 新增 `EmissaryFlameGeyser` 持久实体及注册/renderer。每个选中玩家各自建立 group UUID；均匀圆盘散布、WORLD_SURFACE 放置、同组最小 5 格水平间距；地表/区块采样上限 14×500，避免无界循环与强制加载。每点保存 owner/group/target/timer；纵向 50 格样点按 5 格范围处理 Dissonance 40 基础伤害。保留 Forge 伤害事件。源第二目标再次通过全局 selector 重散布旧组会串场，native 按 group 隔离。
- 右手 cue 取源动画 frame 10/20/25/30/33/36/38 的 flame/flash/lava粒子；damage locator exporter 增加 `righthand`，bundle 目前 10 locators、44 animations、2601 frames、2640 source files，SHA-256 `9856F853246135E80DF0C635F93A9A1B5C1D131EA11855ED652A4E4305C2AF82`。
- 首轮 0 节点测试因 query AABB 垂直范围低于 `WORLD_SURFACE` 而漏查，已扩大到 build height；另修正错误的测试假设，不把 spreadDistance=5 当作节点离中心最低半径。最终增加的倒计时断言确认 timer40 开始警示、归零喷发 tick 不重复发警示环。最终隔离命令 `gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d09-geysers-final10-20260930"`：**161/161 required GameTests 通过，BUILD SUCCESSFUL**；日志 `nyctocosm-native/run-d09-geysers-final10-20260930/logs/latest.log`。其间两项既有 Serpent 远距 beam 测试在若干完整回归中间歇失败；本次最终全套均通过。
- 当前 alpha.12 JAR audit **errors=0**：3020 entries、55,694,818 bytes，SHA-256 `798949bc18520d237d2524f89259b2f0aee6f9dc49ffb9f098b7013a4521a2e8`；无 `NativeGameTests`/`ClientSmoke` 或 runtime mcfunction。审计快照 `nyctocosm-native/docs/validation/native-audit-after-d09-prefire.json`，前态 `native-audit-before-d09.json`；详情 [D09 Flame Geysers 报告](nyctocosm-native/docs/validation/D09_flame_geysers.md)。
- D09 仍为**实施中**：`effects:shockwave/inculcate` emitter/完整三组环形粒子还没等价接入，当前 plume 是聚合近似；`fill fire` 等待 C03 用户选择，未落世界火；D22 非默认属性、客户端表现、多人/生存实机也待做。未改 `_nyctocosm_source` 或真实存档，alpha.12 未升版、未交付 JAR。下一优先项 D10 Apocalypse Rain；D06/D08/D09/D44 放火选择继续等待用户答复。

### 阶段 49：D10 Apocalypse Rain 曲线弹幕（2026-09-30）

- 对照动画 `leaf_10`–`leaf_44`，确认连续 **35 帧**调用 `fire`，每次四个初始角度分支，共 140 枚；蓄力声在 frame 0。block/689 的俯仰选择器顺序累积、每 tick 最多两次各一格 slowcast，timer 第 60 tick 在移动前终止。impact 是半径 5 玩家范围，Dissonance 基础值 12，源分支还包括 D22 的动态属性/统计计算。
- 新增 `EmissaryRainShard` 实体、注册与 renderer。NativeBoss 从原先玩家点上空随机落火弹改为 frame 10–44 四枚/帧从使者根部发射；保存上下组、owner、计时/子步/resolved；复现源角度扰动、非互斥俯仰增量、passthrough/玩家碰撞、爆炸半径、force trail 和 Reflectance 储量/转向。目标丢失时动画期间的雨仍继续；不强加载未加载区块。伤害走 Combat/Forge，护甲夹具基础 12 实扣 7，D22 非默认属性未接入。
- 新增四项 `emissary` GameTest：35 次 fire 的精确数量与初始四向/俯仰/根部原点、目标消失后续发射；上下组俯仰与两步坐标、NBT、第 60 tick 不移动；墙体与玩家触碰终止及 5 格命中；Reflectance 两次子步的储量耗尽/转向。首轮俯仰坐标因 float 误差约 0.00017，将断言容差订为 0.001；伤害断言修为检查穿过原版/Forge 防护后的 `0 < 实伤 <= 12`，没有改动生产边界。
- 最终隔离全套命令 `gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d10-rain-final7-20260930"`：**165/165 required GameTests 通过**；日志 `nyctocosm-native/run-d10-rain-final7-20260930/logs/latest.log`。稍早 final4/5/6 有同一既有 Serpent late-landing 测试间歇失败，但 D10 四项均通过，final7 全套通过。随后 `gradlew.bat build --offline --console=plain` 成功。
- 当前 alpha.12 JAR audit **errors=0**：3,021 entries、55,700,369 bytes，SHA-256 `90825774bd3540bba97257fca1e0eac3c48410763151a010d9bc35c9f592fc44`；无 GameTest/ClientSmoke 类与 runtime mcfunction。前/后态快照 `nyctocosm-native/docs/validation/native-audit-before-d10.json`、`native-audit-after-d10.json`；详见 [D10 验证报告](nyctocosm-native/docs/validation/D10_apocalypse_rain.md)。
- D10 保持**实施中**：自动行为闭环已覆盖，但 D22 weakness/resistance 与记账、D15/D16 广义反射/passthrough 语义、逐帧原包坐标重放、客户端/多人/生存实机未验收。该调用链没有世界放火，不依赖 C03；原决策仍未批准、没有放火。没有改 `_nyctocosm_source` 或真实存档；alpha.12 不升版、不交付 JAR。下一排序项 D11 Celestial Smite。

### 阶段 50：D11 Celestial Smite tear 与 raycast 首片（2026-09-30）

- 重新追踪 start、动画帧函数及 generated block 26/50/93，发现并修正原方案漏写的同步起手路径：start 会立即执行 tear→circles→activate，因此源攻击是起手一枚加动画第 26/50/93 帧延迟再一枚。raycast 检查 step 1–150（末端原点 +149）；墙体/距线 5 格玩家是终止条件而非沿线伤害，impact 伤害终点 15 格内玩家、基础 Radiance 80。终点还有 9×5×9 fill fire replace air，因 C03 玩法选择未确认，本轮不放火。
- 新增 EmissaryCelestialTear 持久实体与注册/renderer；NativeBoss age0 同步起手命中，age26 生成延迟 tear、age50 active、age93 raycast。接入源 passthrough、终止条件、150 次上限、终点范围和 Combat/Forge 基础伤害。完整模型/beamwave 仍为原生粒子近似，D22 动态伤害与 weakness/resistance/记账尚未闭合；未知区块安全结束、不强加载。
- 三项新增 GameTest 覆盖同步起手与动画边界/NBT、墙体和终点 15 格伤害、玩家 5 格提前终止与 150 检查边界。最终完整隔离命令 gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d11-celestial-final3-20260930"：**168/168 required GameTests 通过**，日志 nyctocosm-native/run-d11-celestial-final3-20260930/logs/latest.log；随后 gradlew.bat build --offline --console=plain 成功。
- alpha.12 JAR audit errors=[]：3,023 entries、55,709,573 bytes、SHA-256 607bf3ff8ea1a3bd002c736b85daf0f21aa60ad930a162cd631f2b86feaf176c。前/后态见 nyctocosm-native/docs/validation/native-audit-before-d11-20260930.json 和 native-audit-after-d11-20260930.json；详见 [D11 阶段报告](nyctocosm-native/docs/validation/D11_celestial_smite.md)。源文件 SHA-256 已归档在报告中。
- D11 实施中，待 C03 火方块选择、D22、精确源视觉与客户端/多人/生存实机；C03/D28 相关玩法选择仍待用户回答，本轮未据“继续”视为批准。未改 _nyctocosm_source 或真实存档，版本仍 alpha.12，无 JAR 交付。按清单下一项为 D12 Flame Slash；先审查是否存在无需待决玩法批准的独立实现片段。
### 阶段 51：D12 Flame Slash 单一波面（2026-09-30）

- 对照 leaf_2/slash1 与 leaf_13/slash2，确认源在第 2 帧以 yaw +60°、局部上移 1.5/前移 1.5 发第一枚；第 13 帧 yaw -60°、局部上移 1.5/前移 2 发第二枚。两者各播 MASTER 音量 5、听距 100 的对应声音。原生旧行为是只在第 13 帧发五枚速度 1.5、寿命70的直线 FIRE 弹。
- 源 subtick 每次 age++ 后追踪、Reflectance 与移动 0.8；半径2垂直截面的20个采样方块中触碰玩家即终止并标记目标，遇非 passthrough 方块仅终止；第40步仍移动/采样，下一 tick impact/清理。源基础 Dissonance 50；D22 增伤、weakness/resistance、统计记账另行处理。
- 新增 EmissaryFlameSlashWave 持久实体/注册/renderer，按age2/13调度两次波面，保存 owner/age/ending/待结算玩家；实现20点表面扫描、同 tick 玩家去重、逐 tick 最近目标追踪、0.8步长、Reflectance、墙体与40步终止、下一 tick Combat/Forge命中。接入近似原生 flame/smoke 粒子；没有世界 fire placement，所以不依赖 C03。Native yaw/pitch 目前直接朝最近玩家求解，源连续 facing/position 转向公式还需逐帧坐标对照；精确模型粒子、D22和客户端/多人/生存实机未闭合。
- 新增三项 GameTest：frame2/13及±60°；0.8步长、20点半径2、NBT与40步+延迟impact；采样命中、墙体只终止、无 Fire 方块。隔离完整命令 gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d12-flame-slash-final4-20260930" 曾得 **171/171 required GameTests**；final6最终夹具下的三项 D12 测试也全部通过，但完整套件报6项不相关的Wing Blast/Flame Geysers/Serpent失败，因此不把final6记为全套通过。随后单独 gradlew.bat build --offline --console=plain：BUILD SUCCESSFUL。
- alpha.12 JAR audit errors=[]：3,024 entries、55,716,049 bytes、SHA-256 be504a23d9f644df03249e815ab8435a42f62c08695981f55d5b3a7cf9849f9a；含 EmissaryFlameSlashWave，不含 NativeGameTests/ClientSmoke/runtime mcfunction。审计前/后态 nyctocosm-native/docs/validation/native-audit-before-d12-20260930.json、native-audit-after-d12-20260930.json；完整记录 [D12 验证报告](nyctocosm-native/docs/validation/D12_flame_slash.md)。git diff --check 通过。
- D12 仍为实施中：源转向精确对照、D22、源模型/效果及实机验收待补；未改 _nyctocosm_source 或真实存档，alpha.12 不升版、不交付 JAR。未将“继续”视为对 C03/D28/D42 待选玩法的批准。按差异清单下一项 D13 法阵类型/持续/跟随；开始前先分离无需待确认选择的独立核验与依赖行为。

### 阶段 52：D13 法阵锚点、取消与持有变化（2026-09-30）

- 回查原包 `spells/__generated__/block/2`、`block/4`、`block/177` 与 `spells/cancel_cast`：固定/移动 marker 都在半径 3 无玩家时清理；移动阵只在非 `s.casting` 时扣 `s.duration`。`execute at <matched player> run tp @s ~ ~ ~ ~ 0` 保持 marker 为 `@s`，所以移动 marker 跟随匹配玩家位置、复制 yaw、marker pitch 为 0；取消施法把最近移动阵 duration 设为 0，固定阵不因该路径删除。
- 修正 `SeelicField.tick/cancelCast`：移动类型按创建时配置跟随在线关联玩家、更新位置与 yaw、marker pitch 设 0，不再依赖当前 Transposition 库存；固定阵不因持有变化移动。固定和移动阵统一执行三格内无人清理；移动施法期间暂停寿命，取消后置 0 并由正常 tick 清理，固定取消仅结束施法。
- 新增三项 `field_lifecycle` GameTest，现共 5 项：固定阵创建后获得 Transposition 仍不跟随；移动阵创建后移除 Transposition 仍跟随并保持源 yaw/pitch；移动阵全员离开时即使施法中也移除；固定取消保留法阵，移动取消 next tick 过期。离线主人完成施法的用例保留另一名附近玩家，匹配源三格清理前提。
- `gradlew.bat compileJava --offline --console=plain`：BUILD SUCCESSFUL。最终代码的两次隔离全套各执行 **174 项 required GameTest**，5 项 `field_lifecycle` 均未失败；followup2 有 1 项 Serpent 高处 beam 测试失败，followup3 有 2 项 Serpent retarget 测试失败，均不在 D13 批次，详见 `run-d13-followup2-20260930/logs/latest.log` 与 `run-d13-followup3-20260930/logs/latest.log`。不把这两次运行记作全套通过。随后 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- alpha.12 JAR audit `errors=[]`：3,024 entries，55,716,022 bytes，SHA-256 `69fa132508f65f6074326c66fa47fe1aa72573851b8e6dbd8b634581819c2fee`；无 runtime mcfunction 或 GameTest/ClientSmoke 类。审计前态 `nyctocosm-native/docs/validation/native-audit-before-d13-followup-20260930.json`，审计后态 `nyctocosm-native/docs/validation/native-audit-after-d13-followup-20260930.json`。`git diff --check` 通过。
- D13 仍为实施中：Dream power bonus 需 D22 属性管线；主人离线但另有玩家留在附近时，各法术源 executor 的剩余上下文需进一步追踪，原生 owner-dependent effect 未据此猜测；无客户端/真实多人实机。无世界火或取消伤害附加状态改动；未改 `_nyctocosm_source`、真实存档或旧 MDK，alpha.12 未升版、JAR 未安装/分发。按差异顺序下项为 D14 Hypertrace：核对剩余逐招/武器生成分支及 D22 外独立闭环项。

### 阶段 53：D14 非玩家 Hypertrace 发射源审计（2026-09-30）

- 对照 D06–D12 现有原生攻击实现和 source 命令链，新定位若干额外 trace 发射点：Sword Plunge 以攻击实体执行；Wing Blast、Pillars、Flame Geysers、Apocalypse Rain、Celestial Smite 以 Emissary 模型执行；Flame Slash 以临时 impact 实体执行。它们不同于已实现的玩家施法起手和 Nightmare/Void 玩家来源命中分支。
- 追查初始化路径后，未找到这些 Boss/临时实体的 global/element tracechance 可靠默认写入；`resetstats` 不在观测到的运行调用链，玩家 `playertick` 初始化也只覆盖玩家。source 是 `roll <= chance` 的 0–99 包含式判定，猜测缺省 0 会带来 1% 触发。该行为选择向用户确认中；未引入默认概率或额外 hook。重验玩家来源分支仍以攻击者为 owner 并在受击实体坐标生成，与原包执行上下文一致。
- 全套隔离验证 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d14-source-audit-20260930"`：**174/174 required GameTests 通过，BUILD SUCCESSFUL**。这是未加入非玩家 hook 的现有代码基线验证。
- alpha.12 JAR 保持 3,024 项、SHA-256 `69fa132508f65f6074326c66fa47fe1aa72573851b8e6dbd8b634581819c2fee`；报告和证据见 [D14 Hypertrace 验证](nyctocosm-native/docs/validation/D14_hypertrace_lifecycle.md)。阶段不把待选择的发射概率记为完成；用户确认后再补实现和针对性验证。

### 阶段 54：D15 Reflectance 斜向出射方向首片（2026-09-30）

- 按原包 Emissary fireball slowcast 对照反射器 rotation，修正 `SpellBolt.reflectFrom`：将 reflector 朝向作为新方向，保留入射速度大小，更新 yaw/pitch，转移 owner、清 homing target 并把 HOMING 降为 DAMAGE。修改 `reflectance_consumes_source_twenty_damage_capacity`，用斜向来弹断言出射 X≈0 且 Z>0，排除旧镜面公式；原 20 damage 储量边界断言保留。
- 两次完整隔离 GameTest：`run-d15-bolt-reflect-20260930` 为 **172/174**，失败 `echo_shard_cast_through_open_frame_activates_portal`、`serpent_departure_cleanup_uses_source_500_block_radius`；`run-d15-bolt-reflect-final2-20260930` 为 **171/174**，失败 `wing_blast_player_contact_ends_ray_and_damages_on_cooldown`、`serpent_beam_selects_late_target_after_reload`、`serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`。两轮 Reflectance 用例均未列入失败，但不能据此声称全套通过；其失败集合不同且均非反射用例。另行 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- alpha.12 JAR audit `errors=[]`，3,024 项、55,716,141 bytes，SHA-256 `17d46fa68b401b10939ee502c48811c3ad945abd4acd661f3ded6d710feeb241`；审计前快照 `nyctocosm-native/docs/validation/native-audit-before-d15-reflect-20260930.json`，审计后见 `nyctocosm-native/docs/native-audit.json`；完整范围见 [D15 阶段验证](nyctocosm-native/docs/validation/D15_reflectance.md)。本阶段 `SpellBolt.java` / `NativeGameTests.java` 的 `git diff --check` 通过，仅有既存 LF/CRLF 提示；D15 仍实施中，范围体积/距离边界、角点、反射次数及 beam/hazard 差异还未统一验证。无客户端/多人实机；未更改原包与真实存档，版本仍 alpha.12，未安装/分发 JAR。
- 按差异表全局排序，下一个未完成高优先级项为第 34 位 D17（Sorcerer / Death Walker 攻击读档）；D16 位于第 43 位，留待之后处理。D15 保持实施中并在较高排序项完成后继续补边界与分支规则。

### 阶段 55：D17 Sorcerer / Death Walker 攻击读档首片（2026-09-30）

- 按排序最高未完成项 D17 复核源和原生读档：Sorcerer 的 `AttackAge`、`Remaining`、`AnimationAge` 原有保存但普通 attack 在 `readAdditionalSaveData` 被设为 idle；若干 Hail/Meteor 独立年龄另有保存。Death Walker 已保存普通状态和动画剩余时间，但读档也先重设 idle，导致 slam 中止。
- Sorcerer 现在额外保存活动 attack ID、目标 UUID；读档将全部 `SorcererTimeline` attack ID 还原为原来的剩余时间与帧，ICE_SPIKES 用专门 ID 标识；HaloBeam 已生成节点坐标一并保存。已单独保存的 Hail/Meteor 状态继续推进，恢复 timeline age 不重复启动之前的关键帧。Death Walker kind 3 只恢复有效 slam clip，保留 `remaining` 与 `AnimationAge`，状态恢复至 attacking，伤害依旧由原 countdown 边界触发。未知/无效状态回空闲。
- 新增 5 项 `native_boss_attack_restore` GameTest：全部 Sorcerer timeline ID/目标/剩余帧往返、HaloBeam 节点坐标、Hail 持续波、Void Meteor frame30 发射前续接并验证次帧不重放、Death Walker slam damage frame 前与已越过 damage frame 后保存恢复。
- 最终全套隔离运行 `gradlew.bat runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d17-attack-restore-verified-20260930"`：**179 项中 173 项通过**；5 项 D17 测试全部通过。其余 6 项为 `wing_blast_player_contact_ends_ray_and_damages_on_cooldown`、`serpent_beam_selects_late_target_after_reload`、`serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`、两项 Flame Geysers 测试和 `emissary_wing_parts_take_ranged_spell_damage`。之前完整回归亦分别出现 7 项、1 项失败，失败组合间歇变化。第一次 D17 Meteor 用例的 `-2` 年龄 sentinel 预期写错，修正夹具改测源 frame30 的 0→1 推进后通过；无生产代码测试失败。
- `gradlew.bat compileJava --offline --console=plain` 与 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。相关代码 `git diff --check` 无 whitespace 错误，仅有既存 CRLF 提示。
- alpha.12 JAR 审计 `errors=[]`：3,024 entries、55,716,915 bytes，SHA-256 `2cf4574920fe35ab870da8403cd65758a7d2acee796995b2a31ca68c91f7a92c`；GameTest/client smoke 类与 runtime mcfunction 未进包。审计快照 `nyctocosm-native/docs/validation/native-audit-before-d17-attack-restore-20260930.json`，审计后 `nyctocosm-native/docs/native-audit.json`；明细见 [D17 阶段报告](nyctocosm-native/docs/validation/D17_attack_restore.md)。
- D17 仍为**实施中**：自动 NBT 往返不等同真实 chunk unload/世界重启；无客户端动画连续性、跨区块和实战逐招验收。没有改原包、真实存档、旧 MDK 或安装/分发 JAR；alpha.12 不变。差异表 rank34 仍是最高未完成项，因此续接应留在 D17，补完实际 world save/reload 与 remaining attack-phase persistence 验收后再前进，D16 不因编号而插队。

### 阶段 56：D17 Death Walker 新实体 NBT 恢复复核（2026-09-30）

- 回查源动画明确只有生成的 `slam/tree/leaf_50.mcfunction` 会调用 `nyctocosm:mobs/death_walker/slam`；leaf_49 与 leaf_51 不触发伤害。源 age/frame50 对应原生倒数 `Remaining=41`，即从 42 递减到 41 的 tick 发伤害；保存于 41/age50 后再推进到 40 不可重打。已将该帧坐标及 leaf_49/50/51、伤害函数 SHA-256 写入 D17 报告。
- 检查旧 GameTest 后发现 Death Walker 两条用例之前对同一对象 `load`，改成保存旧对象、移除它，再由全新 `NativeBoss` 实例读取 NBT，并验证 frame50 命中与命中后不重放。Sorcerer 用例本已用新实例恢复，无需改动。
- 新隔离全套 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d17-fresh-entity-restore-20260930"`：**175/179 required GameTests 通过**，五项 D17 全部通过。余下 4 项为一个 Serpent Beam 目标延迟测试、两项 Flame Geysers 和一项使者翼部投射物伤害，不在 D17；组合命令在 GameTest 失败后跳过 build。随后独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 审计 `errors=[]`，3,024 entries、55,716,915 bytes，SHA-256 `2cf4574920fe35ab870da8403cd65758a7d2acee796995b2a31ca68c91f7a92c`。`git diff --check` 对本轮 Java 文件无 whitespace 错误（仅 LF/CRLF 提示）。
- D17 仍实施中：新实体 NBT 夹具仍不能替代真实 chunk unload/专服重启、客户端动画连续性与剩余每招阶段逐帧验收。待办仍按优先级停留 D17；D14 非玩家 Hypertrace 发射率、C03/D28/D42 玩法选择没有因本轮进展而获得批准。

### 阶段 57：D17 隔离专服重启保存记录验证（2026-09-30）

- 为避免碰现有 `run/world`，用 `run-d17-server-restart-smoke-20260930` 新建独立 Forge dedicated-server 运行目录，RCON 只绑定 `127.0.0.1`。在 Easy 难度生成 Death Walker，进入 `AnimationAge=49 / Remaining=42 / OrdinaryState=3`，执行 `save-all flush` 与正常 `stop`，再启动同一个新测试世界。
- 重启后实体 region `nyctocosm-native/run-d17-server-restart-smoke-20260930/d17-restart-world/entities/r.0.0.mca` 的 chunk NBT 保留 `id=nyctocosm:death_walker`、`Animation=slam`、`Remaining=41`、`AnimationAge=50`、`OrdinaryState=3`，与 leaf_50 的唯一伤害帧相符。初次错误难度为 Peaceful 会清除敌对实体；仅在隔离测试世界修为 Easy 后重跑，非生产缺陷。无玩家的 headless server 重启后 `@e` 查询仍无已激活实体，即使 chunk 0,0 设为 force-loaded；因此只记为**实体 NBT 跨进程保存成功**，运行时 player-ticking chunk 激活与客户端表现仍未验收。
- 验证说明已写入 [D17 报告](nyctocosm-native/docs/validation/D17_attack_restore.md) 和 D17 方案/差异行。服务器停止、RCON 已禁用，临时 Gradle/RCON 脚本已移除；`nyctocosm-native/run/world` 未载入或修改，先前 EULA 门禁探测生成的 `run/eula.txt` 已移除，原 `latest.log` / `debug.log` 已从启动时轮转出的压缩副本恢复。alpha.12 不变。
- 最高优先级仍为 D17：5 项 D17 GameTests 均通过；完整套件为 175/179，4 个非 D17 测试失败。用户待选的 D14 发射率与 C03/D28/D42 玩法选择仍未批准，不因这次存档验证而推进相关实现。

### 阶段 58：D17 Sorcerer 目标 UUID 续接断言（2026-09-30）

- 在 Void Meteor frame29→30 读档用例补断言：新建并载入 Sorcerer 后第一 tick，`getTarget()` 必须重新解析到保存 UUID 对应的在线玩家；同一 tick 仍须执行尚未触发的 Meteor 关键帧，并在下一 tick 不重放。5 项 D17 GameTests 均通过。
- 最新完整运行 `gradlew.bat runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d17-target-restore-verified-20260930"`：**175/179**。四项失败为 `serpent_beam_selects_late_target_after_reload`、两项 Flame Geysers 用例、`serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`；均非 D17。其后 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL；alpha.12 audit `errors=[]`、3,024 entries、55,716,915 bytes、SHA-256 `2cf4574920fe35ab870da8403cd65758a7d2acee796995b2a31ca68c91f7a92c`。
- D17 仍实施中：隔离专服 restart 验证了实体 region 中 slam NBT 保存，但无玩家 headless server 未激活该实体，真实玩家加载、客户端动画和余下 attack-phase 连续性待验收。继续依差异清单停留 rank34；不因“继续”视作批准 D14 概率、C03 放火、D28/D42 伤害/状态选择。

### 阶段 59：D35 源时序取证与修复前反例（2026-09-30）

- 按 rank35 先核对 Sorcerer event-age40 的 animated_java leaf、`start`、`subtick`、Ice Spikes generated block48–52/59–61、`hit` 与 passthrough tag；原包只读。leaf40 执行相对 staff world Y+2 的 wave 函数；每个波标记初始向前 0.4，block51 每子步先计龄、再前进0.5、再 `remove @s value 1`，block52 到 `value<=0` 才生刺。源初始化没有给这个实体分数；隔离 vanilla GameTest command 确认对缺失分数执行 remove 1 后实际为 -1，因此首刺发生于第1 tick，后续第6/11/16/21 tick，半径为0.9/3.4/5.9/8.4/10.9。
- 源生成顺序是 spike subtick 在 wave subtick 前：新刺在出生 tick不移动，下一世界 tick timer1 上升0.4；timer6–10 以刺根为原点运行 `@a[distance=..2]`；timer20–24下降，25清除。`hit` 使用玩家 selector。passthrough tag递归从320向下到首个非标签方块；原生应受世界高度/已加载区块界限保护。
- 修复前新增首圈第7 tick伤害、NPC不得受击两个反例，在 `run-d35-red-20260930` 运行180项：两项 D35 断言按预期失败；另有六项既有的非 D35失败。未删除/放宽反例。源与攻击事件SHA-256汇总于 [D35验证记录](nyctocosm-native/docs/validation/D35_ice_spikes.md)。
- 此阶段仅记录可执行差异和红测结果；无玩法选择获批。

### 阶段 60：D35 波前、地表锚点、命中和恢复（2026-09-30）

- `IceSpikeWave` 使用绝对 WaveAge，按1/6/11/16/21生成30臂，保存/同步攻击yaw、当前波标记表面和150个每圈每臂刺表面锚点。每根刺的根位置、6–10命中阶段与模型显示高度分离计算；地表搜索服从passthrough tag、世界build height和loaded chunk限制。NativeBoss把staff朝向交给波实体；`ClientEvents` 按同步年龄和表面锚点只显示剩余刺。伤害只筛玩家并用脚位置到刺根的半径2球面精确过滤，保留Forge伤害流程；D28取消伤害状态策略没有改动。
- 四项 D35 GameTest通过：source tick1/5/6/10/11/25的波龄/圈数/半径、缺失计分项remove语义与yaw；首圈第7 tick地表上玩家受击、snow passthrough和垂直/水平球面边界；NPC免受该源player-only攻击；保存age32并换新实体载入后保留150锚点、不会重播已结束的内圈命中，age45继续到末刺timer24、age46清理。
- 最新全套 `gradlew.bat runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d35-source-score-final-20260930"`：**182项中180项通过**，仅两项既有 Flame Geysers 测试失败：`flame_geysers_caps_at_two_players_without_cross_spreading_node_groups`、`flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`。该日志内四项 D35 全部通过。独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。`python .\tools\audit_native.py .\build\libs\nyctocosm-native-0.3.0-native-alpha.12.jar`：errors=[]、3,024 entries、55,719,531 bytes、SHA-256 `48aef11ef9b55bbecb2b1209c158d78c76b03168098d7a30ea6c92bde7bb509b`；JAR中没有 `NativeGameTests`/`ClientSmoke`。重建前将旧 alpha.12 jar备份到 `nyctocosm-native/run-d35-source-score-final-20260930/pre-build-nyctocosm-native-0.3.0-native-alpha.12.jar`，旧摘要 `2cf4574920fe35ab870da8403cd65758a7d2acee796995b2a31ca68c91f7a92c`。
- 源文件、原存档、相邻MDK和安装目录均未改；测试/build进程已结束。此工作副本没有`.git`元数据，Git状态/`git diff --check`不可用；对四个Java文件、三份根文档及D35报告作独立尾随空白扫描后通过（0行）。
- 状态仍是**实施中**：客户端中途追踪/晚加入画面与真实chunk unload/reload或专服重启未验收。D35报告、差异行和方案实现记录已更新。未修改任何待确认玩法选择；D14非玩家Hypertrace率仍无回答；C03、D28/D42、D02/D03/D20/D41等选择已集中发给用户，收到答复前不依其分支实现。下一顺序为差异清单rank36 D46；继续独立、可实施的代码项。


### 阶段 61：D02 已确认模式恢复与 D46 主副手冷却首片（2026-09-30）

- 按用户对 D02 的明确选择“按建议恢复仅由流程改动的模式”实现模式单人切片：使者安全入域成功后只把 Survival 切为 Adventure；离开领域或死亡重生时，仅当前仍是流程管理的 Adventure 才恢复原模式。标记保存在玩家 persistent data 并随 Clone 复制，避免覆盖既有 Creative/Spectator 或流程期间外部改模式。没有把该回复扩展为队伍入口、全队/局部失败、扣料/回滚或补偿许可；这些仍待用户确认。
- 在 `NativeGameTests` 增加模式状态断言；本阶段完整运行 `run-d02-mode-d46-final2-20260930` 共185项，181项通过。D02 `incurrence_mode` 与 D46 两项 `weapon_ability_cooldowns` 均通过。4项失败：两项 Flame Geysers、`emissary_wing_parts_take_ranged_spell_damage`、`serpent_beam_hits_high_targets_and_keeps_round_boundary`。失败日志：`nyctocosm-native/run-d02-mode-d46-final2-20260930/logs/latest.log`。
- D46 新增玩家级服务端能力计数器，Nullfissure swing/cast 20/25 tick、Semblance swing/shoot 30/30 tick独立；Scythe/Wyrmsliver 无源副手入口返回 PASS。玩家数据按能力保存并在登录同步 UI 计时，保留 Scythe 现有 20 tick 冷却，等待 D41 决定。两项自动测试覆盖独立启动/递减、玩家数据复制与实际 offhand use 返回。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 audit：errors=[]、3,026 entries、55,725,023 bytes、SHA-256 `6e72f036e87cae06566a24d078a0e1afbf4e8168a066afc8a8a8bb1d952271a4`；排除 GameTest 与 ClientSmoke。重建前的 D35 包 `48aef11ef9b55bbecb2b1209c158d78c76b03168098d7a30ea6c92bde7bb509b` 已复制备份至该隔离运行目录。
- D02 仍实施中：多人参与者/逐人返程、无安全落点失败策略和真实入口尚未验收。D46 仍实施中：服务端自动验证与构建审计已过，客户端主/副手冷却覆盖、真实断线/服务端重启恢复未实机验证。D35 仍待客户端晚加入与实际区块卸载/重载验证。
- 对本轮 6 个 Java 源文件、D02/D46 报告及根计划、差异表、补充指南、TODO 做 PowerShell 尾随空格/制表符扫描，结果 0 行。D46 报告中的 14 条原包源码路径均逐条存在且 SHA-256 与本地只读源文件一致；报告 16 个摘要字段长度均为 64 个十六进制字符。JAR 条目查询未发现 `NativeGameTests` / `ClientSmoke`。工作副本无 `.git` 元数据，未声称 git status 或 diff-check。alpha.12 版本不变；原数据包、真实存档、相邻 MDK 与安装目录未修改，无提交/分发。

### 阶段 62：D34 Seeking Singularities 追踪、反射与命中闭环（2026-09-30）

- 复核 `summon`/`subtick`/`hit` 和生成块141–158，确认8枚、70–89 tick、0.24格/tick；每 tick 找50格内最近非旁观玩家眼部，方向为 `normalize(6×当前前向 + 0.5×指向眼部)`；不做方块终止。源 hit 选择器后执行 Void damage，再无条件给5秒 Slowness II。源文件与 Reflectance 摘要收录在 [D34报告](nyctocosm-native/docs/validation/D34_seeking_singularities.md)。
- 寻星弹独立服务端 tick：动态选人（补上精确50格边界）、眼部转向、源半格玩家碰撞盒、穿越任意实心方块、到期清除；Slowness II 按用户明确答复“仍按命中选择器施加迟缓”实现，即使目标无敌或 Forge 伤害被拒也施加。加专属三色粒子/逐tick Gravitance hum。反射会清除旧追踪并保留 IgnoreBlocks；补上 entity-hit query 为空时的安全处理。
- 共7项 `seeking_singularities` GameTest：最近目标重选、50格闭边界、8枚数量/寿命/速度、过期不误伤、黑曜石墙后命中、拒伤仍迟缓、反射后穿墙命中；最终全绿。边界反例先红；反射回归捕获了 query 空结果导致的 NPE，修复后回归通过。
- 最终整套 `gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d34-source-feedback-final-20260930"`：192项中189通过、3项失败，分别为一个 Wing Blast 和两项 Flame Geysers，D34七项均通过。日志 `nyctocosm-native/run-d34-source-feedback-final-20260930/logs/latest.log`。独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- alpha.12 audit `errors=[]`：3,026 entries、55,726,735 bytes、SHA-256 `55b1329dfa2bd540844bfe0afc1dd3fe8d5b3e0a26fb8eb4aec6ae3c5bf70586`；无 `NativeGameTests`/`ClientSmoke`、runtime mcfunction。审计前/后快照为 `nyctocosm-native/docs/native-audit-before-d34-20260930.json`、`native-audit-after-d34-20260930.json`；构建前包 `cdfbb80330d11f68fcf799852c1411af122f0cb28191365be9ca10d0b6f9b38a` 已备份在最终隔离运行目录。
- D34 自动验证通过·待实机：未验收真实客户端方向/音画、damage stats 与 D22 动态伤害、跨区块卸载/重载/专服重启后的追踪续接。未改源包/真实存档/相邻MDK/安装目录；alpha.12不升版，无提交或分发。下一个未完成 rank38 为 D48 Serpent Laser 命中体积与受伤间隔。

### 阶段 63：D48 Serpent Laser 脚部球采样与 HurtTime（2026-09-30）

- 从原包 `start`、`subtick`、生成块205–207、raycast、while/3–5、block208–215 核对：marker 每 active tick 前进0.5；ray 每整格取样 1–150，玩家 `@a[distance=..3]` 以脚部为中心；命中前读取 `HurtTime`，非零跳过；归零才执行基础12 Nightmare。最初 D48 实现把该招错误接入通用连续线段/AABB 测试。
- `Combat.serpentLaser` 按每整数格点查询玩家脚部半径3并检查 `hurtTime==0`，同一调用按玩家去重，仍经 `Combat.hit` 保留 Forge 伤害流程；`NativeBoss.tickSerpent` 的 Laser 分支切到专用方法，普通 beams/线段伤害未改变。
- 新增三项 `serpent_laser_damage` GameTest。旧路径红测分别证明：被扩展目标盒擦到但脚部在范围外会误伤、非零 HurtTime 仍受击、玩家选择器错误伤到 Cow；修复后均通过，且恰好半径3可命中。既有 `serpent_laser_survives_reload`、固定 marker sweep 一并在全套中回归。
- 最终 `gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d48-source-samples-final-20260930"`：**195项中193项通过，三项D48测试通过**；两项失败均为既有 `flame_geysers_*`。日志 `nyctocosm-native/run-d48-source-samples-final-20260930/logs/latest.log`。独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- alpha.12 audit `errors=[]`，3,026 entries、55,727,468 bytes、SHA-256 `0918620d7f7bb60e10604fea3d2e0c26a5ae4a1ad50d6eb5dc75b301dafc0f87`；无 GameTest/ClientSmoke/runtime mcfunction。前后审计快照 `nyctocosm-native/docs/native-audit-before-d48-20260930.json`、`native-audit-after-d48-20260930.json`；重建前包 `55b1329dfa2bd540844bfe0afc1dd3fe8d5b3e0a26fb8eb4aec6ae3c5bf70586` 已备份。
- D48 自动验证通过·待实机：真实marker在100 tick移动期间的可命中 tick 集合、客户端表现、Forge第三方伤害取消与实际区块卸载/重载仍待验收。未修改原包/真实存档/相邻MDK/安装目录；alpha.12不升版、无提交/分发。下一项为差异清单 rank39 D36 Sorcerer Void Wave / Javelin Swing 命中原点。

### 阶段 64：D36 Void Wave / Javelin Swing 脚部球命中原点（2026-10-01）

- 沿原包调用链核对 `leaf_38 → voidwave/discharge → @a[distance=..6]` 与 `leaf_35 → javelinswing/swing → @a[distance=..3]`：前者从根部世界 Y+2 施法，后者在实体局部 `^ ^1 ^2` 原点取玩家距离；两招选择器都量到脚部。Javelin 原点方向按本地 1.20.1 `LocalCoordinates.getPosition` 映射字节码确认 yaw/pitch 变换。
- 新增 `Combat.playerFeetSphere`：AABB 仅作候选粗筛，按 `Player.position()` 精确过滤闭边界球体，只查询玩家，命中保留 `canHit` 与 Forge `Combat.hit` 管线，不添加额外固定击退。Void Wave 接入世界 Y+2 半径6；Javelin 接入 yaw/pitch 旋转后的局部 up+forward 原点半径3；移除两处旧 `Combat.area` 推力，恢复 Javelin 7 个 Sweep 粒子，并将 Void Wave 直接爆发/环形 cue 放至 Y+2。攻击 tick38/35 未动，D28 的命中状态未在本项代批。
- 两项 `sorcerer_hit_origins` GameTest 均先在旧路径红、后在修复路径绿：测试包含 Void Wave Y+2 与半径6边界/外部脚部但身体进入粗筛框，Javelin yaw90°/pitch30°局部坐标、半径3边界/外部脚部，以及两招玩家选择器不伤 Cow。
- 最终全套 `run-d36-source-origins-verified-20260930` 为**197项中194项通过**，两项D36测试全部通过。三项未通过：两项既有 Flame Geysers（节点组/第40帧）与 `serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`。日志 `nyctocosm-native/run-d36-source-origins-verified-20260930/logs/latest.log`。独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- alpha.12 JAR 审计 errors=[]，3,026 entries、55,727,848 bytes，SHA-256 `e099c311396850ff5b5f0964f00158b62ea886ab6f61c33d3a45d483bd22f298`；无 GameTest/ClientSmoke/runtime mcfunction。前后快照为 `nyctocosm-native/docs/native-audit-before-d36-20261001.json` 与 `native-audit-after-d36-20261001.json`。重建前包 `0918620d7f7bb60e10604fea3d2e0c26a5ae4a1ad50d6eb5dc75b301dafc0f87` 已备份至隔离运行目录。完整源/实现 SHA-256、测试说明及待实机范围见 [D36阶段报告](nyctocosm-native/docs/validation/D36_sorcerer_melee_origins.md)。alpha12不升版；未改原包、真实存档、邻接MDK或安装目录，无提交/分发。差异清单 rank39 D36 更新为自动验证通过待实机；下一项按顺序为 rank40 D45 Semblance of Doom 命中面。

### 阶段 65：D45 Semblance of Doom 横斩/纵斩命中采样（2026-10-01）

- 沿原包 `swing → OnGround 分支 → 生成块62/63 → swing_hitbox → block64` 核对：ground/air 原点分别为 Y+1.5/Y+0.9，72 点半径2局部圆，点位角 `i*5`；`dx=0` 查询形成 1×1×1 盒。目标先收集后按标记去重，block64 先伤害再无条件 `Fire:100s`。
- Semblance 主手命中单独切至源样点：地面按 yaw local left/forward，空中按 yaw/pitch 旋转 local up/forward；候选通过导入的原包 `livingthings`（mobs + player）类型标签过滤，从 72 样点盒中收集后按 UUID 去重，每目标只尝试一次 19 Dissonance。依用户确认，Forge 取消伤害不阻止后续 5 秒燃烧。Nullfissure 锥形、冷却与攻击属性保持不变；复用已有 72 点视觉轮廓。
- 两项 `semblance_swing_samples` GameTest 修复前均红，最终均绿：地面侧后命中、环外前方不命中、旋转空中竖直环命中及拒绝伤害仍燃烧、空中不选地面专属局部左右目标、Armor Stand 按源类型标签排除。
- 最终全套 `run-d45-source-tag-verified-20261001` 为**199项中193项通过**，6 项失败为两个 Wing Blast、`serpent_beam_selects_late_target_after_reload`、两个 Flame Geysers 和 `serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`；D45 两项均通过。日志 `nyctocosm-native/run-d45-source-tag-verified-20261001/logs/latest.log`。独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- alpha.12 审计 errors=[]，3,029 entries、55,730,260 bytes，SHA-256 `bd6271b24d07eb2975225d556daa8ed1395a22a32e4557277cc08bd57ba1a35b`；无 GameTest/ClientSmoke/runtime mcfunction。快照为 `nyctocosm-native/docs/native-audit-before-d45-20261001.json`、`native-audit-after-d45-20261001.json`；构建前包 `21484d84b2eef74651e018e2cd945838dc1aba8d5e770028019d9b47b127d3d2` 已备份到本次隔离运行目录。源/实现 SHA-256 与验收范围见 [D45阶段报告](nyctocosm-native/docs/validation/D45_semblance_swing_samples.md)。alpha.12不升版；未修改原包、真实存档、相邻 MDK 或安装目录，无提交/分发。差异清单 rank40 D45 更新为自动验证通过待实机；下一项按顺序为 rank41 D51 Expulsion 爆炸目标脚部距离边界。
### 阶段 66：D51 Expulsion 爆炸脚部距离边界（2026-10-01）

- 沿原包 `explosion → @e[type=#nyctocosm:livingthings,distance=..6,tag=!damagetemp] → block32/33` 核对：目标距离从爆炸实体原点量到实体脚部，球形三维半径6；爆炸来源 `damagetemp` 排除。另核对 `subtick → block28/29`，标记15 tick后爆炸，连锁击杀建立独立下一标记。伤害基值7、归属和延迟行为保持本项范围外既定语义。
- 新增 `Combat.livingFeetSphere`：半径AABB仅粗筛，随后按 `LivingEntity.position()` 精确过滤闭边界3D球；只选择导入源 `livingthings` 标签实体、跳过 `damagetemp`，仍走 Forge `Combat.hit`。`ExpulsionBurst.explode` 单点改用新筛选；Combat.area、原点、半径6、7 Dissonance、所有者、零推力和保存/延迟链均未改。
- 两项 `expulsion_feet_sphere` GameTest 修复前均红，修复后均绿：水平脚部5.99/6.01，垂直上方5.99，以及巨大 Giant 身体进入但脚部在下方6.01格；同轮既有15 tick链路与D45两项测试也通过。
- 最终全套 `run-d51-feet-verified-20261001` 为**201项中195项通过**。6 项未通过：`serpent_beam_selects_late_target_after_reload`、两个 Flame Geysers、`emissary_wing_parts_take_ranged_spell_damage`、`serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`、`celestial_smite_player_contact_stops_early_and_open_ray_checks_at_most_150_cells`；均未调用 D51 路径。日志 `nyctocosm-native/run-d51-feet-verified-20261001/logs/latest.log`。独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- alpha.12 审计 errors=[]，3,029 entries、55,730,499 bytes，SHA-256 `cfe7c04cea23a5281e7002b2f4fafc9a50337c0c18643535b64171100c8255ab`；无 GameTest/ClientSmoke/runtime mcfunction。快照为 `nyctocosm-native/docs/native-audit-before-d51-20261001.json`、`native-audit-after-d51-20261001.json`；构建前包 `bd6271b24d07eb2975225d556daa8ed1395a22a32e4557277cc08bd57ba1a35b` 已备份到本次隔离运行目录。源/实现 SHA-256 与验收范围见 [D51阶段报告](nyctocosm-native/docs/validation/D51_expulsion_feet_sphere.md)。alpha.12不升版；未改原包、真实存档、相邻 MDK 或安装目录，无提交/分发。差异清单 rank41 D51 更新为自动验证通过待实机；下一项按顺序为 rank42 D15 Reflectance 出射方向/边界。

### 阶段 67：D15 Reflectance 球形选择器、容量与归属协议（2026-10-01）

- 对照 `spells/functions/load.mcfunction`、`reflectance/reflect.mcfunction` 与生成块的 `distance=..3` 分支：默认储量20；按各攻击 `TEMP s.damage` 扣除；目标采样是球形中心距离，nearest 异主 reflector 决定复制出的 owner 和 facing。同一反射后攻击归属改变，后续相同盾标记不应再次被选为 redirect。
- 新增 `ReflectableAttack`/`Reflectance.tryReflect` 共用协议：AABB 只做候选粗筛，精确半径3球体决定命中；有异主 redirect 候选后，本次 selector 内所有盾按当前攻击 cost 扣容量，方向及 owner 来自最近异主 shield。SpellBolt 移除迎面 dot 门槛、保留速度、改 owner、清 homing，并存 reflection gameTime 防同 tick 重入。
- 接入现有 Emissary beam/Wingbeat、Wing Blast、Rain Shard、Pillars、Flame Slash：保留原 Boss UUID 供遭遇生命周期用，单独保存/传递 `DamageOwner` 给后续伤害；攻击 cost、sample 时点、外部速度 profile 保持调用方规则。Apocalypse Rain 反射后第二个同 tick 子步由归属过滤通过，剩余8而不是重复收取12。
- 新增 3 格侧向/背侧/精确边界、3.1外侧、AABB角落、精确20耗尽、Wingbeat同节点三步只扣一次等断言。SpellBolt容量、Emissary beam、Wing Blast、Pillars、Rain 测试均绿。最终完整 `run-d15-reflectance-verified3-20261001`：**203 项中201项通过**；仅 `flame_geysers_caps_at_two_players_without_cross_spreading_node_groups` 与 `flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers` 失败，均无 D15 调用路径。日志：`nyctocosm-native/run-d15-reflectance-verified3-20261001/logs/latest.log`。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 audit `errors=[]`、3,032 entries、55,736,204 bytes、SHA-256 `580a30ba94c2947e75a414d7c5baaf19cce134d9cabfbb4660141b015be35118`；无 GameTest/ClientSmoke/runtime mcfunction。快照 `nyctocosm-native/docs/native-audit-before-d15-unified-20261001.json` 与 `native-audit-after-d15-unified-20261001.json`；构建前 JAR `cfe7c04cea23a5281e7002b2f4fafc9a50337c0c18643535b64171100c8255ab` 备份于最终运行目录。完整 Java hashes 与源对照见 [D15 阶段报告](nyctocosm-native/docs/validation/D15_reflectance.md)。
- D15 仍实施中：Halo/Void Beam native 分支属于 D32；power 非默认时的反射容量属于尚未闭合的 D22 属性管线；重叠盾 tie 顺序和客户端/多人实机待验。未借此确认这些待决边界。alpha.12 不升版；未修改原包、真实存档、邻接 MDK、安装 Mods、无提交/分发。下一项按清单 rank43 D16 passthrough 与物理碰撞；遇到未决玩法边界先向用户确认。

### 阶段 68：D16 passthrough 与 SpellBolt 碰撞策略（2026-10-01）

- 对照源 `passthrough.json`、Emissary fireball/lightning_javelin/seeking_missiles slowcast 和法术移动分支确认标签语义：穿越集合与无物理碰撞形状并不等价。对审计过的分支启用源 tag policy；未审计弹体和原版箭保持各自旧规则。Abhorrence、Incandescence、Fulminance 与三类 Emissary 弹体按 tag 穿越，Gravitance 在阻挡体素停留但继续攻击计时，seeking singularity 忽略方块。
- 新增 `SpellCollisionPolicy` 和有界体素 DDA；多轴同时穿越时检查侧向体素，实体扫掠独立执行并与方块命中比较先后。Forge projectile impact 事件继续有效；build-height 是阻挡，未加载 chunk 和无效/过长扫掠停止推进。策略做 NBT 持久化并保留旧 `IgnoreBlocks` 字段兼容。
- 四项 D16 GameTest 均通过：tag 与碰撞形状对照、快弹穿墙/对角角点/build-height/未加载 chunk、Forge impact 取消、NBT 往返。最终全套 `run-d16-collision-verified3-20261001` 为 **207 项完成，204 项通过**；3 项失败是两项既有 Flame Geysers 与 `serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`，均未经过 D16 policy。日志 `nyctocosm-native/run-d16-collision-verified3-20261001/logs/latest.log`。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 审计 `errors=[]`，3,036 entries、55,744,877 bytes，SHA-256 `c06535ea76f3c1027c3332b51bdcca8b207e03c810763b204fc39f20ff2c96ee`；无 GameTest/ClientSmoke/runtime mcfunction。审计快照 `nyctocosm-native/docs/native-audit-before-d16-collision-20261001.json`、`native-audit-after-d16-collision-20261001.json`；构建前 JAR `580a30ba94c2947e75a414d7c5baaf19cce134d9cabfbb4660141b015be35118` 已备份至隔离运行目录。实现文件 SHA 与限制见 [D16 阶段报告](nyctocosm-native/docs/validation/D16_spell_collision.md)。alpha.12 不升版；未改原包、真实存档、相邻 MDK、安装 Mods，无提交/分发。差异清单 rank43 标记为自动验证通过待实机；下一项按序 rank44 D18 共享 locator 与飞行根姿态。

### 阶段 69：D18 使者 locator 发射点与飞行根 pitch（2026-10-01）

- 扩展 Emissary 命令 locator 导出，新增 lefthand、四个 wingtip2 seeker 节点与 grapple wingtip3；覆盖 44 动画、2601 帧、16 locators。与 D16 包中旧 10 locator 比较，26,010 个旧采样完全一致。locator 审计器同步验证新集合与帧完整性。
- 新增共享 `EmissaryPose.localPoint`，将服务端 locator 与客户端 root 变换统一为 Minecraft yaw/pitch 坐标基底；使者模型渲染按 entity XRot 插值应用 pitch。接入 Fireball 左右手、Lightning Javelin 右手、Seeking Missiles 7 个动画节点、Meteor Shower 与 Wingbeat center、Divine Blast/Godrays center、Grapple 抓取座椅 wingtip3，新增 Sword Plunge 右手到刀尖的生成粒子轨迹。
- 两项 D18 GameTest 通过：四个正交 yaw 与正负 pitch 的局部基向量/世界点匹配，关键帧半帧插值；Fireball 第 8/18 帧手部原点、Seeking Missiles 七个 locator 节点和 Grapple wingtip3 座椅原点。最终完整 `run-d18-pitch-nodes-verified3-20261001` 为 **209 项中205项通过**；4 项未通过：`expulsion_uses_inclusive_horizontal_target_feet_radius`、两项既有 Flame Geysers、`serpent_acquires_player_through_wall_inside_source_range`，均不调用 D18 新接入分支。日志 `nyctocosm-native/run-d18-pitch-nodes-verified3-20261001/logs/latest.log`。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 审计 `errors=[]`、3,037 entries、55,933,196 bytes，SHA-256 `34054f771fa9586bee0fc7d506804c9367b70a96d03f0bcb45c78f6254cc6c67`；无 GameTest/ClientSmoke/runtime mcfunction。locator 审计前后快照 `nyctocosm-native/docs/native-audit-before-d18-pitch-20261001.json`、`native-audit-after-d18-pitch-20261001.json`；构建前 D16 JAR `c06535ea76f3c1027c3332b51bdcca8b207e03c810763b204fc39f20ff2c96ee` 已备份至 `nyctocosm-native/run-d18-pitch-nodes-verified3-20261001/pre-build-backup/`。各实现 SHA、导出摘要与客户端验收剩余项见 [D18 阶段报告](nyctocosm-native/docs/validation/D18_emissary_pose_and_locators.md)。alpha.12 不升版；未改原包、真实存档、相邻 MDK、安装 Mods，无提交/分发。D18 仍实施中；下一项按序 rank45 D19 Fireball 两子步速度与 60 tick 终止。


### 阶段 69：D18 使者 locator 发射点与飞行根 pitch（2026-10-01）

- 扩展 Emissary 命令 locator 导出，新增 lefthand、四个 wingtip2 seeker 节点与 grapple wingtip3；覆盖 44 动画、2601 帧、16 locators。与 D16 包中旧 10 locator 比较，26,010 个旧采样完全一致。locator 审计器同步验证新集合与帧完整性。
- 新增共享 `EmissaryPose.localPoint`，将服务端 locator 与客户端 root 变换统一为 Minecraft yaw/pitch 坐标基底；使者模型渲染按 entity XRot 插值应用 pitch。接入 Fireball 左右手、Lightning Javelin 右手、Seeking Missiles 7 个动画节点、Meteor Shower 与 Wingbeat center、Divine Blast/Godrays center、Grapple 抓取座椅 wingtip3，新增 Sword Plunge 右手到刀尖的生成粒子轨迹。
- 两项 D18 GameTest 通过：四个正交 yaw 与正负 pitch 的局部基向量/世界点匹配，关键帧半帧插值；Fireball 第 8/18 帧手部原点、Seeking Missiles 七个 locator 节点和 Grapple wingtip3 座椅原点。最终完整 `run-d18-pitch-nodes-verified3-20261001` 为 **209 项中205项通过**；4 项未通过：`expulsion_uses_inclusive_horizontal_target_feet_radius`、两项既有 Flame Geysers、`serpent_acquires_player_through_wall_inside_source_range`，均不调用 D18 新接入分支。日志 `nyctocosm-native/run-d18-pitch-nodes-verified3-20261001/logs/latest.log`。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 审计 `errors=[]`、3,037 entries、55,933,196 bytes，SHA-256 `34054f771fa9586bee0fc7d506804c9367b70a96d03f0bcb45c78f6254cc6c67`；无 GameTest/ClientSmoke/runtime mcfunction。locator 审计前后快照 `nyctocosm-native/docs/native-audit-before-d18-pitch-20261001.json`、`native-audit-after-d18-pitch-20261001.json`；构建前 D16 JAR `c06535ea76f3c1027c3332b51bdcca8b207e03c810763b204fc39f20ff2c96ee` 已备份至隔离运行目录。各实现 SHA、导出摘要与客户端验收剩余项见 [D18 阶段报告](nyctocosm-native/docs/validation/D18_emissary_pose_and_locators.md)。
- alpha.12 不升版；未改原包、真实存档、相邻 MDK、安装 Mods，无提交/分发。D18 仍实施中；下一项按序 rank45 D19 Fireball 两子步速度与 60 tick 终止。

### 阶段 70：D19 Fireball 子步、超时与源选靶（2026-10-01）

- 核对原包 `phase4.attack.fireball` 第 8/18 帧由右/左手 locator 发射；block/711/712 对每发弹独立随机选取发射点 100 格内、非旁观且不带 `emissarydebug` 的玩家，并瞄准脚部。原生按同一过滤条件选靶，空选择沿 locator 朝向。核对第四阶段选招：地面 roll 66..85 才抽到 Fireball，飞行池不含 Fireball；删除原生此前第 20 帧 50% 概率重播同招的分支。
- `SpellBolt` 为 Emissary Fireball 增加独立 flight profile：每 tick 两个一格子步、计时 60；前 59 tick 共前进 118 格，第 60 tick 不移动并只终止一次。实体、阻挡体素、到期路径分别标记 ENTITY/BLOCK/TIMEOUT；保留 D16 passthrough、Forge projectile impact 与 D15 反射后的双子步。
- D19 GameTest 覆盖双子步/速度、59/60 tick 边界、实体扫掠、首个阻挡体素、反射、选靶排除规则/脚部向量、左右手发射与第 20 帧不重播；这些 D19 断言全通过。完整 `run-d19-fireball-targeting-final2-20261001` 为 **215 项完成，211 项通过**；4 项失败是两项既有 Flame Geysers、Ice Spikes 首环和 Flame Slash 轨迹用例，不经过 Fireball 路径。日志见 `nyctocosm-native/run-d19-fireball-targeting-final2-20261001/logs/latest.log`。
- `compileJava` 与独立 `gradlew.bat build --offline --console=plain` 均成功。alpha.12 JAR 审计 `errors=[]`，3039 entries、55,936,809 bytes，SHA-256 `e6d49d3838582607255c789ac7fbb41124a531071bd276b519b87a2377df832f`；无 GameTest/ClientSmoke/runtime mcfunction。审计快照为 `nyctocosm-native/docs/native-audit-before-d19-targeting-20261001.json` 与 `native-audit-after-d19-targeting-20261001.json`；构建覆盖前包已备份在隔离运行目录。细节见 [D19 阶段验证](nyctocosm-native/docs/validation/D19_emissary_fireball.md)。
- 原包终端 impact 还会在 3×3×3 范围把空气设为火；该世界方块效果正等待用户选择是否实施及 Forge 保护策略，不按默认同意处理。因此 D19 仍实施中，不推进 rank46 前先完成该分支。客户端、多人战斗、保护插件和真实存档未验收；alpha.12 未升版、未改源包/真实存档/邻接 MDK/安装 Mods，无提交或分发。


### 阶段 71：项目重新检查、失败夹具修正与 alpha.13 交付（2026-10-01）

- 本阶段响应用户“重新检查一遍项目，完成后编译出一版mod”。重读修复提示词、差异清单、方案、实施补充、扩展审计和TODO尾部；并行复查生产代码/专服分离/保存/注册/打包及多轮回归失败。未把未确认玩法作为已批准。
- D02：补正已确认的“流程结束、死亡或退出”范围，`NativeBoss.completeDefeat` 在使者100死亡tick最终结算时恢复流程实际改动且仍管理的模式，不覆盖外部Creative/Spectator。`NativeEvents.spawn/incur` 阻止旧使者尚未移除（含死亡动画）时开始新流程，避免结算重叠。新增结算99/100、模式保护、幂等与入口排重回归。多人入口、失败补偿、encounter ID与离线关联仍实施中。
- D19：重新核对 impact/slowcast 和 generated block705–707，把终端伤害从通用FIRE路径切至玩家脚部半径5球面，排除spectator/debug；保留基础35 Dissonance及Forge/原版难度缩放，去除源外实体燃烧和范围推力。碰撞候选排除NPC/debug。新增水平/垂直5闭边界、5.01反例、无敌保护、抗击退、候选及一次终止测试。世界3×3×3放火仍待选择，D19没有整项勾选。
- 失败诊断：Geysers的created=28但查询0/14来自实体区块未可见及同批玩家/节点串场；Ice首环的90层石板被更高天然地表覆盖，NPC测试受并行攻击污染；FlameSlash固定120高路径撞天然地形；WingBlast同批射线/reflector串场；Serpent晚选平台在-58海底而整片顶面62为水，且独立场地缺少满足500格存活规则的ServerPlayer。仅修测试准备/归属/场地，不削弱14/28节点、命中边界、帧或生命周期断言。临时region票据等待areEntitiesLoaded/isPositionEntityTicking，并用通过/失败监听器回收自身实体和票据。1200 tick只放宽加载准备上限，攻击时序不变。
- 保留红测/诊断证据：`run-project-recheck-baseline-20261001` 215/212，2项Geysers与Ice NPC失败；`run-project-recheck-regressions-red-20261001`、`red2`各218/209（模式结算/火球半径暴露旧实现，另含加载准备失败）；`run-project-recheck-green-20261001` 218/214（2项Wing、Serpent存活夹具、Fireball预期需保留Easy缩放）。全部旧日志、世界和JAR保留。
- 最终 `gradlew.bat runGameTestServer --offline --console=plain '-PgameTestRunDir=run-project-recheck-alpha13-final-20261001'`：**219/219 required通过，退出0**。换新世界 `gradlew.bat runGameTestServer build --offline --console=plain '-PgameTestRunDir=run-project-recheck-alpha13-confirm-20261001'`：**219/219 required通过，build/reobfJar成功，退出0**。两个种子分别8305437981795680883、-6212535458866628845，不用同一测试世界重复结果充当新隔离复验。
- 新版本 `0.3.0-native-alpha.13`，路径 `D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.13.jar`，**55,937,267 bytes，3039 entries**，SHA-256 **`1fd0e3caaac1cdcc3b7647bc9e93f92b88c0f3e4875af58024cfabfe124c604c`**。`python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.13.jar`退出0、errors=[]，包内版本匹配，无NativeGameTests/ClientSmoke、测试结构或runtime mcfunction。构建前后审计、完整最终控制台、源/Java摘要和交付manifest位于 `nyctocosm-native/docs/validation/project-recheck-20261001/`。
- 原alpha.12仍为55,936,809 bytes，SHA-256 `e6d49d3838582607255c789ac7fbb41124a531071bd276b519b87a2377df832f`，未被覆盖；另存为证据目录 previous-alpha.12.jar。旧版本产物全部保留。版本引用、D02/D19验证报告、D09规格补正、差异行、方案和实施补充已更新。完整报告见 [alpha.13项目复查](nyctocosm-native/docs/validation/PROJECT_RECHECK_ALPHA13.md)。
- Git事实更正：根目录不是仓库，但 `nyctocosm-native/.git` 实际存在，HEAD193a7d8；此前阶段“没有Git元数据”判断错误，历史原文保留。本轮用命令级safe.directory读取，未改全局信任设置；git diff --check按cr-at-eol处理Windows行尾，退出0。已有修改/未跟踪文件均保留，无commit/push。
- 新发现D09技术缺口：原`spreadplayers 5 25`用X/Z方形±25区域，原生仍用半径25圆盘；之前“最大半径”解释已更正，分布/量化尚未对齐。此次数量/时序验证不能证明散布分布等价，D09保持实施中。
- 未实测：新JAR真实客户端/多人战斗、保护插件取消、真实chunk unload/reload/专服重启/晚加入及音画对照。NBT夹具或GameTest启动不当作真实重启验收。D19/C03放火、D20领取/拆除退款/卸载事务、D02组队补偿及其他既有待决策项继续等待用户答复。原数据包/源zip/相邻MDK/用户真实存档和安装mods未改；本轮没有自动安装或分发。全部测试/build进程已结束。
- 续接：用户本轮要求的重新检查和开发版编译已完成；下一轮按差异清单优先级处理未闭合项，首先D09散布规格/源命令独立对照，以及各高优先级待确认玩法；D19火块与D20事务收到答复后按选择实施。此构建用于用户实机验收，不表示51项完全还原。

### 阶段 72：用户实机反馈的模型／材质／粒子修复（2026-10-02）

- 按最新用户反馈优先处理客户端反例，使用diagnose流程；保留已有源、安全落地、状态／受击体、用户D02/D34/D45确认与全部工作区成果。原生默认客户端其实能看到传送门粒子，已向用户询问版本／粒子／渲染距离／shader环境，没有把该组合问题声称为已复现。
- Serpent：42段在AFTER_ENTITIES独立视锥／256格距离／局部光照渲染，避免身体继承埋入头部的区段可见性和黑光照。标准LevelRenderer仍会在根区段未编译时跳过实体，单独shouldRender=true不足；已检查同版本mapped代码。实体石块完全包住头部，绿截图780仍见外露躯干；没有修改20个受击部件。
- Emissary：按用户明确要求二阶段保留一阶段翼调色板，新增12个wing_phase2继承模型保持config1几何和透明槽；三阶段才用后续材质。材质应用次序防armor覆盖sword。后期飞行保存目标中心，96格回航／128格硬边界，无目标时沿保存中心；索敌不要求视线。红测127.8→132.299失败，绿边界测试通过；此范围是用户覆盖要求，不冒充原包所有飞行路线。
- Sorcerer：导出ajsorcerer/m1/m2源apply_variant时间线，开场staffless隐藏smear1弧片，后续合法slash/staff仍按帧切换。定向导出8条动画4100条直接／helper粒子数值，包括充能Ice/Void物品、AirDash每帧150点图案、deathpoof300墨点及三段shockwave；补single soul、90速度样点void swirl与20条void vein纯客户端数学。未全量重新导入资产，没有运行时函数执行。
- PortalActivation视觉从实体tick移至WorldEffectController；源同步年龄／服务端开门不变。客户端夹具主动canUpdate(false)后截图585/635仍有粒子，隔离客户端支持该兼容切片。用户渲染组合仍待信息与验收。
- 巨剑：.1载体默认距离限约6.4格，虽有renderer仍会被裁掉；扩展视觉bounds80／距离256，源HEAD×10模型、根yaw/pitch、sky10/block0；红490实体存在而无剑，绿490巨剑可见。D06放火和完整shockwave等原未决项没有批准。
- 巨蛇20柱粒子：连续服务器广播移到观察端，全局600个新粒子/tick预算，20柱旧4800→600（减少87.5%），仍覆盖1..80高；服务器半径5脚部伤害未修改。GameTest验证预算与伤害高度，1020截图验证密集表现；没有声称测得FPS改善。
- 红／绿证据和修改文件详见 [alpha.14客户端修复](nyctocosm-native/docs/validation/VISUAL_REPAIR_ALPHA14.md)。多轮runClient全部新建隔离世界、最后自动退出；不打开用户存档。初轮onboarding和错误飞行动画夹具日志保留。222项一条death116失败揭示实际helper遗漏，已按源码补而非删测；225项远处玩家加载夹具、Zombie自然despawn和120格目标chunk准备问题均只修测试前置，距离／时序断言不放宽。
- `run-visual-repair-green3-20261002`：225／225 required通过。客户端开场无弧片、巨剑可见、蛇头埋石身体仍见、死亡灵魂可见。源充能／AirDash场景与前一场景长寿命蛇尘曾混杂，夹具最终在场景切换后清除旧粒子，污染截图不作为该招验收。玩家真实战斗、多人、shader、保护插件和FPS仍待验。
- 十二招逐项差异表已落实到报告；D29地面节点／落冰、D30追踪弧面、D32射线节点／反射、D33锁点位移仍未完成，D28策略已向用户确认但尚无答复。不把8条动画粒子恢复当12招完整还原；玩家17招其余D13/D14/D20–D23缺口继续跟踪。

### 阶段 73：D31 Singularity Charge 弹道、到期爆炸与函数上下文（2026-10-02）

- 沿leaf44→summon→subtick→block63→explode/hit重新核实：局部(0,2,.5)发射、最近50格非旁观玩家脚部，基础Void20；独立100tick，.3先移、5/.5朝40格最近脚部转向，3格近炸、passthrough阻挡或100tick走半径5玩家爆炸。源反射成本20，检查后才转移owner／方向并重定位。旧weapon4直线弹超时静默discard的反例在223项红测中失败。
- 增加独立flight profile、one-shot、NBT／旧原生profile恢复，排除SpellArea重复提前反射；Forge伤害／projectile impact、无敌帧和保护边界保留。禁止猜测D28批准，源Slowness II10秒仍待用户确认；D34的单招选择不外推。
- 封包前发现原方案简述忽略命令上下文：tp @s后外层位置保持函数进入点，只有转向的at @s使用新位置；near/block/hum/explode/reflect仍基于进入点。新增同版本原版CommandSourceStack夹具：前方3.15、后方-2.9、timeout后方4.99、20容量反射重定位、跨体素次tick触发。独立命令验证先于原生比较；开发测试命令不进发布包。
- `run-d31-context-red-20261002`：226项、2项失败，真实暴露0.3格上下文偏差；`run-d31-context-green-20261002`：226／226 required通过、退出0。此前225／225也保留，不代替最新结果。发射／方向／NBT／3格边界／timeout及函数上下文用例未删除或放宽。
- 规格、源调用与未闭合边界见 [D31报告](nyctocosm-native/docs/validation/D31_singularity_charge.md)。差异表D31更新“实施中：弹道／终止自动验证通过，状态待选择”，方案补充原版命令上下文更正。最终alpha.14构建／包审计单列下一阶段，不把中途测试包当交付。

### 阶段 74：灵魂 helper 补正、最终客户端验证与 alpha.14（2026-10-02）

- 封包前逐行计数发现阶段72初版把death的explode接成single；源 `soul_swirl/__generated__/block/628` **实际15次single调用**。`run-soul-count-red2-20261002`：227项、1项数量反例失败，总轨迹点仅70，而15条源范围为600–1650。现在56／116帧各生成15条独立轨迹；Portal与Sorcerer共享同一计划，保留半径3..14／60%初始直线／每tick30%增半径（包括等待期）／4..11寿命／0..9延迟／十个.2子步／俯仰反射。
- 源坐标与tick顺序再补正：先减delay，0/1延迟在首次效果tick即可活动；Soul move里的emit和VoidVein direct particle保留函数进入位置；VoidVein第30tick的kill不会终止后续直接粒子命令，计30帧。`soul-count-red-20261002`的226项一条反例真实暴露该坐标／寿命差异。`minecraft` tick标签的ajloadtick先于effects，death创建shockwave后同世界tick推进timer1，因此数值帧改为116／119／121，而非117／120／122。根源摘要已纳入tick入口和predicate。
- 最终 `gradlew.bat runClient --offline -PvisualRepairSmoke -PvisualRepairRunDir=run-visual-repair-alpha14-soul-client-20261002`：BUILD SUCCESSFUL、1320tick capture complete、退出0。895截图显示恢复后的大范围多条蓝／金灵魂轨迹；410无开场弧片、490巨剑可见、585门粒子（客户端carrier canUpdate(false)）、780蛇头埋石仍见身体、1020密集蛇柱、1080独立充能、1205 AirDash几何均有最终截图。本烟测冻结部分Boss AI来复核视觉，不能充当完整生存战斗。
- 最终 `gradlew.bat runGameTestServer build --offline -PgameTestRunDir=run-visual-repair-alpha14-soul-final-20261002`：**227／227 required通过，build/reobfJar成功，退出0**。日志 `nyctocosm-native/docs/validation/visual-repair-alpha14-soul-final-20261002.log`。测试仍隔离新世界，无失败用例删除／距离放宽。此前225／226、红测日志和首个alpha14候选都保存，首候选备份 `pre-soul-count-alpha14-20261002.jar`不是最终交付。
- 最终JAR：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.14.jar`，**55,983,723 bytes，3063 entries**，SHA-256 **`d1ac9901cfde0b4b19dda39cdfdd4e72cdc0fd0bccfba1704ca0c12b66e4c55f`**。`python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.14.jar`退出0、errors=[]；检查新增变体模型／时间线、4100条粒子数值和JAR资源一致性，10rig／5607pose、2601locator、247sound保持，runtime mcfunction=0。没有NativeGameTests／ClientSmoke／测试结构，所有class major=61（Java17）。
- `python tools/package_visual_repair.py`退出0，核验源文件摘要、全部测试结果、最终客户端完成、JAR版本／资源／字节码以及alpha.13保持不变。manifest、Java／源哈希、测试与截图路径：`nyctocosm-native/docs/validation/visual-repair-20261002/manifest.json`。最终审计UTF-8快照：`visual-repair-audit-alpha14-soul-final-20261002.json`。alpha.13仍为55,937,267 bytes，SHA-256 `1fd0e3caaac1cdcc3b7647bc9e93f92b88c0f3e4875af58024cfabfe124c604c`。
- Git diff --check按Windows cr-at-eol退出0；命令级safe.directory，未改全局信任／没有commit/push。没有修改原数据包、源zip、相邻MDK、用户真实存档或安装mods。客户端／GameTest执行完自动结束；Gradle后台缓存daemon不视为游戏进程。
- 待确认／未闭合：D28其他术士命中附带状态取消伤害策略尚无答复，D34/D45既有确认继续有效；门户用户shader／粒子／版本环境待提供。D29独立球／地面节点／冰锥、D30追踪弧面、D32Halo/Void marker／反射、D33锁点位移仍是明确续接点，见报告十二招表，不能据此次粒子修复宣称全法术完成。历史D02团队补偿／D19世界火块／D20祭坛事务等等待用户；真实多人、生存Boss战、实际卸载／重启、shader和FPS待验收。

### 阶段 75：D29 Shard Hail 独立冰球／地面节点／物理冰锥（2026-10-02）

- 按用户“逐一修复法术”继续推进；重新核对35帧局部(0,2,.6)、每tick升1格、30孵化18节点、初始score0..39、41散布／落冰、90节点清理、落地2/5命中／清理。新增SorcererHail三stage独立持久化攻击，不再由Boss保存目标中心统一发18条直弹；新动画结束不删除未完成子实体。只保留旧原生HailAge字段用于旧式在途wave兼容收尾，无数据包存档迁移。
- 源多人选择器sort=nearest却没有limit，实际按固定初始位置选80格内所有非旁观玩家，依次围绕各人做方形±15散布，最终通常最远者；原版命令夹具证明，已修正旧方案“只取近玩家”的误读。41帧直接减速保持函数进入位置，圆环与落冰使用新节点位置。节点的独立减速没有伤害事件；冰锥命中附带迟缓仍等待D28，未经批准没有外推D34策略。
- source quartz CMD10模型通过内部icicle物品接入原版ArmorStandRenderer；攻击载体继承ArmorStand，沿原物理落下。独立原版ArmorStand对照逐tick位置／速度相同；半径2脚部玩家命中Void10、NPC不受击；第2落地spell tick造成伤害，第5清理，未绕过Forge／原版无敌帧。
- Forbiddance只清除源有spell标签的BALL，NODE／ICICLE不扩展反制；取消后不再孵化。NBT分别验证三个stage、HEAD、gravity、timer、41后不重复落冰。源未改，旧测试／JAR保留。
- 红 `run-d29-ball-red-20261002`：228项1失败，真实暴露35帧没有冰球；绿 `run-d29-chain-green-20261002`：**232/232 required通过，BUILD SUCCESSFUL，退出0**。报告与双侧参数：[D29](nyctocosm-native/docs/validation/D29_shard_hail.md)。客户端生存／多人／shader／真实卸载重启仍待验，当前尚未把新增代码封入发布包。
- 下一项D30：逐行核对ItemDisplay骨骼命令坐标、两段procast及12点弧形脚部区域。当前已补创建双波面红测；禁止把画面手部矩阵平移直接当execute at实体坐标。

### 阶段 76：D30 Frozen Blades 纵弧／追踪／源递归坐标（2026-10-02）

- 两个独立FrozenBlade替换1.25速度普通弹；34帧从root世界Y+3，yaw±90／pitch+20前.5发射。源码ItemDisplay hand乘客tick复制root位置／rotation，leaf只改render matrix；原版display矩阵与命令夹具证明外层(0,2,1)被at骨骼重置，不使用不存在的hand locator。补两侧10点充能与32帧sweep。
- 每tick两次12点纵弧（pitch−60..+50／前1.5），玩家脚部2格Void14；一次.4移动，首次turn被继承rotation的移动tp重置，末次5/.5追踪保留。源码是Mth facing＋LocalCoordinates lookup；独立原版tp与局部坐标夹具发现两次过度转向及理想三角精度差，修公式，不放宽边界。
- 命中／阻挡／100tick标kill后仍执行当前两次procast和一次位移再声响删除；每hitbox内去重，第二hitbox依源允许再次尝试但保留Forge／原版无敌帧。反射只在移动前，成本14／转移damageOwner／shield方向；Forbiddance跟随反射后owner。NBT独立续行，不在动画结束删除。
- 红d30-wave-red：233项2失败（当前创建反例＋既有Emissary加载夹具偶发，后续通过）；context-red：234项1失败；第一绿236项2失败揭示Mth坐标差。`run-d30-wave-green2-20261002`：**236/236 required通过、退出0**。参数／证据见[D30报告](nyctocosm-native/docs/validation/D30_frozen_blades.md)。随后以mapped字节码补定57.2957763671875的原版角度常数，将随D32整套再验。
- D28命中迟缓仍待选择，真实客户端生存／多人／shader／卸载重启未验；alpha.14已编译的交付包不被覆盖。继续D32独立Halo/Void marker；新红测用于暴露数组和多玩家生成缺口，未把旧射线表的75..110误当源命令关键帧。

### 阶段 77：D32 独立 Halo／Void Beam 与源命令选择箱（2026-10-02）

- Halo独立age/active/fired/owner；65锁定、70发射，出生score1使实际首发74而非旧计划75。Void20帧无limit生成每名玩家一个marker，30..57发射、58清理；平滑转向按原版execute forks共用进入rotation，不累计多名玩家。
- 源ray双dx=0选箱以玩家AABB相交，首个玩家或passthrough阻挡就终止，仍记录阻挡样点；保留Forge/无敌帧，纠正旧计划贯穿玩家。60×.5最大序列，Halo/Void Reflectance成本10/3；Forbiddance只清除Halo，独立NBT不重放发射。
- 源helper35/70/38数值行导入，服务端每条ray发单一路径包（≤60样点），客户端呈现，协议4。源码不改，运行时不执行mcfunction。红237项1失败；初绿241项1失败暴露多人fork，修正后`run-d32-ray-green2-20261002`：**242/242 required通过，退出0**。报告：[D32](nyctocosm-native/docs/validation/D32_sorcerer_beams.md)。
- 真实生存/多人/光影/卸载重启仍待验，alpha.14包保留。下一项D33已有两项红测：升空frame1无实际位移、Dash41仍追实时玩家；按源修固定点/根tp/进入坐标/保存，源aj.frame39未被新动画读取，不添加提前slam玩法。

### 阶段 78：D33 固定落点、命令进入坐标与根移动（2026-10-02）

- SorcererDash保存世界落点/yaw/finished/gravity；Dash37锁前15格点、41..49每帧tp1.5、49到保存点、50不移动；Air1..19升7.5、20保存目标安全地表点、26..39 tp1.5、40正常slam。staff hover与物理积分不再重复位移；换招/死亡/结束还原原gravity，新NBT续行。安全地表/XZ保持，无清块/地下回退。
- 源aj.frame39从未被ajsorcererm2动画引擎读取，纠正旧“接近提前slam”计划。Dash41..48按进入位置脚部1.5伤害，49进入位置沿根rotation前1.5/radius2与最终点1.5各一次；Air飞行不持续伤害，40进入位置radius4一次，NPC不扩大命中。均Void15/Forge/无敌帧，D28迟缓未批准不实施。
- client cue位置包使升空/位移/slam按源函数进入坐标生成数值粒子，49蓝尘独用最终点。Air20补320点双dashfly2与1flash、移动每tick35尘；不逐点广播。红244项2失败；初绿244项2失败、次绿248项3失败揭示理想Z轴预期与Mth lookup不同，改为独立原版tp，不放宽1e-6。
- `run-d33-green3-20261002`六项D33全部通过，整套**247/248**；唯一既有Celestial Smite测试原共享5000格arena混入他例玩家导致6步而非20步终止。已将该夹具移到独立arena，保留20/150断言，等待最终全套复验。报告：[D33](nyctocosm-native/docs/validation/D33_sorcerer_dash.md)。
- build.gradle已升alpha.15，旧13/14包保留。独立新世界客户端烟测正在检查Halo、Shard Hail、Frozen Blades、Void Beam、Dash Strike、Air Dash；尚未将人工关键帧夹具算作自然AI生存/多人/光影/FPS验收。

### 阶段 79：alpha.15 全套构建、原反馈场景复查与交付（2026-10-02）

- 更正阶段78对Celestial旧共享场地的归因：6步早停没有直接记录阻挡块／旁观者，不能断言一定是其他测试玩家；未据此修改生产ray。独立场地第一轮失败明确由测试加载用旁观者站在起点造成1步终止，源@a选择器包含旁观者。移除装载玩家、以region ticket保持加载，20步接触和150步最大序列断言不变。
- 最终命令 `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-sorcerer-repair-alpha15-final2-20261002"`：**248/248 required通过、BUILD SUCCESSFUL、退出0**，build/reobf成功。日志`sorcerer-repair-alpha15-final2-20261002.log`；初次final仍247/248的失败证据保留，未覆盖，未删除失败用例或放宽边界。
- 两个独立新世界客户端验证均退出0：六招`SORCERER_REPAIR_CAPTURE_COMPLETE`；原反馈场景`VISUAL_VARIANT_ASSERTIONS_PASS`／`VISUAL_REPAIR_CAPTURE_COMPLETE`。已逐张审阅冰锥270、双刃398、Halo85、ray100/483、Dash590、Air675；原反馈使者110/220/330、无弧410、巨剑490、portal585、头入方块身体780、死亡灵魂895、Air1215可见。手动帧/NoAI/creative夹具不算自然AI生存、多人与shader验收。
- `python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.15.jar`：**errors=[]**。1793模型／10rig／5607动画帧／4451直接particle cue／143beam helper行，3078 JAR entries；无GameTest、ClientSmoke、runtime mcfunction，所有class major61。审计前快照`native-audit-before-alpha15-20261002.json`，后快照`sorcerer-repair-audit-alpha15-20261002.json`。`git diff --check`退出0，仅项目已有LF/CRLF提示。
- 发布包：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.15.jar`，**56,026,248 bytes**，SHA-256 `7c9a89a5790962a7032ea32d8f82faace927306f4fad98cb50a7c964b037103d`。alpha.13=`1fd0e3caaac1cdcc3b7647bc9e93f92b88c0f3e4875af58024cfabfe124c604c`、alpha.14=`d1ac9901cfde0b4b19dda39cdfdd4e72cdc0fd0bccfba1704ca0c12b66e4c55f`均保持不变。新协议4，双方需同版；未自动安装、提交、推送或发布。
- 精确日志/源与实现SHA/旧包SHA见`docs/validation/sorcerer-repair-alpha15-20261002/manifest.json`，汇总及手工验收：[alpha.15报告](nyctocosm-native/docs/validation/SORCERER_REPAIR_ALPHA15.md)。旧alpha.14manifest保留，并为历史打包脚本加入版本保护，避免未来误写。
- 本次实施和自动验证完成，实机项待验收。D29/30/33命中附带迟缓、D26黑暗及其他D28取消伤害状态仍等待用户选择；D34/D45已选策略没有外推。Portal原用户shader/设置环境未提供，无法声称复现该组合；真实多人/卸载重启/生存/光影/FPS未验。巨蛇粒子4800→600/tick即87.5%数量下降，不冒充FPS实测。世界火焰、组队补偿、祭坛交易、镰刀成本及玩家D13–D23剩余项继续未完成；没有勾完全部51项。

### 阶段 80：用户要求最终重编译并关机（2026-10-02）

- 用户明确授权“完成后编译一版，编译完成后帮我关机”。为保留已交付alpha.15，本次生成alpha.16；未新增未批准的玩法选择。
- `gradlew.bat build --rerun-tasks --offline --console=plain`：**BUILD SUCCESSFUL，退出0，7项任务完整重跑，build/reobf成功**。日志`shutdown-build-alpha16-20261002-080804.log`；JAR审计errors=[]，旧审计快照与本次审计均保留。
- 新包`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.16.jar`，**56,026,248 bytes**／3078 entries／Java major61；SHA-256 `81a568b2acd673113e627372d8f87fea3a3c0230c501fc48517671359cc34239`。生产class集合／逐class字节与alpha.15一致，alpha.15 SHA未变。沿用248/248回归证据，不冒充本次再跑GameTest。
- [编译交付记录](nyctocosm-native/docs/validation/SHUTDOWN_BUILD_ALPHA16.md)及`SHUTDOWN_BUILD_ALPHA16_20261002-080804.json`已保存；最后按明确授权启动隐藏helper，确认构建成功／audit为空／JAR SHA匹配后，延迟20秒请求正常关机，启动与请求返回码分别写入validation日志。未自动安装／提交／推送。
- 阶段79的D28与其他未选玩法、真实生存/多人/光影/卸载/FPS验收仍待确认或验收；关机授权不替代玩法批准。


## 阶段 81 · 2026-10-03 · alpha.16 实机反馈复查与修复前反例

- 按本轮用户反馈优先处理 D06/D18/D25/D22 的关联切片；保留历史成果和待决策边界。地面近战是源 phase3，末期高速飞行是 phase4，纠正此前将用户“二阶段”仅对应代码 phase2 的解释。
- 原包 Sword Plunge 的 leaf0/10/45 分别 swordless/swordful/swordless；idle 不重新显示剑。phase3_transition 的 armorless 在171，近战阶段仍有护甲。当前渲染器按 phase>=2 强制 swordful、phase>=3 强制 armorless，是直接差异。
- 原包 block344 的 ItemDisplay 无 Rotation；旧原生错误借用了另一招 block335 的随机 yaw/pitch。原包飞行目标只在抵达5格或招式事件/特定碰撞时更新，旧原生另有20..59 tick 换路计时器。
- 隔离修复前回归：`nyctocosm-native/docs/validation/feedback-alpha17-red-20261003.log`，250项中3项按预期失败（巨剑倾斜、荧石不发光、飞行提前换路），Gradle退出非零；原有248项通过。测试世界 `run-feedback-alpha17-red-20261003`。
- 新增变体每骨骼持久状态、实际Boss关键帧/NBT、原版execute飞行角度、法术状态/粒子数据断言；初次编译夹具使用 protected setRot 导致失败，已改为公共yaw/pitch setter，失败日志保留。
- 持续核对17个法术的原包默认值和入口；用户概略列表不作为自动批准：本原包Reflectance20、Resistance250、Submergence选择3..7格最多6个。炽光术在Forge取消伤害后的燃烧已单独询问，未收到答复前不批准该争议策略。
- 下一步：完成焦点物品的独立灵魂爆散/法阵冲击波和MASTER音效，检查飞行攻击/玩家投射法术轨迹；完整回归和隔离客户端验收后构建alpha.17，记录包审计、哈希，再按用户要求关机。

## 阶段 82 · 2026-10-03 · 持久变体、源飞行路径与玩家法术修复

- 新增 `EmissaryAppearance`，读取已导入原包每骨骼 variant_keyframes，服务端在确切帧应用且跨 idle／原生 NBT 保留。Sword Plunge 10帧 swordful、45帧 swordless；源 phase3 地面近战 armorful，phase3_transition 171帧才 armorless 进入 phase4 高速飞行。客户端不再按阶段硬盖 swordful／armorless。巨剑创建和渲染移除错误随机 root Rotation，保留 HEAD×10／sky10 及既有下落事件。
- 飞行移除20..59 tick自动换路，按源25/.5和quickturn10/.5两次原版面对/局部坐标构造转向；速度N对应max(1,N−1)次.5移动。恢复全部玩家 waypoint 和任一5格到达判断、原Corkscrew/Grapple/Rain关键帧、takeoff5／land减速关键帧。保存quickturn、corkscrew score和其余waypoint；用户96回航/128边界继续保留。独立 vanilla execute 角度测试通过；不把单点夹具当多人自然飞行验收。
- phase4 Divine/Godrays 改成非旁观、非debug玩家 cube 截断、脚部4格冲击；NPC不挡射线、不被扩大命中。修正源Godrays创建即MASTER50/200音效和一次四路红色预警；恢复源27/3行ray图案、终端3行粒子和两种shockwave数值，三次Godray子步各发路径包。旧反射用例“反射应打伤Boss”与源@a冲突，保留容量/方向断言，明确改为不伤NPC施法者。
- 焦点物品恢复 source spellplane 三环1000数值行、21tick灵魂爆散、MASTER magic_plane/soul_release(1/10)；SoulEntity sculk_soul2 force(.2,.4,.2,.02)、瓶捕获dragonbreath MASTER1/10、线圈吸收MASTER1/10、镰刀reap MASTER4/10及水平前2/Y+1生成点。最近单人捕获、祭坛交易/退款、移动/固定阵位置及其它D20事务规则没有在本轮获准变更。
- 玩家Luminescence恢复Glowing6000／每tick清Darkness和Blindness，移除错误NightVision；Resistance恢复250tick Resistance I及仅本模组临时击退抗性modifier；Subsistence10治疗/饱食/Radiance/poof恢复。17招 damage/duration/cast_time 默认审计全部与本原包load一致，报告明确只有默认值范围。Reflectance20、Resistance250、Submergence3..7最多6等不按用户概略表擅改。
- ABH/FORB/GRAV发射为Y1.625+水平前3，INC/FUL为全俯仰前4。Abhorrence30次1.5移动、当前+后1格双solid阻挡、endpoint cube无额外splash；Fulminance六次1.5子步／源Dissonance命中，闪电视觉保留且世界火焰/真实Lightning伤害仍待C03；Gravitance当前voxel可过才移动、脚部6..9/.2后..6/.4顺序tp并保留immoveable保护。非默认power、多个引力owner交互与其余D22仍未完成。
- `feedback-alpha17-green4-20261003.log` **256/256**通过；新增飞行射线与玩家路径后green6 **259/260**，唯一旧Serpent自然游动索敌用例失败。诊断7 **260/260**通过，日志记录该夹具45tick实际距离已从120游到191；原失败没有距离证据，不能断言确切越界是唯一根因。最后仅把该夹具Boss位置固定、保留真实AI索敌/120格/墙，避免范围前提随机漂移。
- 客户端第一新世界完整退出0，日志确认持剑变体隐藏／地面phase3 armorful／phase4 armorless、瓶/线圈有数千可见粒子、Glowing=true；但该轮creative目标令Sword Plunge伤害调用链不执行，没有新落剑实体，不把它当真实落剑验收。最终夹具为该场景临时使用免伤Survival可飞目标，焦点场景清除前场景粒子，日志/截图保留两轮。不是用户存档，也不是自然AI生存/多人/光影验收。
- 最后补Luminescence独立世界位置20tick circle同步/保存和新endpoint载体的multipart翼部命中；正在执行全套262项+build以及最终客户端，成功结果和alpha.17包摘要另追加。协议5，两端需同版。未覆盖旧alpha.13–16或源数据包，无安装/提交/推送。
- 玩家炽光术取消伤害后是否燃烧已单独询问、尚无答复；未把D45主手Semblance确认外推，争议规则不批准、不闭合。其余C03、D20、D02组队/离线、D14 chance、D41及D28待选边界继续保留。

## 阶段 83 · 2026-10-03 · alpha.17 最终验证、编译交付与关机准备

- 最终 `gradlew.bat runGameTestServer build --offline --console=plain -PgameTestRunDir=run-feedback-alpha17-final-20261003`：**262/262 required通过，BUILD SUCCESSFUL，退出0，3m10s**；独立世界和完整日志`feedback-alpha17-final-20261003.log`保留。最后`gradlew.bat build --offline --console=plain`亦退出0，10s，记录`feedback-alpha17-release-build-20261003.log`，不是拿历史248项充当新结果。
- 三个新建客户端均正常退出0；最终两轮1380独立剑数1且hand swordless，1440 phase3 armorful、1480 phase4 armorless，瓶/线圈演出与Glowing=true。补测两种射线路径包：1717四路红色预警、1735扫射、1880冲击可见；新源数值粒子在夹具可达约3.3万活跃量，未测真实FPS，不宣称所有渲染组合无掉帧。
- 逐图复查最终场景：490竖直巨剑、780蛇头埋石身体仍见、585门粒子、895巫师死亡灵魂、1440/1480材质、1510/1570焦点、1650亮光、1717/1735/1880射线。手动NoAI/免伤/关键帧夹具不当作自然生存/多人/原包整场视频对照。client-final/ray-client场景日志与截图完整保留。
- 新包 `D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.17.jar`，**56,065,908 bytes／3085 entries／Java class major61**，SHA-256 **`84c4f1ce6a1f862665ab49cdf56c03cd7f10deda0a3a2845adae410fc9c271f5`**。新增3373数值行效果资产，封入资源与工作区字节一致；JAR audit errors=[]，无测试/烟测类和runtime mcfunction。旧alpha.13/14/15/16逐包SHA均未变。
- [完整交付报告](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA17.md)逐招列出17法术原包默认值、已修切片和剩余差异、失败记录、源/原生/日志SHA及实机步骤；manifest在`docs/validation/feedback-alpha17-20261003/manifest.json`。差异清单/方案/实施补充/README已更新；未勾完51项。Git diff --check（cr-at-eol）退出0，无commit/push/安装。
- 炽光取消伤害燃烧、其独立三子步/源命中载体、其它D28与C03等仍待确认；D22动态属性及其它逐招细节继续未闭合。真实自然AI、生存、多人、shader、保护模组、chunk卸载/真实专服重启/晚加入/FPS仍待验收。只读源、旧JAR、真实存档/安装mods均保留。
- 用户本轮明确要求编译后关闭计算机；全部构建/测试/客户端进程已正常结束。交付和TODO保存后由本轮新隐藏helper验证manifest／包SHA／审计／成功日志，再延迟20秒请求普通关机，不用/f；helper启动与请求返回码另记validation。未将关机授权当作玩法批准。
- 前两次关机helper因独立PowerShell哈希命令/脚本编码问题中止，均未发出关机请求；改用.NET SHA256与纯ASCII脚本后隐藏重试，PID 3264。已复验alpha.17包SHA/262测试/构建/三客户端日志与已保存TODO，进入20秒正常关机等待；各次启动凭据、执行日志与receipt保留在 `nyctocosm-native/docs/validation/feedback-alpha17-20261003/`。


## 阶段 84 · 2026-10-03 · 三 Boss 全招行动/粒子只读对比

- 按用户最新“先不改代码只对比”要求，覆盖原包 Serpent/Sorcerer/Emissary 三个命名空间全部 1,602 个函数、相关动画命令关键帧和 effects 引用闭包；逐项比较 3+12+22=37 种攻击及索敌/移动/连招/开场/持续特效/转阶段/死亡。报告：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\docs\validation\BOSS_ACTION_EFFECT_COMPARISON_20261003.md`。
- 差异重点：使者追踪飞弹生成即飞而源23帧统一激活、完整死亡/转阶段/持续翅手脚光环演出缺失、Wing Blast/Celestial Smite 等使用近似纹样；巫师地面远程距离/导航不同、死亡133而源180、陨石裂口展开与Void Wave冲击波不完整；巨蛇出生中心/首次等待、红须/尾雾/转圈脉络/死亡运动与灵魂特效不同。
- 当前alpha.17已存在的持久剑隐藏/竖直巨剑/phase3保甲171帧换甲、巨蛇身体分段渲染、AirDash/Frozen/独立Halo、God/Divine射线等分别标出，未复用早期版本差异。核对源调用顺序，VoidMeteor头顶12格和双计时器六次发射、使者出生七类粒子数量相符；初次登场330转rise、rise127才法阵，两边对应，排除中途误判。
- 证据JSON保存6,404个源文件记录及78个Java文件、行号/SHA/命令/效果引用，未解析引用=[]；原始行数/调用图不是运行计数，未把源条件通常无法命中的FlySpeed @a分支当作必达行为。记录源码确认项与自然AI/多人/晚加入/chunk/FPS实机待验收的区别。
- 96/128飞行限制与巨蛇600/tick预算作为既有授权调整；D34已确认迟缓不外推D28。C03、其它D28、D20等未获批准规则保持未闭合；本报告不构成玩法批准。
- 比较前后SHA集合检查全部一致：原包29,657文件、生产代码/资源+build.gradle 2,695文件、tools21文件；alpha.17 JAR仍56,065,908 bytes，SHA256 `84c4f1ce6a1f862665ab49cdf56c03cd7f10deda0a3a2845adae410fc9c271f5`。记录在`docs/validation/boss-comparison-20261003/immutability.json`。
- 本轮仅新增对比文档/证据并追加TODO；没有玩法代码修改、测试执行、游戏启动、构建、安装、commit/push或关机。历史262测试/客户端截图不作为本轮完整还原证据；完整原包/mod并排实机录像尚未进行。


## 阶段 85 · 2026-10-03 · boss 对照后高影响切片实施与修复前反例

- 用户最新授权从完整对照报告中选择容易修复且影响战斗体验的部分；本轮优先使者七枚飞弹23帧统一激活、巫师地面7/8/20格选招门槛与固定战斗、180tick完整死亡，再补使者转阶段/落地砸击/死亡粒子、巫师法阵冲击波及杖粒子、巨蛇尾雾。未把本轮切片当37招全部闭合；既有96/128飞行范围、蛇柱600共享预算保留。
- 修复前基线：docs/validation/boss-repair-alpha18-20261003/before.json，2695生产文件、原包29657文件和11个旧JAR摘要保留。新测试首轮因模板前缀错误失败，red.log保留；修正测试模板标注后red2.log中265项运行，原262项通过，3个新反例按预期失败（提前移动、7–8格仍选招、180前移除），Gradle退出1。没有删除或放宽失败用例。
- 新增独立EmissarySeekingMissile服务端载体，悬停/激活/随机初摆/最近玩家追踪/双子步.7/端点cube/反射/NBT；选中原包粒子与声音由export_combat_repair_effects.py编译为数值资源，客户端独立短生命周期表现，服务端持有事件状态。补翼尖3节点的原动画locator导出。当前为代码完成待验证，不使用历史262/262作为本轮结果。
- 未自动批准D28/C03取消伤害附加效果和世界火焰、D20事务、D02组队离线、D14 chance、D41成本等；不改只读原包、不覆盖旧测试世界、不安装mods、不提交推送。本轮最新请求没有关机动作。
- 下一步：补飞弹激活/追踪/碰撞/反射/NBT和死亡恢复回归，运行全套GameTest、隔离客户端演出检查并构建不同版本alpha.18，追加真实结果与未实测边界。


## 阶段 86 · 2026-10-03 · alpha.18 高影响战斗修复回归完成

- 最终隔离 `gradlew.bat runGameTestServer build --offline --console=plain -PgameTestRunDir=run-boss-repair18-final-20261003`：**275/275 required通过，BUILD SUCCESSFUL，退出0，3m57s**；完整日志`docs/validation/boss-repair-alpha18-20261003/final.log`。本轮新增13项，修正旧巫师死亡和参与者奖励用例至源180tick；包含真AI入口选招/导航、23激活、独立vanilla命令转向、双.7子步、端点无额外splash、超时、反射容量、原生NBT、所属清理/卸载保护、数值粒子及声音表。原用例继续保留。
- green1为274项中3项失败：局部坐标角度精度、反射夹具玩家碰箱、旧奖励时长；修正局部移动为vanilla LocalCoordinates等价计算，反射玩家移出端点箱。green2有2个飞弹用例失败，复查夹具处于同一区域且源最近玩家范围300，改为1024格间隔的独立坐标并排除removed玩家；green3 **274/274**通过。失败和编译日志全部保留，不当最终结果。
- 实施复查发现对照报告地面侧移结论错误：动画10–14调用不存在的dash_skid，实际文件为未被动画调用的dash_slow；此前拟加入的.3尾段撤回，保持源可达0–9移动，新增左右侧移回归并更正原对照表。没有为符合旧方案而补不可达分支。
- 导入选中原动画53行转阶段粒子、2行落地砸击、15个转阶段声音；helper3156数值行覆盖悬停飞弹44、十字82、最终爆散900、法阵1000、Voidwave1130。独立30tick死亡旋涡随服务器事件播出；翼尖3原包locator导出纳入18节点/44动画/2601帧。Staff恢复每tick20血阈值、Y+2/size1及召回墨汁；蛇头/尾五点黑雾恢复，柱状光束600共享预算与160格可见门槛保留。
- 静态审计static-audit2.log错误列表为空；审计范围扩展新增生产类/资源，测试命令类仍只在开发源，发布JAR排除全部NativeGameTests*/ClientSmoke*。协议6，两端需同版。客户端隔离世界run-boss-repair18-client-20261003正在运行，截图、最终包摘要与限制另追加。没有安装、关机或提交。


## 阶段 87 · 2026-10-03 · alpha.18 最终客户端、反制集成和开发包交付

- 交付前补查专用飞弹的原spell/projectile标签，发现必须保留禁锢术入口；SpellBolt.tickCounter接入EmissarySeekingMissile.damageOwnerId，第三tick才消除，反射后同owner不误消除。新增第14项回归；最终 `gradlew.bat runGameTestServer build --offline --console=plain -PgameTestRunDir=run-boss-repair18-counter-final-20261003`：**276/276 required通过，BUILD SUCCESSFUL，退出0，3m51s**，final-counter.log。此前275项记录保持历史，不充作最终结果。
- 三个隔离客户端世界均正常退出0；扩展第二轮虽结束但夹具在招式完成后继续反射调用perform导致attack=null Server task错误，该轮不能当无错误验收。修正仅驱动仍活动的原招式后，第三轮client-verified.log **无Server task/渲染异常，退出0，1m53s，37张截图/9场景**。逐图查悬停七节点、激活尾迹、转阶段、双手十字/死亡爆散、Voidwave、巫师138/179年龄保留、蛇头尾雾、voidplane和杖召回墨汁。首轮/失败轮完整输出和截图保留。
- 第三轮510/520 tick Boss数0仍有12568/12945粒子；死亡粒子独立延续。138/179年龄日志removed=false, clip=die。源最终爆散约1.3万活跃粒子仅为夹具观察，不是FPS或自然多人验收。源37招仍未全部还原；使者首转阶段电火花辅助、最终爆炸网格、常态翼火/phase4眼手脚halo及剩余技能、巫师共同元素FX/D37、蛇其余出生死亡音效等继续未闭合。
- 客户端演出检查在禁锢术入口补充前执行；随后最新276项回归与最终包审计验证反制，渲染资产未改。真实自然生存、多人、保护模组、shader、晚加入、真实专服重启/区块卸载/FPS仍待实机，NBT/卸载原因测试不等同这些集成验收。
- 最终包 `D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.18.jar`：**56,163,014 bytes／3091 entries／Java17 major61**，SHA-256 **`060b9cd423500d2bddbb0175e5a98f0fbe4f65f02742238c7b7f7b019bdb137e`**。release-audit.log与jar-audit.json errors=[]；新增资源字节一致，无测试/烟测类或执行式mcfunction。原包29657文件 aggregate e75767ed38c2b83cacefb7b28db72c315071f7657db12dfcee512dd6813f3c18及11个旧JAR逐包摘要保持不变。manifest.json记录修改/新增/夹具/日志/截图摘要，无基线文件删除。
- 报告BOSS_COMBAT_REPAIR_ALPHA18.md、差异清单D18/D25/D38、方案/实施补充和README已更新；D18/D25分项实施中，D38新切片自动通过待实机，51项未整体勾选。源侧移悬空dash_skid纠错保留。协议6，两端需同版。git diff --check(cr-at-eol)退出0；无安装、提交、推送、关机，测试/客户端进程已正常结束。
- D28/C03附加状态和世界火焰、D20事务、D02组队/离线、D14 chance、D41成本等仍待用户选择，未外推D34/D45确认。可直接进入报告的四步实机验收，再按完整对照选择下一批实际差异。


## 阶段 88 · 2026-10-04 · alpha.19 三 Boss 反馈定位与修正

- 本轮按用户最新三条反馈实施；已确认“使者一阶段”是出场后的悬浮形态。基线before.json保留2703生产文件、29657只读源文件和12个旧JAR哈希。未修改原包、真实存档或mods，没有提交/推送。本轮不执行关机。
- 修复前新5项加入全套281项，red.log中4个新反例失败：巫师击退仍有.2上抬/.141侧移、飞行超过60仍选择降落、FlyTimer=1不更新、头部网络位置误差平移整条蛇身。原276项及新增全idle关键帧矩阵检查通过；Gradle退出1。
- 重新追踪phase4 choose/661/conditional8、ai/subtick/736/737，确认原包超过60格保持飞行、未飞则起飞；巡航目标20..59tick定时更新可达。此前alpha.17将该计时器误删，旧报告/测试结论应更正；@a[tag=emissary,tag=flytarget]自动加速分支仍与此不同，没有擅自补不可达加速。已恢复计时器、无选招时AttackTimer0..29、全部玩家标记依创建顺序转向，保留96回航/128范围。
- 巫师注册击退抗性1，kind1 knockback屏蔽覆盖旧原生NBT中的.9，实际伤害继续经过Forge。按用户要求仅在initial_spawn隐藏smear1（双面开场弧片），不移除法杖和战斗挥杖特效。
- 巨蛇服务端41节追逐链经旧用例17220采样仍与源逐命令相符；修客户端头/身体双时钟误差：头部位置和角度、全部身体坐标使用同一BODY快照，移除整条身体随滞后实体头部坐标平移。20部件受伤时序、路径步长/门槛、600粒子共享预算保留，不用新历史路径替换原包追逐算法。
- 待机排查保留多轮探针和失败/临时方向实验：模型/变体/HEAD变换正确，Minecraft Transformation.EXTENDED_CODEC解码与原矩阵相符；source ItemDisplay夹具与原phase1_idle对照留存。简单删掉item半周翻转会产生分离翅膀和垂手，已撤回。最终按用户明确抱臂护身要求，用原spawn75抱臂经原idle torso对齐，固定原idle0护身翼位；身体/头腿及轻微呼吸保持原idle采样，新增独立phase1_idle_guarded，仅客户端idle采样选择，原clip保留。此为用户要求的表现调整，不谎称源原phase1_idle默认全部就是此姿势。
- 第一轮green1：285项中284通过、1个旧“timer不应换路”用例失败，green1.log保留。修正旧用例为timer2→1保持目标、再0按源重选20..59，没有删除失败；新增源抱臂/护翼/尺寸、不选招计时，最终287项及build/隔离客户端正在验证，真实结果另追加。协议6。
- NativeGameTestsFeedback19/ClientSmokeFeedback19均沿现有发布排除规则；source-display/guard多姿态烟测只用于独立世界。临时feedback_guard_*资产已删除。其余D28/C03、D20、D02组队离线、D14chance、D41仍未获批准，不自动闭合。


## 阶段 89 · 2026-10-04 · 连续飞行复查发现源位移与重力偏差

- 第一版287/287和build通过，final.log留存；client-final.log正常结束，但静态镜头未跟住使者，不能当连续飞行验收。补跟随镜头的client-follow.log正常结束，2m32s；730–2299由真实AI选招，免伤飞行玩家仍可被抓取，2300另设玩家上方30格的手动Rain演示，两种证据分开记录。
- 连续日志暴露起飞/降落遗漏：源takeoff leaf1..5各上移.80625，land leaf0..4各下移.80625，减速只在0/3/6/8/9/10/11；原生省略根位移、在7多减速，而且phase4非Flying用了普通重力，抹掉Godrays升高并在降落时持续坠落。source root是marker，entitytick/block10没有相应自由落体，不能只看动画名字。
- 新增第288项修复前反例；flight-red.log中根实体上升遗漏按预期失败，另有旧Shard Hail隔离场地entity-ticking加载超时，288中286通过、2失败，Gradle退出1，3m25s。保留两项失败；不据此改变Hail取消/命中语义。
- 已补源起飞/降落位移及精确减速帧，phase4活体保持无重力；降落额外保留地表夹紧防埋地安全边界。旧takeoff用例改为先核对5*.80625升高，再核对14帧才启动巡航，而非错误断言14前root完全不动。继续全套288回归、同版跟随客户端和包审计，最终结果另追加。
- Rain仍保留玩家上方30格航点、10..44每帧四枚弹体及源曲线；没有擅加源码不存在的最低高度门槛。全部37招、多人/自然生存/FPS仍待验收，其余玩法待决策不变。


## 阶段 90 · 2026-10-04 · alpha.19 最终回归、连续客户端和开发包交付

- 最终隔离 `gradlew.bat runGameTestServer build --offline --console=plain -PgameTestRunDir=run-feedback19-flight-final2-20261004`：**288/288 required通过，BUILD SUCCESSFUL，退出0，3m22s**，flight-final2.log。新增12项覆盖玩家冲刺/击退II伤害、开场smear可见性、源护身姿态/尺寸、body双时钟/朝向/NBT、多航点vanilla转向、60格自然AI门槛与无招计时、FlyTimer及起落位移/减速/无重力。原17220身体命令采样maxError=0，回归没有删除用例。
- flight-final.log为287/288：新空中下降夹具基点Y60低于实际world_surface63，地表防埋夹紧触发；改为实际地表上方40格，再单独核对地表夹紧。最新日志surface63/start103/actual98.96874999999997/expected98.96875，位移误差门槛不放宽，source减速7不执行及最终speed3均通过。flight-red里的旧Shard Hail加载超时在最终全套正常通过，未改变取消/命中规则。
- 最新隔离客户端 `run-feedback19-flight-fixed-client-20261004`：client-flight-fixed.log **退出0，BUILD SUCCESSFUL 2m34s，无Server task/渲染异常，42张截图**。核查65/90/115护身待机、390/394法杖仍见且开场弧片消失、650/700急转身体曲线。730–2299真实AI选招（免伤/可飞玩家、独立跟随镜头），三次自然Rain的105个发射帧root高于玩家18.7698..21.1683格，1904等可见高空曲线；2300以后35帧另属手动高空场景，不冒充自然选招。旧静态镜头/方向探针/姿态试验/失败记录均保留。
- 最终包 `D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.19.jar`：**56,178,052 bytes／3092 entries／Java17 major61**，SHA-256 **`2e8e7c7928dc4e6f04e39083ba39397cf97e81e14b6652c0720774d56159506b`**。release-audit.log及jar-audit.json errors=[]；无NativeGameTests*/ClientSmoke*与runtime mcfunction，guarded rig和既有粒子资源字节一致，临时feedback_guard_*资产与开发开关清理。协议6，两端需同版。
- manifest.json确认只读原包**29657文件 aggregate e75767ed38c2b83cacefb7b28db72c315071f7657db12dfcee512dd6813f3c18**及12个旧JAR逐包SHA均未变；本轮无基线生产文件删除。源飞行可达代码/动画根命令逐行SHA另存source-flight-evidence.json；guarded-idle-import.json记录源spawn75/idle0，明确用户要求的表现调整。生产/夹具/日志/42截图及body trace证据留存。
- [alpha.19完整报告](nyctocosm-native/docs/validation/FEEDBACK_REPAIR_ALPHA19.md)、差异清单D18/D27/D38、方案/实施补充/覆盖审计/README更新；原对照“FlyTimer不可达/仅事件换路”结论更正，TODO阶段88/89保留历史。Git diff --check(cr-at-eol)退出0。构建、测试、客户端进程均正常结束；没有安装、提交、推送或关机。
- **实现和自动验证完成，实机待验收**：真实自然生存/多人、原包/mod并排完整战斗、保护模组/shader、专服重启/卸载/晚加入/FPS未测；37招和D18/D25其它切片仍未全部闭合。D28/C03、D20、D02组队离线、D14chance、D41待选玩法不自动批准。下一步按报告四条实机步骤检查，再按差异清单继续剩余项。


## 阶段 91 · 2026-10-04 · 用户要求重新编译并关机

- 用户明确授权“完成后编译一版并关机”；在完成alpha.19三Boss切片后重新强制执行build/reobf，版本区分为alpha.20，保留所有旧包。`gradlew.bat build --offline --console=plain --rerun-tasks`：**BUILD SUCCESSFUL，退出0，27s，7任务执行**。
- 新包 `D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.20.jar`，**56,178,050 bytes／3092 entries／Java17 major61**；SHA-256 **`28656ccfc2c9d87067b0e9f27647132b5db80bc760886e2327d88b316e5022d7`**。audit errors=[]，所有生产class/资源逐字节与alpha.19一致，仅META-INF/mods.toml版本不同；13个旧JAR摘要未变，生产源/资源未改。
- 本轮只编译及包审计，沿用上一轮288/288和42截图证据，不宣称重跑GameTest或全部玩法验收。待选规则、真实生存/多人/shader/重启卸载/FPS限制继续保留。
- 交付记录SHUTDOWN_BUILD_ALPHA20.md与 `nyctocosm-native/docs/validation/shutdown-alpha20-20261004-044310/manifest.json`已保存；构建/审计与前后SHA、关机helper预检/启动和请求日志保留在同一目录。保存完毕后，隐藏helper核对包SHA/构建成功/audit为空/既有回归SHA/TODO91，等待20秒请求正常关机，不使用/f；实际状态由shutdown-receipt.json更新。未安装、提交或推送。


## 阶段 92 · 2026-10-17 · GitHub私有仓库备份

- 按用户明确要求新建私有仓库 https://github.com/EchoAria/nyctocosm-native，main分支；初始上传提交757f6f33d9d88474f5740f4c0e9991d8be911a07已由GitHub API核对，private=true。源码、资源、Gradle脚本、工具、测试源码和验证/修复文档一并上传。
- 根目录六份交接/方案/TODO文档复制到模组仓库docs/gameplay；原始文档继续保留。扩展.gitignore排除run*/logs/构建JAR/图像视频输出和本地凭据文件，保留Gradle wrapper JAR。候选文件3042个，约79.8MiB，排除约26GB运行产物。只读原数据包、源ZIP、旁边旧MDK和用户存档不属于本仓库上传范围。
- GitHub设备流程由用户完成授权；令牌存于Windows凭据管理器，未写入项目或聊天。未改变玩法/重新编译/执行测试/关机，alpha.20产物及上一轮288/288证据保留。
