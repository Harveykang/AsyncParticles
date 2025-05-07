package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.addon.SpinLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.util.ReentrantSpinLock;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(Particle.class) // Will be replaced by the actual targets
public class MixinParticles_LockProvider implements SpinLockProvider {
	@Unique
	protected SpinLock asyncparticles$lock;

	@Override
	public SpinLock asyncparticles$getSpinLock() {
		// Lazy initialization
		SpinLock lock = this.asyncparticles$lock;
		if (lock != null) {
			return lock;
		}
		synchronized (this) {
			SpinLock lock1 = this.asyncparticles$lock;
			if (lock1 != null) {
				return lock1;
			}
			return this.asyncparticles$lock = new ReentrantSpinLock();
		}
	}
}
