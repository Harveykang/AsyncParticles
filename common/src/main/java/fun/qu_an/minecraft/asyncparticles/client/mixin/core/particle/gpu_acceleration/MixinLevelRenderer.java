package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void renderLevelHead(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix, Matrix4f cullingProjectionMatrix, GpuBufferSlice shaderFog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
		if (ConfigHelper.isGpuParticles()) {
			GpuParticleBehavior.getInstance().beginFrame(deltaTracker.getGameTimeDeltaPartialTick(false));
			GpuParticleBehavior.getInstance().compute();
		}
	}
}
