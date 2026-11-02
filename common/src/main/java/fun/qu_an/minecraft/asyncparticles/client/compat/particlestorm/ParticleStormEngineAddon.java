package fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm;

import net.minecraft.client.particle.ParticleRenderType;
import org.mesdag.particlestorm.api.IMolangParticleInstance;

import java.util.Map;
import java.util.Queue;

public interface ParticleStormEngineAddon {
	Map<ParticleRenderType, Queue<IMolangParticleInstance>> asyncparticles$getGroupedParticles();

	Queue<IMolangParticleInstance> asyncparticles$getParticlesToAdd();
}
