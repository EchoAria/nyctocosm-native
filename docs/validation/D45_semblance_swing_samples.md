# D45：Semblance of Doom 主手横斩/纵斩命中面

日期：2026-10-01

状态：自动验证通过·待实机。主手横斩/纵斩的源采样面、目标去重与伤害取消后的燃烧顺序已完成 GameTest；真实多人客户端表现仍待验。

## 原包调用链

- `items:semblanceofdoom/swing` 按 `OnGround` 选择生成块62或63，分别把局部采样原点放在玩家脚部上方 1.5 或 0.9 格；声音、命中标记清理和 30 tick 主手冷却保持原顺序。
- 两个生成块各执行 72 个半径 2 样点，角度为 `i*5`（i=0..71），局部圆周坐标为 `(2 sin(angle), 2 cos(angle))`。落地分支把第一个坐标放在局部 left，空中分支放在局部 up，第二个坐标始终为 local forward。
- 每个样点把 1×1×1 立方体定位到 `center - (0.5,0.5,0.5)`，选择 `#nyctocosm:livingthings` 并标记 `doomswinghit`。执行 block64 时先移除命中标记、尝试 Dissonance 伤害，之后无条件写入 `Fire:100s`；因此每个目标只结算一次，伤害被取消仍燃烧 5 秒。

## 实现

- `WeaponItem.sweep` 仅在 Dissonance / Semblance 主手分支转到 `semblanceSweep`；Nullfissure 的既有锥形命中、武器冷却入口、19 Dissonance 与普通左键属性不变。
- `semblanceSweep` 按玩家 `onGround` 选择 Y+1.5 / Y+0.9 原点，复用源圆周的 72 个 `i*5` 样点。grounded 使用 yaw 定义的 local left，空中使用与 Minecraft local coordinates 一致的 yaw/pitch local up；两者都沿 `getLookAngle()` 的 local forward 轴。
- 每个样点建立以中心为中心、边长 1 的 AABB；合并候选后按 UUID 去重，再逐目标调用一次 `Combat.hit`。伤害返回值不控制燃烧：每个源样点选择到的 LivingEntity 都在伤害尝试后 `setSecondsOnFire(5)`，符合已确认的 D45 决定。排除 ArmorStand，与源 `mobs + player` livingthings 标签范围一致。
- 为保持原包 `#nyctocosm:livingthings` 的选择范围，导入源 `mobs` 类型表及 `livingthings`（`#nyctocosm:mobs` + `minecraft:player`）标签；运行时按实体类型标签过滤，避免误选其他模组未入源标签的 LivingEntity。
- `WeaponEffects.sweep(kind=3)` 已画出相同的 72 点火焰/烟/烟花轮廓，因此验收使用现有源轮廓，不新增仅调试用运行时特效。

## 验证

- 新增两项 `semblance_swing_samples` GameTest。地面测试：yaw 90° 的横向圆周后侧目标可命中、前方 3.5 格环外目标不命中、invulnerable 命中目标不掉血但仍燃烧、Armor Stand 被原包实体标签排除。
- 空中测试：yaw 45°/pitch 30° 的旋转 local-up 样点命中，即使目标拒绝伤害仍燃烧；另以 local-left 纯法向目标验证空中样本面没有误用地面 left/forward 圆面。
- 修复前完整 199 项 GameTest 中两项 D45 反例均按预期失败。导入源类型标签后的最终 `run-d45-source-tag-verified-20261001` 完成 199 项，193 项通过；D45 两项（含 Armor Stand 排除）全绿。6 项未通过为两个既有 Wing Blast、`serpent_beam_selects_late_target_after_reload`、两个既有 Flame Geysers、`serpent_beam_retargets_with_owner_and_body_hitboxes_lag_one_step`，均未调用 D45 代码。日志：[GameTest latest.log](../../run-d45-source-tag-verified-20261001/logs/latest.log)。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。alpha.12 审计 errors=0、3,029 JAR entries、0 runtime mcfunction；无 `NativeGameTests`/`ClientSmoke` 类。审计快照：[before](../native-audit-before-d45-20261001.json)、[after](../native-audit-after-d45-20261001.json)。

## 摘要与待验

| 文件 | SHA-256 |
|---|---|
| 原包 `semblanceofdoom/swing.mcfunction` | `7D4DFC4063873180AD738C3E295E01C270ADFC775A15AC45481C56DC30B68739` |
| 原包 `semblanceofdoom/swing_hitbox.mcfunction` | `F6CFB252470E89B0C85A53602705D3A5E73E199FF9F027A834767E89345D398F` |
| 原包生成块62 | `8F8E6E35BFB1A54B0D2A57518FB15CBA5139AA73821FDC7471C477280FECCBFE` |
| 原包生成块63 | `523753A84DC83B1102463CFF24EB00E2F51C87E07B0C6E0930033C3B7BEF64E1` |
| 原包生成块64 | `91B369DB0F4DBAF02731BA8BC4B1C8FED44C6966410D9DE90C2EEB4F322BDA85` |
| `WeaponItem.java` | `36C22A94BCB0FCD2EBF79CB5C9BD946C0F1DE6BCCA87D932AF9B4630A13CBA37` |
| `NativeGameTests.java` | `F09B3D4C29D5A8370C3E02C02744C7CEBCBA2967F1D828F135FA4D89C9A58A99` |
| Source `mobs.json` | `6FBA93ED7B13DAC40DA2C21DD83F1C7EBD3F2DAAC375A0510F8F02058AD582B3` |
| Native `mobs.json` | `FFDDBE61F215C66B332950E38B3FAF4B9F49A313493849D656D80AF3B84B37FE` |
| Native `livingthings.json` | `3206A0EF32E43AC2645250E55367DA16D48148ACACF3C89C23687F99D7F87837` |
| alpha.12 JAR | `BD6271B24D07EB2975225D556DAA8ED1395A22A32E4557277CC08BD57BA1A35B` |

构建前 alpha.12（`21484D84B2EEF74651E018E2CD945838DC1ABA8D5E770028019D9B47B127D3D2`）已备份到 `run-d45-source-tag-verified-20261001/pre-build-nyctocosm-native-0.3.0-native-alpha.12.jar`。客户端音画、多人保护插件对伤害取消后的火焰行为、实际多人近身挥斩仍待实机；未修改源包/真实存档/邻接 MDK/安装目录，不升 alpha 版本、无提交或分发。
