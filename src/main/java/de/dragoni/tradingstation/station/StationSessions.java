package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.item.NpcCatcherItem;
import de.markusbordihn.easynpc.api.action.EasyNPCActionHandler;
import de.markusbordihn.easynpc.api.handler.EasyNPCPauseHandler;
import de.markusbordihn.easynpc.data.preset.PresetData;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Server side. A station holds only {@code PresetData}. While a player talks or trades, one paused,
 * invulnerable entity exists in the level; when the menu closes its state is written back and it is
 * discarded again. Nothing exists (or ticks) while no one is using a station.
 */
public final class StationSessions {

  /** Tag that marks transient entities so leftovers after a crash can be removed on load. */
  public static final String TRANSIENT_TAG = TradingStationMod.MOD_ID + ":transient";

  /** Ticks without any open menu after which a session is closed (NPCs that only run actions, screens without a menu). */
  private static final int NO_MENU_GRACE = 40;

  private static final int MAX_DISTANCE_SQR = 24 * 24;
  private static final long RESTOCK_INTERVAL = 12000L;
  private static final long DAY_TICKS = 24000L;

  private static final class Session {
    final ServerLevel level;
    final BlockPos stationPos;
    final Entity entity;
    final boolean wasInvulnerable;
    final double originalScale;
    int idleTicks;

    Session(
        ServerLevel level,
        BlockPos stationPos,
        Entity entity,
        boolean wasInvulnerable,
        double originalScale) {
      this.level = level;
      this.stationPos = stationPos;
      this.entity = entity;
      this.wasInvulnerable = wasInvulnerable;
      this.originalScale = originalScale;
    }
  }

  private static final Map<UUID, Session> SESSIONS = new HashMap<>();

  private StationSessions() {}

  public static boolean isBusy(Level level, TradingStationBlockEntity station) {
    for (Session session : SESSIONS.values()) {
      if (session.level == level && session.stationPos.equals(station.getBlockPos())) {
        return true;
      }
    }
    return false;
  }

  public static boolean isTransient(Entity entity) {
    return entity.entityTags().contains(TRANSIENT_TAG);
  }

  public static boolean isActive(Entity entity) {
    for (Session session : SESSIONS.values()) {
      if (session.entity == entity) {
        return true;
      }
    }
    return false;
  }

  public static void open(
      ServerPlayer player,
      ServerLevel level,
      TradingStationBlockEntity station,
      Direction facing,
      BlockHitResult hit) {
    if (SESSIONS.containsKey(player.getUUID())) {
      finish(player.getUUID());
    }
    if (isBusy(level, station)) {
      player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.busy"));
      return;
    }
    String idle = station.getIdleKind();
    if (!idle.isEmpty()) {
      // no profession, no trades: say so instead of starting a session (the villager would shake its head)
      player.sendOverlayMessage(
          Component.translatable("message.easy_npc_trading_station.idle_" + idle));
      return;
    }
    if (!station.isNpcActive()) {
      sendMissingWorkstation(player, station);
      return;
    }
    PresetData stored = prepareStoredData(level, station);
    Entity entity = StationNpc.create(level, stored, EntitySpawnReason.LOAD);
    if (entity == null) {
      player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.failed"));
      return;
    }
    if (entity instanceof net.minecraft.world.entity.npc.villager.Villager villager
        && stored.data().getLongOr(StationNpc.RESTOCK_KEY, -1L) == level.getOverworldClockTime()) {
      villager.restock();
    }
    if (entity instanceof net.minecraft.world.entity.Mob mob && !(entity instanceof EasyNPC<?>)) {
      mob.setNoAi(true);
      mob.setSilent(true);
      mob.setPersistenceRequired();
    }
    boolean wasInvulnerable = entity.isInvulnerable();
    double originalScale = 1.0;
    if (entity instanceof net.minecraft.world.entity.LivingEntity living
        && living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) != null) {
      var scale = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
      originalScale = scale.getBaseValue();
      // same fit as the renderer: tall NPCs shrink to stay under the station roof
      scale.setBaseValue(originalScale * Math.min(1.0, 1.7 / Math.max(entity.getBbHeight(), 0.1F)));
    }
    StationNpc.place(entity, station.getBlockPos(), facing);
    station.setInUse(true);
    entity.setInvulnerable(true);
    entity.addTag(TRANSIENT_TAG);
    if (entity instanceof EasyNPC<?> npc) {
      EasyNPCPauseHandler.pause(npc);
    }
    // Register first: the join guard in StationEvents cancels transient entities without a session.
    SESSIONS.put(player.getUUID(), new Session(level, station.getBlockPos(), entity, wasInvulnerable, originalScale));
    if (!level.addFreshEntity(entity)) {
      SESSIONS.remove(player.getUUID());
      TradingStationMod.LOGGER.warn("Could not add station NPC {} to the level", entity.getType());
      player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.failed"));
      return;
    }

