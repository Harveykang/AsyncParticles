package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import com.bawnorton.mixinsquared.TargetHandler;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientLevel.class, priority = 1500)
public class MixinMixinClientLevel {
	@TargetHandler(
		mixin = "dev.ryanhcode.sable.mixin.clip_overwrite.ClientLevelMixin",
		name = "sable$getPose"
	)
	@Inject(method = "@MixinSquared:Handler", require = 0, at =@At("HEAD"), cancellable = true)
	private void sableGetPose(SubLevel subLevel, CallbackInfoReturnable<Pose3dc> cir) {
		if (ThreadUtil.isOnParticleThread()) {
			cir.setReturnValue(subLevel.logicalPose());
		}
	}
}
