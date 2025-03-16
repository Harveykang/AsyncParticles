package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeArrayList;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

// some mod get entities when ticking particles, may cause a CME
@Mixin(ClassInstanceMultiMap.class)
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
		byClass = new ConcurrentHashMap<>();
		allInstances = new IterationSafeArrayList<>();
		byClass.put(baseClass, allInstances);
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private <K, V> V put(Map<K, V> map, K key, V value) {
		// do nothing
		return null;
	}

	@Redirect(method = "method_15217", at = @At(value = "INVOKE", target = "Ljava/util/stream/Collectors;toList()Ljava/util/stream/Collector;"))
	private <T> Collector<T, ?, List<T>> collect() {
		return Collectors.toCollection(IterationSafeArrayList::new);
	}
}
