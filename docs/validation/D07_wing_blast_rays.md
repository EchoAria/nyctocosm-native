# D07 Wing Blast 六路射线验证记录

日期：2026-09-30。对象：`_nyctocosm_source` 与 `nyctocosm-native` alpha.12 工作区。

## 源行为与变更

源行为由 `wing_blast/eyes.mcfunction` 在动画第 12 帧创建六个独立翼尖 marker；`activate.mcfunction` 在第 35 帧将它们切换为 blasting。`subtick.mcfunction` 按 marker 的 `em.timer` 分预警和主动射线，并在达到 43 后处理末 tick 再清理。两类射线最多逐格前进 100 格，以 `#nyctocosm:passthrough` 判定阻挡。主动射线还会在玩家所在 block cell 终止、检查 Reflectance 消耗/偏转，并在终点产生半径 4 爆炸和 40 radiance 伤害；只对 hurtTime=0 且非 spectator/debug 玩家造成伤害。六翼每 tick 具有各自 yaw/pitch 微调。frame 0 播放 `rise`，frame 12 播放 `eyes`；两者通过 MASTER 声道对 300 格内玩家播放，源音量为 50。

改动包括：

- 新增 `EmissaryWingBlastRay` 服务端实体和注册；marker 保存 owner UUID、翼编号、年龄、预警/主动状态。第 12/35 帧由 `NativeBoss.performEmissary` 驱动，开始/换招清理由独立生命周期管理。
- `EmissaryDamagePose.sampleAt` 支持按动画、locator、指定帧取样。定位资源 exporter 扩为 9 个 locator：原 D01 的三个受击 locator 加六个 `wingtip1` locator，导出 44 个动画、2601 帧、2640 个源文件；资源 `sourceSha256=9856F853246135E80DF0C635F93A9A1B5C1D131EA11855ED652A4E4305C2AF82`。审计现在逐帧检查九个 locator 集合。
- 射线同步使用当前 marker 起点/末端。方块边界、玩家格终止、半径伤害、保存恢复和 Reflectance 储量/朝向纳入测试。rise/eyes 声音现复用 MASTER/50/300 范围。

## 自动验证

四项 `wing_blast` GameTest 覆盖：第 11/12/34/35 帧时序、六翼 locator 生成坐标、owner 和 NBT 恢复、预警到主动状态切换；非 passthrough 墙体、墙后目标和近远受害者；玩家格接触与冷却；Reflectance 储量消耗/转向及 age 42→43 的末 tick 清理。

调试时三项首轮测试问题均定位在测试夹具并修正：Minecraft yaw=0 朝 +Z，方向夹具改为 180°；恢复实体前先移除旧 UUID，避免同一 level 里出现重复实体；D06 用例源规则会随机选择合格玩家，测试现依据实际抽中目标所在列断言。随后扩充 frame、Reflectance 与生命周期边界检查。

完整命令：

```powershell
.\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d07-wingblast-final-20260930"
```

结果：**155/155 required GameTests 通过，BUILD SUCCESSFUL**。日志：`run-d07-wingblast-final-20260930/logs/latest.log`。

JAR 审计：`python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`，**errors=0**；3017 项、55,625,155 字节，SHA-256 `58f9b493eea94384db4f0545fe680cec0232ea8e726ea9af7ba11dbbb24c371d`。包内包含 `EmissaryWingBlastRay.class` 和更新的 locator bundle；无 `NativeGameTests`、`ClientSmoke` 或 `.mcfunction`。快照：[native-audit-after-d07.json](native-audit-after-d07.json)。当前 bundle 为 1 份、2601 帧；全包保留 10 个骨骼 rig、5607 帧、247 个声音事件。

关键源函数 SHA-256：

| 源文件 | SHA-256 |
|---|---|
| `wing_blast/eyes.mcfunction` | `592452689400831494C9AA05112424BDB0CD164E5D71D0A3B8A15B62A9F15620` |
| `wing_blast/activate.mcfunction` | `01669F80A4D7BB7B4222FD451FAA99E0A48D64E98778BBD7C90B1C137B4A6A6D` |
| `wing_blast/subtick.mcfunction` | `60C0110297DD5C95EC9E69431CF570768C5694AD0F23572869A1E601BD11206B` |
| `wing_blast/raycast_signal.mcfunction` | `0AE42BD5F65799DE9776E600FB7E9E8972BBC5E06DB03C5BD58B3AC02E8CF61A` |
| `wing_blast/raycast.mcfunction` | `C8581FD73FBEEEB8E18597FAEBD109AFE7BC0D4EFECA232D54D7C60F3C43F9BD` |

## 状态与剩余验收

D07 自动测试和 alpha.12 包审计通过，状态仍为**实施中、待实机**。尚未确认客户端六束 beam 在实际动画坐标中的可见性、高空对地战斗覆盖、音量听感和多人声道范围。Reflectance/passthrough 与 D15/D16 的通用语义也要继续对照。自动服务端测试不能替代这些客户端/生存验收。

玩法确认：本阶段无。D06/D08/D44 涉及的世界放火规则仍待用户选择，本阶段没有设置世界火焰。未修改 `_nyctocosm_source` 或真实世界；保留 alpha.12，没有发布中间 JAR。
