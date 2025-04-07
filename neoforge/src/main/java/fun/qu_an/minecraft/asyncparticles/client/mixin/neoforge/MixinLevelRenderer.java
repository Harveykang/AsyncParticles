package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@WrapOperation(method = "lambda$addMainPass$2", remap = false,
		at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void shouldRenderParticles(ParticleEngine instance,
									   Camera camera,
									   float v,
									   MultiBufferSource.BufferSource bufferSource,
									   Frustum frustum,
									   Predicate<ParticleRenderType> predicate,
									   Operation<Void> original) {
		if (ModListHelper.IRIS_LIKE_LOADED &&
			AsyncRenderer.isMixedParticleRenderingSetting()) {
			original.call(instance, camera, v, bufferSource, frustum, (Predicate<ParticleRenderType>) p -> !p.translucent());
		}
	}

	@ModifyArg(method = "lambda$addParticlesPass$5", remap = false, index = 4,
		at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private Predicate<ParticleRenderType> shouldRenderParticles(Predicate<ParticleRenderType> predicate) {
		if (ModListHelper.IRIS_LIKE_LOADED &&
			AsyncRenderer.isMixedParticleRenderingSetting()) {
			return ParticleRenderType::translucent;
		} else {
			return predicate;
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
