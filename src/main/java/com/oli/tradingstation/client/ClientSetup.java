package com.oli.tradingstation.client;

import com.oli.tradingstation.TradingStationMod;
import com.oli.tradingstation.registry.ModBlocks;
import com.oli.tradingstation.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TradingStationMod.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {

  private ClientSetup() {}

  @SubscribeEvent
  public static void registerScreens(RegisterMenuScreensEvent event) {
    event.register(ModMenus.STATION.get(), StationScreen::new);
  }

  @SubscribeEvent
  public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(ModBlocks.STATION_BLOCK_ENTITY, TradingStationRenderer::new);
  }
}
