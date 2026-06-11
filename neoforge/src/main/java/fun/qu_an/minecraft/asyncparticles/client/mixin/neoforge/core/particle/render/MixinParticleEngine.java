package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.core.particle.render;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements ParticleEngineAddon {
	@Mutable
	@Shadow
	@Final
	private List<ParticleRenderType> particleRenderOrder;

	@Override
	public void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		if (particleRenderOrder.contains(particleRenderType)) {
			return;
		}
		List<ParticleRenderType> renderOrder = particleRenderOrder;
		if (!(renderOrder instanceof ArrayList<ParticleRenderType>)) {
			renderOrder = particleRenderOrder = new ArrayList<>(particleRenderOrder);
		}
		if (particleRenderType == GpuParticleBehavior.getInstance().ofRenderType(ParticleRenderType.SINGLE_QUADS)) {
			renderOrder.add(renderOrder.indexOf(ParticleRenderType.SINGLE_QUADS), particleRenderType);
		} else {
			renderOrder.add(particleRenderType);
		}
	}
}
