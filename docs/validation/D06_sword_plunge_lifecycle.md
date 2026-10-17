# D06 Sword Plunge：独立巨剑生命周期

日期：2026-09-30。实现基于 alpha.12 工作区；此记录反映 D06 首轮纵向切片，仍有待决策与实机项目。

## 原数据包事件

关键源函数：

- [`activate.mcfunction`](../../../_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/activate.mcfunction)：从 300 格内随机选择非旁观、非 `emissarydebug` 在线玩家，在其 X/Z 的 `world_surface + 220` 处生成剑；动画第 45 帧执行此函数。
- [`subtick.mcfunction`](../../../_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/subtick.mcfunction)：每个实体 tick 增加一次 `em.timer`，最多调用三次 `slowcast`，处理该 tick 落入扫掠选择盒的玩家；攻击 timer 105 调 shockwave，110 调爆炸。
- [`slowcast.mcfunction`](../../../_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/slowcast.mcfunction)：前 200 子步每步下降 1 格，之后 50 子步每步下降 0.2 格；完成第 250 子步后停止移动。因此 3 子步/tick 下第 84 tick 到达 `surface + 10` 并停住。
- [`explode.mcfunction`](../../../_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/explode.mcfunction)：在目标列地表对 15 格内非旁观、非 debug、HurtTime 为 0 的玩家走相同 60 点 generic damage；随后把相对剑实体的 `[-10,+10] × [-10,+10] × [-3,+3]` 空气格换成火，并移除剑。
- [`block/344.mcfunction`](../../../_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/__generated__/block/344.mcfunction)：生成以 head item-display 渲染、缩放 10、CustomModelData 12 的石英巨剑，并在地表播放 fall 声。
- [`block/334.mcfunction`](../../../_nyctocosm_source/datapack/data/emissary/functions/attacks/phase2/sword_plunge/__generated__/block/334.mcfunction)：timer 105 时在剑身高度 -10 到 +40 每隔 10 格触发 shockwave 和闪光效果。
- 动画 [`leaf_5_effects_0`](../../../_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/phase2.attack.sword_plunge/tree/leaf_5_effects_0.mcfunction) 在第 5 帧播放 conjure；[`leaf_45_effects_0`](../../../_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations/phase2.attack.sword_plunge/tree/leaf_45_effects_0.mcfunction) 在第 45 帧播放 drop 并激活攻击。

## 本轮实现

- 新增 [`EmissarySwordPlunge.java`](../../src/main/java/com/flubberschnub/nyctocosm/EmissarySwordPlunge.java) 作为独立服务器实体：目标列地表 +220、源选择范围和排除规则、源随机 yaw/pitch、逐步下落与停驻、105 tick shockwave 事件、110 tick 地表爆炸与一次性清理。Boss 的攻击动画结束或 Boss 实体移除不会提前取消剑实体。
- 保存并恢复 owner UUID、年龄、子步数和 one-shot 事件位；在 Forge 正常 damage-source 流程中用巨剑作为 direct entity、Boss 作为 attacker。Damage amount 与源初始化一致为 60。
- `NativeBoss.performEmissary` 将 conjure/drop/落剑生成改为对应的第 5/45 动画帧时机，并移除原来推动 Boss 本体的 30–44 帧逻辑。
- 注册内部实体与隐藏视觉物品，增加客户端 renderer 和 item model。JAR 条目核验确认含两个模型、实体类和 renderer，未包含 GameTest 类或运行时 mcfunction。
- 新增 GameTest：Boss 锚定且第 45 帧发起攻击；地表 +220 和 1/0.2 子步速度边界；保存后独立计时、105/110 one-shot 与地表爆炸伤害。

目前 shockwave 粒子为原生近似；源 `effects:shockwave/meteor_shower` 的完整效果实体/粒子没有移植。原包巨大范围 fill 火尚未实现，等待用户对世界火焰选择的答复。D14/D22 的通用 generic damage 附加管线（非默认弱点/抗性与 generic trace）也不在本切片的等价保证中。

## 失败到通过

- 红测 `run-d06-red-boss-immobile-20260930`：旧行为在动画第 40 帧将 Boss 速度设为 1.2，新增锚定断言失败；其余 148 个 required GameTest 通过。随后移除 Boss 本体冲刺，将攻击移交独立剑实体。
- 首轮编译发现该 1.20.1 Forge 映射的 Heightmap API 接收 X/Z 而非 BlockPos，改用 `getHeight(type, x, z)` 后编译通过。
- 两条新断言初次失败是 GameTest 世界高度夹具错误：玩家位于 Y=-55，而测试列的 WORLD_SURFACE 为 Y=67。放宽目标查询盒并把爆炸受害者放到源地表范围后，生产逻辑未再改动，完整套件通过。
- 最终隔离运行：`gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d06-core-verified5-20260930"`。结果 **151/151 required GameTest 通过，BUILD SUCCESSFUL**。最终日志在 `nyctocosm-native/run-d06-core-verified5-20260930/logs/latest.log`。
- `tools/audit_native.py` 审计 **errors=0**：JAR 3,016 项、55,391,733 bytes、SHA-256 `be7e8b78ffe354b15a6c514bd45a0595574b91e3beb8a33463856151d436cd83`；runtime mcfunction 0。审计报告快照：[`native-audit-before-d06.json`](native-audit-before-d06.json)；当前审计文件此后可被后续阶段覆盖。

## 源文件 SHA-256

| 源文件 | SHA-256 |
|---|---|
| `sword_plunge/activate.mcfunction` | `06c88e869670f132be3c8cce3f58e607b73e8ca8dd0d55655886ad1a66bf6f00` |
| `sword_plunge/subtick.mcfunction` | `cb11af39c30e490d2762ccfb919c46378aa411bd7ad221a4bc8a27f2e96665aa` |
| `sword_plunge/slowcast.mcfunction` | `18a492e2921ea15353115d67209dcfe757e801c0e8fb963b950b4abfeb22f58e` |
| `sword_plunge/explode.mcfunction` | `42607327a3b29df68c4a1ad390da24ebba7daa86c48fca0065c88028d484dd91` |
| `sword_plunge/__generated__/block/344.mcfunction` | `e8e47899981a80cccd254aa21e6d4a830c4810a71d6269abc87a659f5fe3c334` |
| `sword_plunge/__generated__/block/334.mcfunction` | `0c5b8f4ad41dc079e11f128570867c9a62f9b152dc8e6ce7e526f8712e45e2c9` |

## 状态与后续

D06 维持**实施中**。世界放火规则等用户选择；客户端模型/多人实机和 shockwave 效果仍需验收。GameTest 通过不等于客户端视觉验收。没有修改原数据包或真实存档；没有生成/交付新版号 JAR。
