package fun.qu_an.minecraft.asyncparticles.client.mixin.core.deffered_texture_tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(TextureManager.class)
public class MixinTextureManager {
	@Unique
	private static boolean asyncparticles$deferredTickEnabled = true;

	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {
		if (ConfigHelper.isDeferredTextureTick()
			&& AsyncTickBehavior.getInstance().isTailTick()) {
			ThreadUtil.enqueueClientTask(() -> {
				if (asyncparticles$deferredTickEnabled) {
					original.call();
				}
			});
		} else {
			original.call();
		}
	}

	/**
	 * When TextureManager.close() is called (typically before reload),
	 * disable deferred ticks to prevent stale callbacks from trying to
	 * upload textures on already-destroyed GPU resources.
	 */
	@Inject(method = "close", at = @At("HEAD"))
	private void onClose(CallbackInfo ci) {
		asyncparticles$deferredTickEnabled = false;
	}

	/**
	 * Wrap the reload to disable deferred ticks during the entire reload window.
	 * Re-enable only after the reload future completes (success or failure),
	 * since the new GPU resources are not valid until then.
	 */
	@WrapMethod(method = "reload")
	private CompletableFuture<Void> wrapReload(
		PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor mainExecutor, Operation<CompletableFuture<Void>> original
	) {
		asyncparticles$deferredTickEnabled = false;
		return original.call(preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, mainExecutor)
			.whenCompleteAsync((a, b) -> asyncparticles$deferredTickEnabled = true, mainExecutor);
	}
}
