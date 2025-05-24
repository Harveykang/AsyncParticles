package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.cooparticlesapi;

import cn.coostack.cooparticlesapi.network.particle.style.ParticleGroupStyle;
import cn.coostack.cooparticlesapi.network.particle.style.ParticleStyleManager;
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

@Mixin(value = ParticleStyleManager.class, remap = false)
public class MixinParticleStyleManager {
	@Final
	@Shadow
	private static ConcurrentHashMap<UUID, ParticleGroupStyle> clientViewStyles;

	@WrapMethod(method = "doTickClient")
	public void doClientTick(Operation<Void> original) {
		if (ConfigHelper.cooparticlesapi$getTickMode() == CooTickMode.ASYNC_IN_PARALLEL) {
			if (clientViewStyles.isEmpty()) {
				return;
			}
			clientViewStyles.values().parallelStream().forEach(particleGroupStyle -> {
				if (AsyncTicker.isCancelled() && !ConfigHelper.forceDoneParticleTick()) {
					return;
				}
				particleGroupStyle.tick();
				if (!particleGroupStyle.getValid()) {
					clientViewStyles.remove(particleGroupStyle.getUuid());
				}
			});
		} else {
			original.call();
		}
	}
}
