package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleVertexStorageAddition;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(QuadParticleRenderState.Storage.class)
public class MixinVertexVertexStorage implements ParticleVertexStorageAddition {
	@Shadow
	private float[] floatValues;

	@Shadow
	private int[] intValues;

	@Override
	public ParticleSlice asyncparticles$slice(int start, int end) {
		return consumer -> {
			for (int i = start; i < end; i++) {
				int j = i * 12;
				int k = i * 2;
				consumer.consume(
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j++],
					this.floatValues[j],
					this.intValues[k++],
					this.intValues[k]
				);
			}
		};
	}
}
