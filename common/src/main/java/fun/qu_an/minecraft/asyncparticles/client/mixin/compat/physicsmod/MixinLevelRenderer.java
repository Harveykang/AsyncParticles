package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.physicsmod;

import net.diebuddies.minecraft.weather.WeatherEffects;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Inject(method = "tickRain", at = @At("RETURN"))
	private void tickRain(CallbackInfo info) {
		WeatherEffects.aliveParticles = 0;
	}
}
