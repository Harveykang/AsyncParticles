package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.fabric.iris;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer {
	@Inject(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
	private void beforeRenderEntities(DeltaTracker deltaTracker,
									  boolean renderBlockOutline,
									  Camera camera,
									  GameRenderer gameRenderer,
									  LightTexture lightTexture,
									  Matrix4f frustumMatrix,
									  Matrix4f projectionMatrix,
									  CallbackInfo ci,
									  @Local(ordinal = 0) float partialTick,
									  @Share(namespace = AsyncParticlesClient.MOD_ID, value = "internalRenderingMode")
									  LocalIntRef irm) {
		switch (irm.get()) {
			case MIXED_SYNC -> AsyncRenderBehavior.INSTANCE.endOpaque(lightTexture, camera, partialTick, false);
			case BEFORE_SYNC -> AsyncRenderBehavior.INSTANCE.endAll(partialTick, camera, lightTexture, false);
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "FIELD", ordinal = 0,
		target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;", opcode = Opcodes.GETFIELD))
	private void onRenderLevelTransparencyChain(DeltaTracker deltaTracker,
												boolean renderBlockOutline,
												Camera camera,
												GameRenderer gameRenderer,
												LightTexture lightTexture,
												Matrix4f frustumMatrix,
												Matrix4f projectionMatrix,
												CallbackInfo ci,
												@Local(ordinal = 0) float partialTick,
												@Share(namespace = AsyncParticlesClient.MOD_ID, value = "internalRenderingMode")
												LocalIntRef irm) {
		switch (irm.get()) {
			case SYNC -> AsyncRenderBehavior.INSTANCE.endOpaque(lightTexture, camera, partialTick, false);
			case MIXED_ASYNC, COMPATIBILITY_ASYNC -> AsyncRenderBehavior.INSTANCE.endOpaque(lightTexture, camera, partialTick, true);
			case BEFORE_ASYNC -> AsyncRenderBehavior.INSTANCE.endAll(partialTick, camera, lightTexture, true);
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
	private void redirectRenderParticles(ParticleEngine instance,
										 LightTexture lightTexture,
										 Camera camera,
										 float partialTick,
										 @Share(namespace = AsyncParticlesClient.MOD_ID, value = "internalRenderingMode")
										 LocalIntRef irm) {
		switch (irm.get()) {
			case MIXED_SYNC, SYNC -> AsyncRenderBehavior.INSTANCE.endTranslucent(lightTexture, camera, partialTick, false);
			case MIXED_ASYNC, COMPATIBILITY_ASYNC ->
				AsyncRenderBehavior.INSTANCE.endTranslucent(lightTexture, camera, partialTick, true);
		}
	}
}
