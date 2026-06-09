package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.core.particle.render;

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
	private static List<ParticleRenderType> RENDER_ORDER;

	@Override
	public void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		if (RENDER_ORDER.contains(particleRenderType)) {
			return;
		}
		List<ParticleRenderType> renderOrder = RENDER_ORDER;
		if (!(renderOrder instanceof ArrayList<ParticleRenderType>)) {
			renderOrder = RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
		}
		if (particleRenderType == GpuParticleBehavior.INSTANCE.ofRenderType(ParticleRenderType.SINGLE_QUADS)) {
			renderOrder.add(renderOrder.indexOf(ParticleRenderType.SINGLE_QUADS), particleRenderType);
		} else {
			renderOrder.add(particleRenderType);
		}
	}
}
