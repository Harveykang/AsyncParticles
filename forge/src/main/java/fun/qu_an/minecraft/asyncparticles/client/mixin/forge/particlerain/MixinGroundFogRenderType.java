package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.particle.render.GroundFogRenderType;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(GroundFogRenderType.class)
public class MixinGroundFogRenderType {
	@WrapWithCondition(method = "end", require = 0, at = @At(value = "INVOKE", remap = false,
		target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(II)V"))
	private boolean redirectSetShaderTexture(int unit, int texture) {
		return unit != 2;
	}
}
