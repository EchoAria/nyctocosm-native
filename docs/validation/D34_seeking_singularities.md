# D34 Seeking Singularities 验证记录

日期：2026-09-30

状态：自动验证通过·待实机；客户端音画、真实存档续接与 D22 动态伤害仍未验收。

## 原包行为证据

攻击入口由 Sorcerer timeline 的 `seekingsingularities` 帧调用 `summon.mcfunction`：共生成 8 枚弹体，给每枚 marker 分配随机 70–89 tick 年龄。每个 subtick 先扣年龄，再找 50 格内最近的非旁观玩家；转向命令链相当于归一化 `6 × 当前朝向 + 0.5 × 指向玩家眼睛的向量`，随后前进 0.24 格。这里不检查方块，也不使用 D16 passthrough tag。移动后做玩家选择器碰撞；选中玩家时先执行 Void 伤害，再无条件施加隐藏粒子的 Slowness II 5 秒，最后清除弹体。寿命归零会播放 flash/impact 并清除，不造成范围爆炸。每个存续 tick 还播放 0.2 音量的 Gravitance hum 并生成青、紫、黑三色粒子。

源文件 SHA-256（相同摘要的生成块已合并列出）：

| 源路径（相对 `_nyctocosm_source`） | SHA-256 |
|---|---|
| `datapack/data/sorcerer/functions/attacks/seekingsingularities/summon.mcfunction` | `6054b36e3b997587cfc66dc4237b4b86660eae9f869695732737d55930352b78` |
| `datapack/data/sorcerer/functions/attacks/seekingsingularities/subtick.mcfunction` | `5f0471221893fb59f7ce1be1f36b0b36da99bb3e79bb07339032b820aee814b6` |
| `datapack/data/sorcerer/functions/attacks/seekingsingularities/start.mcfunction` | `16497877adfb596ab07fea3b2f841164dbe24239ea1a1ff4478c628900f87350` |
| `datapack/data/sorcerer/functions/attacks/seekingsingularities/hit.mcfunction` | `f10bf69146a02c384d9a8a7ed06aa5569698bb13ab50155e7e46a1eef9b7641d` |
| generated block `141` | `11d0c957830de13b0786cc1a68d792d575430460966cfd7603feb06723e1a765` |
| generated block `142` | `2e5a08b8df890ef18e0d39f3e9374d6245268af5115648f7f0dcba4458bfc500` |
| generated block `143` | `04043e6a1f42398e99f855727728957e8cb93dc4e2d155d36f725a08b7b6e5f0` |
| generated block `144` | `d77e1dde5b220827798f4a7eca2d0a9e84f3b63ec82bafff948a875bed84c085` |
| generated block `145` | `f63b7afb1a0f4e220033f3eb57d8a826cfa9a48754fd6f9e7e6b50b78f081dd8` |
| generated blocks `146`–`153` | `a0bc8e1b65642512a9e15f007f07b15b7d31734287ad99ceffff7fa5a50dc69a` |
| generated block `154` | `48008965bac2944c90a77c9f0bf1d58126be5f89a597ee9be90ff64c868e23ce` |
| generated block `155` | `357b94afb1b4e5053f2d10738d0a12e752d75bc7c6293ca14cca44cd7cf933b0` |
| generated block `156` | `4f2d7e76a374adb15f23ce4393328ce81ff5c7d1ccd8eeebd6d453485a702590` |
| generated block `157` | `32055bf76137413fe65f4e69bfc194e9cc8596d8d36fb5a1a23b473dff7e5ed7` |
| generated block `158` | `5e0dcd684a9bf826ba9e137fd06643c114f42a067c93ce93cc8ac87256fe2226` |
| `datapack/data/spells/functions/reflectance/reflect.mcfunction` | `51e8ee3fbaf1e0c84242aa9d86b2ec8528259dd20bccc5155c138bf8f5e9988e` |

## 修复与边界

