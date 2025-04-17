package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@WrapOperation(method = "renderLevel",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addParticlesPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/FogParameters;)V"))
	private void redirectAddParticlesPass(LevelRenderer instance,
										  FrameGraphBuilder frameGraphBuilder,
										  Camera camera,
										  float partialTick,
										  FogParameters fog,
										  Operation<Void> original,
										  @Share("asyncparticles$addParticlesPassOperation")
										  LocalRef<Operation<Void>> originalRef) {
//		this.asyncparticles$addParticlesPassOperation = original;
		// we'll call the original method later
		if (!SimplePropertiesConfig.isRenderAsync()) {
			original.call(instance, frameGraphBuilder, camera, partialTick, fog);
		} else {
			originalRef.set(original);
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
								   @Share("asyncparticles$addParticlesPassOperation")
								   LocalRef<Operation<Void>> originalRef) {
		// as late as possible
//		this.asyncparticles$addParticlesPassOperation.call(frameGraphBuilder, camera, f, fogParameters);
		if (SimplePropertiesConfig.isRenderAsync()) {
			originalRef.get().call(this, frameGraphBuilder, camera, f, fogParameters);
		}
	}

	@Inject(method = "method_62213", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
	private void onRenderParticles(CallbackInfo ci) {
		// reset blend func and culling state
		// other mods may change them...
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
	}
}
