package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexFormatElement.class)
public class MixinVertexFormatElement {
	@Final
	@Shadow
	@Mutable
	private static VertexFormatElement[] BY_ID;
}
