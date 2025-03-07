package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.SingleQuadParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class MixinSingleQuadParticle extends Particle implements SingleQuadParticleAddon {
	@Unique
	private byte asyncParticles$lightCache;

	protected MixinSingleQuadParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		int lightColor = super.getLightColor(0.0f);
		// FIXME: use super to avoid NPE with some mods
		asyncParticles$setLight(lightColor);
	}

	@Redirect(method = "renderRotatedQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Quaternionf;FFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/SingleQuadParticle;getLightColor(F)I"))
	private int redirectGetLightColor(SingleQuadParticle particle, float partialTicks) {
		return SimplePropertiesConfig.particleLightCache() ? asyncParticles$getLight() : particle.getLightColor(partialTicks);
	}

	@Override
	public void asyncParticles$setLight(int light) {
		asyncParticles$lightCache = (byte) (light >> 4 & 0xF | light >> 16 & 0xF0);
	}

	@Override
	public int asyncParticles$getLight() {
		byte lightCache = asyncParticles$lightCache;
		return (lightCache & 0xF) << 4 | (lightCache & 0xF0) << 16;
	}
}
