package fun.qu_an.minecraft.asyncedparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import java.util.Queue;
import java.util.function.Function;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine2 {
	@ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"), index = 1)
	private Function<ParticleRenderType, Queue<Particle>> madparticleUseEvictingLinkedHashSetQueueInsteadOfEvictingQueue(Function<ParticleRenderType, Queue<Particle>> mappingFunction) {
		return t -> EvictingQueue.create(32768);
	}
}
