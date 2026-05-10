package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vs2;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, remap = false, priority = 1500)
public class MixinVSAnimation {
	@Dynamic
	@TargetHandler(
		mixin = "org.valkyrienskies.mod.mixin.client.world.MixinClientLevel",
		name = "afterAnimatedTick"
	)
	@Inject(method = "@MixinSquared:Handler",
		at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"),
		cancellable = true) // two injections with the same mixin
	private void afterAnimatedTick1(CallbackInfo ci) {
		if (AsyncTickBehavior.INSTANCE.isCancelled() && !ConfigHelper.forceDoneBlockAnimateTick()) {
			ci.cancel();
		}
	}
}
