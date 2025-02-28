package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.AssertionUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Tesselator.class, priority = 100) // make sure before other mod's mixins
public class MixinTesselator {
	@Inject(method = "getInstance", at = @At("HEAD"))
	private static void getInstance(CallbackInfoReturnable<Tesselator> cir) {
		AssertionUtil.assertNotParticleRendererThread();
	}

	@Inject(method = "getBuilder", at = @At("HEAD"))
	private void getBuilder(CallbackInfoReturnable<BufferBuilder> cir) {
		AssertionUtil.assertNotParticleRendererThread();
	}
}
