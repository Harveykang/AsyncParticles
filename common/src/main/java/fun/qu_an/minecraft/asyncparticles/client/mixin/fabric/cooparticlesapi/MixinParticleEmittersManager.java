package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.cooparticlesapi;

import cn.coostack.cooparticlesapi.network.particle.emitters.ParticleEmitters;
import cn.coostack.cooparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.cooparticlesapi.CooTickMode;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = ParticleEmittersManager.class, remap = false)
public class MixinParticleEmittersManager {
	@Final
	@Shadow
	private static ConcurrentHashMap<UUID, ParticleEmitters> clientEmitters;

	@WrapMethod(method = "doTickClient")
	public void doClientTick(Operation<Void> original) {
		if (ConfigHelper.cooparticlesapi$getTickMode() == CooTickMode.ASYNC_IN_PARALLEL) {
			if (clientEmitters.isEmpty()) {
				return;
			}
			clientEmitters.values().parallelStream().forEach(particleEmitters -> {
				if (AsyncTicker.isCancelled() && !ConfigHelper.forceDoneParticleTick()) {
					return;
				}
				particleEmitters.tick();
				if (particleEmitters.getCancelled()) {
					clientEmitters.remove(particleEmitters.getUuid());
				}
			});
		} else {
			original.call();
		}
	}
}
