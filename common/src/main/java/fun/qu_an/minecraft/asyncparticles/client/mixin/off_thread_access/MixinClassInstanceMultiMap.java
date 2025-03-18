package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.addon.IsClientAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeArrayList;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

// some mod get entities when ticking particles, may cause a CME
@Mixin(ClassInstanceMultiMap.class)
public class MixinClassInstanceMultiMap implements IsClientAddon {
	@Final
	@Mutable
	@Shadow
	private Map<Class<?>, List<?>> byClass;

	@Mutable
	@Final
	@Shadow
	private List<?> allInstances;

	@Shadow
	@Final
	private Class<?> baseClass;
	@Unique
	private boolean asyncparticles$isClientSide;

	@Override
	public boolean asyncparticles$isClientSide() {
		return asyncparticles$isClientSide;
	}

	@Override
	public void asyncparticles$setClientSide() {
		if (!asyncparticles$isClientSide) {
			asyncparticles$isClientSide = true;
			byClass = new ConcurrentHashMap<>(byClass);
			allInstances = new IterationSafeArrayList<>(allInstances);
			byClass.put(baseClass, allInstances);
		}
	}

	@Redirect(method = "method_15217", at = @At(value = "INVOKE", target = "Ljava/util/stream/Collectors;toList()Ljava/util/stream/Collector;"))
	private <T> Collector<T, ?, List<T>> collect() {
		return Collectors.toCollection(IterationSafeArrayList::new);
	}
}
