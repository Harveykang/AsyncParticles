package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.mehvahdjukaar.polytone.content.config.OptionHolder;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OptionHolder.class, remap = false)
public abstract class MixinOptionHolder {
	@Shadow
	@Final
	public ResourceLocation fileId;
	@Unique
	private boolean asyncparticles$asyncTickOption;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void identifyAsyncTickOption(CallbackInfo ci) {
		asyncparticles$asyncTickOption = fileId.getNamespace().equals("polytone")
			&& fileId.getPath().equals("custom_particles_async");
	}

	@ModifyReturnValue(method = "get", at = @At("RETURN"))
	private Object useAsyncTickSetting(Object original) {
		if (!asyncparticles$asyncTickOption) {
			return original;
		}
		return Boolean.TRUE.equals(original) || (ConfigHelper.isAsyncParticleTick() && ConfigHelper.isPolytoneAsyncTick());
	}
}
