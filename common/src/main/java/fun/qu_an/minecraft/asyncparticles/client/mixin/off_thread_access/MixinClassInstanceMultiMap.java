package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeArrayList;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

	// FIXME: can't remap lambda method_15217 properly, use * instead
	@Dynamic
	@Group(name = "redirect_collector", min = 1)
	@Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;toMutableList()Ljava/util/stream/Collector;"))
	private <T> Collector<T, ?, List<T>> collect1() {
		return Collectors.toCollection(IterationSafeArrayList::new);
	}

	// FIXME: can't remap lambda method_15217 properly, use * instead
	@Dynamic
	@Group(name = "redirect_collector", min = 1)
	@Redirect(method = "*", at = @At(value = "INVOKE", remap = false,
		target = "Ljava/util/stream/Collectors;toList()Ljava/util/stream/Collector;"))
	private <T> Collector<T, ?, List<T>> collect2() {
		return Collectors.toCollection(IterationSafeArrayList::new);
	}

	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.lithium.mixin.collections.entity_filtering.TypeFilterableListMixin",
		name = "createAllOfType"
	)
	@ModifyVariable(method = "@MixinSquared:Handler", name = "list", require = 0,
		at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
	private <T> List<T> modifyCreateAllOfType(List<T> value) {
		return new IterationSafeArrayList<>();
	}
}
