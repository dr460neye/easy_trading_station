package de.dragoni.tradingstation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.dragoni.tradingstation.station.StationNpc;
import de.dragoni.tradingstation.station.TradingStationBlock;
import de.dragoni.tradingstation.station.TradingStationBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Draws the NPC (cached, never ticking, never added to the level) and the workstation. */
public class TradingStationRenderer
    implements BlockEntityRenderer<TradingStationBlockEntity, TradingStationRenderer.State> {

  public static class State extends BlockEntityRenderState {
    EntityRenderState npc;
    final BlockModelRenderState workstation = new BlockModelRenderState();
    float yaw;
    float npcScale = 1.0F;
    float frontOffsetX;
    float frontOffsetZ;
    boolean hasWorkstation;
    java.util.List<net.minecraft.util.FormattedCharSequence> sign = java.util.List.of();
    float signAngle;
  }

  private final EntityRenderDispatcher entityRenderer;
  private final BlockModelResolver blockModels;

  public TradingStationRenderer(BlockEntityRendererProvider.Context context) {
    this.entityRenderer = context.entityRenderer();
    this.blockModels = context.blockModelResolver();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(
      TradingStationBlockEntity station,
      State state,
      float partialTick,
      Vec3 cameraPos,
      ModelFeatureRenderer.CrumblingOverlay crumbling) {
    BlockEntityRenderer.super.extractRenderState(station, state, partialTick, cameraPos, crumbling);
    BlockState blockState = station.getBlockState();
    if (!blockState.hasProperty(TradingStationBlock.FACING)) {
      return;
    }
    Direction facing = blockState.getValue(TradingStationBlock.FACING);
    // BACK_LOW cell is behind FRONT_LOW; the front cell is one step towards the facing direction
    state.frontOffsetX = facing.getStepX();
    state.frontOffsetZ = facing.getStepZ();
    state.yaw = facing.toYRot();

    state.npc = null;
    if (station.hasNpc() && !station.isInUse() && station.getLevel() != null) {
      if (station.displayEntity == null || station.displayVersion != station.getVersion()) {
        station.displayEntity = StationNpc.create(station.getLevel(), station.getNpc(), EntitySpawnReason.LOAD);
        station.displayVersion = station.getVersion();
        if (station.displayEntity != null) {
          StationNpc.place(station.displayEntity, station.getBlockPos(), facing);
        }
      }
      Entity entity = station.displayEntity;
      if (entity != null) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        EntityRenderer renderer = entityRenderer.getRenderer(entity);
        @SuppressWarnings("unchecked")
        EntityRenderState entityState = renderer.createRenderState(entity, partialTick);
        entityState.lightCoords = state.lightCoords;
        state.npc = entityState;
        // inner height above the podium is 1.8125 blocks (model is ~4% taller than its hitbox); shrink taller NPCs (villager types) to fit
        state.npcScale = Math.min(1.0F, 1.7F / Math.max(entity.getBbHeight(), 0.1F));
      }
    } else {
      station.displayEntity = null;
    }

    state.sign = java.util.List.of();
    if (station.hasNpc() && station.getMissingLabel() != null) {
      var font = net.minecraft.client.Minecraft.getInstance().font;
      var lines = new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
      lines.add(
          net.minecraft.network.chat.Component.translatable("sign.easy_npc_trading_station.strike.title")
              .withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.RED)
              .getVisualOrderText());
      lines.addAll(
          font.split(
              net.minecraft.network.chat.Component.translatable(
                  "sign.easy_npc_trading_station.strike.body", station.getMissingLabel()),
              70));
      state.sign = lines;
      state.signAngle = (float) Math.toDegrees(Math.atan2(facing.getStepX(), facing.getStepZ()));
    }
    state.hasWorkstation = false;
    if (station.getDisplayWorkstation() != null) {
      Block block = BuiltInRegistries.BLOCK.getValue(station.getDisplayWorkstation());
      if (block != Blocks.AIR) {
        blockModels.update(state.workstation, block.defaultBlockState(), BlockDisplayContext.create());
        state.hasWorkstation = !state.workstation.isEmpty();
      }
    }
  }

  @Override
  public void submit(
      State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.npc != null) {
      pose.pushPose();
      pose.translate(0.5, StationNpc.PODIUM_HEIGHT, 0.5);
      pose.scale(state.npcScale, state.npcScale, state.npcScale);
      entityRenderer.submit(state.npc, camera, 0, 0, 0, pose, collector);
      pose.popPose();
    }
    if (!state.sign.isEmpty()) {
      var font = net.minecraft.client.Minecraft.getInstance().font;
      pose.pushPose();
      pose.translate(0.5 + state.frontOffsetX, 0.62, 0.5 + state.frontOffsetZ);
      pose.mulPose(Axis.YP.rotationDegrees(state.signAngle));
      pose.translate(0.0, 0.0, 0.3);
      float scale = 0.0125F;
      pose.scale(scale, -scale, scale);
      float top = -(state.sign.size() * 10) / 2.0F;
      for (int i = 0; i < state.sign.size(); i++) {
        var line = state.sign.get(i);
        collector.submitText(
            pose,
            -font.width(line) / 2.0F,
            top + i * 10,
            line,
            false,
            net.minecraft.client.gui.Font.DisplayMode.NORMAL,
            state.lightCoords,
            0xFFFFFFFF,
            0xFF7A4A1E,
            0);
      }
      pose.popPose();
    }
    if (state.hasWorkstation) {
      pose.pushPose();
      // one cell towards the front, slightly smaller so it sits inside the glass
      pose.translate(0.5 + state.frontOffsetX, 0.0, 0.5 + state.frontOffsetZ);
      pose.mulPose(Axis.YP.rotationDegrees(-state.yaw + 180));
      pose.scale(0.8F, 0.8F, 0.8F);
      pose.translate(-0.5, 1.0 / 16.0, -0.5);
      state.workstation.submit(pose, collector, state.lightCoords, 0, 0);
      pose.popPose();
    }
  }
}
