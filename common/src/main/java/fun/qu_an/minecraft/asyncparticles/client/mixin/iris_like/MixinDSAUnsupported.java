package fun.qu_an.minecraft.asyncparticles.client.mixin.iris_like;

import com.mojang.blaze3d.platform.GlConst;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IrisRenderSystem.DSAUnsupported.class, remap = false)
public class MixinDSAUnsupported {
	// Once Iris fixes the bug, this injection point will fail-safe with no side effects.
	@Inject(method = "bindTextureToUnit", require = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/irisshaders/iris/gl/IrisRenderSystem;bindTextureForSetup(II)V"))
	public void fixBindingTexture(int target, int unit, int texture, CallbackInfo ci) {
		if (target == GlConst.GL_TEXTURE_2D) {
			GlStateManagerAccessor.getTEXTURES()[unit].binding = texture;
		}
	}
}
