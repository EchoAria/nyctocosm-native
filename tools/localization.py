"""Native UI translations. Minecraft supplies en_us fallback for every other locale."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
ENTRIES={
 'nyctocosmic_ash':('Nyctocosmic Ash','夜域灰烬','A common remnant found by mining gravel in the Nyctocosm.','在夜域挖掘沙砾时常见的残留灰烬。'),
 'seelic_essence':('Seelic Essence','灵性精华','Dais ingredient for the coil and weapons.','灌注祭坛材料，用于制作位面线圈和武器。'),
 'null_splinter':('Null Splinter','虚无碎片','A sorcerer drop used to forge Nullfissure.','巫师掉落的材料，用于锻造虚无裂隙。'),
 'void_shard':('Void Shard','虚空晶片','A fragment of void power. No dedicated recipe yet.','蕴含虚空力量的晶片；当前版本暂无专用配方。'),
 'nightmare_scale':('Nightmare Scale','梦魇鳞片','A serpent drop used to forge Wyrmsliver.','梦魇巨蛇掉落的材料，用于锻造龙鳞之刃。'),
 'dais':('Dais of Inculcation','灌注祭坛','Place and use to combine two ingredients.','放置后右键打开，按配方灌注两种材料。'),
 'soul_bottle':('Soul Bottle','灵魂瓶','A focus for a stationary seelic field.','展开固定灵场的施法媒介。'),
 'planar_coil':('Planar Coil','位面线圈','A focus for a mobile seelic field.','展开跟随自身移动的灵场。'),
 'seelic_scythe':('Seelic Scythe','灵魂镰刀','Use: dash at a health cost. Kills collect souls.','右键消耗生命短距穿行；击杀可收集灵魂。'),
 'wyrmsliver':('Wyrmsliver','龙鳞之刃','Use: launch a Nightmare slash.','右键释放梦魇斩击。'),
 'nullfissure':('Nullfissure','虚无裂隙','Use: main-hand sweep; off-hand Void beam.','右键：主手近距挥击，副手释放虚空射线。'),
 'semblanceofdoom':('Semblance of Doom','末日之影','Use: main-hand sweep; off-hand Dissonance shot.','右键：主手近距挥击，副手发射失谐攻击。'),
 'ancient_journals':('Ancient Journals','古老日记','A record left by the Researcher. Use to read.','研究者留下的记录；右键阅读。'),
 'catechism':('Catechism of the Seelic Mystics','灵性秘教问答','A volume of the seelic mystics. Use to read.','灵性秘教的典籍；右键阅读。'),
 'dreams':('Dreams','梦境手记','A record of dreams. Use to read.','记述梦境的书卷；右键阅读。'),
 'axiotopology':('Axiotopology','公理拓扑学','A treatise on the planes. Use to read.','关于位面的论著；右键阅读。'),
 'adherent_serpent':('Adherent: The Serpent','追随者手记：巨蛇','Lore concerning the Nightmare Serpent.','关于梦魇巨蛇的记述；右键阅读。'),
 'adherent_sorcerer':('Adherent: The Sorcerer','追随者手记：巫师','Lore concerning the Sorcerer.','关于巫师的记述；右键阅读。'),
 'adherent_emissary':('Adherent: The Emissary','追随者手记：使者','Lore concerning the Emissary.','关于使者的记述；右键阅读。'),
 'artifact_transposition':('Artifact of Transposition','移位遗物','Your seelic fields follow you.','使你的灵场跟随你移动。'),
 'artifact_amelioration':('Artifact of Amelioration','疗愈遗物','Breaking your nearby Hypertrace restores health.','击碎附近属于自己的超迹，可恢复生命。'),
 'artifact_levitation':('Artifact of Levitation','悬浮遗物','Sneak while falling: spend 3 trace charges to levitate.','下落时潜行，消耗 3 点超迹充能暂时悬浮。'),
 'artifact_absorption':('Artifact of Absorption','汲取遗物','Void damage heals you; your traces weaken enemies.','造成虚空伤害可回血；你的超迹使敌人易受虚空伤害。'),
 'artifact_accumulation':('Artifact of Accumulation','积聚遗物','Nearby Hypertraces improve trace generation chance.','附近的超迹提高新超迹的生成概率。'),
 'artifact_expulsion':('Artifact of Expulsion','驱逐遗物','Dissonance kills and broken traces cause explosions.','失谐击杀或击碎自己的超迹，会引发爆炸。'),
 'artifact_extraction':('Artifact of Extraction','萃取遗物','Breaking your Hypertrace grants a soul to a focus.','击碎自己的超迹，可为施法媒介补充一个灵魂。'),
}
ELEMENTS={'dream':('Dream','梦境'),'radiance':('Radiance','光辉'),'resonance':('Resonance','共鸣'),'nightmare':('Nightmare','梦魇'),'void':('Void','虚空'),'dissonance':('Dissonance','失谐')}
for element,(en,zh) in ELEMENTS.items():
 ENTRIES['artifact_protection_'+element]=(f'Scroll of Protection: {en}',f'{zh}防护卷轴',f'Nearby trace charges reduce incoming {en} damage.',f'附近的超迹充能可降低受到的{zh}伤害。')
MOBS={'serpent':('Nightmare Serpent','梦魇巨蛇'),'sorcerer':('The Sorcerer','巫师'),'emissary':('The Emissary','使者'),'death_walker':('Death Walker','死亡行者'),'nightmare_apparition':('Nightmare Apparition','梦魇幻影')}
for mob,(en,zh) in MOBS.items():ENTRIES[mob+'_spawn_egg']=(en+' Spawn Egg',zh+'刷怪蛋','Use on a block to spawn '+en+'.','对方块右键生成'+zh+'。')
UI={
 'itemGroup.nyctocosm':('Nyctocosm Native','Nyctocosm · 夜域'),
 'tooltip.nyctocosm.artifact_passive':('Carry to activate. Identical artifacts do not stack.','携带即可生效；相同遗物不叠加。'),
 'tooltip.nyctocosm.focus_souls':('Souls: %s','灵魂：%s'),
 'tooltip.nyctocosm.focus_use':('Use to toggle the field. Costs 1 soul outside Creative.','右键展开或关闭灵场；非创造模式消耗 1 个灵魂。'),
 'tooltip.nyctocosm.focus_cast':('Drop a reagent inside your field to cast a spell.','在自己的灵场内丢出材料施法。'),
 'tooltip.nyctocosm.lore_unread':('Unread: use to reveal the next entry.','未解读：右键揭示下一篇章节。'),
 'tooltip.nyctocosm.lore_chapter':('Chapter %s / %s','章节 %s / %s'),
 'item.nyctocosm.chapter_title':('%s — Chapter %s','%s——第 %s 章'),
 'message.nyctocosm.no_souls':('Not enough souls. Collect them with the Seelic Scythe.','灵魂不足：使用灵魂镰刀击杀生物收集灵魂。'),
 'message.nyctocosm.field_open':('Field active: drop reagents to cast; sneak and look down to cancel.','灵场已展开：丢出材料施法；潜行并向下看可取消。'),
 'message.nyctocosm.casting':('Casting %s: %s ticks','正在施放%s：剩余 %s 刻'),
 'message.nyctocosm.dimension_unavailable':('The destination dimension is unavailable.','目标维度不可用。'),
 'message.nyctocosm.no_safe_landing':('No safe landing nearby. Teleport cancelled.','附近没有安全落点，传送已取消。'),
 'message.nyctocosm.no_safe_spawn':('No safe landing near your spawn. Recall cancelled.','重生点附近没有安全落点，召回已取消。'),
 'message.nyctocosm.no_safe_return':('No safe landing near the return point. Teleport cancelled.','返回点附近没有安全落点，传送已取消。'),
 'message.nyctocosm.incur_unavailable':('Incurrence cannot answer in this biome.','此地无法回应降临法术。'),
 'gui.nyctocosm.dais_title':('Dais of Inculcation','灌注祭坛'),
 'gui.nyctocosm.dais_hint':('Materials → Inculcation','放入材料 → 灌注'),
}
SPELLS={'luminescence':'辉光','resonance':'共鸣','evanescence':'归返','incurrence':'降临','abhorrence':'憎恶','forbiddance':'禁制','gravitance':'引力','subsistence':'滋养','reflectance':'反射','submergence':'沉陷','adamance':'坚守','incandescence':'炽燃','resistance':'抵御','transmittance':'跃迁','vacance':'排空','repellance':'斥离','fulminance':'雷霆'}

# The original pack stores every book page as a translation component.  Keep the
# Chinese edition here (rather than editing the generated language file) so asset
# re-imports cannot silently erase the translation.
LORE_ZH={
 'item.nyctocosm.prefix.lore':'典',
 'item.nyctocosm.book_number.1':'I',
 'item.nyctocosm.book_number.2':'II',
 'item.nyctocosm.book_number.3':'III',
 'item.nyctocosm.book_number.4':'IV',
 'item.nyctocosm.book_number.5':'V',
 'item.nyctocosm.book_number.6':'VI',
 'item.nyctocosm.book_number.7':'VII',
 'item.nyctocosm.book_number.8':'VIII',
 'item.nyctocosm.book_number.9':'IX',
 'item.nyctocosm.book_number.10':'X',
 'item.nyctocosm.book_number.11':'XI',
 'item.nyctocosm.book_number.12':'XII',
 'item.nyctocosm.book.generation':'坚志之作',

 'item.nyctocosm.journal':'古老日记',
 'item.nyctocosm.journal.author':'研究者',
 'item.nyctocosm.journal.lore':'一本布满尘埃、磨损严重的日记',
 'item.nyctocosm.journal.page1':'这本日记的秘密逐渐显现……',
 'item.nyctocosm.journal.title':'记录 %s：%s',
 'item.nyctocosm.journal.1.title':'踏入',
 'item.nyctocosm.journal.1.page1':'我原以为自己必死无疑。然而，没有哪则神话把死后世界描绘得如此荒芜、如此多岩、如此死寂。至少我从未听说过。\n我真的敢相信这不可能之事吗？尽管希望渺茫，§k门扉§r竟真的完成了它的使命？我们经年累月的',
 'item.nyctocosm.journal.1.page2':'辛劳与苦难，终于换来了成功？\n即便身处此境，我仍容许自己独自静静欣喜片刻。我们成功了。夜域已在我们的掌握之中。',
 'item.nyctocosm.journal.2.title':'初步考察',
 'item.nyctocosm.journal.2.page1':'§n观察一：§r夜域是黑暗的。它的天空黯淡而无光。一个仿佛太阳的天体不断回旋、剧烈搏动，在虚空中灼出两道光痕。\n\n§n观察二：§r夜域已经死亡。散布于夜景中的那些生长物都是石化的尸骸，',
 'item.nyctocosm.journal.2.page2':'是昔日之物留下的残迹与影子。\n\n§n观察三：§r那种被我们称为“幽匿”的共鸣生长物，曾如饥似渴地覆盖我们的维度裂口，在这里却已衰弱。我的理论似乎是正确的：幽匿诞生于',
 'item.nyctocosm.journal.2.page3':'维度的边界，并非夜域本身的原生事物。\n\n§n观察四：§r夜域已经破碎。我们的推演一致指向“终极无瑕态”：一个生机盎然且不受阻碍的世界，必然会抵达演化的完美，而非死亡。',
 'item.nyctocosm.journal.2.page4':'因此，夜域如今的状态留下了几个未解之谜：\n\n这里曾住着谁？发生了什么？又将会发生什么？',
 'item.nyctocosm.journal.3.title':'幽暗',
 'item.nyctocosm.journal.3.page1':'我看不见。我听不见，无法呼吸，无法思考，无法……\n抱歉，我还没有完全恢复。理性仍躲避着我这颗破碎的心。\n\n我没有料到此地会如此侵蚀我的精神。我自认意志坚定，',
 'item.nyctocosm.journal.3.page2':'可在刚才的发作中，我彻底失去了全部理智。\n\n当时，我正在掂量从这片土地采集的几份地质样本。毫无征兆地，我失去了视觉，紧随而来的是剧烈的',
 'item.nyctocosm.journal.3.page3':'幻觉。我听见自己的心脏猛烈跳动，比以往任何一次都更响。恐惧、惊骇与§o末日将临§r般的剧痛席卷了我，除此之外我什么也感觉不到。我不知道该怎么办。\n\n慌乱中，我抬头望向天空，望向',
 'item.nyctocosm.journal.3.page4':'上方那颗搏动旋转的星。微弱的星光让我的意识短暂浮出水面，我这才明白：自己正因缺少光明而窒息。\n\n我勉强摸出引火工具，击出几粒火星点燃火焰。顷刻间，我的神智恢复，',
 'item.nyctocosm.journal.3.page5':'思绪也重新清晰。我们总把光明视作理所当然，却很少意识到失去它后，自己会何等惶恐、何等迷失。\n\n我不禁怀疑：这个维度的某种未知性质，是否会放大黑暗对心智的影响？无论真相为何，',
 'item.nyctocosm.journal.3.page6':'我都已经得到教训。我绝不会再离开光明。',
 'item.nyctocosm.journal.4.title':'坚志',
 'item.nyctocosm.journal.4.page1':'想象一棵巨树，扎根于时间的沃土。熵的浪潮冲刷、牵扯着树干，却全然徒劳；机遇与变迁的狂风拍打它的枝叶，它依然挺立，绝不动摇。\n\n现在再想象另一棵更小的树。它',
 'item.nyctocosm.journal.4.page2':'根系浅薄而脆弱。风与水从四面八方无情轰击，树木被连根拔起，卷入永恒，再也无人得见。它存在过的一切痕迹都被抹去。\n\n我的研究使我开始探究存在、',
 'item.nyctocosm.journal.4.page3':'因果优位，以及万物的目的论。有些事物如同那棵巨树：强大、不屈，无法被移除。它们的存在保持静止与恒定。\n另一些则更像那棵小树：柔顺、依附、轻易便会倾覆。',
 'item.nyctocosm.journal.4.page4':'这些事物的存在十分薄弱，只需最轻微的一推，就会迅速消散在深渊中。\n\n我提出一种目的论性质，并将其称为“坚志性”。具备坚志的物体能够抵御因果层面的威胁。',
 'item.nyctocosm.journal.4.page5':'我还不确定物体要如何获得高度的坚志性。也许，它必须与更高层的现实发生交互？一个比这里更加……§o真实§r的位面？\n\n我还需要继续研究。根据我与§k守卫§r的遭遇，',
 'item.nyctocosm.journal.4.page6':'我担心将来我们会需要它。',
 'item.nyctocosm.journal.5.title':'半公理',

 'item.nyctocosm.catechism':'灵性秘教问答',
 'item.nyctocosm.catechism.author':'大法师',
 'item.nyctocosm.catechism.lore':'一部蕴含宇宙智慧的典籍',
 'item.nyctocosm.catechism.page1':'这本典籍的秘密逐渐显现……',
 'item.nyctocosm.catechism.title':'戒律 %s：%s',
 'item.nyctocosm.catechism.1.title':'论灵魂',
 'item.nyctocosm.catechism.1.page1':'永不终结之魂，并不存在。\n\n因为灵魂只容纳自身，别无他物。存在之物即有灵魂；不存在之物便无灵魂。\n\n灵魂流动，灵魂无界，灵魂强大，灵魂实存。让我们',
 'item.nyctocosm.catechism.1.page2':'倾泻灵魂的力量，将它撕开，为我们所用。\n\n让我们编织、束缚、聚合灵魂的力量，使其履行目的。\n\n因为没有目的的灵魂必将迷失，而迷失的灵魂无根可归。',
 'item.nyctocosm.catechism.1.page3':'愿其如此。',
 'item.nyctocosm.catechism.2.title':'幻光',
 'item.nyctocosm.catechism.2.page1':'世界是一张蛛网。世界是一面罗网。世界是一窝杂乱无章的心智线枝。\n\n我们应当束缚与编织，指引与传导。何为真实，何为虚妄？这由秘法师裁定。',
 'item.nyctocosm.catechism.2.page2':'秘法师手持一根针。它比现实更加真实。借助它，他可以刺穿空间的褶皱，编接时间的缝隙。拨开因果的波涛，秘法师在无处不在的宇宙风暴中睁开一只眼。',
 'item.nyctocosm.catechism.2.page3':'风暴包罗一切，将思想与物质熔为一体。只有在风眼之中，她才能同时驾驭二者，并将它们分离。只有在风眼之中，无限的思想才会嬗变为可感知的实体。\n\n这便是秘法师的力量。',

 'item.nyctocosm.dreams':'梦境手记',
 'item.nyctocosm.dreams.title':'洞见 %s：%s',
 'item.nyctocosm.dreams.author':'兽',
 'item.nyctocosm.dreams.lore':'来自某个古老心灵的奇异、零散幻象',
 'item.nyctocosm.dreams.page1':'返祖般的记忆涌入你的脑海……',
 'item.nyctocosm.dreams.1.title':'大爆炸',
 'item.nyctocosm.dreams.1.page1':'汪。我是狗吗？不，我是猫。或者不是？对，我是猫。嗯，对，我是托尼。\n\n鱼在哪儿？我饿了。\n\n好蓝！转来转去的。像一片水。水里有鱼。鱼在哪儿？我饿了。',
 'item.nyctocosm.dreams.1.page2':'啊！他有一条鱼。在蓝色里面！我要去抓住它。\n\n哦，他为什么要那么做？现在鱼跑进蓝色里了。我得进去抓它。\n\n蓝色真奇怪。我饿了。',
 'item.nyctocosm.dreams.1.page3':'哦，我在往下掉！好吧，现在我在哪儿？这里已经不是蓝色了。\n\n好黑。我好冷。鱼在哪儿？我饿了。\n\n汪。\n\n……',
 'item.nyctocosm.dreams.1.page4':'我——',
 'item.nyctocosm.dreams.2.title':'创生',
 'item.nyctocosm.dreams.2.page1':'——存在着。活着？不。我也永远不该活着。\n\n我明白了。存在，就是在宇宙上压下一道印记；就是让自身成为风暴／时空／混沌中的一只眼。这个领域刚刚开始存在，我亦如此。',
 'item.nyctocosm.dreams.2.page2':'§6~你也在这里。很好。只有一个／零个／可数无穷个，并不合适。~\n\n§0我同意。\n\n§6~看来你已经找到了一副形体。~',
 'item.nyctocosm.dreams.2.page3':'这具容器？我们苏醒后的最初瞬间，我偶然发现了它。\n\n§6~瞬间？这里还没有任何有意义的时空正交性。我们要让它成立吗？~\n\n§0我们……应当如此。',
 'item.nyctocosm.dreams.2.page4':'§6~那么，就让世间有——',

 'item.nyctocosm.adherent.author':'暮色',
 'item.nyctocosm.adherent.lore':'一段失而复得的记忆',
 'item.nyctocosm.adherent.title':'追随者 %s：%s',
 'item.nyctocosm.adherent.1.title':'巨蛇',
 'item.nyctocosm.adherent.1.page1':'§k维斯珀§r——恐惧／痛苦／折磨，你渴望它们吗？\n\n§4§k萨罗博罗斯§r§4——*我渴望*§r\n\n吞食／消耗／同化，永远不要满足\n\n§4*你诅咒了我*§r',
 'item.nyctocosm.adherent.1.page2':'不／你将永远生长／延伸／强大\n\n§4*我明白了*§r\n\n去吧，我的孩子／宠物／食粮，成为真正的梦魇',
 'item.nyctocosm.adherent.2.title':'巫师',
 'item.nyctocosm.adherent.2.page1':'§5§k西辛迪里尔§r§5——§o我终于找到你了§r§5……§r\n\n§k维斯珀§r——我会聆听／承诺／满足你的请求\n\n§5§o让我与你一同归于永恒的虚无§r§5……§r\n\n你必须服从／追随／热爱暮色的审判',
 'item.nyctocosm.adherent.2.page2':'§5§o我已经如此，也将永远如此§r§5……§r\n\n那么，你将分得虚空／空无／寂灭',
 'item.nyctocosm.adherent.3.title':'使者',
 'item.nyctocosm.adherent.3.page1':'§k维斯珀§r——你是§e§k奥罗尔§r的代理／棋子／延伸。\n\n§6§k阿比顿§r§6——§l正因如此，我绝不会拥抱黑夜！§r\n\n你是一名使者。为谁而来？',
 'item.nyctocosm.adherent.3.page2':'§6§l为最终审判！为宇宙秩序的重塑！你明明知道！§r\n\n你是毁灭者／湮灭者／行刑者。你的本性与我相近。',
 'item.nyctocosm.adherent.3.page3':'§6§l我拒绝为你的目的效力！§r\n\n你没有选择／意志／退路。',

 'item.nyctocosm.axiotopology':'公理拓扑学',
 'item.nyctocosm.axiotopology.title':'定理 %s：%s',
 'item.nyctocosm.axiotopology.author':'塔夫-索莱塔',
 'item.nyctocosm.axiotopology.lore':'一本古老残缺、写满手记的教科书',
 'item.nyctocosm.axiotopology.page1':'这本书的知识灌注进你的意识……',
 'item.nyctocosm.axiotopology.1.title':'绪论',
 'item.nyctocosm.axiotopology.1.page1':'“公理拓扑学”是我们对以下学问的称呼：研究半公理超现象，以及它们与宇宙中因果“流动”的底层拓扑之间的关系。充分掌握公理拓扑学后，便可构造半公理，解决诸如',
 'item.nyctocosm.axiotopology.1.page2':'逆同调因果约化问题、熵的手性问题、流后决策矩阵解析问题等难题。解决这些问题，是成功显化',
 'item.nyctocosm.axiotopology.1.page3':'超现象最首要的步骤之一。',
 'item.nyctocosm.axiotopology.2.title':'半公理场与标准拓扑',
 'item.nyctocosm.axiotopology.2.page1.part1':'超现象显化所涉及的最根本过程，或许就是构造一个局部化的',
 'item.nyctocosm.axiotopology.2.page1.part2':'半公理场',
 'item.nyctocosm.axiotopology.2.page1.part2.hovertext':'§6§l定义 §f§n§o半公理场\n\n§r§7封闭于时空中某个拓扑同胚区域内，\n由降低或增强的半公理能量构成的\n自由、有限体积。',
 'item.nyctocosm.axiotopology.2.page1.part3':'（SAF）。\n\n任何试图显化超现象的过程，都必须先构造一个半公理场。这是因为',
 'item.nyctocosm.axiotopology.2.page2.part1':'超现象无法发生在',
 'item.nyctocosm.axiotopology.2.page2.part2':'标准拓扑',
 'item.nyctocosm.axiotopology.2.page2.part2.hovertext':'§6§l定义 §f§n§o标准拓扑\n\n§r§7遍布宇宙的半公理能级。\n按惯例定义为 0Þ。',
 'item.nyctocosm.axiotopology.2.page2.part3':'的闭包内部。\n\n生成半公理场后，便可为局部时空赋予超标准行为。这些行为一旦实现，就能从',
 'item.nyctocosm.axiotopology.2.page3':'半公理场内部传播至外部的标准拓扑时空。这个过程称为嬗变。\n\n理论上，可以生成无限扩张、并以标准拓扑最高速度扩散的半公理场。然而，这个过程不可逆，',
 'item.nyctocosm.axiotopology.2.page4':'最终会抹除全部标准拓扑，以半公理场中的能级为基础的新拓扑取而代之。不过，由于催化此类事件所需的半公理势规模极端庞大，这种情况不太可能发生。',

 'item.nyctocosm.adherent_serpent':'追随者：巨蛇',
 'item.nyctocosm.adherent_sorcerer':'追随者：巫师',
 'item.nyctocosm.adherent_emissary':'追随者：使者',
}

def export():
 source=ROOT.parent/'_nyctocosm_source/resources/assets/minecraft/lang/en_us.json'
 english={k:v for k,v in json.loads(source.read_text(encoding='utf-8-sig')).items() if not k.startswith(('item.minecraft.','block.minecraft.','entity.minecraft.'))}
 chinese={}
 for item,(en,zh,description,translated) in ENTRIES.items():
  english['item.nyctocosm.'+item]=en;chinese['item.nyctocosm.'+item]=zh
  english['tooltip.nyctocosm.item.'+item]=description;chinese['tooltip.nyctocosm.item.'+item]=translated
 english['block.nyctocosm.dais']=ENTRIES['dais'][0];chinese['block.nyctocosm.dais']=ENTRIES['dais'][1]
 for key,(en,zh) in UI.items():english[key]=en;chinese[key]=zh
 for mob,(en,zh) in MOBS.items():english['entity.nyctocosm.'+mob]=en;chinese['entity.nyctocosm.'+mob]=zh
 for spell,zh in SPELLS.items():english['spell.nyctocosm.native.'+spell]=spell.title();chinese['spell.nyctocosm.native.'+spell]=zh
 for element,(en,zh) in ELEMENTS.items():
  key='death.attack.nyctocosm.'+element
  english[key]=f'%1$s was consumed by {en}';chinese[key]=f'%1$s被{zh}吞噬了'
  english[key+'.player']=f'%1$s was consumed by %2$s\'s {en}';chinese[key+'.player']=f'%1$s被%2$s的{zh}吞噬了'
 chinese.update(LORE_ZH)
 folder=ROOT/'src/main/resources/assets/nyctocosm/lang';folder.mkdir(parents=True,exist_ok=True)
 for locale,data in [('en_us',english),('zh_cn',chinese)]:
  (folder/(locale+'.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(f'Localized {len(ENTRIES)} native item types; en_us fallback and zh_cn overlay.')
if __name__=='__main__':export()
