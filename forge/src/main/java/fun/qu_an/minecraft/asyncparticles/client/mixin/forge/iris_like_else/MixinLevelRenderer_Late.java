package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.iris_like_else;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"))
	private boolean redirectRenderParticles(ParticleEngine instance,
											PoseStack poseStack,
											MultiBufferSource.BufferSource bufferSource,
											LightTexture lightTexture,
											Camera camera,
											float partialTicks,
											Frustum frustum,
											@Share(namespace = "asyncparticles", value = "isRenderAsync")
											LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			return true;
		}
//		assert !isMixedParticleRendering.get();
		if (ConfigHelper.isCompatibilityRendering()) {
			AsyncRenderer.join(poseStack, partialTicks, camera, lightTexture);
		}
		return false;
	}
}
