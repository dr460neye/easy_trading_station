package com.oli.tradingstation.registry;

import com.oli.tradingstation.TradingStationMod;
import de.markusbordihn.easynpc.data.preset.PresetData;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

  public static final DeferredRegister.DataComponents DATA_COMPONENTS =
      DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TradingStationMod.MOD_ID);

  public static final Supplier<DataComponentType<PresetData>> CAPTURED_NPC =
      DATA_COMPONENTS.registerComponentType(
          "captured_npc",
          builder -> builder.persistent(PresetData.CODEC).networkSynchronized(PresetData.STREAM_CODEC));

  private ModDataComponents() {}
}
