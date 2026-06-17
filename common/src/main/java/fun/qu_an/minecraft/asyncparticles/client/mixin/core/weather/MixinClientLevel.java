package fun.qu_an.minecraft.asyncparticles.client.mixin.core.weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
	@WrapMethod(method = "tickWeatherEffects")
	private void wrapTickRain(Operation<Void> original) {
		if (ConfigHelper.isTickWeatherAsync()) {
			AsyncTickBehavior.getInstance().dispatch(original::call);
		} else {
			original.call();
		}
	}
}
