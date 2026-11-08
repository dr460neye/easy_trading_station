package de.dragoni.tradingstation.client;

import de.dragoni.tradingstation.station.StationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Plain panel with two labelled slots; drawn with fills so no texture is needed. */
public class StationScreen extends AbstractContainerScreen<StationMenu> {

  private static final int PANEL = 0xFFC6C6C6;
  private static final int LIGHT = 0xFFFFFFFF;
  private static final int DARK = 0xFF555555;
  private static final int SLOT_DARK = 0xFF373737;
  private static final int SLOT_FILL = 0xFF8B8B8B;

  public StationScreen(StationMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title, 176, StationMenu.INVENTORY_Y + 86);
    this.inventoryLabelY = StationMenu.INVENTORY_Y - 11;
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractBackground(graphics, mouseX, mouseY, partialTick);
    int x = leftPos;
    int y = topPos;
    graphics.fill(x, y, x + imageWidth, y + imageHeight, SLOT_DARK);
    graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, LIGHT);
    graphics.fill(x + 2, y + 2, x + imageWidth - 1, y + imageHeight - 1, DARK);
    graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL);
    for (Slot slot : menu.slots) {
      int sx = x + slot.x;
      int sy = y + slot.y;
      graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_DARK);
      graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_FILL);
    }
    Component npc = Component.translatable("container.easy_npc_trading_station.npc");
    Component workstation = Component.translatable("container.easy_npc_trading_station.workstation");
    int labelX = x + StationMenu.SLOT_X + 16 + 6;
    graphics.text(font, npc, labelX, y + StationMenu.NPC_Y + 4, 0xFF404040, false);
    graphics.text(font, workstation, labelX, y + StationMenu.WORKSTATION_Y + 4, 0xFF404040, false);
  }
}
