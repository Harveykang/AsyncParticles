package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.draconicevolution;

import com.brandon3055.draconicevolution.client.render.effect.CrystalFXWireless;
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
import java.util.List;

@Mixin(CrystalFXWireless.class)
public class MixinCrystalFXWireless {
	@Unique
	private final SpinLock asyncParticles$lock = new SpinLock();

	@WrapMethod(method = "render")
	public void wrapRender(VertexConsumer buffer, Camera renderInfo, float partialTicks, Operation<Void> original) {
		asyncParticles$lock.lock();
		try {
			original.call(buffer, renderInfo, partialTicks);
		} finally {
			asyncParticles$lock.unlock();
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	public boolean wrapAdd(List<Object> instance, Object e) {
		asyncParticles$lock.lock();
		try {
			return instance.add(e);
		} finally {
			asyncParticles$lock.unlock();
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
	public void wrapRemove(Iterator<?> instance) {
		asyncParticles$lock.lock();
		try {
			instance.remove();
		} finally {
			asyncParticles$lock.unlock();
		}
	}
}
