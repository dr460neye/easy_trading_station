package de.dragoni.tradingstation.item;

import de.dragoni.tradingstation.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An upgrade for the upgrade slot of a station; its kind decides which NPCs the station can host. */
public class StationUpgradeItem extends Item {

  private final String kind;

  public StationUpgradeItem(Properties properties, String kind) {
    super(properties);
    this.kind = kind;
  }

  public String kind() {
    return kind;
  }

  /** The upgrade kind of a stack, or an empty string if it is not an upgrade. */
  public static String kindOf(ItemStack stack) {
    return stack.getItem() instanceof StationUpgradeItem upgrade ? upgrade.kind : "";
  }

  public static boolean isLegacy(ItemStack stack) {
    return Boolean.TRUE.equals(stack.get(ModDataComponents.LEGACY_UPGRADE.get()));
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      TooltipContext context,
      TooltipDisplay display,
      Consumer<Component> tooltip,
      TooltipFlag flag) {
    tooltip.accept(
        Component.translatable(getDescriptionId() + ".tooltip")
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    if (isLegacy(stack)) {
      tooltip.accept(
          Component.translatable("item.easy_npc_trading_station.station_upgrade.legacy")
              .withStyle(net.minecraft.ChatFormatting.YELLOW));
    }
  }
}
