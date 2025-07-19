package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.VertexHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer {
	/**
	 * @author Harvey_Husky
	 * @reason Fix negative particle alpha.
	 * I have no idea why 'default' and 'private' injections are causing crash in different mixin environments (on Forge).
	 * And it has NEVER crashed my game, either on Forge or Fabric. So I have to use '@Overwrite' instead.
	 */
	@Overwrite
	default VertexConsumer setColor(float red, float green, float blue, float alpha) {
		return VertexHelper.setColor((VertexConsumer) this, red, green, blue, alpha);
	}
}
