package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer {
	@Shadow VertexConsumer color(int red, int green, int blue, int alpha);

	/**
	 * @author Harvey_Husky
	 * @reason Fix negative particle alpha.
	 * I have no idea why 'default' and 'private' injections are causing crash in different mixin environments (on Forge).
	 * And it has NEVER crashed my game, either on Forge or Fabric. So I have to use '@Overwrite' instead.
	 */
	@Overwrite
	default VertexConsumer color(float red, float green, float blue, float alpha) {
		return this.color((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F),
			Math.max(0, (int) (alpha * 255.0F)));
	}
}
