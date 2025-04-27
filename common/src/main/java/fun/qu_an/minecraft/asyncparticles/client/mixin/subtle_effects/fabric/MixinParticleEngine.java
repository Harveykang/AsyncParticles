package fun.qu_an.minecraft.asyncparticles.client.mixin.subtle_effects.fabric;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects.SubtleEffectsCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
	@WrapWithCondition(method = "renderParticleType",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private static boolean shouldRenderParticle(Particle instance,
												VertexConsumer vertexConsumer,
												Camera camera,
												float v) {
		return SubtleEffectsCompat.shouldRenderParticle(instance, camera);
	}
}
