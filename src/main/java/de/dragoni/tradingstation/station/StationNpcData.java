package de.dragoni.tradingstation.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;

/**
 * An NPC as the catcher and the station store it: the entity type and its data. For Easy NPCs the
 * data is the Easy NPC preset, for villagers and traders the saved entity. Nothing here needs Easy
 * NPC, so the mod also works without it; data written by earlier builds in the Easy NPC format is
 * still read (only when Easy NPC is installed).
 */
public record StationNpcData(EntityType<?> entityType, CompoundTag data) {

  private static final Codec<StationNpcData> CURRENT =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("npc_entity").forGetter(StationNpcData::entityType),
                      CompoundTag.CODEC.fieldOf("npc_data").forGetter(StationNpcData::data))
                  .apply(instance, StationNpcData::new));

  public static final Codec<StationNpcData> CODEC =
      de.dragoni.tradingstation.compat.EasyNpcCompat.loaded()
          ? Codec.withAlternative(CURRENT, de.dragoni.tradingstation.compat.EasyNpcCompat.legacyCodec())
          : CURRENT;

  public static final StreamCodec<RegistryFriendlyByteBuf, StationNpcData> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.registry(Registries.ENTITY_TYPE),
          StationNpcData::entityType,
          ByteBufCodecs.COMPOUND_TAG,
          StationNpcData::data,
          StationNpcData::new);

  public boolean hasValidData() {
    return entityType != null && data != null && !data.isEmpty();
  }
}
