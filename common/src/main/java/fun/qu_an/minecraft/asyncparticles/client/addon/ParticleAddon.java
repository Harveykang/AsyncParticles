package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ParticleAddon {
	void asyncparticles$setTicked();

	void asyncparticles$resetTicked();

	boolean asyncparticles$isTicked();

	void asyncparticles$setGpuLightGot();

	boolean asyncparticles$isFirstGpuLightGet();

	Class<? extends Particle> asyncparticles$getRealClass();

	byte getTickFlag();
}
