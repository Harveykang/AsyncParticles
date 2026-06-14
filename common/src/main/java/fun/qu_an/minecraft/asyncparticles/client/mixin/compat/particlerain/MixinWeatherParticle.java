package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends SingleQuadParticle {
	protected MixinWeatherParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
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
