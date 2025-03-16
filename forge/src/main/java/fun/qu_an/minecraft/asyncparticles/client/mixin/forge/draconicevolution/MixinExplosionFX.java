package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.draconicevolution;

import com.brandon3055.draconicevolution.client.render.effect.ExplosionFX;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.LinkedList;

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

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedList;addFirst(Ljava/lang/Object;)V"))
	public void addFirst(LinkedList<Object> list, Object fx) {
		asyncParticles$lock.lock();
		try {
			list.addFirst(fx);
		} finally {
			asyncParticles$lock.unlock();
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
	public void tick(Iterator<?> instance) {
		asyncParticles$lock.lock();
		try {
			instance.remove();
		} finally {
			asyncParticles$lock.unlock();
		}
	}
}
