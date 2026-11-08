package de.dragoni.tradingstation;

import com.mojang.logging.LogUtils;
import de.dragoni.tradingstation.config.TradingStationCommonConfig;
import de.dragoni.tradingstation.config.TradingStationConfig;
import de.dragoni.tradingstation.registry.ModBlocks;
import de.dragoni.tradingstation.registry.ModDataComponents;
import de.dragoni.tradingstation.registry.ModItems;
import de.dragoni.tradingstation.registry.ModMenus;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(TradingStationMod.MOD_ID)
public class TradingStationMod {

  public static final String MOD_ID = "easy_npc_trading_station";
  public static final Logger LOGGER = LogUtils.getLogger();

  public TradingStationMod(IEventBus modEventBus, ModContainer container) {
    ModDataComponents.DATA_COMPONENTS.register(modEventBus);
    ModItems.ITEMS.register(modEventBus);
    ModBlocks.init(modEventBus);
    ModMenus.MENUS.register(modEventBus);
    modEventBus.addListener(TradingStationMod::addCreativeTabItems);
    container.registerConfig(ModConfig.Type.SERVER, TradingStationConfig.SPEC);
    container.registerConfig(ModConfig.Type.COMMON, TradingStationCommonConfig.SPEC);
  }

  private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
      event.accept(ModItems.NPC_CATCHER);
      event.accept(ModItems.STATION_UPGRADE_ADVANCED);
      event.accept(ModItems.STATION_UPGRADE_WANDERING);
    }
    if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
      ModBlocks.STATION_ITEMS.values().forEach(event::accept);
    }
  }
}