    boolean handled = false;
    try {
      if (entity instanceof EasyNPC<?> npc) {
        npc.getEasyNPCTradingData().resetExpiredTradingOffers();
      }
      // Fire the same event a normal right-click fires, so scripts (KubeJS shops, quests) see it,
      // then fall back to the NPC's own interaction (security, action, dialog, trading).
      InteractionResult result =
          net.neoforged.neoforge.common.CommonHooks.onInteractEntity(
              player, entity, InteractionHand.MAIN_HAND);
      if (result == null) {
        result = player.interactOn(entity, InteractionHand.MAIN_HAND, hit.getLocation());
      }
      handled = result != null && result.consumesAction();
      if (!handled && entity instanceof EasyNPC<?> npc) {
        if (npc.getEasyNPCTradingData().hasTradingData()) {
          handled = EasyNPCActionHandler.openTradingScreen(npc, player);
        } else if (npc.getEasyNPCDialogData().hasDialog()) {
          handled = EasyNPCActionHandler.openDialog(npc, player);
        }
      }
    } catch (RuntimeException e) {
      TradingStationMod.LOGGER.error("Station interaction failed", e);
    }
    // No screen is not an error: NPCs may only run a command, whisper or play a sound. The session
    // stays for a short grace period (see tick()) so such actions can finish, then it is closed.
    TradingStationMod.LOGGER.debug("Station interaction handled={}", handled);
  }

  /**
   * Standard villagers restock at most twice per day while stored (like at a job site); a wandering
   * trader in a station with the wandering upgrade gets new offers once per day. The marks are saved
   * in the stored data and the station is updated before the entity is built from it.
   */
  // The day clock is used, not the game time: sleeping moves the day clock but not the game time.
  private static PresetData prepareStoredData(ServerLevel level, TradingStationBlockEntity station) {
    PresetData stored = station.getNpc();
    if (StationNpc.isEasyNpc(stored.entityType())) {
      return stored;
    }
    long now = level.getOverworldClockTime();
    net.minecraft.nbt.CompoundTag tag = stored.data().copy();
    boolean changed = false;
    if (stored.entityType() == net.minecraft.world.entity.EntityType.VILLAGER
        && now - tag.getLongOr(StationNpc.RESTOCK_KEY, Long.MIN_VALUE / 2) >= RESTOCK_INTERVAL) {
      tag.putLong(StationNpc.RESTOCK_KEY, now);
      changed = true;
    }
    if (stored.entityType() == net.minecraft.world.entity.EntityType.WANDERING_TRADER
        && "wandering".equals(station.upgradeKind())
        && now / DAY_TICKS > tag.getLongOr(StationNpc.OFFERS_DAY_KEY, -1L)) {
      tag.remove("Offers"); // the trader rolls new offers when it is built without any
      tag.putLong(StationNpc.OFFERS_DAY_KEY, now / DAY_TICKS);
      changed = true;
    }
    if (!changed) {
      return stored;
    }
    stored = new PresetData(stored.entityType(), tag);
    station.setNpc(stored);
    return stored;
  }

  public static void sendMissingWorkstation(ServerPlayer player, TradingStationBlockEntity station) {
    var requirement = station.requirement();
    if (requirement != null) {
      player.sendSystemMessage(
          Component.translatable(
              "message.easy_npc_trading_station.workstation_missing", requirement.displayName()));
    }
  }

  /** Menu closed: write the NPC state back into the station and discard the entity. */
  public static void onMenuClosed(ServerPlayer player) {
    if (SESSIONS.containsKey(player.getUUID())) {
      finish(player.getUUID());
    }
  }

  /** Called when the station block entity is removed so no state is lost. */
  public static void finishAt(TradingStationBlockEntity station) {
    UUID owner = null;
    for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
      if (entry.getValue().stationPos.equals(station.getBlockPos())
          && entry.getValue().level == station.getLevel()) {
        owner = entry.getKey();
      }
    }
    if (owner != null) {
      finish(owner);
    }
  }

  public static void finishAll() {
    for (UUID id : java.util.List.copyOf(SESSIONS.keySet())) {
      finish(id);
    }
  }

  /** Once per server tick: closes sessions whose player left, walked away or has no screen. */
  public static void tick() {
    if (SESSIONS.isEmpty()) {
      return;
    }
    Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
    java.util.List<UUID> done = new java.util.ArrayList<>();
    while (it.hasNext()) {
      Map.Entry<UUID, Session> entry = it.next();
      Session session = entry.getValue();
      ServerPlayer player = session.level.getServer().getPlayerList().getPlayer(entry.getKey());
      if (player == null
          || player.isRemoved()
          || player.level() != session.level
          || player.distanceToSqr(session.entity) > MAX_DISTANCE_SQR
          || !session.entity.isAlive()) {
        done.add(entry.getKey());
      } else if (player.containerMenu == player.inventoryMenu) {
        if (++session.idleTicks > NO_MENU_GRACE) {
          done.add(entry.getKey());
        }
      } else {
        session.idleTicks = 0;
      }
    }
    done.forEach(StationSessions::finish);
  }

  private static PresetData station(Session session) {
    return session.level.getBlockEntity(session.stationPos) instanceof TradingStationBlockEntity be
        ? be.getNpc()
        : null;
  }

  private static void finish(UUID playerId) {
    Session session = SESSIONS.remove(playerId);
    if (session == null) {
      return;
    }
    Entity entity = session.entity;
    PresetData updated = null;
    try {
      if (entity instanceof Merchant merchant) {
        merchant.setTradingPlayer(null);
      }
      if (entity instanceof EasyNPC<?> npc && entity.isAlive()) {
        EasyNPCPauseHandler.resume(npc);
        if (entity instanceof net.minecraft.world.entity.LivingEntity living
            && living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) != null) {
          living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)
              .setBaseValue(session.originalScale);
        }
        entity.setInvulnerable(session.wasInvulnerable);
        var tag = npc.getEasyNPCPresetData().serializePresetData();
        updated = NpcOrigin.carryOver(station(session), new PresetData(entity.getType(), tag));
      } else if (!(entity instanceof EasyNPC<?>) && entity.isAlive()) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity living
            && living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) != null) {
          living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)
              .setBaseValue(session.originalScale);
        }
        entity.setInvulnerable(session.wasInvulnerable);
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
          mob.setNoAi(false);
          mob.setSilent(false);
        }
        updated =
            StationNpc.carryKeys(
                station(session), new PresetData(entity.getType(), StationNpc.entityTag(entity)));
      }
    } catch (RuntimeException e) {
      TradingStationMod.LOGGER.error("Could not read back NPC state, keeping previous data", e);
    }
    entity.discard();

    if (session.level.getBlockEntity(session.stationPos) instanceof TradingStationBlockEntity station) {
      station.setInUse(false);
      if (updated != null && updated.hasValidData()) {
        station.setNpc(updated);
      }
    } else if (updated != null && updated.hasValidData()) {
      // station vanished while in use: hand the NPC out instead of losing it
      Block.popResource(session.level, session.stationPos, NpcCatcherItem.filled(updated));
    }
  }
}
