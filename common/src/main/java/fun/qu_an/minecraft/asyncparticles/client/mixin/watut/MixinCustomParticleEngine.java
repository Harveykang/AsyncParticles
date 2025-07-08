package fun.qu_an.minecraft.asyncparticles.client.mixin.watut;

import com.corosus.watut.client.CustomParticleEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(value = CustomParticleEngine.class, remap = false)
public class MixinCustomParticleEngine {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
	private boolean onAdd(Queue<Particle> instance, Object p, Operation<Boolean> original) {
		((LightCachedParticleAddon) p).asyncparticles$refresh();
		switch (ConfigHelper.getParticleCullingMode()) {
			case ASYNC_AABB -> ((ParticleAddon) p).asyncparticles$tickAABBCulling();
			case ASYNC_SPHERE -> ((ParticleAddon) p).asyncparticles$tickSphereCulling();
		}
		return original.call(instance, p);
	}

	@Inject(method = "tickParticle", at = @At(value = "HEAD"))
	private void onTickParticle(Particle p, CallbackInfo ci) {
		((LightCachedParticleAddon) p).asyncparticles$refresh();
		switch (ConfigHelper.getParticleCullingMode()) {
			case ASYNC_AABB -> ((ParticleAddon) p).asyncparticles$tickAABBCulling();
			case ASYNC_SPHERE -> ((ParticleAddon) p).asyncparticles$tickSphereCulling();
		}
	}
}
