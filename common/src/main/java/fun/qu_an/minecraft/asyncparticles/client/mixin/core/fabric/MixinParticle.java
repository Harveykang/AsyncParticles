package fun.qu_an.minecraft.asyncparticles.client.mixin.core.fabric;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon {
	// Forge has implemented this method.
	// So we need only to implement for fabric.
	@Override
	public boolean shouldCull() {
		return true;
	}
}
