# alpha.19：使者悬浮待机与飞行、巫师抗击退、巨蛇转弯

日期：2026-10-04。平台 Minecraft 1.20.1 / Forge 47.4.13 / Java 17；协议6，客户端与服务器使用同一版本。

## 范围与状态

本轮用户确认“舒张身体”指使者出场后的悬浮第一阶段。按最新三个问题实施，D18/D27/D38相关切片完成代码及隔离客户端检查，最终288/288回归及发布包审计通过，摘要见下文；整项D18及37招对照仍未全部验收。本轮不执行关机。

| 用户问题 | 本轮修正 | 原包依据及边界 |
|---|---|---|
| 出场后待机应抱臂、翅膀护在身前 | 新增61帧 `phase1_idle_guarded`，抱臂使用原spawn75经idle torso对齐，翼位固定为原idle0；身体/头腿尺寸及轻微呼吸保留。仅客户端待机选用该clip | 原phase1_idle矩阵本已正确，原clip保留；这是用户明确要求的姿态调整，不声称源默认整段就是该姿势。原HEAD半周变换正确，删除它的试验已撤回 |
| 末期飞行轨迹与原包有偏差 | 恢复可达FlyTimer20..59、任一60格内玩家的选招门槛、无选招AttackTimer0..29、全部航点依创建顺序参与正常/quick转向 | `ai/subtick → block736/737`；phase4 choose/661/662。先前alpha.17误删定时换路，本轮更正；自动加速的 `@a[tag=emissary,tag=flytarget]`仍通常不可达，两者不能混为一谈 |
| 飞行高度与弹幕时机 | 恢复起飞1..5帧每帧+.80625；降落0..4每帧−.80625，减速0/3/6/8/9/10/11；phase4活体保持source marker无重力，保留Godrays5..9帧共+7.5升高 | 起飞14帧启飞、降落4帧停飞。降落保留已有防埋地的地表夹紧；96回航/128上限继续保留。Rain航点为玩家+30，10..44每帧四弹从当前root发射；源无最低发射高度判断，本轮没有增加该玩法规则 |
| 巫师被玩家击退 | 注册KNOCKBACK_RESISTANCE=1；仅kind1屏蔽knockback，覆盖旧原生NBT仍有.9的实体 | 原模型marker与受击代理分离；原生冲刺/击退II玩家剑攻击实测造成伤害但速度为0。Forge伤害流程继续保留；自己的dash仍正常 |
| 巫师开场两片弧片 | 仅 `ajsorcerer/initial_spawn/smear1`不渲染，保留法杖/双手和战斗挥杖弧片 | 源231/252确有smear变体；按本轮用户明确要求去掉开场的双面平面，不谎称原包从未绘制 |
| 巨蛇急转时整条身体横向摆动 | 头部位置/朝向与所有身体段使用同一BODY快照；移除身体全链叠加滞后实体头部位置差 | 服务端原41节逐节追逐链保留，17220采样原命令对照继续通过；未换成另一套历史路径算法。独立身体裁剪/光照、20受击部件及600粒子共享预算保留 |

## 代码与取证

- `NativeBoss.java`：飞行选择/计时/多航点转向/起落根位移/phase4重力及巫师击退。
- `NativeMod.java`：仅巫师的击退抗性注册值。
- `SerpentBody.java`、`client/SerpentWorldRenderer.java`：相同快照驱动头身及其角度，保存HeadYaw/HeadPitch。
- `BossVisuals.java`、`client/ClientEvents.java`：开场smear可见性。
- `client/RigLibrary.java`、`assets/nyctocosm/native_rigs/ajemissary.json.gz`：护身待机采样。
- `tools/export_emissary_guarded_idle.py`：以只读源姿态生成独立clip；原source两帧逐float核对和SHA在 `feedback-alpha19-20261004/guarded-idle-import.json`。
- `NativeGameTestsFeedback19.java`新增12项；旧NativeGameTests仅更正已证伪的换路与起飞位移预期，保留原用例和边界。
- `ClientSmokeFeedback19.java`为隔离客户端夹具；全部NativeGameTests*/ClientSmoke*由JAR排除。临时feedback_guard_*动画和对应开发开关已清理。

## 验证与失败记录

所有命令在 `nyctocosm-native`，JAVA_HOME为 `C:\Users\jzy19\.jdks\ms-17.0.19`，使用offline。完整日志在 `docs/validation/feedback-alpha19-20261004/`，每轮独立world/runDir。

