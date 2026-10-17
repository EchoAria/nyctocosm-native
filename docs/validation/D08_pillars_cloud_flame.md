# D08 Pillars 双类型持续载体验证记录

日期：2026-09-30。源为 `_nyctocosm_source/datapack/data/emissary/functions/attacks/phase1/pillars`，实现目标为 `nyctocosm-native` alpha.12。

## 源行为与本阶段代码

原包在 `phase1.attack.pillars` 第 33 帧创建 flame/cloud 两个 marker，从 boss 朝向 +60°/-60°开始。`subtick` 分别推进：云柱每 tick 0.4 格，火柱 0.6 格；两者每 tick朝 300 格内最近的非 spectator/debug 玩家转向、生成对应柱体粒子与 MASTER ambient 音效，最多存续 300 tick。接触选择器以 marker 为原点覆盖脚部 block 坐标 x/z -2..+1、y -3..+17，且只在玩家 hurtTime=0 时处理。云柱造成 Dissonance 基础 35 伤害、40 tick Blindness I 和 Slowness III；火柱造成对应伤害并沿途对空气 `fill fire`。两类 impact 均有各自音效/粒子，Reflectance 能重定向 marker。

本阶段新增 `EmissaryPillar` entity 和注册/renderer，在 NativeBoss frame 10 播放 throw cue、frame 33 创建成对持久载体。载体保存 owner、kind 和 age；按最近玩家导航、逐 tick ambient/柱体粒子、脚部方块矩形 + hurtTime 命中、Dissonance 35，以及 Reflectance 能耗/转向推进。状态效果严格限制为 cloud mode。

火柱的粒子、移动与接触伤害已经接入；**世界火焰方块放置没有实现**。C03 放火规则仍未获用户选择，因此代码没有执行 source 的 fill fire，也没有把任一放火选项视为已批准。

## 自动验证

3 项 `emissary_pillars` GameTest 覆盖：

- 第 32/33 帧未生成/恰生成 cloud+flame 两个独立 carrier；
- 云柱 0.4 格移动、source contact damage/Blindness/Slowness III、owner/kind/age 保存恢复、火方块未改变、age 299→300 清理；
- Pillars marker 穿过 Reflectance 后消耗储量并沿 reflector 朝向继续移动。

完整命令：

```powershell
.\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d08-pillar-final2-20260930"
```

结果：**158/158 required GameTests 通过，BUILD SUCCESSFUL**。日志：`run-d08-pillar-final2-20260930/logs/latest.log`。先前错误的最近玩家方法重载导致一次编译失败，改为坐标式谓词重载后通过；最终复验还修正了 flame mode 不施加 cloud-only 负面效果。

alpha.12 JAR 审计：`python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`，**errors=0**；3019 项、55,631,849 字节，SHA-256 `02c4a496c83dc21029acd3b7f2828219070d654bacc15f7f742b27737c5c6fd9`。包含 `EmissaryPillar.class`，未包含 GameTest/ClientSmoke 类或 runtime mcfunction。快照：[native-audit-after-d08-cloud.json](native-audit-after-d08-cloud.json)。

关键源函数 SHA-256：

| 源文件 | SHA-256 |
|---|---|
| `pillars/activate.mcfunction` | `5108D6950FF20DE36E95EF56EBFF9FF6E6611AEB53EA62D554AEB38E468BE76C` |
| `pillars/subtick.mcfunction` | `C103CF0FC64CA11064A95B384DE9365D22BF7C2E23BD36FB03B74AB043922A11` |
| `pillars/slowcast_cloud.mcfunction` | `551CD1F0329D03D6888AF8E403D4FAFE69866CFFA5912707A48E7CF094E75676` |
| `pillars/slowcast_flame.mcfunction` | `203BCF8AD111C0558DC49323DE8FA1F631DCBE644D69BE681E8BF9D28489A46A` |
| `pillars/impact_cloud.mcfunction` | `4033BF09492DB264B1A5C547C988E745CB92D2CEC49D8D23747C747FE9447105` |
| `pillars/impact_flame.mcfunction` | `791404E06D772BCE15EEF17B4F248A244B979AD4A86BB8F685C3482D266B52F8` |

## 状态与下一步

D08 **实施中**：双 carrier 的自动行为通过，仍缺 C03 用户选择后的火方块规则与回归、客户端视觉和生存多人验收；完全 300 tick 实战及 Boss 换招后运行也待手测。没有改动 `_nyctocosm_source`/真实存档，没有发布新版本或中间 JAR。
