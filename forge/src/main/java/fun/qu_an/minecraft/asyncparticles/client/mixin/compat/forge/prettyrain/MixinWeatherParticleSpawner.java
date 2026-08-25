package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.prettyrain;

import com.leclowndu93150.particlerain.WeatherParticleSpawner;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.prettyrain.forge.PrettyRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WeatherParticleSpawner.class)
public class MixinWeatherParticleSpawner {
	@ModifyExpressionValue(method = "spawnParticle", remap = false, at = @At(value = "FIELD", remap = false, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return PrettyRainCompat.particleCount.get();
	}

	@ModifyExpressionValue(method = "spawnParticle", remap = false, at = @At(value = "FIELD", remap = false, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return PrettyRainCompat.fogCount.get();
	}

	@WrapMethod(method = "update", remap = false)
	private static void onUpdate(ClientLevel level, Entity entity, float f, Operation<Void> original) {
		if (ConfigHelper.isTickWeatherAsync()
			&& AsyncTickBehavior.getInstance().isTailTick()) {
			AsyncTickBehavior.getInstance().addTaskEnsureLevelRunning(
				() -> original.call(level, entity, f), ExceptionUtil::toThrowDirectly);
		} else {
			original.call(level, entity, f);
		}
	}

	@ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;getY()I"))
	private static int redirectGetY(int original) {
		return original - 2;
	}

	@WrapWithCondition(method = "spawnParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private static boolean onSpawnParticle(ClientLevel instance, ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return (!ModListHelper.CREATE_LOADED || CreateCompat.canSpawnWeatherParticleFloorToInt(instance, x, y, z))
			&& (!ModListHelper.VS_LOADED || VSCompat.canSpawnWeatherParticle(instance, x, y, z));
	}
}
