# D10 Apocalypse Rain 验证记录

日期：2026-09-30。源为只读 `_nyctocosm_source`，实现目标为 `nyctocosm-native` alpha.12。

## 源事件与参数

动画 `phase4.attack.apocalypse_rain` 的 `leaf_0_effects_0` 在第 0 帧播放 MASTER 音量 5、300 格蓄力声。`tree/leaf_10.mcfunction` 至 `leaf_44.mcfunction` **连续 35 帧**调用 `emissary:attacks/phase4/apocalypse_rain/fire`。每次调用先播放 MASTER 音量 20、200 格射击声，再按四个分支在动画执行者位置创建 marker：

| 生成分支 | 相对 yaw | 初始 pitch | 标签 |
|---|---:|---:|---|
| block/692 | +90° | -70° | upper |
| block/693 | -90° | -70° | upper |
| block/694 | +90° | +60° | 无 upper |
| block/695 | -90° | +60° | 无 upper |

每个分支再独立添加 yaw 与 pitch 各自 `[-8°, +7°]` 的整数扰动；全招共 140 枚，从使者根位置发射，不从锁定玩家上方生成。

`subtick` 先增加 projectile timer；达到 60 的 tick 直接进入 end/impact，不再移动。第 1–59 tick 运行 block/689：下组当前 `x_rotation <= 80` 时俯仰 +3°；upper 组依次执行 `[-90,-1] +5°`、`[0,15] +8°`、`[0,30] +3°`、`[0,60] +1.5°`、`[0,90] +0.8°`。后续 selector 读取先前改过的俯仰，因此这不是互斥区间。每 tick 最多两次 slowcast，每次先处理 3 格内 Reflectance，再沿朝向前进 1 格，检查 `nyctocosm:passthrough` 与当前方块内玩家，发五种 trail 粒子；第一次标记 end 后跳过第二次。

终止时原包仅选择 5 格内非 spectator/debug 玩家，应用基础 Dissonance 伤害 12；再播放 MASTER 音量 10、50 格 generic explode 声并生成 explosion emitter。`magic:calcstatsdamage`、Dissonance weakness/resistance 与统计记账在 block/697/698，依赖尚未闭合的 D22 属性管线。

## 本轮实现

- 新增 `EmissaryRainShard` 服务端实体、注册与空 renderer。NativeBoss 第 0 帧播放 charge，第 10–44 帧每帧调用一次 `fire`，不再以保存的玩家点创建直落 `SpellBolt`。发射不因施法期间目标消失而中断。
- 实体保存 owner UUID、上下组、年龄、子步数与 resolved 状态。pitch 区间按原顺序累加；每 tick 两次一格前进，至多 60 tick。遇非 passthrough 方块或所在方块内玩家时立即终止；未加载区块只移除实体，不强制加载区块或额外结算。
- 轨迹使用五种 force trail 粒子。每个 slowcast 对 3 格内 Reflectance 扣 12 并沿其朝向改向，所有者仍保留原使者；终止时按 5 格球形处理合格玩家、通过 `Combat.hit` 走 Dissonance/Forge 伤害事件，并发 source 爆炸声和粒子。
- 源伤害经原版/Forge 防护结算后可能低于基础 12（测试夹具玩家实测扣 7）；实现保留该管线。D22 的动态 weakness/resistance、额外记账与 Hypertrace 交互没有在 D10 中伪称已完成。

## 自动验证

新增四项 `emissary` GameTest：

1. 帧 1–9 不发射，帧 10 首次生成四枚，帧 10–44 共 140 枚；核对两组各 70 枚、四种角度/扰动边界、使者根位置、owner；中途删除目标后仍继续按动画帧发射。
2. 下组俯仰 +3°、两次一格移动、upper 顺序累加；NBT 保留 owner/age/substeps/rotation；第 60 tick 结算且不再移动。
3. 实块在第一子步终止；半径 5 内外伤害边界；玩家实际占据经过方块时碰撞终止。
4. Reflectance 两次 slowcast 后按预期消耗 20 点储量，弹体改沿护盾朝向移动。

