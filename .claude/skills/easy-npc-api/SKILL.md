---
name: easy-npc-api
description: Verified cheat sheet of Easy NPC 7.18.0 (26.1.2 NeoForge) classes and methods used by this mod - presets, spawning, pausing, trading, dialogs, security, rendering. Use instead of re-researching or decompiling.
---

# Easy NPC API cheat sheet (package `de.markusbordihn.easynpc`)

Source: github.com/MarkusBordihn/BOs-Easy-NPC branch `1.21.1`; signatures below were checked with javap on the jar. Items marked (src) come from source reading, not javap.

## Detect / serialize
- `entity.easynpc.EasyNPC<E extends Mob>`: `getEntity()`, `getMob()`, `getLivingEntity()`, `getEntityLevel()`, `getEntityServerLevel()`, `isServerSideInstance()`, `registerEasyNPCDefaultData()`, `getEntityTypeId()`, static `isUsableServerSideInstance(npc)`. Detect with `entity instanceof EasyNPC<?>`.
- Preset accessor: `npc.getEasyNPCPresetData()` -> `PresetDataCapable`: `serializePresetData()` (CompoundTag, strips volatile fields), `importPresetData(CompoundTag)`.
- `data.preset.PresetData` (record): `entityType()`, `data()`, `name()`, `hasValidData()`, `getDisplayName()`, `withoutPosition()`, constants `CODEC`, `STREAM_CODEC`, `EMPTY`; ctor `(EntityType<?>, CompoundTag)`; static `has/get/set(ItemStack,...)`.
- `data.preset.PresetDataUtils`: `toItemStack(EasyNPC)` / `toItemStack(PresetData)`, `fromItemStack(ItemStack)`, `spawnEntity(PresetData, Level, BlockPos[, Player])` (runs sanitizer when player given), `toSpawnData(...)`.
- Our DataComponent `captured_npc` stores `PresetData` directly (own component type, same codecs).

## Security
- `security.SecurityManager`: `checkAccess(ServerPlayer, EasyNPC)` -> `SecurityDecision.allowed()`; `sanitizePresetDataForSpawn(PresetData, Level, Player)` (RISK: may strip trading/dialog; verify in S5.1); `sanitizePresetExport(CompoundTag)`.

## Pause / actions / trading / dialog
- `api.handler.EasyNPCPauseHandler`: static `pause(npc)`, `resume(npc)`, `isPaused(npc)` (sets PAUSED + NoAI; BaseTickHandler skips actions while paused).
- `api.action.EasyNPCActionHandler` (src): `openDialog(npc, serverPlayer[, label])`, `trigger(npc, ActionEventType.ON_INTERACTION, ActionContext.of(player))`, `execute(...)`.
- `entity.easynpc.data.TradingDataCapable` extends vanilla `Merchant`: `openTradingScreen(ServerPlayer)`, `getOffers()`, `hasTradingData()`, `setTradingPlayer`, `stopTrading`, (src) `resetExpiredTradingOffers()`. Trading uses vanilla `MerchantMenu`.
- `DialogDataCapable.openDialog(ServerPlayer, UUID)` / `openDefaultDialog(ServerPlayer)` (src).
- `api.handler.EasyNPCEntityHandler` (src): `spawnFromPreset(...)`, `spawn/despawn/revive/respawn/delete/find`.
- Events (src): `api.event.EasyNPCEventRegistry` register* listeners (Dialog, Action, EntityLifecycle ...); not NeoForge bus events.

## Rendering (src)
- Pattern to copy: `client.renderer.blockentity.BaseEasyNPCSpawnerBlockEntityRenderer` (class exists in jar). Display entity never added to level; render via `EntityRenderDispatcher.render(entity, 0,0,0, yaw, partialTick, poseStack, buffer, light)`.
- Spawner storage pattern: `level.BaseEasyNPCSpawner` keeps `PresetData storedPresetData` (tag `StoredPresetData`); `block.entity.EasyNPCSpawnerBlockEntity` provides update tag/packet.
- Skins resolve automatically in the renderer when display entity has the preset data imported.

## Entity types
- Registered as `easy_npc:<lowercase DefaultNPCType>` e.g. `easy_npc:humanoid`, `easy_npc:villager` (src; verify ids in game with `/summon easy_npc:` tab completion).

## Gradle
`implementation "maven.modrinth:easy-npc-core:Amlap22E"` (26.1.2; the 1.21.1 build is `9TVwS7rU`) with Modrinth Maven repo. Signatures were verified on the 1.21.1 jar and compile unchanged against 26.1.2; only MC types differ (`ResourceLocation` is `Identifier`). Version number 7.18.0 exists for several MC versions - always use the ID.

Update this file when a story verifies or refutes an item marked (src) or RISK.
