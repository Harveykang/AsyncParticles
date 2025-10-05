package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.render;

import fun.qu_an.minecraft.asyncparticles.client.addon.SingleQuadParticleLayerAddition;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.render.ParticleLayerAttached;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SingleQuadParticle.Layer.class)
public class MixinSingleQuadParticle$Layer implements SingleQuadParticleLayerAddition {
	@Unique
	private final ParticleLayerAttached asyncparticles$attached = new ParticleLayerAttached((SingleQuadParticle.Layer) (Object) this);

	@Unique
	@Override
	public ParticleLayerAttached asyncparticles$getAttached() {
		return asyncparticles$attached;
	}
}
