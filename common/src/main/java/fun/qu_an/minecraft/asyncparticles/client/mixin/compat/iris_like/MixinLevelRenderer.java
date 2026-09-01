package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.iris_like;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.compat.iris.IrisCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	public RenderBuffers renderBuffers;

	@TargetHandler(
		mixin = "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer",
		name = "iris$renderOpaqueParticles"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true)
	public void improveParticlesInWater(CallbackInfo ci) {
		if (ConfigHelper.isIrisImproveOpaqueParticlesInWater() && IrisApi.getInstance().isShaderPackInUse()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderLevel",
		at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"))
	public void improveParticlesInWater(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci,
	                                    @Local(ordinal = 0) ProfilerFiller profiler) {
		if (!ConfigHelper.isIrisImproveOpaqueParticlesInWater() || !IrisApi.getInstance().isShaderPackInUse()) {
			return;
		}
		profiler.popPush("opaque_particles");
		MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
		ParticleRenderingSettings settings = IrisCompat.getParticleRenderingSettings();
		if (settings == ParticleRenderingSettings.BEFORE) {
			this.minecraft.particleEngine.render(poseStack, bufferSource, lightTexture, camera, partialTick);
		} else if (settings == ParticleRenderingSettings.MIXED) {
			((PhasedParticleEngine) this.minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
			this.minecraft.particleEngine.render(poseStack, bufferSource, lightTexture, camera, partialTick);
		}
	}
}
