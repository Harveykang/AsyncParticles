package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particlerain;

import com.leclowndu93150.particlerain.WeatherParticleSpawner;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WeatherParticleSpawner.class)
public class MixinWeatherParticleSpawner {
	@ModifyExpressionValue(method = "spawnParticle", remap = false, at = @At(value = "FIELD", remap = false, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.asyncparticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "spawnParticle", remap = false, at = @At(value = "FIELD", remap = false, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.asyncparticles$fogCount.get();
	}

	@Unique
	private static final ResourceLocation asyncparticles$PARTICLE_RAIN$UPDATE =
		ResourceLocation.fromNamespaceAndPath("particlerain", "update");
	@WrapMethod(method = "update", remap = false)
	private static void onUpdate(ClientLevel level, Entity entity, float f, Operation<Void> original) {
		AsyncTicker.addEndTickTask(asyncparticles$PARTICLE_RAIN$UPDATE, () -> original.call(level, entity, f));
	}

	@ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;getY()I"))
	private static int redirectGetY(int original) {
		return original - 2;
	}
}
