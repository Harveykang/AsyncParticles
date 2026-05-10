package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.figura;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = ParticleEngine.class, priority = 1500)
public class MixinParticleEngine {
	@Unique
	private final Map<Particle, UUID> asyncparticles$concurrentFiguraParticleMap = new ConcurrentHashMap<>();

	@TargetHandler(
		mixin = "org.figuramc.figura.mixin.particle.ParticleEngineMixin",
		name = "figura$clearParticles"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;entrySet()Ljava/util/Set;"))
	private Set<Map.Entry<Particle, UUID>> onClearFiguraParticles(HashMap<Particle, UUID> original) {
		return asyncparticles$concurrentFiguraParticleMap.entrySet();
	}

	@TargetHandler(
		mixin = "org.figuramc.figura.mixin.particle.ParticleEngineMixin",
		name = "tickParticleList"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object onRemoveFiguraParticles(HashMap<Particle, UUID> instance, Object key) {
		return asyncparticles$concurrentFiguraParticleMap.remove((Particle) key);
	}

	@TargetHandler(
		mixin = "org.figuramc.figura.mixin.particle.ParticleEngineMixin",
		name = "figura$spawnParticle"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object onSpawnFiguraParticles(HashMap<Particle, UUID> instance, Object key, Object value) {
		return asyncparticles$concurrentFiguraParticleMap.put((Particle) key, (UUID) value);
	}
}
