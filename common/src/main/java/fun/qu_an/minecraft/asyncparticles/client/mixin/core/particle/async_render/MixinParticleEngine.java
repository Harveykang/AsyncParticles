
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_render;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Mutable
	@Shadow
	@Final
	private static List<ParticleRenderType> RENDER_ORDER;

	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Override
	public void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		if (!ModListHelper.IS_FORGE && !RENDER_ORDER.contains(particleRenderType)) {
			List<ParticleRenderType> renderOrder = RENDER_ORDER;
			if (!(renderOrder instanceof ArrayList<ParticleRenderType>)) {
				renderOrder = RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
			}
			if (particleRenderType == GpuParticleBehavior.INSTANCE.getRenderType(ParticleRenderType.SINGLE_QUADS)) {
				renderOrder.add(renderOrder.indexOf(ParticleRenderType.SINGLE_QUADS), particleRenderType);
			} else {
				renderOrder.add(particleRenderType);
			}
		}
	}
}
