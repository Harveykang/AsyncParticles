package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.ModListHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.SnowParticle;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(SnowParticle.class)
public abstract class MixinSnowParticle extends WeatherParticle {
	protected MixinSnowParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo ci) {
		if (!level.getFluidState(ModListHelper.VS_LOADED ? pos : pos.below()).isEmpty()) {
			alpha = 0.0F;
		}
	}
}
