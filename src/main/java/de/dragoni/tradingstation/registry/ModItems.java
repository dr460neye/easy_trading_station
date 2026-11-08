package de.dragoni.tradingstation.registry;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.item.NpcCatcherItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems(TradingStationMod.MOD_ID);

  public static final DeferredItem<NpcCatcherItem> NPC_CATCHER =
      ITEMS.registerItem("npc_catcher", NpcCatcherItem::new, props -> props.stacksTo(1));

  private ModItems() {}
}
