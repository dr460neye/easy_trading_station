package de.dragoni.tradingstation.event;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.item.NpcCatcherItem;
import de.dragoni.tradingstation.station.StationNpc;
import de.dragoni.tradingstation.station.StationSessions;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TradingStationMod.MOD_ID)
public class NpcCatcherEvents {

  private NpcCatcherEvents() {}

  /**
   * Runs before the NPC's own interaction (dialog, trading), so an empty catcher wins over it, and
   * early: shop scripts (KubeJS) cancel this event for shopkeepers, so a late listener is never
   * reached. Because of that the catcher can not rely on claim mods cancelling first; it asks them
   * itself in {@link NpcCatcherItem#capture}.
   */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
    var stack = event.getItemStack();
    if (!(stack.getItem() instanceof NpcCatcherItem)) {
      return;
    }
    boolean server = !event.getLevel().isClientSide();
    if (server) {
      TradingStationMod.LOGGER.info(
          "Catcher used on {} (EasyNPC={}, filled={})",
          event.getTarget().getType(),
          event.getTarget() instanceof EasyNPC<?>,
          NpcCatcherItem.isFilled(stack));
    }
    if (NpcCatcherItem.isFilled(stack)) {
      return;
    }
    if (StationSessions.isTransient(event.getTarget())) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.FAIL);
      if (server && event.getEntity() instanceof ServerPlayer p) {
        p.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.in_station"));
      }
      return;
    }
    if (!(event.getTarget() instanceof EasyNPC<?>) && !StationNpc.isGenericSupported(event.getTarget())) {
      if (server && event.getTarget() instanceof LivingEntity && event.getEntity() instanceof ServerPlayer p) {
        p.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.unsupported"));
      }
      return;
    }
    event.setCanceled(true);
    if (!server || !(event.getEntity() instanceof ServerPlayer player)) {
      event.setCancellationResult(InteractionResult.SUCCESS);
      return;
    }
    event.setCancellationResult(NpcCatcherItem.capture(player, event.getHand(), event.getTarget()));
  }
}
