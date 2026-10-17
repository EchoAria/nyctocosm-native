# D46：武器主副手入口及独立冷却

日期：2026-09-30。状态：**首片已实现并通过自动断言、构建与发布包审计；客户端/真实重连体验待验收。**

## 源代码依据

四种武器由原包主手 predicate/动画分支触发；Nullfissure swing/cast 各自计 20/25 个玩家 tick，Semblance swing/shoot 各自计 30 tick。Scythe/Wyrmsliver 没有可见副手能力分支。

| 原包相对路径（`_nyctocosm_source/datapack/data/` 下） | SHA-256 |
|---|---|
| `items/functions/playertick.mcfunction` | `9d8a9ac21fe8041db70a384777a34131798591dcb2005af00d3d39cf1145942e` |
| `items/predicates/scythe/holding_scythe.json` | `c719e2a23c2208f65db0c38d9fe868a8644dd505df6090a3f6a1cccb84402af7` |
| `items/predicates/wyrmsliver/holding_wyrmsliver.json` | `811511e8b1c0975a04302b1cb7544a95f17cf6d7865835800de491cda52d5f17` |
| `items/functions/__generated__/block/14.mcfunction` | `901aa46d6e413b782b6eec59912bb314e659981dd6c32976a3ad5972567fdcae` |
| `items/functions/__generated__/block/15.mcfunction` | `fb2502a2cfedc7e1d0080ea3c6f7dd747ce2b4382140f18dc3d58f17fff7477e` |
| `items/functions/__generated__/block/17.mcfunction` | `82c9fab8612e9768695ac109ac6a954e61c1896870b3a57f33fa6d39f41663c5` |
| `items/functions/__generated__/block/19.mcfunction` | `f49f641d2491f93676940460f53ee4a9223584217da2e586d52d348b0177a22a` |
| `items/functions/__generated__/block/21.mcfunction` | `83d8ada9cd0c125fc57ed66ed2567c370ba4167419a2b3b1f67194068cae0f01` |
| `items/functions/__generated__/block/23.mcfunction` | `3b8a05fba6a01026c771f9eb816f43da2bcf5d7ad3bcc3f633896d27f5d68138` |
| `items/functions/wyrmsliver/slash.mcfunction` | `fd7b336431af5ff882f28b9f7a8a8dd0bf3c72017620cb293e935972f32e48eb` |
| `items/functions/nullfissure/swing.mcfunction` | `bc593eb6d3927e08319cfebfdc25d9ac07375418c6030218d100b805ecd3679e` |
| `items/functions/nullfissure/cast.mcfunction` | `510f0545a31081c00566cd9d9bd49ae5bcd131a43bea5b320be81943c3c8f968` |
| `items/functions/semblanceofdoom/swing.mcfunction` | `7d4dfc4063873180ad738c3e295e01c270adfc775a15ac45481c56dc30b68739` |
| `items/functions/semblanceofdoom/shoot.mcfunction` | `76797c8d08bcdb4c5553d3a9109e07dd76b9edd2f4914249170dcbb7fbf9c0ab` |

## 实施

- 新增玩家级 `WeaponAbilityCooldowns`，以武器能力而非物品堆栈/快捷栏位置索引，服务端验证并保存在玩家 persistent data。只在线玩家 tick 推进；登录、计时变化时同步四个计数给客户端。
- Nullfissure swing/cast 分别 20/25 tick；Semblance swing/shoot 分别 30/30 tick。更换同类武器、移动物品或换手不会重置计时。保留 Scythe/Wyrmsliver 现有主手能力，并让它们的副手 use 返回 PASS。
- 为四种武器注册 `casting` 属性，主副手各看对应能力剩余时间；协议版本随新增冷却消息升至 3。
- Scythe 当前 20 tick 的原生 cooldown 保留，属于 D41 的冷却/失败代价决策，不能由 D46 擅自移除。

## 自动验证

隔离全套命令：

```powershell
.\gradlew.bat runGameTestServer --offline --console=plain "-PgameTestRunDir=run-d02-mode-d46-final2-20260930"
```

共 185 项 required GameTest，181 项通过。D46 两项 `weapon_ability_cooldowns` 通过，D02 `incurrence_mode` 通过；失败的四项为 `flame_geysers_caps_at_two_players_without_cross_spreading_node_groups`、`flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`、`emissary_wing_parts_take_ranged_spell_damage`、`serpent_beam_hits_high_targets_and_keeps_round_boundary`。本记录只说明失败名单，不把未通过的其他回归算作 D46 失败或宣称全套全绿。

独立构建：

```powershell
.\gradlew.bat build --offline --console=plain
python .\tools\audit_native.py .\build\libs\nyctocosm-native-0.3.0-native-alpha.12.jar
```

结果：`BUILD SUCCESSFUL`；审计 `errors=[]`，3,026 个 JAR entries，55,725,023 bytes，SHA-256 `6e72f036e87cae06566a24d078a0e1afbf4e8168a066afc8a8a8bb1d952271a4`。JAR 不含 `NativeGameTests` / `ClientSmoke`，仍为 `0.3.0-native-alpha.12`。构建前的 alpha.12 包已复制至 `run-d02-mode-d46-final2-20260930/pre-build-nyctocosm-native-0.3.0-native-alpha.12.jar`，旧 SHA-256 `48aef11ef9b55bbecb2b1209c158d78c76b03168098d7a30ea6c92bde7bb509b`。

## 未验收边界

- GameTest 覆盖计数、独立主副手时钟、玩家数据复制和 Scythe/Wyrmsliver 的真实 use 返回；尚未实机验证断线/服务端重启后的玩家数据恢复、不同客户端的持手冷却覆盖层或联机同步视觉。
- D41 的 Scythe 冷却/失败成本选择未获确认，因此既有 20 tick 未变。
- 不修改原始数据包、真实存档、相邻 Forge MDK、安装目录；没有提交或分发 JAR。工作副本无 `.git` 元数据；空白检查记于主线 TODO。
