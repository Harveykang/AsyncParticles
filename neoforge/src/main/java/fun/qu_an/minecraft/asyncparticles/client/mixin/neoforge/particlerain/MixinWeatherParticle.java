package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particlerain;

import com.leclowndu93150.particlerain.particle.WeatherParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle {
	@Shadow
	public abstract void remove();

	@Shadow(remap = false) protected BlockPos.MutableBlockPos pos;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		ParticleRainCompat.asyncParticles$particleCount.getAndIncrement();
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private void onRemove(CallbackInfo ci) {
		ParticleRainCompat.asyncParticles$particleCount.getAndDecrement();
	}

	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Override
	public @NotNull AABB getRenderBoundingBox(float partialTicks) {
		return this.getBoundingBox().inflate(2.0);
	}
}
