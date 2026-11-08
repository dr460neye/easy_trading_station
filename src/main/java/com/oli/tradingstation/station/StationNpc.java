package com.oli.tradingstation.station;

import de.markusbordihn.easynpc.data.preset.PresetData;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import net.minecraft.core.BlockPos;
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
  public static Entity create(Level level, PresetData data, EntitySpawnReason reason) {
    if (data == null || !data.hasValidData()) {
      return null;
    }
    Entity entity = data.entityType().create(level, reason);
    if (entity == null) {
      return null;
    }
    var tag = data.data().copy();
    if (entity instanceof EasyNPC<?> npc) {
      npc.registerEasyNPCDefaultData();
      npc.getEasyNPCPresetData().importPresetData(tag);
    } else {
      entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
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
}
