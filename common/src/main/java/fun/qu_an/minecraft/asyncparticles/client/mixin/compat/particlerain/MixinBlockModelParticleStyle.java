package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import pigcart.particlerain.config.ParticleData;

@Mixin(targets = "pigcart.particlerain.config.ParticleData$ParticleStyle$2", remap = false)
public class MixinBlockModelParticleStyle {
	@WrapMethod(method = "spawn")
	private void wrapSpawn(ClientLevel level, double x, double y, double z, ParticleData data, Operation<Void> original) {
		if (ThreadUtil.isOnParticleThread()) {
			ThreadUtil.enqueueClientTask(() -> original.call(level, x, y, z, data));
		} else {
			original.call(level, x, y, z, data);
		}
	}
}
