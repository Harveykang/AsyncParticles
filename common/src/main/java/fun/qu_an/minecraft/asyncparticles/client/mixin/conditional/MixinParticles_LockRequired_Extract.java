package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.ISpinLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(SingleQuadParticle.class) // Will be replaced with the actual targets
public abstract class MixinParticles_LockRequired_Extract implements ISpinLockProvider {
	@WrapMethod(method = "extract")
	public void wrapTick(QuadParticleRenderState quadParticleRenderState, Camera camera, float f, Operation<Void> original) {
		SpinLock lock = asyncparticles$getSpinLock();
		lock.lock();
		try {
			original.call(quadParticleRenderState, camera, f);
		} finally {
			lock.unlock();
		}
	}
}
