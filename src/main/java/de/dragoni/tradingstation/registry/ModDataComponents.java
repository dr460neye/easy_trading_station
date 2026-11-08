package de.dragoni.tradingstation.registry;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.station.StationNpcData;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

  public static final DeferredRegister.DataComponents DATA_COMPONENTS =
      DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TradingStationMod.MOD_ID);

  public static final Supplier<DataComponentType<StationNpcData>> CAPTURED_NPC =
      DATA_COMPONENTS.registerComponentType(
          "captured_npc",
          builder -> builder.persistent(StationNpcData.CODEC).networkSynchronized(StationNpcData.STREAM_CODEC));

  /** Marks the upgrade that old stations got automatically; it can not be taken out. */
  public static final Supplier<DataComponentType<Boolean>> LEGACY_UPGRADE =
      DATA_COMPONENTS.registerComponentType(
          "legacy_upgrade",
          builder ->
              builder
                  .persistent(com.mojang.serialization.Codec.BOOL)
                  .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

  private ModDataComponents() {}
}
