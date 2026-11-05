package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.compat.particlestorm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Inject(method = "tick", at = @At("TAIL"))
	private void publishTick(CallbackInfo ci) {
		if (ConfigHelper.isParticleStormGpuRendering()) {
			ParticleStormCompat.afterTick(particles, Minecraft.getInstance().gameRenderer.getMainCamera());
		}
	}

	@Dynamic
	@Inject(method = {
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"},
		at = @At(value = "INVOKE", shift = At.Shift.AFTER, target =
		"Lnet/minecraft/client/particle/ParticleRenderType;begin(Lcom/mojang/blaze3d/vertex/Tesselator;Lnet/minecraft/client/renderer/texture/TextureManager;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"))
	private void renderLayer(CallbackInfo ci,
							 @Local(argsOnly = true) Camera camera,
							 @Local(argsOnly = true) float f,
							 @Local(ordinal = 0) ParticleRenderType type) {
		ParticleStormCompat.renderLayer(type, camera, f);
	}

	@Dynamic
	@WrapOperation(method = {
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"},
		at = @At(value = "INVOKE", target =
		"Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private void renderParticle(Particle particle, VertexConsumer buffer, Camera camera, float partialTick,
								Operation<Void> original) {
		if (!ParticleStormCompat.isGpuParticle(particle)) {
			original.call(particle, buffer, camera, partialTick);
		}
	}

	@Inject(method = "clearParticles", at = @At("HEAD"), order = 2000)
	private void releaseRenderer(CallbackInfo ci) {
		ParticleStormCompat.release();
	}
}