| 记录 | 结果及解释 |
|---|---|
| red.log | 281项：4个新反例按预期失败（巫师击退、body受头部packet拖动、>60降落、源计时器不更新），原276项及idle矩阵检查通过 |
| green1.log | 285项：284通过，旧“timer不得换路”预期失败；查源纠正并加到期断言 |
| final.log | 第一版287/287及build通过，3m48s；后来发现遗漏起落根位移，不用这份证明最终飞行修复 |
| flight-red.log | 增至288项；新增起飞根位移反例失败，另有旧Shard Hail场地entity-ticking加载超时；保留两项失败及3m25s输出 |
| flight-final.log | 287/288，新增空中下降夹具的Y60低于实际地表63，碰到防埋夹紧边界。后续将空中场景移到实际地表上方40格，并另加地表防埋断言；没有改伤害/选招或放宽位移误差 |
| flight-final2.log | **288/288 required通过、BUILD SUCCESSFUL、退出0、3m22s**；地表63/空中起点103，下降后98.96875，与源5*.80625一致；单独防埋地表断言亦通过 |
| client-flight-fixed.log | 最新生产代码的客户端正常结束0、BUILD SUCCESSFUL 2m34s，无Server task/渲染异常；42张截图；统计与摘要见client-summary.json/manifest.json |

最终回归入口：

```powershell
.\gradlew.bat runGameTestServer build --offline --console=plain -PgameTestRunDir=run-feedback19-flight-final2-20261004
```

最新客户端入口：

```powershell
.\gradlew.bat runClient --offline --console=plain -Pfeedback19Smoke -Pfeedback19RunDir=run-feedback19-flight-fixed-client-20261004
```

### 客户端实际范围

最新场景65/90/115核查护身待机，390/394核查挥杖时法杖可见且弧片移除，650/700核查头向发生90°变化后的身体连续弯曲。730–2299为真实AI选招，玩家免伤/可飞，摄像机独立跟随；自然弹幕从1751、1819、1892开始各35个发射帧，发射root比玩家高约18.8–21.2格，截图1753/1831/1904等可见高空弹幕。2300以后另设高空Rain演示，属于手动夹具，不能充当自然选招证据。

早期静态镜头未跟上Boss、方向探针、source ItemDisplay参考、多个护身姿态试验和首次参考模型夹具编译失败均保留。`client-final.log`正常退出但没有完整飞行画面，`client-follow.log`还使用修重力前版本；最终判断使用 `client-flight-fixed.log`。单幅截图不能证明整段无扭动/丢帧。

## 最终包

版本：`0.3.0-native-alpha.19`。路径：`D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.19.jar`。

**56,178,052 bytes／3092 entries／Java class major61**；SHA-256：`2e8e7c7928dc4e6f04e39083ba39397cf97e81e14b6652c0720774d56159506b`。release-audit.log及jar-audit.json：errors=[]，无测试/烟测类及runtime mcfunction，guarded rig与工作区资源字节一致。审计脚本：`tools/audit_native.py`、`tools/package_feedback19.py`；manifest确认只读源29657文件及12个旧JAR均未变，无基线生产文件删除，并记录修改文件与测试/客户端/截图SHA。不自动安装、提交或推送。

## 实机验收

实现和自动验证完成，以下实机项仍待验收：

1. 新测试存档观察使者出场后的悬浮待机，应抱臂护身；攻击结束再观察，身体尺度和材质时序不应改变。
2. 战斗进入末期高速飞行（内部phase4），持续观察起落、巡航、Godrays升高和高空Rain。保留八区块内约束；随机数使用Java RNG，不能承诺与源LCG逐次轨迹相同。
3. 巫师开场法杖挥舞时不显示两片弧片；普通/冲刺/击退附魔攻击不推走模型，仍能造成伤害；巫师自己的位移技能正常。
4. 跟随巨蛇连续急转、升降和穿地，观察整条身体沿追逐链弯曲，头埋块后身体仍能独立显示；联机延迟下的同步效果另测。

未测：完整原包/mod并排整场录屏、真实多人/保护模组/shader、真实专服重启/卸载/晚加入、FPS。D28/C03、D20、D02组队离线、D14chance、D41等待选规则没有在本轮获批准。
