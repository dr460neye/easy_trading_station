package de.dragoni.tradingstation.compat;

import com.mojang.serialization.Codec;
import de.dragoni.tradingstation.station.StationNpcData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Everything that touches Easy NPC. Easy NPC is an optional dependency: every method first checks
 * that it is installed, and the Easy NPC classes are only used in {@link EasyNpcImpl}, which is
 * never loaded without it.
 */
public final class EasyNpcCompat {

  private static Boolean loaded;

  private EasyNpcCompat() {}

  public static synchronized boolean loaded() {
    if (loaded == null) {
      ModList list = ModList.get();
      loaded = list != null && list.isLoaded("easy_npc");
    }
    return loaded;
  }

  /** The entity is an Easy NPC. */
  public static boolean is(Entity entity) {
    return loaded() && EasyNpcImpl.is(entity);
  }

  /** Codec of the data format of earlier builds (the Easy NPC preset). Only call if {@link #loaded()}. */
  public static Codec<StationNpcData> legacyCodec() {
    return EasyNpcImpl.legacyCodec();
  }

  /** The preset of an Easy NPC as station data, null if it can not be read. */
  public static StationNpcData capture(Entity entity) {
    return EasyNpcImpl.capture(entity);
  }

  /** Easy NPC's own security check for capturing. */
  public static boolean accessAllowed(ServerPlayer player, Entity entity) {
    return EasyNpcImpl.accessAllowed(player, entity);
  }

  /** Home position of an Easy NPC, or its current position if it has none. */
  public static BlockPos home(Entity entity) {
    return EasyNpcImpl.home(entity);
  }

  /** Spawns a stored Easy NPC at the position; false if Easy NPC refuses. */
  public static boolean spawn(StationNpcData data, Level level, BlockPos pos, ServerPlayer player) {
    return loaded() && EasyNpcImpl.spawn(data, level, pos, player);
  }

  /** Fills a freshly created Easy NPC entity with the stored preset. */
  public static void configure(Entity entity, CompoundTag tag) {
    EasyNpcImpl.configure(entity, tag);
  }

  public static void pause(Entity entity) {
    EasyNpcImpl.pause(entity);
  }

  public static void resume(Entity entity) {
    EasyNpcImpl.resume(entity);
  }

  public static void resetExpiredOffers(Entity entity) {
    EasyNpcImpl.resetExpiredOffers(entity);
  }

  /** The preset of the live Easy NPC, written back into the station. */
  public static CompoundTag serialize(Entity entity) {
    return EasyNpcImpl.serialize(entity);
  }

  /** Trading screen or dialog if the normal interaction did nothing; true if something opened. */
  public static boolean openFallback(Entity entity, ServerPlayer player) {
    return EasyNpcImpl.openFallback(entity, player);
  }
}
