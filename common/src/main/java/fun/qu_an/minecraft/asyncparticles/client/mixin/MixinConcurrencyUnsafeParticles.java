package fun.qu_an.minecraft.asyncparticles.client.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin({
	Particle.class,
	// TODO: configurability
})
public class MixinConcurrencyUnsafeParticles {
//	@Unique
//	private final SpinLock asyncParticles$lock = new SpinLock();
//
//	@WrapMethod(method = "render")
//	public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks, Operation<Void> original) {
//		asyncParticles$lock.lock();
//		try {
//			original.call(buffer, renderInfo, partialTicks);
//		} finally {
//			asyncParticles$lock.unlock();
//		}
//	}
//
//	@WrapMethod(method = "tick")
//	public void tick(Operation<Void> original) {
//		asyncParticles$lock.lock();
//		try {
//			original.call();
//		} finally {
//			asyncParticles$lock.unlock();
//		}
//	}
}
