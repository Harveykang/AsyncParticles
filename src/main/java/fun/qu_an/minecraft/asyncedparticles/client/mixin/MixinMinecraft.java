package fun.qu_an.minecraft.asyncedparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncedparticles.client.AsyncedparticlesClient;
import fun.qu_an.minecraft.asyncedparticles.client.Caches;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.*;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		Future<?> textureTickAsync = Caches.textureTickAsync;
		if (textureTickAsync!= null) {
			Caches.texturesOperationCancelled = true;
			try {
				textureTickAsync.get();
			} catch (InterruptedException | ExecutionException ignored) {
			}
			Caches.texturesOperationCancelled = false;
			Caches.textureTickAsync = null;
		}
		Future<?> particleTickAsync = Caches.particleTickAsync;
		if (particleTickAsync!= null) {
			Caches.particlesOperationCancelled = true;
			try {
				particleTickAsync.get();
			} catch (InterruptedException | ExecutionException ignored) {
			}
			Caches.particlesOperationCancelled = false;
			Caches.particleTickAsync = null;
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTickReturn(CallbackInfo ci) {
		ExecutorService executor = (ExecutorService) Util.backgroundExecutor();
		Caches.textureTickAsync = executor.submit(() -> {
			Operation<Void> texturesOperation = Caches.texturesOperation;
			if (texturesOperation != null) {
				try {
					texturesOperation.call();
				} catch (Throwable e) {
					AsyncedparticlesClient.LOGGER.warn("Error while executing textures operation", e);
				}
				Caches.texturesOperation = null;
			}
		});
		Caches.particleTickAsync = executor.submit(() -> {
			Operation<Void> particlesOperation = Caches.particlesOperation;
			if (particlesOperation != null) {
				try {
					particlesOperation.call();
				} catch (Throwable e) {
					AsyncedparticlesClient.LOGGER.warn("Error while executing particles operation", e);
				}
				Caches.particlesOperation = null;
			}
		});
	}
}
