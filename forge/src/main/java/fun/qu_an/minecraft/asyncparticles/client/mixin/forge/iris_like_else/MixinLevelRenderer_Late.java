package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.iris_like_else;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.COMPATIBILITY_ASYNC;
import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.SYNC;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"))
	private void redirectRenderParticles(ParticleEngine instance,
										 PoseStack poseStack,
										 MultiBufferSource.BufferSource bufferSource,
										 LightTexture lightTexture,
										 Camera camera,
										 float partialTick,
										 Frustum frustum,
										 @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										 LocalIntRef irm) {
		switch (irm.get()) {
			case SYNC -> AsyncRenderer.endAll(poseStack, partialTick, camera, lightTexture, false);
			case COMPATIBILITY_ASYNC -> AsyncRenderer.endAll(poseStack, partialTick, camera, lightTexture, true);
		}
	}
}
