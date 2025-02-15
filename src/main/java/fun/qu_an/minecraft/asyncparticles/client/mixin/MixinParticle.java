package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.ParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.*;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon {
	@Shadow
	public abstract void remove();

	@Shadow
	public abstract boolean isAlive();

	@Unique
	private boolean asyncParticles$ticked;

	@Override
	public boolean asyncParticles$shouldRemove() {
		if (!isAlive()) return true;
		if (asyncParticles$ticked) return asyncParticles$ticked = false;
		remove();
		return true;
	}

	@Override
	public void asyncParticles$setTicked() {
		this.asyncParticles$ticked = true;
	}

	@Override
	public boolean asyncParticles$isTicked() {
		return this.asyncParticles$ticked;
	}
}
