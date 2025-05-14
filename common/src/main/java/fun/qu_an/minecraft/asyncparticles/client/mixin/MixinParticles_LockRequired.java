package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.addon.SpinLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(Particle.class) // Will be replaced by the actual targets
public abstract class MixinParticles_LockRequired implements SpinLockProvider {
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

	@WrapMethod(method = "render")
	public void wrapRender(VertexConsumer buffer, Camera renderInfo, float partialTick, Operation<Void> original) {
		SpinLock lock = asyncparticles$getSpinLock();
		lock.lock();
		try {
			original.call(buffer, renderInfo, partialTick);
		} finally {
			lock.unlock();
		}
	}
}
