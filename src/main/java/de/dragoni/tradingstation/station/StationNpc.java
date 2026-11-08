package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.station.StationNpcData;
import de.dragoni.tradingstation.compat.EasyNpcCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;

/** Builds an NPC entity from stored preset data. Used by the client renderer and the server session. */
public final class StationNpc {

  /** Height of the podium surface above the floor of the NPC cell, in blocks. */
  public static final double PODIUM_HEIGHT = 2.0 / 16.0;

  private StationNpc() {}

  /** Creates the entity without adding it to the level; null if the preset can not be built. */
  public static Entity create(Level level, StationNpcData data, EntitySpawnReason reason) {
    if (data == null || !data.hasValidData()) {
      return null;
    }
    Entity entity = data.entityType().create(level, reason);
    if (entity == null) {
      return null;
    }
    var tag = data.data().copy();
    if (EasyNpcCompat.is(entity)) {
      EasyNpcCompat.configure(entity, tag);
    } else {
      entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
      entity.removeTag(StationSessions.TRANSIENT_TAG);
    }
    return entity;
  }

  /** Places the entity on the podium of the given BACK_LOW cell, looking out of the front window. */
  public static void place(Entity entity, BlockPos backLow, Direction facing) {
    entity.setPos(backLow.getX() + 0.5, backLow.getY() + PODIUM_HEIGHT, backLow.getZ() + 0.5);
    float yaw = facing.toYRot();
    entity.setYRot(yaw);
    entity.setYHeadRot(yaw);
    entity.setYBodyRot(yaw);
    entity.setXRot(0);
    // without this the renderer interpolates from rotation 0 every frame and the NPC spins
    entity.setOldPosAndRot();
    if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
      living.yBodyRotO = yaw;
      living.yHeadRotO = yaw;
    }
  }

  // ---- NPCs that are not Easy NPCs (standard villagers, wandering traders) ------------------

  /** Keys the station keeps in the stored data next to the entity's own data. */
  public static final String RESTOCK_KEY = "TradingStationRestock";

  public static final String OFFERS_DAY_KEY = "TradingStationOffersDay";

  /** True for entity types of Easy NPC. */
  public static boolean isEasyNpc(net.minecraft.world.entity.EntityType<?> type) {
    return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
        .getKey(type)
        .getNamespace()
        .equals("easy_npc");
  }

  /** Entities the catcher can hold besides Easy NPCs (more through the allow list, later). */
  public static boolean isGenericSupported(Entity entity) {
    var type = entity.getType();
    return type == net.minecraft.world.entity.EntityType.VILLAGER
        || type == net.minecraft.world.entity.EntityType.WANDERING_TRADER;
  }

  public static boolean easyVillagersInstalled() {
    return net.neoforged.fml.ModList.get().isLoaded("easy_villagers");
  }

  /** The state of a non-Easy-NPC entity as station data. Identity and despawn timer are left out. */
  public static net.minecraft.nbt.CompoundTag entityTag(Entity entity) {
    var output =
        net.minecraft.world.level.storage.TagValueOutput.createWithContext(
            ProblemReporter.DISCARDING, entity.level().registryAccess());
    // the marker of a running station session must not be stored, or the released entity is removed again
    boolean marked = entity.entityTags().contains(StationSessions.TRANSIENT_TAG);
    if (marked) {
      entity.removeTag(StationSessions.TRANSIENT_TAG);
    }
    entity.saveWithoutId(output);
    if (marked) {
      entity.addTag(StationSessions.TRANSIENT_TAG);
    }
    net.minecraft.nbt.CompoundTag tag = output.buildResult();
    tag.remove("UUID");
    // The brain stays: its job site memory keeps a released villager from losing its profession
    // (Easy Villagers keeps it too); no experience is added.
    tag.remove("DespawnDelay");
    return tag;
  }

  /** Copies the station's own bookkeeping keys of the old data into the new data. */
  public static de.dragoni.tradingstation.station.StationNpcData carryKeys(
      de.dragoni.tradingstation.station.StationNpcData from,
      de.dragoni.tradingstation.station.StationNpcData to) {
    if (from == null || to == null) {
      return to;
    }
    net.minecraft.nbt.CompoundTag tag = to.data().copy();
    for (String key : new String[] {RESTOCK_KEY, OFFERS_DAY_KEY}) {
      if (from.data().contains(key)) {
        tag.put(key, from.data().get(key).copy());
      }
    }
    return new de.dragoni.tradingstation.station.StationNpcData(to.entityType(), tag);
  }

  /** Releases a stored non-Easy-NPC entity at the position. */
  public static boolean spawnGeneric(
      net.minecraft.server.level.ServerLevel level,
      de.dragoni.tradingstation.station.StationNpcData data,
      BlockPos pos,
      float yaw) {
    Entity entity = create(level, data, EntitySpawnReason.SPAWN_ITEM_USE);
    if (entity == null) {
      return false;
    }
    entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    entity.setYRot(yaw + 180.0F);
    entity.setOldPosAndRot();
    return level.addFreshEntity(entity);
  }

  /**
   * The text shown on the front frame of the station: the own name of the NPC if it has one, for a
   * villager its profession, otherwise the name of the entity type.
   */
  public static net.minecraft.network.chat.Component title(
      de.dragoni.tradingstation.station.StationNpcData data) {
    var named = de.dragoni.tradingstation.item.NpcCatcherItem.nameComponent(data);
    if (named != null) {
      return named;
    }
    if (data.entityType() == net.minecraft.world.entity.EntityType.VILLAGER) {
      Identifier profession = VillagerWorkstations.professionOf(data.data());
      String key =
          "entity.minecraft.villager."
              + (profession.getNamespace().equals("minecraft")
                  ? profession.getPath()
                  : profession.getNamespace() + "." + profession.getPath());
      return net.minecraft.network.chat.Component.translatableWithFallback(key, prettify(profession.getPath()));
    }
    return data.entityType().getDescription();
  }

  /** "fluix_researcher" -> "Fluix Researcher" (used when a profession has no translation). */
  static String prettify(String path) {
    StringBuilder out = new StringBuilder();
    for (String word : path.split("[_./]")) {
      if (!word.isEmpty()) {
        out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
      }
    }
    return out.toString().strip();
  }
}
