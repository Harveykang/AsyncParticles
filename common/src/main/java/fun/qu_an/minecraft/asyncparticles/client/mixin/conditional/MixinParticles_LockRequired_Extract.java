package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.addon.SpinLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(SingleQuadParticle.class) // Will be replaced with the actual targets
public abstract class MixinParticles_LockRequired_Extract implements SpinLockProvider {
	@WrapMethod(method = "render")
	public void wrapTick(VertexConsumer vertexConsumer, Camera camera, float f, Operation<Void> original) {
		SpinLock lock = asyncparticles$getSpinLock();
		lock.lock();
		try {
			original.call(vertexConsumer, camera, f);
		} finally {
			lock.unlock();
		}
	}
}
