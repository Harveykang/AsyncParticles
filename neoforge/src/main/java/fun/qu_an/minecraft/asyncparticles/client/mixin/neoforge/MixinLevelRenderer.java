package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@WrapOperation(method = "renderLevel",
		at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/client/renderer/LevelRenderer;addParticlesPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;FLcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/culling/Frustum;Lorg/joml/Matrix4f;)V"))
	private void redirectAddParticlesPass(LevelRenderer instance,
										  FrameGraphBuilder frameGraphBuilder,
										  Camera camera,
										  float partialTick,
										  GpuBufferSlice gpuBufferSlice,
										  Frustum frustum,
										  Matrix4f matrix4f,
										  Operation<Void> original,
										  @Share("asyncparticles$addParticlesPassOperation")
										  LocalRef<Operation<Void>> originalRef,
										  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										  LocalIntRef irm) {
		switch (irm.get()) {
			case COMPATIBILITY_ASYNC, MIXED_ASYNC, SYNC, MIXED_SYNC ->
				original.call(instance, frameGraphBuilder, camera, partialTick, gpuBufferSlice, frustum, matrix4f);
			case BEFORE_SYNC, BEFORE_ASYNC -> {
				// no-op
			}
			case DELAYED_ASYNC -> originalRef.set(original);
		}
	}

	@Inject(method = "renderLevel", order = 1500,
		slice = @Slice(from = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/client/renderer/LevelRenderer;addWeatherPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/world/phys/Vec3;FLcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;Lnet/minecraft/client/Camera;)V")),
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;addToFrame(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;IILnet/minecraft/client/renderer/PostChain$TargetBundle;)V"))
	private void onRenderLevelTail1(GraphicsResourceAllocator graphicsResourceAllocator,
									DeltaTracker deltaTracker,
									boolean bl,
									Camera camera,
									Matrix4f modelViewMatrix, // first one
									Matrix4f matrix4f2,
									GpuBufferSlice gpuBufferSlice,
									Vector4f vector4f,
									boolean bl2,
									CallbackInfo ci,
									@Local(ordinal = 0) FrameGraphBuilder frameGraphBuilder,
									@Local(ordinal = 0) float partialTick,
									@Local(ordinal = 0) Frustum frustum,
									@Share("asyncparticles$addParticlesPassOperation")
									LocalRef<Operation<Void>> originalRef,
									@Share(namespace = "asyncparticles", value = "internalRenderingMode")
									LocalIntRef irm) {
		// Fabulous graphics
		if (irm.get() == DELAYED_ASYNC) {
			originalRef.get().call(this, frameGraphBuilder, camera, partialTick, gpuBufferSlice, frustum, modelViewMatrix);
		}
	}

	@Inject(method = "renderLevel", order = 1500, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/world/phys/Vec3;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private void onRenderLevelTail0(GraphicsResourceAllocator graphicsResourceAllocator,
									DeltaTracker deltaTracker,
									boolean bl,
									Camera camera,
									Matrix4f modelViewMatrix, // first one
									Matrix4f matrix4f2,
									GpuBufferSlice gpuBufferSlice,
									Vector4f vector4f,
									boolean bl2,
									CallbackInfo ci,
									@Local(ordinal = 0) FrameGraphBuilder frameGraphBuilder,
									@Local(ordinal = 0) float partialTick,
									@Local(ordinal = 0) Frustum frustum,
									@Local(ordinal = 0) PostChain postChain,
									@Share("asyncparticles$addParticlesPassOperation")
									LocalRef<Operation<Void>> originalRef,
									@Share(namespace = "asyncparticles", value = "internalRenderingMode")
									LocalIntRef irm) {
		// non-Fabulous graphics
		if (irm.get() == DELAYED_ASYNC && !Minecraft.useShaderTransparency()) {
			originalRef.get().call(this, frameGraphBuilder, camera, partialTick, gpuBufferSlice, frustum, modelViewMatrix);
		}
	}

	// BEFORE
	@Redirect(method = "lambda$addMainPass$3", remap = false, at = @At(value = "INVOKE", remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void onRenderMain(ParticleEngine instance,
							  Camera camera,
							  float partialTick,
							  MultiBufferSource.BufferSource bufferSource,
							  Frustum frustum,
							  Predicate predicate) {
		switch (InternalRenderingMode.getMode()) {
			case MIXED_ASYNC, MIXED_SYNC, COMPATIBILITY_ASYNC, SYNC ->
				instance.render(camera, partialTick, bufferSource, frustum, t -> !t.translucent());
			case BEFORE_ASYNC, BEFORE_SYNC -> instance.render(camera, partialTick, bufferSource, frustum, t -> true);
		}
	}

	// AFTER
	@Redirect(method = "lambda$addParticlesPass$6", remap = false, at = @At(value = "INVOKE", remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void onRenderParticles(ParticleEngine instance,
								   Camera camera,
								   float partialTick,
								   MultiBufferSource.BufferSource bufferSource,
								   Frustum frustum,
								   Predicate<ParticleRenderType> predicate,
								   @Local(argsOnly = true, ordinal = 0) ResourceHandle resourcehandle1) {
		switch (InternalRenderingMode.getMode()) {
			case MIXED_ASYNC, MIXED_SYNC, COMPATIBILITY_ASYNC, SYNC ->
				instance.render(camera, partialTick, bufferSource, frustum, resourcehandle1 == null ? ParticleRenderType::translucent : t -> true);
			case DELAYED_ASYNC -> instance.render(camera, partialTick, bufferSource, frustum, t -> true);
		}
	}
}
