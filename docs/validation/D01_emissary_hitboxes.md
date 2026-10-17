# D01：Emissary 分阶段受击部位验证

日期：2026-09-30。状态：**代码完成，自动验证通过，待客户端实机验收。**

## 源行为与实现

- 原包阶段一由 `emissary/functions/ai/hitbox/mode/phase1.mcfunction` 及生成函数 `block/745.mcfunction` 建立四个 Size:4 slime 部位；locator 跟随命令位于 `emissary/functions/ai/hitbox`。阶段二切换为 Giant，阶段三/四切换为 Wither Skeleton。
- 导出器 `tools/export_emissary_damage_locators.py` 从只读 `_nyctocosm_source/datapack/data/animated_java/functions/ajemissary/zzzzzzzz/animations` 提取 `center`、左右 `middlewingtip0` locator，并同时核对 hitbox 函数。数据文件为 `src/main/resources/assets/nyctocosm/native_rigs/emissary_damage_locators.json.gz`。
- 资源包含 44 个动画、2601 帧和 2640 个原始函数文件，聚合源 SHA-256：`9856F853246135E80DF0C635F93A9A1B5C1D131EA11855ED652A4E4305C2AF82`。连续两次导出后的 gzip SHA-256 相同：`7E55D5ED5C5B6DED905C94D94E8B9195E6C1D2CB63464F2FAC812DDF144EBB3B`。
- `EmissaryDamagePose` 在服务端解析动画帧并插值 locator。`NativeBoss` 每 tick 更新四个 `EmissaryPart`：阶段一四个都可命中并按源局部偏移定位；阶段二仅 Giant 部位可命中；阶段三/四仅 Wither Skeleton 部位可命中，按 `center` 减去 1.3 格定位。按同版本原版 Size:4 Slime GameTest 实测并与代理包围盒逐轴比较，尺寸为 **2.0808×2.0808**。原阶段 36 中的 1.02×1.02 是计算错误：漏乘 `Slime.getDimensions()` 的基础尺寸；阶段 39 记录修正与复验。
- `Combat` 对面积/线段查询检测 Emissary multipart 包围盒，`SpellBolt` 解析 `PartEntity` 父实体；伤害仍进入父 Boss 的正常 Forge 受伤路径。

## 自动验证

GameTest `emissary_damage_parts` 的五项验证覆盖：阶段一四个可选取部位、源帧 0 locator 与局部坐标、伤害转发、面积法术、远程 `SpellBolt`、原版箭碰撞，以及二至四阶段的命中实体类型、尺寸和中心 locator。

最新完整复验命令（含 D27，并重新覆盖 D01）：

```powershell
.\gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-d27-verified6-20260930"
```

结果：**131/131 required GameTest 通过，BUILD SUCCESSFUL**。包括 D01 五项部位测试、D27 直接箭/SpellBolt、范围/线段命中；Serpent 对照测试在 420 tick × 41 段、17,220 个位置上的最大误差 **0**。夹具执行原 teleport 子命令；为避免手动移动 marker 后实体 section 查询失效，距离下限由夹具直接应用，`facing entity ... feet` 使用已知前置 marker 的脚部坐标。

`tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`：**errors=0**；1 个 locator bundle / 2601 帧，10 个骨骼 rig / 5607 动画帧，1778 模型、247 音效事件，JAR 3011 项。审计确认压缩 locator 资源与 JAR 内副本逐字节相同，且无运行时 mcfunction、GameTest 或客户端烟测类。

JAR：`build/libs/nyctocosm-native-0.3.0-native-alpha.12.jar`，55,375,927 字节，SHA-256：`c03ca13facd558963e4fae06aa3f6e65bdd9f305c1420e5e96884d2b0fea10ad`。

## 尚未验收

没有进行客户端视觉对照或真实生存战斗。仍需核对四翼可见位置、阶段转换瞬间的命中体、主/侧/背面近战与箭、瞄准模型空隙是否误命中，以及多人/重连后的部位同步。D01 自动通过不代表这项实机验收已经完成；D27 的 Serpent 部件服务端自动验证见独立记录，客户端实机验收仍待完成。

## D07 共享资源更新（2026-09-30）

D07 将同一压缩 locator bundle 从 3 个受击部位 locator 扩充为 9 个：保留原来的 `center`、`leftmiddlewingtip0`、`rightmiddlewingtip0`，并新增六个 Wing Blast 第 12 帧使用的 `wingtip1` locator。D01 所依赖的三个 locator 坐标与动画采样行为未改；`audit_native.py` 现对九个 locator 的每个动画帧做完整集合校验。当前资源仍为 1 个 bundle / 2601 帧，文件由 D07 阶段更新。D07 自动验证和实机缺口见 [Wing Blast 六路射线](D07_wing_blast_rays.md)。
