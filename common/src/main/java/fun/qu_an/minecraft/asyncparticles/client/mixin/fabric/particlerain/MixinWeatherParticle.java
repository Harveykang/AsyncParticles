package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.CountManagements;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle {
	@Shadow protected BlockPos.MutableBlockPos pos;

	protected MixinWeatherParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		CountManagements.asyncParticles$particleCount.getAndIncrement();
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lpigcart/particlerain/ParticleRainClient;particleCount:I"))
	private void onRemove(CallbackInfo ci) {
		CountManagements.asyncParticles$particleCount.getAndDecrement();
	}
}
