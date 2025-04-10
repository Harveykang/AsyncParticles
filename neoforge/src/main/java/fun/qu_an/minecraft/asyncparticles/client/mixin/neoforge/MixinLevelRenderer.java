package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
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
