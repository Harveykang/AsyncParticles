package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer {
	@ModifyArg(method = "color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", index = 3,
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private int fixNegativeAlpha(int alpha) {
		return Math.max(alpha, 0);
	}
}
