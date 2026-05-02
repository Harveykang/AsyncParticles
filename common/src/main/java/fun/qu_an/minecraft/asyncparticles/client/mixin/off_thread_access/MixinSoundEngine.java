package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

// TODO: Any better way to handle this?
@Mixin(value = SoundEngine.class, priority = 500)
public abstract class MixinSoundEngine {
	@Shadow
	private boolean loaded;

	@Shadow
	public abstract void resume();

	@Shadow
	public abstract void reload();

	@Shadow
	public abstract void stopAll();

	@Shadow
	public abstract void destroy();

	@Shadow
	public abstract void refreshCategoryVolume(SoundSource soundSource);

	@Shadow
	public abstract SoundEngine.PlayResult play(SoundInstance soundInstance);

	@Shadow
	public abstract void addEventListener(SoundEventListener soundEventListener);

	@Shadow
	public abstract void removeEventListener(SoundEventListener soundEventListener);

	@Shadow
	public abstract void queueTickingSound(TickableSoundInstance tickableSoundInstance);

	@Shadow
	public abstract void requestPreload(Sound sound);

	@Shadow
	public abstract void playDelayed(SoundInstance soundInstance, int i);

	@Shadow
	public abstract void stop(@Nullable Identifier resourceLocation, @Nullable SoundSource soundSource);

	@Shadow
	public abstract void stop(SoundInstance soundInstance);

	@Shadow
	public abstract float calculateVolume(float f, SoundSource soundSource);

	@Shadow
	public abstract void pauseAllExcept(SoundSource... soundSources);

	@Shadow
	public abstract void updateSource(Camera camera);

	@Shadow
	public abstract void updateCategoryVolume(SoundSource soundSource, float f);

	@Shadow
	public abstract void emergencyShutdown();

	@Inject(method = "tick", at = @At("HEAD"))
	public void injectTick(CallbackInfo ci) {
		ThreadUtil.assertNotParticleThread();
	}

	@Inject(method = "resume", at = @At("HEAD"), cancellable = true)
	public void injectResume(CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(this::resume);
		}
	}

	@Inject(method = "reload", at = @At("HEAD"), cancellable = true)
	public void injectReload(CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(this::reload);
		}
	}

	@Inject(method = "stopAll", at = @At("HEAD"), cancellable = true)
	public void injectStopAll(CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(this::stopAll);
		}
	}

	@Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
	public void injectDestroy(CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(this::destroy);
		}
	}

	@Inject(method = "updateCategoryVolume", at = @At("HEAD"), cancellable = true)
	public void injectUpdateCategoryVolume(SoundSource soundSource, float f, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.updateCategoryVolume(soundSource, f));
		}
	}

	@Inject(method = "refreshCategoryVolume", at = @At("HEAD"), cancellable = true)
	public void injectRefreshCategoryVolume(SoundSource soundSource, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.refreshCategoryVolume(soundSource));
		}
	}

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	public void injectPlay(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (ThreadUtil.isOnParticleThread()) {
			cir.setReturnValue(this.loaded ? SoundEngine.PlayResult.STARTED : SoundEngine.PlayResult.NOT_STARTED);
			ThreadUtil.enqueueClientTask(() -> cir.setReturnValue(this.play(soundInstance)));
		}
	}

	@Inject(method = "addEventListener", at = @At("HEAD"), cancellable = true)
	public void injectAddEventListener(SoundEventListener listener, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.addEventListener(listener));
		}
	}

	@Inject(method = "removeEventListener", at = @At("HEAD"), cancellable = true)
	public void injectRemoveEventListener(SoundEventListener listener, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.removeEventListener(listener));
		}
	}

	@WrapOperation(method = "isActive", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
	public Object redirectIsActive(Map<Object, Object> instance, Object o, Operation<Object> original) {
		Object o1 = original.call(instance, o);
		return o1 == null ? Integer.MAX_VALUE : o1;
	}

	@Inject(method = "queueTickingSound", at = @At("HEAD"), cancellable = true)
	public void injectQueueTickingSound(TickableSoundInstance tickableSound, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.queueTickingSound(tickableSound));
		}
	}

	@Inject(method = "requestPreload", at = @At("HEAD"), cancellable = true)
	public void injectRequestPreload(Sound sound, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.requestPreload(sound));
		}
	}

	@Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
	public void injectPlayDelayed(SoundInstance soundInstance, int i, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.playDelayed(soundInstance, i));
		}
	}

	@Inject(method = "stop(Lnet/minecraft/resources/Identifier;Lnet/minecraft/sounds/SoundSource;)V", at = @At("HEAD"), cancellable = true)
	public void injectStop(Identifier soundName, SoundSource category, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.stop(soundName, category));
		}
	}

	@Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
	public void injectStop(SoundInstance soundInstance, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.stop(soundInstance));
		}
	}

	@Inject(method = "pauseAllExcept", at = @At("HEAD"), cancellable = true)
	public void injectPauseAllExcept(SoundSource[] soundSources, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.pauseAllExcept(soundSources));
		}
	}

	@Inject(method = "updateSource", at = @At("HEAD"), cancellable = true)
	public void injectUpdateSource(Camera camera, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.updateSource(camera));
		}
	}
}
