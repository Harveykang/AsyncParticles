package fun.qu_an.minecraft.asyncedparticles.client.mixin;

import fun.qu_an.minecraft.asyncedparticles.client.AsyncedparticlesClient;
import fun.qu_an.minecraft.asyncedparticles.client.Caches;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Unique
	private static final List<Future<?>> tasks = new ArrayList<>();

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		Caches.cancelled = true;
		tasks.forEach(task -> {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException ignored) {
			}
		});
		tasks.clear();
		Caches.particleOperations.clear();
		Caches.texturesOperation = null;
		Caches.cancelled = false;
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTickReturn(CallbackInfo ci) {
		ForkJoinPool executor = AsyncedparticlesClient.EXECUTOR;

		Caches.particleOperations.forEach(operation -> {
			tasks.add(executor.submit(() -> {
				try {
					operation.run();
				} catch (Throwable ignored) {
				}
			}));
		});

		tasks.add(executor.submit(() -> {
			try {
				Caches.texturesOperation.call();
			} catch (Throwable ignored) {
			}
		}));

	}
}
