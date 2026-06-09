package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(value = SoundEngine.class, priority = 500)
public abstract class MixinSoundEngine {
	@WrapOperation(method = "isActive", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
	public Object redirectIsActive(Map<Object, Object> instance, Object o, Operation<Object> original) {
		Object o1 = original.call(instance, o);
		return o1 == null ? Integer.MAX_VALUE : o1;
	}
}
