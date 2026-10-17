# alpha.15 — Sorcerer 逐招修复与最终视觉复查

2026-10-02。Minecraft1.20.1 / Forge47.4.13 / Java17。实现和自动验证完成，以下实机项待验收；不宣称全部51项或玩家所有法术已等价还原。

## 交付

- `D:\MinecraftModDev\DEV - 副本\nyctocosm-native\build\libs\nyctocosm-native-0.3.0-native-alpha.15.jar`
- **56,026,248 bytes**，3,078 entries。
- SHA-256：`7c9a89a5790962a7032ea32d8f82faace927306f4fad98cb50a7c964b037103d`。
- 新协议4，客户端和服务器使用同一alpha.15。未安装进用户游戏，未提交／推送；alpha.13/14 JAR及其hash不变。

## 用户反馈修复

承接[alpha.14原始视觉对照](VISUAL_REPAIR_ALPHA14.md)：巨蛇身体按42段独立视锥和光照绘制，头入实块仍画露出的身体；使者二阶段保留用户指定调色板、三阶段才变，后期飞行96格回航／128格硬限且无视线索敌；巫师initial_spawn默认smear1按源staffless隐藏；巨剑载体HEAD×10及256格视觉距离；不可见portal载体canUpdate(false)时独立客户端控制器仍生成演出；巫师死亡56/116各15条soul_swirl、20条void vein与ink/shockwave。

巨蛇20根大范围柱共享客户端600新粒子/tick，原持续广播4800/tick，数量减少**87.5%**，服务器伤害高度1..80不变。这是数量与包量优化，未做真实硬件FPS对比。

本版本继续补D29/D30/D32/D33，具体参数由源实际调用和独立原版命令夹具确定。

| Sorcerer招式 | 当前代码／自动验证 | 仍需确认或验收 |
| --- | --- | --- |
| Shard Hail D29 | 35帧升空球，每tick+1Y，30孵化18独立节点；41晚选80格全部玩家并spread，90清理；原版ArmorStand冰锥物理，落地第2/5tick命中/清理，Void10；仅球可反制 | D28命中迟缓；生存／多人 |
| Frozen Blades D30 | 34双节点，根世界Y+3、yaw±90/pitch20、前.5；每tick两次12点纵弧／一次.4，5/.5转向；Void14／源14反射／100tick最终步／反制／NBT；两侧充能 | D28命中迟缓；生存／多人 |
| Halo D32 | 八个独立年龄，65锁定70开火；出生score1，动画74..109发射；10反射／Halo反制；源35/70/38数值helper | 生存／多人／真实卸载 |
| Void Beam D32 | 20帧每名40格内玩家一个marker，30..57源fork转向，58清理；60半格双dx=0选箱，遇第一玩家／墙终止，源3反射 | 生存／多人；取消伤害仍阻止ray，不加未存在状态 |
| Dash Strike D33 | 37保存根前15格点，41..49每帧根tp1.5，49到点/双命中，50不动；Void15；进入位置粒子与伤害；点/yaw/gravity/finished保存 | D28迟缓；真实地形／生存 |
| Air Dash D33 | 前19帧升7.5，20锁点＋320点双图形，26..39根tp1.5／每tick35尘，不持续伤害；40在进入位置4格Void15并到保存点；源aj.frame39不被新版引擎读取 | D28迟缓；真实地形／生存 |
| Singularity Charge D31 | 已恢复.3移动／5/.5转向／3格近炸／100tick／5格Void20爆炸，源进入坐标与反射20，充能环／void swirl | D28迟缓，D22非默认属性 |
| Void Meteor D26 | 既有六发／25tick／三子步追踪及命中管线 | D28黑暗、完整独立tear视觉／反制验收 |
| Seeking Singularities D34 | 既有八发／.24／70..89寿命，源穿墙追踪；用户已明确伤害取消仍按命中给5秒迟缓 | 生存／多人与D22属性 |
| Ice Spikes D35 | 既有独立30臂×5环、计分与模型；本轮继续回归 | D28状态／真实卸载与多人 |
| Void Wave D36 | 既有世界Y+2、6格玩家脚部球，源直接keyframe粒子 | D28状态／其他helper／生存 |
| Javelin Swing D36 | 既有局部(0,1,2)、3格玩家脚部球，源合法slash/staff变体 | D28状态／完整演出验收 |

没有将D34／D45的单项批准扩大到D28全部招式。源独立地面节点的迟缓没有伤害事件，按其直接调用实现；命中附带状态仍待选择。

## 验证证据

- `gradlew.bat runGameTestServer build --offline --console=plain "-PgameTestRunDir=run-sorcerer-repair-alpha15-final2-20261002"`：**248/248 required通过，BUILD SUCCESSFUL，退出0**，含新增D29/30/32/33、既有D31及全部回归。build/reobf成功。
- 新D29红228项1失败→232全过；D30夹具暴露根显示矩阵、rotation重置和lookup精度→236全过；D32独立marker及多人fork红→242全过。D33六项最终全部通过；完整套件首次247/248，既有Celestial Smite旧共享场地6步早停；独立场地第一轮加载旁观者1步早停。源选择器含旁观者，改为测试region ticket加载，保持20/150断言，最终248通过。
- 独立新世界客户端六招烟测 `run-sorcerer-repair-alpha15-client-20261002`：`SORCERER_REPAIR_CAPTURE_COMPLETE`，退出0；实际冰锥模型见270、双刃纵弧398、Halo环85、两种ray100/483、Dash590、Air320点双图形675。由人工关键帧驱动，未冒充自然AI生存战斗。
- 最终视觉复查使用另一独立新世界 `run-visual-repair-alpha15-client-20261002`：`VISUAL_VARIANT_ASSERTIONS_PASS`／`VISUAL_REPAIR_CAPTURE_COMPLETE`，退出0。截图110/220/330确认阶段材质，410无开场弧片，490巨剑可见，585门粒子（载体client tick被禁仍可见），780头在石块中但身体仍显示，895死亡蓝金灵魂轨迹，1215实际Air升空数值图形可见。最终manifest保存日志／实现／源SHA。
- `python tools/audit_native.py build/libs/nyctocosm-native-0.3.0-native-alpha.15.jar`：**errors=[]**，1793模型、4451直接粒子cue、143 beam helper行；无runtime mcfunction／GameTest／ClientSmoke进包。编译器Java17；最终manifest核对每个class major≤61。
- Git diff whitespace检查退出0（只有项目已有LF/CRLF提示）。所有新运行目录独立，未改源数据包／源ZIP／旧MDK／真实存档／mods。

## 实机验收步骤与未测项

1. 在独立测试配置使用alpha.15；联网时双方版本一致。观察巨蛇头穿入石块时外露身体；使者一／二阶段材质相同、三阶段改变，8区块以上渲染距离观察后期回航。
2. 粒子设为全部，先用原版渲染检查portal开启、巫师自刺死亡、Sword Plunge巨剑及爆炸。原反馈的具体mod版本／粒子设置／shader组合尚未提供，不能声称该环境已复现并验收。
3. 生存依次观察上表十二招；在Dash／Air锁点后侧移，并测试Halo65锁定后横移；中途保存退出、重启／卸载区块，确认独立攻击续行且不重复生成／爆炸。
4. 真实多人／Forge保护mod取消伤害／光影兼容／巨蛇FPS对照尚未验收。D28、世界火焰、组队失败补偿、祭坛交易及镰刀成本等未选玩法，以及玩家法术D13–D23剩余差异，继续保持未完成状态。
