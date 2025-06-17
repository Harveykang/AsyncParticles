package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(Particle.class) // Will be replaced with the actual targets
public abstract class MixinParticles_NoCulling implements ParticleAddon {
	@Override
	public boolean shouldCull() {
		return false;
	}
}
