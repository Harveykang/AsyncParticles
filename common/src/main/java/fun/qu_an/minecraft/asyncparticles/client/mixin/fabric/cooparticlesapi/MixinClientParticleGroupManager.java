package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.cooparticlesapi;

import cn.coostack.cooparticlesapi.particles.control.group.ClientParticleGroupManager;
import cn.coostack.cooparticlesapi.particles.control.group.ControlableParticleGroup;
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

@Mixin(value = ClientParticleGroupManager.class, remap = false)
public class MixinClientParticleGroupManager {
	@Final
	@Shadow
	private static ConcurrentHashMap<UUID, ControlableParticleGroup> visibleControls;

	@WrapMethod(method = "doClientTick")
	public void doClientTick(Operation<Void> original) {
		if (ConfigHelper.cooparticlesapi$getTickMode() == CooTickMode.ASYNC_IN_PARALLEL) {
			if (visibleControls.isEmpty()) {
				return;
			}
			visibleControls.values().parallelStream().forEach(controlableParticleGroup -> {
				if (AsyncTicker.isCancelled() && !ConfigHelper.forceDoneParticleTick()) {
					return;
				}
				controlableParticleGroup.tick$coo_particles_api();
			});
		} else {
			original.call();
		}
	}
}
