package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.effective;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.ladysnake.effective.core.particle.RippleParticle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RippleParticle.class, remap = false)
public abstract class MixinRippleParticle extends TextureSheetParticle {
	@Shadow @Final public SpriteSet spriteProvider;

	protected MixinRippleParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "<init>" , at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		setSpriteFromAge(this.spriteProvider);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		setSpriteFromAge(this.spriteProvider);
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lorg/ladysnake/effective/core/particle/RippleParticle;setSpriteFromAge(Lnet/minecraft/client/particle/SpriteSet;)V"))
	private void onRender(RippleParticle particle, SpriteSet spriteProvider) {
		// do nothing
	}
}
