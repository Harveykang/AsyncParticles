package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;

	// BEFORE
	@Inject(method = "method_62214", at = @At(value = "INVOKE", ordinal = 1, shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
	private void onRenderMain(CallbackInfo ci,
							  @Local(argsOnly = true) Camera camera,
							  @Local(argsOnly = true) ProfilerFiller profiler,
							  @Local(ordinal = 0) MultiBufferSource.BufferSource bufferSource,
							  @Local(ordinal = 0) float partialTick) {
		switch (InternalRenderingMode.getMode()) {
			case MIXED_ASYNC, MIXED_SYNC, COMPATIBILITY_ASYNC, SYNC -> {
				profiler.popPush("opaque_particles");
				ParticleEngine particleEngine = this.minecraft.particleEngine;
				((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
				particleEngine.render(camera, partialTick, bufferSource);
			}
			case BEFORE_ASYNC, BEFORE_SYNC -> {
				profiler.popPush("opaque_particles");
				ParticleEngine particleEngine = this.minecraft.particleEngine;
				((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
				particleEngine.render(camera, partialTick, bufferSource);
			}
		}
	}

	// AFTER
	@Inject(method = "method_62213", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
	private void onRenderParticles(GpuBufferSlice gpuBufferSlice,
								   ResourceHandle resourceHandle,
								   ResourceHandle resourceHandle2,
								   Camera camera,
								   float partialTick,
								   CallbackInfo ci) {
		switch (InternalRenderingMode.getMode()) {
			case MIXED_ASYNC, MIXED_SYNC, COMPATIBILITY_ASYNC, SYNC ->
				((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
			case DELAYED_ASYNC ->
				((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
	}
}
