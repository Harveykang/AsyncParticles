package fun.qu_an.minecraft.asyncparticles.client.mixin.veil;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinPerformanceLevelRendererMixin {
	@Dynamic
	@TargetHandler(
		name = "clearParticlesColor",
		mixin = "foundry.veil.mixin.performance.client.PerformanceLevelRendererMixin"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true)
	public void clearParticlesColor(CallbackInfo ci) {
		ci.cancel();
	}
}
