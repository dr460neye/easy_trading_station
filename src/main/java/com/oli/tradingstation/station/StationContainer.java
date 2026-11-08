package com.oli.tradingstation.station;

import com.oli.tradingstation.item.NpcCatcherItem;
import com.oli.tradingstation.registry.ModDataComponents;
import de.markusbordihn.easynpc.data.preset.PresetData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Menu view of a station: slot 0 = NPC (a filled NPC Catcher), slot 1 = workstation block. The
 * block entity stays the single source of truth; nothing else is stored.
 */
public class StationContainer implements Container {

  public static final int SIZE = 2;
  public static final int NPC_SLOT = 0;
  public static final int WORKSTATION_SLOT = 1;

  private final TradingStationBlockEntity station;
  private ItemStack npcStack = ItemStack.EMPTY;

  public StationContainer(TradingStationBlockEntity station) {
    this.station = station;
    refreshNpcStack();
  }

  private void refreshNpcStack() {
    npcStack = station.hasNpc() ? NpcCatcherItem.filled(station.getNpc()) : ItemStack.EMPTY;
  }

  public boolean isNpcLocked(Player player) {
    return StationSessions.isBusy(player.level(), station);
  }

  @Override
  public int getContainerSize() {
    return SIZE;
  }

  @Override
  public boolean isEmpty() {
    return !station.hasNpc() && station.getWorkstation().isEmpty();
  }

  @Override
  public ItemStack getItem(int slot) {
    return switch (slot) {
      case NPC_SLOT -> station.hasNpc() ? npcStack : ItemStack.EMPTY;
      case WORKSTATION_SLOT -> station.getWorkstation();
      default -> ItemStack.EMPTY;
    };
  }

  @Override
  public ItemStack removeItem(int slot, int amount) {
    return removeItemNoUpdate(slot);
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    if (slot == NPC_SLOT && station.hasNpc()) {
      ItemStack out = NpcCatcherItem.filled(station.takeNpc());
      npcStack = ItemStack.EMPTY;
      return out;
    }
    if (slot == WORKSTATION_SLOT && !station.getWorkstation().isEmpty()) {
      return station.takeWorkstation();
    }
    return ItemStack.EMPTY;
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    if (slot == NPC_SLOT) {
      PresetData data = stack.isEmpty() ? null : NpcCatcherItem.getNpc(stack);
      station.setNpc(data);
      refreshNpcStack();
    } else if (slot == WORKSTATION_SLOT) {
      station.setWorkstation(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }
  }

  @Override
  public int getMaxStackSize() {
    return 1;
  }

  @Override
  public boolean canPlaceItem(int slot, ItemStack stack) {
    return accepts(slot, stack);
  }

  /** Placement rule, shared by the server container and the client side menu slots. */
  public static boolean accepts(int slot, ItemStack stack) {
    if (slot == NPC_SLOT) {
      return stack.getItem() instanceof NpcCatcherItem
          && stack.has(ModDataComponents.CAPTURED_NPC.get());
    }
    if (slot == WORKSTATION_SLOT) {
      return stack.getItem() instanceof BlockItem item
          && !(item.getBlock() instanceof TradingStationBlock);
    }
    return false;
  }

  @Override
  public void setChanged() {
    station.setChanged();
  }

  @Override
  public boolean stillValid(Player player) {
    return Container.stillValidBlockEntity(station, player);
  }

  @Override
  public void clearContent() {
    // the station keeps its content when the menu closes
  }
}
