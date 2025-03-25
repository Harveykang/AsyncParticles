package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeArrayList;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// some mod get entities when ticking particles, may cause a CME
@Mixin(value = ClassInstanceMultiMap.class, priority = 1100) // higher priority to run after VMP's mixin
public class MixinClassInstanceMultiMap {
	@Final
	@Mutable
	@Shadow
	private Map<Class<?>, List<?>> byClass;

	@Mutable
	@Final
	@Shadow
	private List<?> allInstances;

	// FIXME: 这样不行啊，到处漏风
	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void newHashMap(Class<?> baseClass, CallbackInfo ci) {
		byClass = new ConcurrentHashMap<>(byClass);
		allInstances = new IterationSafeArrayList<>(allInstances);
		byClass.put(baseClass, allInstances);
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private <K, V> V put(Map<K, V> map, K key, V value) {
		// do nothing
		return null;
	}

	// FIXME: 这行吗？
	@Dynamic
	@Redirect(method = "*", require = 0, at = @At(value = "NEW", remap = false, target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;<init>()V"))
	private <T> List<T> newArrayList() {
		return new IterationSafeArrayList<>();
	}
}
