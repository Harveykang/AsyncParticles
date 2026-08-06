package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.config.ComputeExecutionStage;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class MixinLevelRenderer {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void onRenderLevelHead(DeltaTracker deltaTracker,
	                               boolean renderBlockOutline,
	                               Camera camera,
	                               GameRenderer gameRenderer,
	                               LightTexture lightTexture,
	                               Matrix4f frustumMatrix,
	                               Matrix4f projectionMatrix,
	                               CallbackInfo ci) {
		if (ConfigHelper.isGpuParticles()) {
			float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
			GpuParticleBehavior.getInstance().beginFrame(partialTick);
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=sky"))
	private void onRenderLevelSky(DeltaTracker deltaTracker,
	                              boolean renderBlockOutline,
	                              Camera camera,
	                              GameRenderer gameRenderer,
	                              LightTexture lightTexture,
	                              Matrix4f frustumMatrix,
	                              Matrix4f projectionMatrix,
	                              CallbackInfo ci,
	                              @Local(ordinal = 0) float partialTick) {
		if (ConfigHelper.isGpuParticles()
			&& ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.LEVEL_RENDERING) {
			GpuParticleBehavior.getInstance().compute(camera, partialTick);
		}
	}
}
