package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import fun.qu_an.minecraft.asyncparticles.client.api.ISpinLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.util.ReentrantSpinLock;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(Particle.class) // Will be replaced with the actual targets
public class MixinParticles_LockProvider implements ISpinLockProvider {
	@Unique
	protected SpinLock asyncparticles$lock = new ReentrantSpinLock();

	@Override
	public SpinLock asyncparticles$getSpinLock() {
		return asyncparticles$lock;
	}
}
