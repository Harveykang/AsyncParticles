package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.effecticularity;

import concerrox.effective.particle.RippleParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RippleParticle.class)
public abstract class MixinRippleParticle extends TextureSheetParticle {
	@Shadow(remap = false)
	@Final
	private SpriteSet spriteProvider;

	protected MixinRippleParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "<init>", require = 0, at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		setSpriteFromAge(this.spriteProvider);
	}

	@Inject(method = "tick", require = 0, at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		setSpriteFromAge(this.spriteProvider);
	}

	@Redirect(method = "render", require = 0, at = @At(value = "INVOKE",
		target = "Lconcerrox/effective/particle/RippleParticle;setSpriteFromAge(Lnet/minecraft/client/particle/SpriteSet;)V"))
	private void onRender(RippleParticle particle, SpriteSet spriteProvider) {
		// do nothing
	}
}
