package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.station.StationNpcData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Remembers where a captured (not cloned) NPC came from, inside the preset NBT, so it travels with
 * the catcher and the station. If the catcher is destroyed, the NPC "dies" at that place: the
 * normal death events run, so modpack scripts (e.g. shopkeeper respawn ledgers) behave as if the
 * NPC had been killed. Clones carry no origin and are simply lost with the catcher.
 */
public final class NpcOrigin {

  private static final String KEY = "TradingStationOrigin";
  private static final List<StationNpcData> PENDING = new ArrayList<>();

  private NpcOrigin() {}

  public static StationNpcData mark(StationNpcData data, Level level, BlockPos pos) {
    CompoundTag tag = data.data().copy();
    CompoundTag origin = new CompoundTag();
    origin.putString("dim", level.dimension().identifier().toString());
    origin.putInt("x", pos.getX());
    origin.putInt("y", pos.getY());
    origin.putInt("z", pos.getZ());
    tag.put(KEY, origin);
    return new StationNpcData(data.entityType(), tag);
  }

  public static boolean has(StationNpcData data) {
    return data != null && data.data().contains(KEY);
  }

  /** The entity writes its preset back without our key, so carry it over. */
  public static StationNpcData carryOver(StationNpcData from, StationNpcData to) {
    if (from == null || to == null || !has(from) || has(to)) {
      return to;
    }
    CompoundTag tag = to.data().copy();
    tag.put(KEY, from.data().get(KEY).copy());
    return new StationNpcData(to.entityType(), tag);
  }

  /** Called when a filled catcher with an origin was destroyed. Processed on the next tick. */
  public static synchronized void queueDeath(StationNpcData data) {
    PENDING.add(data);
  }

  public static synchronized void processQueue(MinecraftServer server) {
    if (PENDING.isEmpty()) {
      return;
    }
    List<StationNpcData> work = List.copyOf(PENDING);
    PENDING.clear();
    for (StationNpcData data : work) {
      try {
        die(server, data);
      } catch (RuntimeException e) {
        TradingStationMod.LOGGER.error("Could not process the loss of a captured NPC", e);
      }
    }
  }

  private static void die(MinecraftServer server, StationNpcData data) {
    CompoundTag origin = data.data().getCompound(KEY).orElse(null);
    if (origin == null) {
      return;
    }
    Identifier dimension = Identifier.tryParse(origin.getStringOr("dim", ""));
    ServerLevel level =
        dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    if (level == null) {
      return;
    }
    Entity entity = StationNpc.create(level, data, EntitySpawnReason.LOAD);
    if (!(entity instanceof LivingEntity living)) {
      return;
    }
    entity.setPos(origin.getIntOr("x", 0) + 0.5, origin.getIntOr("y", 0), origin.getIntOr("z", 0) + 0.5);
    entity.setInvulnerable(false);
    // not added to the level: only the death events matter, nothing spawns or drops
    living.die(level.damageSources().genericKill());
    TradingStationMod.LOGGER.info("Captured NPC {} lost with its catcher, death events fired", entity.getType());
  }
}
