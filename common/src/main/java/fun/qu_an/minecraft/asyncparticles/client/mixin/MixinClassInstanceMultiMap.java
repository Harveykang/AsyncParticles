package fun.qu_an.minecraft.asyncparticles.client.mixin;

import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// some mod get entities when ticking particles, may cause a CME
@Mixin(ClassInstanceMultiMap.class)
public class MixinClassInstanceMultiMap {
	@Mutable
	@Shadow @Final private Map<Class<?>, List<?>> byClass;

	// FIXME: 这样不行啊，到处漏风
	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private <K, V> void newHashMap(Class<?> baseClass, CallbackInfo ci) {
		byClass = new ConcurrentHashMap<>(byClass);
	}
}
