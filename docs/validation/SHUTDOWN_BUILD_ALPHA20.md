# alpha.20 编译交付与用户要求关机

2026-10-04。用户明确要求完成后编译并关机。

- 版本：0.3.0-native-alpha.20；平台MC1.20.1 / Forge47.4.13 / Java17，协议6。
- 构建：`gradlew.bat build --offline --console=plain --rerun-tasks`，**BUILD SUCCESSFUL，退出0，27s，7任务执行**。
- 包：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.20.jar`；**56,178,050 bytes／3092 entries／Java major61**。
- SHA-256：`28656ccfc2c9d87067b0e9f27647132b5db80bc760886e2327d88b316e5022d7`。JAR审计errors=[]，没有测试/烟测类及runtime mcfunction。
- 与alpha.19逐条ZIP内容比较，仅META-INF/mods.toml版本变化，所有生产class和资源字节完全一致；13个旧JAR摘要未变。
- 沿用上一轮288/288 GameTest和42张隔离客户端证据，本轮没有再次执行GameTest或启动游戏；真实生存/多人/shader/FPS及未批准玩法仍待验收。
- 构建、audit、before/manifest/receipt日志：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\docs\validation\shutdown-alpha20-20261004-044310`。TODO阶段91已追加。
- 全部记录保存后，由隐藏helper复核构建日志、JAR、审计及上一轮回归SHA，等待20秒后执行正常Windows关机，命令shutdown.exe /s /t 0。启动/验证/请求状态见shutdown-launch.json及shutdown-receipt.json。
