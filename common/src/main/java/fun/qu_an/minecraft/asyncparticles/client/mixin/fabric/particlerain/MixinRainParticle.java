package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.ModListHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.RainParticle;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(RainParticle.class)
public abstract class MixinRainParticle extends WeatherParticle {
	protected MixinRainParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lpigcart/particlerain/particle/WeatherParticle;tick()V"))
	private void onTick(CallbackInfo ci) {
		if (!level.getFluidState(ModListHelper.VS_LOADED ? pos : pos.below(2)).isEmpty()) {
			alpha = 0.0F;
		}
	}
}
