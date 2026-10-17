# D48 Serpent Laser 命中采样验证

日期：2026-09-30

状态：自动验证通过·待实机；Serpent 实际 100 tick 射线与目标移动的端到端伤害尚未录制验收。

## 源码调用链

`start` 选取/固定 `serpenttarget`，初始化 laser 状态并启动旋转；生成块205 建立并放置 target marker；每个 active subtick 的 block206 先递增 Laser timer、推动 marker 0.5 格，再朝 marker feet 调 raycast。`while/3` 从射线起点前进一格进入 `while/4`；while/4 每格先执行距离 3 内的 `@a` 选择器和 block208，再检查 150 格上限与首个非 `nyctocosm:air` 方块。block208 读取玩家 NBT `HurtTime`，仅 `HurtTime=0` 继续 12 Nightmare damage；block207 在 timer 100 收束 attack。

关键源文件 SHA-256（均相对 `_nyctocosm_source`）：

| 路径 | SHA-256 |
|---|---|
| `datapack/data/serpent/functions/attack/laser/start.mcfunction` | `97624a859bbddb9dc184355465d5ac86c5ca06e5edd2c4cc84969b2ceccfb5d1` |
| `datapack/data/serpent/functions/attack/laser/subtick.mcfunction` | `acfd7509868c8ef93d4274ff6d1e9ecfe3ceedb5952bcabd517d8b0cc43eff43` |
| `datapack/data/serpent/functions/attack/laser/raycast.mcfunction` | `115f2a7bdfb70f0aa940dccab775b3968bc1093addf8768e90ddd4976dabbe39` |
| generated `while/3` | `a8c0ec31de17fc318ad89bb3589de4a966869d2cec485d491429be9da89348d1` |
| generated `while/4` | `54183cab9396d70cd896b8178b96b3c8df63b634b388e972e0731e54bdfe5923` |
| generated `while/5` | `11275a61e4d702027fbf632d5bb0fb473b87128068f0572787e79ffb21aa9b11` |
| generated blocks `205`–`207` | `f26de9cdd80da0e4e5ac8d2ee6b5d09554cfda3d6ccd98a2cc926d90dd3aba15`; `0e0b2014dc85a81b074fa5b8d63db451243006d439e0dac4373ada614c1682c7`; `6b3af829502a13cfa8700c23888e156871867a63d0e59e3924f4ca8cdb1c49a9` |
| generated block `208` | `93301ebe9e963f6541aff3c96eedc663ce5974ad0046dbaf5004cd748b3f8da1` |
| generated blocks `209`–`211` | `2bd6464c3bf12601661965e3b63ac62ee19d5eaaea9595c87d0a3420c0f6a363`; `1394bb51353a0c0dc4e60f4141a3d8d68005b1ea3abc34ac2d59f0ee06a730f5`; `daffaac5d5f7c9b882ecbeef3acc691c0bd395324cc86c087e19126560f65626` |
| generated blocks `212`–`215` | `c2c0da50aedd2a16bb4310d317e4f2de4eec0029266bac634eadda8808f0659a`; `2e981fa7e2fb62cc73748f58eface233ef1b25074a665f352368d59b6d43fec2`; `f9a246f4a97f2df007e1267d7167b53741ee5b85a62f885068c2f6e6f2221dcc`; `d81c37927d9aface021d1067aa9217f7417b5bec80828dac975e1952dc38d14d` |

## 修复

旧原生实现将全段射线与放大的目标 AABB 做连续相交；这会击中脚部超出三格但身体碰到扩展盒的玩家、HurtTime 非零的玩家和射线上的非玩家生物。

新增 `Combat.serpentLaser`，只用于 `NativeBoss.tickSerpent` 的 Laser 分支：从距起点 1 格开始，以每整格一个样点、最多 150 点查询玩家；按玩家脚部 `position()` 的欧氏半径 3 做精确过滤；只在 `hurtTime == 0` 且 `Combat.canHit` 时调用 Nightmare 12 伤害。命中一次后剩余样点不对同一玩家重复调用，等价于伤害写回 HurtTime 的源调用顺序，也避免 Forge 取消事件时同 tick 重发多次。通用 `Combat.line` 和其他 beam 行为没有改动。

修改文件 SHA-256：

| 文件 | SHA-256 |
|---|---|
| `src/main/java/com/flubberschnub/nyctocosm/Combat.java` | `7fdda69c10701ed527d6ac3394d2c8739788f457e57a8b8f79bdd89d1a2ee044` |
| `src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java` | `9576fff531621347e3c20911e3b4979d0a024a3b313625c4c9d025fc5d3855dd` |
| `src/main/java/com/flubberschnub/nyctocosm/NativeGameTests.java` | `9a7dee8c3e4b1e37d8f8462686f9054257e6b7617ad1aea5e7360aea0c1556df` |

## 验证

- 三项红测先对旧 `Combat.line` 运行并按预期失败：半径外脚部/擦碰身体、非零 HurtTime 目标、射线上的 Cow 都受到伤害。换成新生产路径后，三项测试均通过；精确脚部半径 3 的边界会命中，半径外不命中。
- 最终命令：`gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d48-source-samples-final-20260930"`。日志：[latest.log](../../run-d48-source-samples-final-20260930/logs/latest.log)。3 项 `serpent_laser_damage` 全通过；全套 **195 项中 193 项通过**，仅两项既有 `flame_geysers_*` 失败。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- native audit `errors=[]`；3,026 entries，alpha.12 JAR 55,727,468 bytes，SHA-256 `0918620d7f7bb60e10604fea3d2e0c26a5ae4a1ad50d6eb5dc75b301dafc0f87`；无 GameTest/ClientSmoke 与 runtime mcfunction。快照：[before](../native-audit-before-d48-20260930.json)、[after](../native-audit-after-d48-20260930.json)。重建前 D34 包 `55b1329dfa2bd540844bfe0afc1dd3fe8d5b3e0a26fb8eb4aec6ae3c5bf70586` 已备份到隔离运行目录。

现有 `serpent_laser_survives_reload` 与 `serpent_laser_sweeps_a_fixed_marker_not_the_live_target` 也在全套回归中运行。尚未实测真实 active marker 的每 tick 伤害、客户端效果、Forge 第三方取消组合和完整区块卸载/重载；没有改动源包、存档或安装目录。
