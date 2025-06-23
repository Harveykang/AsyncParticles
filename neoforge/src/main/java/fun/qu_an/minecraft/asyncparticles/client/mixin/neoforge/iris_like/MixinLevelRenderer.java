package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.iris_like;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
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

	@Shadow
	@Final
	private LevelTargetBundle targets;

	// BEFORE
	@Inject(method = "lambda$addMainPass$3",
		at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
			// after crumbling buffer source endBatch()
			target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
	private void onAddMain(GpuBufferSlice gpuBufferSlice,
						   DeltaTracker deltaTracker,
						   Camera camera,
						   ProfilerFiller profilerFiller,
						   Matrix4f matrix4f,
						   Frustum frustum,
						   ResourceHandle resourcehandle2,
						   ResourceHandle resourcehandle3,
						   boolean bl,
						   ResourceHandle resourcehandle1,
						   ResourceHandle resourcehandle,
						   CallbackInfo ci,
						   @Local(ordinal = 0) MultiBufferSource.BufferSource bufferSource,
						   @Local(ordinal = 0) float partialTick,
						   @Share(namespace = "asyncparticles", value = "internalRenderingMode")
						   LocalIntRef irm) {
		// this fixes Iris's bug:
		// - spam logs with some shader pack (e.g. photon shader)
		// - player model (or something else) are invisible
		if (targets.particles != null) {
			return;
		}
		ParticleEngine particleEngine = minecraft.particleEngine;
		switch (irm.get()) {
			case MIXED_ASYNC, MIXED_SYNC ->
				particleEngine.render(camera, partialTick, bufferSource, frustum, renderType -> !renderType.translucent());
			case BEFORE_ASYNC, BEFORE_SYNC ->
				particleEngine.render(camera, partialTick, bufferSource, frustum, p -> true);
		}
	}
}