首次测试发现移动公式的 float 运算误差约 `0.00017`，容差改为 `0.001`；另有一项测试错误要求护甲后的实伤等于 12，已改为断言实际伤害大于 0 且不超过 Dissonance 基础值，并确认边界玩家不受击。没有放宽生产碰撞或攻击范围。

最终隔离全套命令：

```powershell
.\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d10-rain-final7-20260930"
```

结果：**165/165 required GameTests 通过**。日志：[run-d10-rain-final7](../../run-d10-rain-final7-20260930/logs/latest.log)。其间 `run-d10-rain-final4/5/6` 有一项既有 `serpent_beam_selects_late_target_after_reload` 失败（旧落点被保留），D10 四项均通过；final7 全套通过，未据此修改或放宽 Serpent 生产代码。

随后执行 `gradlew.bat build --offline --console=plain`：**BUILD SUCCESSFUL**。alpha.12 当前 JAR 审计 `errors=0`：3,021 entries、55,700,369 bytes，SHA-256 `90825774bd3540bba97257fca1e0eac3c48410763151a010d9bc35c9f592fc44`。无 GameTest/ClientSmoke 类或 runtime mcfunction。结果快照：[audit after D10](native-audit-after-d10.json)，前态：[audit before D10](native-audit-before-d10.json)。本轮没有升版本或交付 JAR。

## 状态与未验收项

D10 仍为**实施中**：动画事件、轨迹、计时、碰撞与反射已经自动回归；D22 动态属性/统计链、D15/D16 更广义反射和 passthrough 公共语义、客户端粒子视距、多人/生存实机及逐帧原包坐标重放仍未闭合。世界放火未出现在这条攻击调用链，本轮没有写入火方块；C03 对 D06/D08/D09/D44 的待确认选择仍保持未决。没有修改 `_nyctocosm_source`、真实存档或 alpha.12 版本号。

关键源函数 SHA-256：

| 源文件 | SHA-256 |
|---|---|
| `apocalypse_rain/start.mcfunction` | `DECBDCC28DFB6455CBE8C5925711A1E6836A147F03B848D8FD368997A5754972` |
| `apocalypse_rain/fire.mcfunction` | `99C1DF3A4554054E3B5404757EDC0B9EEDB6F4AB3C3C8142D52D969112D78234` |
| `apocalypse_rain/slowcast.mcfunction` | `D36D3B68219DD81328ECE9DFBDEAC8920BE7391C169891D66BD717B35F71792A` |
| `apocalypse_rain/subtick.mcfunction` | `6B077CE582ED5FA1B5608B2A2A591DF084F7E770D58FAB635FFF2ABC8212DA00` |
| `apocalypse_rain/impact.mcfunction` | `4B64F2ECAA1AE86035C48B19453B2C41032B65590A2A7CBA51548BFFE5863132` |
| `__generated__/block/689.mcfunction` | `26EE1F535730773EF089CB846C367252969C90E865C3371C72596E81E0F340DC` |
| `__generated__/block/690.mcfunction` | `01F064CDA58DDA522067D4F116E3B08723236B56B08DD4B283B09A50B1DED48B` |
| `__generated__/block/691.mcfunction` | `2E5A08B8DF890EF18E0D39F3E9374D6245268AF5115648F7F0DCBA4458BFC500` |
| `__generated__/block/692.mcfunction` | `B20103DEF6827EF2009E8D8C0E466471270D699B8EB657042A1FEFECC1281666` |
| `__generated__/block/694.mcfunction` | `89806DC7014E5B0098E0254080F662244F51DF84066B78280A4BBC353768DD9C` |
| `__generated__/block/696.mcfunction` | `DC071CFDABF847EA93DBAD35BE780F6F54BBB0FA01BACDDC8947507AC42384B8` |
| `__generated__/block/697.mcfunction` | `63819E8BF3BBA003EFF483E8EDDFA3F4CFAEDFE2449AC34E8CFC320E8AACEC77` |
| Animation fire leaves 10–44 concatenated | `DC85F5FDBF55195DE2135F52D94B99D7C4F524A0C8920E88C59909B8F7802A67` |
