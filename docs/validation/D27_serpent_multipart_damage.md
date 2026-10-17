# D27：Serpent 躯干部件与投射物/范围命中

日期：2026-09-30。状态：**代码完成，自动验证通过，待客户端实机验收。**

## 源行为与实现

- 原包 `datapack/data/serpent/functions/summon.mcfunction` 在身体偶数段生成 20 个 `Size:8` Slime 受击体；其实体类型属于原包 `livingthings` / `mobs`。Size 数字不等于方块宽度。
- Minecraft 1.20.1 Forge 47.4.13 的 Slime 尺寸依实体基础尺寸和 `0.255 × size` 缩放。GameTest 直接生成 Size:8 原版 Slime 作为夹具，实测尺寸 **4.1615996×4.1615996**；Serpent 20 个 `SerpentPart` 逐轴与该夹具比较，最大差为 **0**。此前原生 `3×3` 部件与源实体不一致，已改为使用同一原版维度公式。
- `NativeBoss` 对 Serpent 和 Emissary 暴露 multipart 部件，DamageTarget 将部件命中解析到其父实体。`Combat.area` / `Combat.line` 在候选 LivingEntity 之外检查 multipart 部件的碰撞盒，并以父实体集合去重，因此同一范围/线段命中多段时只结算一次伤害。
- `SpellBolt` 通过 DamageTarget 接受部件命中；当命中点在爆炸半径外，仍对直接命中的父实体结算一次伤害。直接目标也纳入 FIRE 的附加着火集合。

## 自动验证

新增 `serpent_multipart` GameTest：

- `serpent_body_parts_match_vanilla_size_eight_slime`：20 部件存在，Size:8 原版实体与代理宽/高相等；一并核验 D01 Size:4 Emissary 代理尺寸为 **2.0808×2.0808**。
- `serpent_body_parts_receive_area_and_line_damage_once`：把远端部件移至隔离夹具的空旷位置，分别用 AoE 和线段攻击，父 Boss 扣血严格等于一次指定伤害。
- `serpent_body_part_accepts_direct_spellbolt_damage`：先确认 Forge 实体查询返回远端部件，再由原版箭和 SpellBolt 分别命中并扣除父 Boss 生命值。箭测试起初停在方块内；将夹具移至空气区块并断言起点为空气后通过，未据此推断生产投射物碰撞失败。

完整命令：

```powershell
.\gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d27-verified6-20260930"
```

结果：**131/131 required GameTest 通过，BUILD SUCCESSFUL**。整套测试还包含 D01、D02、D04 回归；Serpent 420 tick × 41 段的源运动对照共 17,220 个位置，最大误差为 **0**。

`python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`：**errors=0**；3011 个 JAR 条目、1778 模型、10 个骨骼 rig / 5607 动画帧、1 个 locator bundle / 2601 帧、247 音效事件、2 个原生结构事件，未打包运行时 mcfunction、GameTest 或客户端烟测类。JAR 为 **55,375,927 字节**，SHA-256：`c03ca13facd558963e4fae06aa3f6e65bdd9f305c1420e5e96884d2b0fea10ad`。

## 尚未验收

GameTest 验证服务端命中/转发与去重，不替代客户端视觉或真实生存战斗。仍需在客户端确认远端身体部件与显示链同步，以及近战、Wyrmsliver、Nullfissure 射线和多人区块边界下的实际命中感受。Serpent 索敌/离场门槛另列 D38；generic 元素类型另列 D47，不以 D27 自动通过代表这些差异已修复。
