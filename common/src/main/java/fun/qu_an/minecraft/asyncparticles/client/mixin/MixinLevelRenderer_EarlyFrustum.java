package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 50)
public abstract class MixinLevelRenderer_EarlyFrustum {
	@Shadow private @Nullable Frustum capturedFrustum;

	@Shadow @Final private Vector3d frustumPos;

	@Shadow private Frustum cullingFrustum;

	// TODO: 有没有更好的方法？
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void renderLevel(CallbackInfo ci) {
		if (this.capturedFrustum != null) {
			Frustum frustum = this.capturedFrustum;
			frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
			AsyncRenderer.frustum = frustum;
		} else {
			AsyncRenderer.frustum = this.cullingFrustum;
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;prepare(DDD)V"))
	private void redirectPrepare(Frustum frustum, double x, double y, double z) {
		// do nothing
	}
}
