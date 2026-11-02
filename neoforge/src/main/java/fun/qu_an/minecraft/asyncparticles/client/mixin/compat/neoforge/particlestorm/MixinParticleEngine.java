package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.particlestorm;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormEngineAddon;
import net.minecraft.client.particle.ParticleEngine;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	@ModifyExpressionValue(method = "countParticles", at = @At(value = "INVOKE", target = "Ljava/util/stream/IntStream;sum()I"))
	private int addParticleStormCount(int original) {
		return original + ((ParticleStormEngineAddon) (Object) MolangParticleEngine.INSTANCE)
			.asyncparticles$getGroupedParticles().values().stream().mapToInt(Queue::size).sum();
	}
}
