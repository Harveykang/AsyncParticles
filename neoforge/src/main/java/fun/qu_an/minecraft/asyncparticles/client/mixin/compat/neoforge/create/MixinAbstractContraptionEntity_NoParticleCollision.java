package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.ContraptionEntityAddon;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @see fun.qu_an.minecraft.asyncparticles.client.coremod.neoforge.AdjusterContraptionNoParticleCollision
 */
@Mixin(AbstractContraptionEntity.class)
public class MixinAbstractContraptionEntity_NoParticleCollision implements ContraptionEntityAddon {
	@Override
	public boolean asyncparticles$isParticleCollision() {
		return false;
	}
}
