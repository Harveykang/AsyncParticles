package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

// TODO: Any better way to handle this?
@Mixin(value = SoundEngine.class, priority = 1500)
public class MixinSoundEngine {
	@Inject(method = "tick", at = @At("HEAD"))
	public void injectTick(CallbackInfo ci) {
		ThreadUtil.assertNotParticleThread();
	}

	@WrapMethod(method = {"reload", "stopAll", "destroy", "stopAll"})
	public void wrapReload(Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call();
		} else {
			ThreadUtil.enqueueClientTask(original::call);
		}
	}

	@WrapMethod(method = "updateCategoryVolume")
	public void wrapUpdateCategoryVolume(SoundSource category, float volume, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(category, volume);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(category, volume));
		}
	}

	@WrapMethod(method = "play")
	public void wrapStop(SoundInstance soundInstance, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(soundInstance);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(soundInstance));
		}
	}

	@WrapMethod(method = {"addEventListener", "removeEventListener"})
	public void wrapAddEventListener(SoundEventListener listener, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(listener);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(listener));
		}
	}

	@Redirect(method = "isActive", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
	public Object redirectIsActive(Map<?, Integer> instance, Object o) {
		return instance.getOrDefault(o, Integer.MAX_VALUE);
	}

	@WrapMethod(method = "queueTickingSound")
	public void wrapQueueTickingSound(TickableSoundInstance tickableSound, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(tickableSound);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(tickableSound));
		}
	}

	@WrapMethod(method = "requestPreload")
	public void wrapRequestPreload(Sound sound, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(sound);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(sound));
		}
	}

	@WrapMethod(method = "playDelayed")
	public void wrapPlayDelayed(SoundInstance sound, int delay, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(sound, delay);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(sound, delay));
		}
	}

	@WrapMethod(method = "stop(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/sounds/SoundSource;)V")
	public void wrapStop(ResourceLocation soundName, SoundSource category, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(soundName, category);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(soundName, category));
		}
	}
}
