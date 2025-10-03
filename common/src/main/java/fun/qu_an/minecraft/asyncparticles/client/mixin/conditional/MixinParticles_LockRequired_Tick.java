package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.ISpinLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(Particle.class) // Will be replaced with the actual targets
public abstract class MixinParticles_LockRequired_Tick implements ISpinLockProvider {
	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {
		SpinLock lock = asyncparticles$getSpinLock();
		lock.lock();
		try {
			original.call();
		} finally {
			lock.unlock();
		}
	}
}
