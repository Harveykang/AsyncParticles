package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import pigcart.particlerain.particle.RippleParticle;

@Mixin(value = RippleParticle.class)
public abstract class MixinRippleParticle extends TextureSheetParticle implements ParticleRainAddon {

	protected MixinRippleParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Override
	public void move(double d, double e, double f) {
		// do nothing
	}
}
