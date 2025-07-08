package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.subtle_effects;

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

	@WrapWithCondition(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private boolean shouldRenderParticle(Particle instance,
										 VertexConsumer vertexConsumer,
										 Camera camera,
										 float v,
										 @Local(ordinal = 0) ParticleRenderType renderType) {
		// if shouldSync is false, the particles are already culled.
		return SubtleEffectsCompat.shouldRenderParticle(renderType, instance, camera, level);
	}
}
