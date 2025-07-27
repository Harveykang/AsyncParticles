package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.veil;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.light.renderer.LightRenderer")
public class MixinLightRenderer {
	@Unique
	private static final MethodHandle asyncparticles$addLight;
	@Unique
	private static final MethodHandle asyncparticles$removeLight;

	static {
		try {
			Class<?> lightClass = Class.forName("foundry.veil.api.client.render.light.Light");
			Method addLight = MixinLightRenderer.class.getDeclaredMethod("addLight",
				lightClass);
			asyncparticles$addLight = MethodHandles.lookup().unreflect(addLight);
			Method removeLight = MixinLightRenderer.class.getDeclaredMethod("removeLight",
				lightClass);
			asyncparticles$removeLight = MethodHandles.lookup().unreflect(removeLight);
		} catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Dynamic
	@Inject(method = "addLight(Lfoundry/veil/api/client/render/light/Light;)V", remap = false, cancellable = true,
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThreadOrInit()V"))
	private void onAddLight(@Coerce Object light, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()){
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> {
				try {
					asyncparticles$addLight.invoke((Object) this, light);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	@Dynamic
	@Inject(method = "removeLight(Lfoundry/veil/api/client/render/light/Light;)V", remap = false, cancellable = true,
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThreadOrInit()V"))
	private void onRemoveLight(@Coerce Object light, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()){
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> {
				try {
					asyncparticles$removeLight.invoke((Object) this, light);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
}
