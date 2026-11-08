package de.dragoni.tradingstation.registry;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.config.UpgradeRules;
import de.dragoni.tradingstation.item.NpcCatcherItem;
import de.dragoni.tradingstation.item.StationUpgradeItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems(TradingStationMod.MOD_ID);

  public static final DeferredItem<NpcCatcherItem> NPC_CATCHER =
      ITEMS.registerItem("npc_catcher", NpcCatcherItem::new, props -> props.stacksTo(1));

  public static final DeferredItem<StationUpgradeItem> STATION_UPGRADE_ADVANCED =
      ITEMS.registerItem(
          "station_upgrade_advanced",
          props -> new StationUpgradeItem(props, UpgradeRules.ADVANCED),
          props -> props.stacksTo(1));

  public static final DeferredItem<StationUpgradeItem> STATION_UPGRADE_WANDERING =
      ITEMS.registerItem(
          "station_upgrade_wandering",
          props -> new StationUpgradeItem(props, UpgradeRules.WANDERING),
          props -> props.stacksTo(1));

  private ModItems() {}

  /** The registered upgrade item of a kind, or null if there is none (yet). */
  public static net.minecraft.world.item.Item upgradeItem(String kind) {
    if (UpgradeRules.ADVANCED.equals(kind)) {
      return STATION_UPGRADE_ADVANCED.get();
    }
    return UpgradeRules.WANDERING.equals(kind) ? STATION_UPGRADE_WANDERING.get() : null;
  }
}
