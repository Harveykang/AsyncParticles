package fun.qu_an.minecraft.asyncparticles.client.mixin.watut;

import com.corosus.watut.particle.ParticleItem;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ParticleItem.class)
public class MixinParticleItem {
	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.CUSTOM;
	}
}
