package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.prettyrain;

import com.leclowndu93150.particlerain.particle.GroundFogParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.prettyrain.forge.PrettyRainCompat;
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
		PrettyRainCompat.fogCount.getAndIncrement();
		setSize(8f, 0.01f);
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private void onRemove(CallbackInfo ci) {
		PrettyRainCompat.fogCount.getAndDecrement();
	}
}
