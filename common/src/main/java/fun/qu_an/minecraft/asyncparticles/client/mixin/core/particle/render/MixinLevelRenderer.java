package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.render;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=captureFrustum"))
	public void renderLevel(CallbackInfo ci, @Local(ordinal = 0) Frustum frustum) {
		((ParticleEngineAddon) minecraft.particleEngine).asyncparticle$setFrustum(frustum);
	}
}
