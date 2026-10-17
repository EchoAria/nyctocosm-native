# D11 Celestial Smite 验证记录

日期：2026-09-30。源数据包只读，目标为 nyctocosm-native alpha.12。

## 源事件和攻击语义

检查 phase2/celestial_smite/start.mcfunction 与其调用链后，发现动画以外还有一条容易漏掉的同步路径：start 直接执行 generated block 26（tear）、block 50（circles）和 block 93（activate）。所以起手会立即创建并激活一枚 tear；动画自身随后仍会在第 26、50、93 帧再次执行这些事件，形成第二枚延迟 tear。每枚 tear 各自走一次 raycast。这是源代码可达路径，不是推测出来的额外攻击。

延迟 tear 在第 26 帧选取 300 格内非 spectator、非 emissarydebug 玩家，marker 放在该玩家脚下 +100 格，再朝最近合格玩家脚下转向；随后发出 MASTER 音量 50、听距 300 的 tear 声和 shockwave cue。第 50 帧将所属节点置为 active；第 93 帧从节点方向启动 raycast。起手同步调用中的这些事件立即连续运行，不等待动画帧。

raycast 从原点开始检查 step 1–150，每步前进 1 格；第 150 次检查的坐标是原点 +149 格。当前方块不带 nyctocosm:passthrough 标签，或射线位置 5 格内发现玩家，射线在该位置终止并造成 impact。抵达 150 步上限自身只结束射线，不视作 impact。沿途 5 格是终止条件，不是沿线伤害区；只有命中终点后，才对终点 15 格内玩家造成基础 Radiance 伤害 80。源调用链还会产生 explosion emitter、音效和粒子；impact 后还会以 fill ... fire replace air 写入 9×5×9 范围内的火方块，此规则依赖尚未确认的 C03 选择，因此本轮没有实现。

终点伤害源链进一步经过 magic:calcstatsdamage、weakness/resistance 和伤害记账（generated block 397–399）。本轮接入基础 Combat/Forge 命中，但 D22 的非默认统计及药水修正仍未闭合。

## 本轮实现

- 新增持久服务端实体 EmissaryCelestialTear，保存 owner、时间、active/resolved 状态与 raycast 结果；注册实体类型并加入客户端空 renderer。
- NativeBoss Celestial Smite 分支按上述同步起手和延迟动画事件调度：age 0 起手立即发射，age 26 建立第二枚预警 tear，age 50 激活，age 93 发射。
- 射线使用源 passthrough 方块标签，按每步方块与 5 格玩家终止，实施 150 次检查；world fire fill 留空，避免在 C03 未确认时写入世界。
- 视觉目前是原生粒子/环形 cue 近似；没有接入源模型资产及其完整 beamwave/explosion emitter 几何。未知区块不强制加载，直接安全结束且不造成 impact。

## 自动验证

新增三项隔离运行的 emissary GameTest：

1. 验证同步起手立即完成一次伤害射击、无残留起手 marker；延迟路径在 age 26/50/93 的创建/激活/命中边界，tear NBT 保存恢复，以及 150/80 参数。
2. 验证墙体阻断与 impact 位置、终点 15 格范围内/外伤害边界，以及 C03 未确认时 9×5×9 区域不会出现 Fire 方块。
3. 验证玩家距离射线 5 格（含边界）时提前终止；空旷路径最多检查 150 个方块位置，末端是原点 +149，单纯达到上限不会造成 impact。

最终完整 GameTest 命令：

    .\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d11-celestial-final3-20260930"

结果：**168/168 required GameTests 通过**；日志：[run-d11-celestial-final3](../../run-d11-celestial-final3-20260930/logs/latest.log)。此前失败的 D11 运行暴露了并发测试玩家污染随机目标选择及测试世界实体串扰；最终将三项测试分批隔离并使用远离其他夹具的区块，完整回归通过，没有缩减生产命中边界。

随后 gradlew.bat build --offline --console=plain 成功。alpha.12 JAR 审计 errors=[]：3,023 entries、55,709,573 bytes、SHA-256 607bf3ff8ea1a3bd002c736b85daf0f21aa60ad930a162cd631f2b86feaf176c；JAR 含 EmissaryCelestialTear，无 NativeGameTests、ClientSmoke 或 runtime mcfunction。审计快照：[D11 前态](native-audit-before-d11-20260930.json)、[D11 后态](native-audit-after-d11-20260930.json)。未升版本或交付 JAR。

## 未闭合项

D11 保持**实施中**：C03 火方块规则等待用户选择；D22 非默认属性、weakness/resistance 与记账未接入；源模型、explosion emitter 与 beamwave 完整视觉尚为近似；未知区块/owner 缺失路径没有专门 GameTest；客户端、多人生存实机和源逐帧视觉对照未验收。没有修改 _nyctocosm_source 或真实存档；alpha.12 保持不变。

## 关键源文件 SHA-256

| 源文件 | SHA-256 |
|---|---|
| celestial_smite/start.mcfunction | C3463D483F24763EFA805FE8B2565EFAC13FC7ACFA7845DBA546FAF2DBF2FCBD |
| celestial_smite/tear.mcfunction | 68458B5ECA6CC1C2D6B08FA437C3260DE9015654C5B7B63A132B25A39D58A83E |
| celestial_smite/activate.mcfunction | 4B994ECD6B1953B5A16220B2EF8E6C56C1635AD39A40A1BF62D28CDA93F8AFB6 |
| celestial_smite/raycast.mcfunction | B272FE779DF35AFA5A944FF40A4A382DCCFFD26A5D931EBB965AEE81BDBC8A6A |
| celestial_smite/__generated__/block/391.mcfunction | E1ACD8F45A070D48060F029E9DB836D483189AC606761FD383C8EE019C88B220 |
| celestial_smite/__generated__/block/394.mcfunction | E032EFCA665780051BD8FAE33DE5E7E85247021960CDB04EC4714E278B7B8526 |
| celestial_smite/__generated__/block/395.mcfunction | E49500E71F5427B3A83DEA8F05368563F565A2FECEB70C55B6CAD22AA4E1D166 |
| celestial_smite/__generated__/block/396.mcfunction | 82C877C6B69847F5D29E8F0F0258B943B43E50B5B4A82E42825D0D1E8CE96CC8 |
| celestial_smite/__generated__/block/397.mcfunction | B420F56D38EF6367DFDD5A08EF427CD05C607EB75FCDB6B85DD4135D9EC9E109 |
| celestial_smite/__generated__/block/398.mcfunction | 871F0C58BD456B7BD159F33750EFA9E788FF66A6B7E6722FC6F482A17D37E77A |
| celestial_smite/__generated__/block/399.mcfunction | 90D22BA55DBA2F007EFF906CE479C751AD50BB91882970178A5E35E42DCF9873 |
