package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particlerain;

import com.leclowndu93150.particlerain.particle.RippleParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = RippleParticle.class)
public abstract class MixinRippleParticle extends MixinWeatherParticle {
	protected MixinRippleParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Override
	public void move(double d, double e, double f) {
		// do nothing
	}
}
