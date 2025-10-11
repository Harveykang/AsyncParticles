package fun.qu_an.minecraft.asyncparticles.client.mixin.core;

import fun.qu_an.minecraft.asyncparticles.client.core.VertexHelper;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ARGB.class)
public class MixinARGB {
	@ModifyVariable(method = "as8BitChannel", at = @At("HEAD"), argsOnly = true)
	private static float modifyColor(float value) {
		return VertexHelper.getColor(value);
	}
}