- `NativeBoss` 在源召唤帧发射 8 枚 `SEEKING_SINGULARITY` 弹体，保留 70–89 tick 与 0.24 格/tick。弹体每 tick 重选 50 格球内最近存活、非旁观玩家，以眼部位置和源局部转向公式计算运动；方块不阻挡，也不会套用通用伤害弹的 locked target 筛选。
- 复现源玩家半格选择体积与只命中玩家的行为。用户确认的 Slowness II 在选择器命中后无条件添加，不依赖 `Combat.hit` 是否被目标无敌状态或 Forge 伤害事件拒绝。
- 对照源选择器闭区间补测发现 native 最近目标初值比较漏掉恰好 50 格的玩家，现改为包含该边界。
- Reflectance 将该弹体转为 DAMAGE、清除追踪目标并保留穿墙标志；一般弹道查询允许无命中时返回 `null`，现安全跳过碰撞处理，避免反射弹在空路径上崩服。
- 为寻星弹单独使用三色源粒子，并按源存续 tick 播放 MASTER Gravitance hum；直线 Singularity Charge 弹维持原视觉分支。
- D22 自适应伤害、来源统计/记账与完整 client 动画/声音试听仍未接入或验收。每 tick 音频范围用服务端半径 30 包围，未实机比对原命令的 `minVolume` 衰减细节。

修改文件 SHA-256：

| 文件 | SHA-256 |
|---|---|
| `src/main/java/com/flubberschnub/nyctocosm/SpellBolt.java` | `b49cad5d09b75b3d2f0c5a5b81b6b994eeb0582a7eb04f7db1cdee84c7665381` |
| `src/main/java/com/flubberschnub/nyctocosm/NativeBoss.java` | `3bcc5c7e8da4799976b85e706199d8ff9e78a9b7a098bf7e9295d3c661bdff33` |
| `src/main/java/com/flubberschnub/nyctocosm/NativeGameTests.java` | `5fd5b0755e75ee76f1f2e298eab4680c2f114df53a13303df723518b8f724474` |

## 自动验证

- 边界回归先按预期失败：精确 50 格时弹体保持直线；修复后通过。
- 反射穿墙回归最初触发 `ProjectileUtil.getEntityHitResult == null` 空指针；修复空结果处理后通过。
- 最终隔离命令：`gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d34-source-feedback-final-20260930"`。日志：[latest.log](../../run-d34-source-feedback-final-20260930/logs/latest.log)。**7 项 `seeking_singularities` 测试全通过；全套 192 项中 189 通过、3 项失败。**失败为 `wing_blast_player_contact_ends_ray_and_damages_on_cooldown` 与两项 `flame_geysers_*`，均非 D34。
- 独立 `gradlew.bat build --offline --console=plain`：BUILD SUCCESSFUL。
- `audit_native.py`：`errors=[]`；3,026 JAR entries、55,726,735 bytes，alpha.12 SHA-256 `55b1329dfa2bd540844bfe0afc1dd3fe8d5b3e0a26fb8eb4aec6ae3c5bf70586`。无 `NativeGameTests`、`ClientSmoke` 或 runtime mcfunction。前/后快照：[before](../native-audit-before-d34-20260930.json)、[after](../native-audit-after-d34-20260930.json)。
- 构建前包备份位于 `run-d34-source-feedback-final-20260930/pre-final-build-nyctocosm-native-0.3.0-native-alpha.12.jar`，SHA-256 `cdfbb80330d11f68fcf799852c1411af122f0cb28191365be9ca10d0b6f9b38a`。

## 待验收

仍需真实客户端验证弹体朝向、墙后可见性、每 tick 声音/粒子和状态图标；还需在真实世界/专服确认区块卸载与重载保存后不会重放、改目标或丢失 IgnoreBlocks。D22 动态伤害与伤害统计路径未包含在本切片中。未修改原数据包、用户存档或安装目录；未安装/发布 JAR。
