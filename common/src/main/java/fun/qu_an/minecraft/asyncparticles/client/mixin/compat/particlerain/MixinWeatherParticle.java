package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle {
	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Redirect(method = "<init>", at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/ParticleSpawner;particleCount:I", opcode = Opcodes.GETSTATIC))
	private int modifyParticleCount() {
		return ParticleRainCompat.particleCount.getAndIncrement();
	}

	@Redirect(method = "remove", at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/ParticleSpawner;particleCount:I", opcode = Opcodes.PUTSTATIC))
	private void modifyParticleCount(int value) {
		ParticleRainCompat.particleCount.getAndDecrement();
	}
}
