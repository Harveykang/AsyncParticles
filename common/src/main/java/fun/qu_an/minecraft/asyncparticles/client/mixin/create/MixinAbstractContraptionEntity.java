package fun.qu_an.minecraft.asyncparticles.client.mixin.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionEntityAddon;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContraptionEntity.class)
public class MixinAbstractContraptionEntity implements ContraptionEntityAddon {
	@Override
	public boolean asyncparticles$doParticleCollision() {
		return true;
	}
}
