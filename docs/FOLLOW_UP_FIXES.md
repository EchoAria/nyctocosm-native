# 用户提出的后续修复

## 法阵跟随必须由 Transposition artifact 启用（待修）

用户要求：仅当玩家背包中存在对应 artifact 时，玩家释放的法阵才跟随玩家。本轮优先修复回响碎片，暂不改变该规则的代码。

- 对应原物品：`artifacts/functions/transposition/give.mcfunction`；原包检查背包中的 `red_dye{artifact:1,transposition:1}`，原生物品为 `nyctocosm:artifact_transposition`。
- 判定来源：`artifacts/functions/playertick.mcfunction`、`spells/functions/__generated__/block/192` 和 `spells/functions/ring/cast_ring_plane.mcfunction`，按是否有 `a.transposition` 区分固定/跟随法阵。
- 当前可疑点：`SeelicField.tick` 使用 `mobile() || hasArtifact("transposition")`；`FocusItem` 的 Planar Coil 路径可直接设置 mobile，绕过背包条件。
- 后续核查 Soul Bottle、Planar Coil、普通法阵和 Hypertrace 的原始差异；还需确认 artifact 移入/移出背包时既有法阵的行为及持久化规则。
- 验收：无 artifact 不跟随；背包有 artifact 才按原规则跟随；覆盖放入/移出、重载、死亡、多人归属、普通法阵与 Hypertrace，并更新物品说明。

## 回响碎片激活远古城市传送门（alpha.10 已修复）

直接射向有效门框内部即可触发；恢复灵魂火蓄力、声波束、三波共振环、收束冲击波及原音效。实现、证据与尚未迁移的装饰细节见 `RESONANCE_PORTAL_BEHAVIOR.md`。
