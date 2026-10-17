# alpha.9 验证记录

- 基线：Git `193a7d8`；原 alpha.8 的 44 项 GameTest 通过。
- 修复前回归：`../p0-red-gametest-console.log` 可见盘旋目标错误、Laser 恢复空指针、首次出场遗漏 rise；另有旧随机运动测试把曲线路程误当净位移，已修正其断言。
- 修复后：`../p0-gametest-console.log` 中 `All 61 required tests passed`。
- JAR 审计：`../native-audit.json`，`errors` 为空，运行时 mcfunction 为 0，无开发测试类。
- 客户端：`../p0-client-console.log` 中出现 `P0_PLANE_TRANSFORM_MATCH`、`P0_SERPENT_PITCH_TRANSFORM_MATCH` 与 `P0_SMOKE_COMPLETE frames=811`，核对法环和四种蛇头俯仰角的原版头部变换。

`p0-continuous.mp4` 为 10 fps、81.1 秒的连续无声画面；`p0-contact-sheet.png` 仅作索引。
视频的 0–29 秒展示 Sorcerer 升起、法环旋转与破杖消失：**右侧为原生版，左侧为原模型/原盔甲架头部姿态参考**。
29–54 秒为 Serpent 盘旋/Dive/中途保存恢复；54–66.5 秒为 Laser；66.5 秒起为 Beam。
Camera 在 Laser 时跟随蛇头，以便位于现有粒子发送范围内；Beam 时切换为近地观察视角。

`p0-native-trace.csv` 保存 1670 tick 的位置、动画、标记链、状态和目标 UUID。
`p0-capture.json` 记录帧数、场景分段、视频哈希及限制。视频和索引图为可再生成文件，Git 忽略它们。

重新生成：

```powershell
.\tools\run_p0_smoke.ps1 -JavaHome 'C:\Users\jzy19\.jdks\ms-17.0.19'
python tools/package_p0_capture.py
```

需要现有的可丢弃 `run/saves/NativeSmoke` 世界。脚本临时启用仅开发使用的 CMD 19 参考资源包，并在退出时恢复客户端选项。

没有完整原数据包环境的同场景战斗录像；此处的原版盔甲架只验证法环渲染，GameTest 内的原生命令只验证指定运动链。
真实多人、第三方生物、声音逐拍和跨硬件性能仍未验证。逐节身体/光柱的来源、已修正项和剩余验证边界见 `../SERPENT_BEHAVIOR.md`。

身段对照另见 `serpent-source-comparison.json` 与 `serpent-body-source-trace.csv`：123 个原移动函数、420 tick、17,220 个位置，最大误差 0。
测试逐个读取原函数的传送子命令并由 Minecraft 执行；夹具直接评估原选择器的最小距离门槛，并以已知前置 marker 的脚部坐标替代 `facing entity ... feet`，避免手动跨区块移动 marker 后实体分区查询过期。原文件 SHA-256 存在摘要中；完整原数据包没有装入模组运行环境。
`python tools/package_serpent_comparison.py` 从最新 GameTest 结果重新归档这些记录。

## alpha.10：回响碎片与传送门

- `../resonance-gametest.log`：67 项测试，覆盖真实投料施法、门洞命中、延迟开启、重载、重复施法、破框和粒子全时序。
- `../resonance-client.log`：`RESONANCE_SMOKE_COMPLETE frames=341 sounds=14`。
- `resonance-portal.mp4`：34.1 秒、10 fps 连续客户端画面，分别激活 X/Z 两个方向的空门框。
- **录像音轨按 `resonance-sound-events.csv` 中的客户端音效事件时间与原始 OGG 合成，非系统录音；混音音量仅供预览。**
- `resonance-trace.csv`：720 tick 的激活进度和门面状态；`resonance-capture.json` 保存验证内容、帧数和视频哈希。
- 重新生成：运行 `tools/run_resonance_smoke.ps1`，再执行 `python tools/package_resonance_capture.py`。

## alpha.11：当前传送门录制

当前 resonance-portal.mp4、截图与 trace 已更新为 alpha.11：两个方向均带幽匿脉络，69 项游戏测试通过；resonance-client.log 记录六个实际渲染抖动阶段、341 帧画面及 14 次音效事件。视频音轨仍为原始 OGG 按客户端事件时间合成，不是系统录音。巨蛇 IN_WALL 免疫及正常战斗受伤由 portal-polish-gametest.log 验证。
