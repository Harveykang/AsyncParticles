package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.fabric.particlerain;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.WeatherParticleSpawner;

@Mixin(value = WeatherParticleSpawner.class)
public class MixinWeatherParticleSpawner {
	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", remap = false, target = "Lpigcart/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.INSTANCE.particleCount.get();
	}

	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", remap = false, target = "Lpigcart/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.INSTANCE.fogCount.get();
	}

	@Unique
	private static final ResourceLocation PARTICLE_RAIN$UPDATE =
		ResourceLocation.fromNamespaceAndPath("particlerain", "update");
	@WrapMethod(method = "update")
	private static void wrapUpdate(ClientLevel level, Entity entity, float f, Operation<Void> original) {
		EndTickOperation.schedule(PARTICLE_RAIN$UPDATE, () -> original.call(level, entity, f));
	}

	@ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;getY()I"))
	private static int redirectGetY(int original) {
		return original - 2;
	}
}
