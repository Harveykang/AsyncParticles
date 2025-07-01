package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@WrapOperation(method = "renderLevel",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addParticlesPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;FLcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private void redirectAddParticlesPass(LevelRenderer instance,
										  FrameGraphBuilder frameGraphBuilder,
										  Camera camera,
										  float partialTick,
										  GpuBufferSlice gpuBufferSlice,
										  Operation<Void> original,
										  @Share("asyncparticles$addParticlesPassOperation")
										  LocalRef<Operation<Void>> originalRef,
										  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										  LocalIntRef irm) {
		switch (irm.get()) {
			case COMPATIBILITY_ASYNC, MIXED_ASYNC, SYNC, MIXED_SYNC ->
				original.call(instance, frameGraphBuilder, camera, partialTick, gpuBufferSlice);
			case BEFORE_SYNC, BEFORE_ASYNC -> {
				// no-op
			}
			case DELAYED_ASYNC -> originalRef.set(original);
		}
	}

	@Inject(method = "renderLevel", order = 1500,
		at =@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addWeatherPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/world/phys/Vec3;FLcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private void onRenderLevelTail1(GraphicsResourceAllocator graphicsResourceAllocator,
									DeltaTracker deltaTracker,
									boolean bl,
									Camera camera,
									Matrix4f matrix4f,
									Matrix4f matrix4f2,
									GpuBufferSlice gpuBufferSlice,
									Vector4f vector4f,
									boolean bl2,
									CallbackInfo ci,
									@Local(ordinal = 0) FrameGraphBuilder frameGraphBuilder,
									@Local(ordinal = 0) float partialTick,
									@Share("asyncparticles$addParticlesPassOperation")
									LocalRef<Operation<Void>> originalRef,
									@Share(namespace = "asyncparticles", value = "internalRenderingMode")
									LocalIntRef irm) {
		// Fabulous graphics
		if (irm.get() == DELAYED_ASYNC) {
			originalRef.get().call(this, frameGraphBuilder, camera, partialTick, gpuBufferSlice);
		}
	}
}
