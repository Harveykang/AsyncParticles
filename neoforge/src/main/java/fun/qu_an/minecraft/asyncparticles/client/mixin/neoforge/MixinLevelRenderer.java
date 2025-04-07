package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private LevelTargetBundle targets;

	@Inject(method = "lambda$addMainPass$2",
		at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
			// after crumbling buffer source endBatch()
			target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
	private void onAddMain(FogParameters arg,
						   DeltaTracker arg2,
						   Camera camera,
						   ProfilerFiller arg4,
						   Matrix4f matrix4f,
						   Matrix4f matrix4f2,
						   ResourceHandle<?> resourcehandle2,
						   ResourceHandle<?> resourcehandle,
						   ResourceHandle<?> resourcehandle3,
						   ResourceHandle<?> resourcehandle4,
						   Frustum frustum,
						   boolean bl,
						   ResourceHandle<?> resourcehandle1,
						   CallbackInfo ci,
						   @Local(ordinal = 0) MultiBufferSource.BufferSource bufferSource,
						   @Local(ordinal = 0) float f) {
		// this fixes Iris bug:
		// - spam logs with some shader pack (e.g. photon shader)
		// - player model (or something else) are invisible
		if (targets.particles == null && AsyncRenderer.isMixedParticleRenderingSetting()) {
			Profiler.get().popPush("solid_particles");
			minecraft.particleEngine.render(camera, f, bufferSource, frustum, p -> !p.translucent());
		}
	}

	@Redirect(method = "lambda$addMainPass$2", remap = false,
		at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void onAddMain(ParticleEngine instance,
						   Camera camera,
						   float v,
						   MultiBufferSource.BufferSource bufferSource1,
						   Frustum frustum,
						   Predicate<ParticleRenderType> predicate) {
//		if (ModListHelper.IRIS_LIKE_LOADED &&
//			AsyncRenderer.isMixedParticleRenderingSetting()) {
//			original.call(instance, camera, v, bufferSource1, frustum, (Predicate<ParticleRenderType>) p -> !p.translucent());
//		}
	}

	@ModifyArg(method = "lambda$addParticlesPass$5", remap = false, index = 4,
		at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private Predicate<ParticleRenderType> shouldRenderParticles(Predicate<ParticleRenderType> predicate) {
		if (ModListHelper.IRIS_LIKE_LOADED &&
			AsyncRenderer.isMixedParticleRenderingSetting()) {
			return ParticleRenderType::translucent;
		} else {
			return p -> true;
		}
	}

	@WrapOperation(method = "renderLevel",
		at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/client/renderer/LevelRenderer;addParticlesPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/FogParameters;Lnet/minecraft/client/renderer/culling/Frustum;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
	private void redirectAddParticlesPass(LevelRenderer instance,
										  FrameGraphBuilder frameGraphBuilder,
										  Camera camera,
										  float f,
										  FogParameters framepass,
										  Frustum frustum,
										  Matrix4f modelViewMatrix,
										  Matrix4f projectionMatrix,
										  Operation<Void> original,
										  @Share("asyncparticles$addParticlesPassOperation") LocalRef<Operation<Void>> originalRef) {
//		this.asyncparticles$addParticlesPassOperation = original;
		// do nothing, we'll call the original method later
		if (SimplePropertiesConfig.isRenderAsync()) {
			originalRef.set(original);
		} else {
			original.call(instance, frameGraphBuilder, camera, f, framepass, frustum, modelViewMatrix, projectionMatrix);
		}
	}

	@Inject(method = "renderLevel",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V"))
	private void onRenderLevelTail(GraphicsResourceAllocator graphicsResourceAllocator,
								   DeltaTracker deltaTracker,
								   boolean renderBlockOutline,
								   Camera camera,
								   GameRenderer gameRenderer,
								   Matrix4f frustumMatrix,
								   Matrix4f projectionMatrix,
								   CallbackInfo ci,
								   @Local(ordinal = 0) FrameGraphBuilder frameGraphBuilder,
								   @Local(ordinal = 0) float f,
								   @Local(ordinal = 0) FogParameters fogParameters,
								   @Local(ordinal = 0) Frustum frustum,
								   @Share("asyncparticles$addParticlesPassOperation") LocalRef<Operation<Void>> originalRef) {
		// as late as possible
//		this.asyncparticles$addParticlesPassOperation.call(frameGraphBuilder, camera, f, fogParameters);
		if (SimplePropertiesConfig.isRenderAsync()) {
			originalRef.get().call(this, frameGraphBuilder, camera, f, fogParameters, frustum, frustumMatrix, projectionMatrix);
		}
	}
}
