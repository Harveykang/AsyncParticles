package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticleHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PolytoneAsyncParticleHandler.class)
public interface AccessorPolytoneAsyncParticleHandler {
	@Accessor(remap = false)
	static List<CustomParticleInstance> getPENDING() {
		throw new AssertionError();
	}
}
