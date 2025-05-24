package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.fluffy_fur;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import mod.maxbogomol.fluffy_fur.integration.client.ShadersIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShadersIntegration.class)
public class MixinShadersIntegration {
	@ModifyReturnValue(method = "shouldApply", remap = false, at = @At("RETURN"))
	private static boolean shouldApply(boolean original) {
		return original || InternalRenderingMode.isDelayed();
	}
}
