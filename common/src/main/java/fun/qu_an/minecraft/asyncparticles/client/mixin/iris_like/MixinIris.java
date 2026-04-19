package fun.qu_an.minecraft.asyncparticles.client.mixin.iris_like;

import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Iris.class)
public class MixinIris {
	@Inject(method = "reload", remap = false, at = @At("RETURN"))
	private static void onReload(CallbackInfo ci) {
		AsyncTickBehavior.INSTANCE.reload(false);
	}
}
