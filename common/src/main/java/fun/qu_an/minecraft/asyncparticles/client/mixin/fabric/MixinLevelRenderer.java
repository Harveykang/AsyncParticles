package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@WrapOperation(method = "renderLevel",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addParticlesPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/FogParameters;)V"))
	private void redirectAddParticlesPass(LevelRenderer instance,
										  FrameGraphBuilder frameGraphBuilder,
										  Camera camera,
										  float partialTick,
										  FogParameters fogParameters,
										  Operation<Void> original,
										  @Share("asyncparticles$addParticlesPassOperation")
										  LocalRef<Operation<Void>> originalRef,
										  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										  LocalIntRef irm) {
		switch (irm.get()) {
			case COMPATIBILITY_ASYNC, MIXED_ASYNC, SYNC, MIXED_SYNC ->
				original.call(instance, frameGraphBuilder, camera, partialTick, fogParameters);
			case BEFORE_SYNC, BEFORE_ASYNC -> {
				// no-op
			}
			case DELAYED_ASYNC -> originalRef.set(original);
		}
	}

	@Inject(method = "renderLevel", order = 1500,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addWeatherPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/world/phys/Vec3;FLnet/minecraft/client/renderer/FogParameters;)V"))
	private void onRenderLevelTail1(GraphicsResourceAllocator graphicsResourceAllocator,
									DeltaTracker deltaTracker,
									boolean bl,
									Camera camera,
									GameRenderer gameRenderer,
									Matrix4f matrix4f,
									Matrix4f matrix4f2,
									CallbackInfo ci,
									@Local(ordinal = 0) FrameGraphBuilder frameGraphBuilder,
									@Local(ordinal = 0) float partialTick,
									@Local(ordinal = 0) FogParameters fogParameters,
									@Share("asyncparticles$addParticlesPassOperation")
									LocalRef<Operation<Void>> originalRef,
									@Share(namespace = "asyncparticles", value = "internalRenderingMode")
									LocalIntRef irm) {
		// Fabulous graphics
		if (irm.get() == DELAYED_ASYNC) {
			originalRef.get().call(this, frameGraphBuilder, camera, partialTick, fogParameters);
		}
	}
}
