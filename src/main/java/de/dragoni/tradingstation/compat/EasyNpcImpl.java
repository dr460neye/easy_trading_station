package de.dragoni.tradingstation.compat;

import com.mojang.serialization.Codec;
import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.station.StationNpcData;
import de.markusbordihn.easynpc.api.action.EasyNPCActionHandler;
import de.markusbordihn.easynpc.api.handler.EasyNPCPauseHandler;
import de.markusbordihn.easynpc.data.preset.PresetData;
import de.markusbordihn.easynpc.data.preset.PresetDataUtils;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import de.markusbordihn.easynpc.security.SecurityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** The only class that uses Easy NPC types. Loaded only when Easy NPC is installed. */
final class EasyNpcImpl {

  private EasyNpcImpl() {}

  static boolean is(Entity entity) {
    return entity instanceof EasyNPC<?>;
  }

  static Codec<StationNpcData> legacyCodec() {
    return PresetData.CODEC.xmap(
        preset -> new StationNpcData(preset.entityType(), preset.data()),
        data -> new PresetData(data.entityType(), data.data()));
  }

  static StationNpcData capture(Entity entity) {
    PresetData preset = PresetData.get(PresetDataUtils.toItemStack((EasyNPC<?>) entity));
    return preset == null ? null : new StationNpcData(preset.entityType(), preset.data());
  }

  static boolean accessAllowed(ServerPlayer player, Entity entity) {
    var decision = SecurityManager.checkAccess(player, (EasyNPC<?>) entity);
    TradingStationMod.LOGGER.info("Capture access: allowed={} reason={}", decision.allowed(), decision.reason());
    return decision.allowed();
  }

  static BlockPos home(Entity entity) {
    var npc = (EasyNPC<?>) entity;
    return npc.getEasyNPCNavigationData().hasNPCHomePosition()
        ? npc.getEasyNPCNavigationData().getNPCHomePosition()
        : entity.blockPosition();
  }

  static boolean spawn(StationNpcData data, Level level, BlockPos pos, ServerPlayer player) {
    return PresetDataUtils.spawnEntity(new PresetData(data.entityType(), data.data()), level, pos, player);
  }

  static void configure(Entity entity, CompoundTag tag) {
    var npc = (EasyNPC<?>) entity;
    npc.registerEasyNPCDefaultData();
    npc.getEasyNPCPresetData().importPresetData(tag);
  }

  static void pause(Entity entity) {
    EasyNPCPauseHandler.pause((EasyNPC<?>) entity);
  }

  static void resume(Entity entity) {
    EasyNPCPauseHandler.resume((EasyNPC<?>) entity);
  }

  static void resetExpiredOffers(Entity entity) {
    ((EasyNPC<?>) entity).getEasyNPCTradingData().resetExpiredTradingOffers();
  }

  static CompoundTag serialize(Entity entity) {
    return ((EasyNPC<?>) entity).getEasyNPCPresetData().serializePresetData();
  }

  static boolean openFallback(Entity entity, ServerPlayer player) {
    var npc = (EasyNPC<?>) entity;
    if (npc.getEasyNPCTradingData().hasTradingData()) {
      return EasyNPCActionHandler.openTradingScreen(npc, player);
    }
    if (npc.getEasyNPCDialogData().hasDialog()) {
      return EasyNPCActionHandler.openDialog(npc, player);
    }
    return false;
  }
}
