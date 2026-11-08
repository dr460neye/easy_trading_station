package com.oli.tradingstation.event;

import com.oli.tradingstation.TradingStationMod;
import com.oli.tradingstation.item.NpcCatcherItem;
import com.oli.tradingstation.station.StationSessions;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TradingStationMod.MOD_ID)
public class NpcCatcherEvents {

  private NpcCatcherEvents() {}

  /** Runs before the NPC's own interaction (dialog, trading), so an empty catcher always wins. */
  @SubscribeEvent
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
    if (!(event.getTarget() instanceof EasyNPC<?>)) {
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
