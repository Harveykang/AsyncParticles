package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.LevelBundle;
import net.mehvahdjukaar.polytone.Polytone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Polytone.class, remap = false)
public abstract class MixinPolytone {
	@Inject(method = "onTagsReceived", at = @At("HEAD"))
	private static void joinBeforeTagsReload(CallbackInfo ci) {
		if (LevelBundle.isLevelAvailable()) {
			AsyncTickBehavior.getInstance().reset();
		}
	}
}
