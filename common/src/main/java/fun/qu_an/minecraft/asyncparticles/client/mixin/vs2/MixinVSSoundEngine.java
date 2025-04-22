package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = SoundEngine.class, priority = 1500)
public class MixinVSSoundEngine {
	@TargetHandler(
		mixin = "org.valkyrienskies.mod.mixin.feature.sound.client.MixinSoundEngine",
		name = "redirectGet"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V"), cancellable = true)
	private void redirectGet1(CallbackInfoReturnable<Object> cir, @Local(name = "handle") ChannelAccess.ChannelHandle handle) {
		if (handle == null) {
			cir.setReturnValue(null);
		}
	}
}
