package de.dragoni.tradingstation.station;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class VillagerWorkstationsTest {

  @Test
  void readsTheProfessionFromTheVillagerData() {
    CompoundTag data = new CompoundTag();
    CompoundTag villager = new CompoundTag();
    villager.putString("profession", "irons_jewelry:jeweler");
    villager.putInt("level", 2);
    data.put("VillagerData", villager);
    assertEquals(Identifier.parse("irons_jewelry:jeweler"), VillagerWorkstations.professionOf(data));
  }

  @Test
  void unknownOrMissingDataMeansNoProfession() {
    assertEquals(Identifier.parse("minecraft:none"), VillagerWorkstations.professionOf(new CompoundTag()));
    CompoundTag data = new CompoundTag();
    CompoundTag villager = new CompoundTag();
    villager.putString("profession", "not a valid id!");
    data.put("VillagerData", villager);
    assertEquals(Identifier.parse("minecraft:none"), VillagerWorkstations.professionOf(data));
  }
}
