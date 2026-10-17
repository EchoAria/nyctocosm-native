# alpha.16 最终重编译与关机记录

2026-10-02。用户明确要求“完成后编译一版，编译完成后帮我关机”。

- 产物：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.16.jar`。
- 大小：**56,026,248 bytes**，3,078 entries，Java class major61。
- SHA-256：`81a568b2acd673113e627372d8f87fea3a3c0230c501fc48517671359cc34239`。
- 命令：`gradlew.bat build --rerun-tasks --offline --console=plain`，7项任务完整重跑，**BUILD SUCCESSFUL，退出0**，build/reobf成功。
- JAR审计errors=[]；生产类集合和每个class字节均与alpha.15一致。alpha.15原SHA保持`7c9a89a5790962a7032ea32d8f82faace927306f4fad98cb50a7c964b037103d`。本次仅新版本重编译，未新增玩法选择；既有248/248回归及客户端验收范围见[alpha.15报告](SORCERER_REPAIR_ALPHA15.md)，不声称重新运行GameTest。
- 构建日志：`shutdown-build-alpha16-20261002-080804.log`；审计：`shutdown-audit-alpha16-20261002-080804.json`；结构化交付证据：`SHUTDOWN_BUILD_ALPHA16_20261002-080804.json`。
- 最后按用户授权启动隐藏PowerShell helper，读取构建成功证据并复核JAR SHA，等待20秒后请求Windows正常关机。helper：`shutdown-after-alpha16-20261002-080804.ps1`；请求返回码将写入`shutdown-request-alpha16-20261002-080804.log`。启动进程记录在`shutdown-launch-alpha16-20261002-080804.log`。

未自动安装Mod。D28等未选玩法和真实生存/多人/光影/卸载重启/FPS验收继续保持待确认或待验收。
