package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Queue;

@ApiStatus.Internal
public interface ParticleEngineAddon {
	void asyncparticle$addRenderType(ParticleRenderType particleRenderType);

	void asyncparticle$sortRenderOrder();
}
