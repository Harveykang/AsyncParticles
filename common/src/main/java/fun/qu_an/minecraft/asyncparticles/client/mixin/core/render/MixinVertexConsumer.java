package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.VertexHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer {
	/**
	 * @author Harvey_Husky
	 * @reason Fix negative particle alpha.
	 */
	@ModifyVariable(method = "setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At(value = "HEAD"),
		ordinal = 3, argsOnly = true)
	default float color(float alpha) {
		return VertexHelper.checkAlpha(alpha);
	}
}
