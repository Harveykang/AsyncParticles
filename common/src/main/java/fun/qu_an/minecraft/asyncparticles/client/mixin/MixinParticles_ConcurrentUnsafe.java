package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.fluids.particle.BasinFluidParticle;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin({Particle.class, BasinFluidParticle.class})
public class MixinParticles_ConcurrentUnsafe {
	@Unique
	protected final SpinLock asyncparticles$lock = new SpinLock();

	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {
		asyncparticles$lock.lock();
		try {
			original.call();
		} finally {
			asyncparticles$lock.unlock();
		}
	}

	@WrapMethod(method = "render")
	public void wrapRender(VertexConsumer buffer, Camera renderInfo, float partialTicks, Operation<Void> original) {
		asyncparticles$lock.lock();
		try {
			original.call(buffer, renderInfo, partialTicks);
		} finally {
			asyncparticles$lock.unlock();
		}
	}
}
