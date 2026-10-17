# D18 使者 locator 与飞行姿态阶段验证

日期：2026-10-01  
状态：已实现定位点/根姿态切片；自动验证通过，待客户端与多人实机验收。

## 源行为与实现

使者源动画通过命令 locator 决定多个攻击生成位置。此次将导出集合从 10 扩到 16 个 locator，覆盖 44 段动画、2601 帧；新增左右手、Seeking Missiles 的左右翼尖节点和 Grapple 使用的右翼尖节点。重导出后与此前 D16 数据比较，旧 10 个 locator 在 44 段动画中的 26,010 个样本逐值不变。源资源摘要：`9856F853246135E80DF0C635F93A9A1B5C1D131EA11855ED652A4E4305C2AF82`。

新增服务端安全的 `EmissaryPose.localPoint`，与客户端 `RigTransforms.orientation(yaw,pitch)` 使用一致的 Minecraft yaw/pitch 基底；使者模型根姿态按上一帧至当前帧插值应用 XRot。NativeBoss 已接入 Fireball 左右手、Lightning Javelin 右手、Seeking Missiles 七个发射节点、Meteor Shower 与 Wingbeat 中心、Divine Blast/Godrays 中心、Grapple 的 wingtip3 座椅点，以及 Sword Plunge 的右手到刀尖粒子轨迹。Wing Blast 与 Flame Geysers 保留已有轨迹来源。

`audit_native.py` 现要求完整 16 locator 集合并检查采样帧完整性、变换有限值。定位点导出和审计所用文件摘要：
- `tools/export_emissary_damage_locators.py`: `00314038861afd580a4b9e83c262d131e01654af90340ed74b2625d662da9f86`
- `tools/audit_native.py`: `a6b42afab5ba0834342deef0808760557b70fded54b3295350e7751fc182f6fe`
- `emissary_damage_locators.json.gz`: `40ea5feef9d71c4772eb94e128ec81a700087bc1bfb0f1800207df816d63d6ad5`

## 自动验证

两项 D18 GameTest 均通过：
- `emissary_rig_pitch_matches_server_locator_transform`：正交 yaw、正负 pitch、局部基向量和半帧插值。
- `emissary_launch_nodes_follow_exported_animation_locators`：Fireball 两只手的 frame 8/18 起点、七个 seeker 节点与 Grapple 座椅 locator。

完整运行 `run-d18-pitch-nodes-verified3-20261001`：209 项中 205 项通过。4 项失败为 `expulsion_uses_inclusive_horizontal_target_feet_radius`、`flame_geysers_caps_at_two_players_without_cross_spreading_node_groups`、`flame_geysers_frame_40_creates_fourteen_spread_ground_nodes_with_independent_timers`、`serpent_acquires_player_through_wall_inside_source_range`；均未调用本阶段新接入的 D18 路径。日志：`nyctocosm-native/run-d18-pitch-nodes-verified3-20261001/logs/latest.log`。

独立 `gradlew.bat build --offline --console=plain` 成功。alpha.12 包审计 `errors=[]`，3037 entries、55,933,196 bytes；SHA-256 `34054f771fa9586bee0fc7d506804c9367b70a96d03f0bcb45c78f6254cc6c67`。包不含 GameTest/ClientSmoke。重建前包 `c06535ea76f3c1027c3332b51bdcca8b207e03c810763b204fc39f20ff2c96ee` 已备份到 `nyctocosm-native/run-d18-pitch-nodes-verified3-20261001/pre-build-backup/`。审计快照为 `nyctocosm-native/docs/native-audit-before-d18-pitch-20261001.json` 与 `native-audit-after-d18-pitch-20261001.json`。

## 待实机范围

客户端录屏仍需逐招确认手/翼发射位置、俯仰和转向时模型与命中体的贴合；多人遭遇、客户端迟入及跨区块卸载/重载连续性没有由这些 GameTest 代替。未经本阶段验证的攻击 locator 也不能视作已接入。alpha.12 未升版；未修改源包、真实存档、相邻 MDK 或安装目录，无提交或分发。

下一优先项为 rank45 D19：使者 Fireball 每 tick 两次一格子步进与 60 tick 终止。

