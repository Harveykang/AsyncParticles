package fun.qu_an.minecraft.asyncparticles.client.mixin.subtle_effects.fabric;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects.SubtleEffectsCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
	@Shadow
	protected ClientLevel level;

	@WrapWithCondition(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private boolean shouldRenderParticle(Particle instance,
										 VertexConsumer vertexConsumer,
										 Camera camera,
										 float v,
										 @SuppressWarnings("UnresolvedLocalCapture")
										 @Local(name = "enableCull") boolean enableCull,
										 @Local(ordinal = 0) ParticleRenderType renderType) {
		return !enableCull || SubtleEffectsCompat.shouldRenderParticle(renderType, instance, camera, level);
	}
}
