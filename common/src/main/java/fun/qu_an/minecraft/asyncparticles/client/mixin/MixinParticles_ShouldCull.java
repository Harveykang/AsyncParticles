package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Particle.class)
public abstract class MixinParticles_ShouldCull implements ParticleAddon {
	@Override
	public boolean shouldCull() {
		return false;
	}
}
