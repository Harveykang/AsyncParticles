package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
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
	@Inject(method = "method_62214",
		at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
			// after crumbling buffer source endBatch()
			target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
	private void onRenderMain(GpuBufferSlice gpuBufferSlice,
						   DeltaTracker deltaTracker,
						   Camera camera,
						   ProfilerFiller profilerFiller,
						   Matrix4f matrix4f,
						   ResourceHandle resourceHandle,
						   ResourceHandle resourceHandle2,
						   boolean bl,
						   Frustum frustum,
						   ResourceHandle resourceHandle3,
						   ResourceHandle resourceHandle4,
						   CallbackInfo ci,
						   @Local(ordinal = 0) MultiBufferSource.BufferSource bufferSource,
						   @Local(ordinal = 0) float partialTick,
						   @Share(namespace = "asyncparticles", value = "internalRenderingMode")
						   LocalIntRef irm) {
		switch (irm.get()) {
			case IRIS_MIXED_ASYNC, IRIS_MIXED_SYNC -> {
				ParticleEngine particleEngine = this.minecraft.particleEngine;
				Profiler.get().popPush("opaque_particles");
				((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
				particleEngine.render(camera, partialTick, bufferSource);
			}
			case IRIS_BEFORE_ASYNC, IRIS_BEFORE_SYNC -> {
				ParticleEngine particleEngine = this.minecraft.particleEngine;
				Profiler.get().popPush("opaque_particles");
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
								   CallbackInfo ci,
								   @Share(namespace = "asyncparticles", value = "internalRenderingMode")
								   LocalIntRef irm) {
		switch (irm.get()) {
			case IRIS_MIXED_ASYNC, IRIS_MIXED_SYNC ->
				((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
			case DELAYED_ASYNC, COMPATIBILITY_ASYNC, SYNC ->
				((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
	}
}
