package fun.qu_an.minecraft.asyncparticles.client.mixin.hexcasting;

import at.petrak.hexcasting.client.particles.ConjureParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConjureParticle.class)
public abstract class MixinConjureParticle extends TextureSheetParticle {
	protected MixinConjureParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTick(CallbackInfo ci) {
		if (alpha <= 0.001) {
			alpha = 0.0f;
		}
	}
}
