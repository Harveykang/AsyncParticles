package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fun.qu_an.minecraft.asyncparticles.client.config.ComputeExecutionStage;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Inject(method = "extractLevel", at = @At("HEAD"))
	private void extractLevelHead(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
		if (ConfigHelper.isGpuParticles()) {
			GpuParticleBehavior.getInstance().beginFrame(deltaPartialTick);
			if (ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.LEVEL_EXTRACTION) {
				GpuParticleBehavior.getInstance().compute();
			}
		}
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void renderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
		if (ConfigHelper.isGpuParticles() && ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.LEVEL_RENDERING) {
			GpuParticleBehavior.getInstance().compute();
		}
	}
}
