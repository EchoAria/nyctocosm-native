# D12 Flame Slash 验证记录

日期：2026-09-30。只读对照 _nyctocosm_source；目标为 nyctocosm-native alpha.12。

## 源事件与规则

动画 phase3.attack.flame_slash 在第 2 帧运行 slash1：以 emissarymodel 为原点，将朝向 yaw 相对旋转 +60°，局部上移 1.5、前移 1.5，创建一枚 projectile marker 并播放 shoot 声。第 13 帧运行 slash2，yaw -60°、局部上移 1.5、前移 2，创建第二枚并播放 shoot2。两个音效均为 MASTER 音量 5、听距 100。原生旧实现则只在第 13 帧从使者眼部发射五枚速度 1.5、寿命 70 的 FIRE 弹。

每枚源 marker 的 subtick 先加年龄，再在非 end 状态执行 slowcast。slowcast 检查 3 格内 Reflectance、向 300 格内最近合格玩家追踪、前进 0.8 格；在垂直截面上的半径 2 圆周按 20 个点逐一测试当前方块内玩家。任一点发现玩家就给波面和目标标记 end/impact；同一步仍完成全部采样。中心方块不带 nyctocosm:passthrough 时只结束波面，本身不添加玩家伤害目标。年龄达到 40 时，最后一次移动/采样已发生，随后 wave 在下一 tick 才运行 impact 并销毁。源 impact 使用基础 Dissonance 伤害 50；伤害统计、weakness/resistance 和 Hypertrace 仍走 D22 源链。

本招没有世界火方块写入，不依赖 C03。粒子在中心和 20 个截面点生成 flame/smoke，另有 lava、wax_on；模型动画另由攻击动画控制。

## 本轮实现

- 新增持久服务端实体 EmissaryFlameSlashWave，按 frame 2 / frame 13 创建两个独立波面，保存 owner、age、ending 状态和待结算玩家。
- 复现起始 yaw ±60、局部位置偏移、每步 0.8、半径 2 的 20 点截面、40 步年龄、passthrough 方块终止、3 格 Reflectance 与基础 Dissonance/Forge 命中。玩家在圆周点的当前方块才会加入待结算集合；同一玩家在同 tick 多个样点只结算一次。达到终态后下一 tick 命中并清理。
- 粒子重建为原生 flame/smoke 圆周 cue；未接回源模型资产及逐帧 locator 动画。源慢速转向由连续 facing/position 命令构成，当前原生实体每 tick 对最近玩家直接计算 yaw/pitch，需另行做逐步坐标对照，不能视为转向公式已等价。D22 动态属性/weakness/resistance/记账也未接入。

## 自动验证

新增三项 GameTest：

1. 检查攻击 age 2 与 13 前后各生成一枚，归属与 yaw ±60 正确。
2. 检查 0.8 位移、20 个半径 2 采样点、NBT owner/age/heading、完成第 40 步后延迟一个 tick 结算。
3. 检查圆周命中通过 Combat/Forge、墙体结束波面但不凭空造成玩家 impact，也不改成 Fire 方块。

final4 的隔离完整运行命令：

    .\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d12-flame-slash-final4-20260930"

结果：**171/171 required GameTests 通过**。随后改进 NBT 测试夹具以避免注册重复 UUID：final6 中 D12 三项新测试仍全通过，但完整套件间歇出现 6 项既有 Wing Blast、Flame Geysers、Serpent 测试失败，因此 final6 的全套结果不能标记为通过。最终版完整日志：[run-d12-flame-slash-final6](../../run-d12-flame-slash-final6-20260930/logs/latest.log)。前一轮 171 项全通过日志：[run-d12-flame-slash-final4](../../run-d12-flame-slash-final4-20260930/logs/latest.log)。final6 报出的 6 项失败均未包含 D12 三项。

随后单独运行 gradlew.bat build --offline --console=plain：BUILD SUCCESSFUL。alpha.12 JAR audit errors=[]：3,024 entries、55,716,049 bytes、SHA-256 be504a23d9f644df03249e815ab8435a42f62c08695981f55d5b3a7cf9849f9a。JAR 含 EmissaryFlameSlashWave，无 NativeGameTests、ClientSmoke 或 runtime mcfunction。审计快照：[D12 前态](native-audit-before-d12-20260930.json)、[D12 后态](native-audit-after-d12-20260930.json)。没有升版或交付 JAR；git diff --check 通过。

## 未闭合项

D12 仍为**实施中**：NativeBoss 的两帧调度、实体轨迹/终止、采样与基础伤害已实现；还需复核源 facing 链在不同方向下的逐 tick 坐标，完整接入 D22 动态属性/weakness/resistance/记账，复原模型与粒子强度/位置，并进行客户端、多人生存实机验收。源 Reflectance 入口已接，但 D15/D16 的跨攻击统一规则仍未完成。本轮未写世界火方块；未修改 _nyctocosm_source 或真实存档；alpha.12 保持不变。

## 关键源文件 SHA-256

| 源文件 | SHA-256 |
|---|---|
| flame_slash/start.mcfunction | D677EF3E6FAABDA1B19F77EF5EF638C545D3F5D9DC9E536032D90ACD4F8844C8 |
| flame_slash/subtick.mcfunction | 6F9090694DEDCA4D4D90A068CC0404A7FFBD557E671AD5B939A20166D710E3CE |
| flame_slash/slowcast.mcfunction | B97D7609068315F504D6925501CD42B2A036C6ADFD4748ED6194032DB884D577 |
| flame_slash/impact.mcfunction | 81D8C196BD90F195F1243F61A20408EA6C468E851F5DB081E4827B48B0C9D59B |
| flame_slash/slash1.mcfunction | 68237DA84E8369DC926CE8683D4AA1819972571987F959A5E216AE67CE948F51 |
| flame_slash/slash2.mcfunction | C46C7C278EA235DEA8A08CC9CD11F8DCC724E73F22D1DF2D05EA315D22F30C3B |
| flame_slash/__generated__/block/542.mcfunction | 110C6956249FB9A3A463BDB519B9A17743ECB4F8FF7EA9A3897AFE30713539BB |
| flame_slash/__generated__/block/543.mcfunction | B739AFB86FC4AB71A65AFD2B4589C5208F8AB1338D3815AFB7939A1A7CEC79A0 |
| flame_slash/__generated__/block/545.mcfunction | CEB78A77DB49CE76DF952B0110E93E164D58EE11AF072CC1B1CD370054401B6E |
| flame_slash/__generated__/block/588.mcfunction | 676EDE4328B54CFF8CCAC56E0C4471A2E413ADC399AC8C1672EF47EF8C7E39A7 |
| flame_slash/__generated__/block/589.mcfunction | B9F844DBBDC3E5A51F4638EA74F12B82528E1CBD1BA44538341E32E9F3414F87 |
