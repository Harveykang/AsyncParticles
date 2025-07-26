package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.prettyrain;

import com.leclowndu93150.particlerain.particle.GroundFogParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GroundFogParticle.class)
public abstract class MixinGroundFogParticle extends MixinWeatherParticle {
	protected MixinGroundFogParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		ParticleRainCompat.INSTANCE.fogCount.getAndIncrement();
		setSize(8f, 0.01f);
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private void onRemove(CallbackInfo ci) {
		ParticleRainCompat.INSTANCE.fogCount.getAndDecrement();
	}
}
