package de.dragoni.tradingstation.event;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.item.NpcCatcherItem;
import de.dragoni.tradingstation.station.NpcOrigin;
import de.dragoni.tradingstation.station.StationSessions;
import de.dragoni.tradingstation.station.StationNpcData;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TradingStationMod.MOD_ID)
public class StationEvents {

  private StationEvents() {}

  @SubscribeEvent
  public static void onMenuClose(PlayerContainerEvent.Close event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      StationSessions.onMenuClosed(player);
    }
  }

  @SubscribeEvent
  public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      StationSessions.onMenuClosed(player);
    }
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    StationSessions.tick();
    NpcOrigin.processQueue(event.getServer());
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    StationSessions.finishAll();
  }

  private static final Set<Entity> DOOMED = Collections.newSetFromMap(new WeakHashMap<>());

  private static boolean isLoadedCatcher(Entity entity) {
    return entity instanceof ItemEntity item
        && item.getItem().getItem() instanceof NpcCatcherItem
        && NpcOrigin.has(NpcCatcherItem.getNpc(item.getItem()));
  }

  @SubscribeEvent
  public static void onItemDamaged(EntityInvulnerabilityCheckEvent event) {
    if (!event.isInvulnerable() && isLoadedCatcher(event.getEntity())) {
      DOOMED.add(event.getEntity());
    }
  }

  @SubscribeEvent
  public static void onItemExpire(ItemExpireEvent event) {
    if (event.getExtraLife() <= 0 && isLoadedCatcher(event.getEntity())) {
      DOOMED.add(event.getEntity());
    }
  }

  /** A filled catcher that is destroyed (explosion, fire, void, despawn) kills its captured NPC. */
  @SubscribeEvent
  public static void onItemLeave(EntityLeaveLevelEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)) {
      return;
    }
    var reason = item.getRemovalReason();
    // KILLED = void / kill. DISCARDED also happens when magnets or hoppers take the item, so it only
    // counts when damage or the despawn timer was seen before.
    boolean destroyed =
        reason == Entity.RemovalReason.KILLED
            || (reason == Entity.RemovalReason.DISCARDED && DOOMED.contains(item));
    if (!destroyed) {
      return;
    }
    ItemStack stack = item.getItem();
    if (stack.isEmpty() || !(stack.getItem() instanceof NpcCatcherItem)) {
      return; // picked up items are empty by now
    }
    StationNpcData data = NpcCatcherItem.getNpc(stack);
    if (NpcOrigin.has(data)) {
      NpcOrigin.queueDeath(data);
    }
  }

  /** A transient NPC that was saved with its chunk (crash while a menu was open) is dropped on load. */
  @SubscribeEvent
  public static void onEntityJoin(EntityJoinLevelEvent event) {
    if (!event.getLevel().isClientSide()
        && StationSessions.isTransient(event.getEntity())
        && !StationSessions.isActive(event.getEntity())) {
      event.setCanceled(true);
    }
  }
}
