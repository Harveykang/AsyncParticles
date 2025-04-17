package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeArrayList;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

// some mod get entities when ticking particles, may cause a CME
@Mixin(value = ClassInstanceMultiMap.class, priority = 1100)
public class MixinClassInstanceMultiMap {
	@Group(name = "redirect_collector", min = 1)
	@Redirect(method = "method_15217", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;toMutableList()Ljava/util/stream/Collector;"))
	private <T> Collector<T, ?, List<T>> collect1() {
		return Collectors.toCollection(IterationSafeArrayList::new);
	}

	@Dynamic
	@Group(name = "redirect_collector", min = 1)
	@Redirect(method = "method_15217", at = @At(value = "INVOKE", remap = false,
		target = "Ljava/util/stream/Collectors;toList()Ljava/util/stream/Collector;"))
	private <T> Collector<T, ?, List<T>> collect2() {
		return Collectors.toCollection(IterationSafeArrayList::new);
	}
}
