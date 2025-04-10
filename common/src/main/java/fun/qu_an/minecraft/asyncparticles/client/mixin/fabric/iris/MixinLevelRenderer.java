package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
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

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	@Final
	private LevelTargetBundle targets;

	@Inject(method = "method_62214",
		at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
			// after crumbling buffer source endBatch()
			target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
	private void onAddMain(FogParameters fogParameters,
						   DeltaTracker deltaTracker,
						   Camera camera,
						   ProfilerFiller profilerFiller,
						   Matrix4f matrix4f,
						   Matrix4f matrix4f2,
						   ResourceHandle<?> resourceHandle,
						   ResourceHandle<?> resourceHandle2,
						   ResourceHandle<?> resourceHandle3,
						   ResourceHandle<?> resourceHandle4,
						   boolean bl,
						   Frustum frustum,
						   ResourceHandle<?> resourceHandle5,
						   CallbackInfo ci,
						   @Local(ordinal = 0) MultiBufferSource.BufferSource bufferSource,
						   @Local(ordinal = 0) float f) {
		if (targets.particles == null && AsyncRenderer.isMixedParticleRenderingSetting()) {
			Profiler.get().popPush("opaque_particles");
			ParticleEngine particleEngine = this.minecraft.particleEngine;
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
			particleEngine.render(camera, f, bufferSource);
		}
	}

	@Inject(method = "method_62213", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
	private void onRenderParticles(FogParameters fogParameters,
								   ResourceHandle<?> resourceHandle,
								   ResourceHandle<?> resourceHandle2,
								   Camera camera,
								   float f,
								   CallbackInfo ci) {
		if (AsyncRenderer.isMixedParticleRenderingSetting()) {
			((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		} else {
			((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		RenderSystem.enableDepthTest(); // This fixes the issue with particles not being rendered properly
	}
}
