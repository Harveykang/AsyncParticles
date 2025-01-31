package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.Caches;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.*;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Unique
	private static CompletableFuture<Void> taskAll = CompletableFuture.completedFuture(null);

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onRunTick(boolean bl, CallbackInfo ci, @Local(name = "j") int j) {
		if (j != 0) {
			Caches.shouldTickParticles = false;
		} else {
			Caches.cancelled = true;
			taskAll.join();
			Caches.cancelled = false;
			Caches.shouldTickParticles = true;
		}
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onRunTickAfter(boolean bl, CallbackInfo ci, @Local(name = "j") int j) {
		if (j != 0) {
			return;
		}
		List<Runnable> deque = Caches.parallelOperations;
		Runnable[] tasks = deque.toArray(new Runnable[0]);
		deque.clear();
		Runnable animateOperation = Caches.animateOperation;
		Caches.animateOperation = null;
		taskAll = CompletableFuture.runAsync(() ->{
				if (animateOperation != null) {
					animateOperation.run();
				}
			}, Util.BACKGROUND_EXECUTOR)
			.thenCompose(v -> CompletableFuture.allOf(Arrays.stream(tasks)
				.map(runnable -> CompletableFuture.runAsync(runnable, Util.BACKGROUND_EXECUTOR)
					.exceptionally(e -> {
						AsyncparticlesClient.LOGGER.error("Error executing particle operation", e);
						return null;
					}))
				.toArray(CompletableFuture[]::new)));
	}
}
