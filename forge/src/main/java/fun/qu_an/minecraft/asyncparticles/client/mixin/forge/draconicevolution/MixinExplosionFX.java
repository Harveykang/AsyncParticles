package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.draconicevolution;

import com.brandon3055.draconicevolution.client.render.effect.ExplosionFX;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ExplosionFX.class)
public class MixinExplosionFX {
	@Unique
	private final SpinLock asyncParticles$lock = new SpinLock();

	@WrapMethod(method = "render")
	public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks, Operation<Void> original) {
		asyncParticles$lock.lock();
		try {
			original.call(buffer, renderInfo, partialTicks);
		} finally {
			asyncParticles$lock.unlock();
		}
	}

	@WrapMethod(method = "tick")
	public void tick(Operation<Void> original) {
		asyncParticles$lock.lock();
		try {
			original.call();
		} finally {
			asyncParticles$lock.unlock();
		}
	}
}
