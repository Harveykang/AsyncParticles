package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.VertexHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer {
	/**
	 * @author Harvey_Husky
	 * @reason Fix negative particle alpha.
	 */
	@Overwrite
	default VertexConsumer color(float red, float green, float blue, float alpha) {
		return VertexHelper.setColor((VertexConsumer) this, red, green, blue, alpha);
	}
}
