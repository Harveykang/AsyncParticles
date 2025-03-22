package fun.qu_an.minecraft.asyncparticles.client.mixin.subtle_effects;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects.SubtleEffectsCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AsyncRenderer.class)
public class MixinAsyncRenderer {
	@WrapWithCondition(method = "renderParticle",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private static boolean shouldRenderParticle(Particle instance,
												VertexConsumer vertexConsumer,
												Camera camera,
												float v) {
		return SubtleEffectsCompat.shouldRenderParticle(instance, camera, Minecraft.getInstance().level);
	}
}
