package fun.qu_an.minecraft.asyncparticles.client.mixin.dsurround;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.ISpinLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.particle.TextureSheetParticle;
import org.orecruncher.dsurround.effects.particles.ParticleRenderCollection;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ParticleRenderCollection.class)
public class MixinParticleRenderCollection {
	@WrapMethod(method = "add")
	public void add(TextureSheetParticle particle, Operation<Void> original) {
		if (this instanceof ISpinLockProvider lockProvider) {
			lockProvider.asyncparticles$getSpinLock().wrap(() -> original.call(particle));
		} else if (ThreadUtil.isOnMainThread()) {
			original.call(particle);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(particle));
		}
	}
}
