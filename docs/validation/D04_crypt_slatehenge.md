# D04：crypt 到地表 Slatehenge

## 源行为

- `structures:crypt/generateslatehenge` 在触发 marker 位置执行 `positioned over ocean_floor`，检查 200 格内带 `crypt`、`slatehenge`、`structure` 标签的实体；不存在时放置 `nyctocosm:night_wastes/slatehenge`，之后无条件移除触发 marker。
- Slatehenge 结构来自唯一的 `nyctocosm:slatehenge` rigid 模板池元素，处理器按 0.75 保留 basalt、deepslate tile/stair/wall 方块，并使用结构旋转。
- Slatehenge 的 marker 在所在方块非空气时每 tick 生成 2 个 `firework` 粒子（1 格 XYZ 扰动、速度 0）；方块为空气时移除 marker，本 tick 粒子命令仍执行。
- 实际调用来自 crypt 结构放置后的结构 marker tick。静态 Slatehenge 世界生成与此动态链路分开保留。

## 实施

- `tools/nbt_assets.py` 只对白名单中的两个源模板及精确 tag 集合做转换：crypt hallway 末端 marker → `nyctocosm:crypt_slatehenge_trigger`；Slatehenge marker → `nyctocosm:slatehenge_effect`。转换后的 NBT 只保留 `id`，不携带命令、原 tags 或其他源 NBT。新增 `tools/import_crypt_structure_events.py` 仅重导入这两份模板和对应报告项，避免重跑全量资产导入。
- 原生触发器使用 `OCEAN_FLOOR` 高度图、200 格内已加载的 `SlatehengeEffect` 实体、随机旋转和同源 BlockRotProcessor（0.75、相同方块集）放置唯一 Slatehenge 模板。与源函数一样，触发 marker 在一次判定/放置尝试后被消费；已处理 crypt 坐标保存在 dimension SavedData，读档后保持幂等。
- Slatehenge 效果实体在实体标记方块非空气时持续发送源粒子参数；为空气时仍发送本 tick 粒子后移除。两类实体均有空客户端 renderer；服务器持有生成、去重与结束状态。
- 不改静态 worldgen JSON，不追溯修改既有区块或已保存结构实例。

## 自动验证

- 新增 GameTest 覆盖模板中的两个实体 ID、200 格范围包含端点、solid/air 特效生命周期、附近 Slatehenge 防重、处理坐标 SavedData 序列化，以及无邻近地标时从真实模板放置并生成唯一原生效果实体。
- 命令：`gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d04-final2-20260930"`。
- 结果：**128/128 required GameTest 通过；BUILD SUCCESSFUL**。动态放置夹具位于隔离 GameTest 存档中的 `nyctocosm:emissary_domain`，预加载测试范围区块，无永久强加载或用户存档修改。
- 同一整套回归中的远距离 Serpent beam 夹具原先偶发受区块实体查询范围及无可站立地面影响。夹具现临时加载目标区块、搭建并在 `finally` 中恢复地面，再次完整运行通过；未放宽战斗断言或修改 Serpent 伤害逻辑。
- `python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`：**errors=0**；检查 2 个 native structure event、0 个运行时 mcfunction、JAR 3011 entries。JAR 大小 **55,375,621 bytes**，SHA-256 `9fc3d7671f00ec51b1162b23987960d46af43050198d8af3761650877d5a34c4`。

## 待实机验收

新建/未生成的世界中探索包含 crypt 末端的结构，确认触发时 Slatehenge 出现在海底地表高度；在 200 格内已有 Slatehenge marker 时不生成重复结构；标记所在方块非空气时持续产生烟花粒子，改为空气后终止。需客户端确认旋转方向和残损方块分布与源包观感相符。旧世界既有结构不会自动注入新 marker。
