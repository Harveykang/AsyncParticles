package fun.qu_an.minecraft.asyncparticles.client.mixin.iris;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@TargetHandler(
		name = "iris$resetParticleManagerPhase",
		mixin = "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer"
	)
	@Inject(method = "@MixinSquared:Handler",
		at = @At("HEAD"),
		cancellable = true)
	private void iris$resetParticleManagerPhase(CallbackInfo ci) {
		ci.cancel();
	}

	@TargetHandler(
		name = "iris$renderOpaqueParticles",
		mixin = "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true)
	private void iris$renderOpaqueParticles(CallbackInfo ci) {
		ci.cancel();
	}

	@TargetHandler(
		name = "iris$renderTranslucentAfterDeferred",
		mixin = "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer"
	)
	@Inject(method = "@MixinSquared:Handler", require = 0, at = @At("HEAD"), cancellable = true)
	private void iris$renderTranslucentAfterDeferred2(CallbackInfo ci) {
		ci.cancel();
	}
}
