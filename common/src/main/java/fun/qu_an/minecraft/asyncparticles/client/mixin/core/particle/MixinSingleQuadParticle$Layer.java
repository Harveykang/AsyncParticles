package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.SingleQuadParticleLayerAddition;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SingleQuadParticle.Layer.class)
public class MixinSingleQuadParticle$Layer implements SingleQuadParticleLayerAddition {
	@Unique
	private int offsetBytes;

	@Override
	public int offsetBytes() {
		return offsetBytes;
	}

	@Override
	public void setOffsetBytes(int offsetBytes) {
		this.offsetBytes = offsetBytes;
	}
}
