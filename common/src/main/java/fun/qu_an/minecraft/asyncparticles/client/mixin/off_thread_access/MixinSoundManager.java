package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO: Any better way to handle this?
@Mixin(SoundManager.class)
public abstract class MixinSoundManager {
	@Shadow
	@Final
	private SoundEngine soundEngine;

	@Shadow
	public abstract void queueTickingSound(TickableSoundInstance instance);

	@Shadow
	public abstract SoundEngine.PlayResult play(SoundInstance instance);

	@Shadow
	public abstract void playDelayed(SoundInstance instance, int delay);

	@Shadow
	public abstract void updateSource(Camera camera);

	@Shadow
	public abstract void pauseAllExcept(SoundSource... ignoredSources);

	@Shadow
	public abstract void stop(SoundInstance soundInstance);

	@Shadow
	public abstract void refreshCategoryVolume(SoundSource category);

	@Shadow
	public abstract void updateCategoryVolume(SoundSource source, float gain);

	@Shadow
	public abstract void addListener(SoundEventListener listener);

	@Shadow
	public abstract void removeListener(SoundEventListener listener);

	@Shadow
	public abstract void stop(@Nullable Identifier sound, @Nullable SoundSource source);

	@Inject(method = {
		"reload",
		"resume",
		"tick",
		"stop()V",
		"emergencyShutdown",
		"destroy"
	}, at = @At("HEAD"))
	public void injectReload(CallbackInfo ci) {
		ThreadUtil.assertNotParticleThread();
	}

	@Inject(method = "queueTickingSound", at = @At("HEAD"), cancellable = true)
	public void injectQueueTickingSound(TickableSoundInstance instance, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.queueTickingSound(instance));
		}
	}

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	public void injectPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (ThreadUtil.isOnParticleThread()) {
			cir.setReturnValue(soundEngine.loaded ? SoundEngine.PlayResult.STARTED : SoundEngine.PlayResult.NOT_STARTED);
			ThreadUtil.enqueueClientTask(() -> this.play(instance));
		}
	}

	@Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
	public void injectPlayDelayed(SoundInstance instance, int delay, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.playDelayed(instance, delay));
		}
	}

	@Inject(method = "updateSource", at = @At("HEAD"), cancellable = true)
	public void injectUpdateSource(Camera camera, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.updateSource(camera));
		}
	}

	@Inject(method = "pauseAllExcept", at = @At("HEAD"), cancellable = true)
	public void injectPauseAllExcept(SoundSource[] ignoredSources, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.pauseAllExcept(ignoredSources));
		}
	}

	@Inject(method = "refreshCategoryVolume", at = @At("HEAD"), cancellable = true)
	public void injectRefreshCategoryVolume(SoundSource category, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.refreshCategoryVolume(category));
		}
	}

	@Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
	public void injectStop(SoundInstance soundInstance, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.stop(soundInstance));
		}
	}

	@Inject(method = "updateCategoryVolume", at = @At("HEAD"), cancellable = true)
	public void injectUpdateCategoryVolume(SoundSource source, float gain, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.updateCategoryVolume(source, gain));
		}
	}

	@Inject(method = "addListener", at = @At("HEAD"), cancellable = true)
	public void injectAddListener(SoundEventListener listener, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.addListener(listener));
		}
	}

	@Inject(method = "removeListener", at = @At("HEAD"), cancellable = true)
	public void injectRemoveListener(SoundEventListener listener, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.removeListener(listener));
		}
	}

	@Inject(method = "stop(Lnet/minecraft/resources/Identifier;Lnet/minecraft/sounds/SoundSource;)V", at = @At("HEAD"), cancellable = true)
	public void injectStop(Identifier sound, SoundSource source, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.stop(sound, source));
		}
	}
}
