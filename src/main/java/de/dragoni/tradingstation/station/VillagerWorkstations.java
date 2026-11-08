package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.config.WorkstationRequirement;
import de.markusbordihn.easynpc.data.preset.PresetData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The workstation of a villager comes from its profession: every block that the profession's job
 * site test accepts (the same data the recipe viewers show), all of them as alternatives. Computed
 * once per profession from the game registries and cached.
 */
public final class VillagerWorkstations {

  private static final Map<Identifier, WorkstationRequirement> CACHE = new HashMap<>();

  private VillagerWorkstations() {}

  /** The profession id stored in the villager's data, "minecraft:none" if unknown. */
  static Identifier professionOf(CompoundTag tag) {
    String id = tag.getCompoundOrEmpty("VillagerData").getStringOr("profession", "minecraft:none");
    Identifier parsed = Identifier.tryParse(id);
    return parsed != null ? parsed : Identifier.withDefaultNamespace("none");
  }

  /** The requirement of a stored villager; null for other entities and for villagers without a job site. */
  public static synchronized WorkstationRequirement requirementFor(PresetData data) {
    if (data == null || data.entityType() != EntityType.VILLAGER) {
      return null;
    }
    Identifier profession = professionOf(data.data());
    if (!CACHE.containsKey(profession)) {
      CACHE.put(profession, compute(profession));
    }
    return CACHE.get(profession);
  }

  private static WorkstationRequirement compute(Identifier professionId) {
    var profession = BuiltInRegistries.VILLAGER_PROFESSION.getValue(professionId);
    if (profession == null) {
      return null;
    }
    List<String> ids =
        BuiltInRegistries.POINT_OF_INTEREST_TYPE
            .listElements()
            .filter(profession.heldJobSite())
            .flatMap(holder -> holder.value().matchingStates().stream())
            .map(BlockState::getBlock)
            .distinct()
            .map(block -> BuiltInRegistries.BLOCK.getKey(block))
            .filter(BuiltInRegistries.ITEM::containsKey)
            .map(Identifier::toString)
            .sorted()
            .toList();
    if (ids.isEmpty()) {
      return null;
    }
    String label =
        ids.stream()
            .limit(2)
            .map(id -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))).getHoverName().getString())
            .reduce((a, b) -> a + " / " + b)
            .orElse("")
            + (ids.size() > 2 ? " / ..." : "");
    return new WorkstationRequirement(ids, label);
  }
}
