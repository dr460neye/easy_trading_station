package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.item.StationUpgradeItem;
import de.dragoni.tradingstation.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Three slots (NPC catcher, workstation block, upgrade) plus the player inventory. */
public class StationMenu extends AbstractContainerMenu {

  // Slots are stacked; the screen draws each label to the right of its slot. Positions are only used
  // on the client, so changing them does not affect servers or clients on another version.
  public static final int SLOT_X = 8;
  public static final int NPC_Y = 20;
  public static final int WORKSTATION_Y = 42;
  public static final int UPGRADE_Y = 64;
  public static final int INVENTORY_Y = 97;

  private final Container container;

  /** Client side constructor: the server fills the slots. */
  public StationMenu(int id, Inventory inventory) {
    this(id, inventory, new SimpleContainer(StationContainer.SIZE));
  }

  public StationMenu(int id, Inventory inventory, Container container) {
    super(ModMenus.STATION.get(), id);
    this.container = container;
    addSlot(new StationSlot(container, StationContainer.NPC_SLOT, SLOT_X, NPC_Y, inventory.player));
    addSlot(
        new StationSlot(
            container, StationContainer.WORKSTATION_SLOT, SLOT_X, WORKSTATION_Y, inventory.player));
    addSlot(
        new StationSlot(
            container, StationContainer.UPGRADE_SLOT, SLOT_X, UPGRADE_Y, inventory.player));
    addStandardInventorySlots(inventory, 8, INVENTORY_Y);
  }

  private static class StationSlot extends Slot {
    private final Container container;
    private final Player owner;
    private final int index;

    StationSlot(Container container, int index, int x, int y, Player owner) {
      super(container, index, x, y);
      this.container = container;
      this.owner = owner;
      this.index = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
      if (!StationContainer.accepts(index, stack) || locked()) {
        return false;
      }
      if (index == StationContainer.NPC_SLOT) {
        return StationContainer.upgradeAllows(container.getItem(StationContainer.UPGRADE_SLOT), stack);
      }
      if (index == StationContainer.UPGRADE_SLOT) {
        // a replacement must still fit the NPC that is inside
        return StationContainer.upgradeAllows(stack, container.getItem(StationContainer.NPC_SLOT));
      }
      return true;
    }

    @Override
    public boolean mayPickup(Player player) {
      if (index == StationContainer.UPGRADE_SLOT) {
        // legacy upgrades stay; so does an upgrade the NPC inside needs
        return !StationUpgradeItem.isLegacy(getItem())
            && StationContainer.requiredUpgrade(container.getItem(StationContainer.NPC_SLOT)).isEmpty();
      }
      return !locked();
    }

    /** The NPC slot is locked while a player trades with the live NPC (server side only). */
    private boolean locked() {
      return index == StationContainer.NPC_SLOT
          && container instanceof StationContainer station
          && station.isNpcLocked(owner);
    }

    @Override
    public int getMaxStackSize() {
      return 1;
    }
  }

  /** Tells the player why an NPC or an upgrade was refused (server side only). */
  @Override
  public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput clickType, Player player) {
    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && slotId >= 0
        && slotId < StationContainer.SIZE) {
      ItemStack carried = getCarried();
      if (slotId == StationContainer.NPC_SLOT && StationContainer.accepts(slotId, carried)
          && !StationContainer.upgradeAllows(container.getItem(StationContainer.UPGRADE_SLOT), carried)) {
        needsUpgrade(serverPlayer, StationContainer.requiredUpgrade(carried));
      } else if (slotId == StationContainer.UPGRADE_SLOT && carried.isEmpty()
          && !slots.get(slotId).getItem().isEmpty() && !slots.get(slotId).mayPickup(player)) {
        serverPlayer.sendOverlayMessage(
            net.minecraft.network.chat.Component.translatable("message.easy_npc_trading_station.upgrade_locked"));
      }
    }
    super.clicked(slotId, button, clickType, player);
  }

  public static void needsUpgrade(net.minecraft.server.level.ServerPlayer player, String kind) {
    player.sendOverlayMessage(
        net.minecraft.network.chat.Component.translatable(
            "message.easy_npc_trading_station.needs_upgrade",
            net.minecraft.network.chat.Component.translatable(
                "item.easy_npc_trading_station.station_upgrade_" + kind)));
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    Slot slot = slots.get(index);
    if (slot == null || !slot.hasItem()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getItem();
    ItemStack copy = stack.copy();
    if (index < StationContainer.SIZE) {
      if (!slot.mayPickup(player)
          || !moveItemStackTo(stack, StationContainer.SIZE, slots.size(), true)) {
        return ItemStack.EMPTY;
      }
    } else {
      boolean moved = false;
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
          && StationContainer.accepts(StationContainer.NPC_SLOT, stack)
          && !StationContainer.upgradeAllows(container.getItem(StationContainer.UPGRADE_SLOT), stack)) {
        needsUpgrade(serverPlayer, StationContainer.requiredUpgrade(stack));
        return ItemStack.EMPTY;
      }
      for (int target :
          new int[] {
            StationContainer.NPC_SLOT, StationContainer.WORKSTATION_SLOT, StationContainer.UPGRADE_SLOT
          }) {
        Slot slotTarget = slots.get(target);
        if (!slotTarget.hasItem() && slotTarget.mayPlace(stack)) {
          slotTarget.setByPlayer(stack.split(1));
          moved = true;
          break;
        }
      }
      if (!moved) {
        return ItemStack.EMPTY;
      }
    }
    if (stack.isEmpty()) {
      slot.setByPlayer(ItemStack.EMPTY);
    } else {
      slot.setChanged();
    }
    return copy;
  }

  @Override
  public boolean stillValid(Player player) {
    return container.stillValid(player) || container instanceof SimpleContainer;
  }
}
