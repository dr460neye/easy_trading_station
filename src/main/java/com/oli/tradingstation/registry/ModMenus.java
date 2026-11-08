package com.oli.tradingstation.registry;

import com.oli.tradingstation.TradingStationMod;
import com.oli.tradingstation.station.StationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, TradingStationMod.MOD_ID);

  public static final DeferredHolder<MenuType<?>, MenuType<StationMenu>> STATION =
      MENUS.register(
          "station", () -> IMenuTypeExtension.create((id, inventory, buf) -> new StationMenu(id, inventory)));

  private ModMenus() {}
}
