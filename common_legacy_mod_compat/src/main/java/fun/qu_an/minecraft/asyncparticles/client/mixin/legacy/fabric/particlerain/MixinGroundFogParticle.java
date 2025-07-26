package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.fabric.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.GroundFogParticle;

@Mixin(GroundFogParticle.class)
public abstract class MixinGroundFogParticle extends MixinWeatherParticle implements ParticleAddon {
	protected MixinGroundFogParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		ParticleRainCompat.INSTANCE.fogCount.getAndIncrement();
		setSize(8f, 0.01f);
	}

	@Inject(method = "remove", at = @At(value = "FIELD", ordinal = 0, remap = false, target = "Lpigcart/particlerain/ParticleRainClient;fogCount:I"))
	private void onRemove(CallbackInfo ci) {
		ParticleRainCompat.INSTANCE.fogCount.getAndDecrement();
	}

	@Override
	public AABB getRenderBoundingBox(float partialTick) {
		return this.getBoundingBox().inflate(4.0);
	}
}
