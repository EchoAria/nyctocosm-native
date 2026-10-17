# 回响碎片与远古城市传送门（alpha.10）

本轮优先处理用户报告的 Resonance 施法、门框内部命中和激活动画。完整元素属性、反射器和法阵移动规则不在本轮完成范围。

## 根因与修复

旧 `SeelicField.finish` 调用通用 `Combat.beam`，只在碰撞终点尝试开门，且用 END_ROD 代替原声波。命中空门洞时终点位于门后，无法识别门框；直接命中门框则立即填满门面，没有演出。

现在使用独立 `ResonanceSpell`：射线逐格检测，穿过有效门框内部即可触发。服务器判伤、扫描、开门，客户端播放粒子。通用 Resonance 元素伤害不再意外触发传送门。

## 原包来源

源码根为只读的 `_nyctocosm_source/datapack/data/`。

| 来源 | 当前实现 |
| --- | --- |
| `spells/functions/resonance/cast.mcfunction`、block/250 | 开始施法时记录脚底上方 1.625 格、朝前 5 格的固定发射点和朝向；80 tick 蓄力，NBT 保存瞄准信息 |
| `spells/functions/__generated__/block/44,45` | 逐渐扩散的灵魂火蓄力、释放声波；不在蓄力结束时重新朝向玩家 |
| `spells/functions/resonance/raycast.mcfunction` | 70 个逐格声波采样，每个采样检测门框；原射线没有方块截断，保留穿块行为；同一生物每次施法只伤害一次 |
| `effects/functions/elements/resonance/transmutate/__generated__/block/597,595,596` | 3 tick 迸发，共 400/144/144 条局部粒子数据，转换为数值 JSON |
| `nyctocosm/functions/portal/activation/sequence.mcfunction` | 第 0/16/32 tick 分别启动三波共振 |
| `nyctocosm/functions/portal/__generated__/block/57–105` | 共振环向门前后扩展再收束；三波各四环，每环 36 点，保留原距离、半径、类型、闪光与烟花数量 |
| `nyctocosm/functions/portal/__generated__/block/58,61,64` | 第 90/106/122 tick 依次收束；第三次收束才生成可通行门面 |
| `effects/functions/shockwave/__generated__/block/36–38` | 开门后第 1/4/6 tick 的双侧冲击波，130/460/540 条局部粒子数据 |

`tools/import_resonance_effects.py` 只在开发阶段转换几何数据。`docs/resonance-particle-import.json` 保存源文件哈希和数量；发布 JAR 不包含或解释 mcfunction。

## 音效

- 施法开始播放 `spells.cast.resonance`，主音量通道，音量 6，50 格范围。
- 三波分别播放 `portal.external_resonance.first/second/third`。
- 三次收束分别播放 `portal.external_resonance.collapse.first/second/third`。
- 六个开门音效均用原音频、主音量通道、音量 50、100 格范围。不会被“敌对生物”音量单独静音。

## 状态与边界

- 20×6 内部空间、X/Z 两种方向均支持，门框仍要求强化深板岩，内部允许空气、幽匿脉络（SCULK_VEIN）和已有门面；开门时替换覆盖的幽匿脉络。
- 每扇门拥有独立 `PortalActivation`。重复命中不重复创建、不重置演出；已经开启的门不重播。
- 中途破坏门框或堵塞内部会取消演出。保存、重载或区块卸载保留进度；重新观察者直接看到当前阶段。
- 开门闪光/烟花为纯视觉，不复制原包借 creeper 制造的地形破坏。
- 当前仍使用已有原生门面；常驻 hum 和完整表面细节仍待独立迁移。未宣称完整原包环境逐帧等价。

## 验证

`docs/resonance-red-gametest.log` 记录修复前“射向门内失败”和“门面立即出现”的两个失败。

回归涵盖真实法阵消耗玩家投出的回响碎片后开门、等待完整演出、重复施法、动画中重载、破框取消、双向穿越 Z 轴门框，以及两方向下完整粒子时序的所有帧。`docs/resonance-client-red.log` 记录客户端发现的末段烟花映射遗漏；已修正并加入全时序回归。

```powershell
$env:JAVA_HOME='C:\Users\jzy19\.jdks\ms-17.0.19'
$env:JAVA_TOOL_OPTIONS='-Dnet.minecraftforge.gradle.check.certs=false'
.\gradlew.bat build runGameTestServer --offline
.\tools\run_resonance_smoke.ps1 -JavaHome $env:JAVA_HOME
python tools/package_resonance_capture.py
```

客户端场景使用真实灵魂瓶与投出的回响碎片，分别激活 X/Z 门框；断言两次开门、14 个客户端音效事件及其时序。
`docs/validation/resonance-portal.mp4` 是客户端连续画面。**预览音轨按客户端记录的事件时间与原始 OGG 合成，并非系统声音录制**；音量混合仅用于预览。

## alpha.11：开门表现与覆盖修复

- 第 90/106/122 tick 补上原 creeper 爆炸对应的 EXPLOSION_EMITTER 视觉，不生成破坏地形的 creeper。
- 对照 effects/screen_shake，50 格内三次抖动分别持续 10 tick，幅度 0.5/1/1.5 度。原包随机修改玩家朝向；原生版改为镜头偏移，结束后恢复，不改变玩家瞄准。波形并非逐帧复制原随机序列。
- 对照 effects/soul_swirl 的 624–636：最终收束释放 15 束旋流，延迟 0–9 tick、寿命 4–11 tick，每 tick 前进十次、每次 0.2 格；成对发射 wax_on 与 soul_fire_flame，保留随机半径、角速度、60% 初始直线概率和 30% 半径增长概率。使用确定性本地随机种子，未复用原数据包全局随机状态。
- 客户端追补跳过的短时序帧，避免漏掉单帧冲击波与收束粒子。
- 幽匿脉络不再阻止开始或维持开门；实心阻挡与破损门框仍被拒绝。
- 巨蛇只过滤 IN_WALL 方块窒息伤害，身体命中部件仍通过本体处理攻击。

修复前的两个失败见 portal-polish-red.log；修复后 69 项测试通过，见 portal-polish-gametest.log。客户端场景现在使用带幽匿脉络的 X/Z 门框，并检查实际镜头事件中的六个抖动阶段。
