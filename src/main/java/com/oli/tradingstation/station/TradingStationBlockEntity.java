package com.oli.tradingstation.station;

import com.oli.tradingstation.config.TradingStationConfig;
import com.oli.tradingstation.config.WorkstationRequirement;
import com.oli.tradingstation.item.NpcCatcherItem;
import com.oli.tradingstation.registry.ModBlocks;
import de.markusbordihn.easynpc.data.preset.PresetData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Lives on the BACK_LOW cell. Stores only data; nothing here ticks. */
public class TradingStationBlockEntity extends BlockEntity {

  private PresetData npc;
  private ItemStack workstation = ItemStack.EMPTY;
  private Identifier displayWorkstation;
  private boolean inUse;
  private net.minecraft.network.chat.Component missingLabel;

  /** Client only: cached non-ticking entity for the renderer, rebuilt when {@link #version} changes. */
  public Entity displayEntity;

  public int displayVersion = -1;
  private int version;

  public TradingStationBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlocks.STATION_BLOCK_ENTITY, pos, state);
  }

  public PresetData getNpc() {
    return npc;
  }

  public boolean hasNpc() {
    return npc != null;
  }

  public ItemStack getWorkstation() {
    return workstation;
  }

  /** True while a transient NPC entity stands in the station; the renderer then hides its copy. */
  /** Server: the NPC's required workstation (if any) is present. Client: synced copy. */
  public boolean isNpcActive() {
    return npc != null && (level != null && level.isClientSide() ? missingLabel == null : requirementMet());
  }

  public WorkstationRequirement requirement() {
    if (npc == null) {
      return null;
    }
    try {
      return TradingStationConfig.requirementFor(npc.entityType(), NpcCatcherItem.npcName(npc));
    } catch (RuntimeException configNotLoaded) {
      return null;
    }
  }

  private boolean requirementMet() {
    WorkstationRequirement requirement = requirement();
    return requirement == null || requirement.matches(workstation);
  }

  /** Client: set while the NPC's required workstation is missing (text for the strike sign). */
  public net.minecraft.network.chat.Component getMissingLabel() {
    return missingLabel;
  }

  public boolean isInUse() {
    return inUse;
  }

  public void setInUse(boolean inUse) {
    if (this.inUse != inUse) {
      this.inUse = inUse;
      changed();
    }
  }

  public int getVersion() {
    return version;
  }

  public Identifier getDisplayWorkstation() {
    return displayWorkstation;
  }

  public void setNpc(PresetData data) {
    this.npc = data;
    changed();
  }

  public PresetData takeNpc() {
    PresetData data = npc;
    npc = null;
    changed();
    return data;
  }

  public void setWorkstation(ItemStack stack) {
    this.workstation = stack;
    changed();
  }

  public ItemStack takeWorkstation() {
    ItemStack stack = workstation;
    workstation = ItemStack.EMPTY;
    changed();
    return stack;
  }

  private void changed() {
    setChanged();
    if (level != null && !level.isClientSide()) {
      BlockState state = getBlockState();
      level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }
  }

  /** Server side: the block shown on the workstation cell. */
  private Identifier effectiveWorkstation() {
    if (!requirementMet()) {
      return null; // the strike sign is shown instead
    }
    if (!workstation.isEmpty() && workstation.getItem() instanceof BlockItem blockItem) {
      return BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
    }
    try {
      return npc != null
          ? TradingStationConfig.defaultWorkstationFor(npc.entityType(), NpcCatcherItem.npcName(npc))
          : TradingStationConfig.defaultWorkstationFor(null, "");
    } catch (RuntimeException e) {
      return Identifier.withDefaultNamespace("crafting_table");
    }
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    if (npc != null) {
      output.store("Npc", PresetData.CODEC, npc);
    }
    if (!workstation.isEmpty()) {
      output.store("Workstation", ItemStack.CODEC, workstation);
    }
    if (level != null && !level.isClientSide()) {
      Identifier display = effectiveWorkstation();
      if (display != null) {
        output.putString("DisplayWorkstation", display.toString());
      }
      WorkstationRequirement requirement = requirement();
      if (requirement != null && !requirement.matches(workstation)) {
        output.store("MissingLabel", net.minecraft.network.chat.ComponentSerialization.CODEC, requirement.displayName());
      }
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    npc = input.read("Npc", PresetData.CODEC).orElse(null);
    workstation = input.read("Workstation", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    displayWorkstation = input.getString("DisplayWorkstation").map(Identifier::tryParse).orElse(null);
    inUse = input.getBooleanOr("InUse", false);
    missingLabel = input.read("MissingLabel", net.minecraft.network.chat.ComponentSerialization.CODEC).orElse(null);
    version++;
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    CompoundTag tag = saveCustomOnly(registries);
    tag.putBoolean("InUse", inUse); // not persisted to disk, only sent to clients
    return tag;
  }

  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  /** The NPC and the workstation item must never be lost when the station disappears. */
  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    super.preRemoveSideEffects(pos, state);
    Level level = getLevel();
    if (level == null || level.isClientSide()) {
      return;
    }
    StationSessions.finishAt(this);
    if (npc != null) {
      Block.popResource(level, pos, NpcCatcherItem.filled(npc));
      npc = null;
    }
    if (!workstation.isEmpty()) {
      Block.popResource(level, pos, workstation);
      workstation = ItemStack.EMPTY;
    }
  }
}
