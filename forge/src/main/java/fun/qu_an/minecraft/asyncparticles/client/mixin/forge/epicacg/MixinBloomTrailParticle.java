package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.epicacg;

import com.dfdyz.epicacg.client.particle.BloomTrailParticle;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BloomTrailParticle.class)
public class MixinBloomTrailParticle {
	@Unique
	private final SpinLock asyncParticles$lock = new SpinLock();

	@WrapMethod(method = "render")
	private void wrapRender(VertexConsumer vertexConsumer, Camera camera, float partialTick, Operation<Void> original) {
		asyncParticles$lock.lock();
		try {
			original.call(vertexConsumer, camera, partialTick);
		} finally {
			asyncParticles$lock.unlock();
		}
	}

	@WrapMethod(method = "tick")
	private void wrapTick(Operation<Void> original) {
		asyncParticles$lock.lock();
		try {
			original.call();
		} finally {
			asyncParticles$lock.unlock();
		}
	}
}
