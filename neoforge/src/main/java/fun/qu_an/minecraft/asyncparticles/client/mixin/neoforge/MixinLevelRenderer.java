package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

@Mixin(value = LevelRenderer.class) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer {
	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles0(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float partialTick,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										  LocalIntRef irm) {
		switch (irm.get()) {
			case SYNC -> AsyncRenderBehavior.getInstance().endAll(partialTick, camera, lightTexture, false);
			case COMPATIBILITY_ASYNC -> AsyncRenderBehavior.getInstance().endAll(partialTick, camera, lightTexture, true);
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 1, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles1(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float partialTick,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										  LocalIntRef irm) {
		switch (irm.get()) {
			case MIXED_SYNC, SYNC -> AsyncRenderBehavior.getInstance().endOpaque(lightTexture, camera, partialTick, false);
			case MIXED_ASYNC, COMPATIBILITY_ASYNC ->
				AsyncRenderBehavior.getInstance().endOpaque(lightTexture, camera, partialTick, true);
			case BEFORE_SYNC -> AsyncRenderBehavior.getInstance().endAll(partialTick, camera, lightTexture, false);
			case BEFORE_ASYNC -> AsyncRenderBehavior.getInstance().endAll(partialTick, camera, lightTexture, true);
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 2, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles2(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float partialTick,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										  LocalIntRef irm) {

		switch (irm.get()) {
			case MIXED_SYNC, SYNC -> AsyncRenderBehavior.getInstance().endTranslucent(lightTexture, camera, partialTick, false);
			case MIXED_ASYNC, COMPATIBILITY_ASYNC ->
				AsyncRenderBehavior.getInstance().endTranslucent(lightTexture, camera, partialTick, true);
		}
	}
}
