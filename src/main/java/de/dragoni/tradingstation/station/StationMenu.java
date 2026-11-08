package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Two slots (NPC catcher, workstation block) plus the player inventory. */
public class StationMenu extends AbstractContainerMenu {

  public static final int NPC_X = 62;
  public static final int WORKSTATION_X = 98;
  public static final int SLOT_Y = 30;
  public static final int INVENTORY_Y = 66;

  private final Container container;

  /** Client side constructor: the server fills the slots. */
  public StationMenu(int id, Inventory inventory) {
    this(id, inventory, new SimpleContainer(StationContainer.SIZE));
  }

  public StationMenu(int id, Inventory inventory, Container container) {
    super(ModMenus.STATION.get(), id);
    this.container = container;
    addSlot(new StationSlot(container, StationContainer.NPC_SLOT, NPC_X, SLOT_Y, inventory.player));
    addSlot(
        new StationSlot(
            container, StationContainer.WORKSTATION_SLOT, WORKSTATION_X, SLOT_Y, inventory.player));
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
      return StationContainer.accepts(index, stack) && !locked();
    }

    @Override
    public boolean mayPickup(Player player) {
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
      for (int target : new int[] {StationContainer.NPC_SLOT, StationContainer.WORKSTATION_SLOT}) {
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
