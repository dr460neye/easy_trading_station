package de.dragoni.tradingstation.registry;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.station.StationVariant;
import de.dragoni.tradingstation.station.TradingStationBlock;
import de.dragoni.tradingstation.station.TradingStationBlockEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Stations are registered from one variant list inside {@link RegisterEvent}, so wood types added
 * by other mods (already present at that time) get a block without any code change (S6.10).
 */
public final class ModBlocks {

  public static final Map<StationVariant, TradingStationBlock> STATIONS = new LinkedHashMap<>();
  public static final Map<StationVariant, BlockItem> STATION_ITEMS = new LinkedHashMap<>();
  public static BlockEntityType<TradingStationBlockEntity> STATION_BLOCK_ENTITY;

  private ModBlocks() {}

  public static void init(IEventBus modEventBus) {
    modEventBus.addListener(ModBlocks::onRegister);
  }

  public static List<StationVariant> variants() {
    return List.copyOf(STATIONS.keySet());
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(TradingStationMod.MOD_ID, path);
  }

  private static void onRegister(RegisterEvent event) {
    if (event.getRegistryKey().equals(Registries.BLOCK)) {
      for (StationVariant variant : StationVariant.discover()) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(variant.blockName()));
        Block.Properties properties =
            Block.Properties.of()
                .setId(key)
                .strength(variant.futuristic() ? 3.0F : 2.0F, 3600000.0F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .pushReaction(PushReaction.BLOCK);
        TradingStationBlock block = new TradingStationBlock(properties);
        STATIONS.put(variant, block);
        event.register(Registries.BLOCK, key.identifier(), () -> block);
      }
    } else if (event.getRegistryKey().equals(Registries.ITEM)) {
      STATIONS.forEach(
          (variant, block) -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(variant.blockName()));
            BlockItem item =
                new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
            STATION_ITEMS.put(variant, item);
            event.register(Registries.ITEM, key.identifier(), () -> item);
          });
    } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
      event.register(
          Registries.BLOCK_ENTITY_TYPE,
          id("trading_station"),
          () -> {
            STATION_BLOCK_ENTITY =
                new BlockEntityType<>(
                    TradingStationBlockEntity::new, java.util.Set.copyOf(STATIONS.values()));
            return STATION_BLOCK_ENTITY;
          });
    }
  }
}
