package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Tesselator.class)
public class MixinTesselator {
	@Inject(method = "getInstance", at = @At("HEAD"))
	private static void getInstance(CallbackInfoReturnable<Tesselator> cir) {
		RenderSystem.assertOnRenderThread();
	}
}
