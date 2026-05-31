package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@Shadow
	@Final
	private LevelRenderState levelRenderState;
	@Unique
	private Operation<Void> asyncparticles$extractParticles;

	@Inject(method = "extractLevel", at = @At(value = "HEAD"))
	public void extractHead(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
		Frustum frustum = new Frustum(camera.getCullFrustum()).offset(-3.0F);
		AsyncRenderBehavior.setFrustum(frustum);
		if (asyncparticles$extractParticles != null) {
			asyncparticles$extractParticles.call(Minecraft.getInstance().particleEngine,
				this.levelRenderState.particlesRenderState, frustum, camera, deltaPartialTick);
		}
	}

	@WrapOperation(method = "extractLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;extract(Lnet/minecraft/client/renderer/state/level/ParticlesRenderState;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;F)V"))
	public void extractParticles(ParticleEngine instance,
								 ParticlesRenderState particlesRenderState,
								 Frustum frustum,
								 Camera camera,
								 float partialTickTime,
								 Operation<Void> original) {
		if (asyncparticles$extractParticles == null) {
			asyncparticles$extractParticles = original;
			original.call(instance, particlesRenderState, frustum, camera, partialTickTime);
		}
	}
}
