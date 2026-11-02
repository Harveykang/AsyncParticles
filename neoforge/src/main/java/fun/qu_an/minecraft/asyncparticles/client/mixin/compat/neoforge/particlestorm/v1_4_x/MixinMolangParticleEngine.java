package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.particlestorm.v1_4_x;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

@Mixin(value = MolangParticleEngine.class, remap = false)
public abstract class MixinMolangParticleEngine implements ParticleStormEngineAddon {
	@Shadow
	@Final
	private Reference2ObjectOpenHashMap<ParticleRenderType, Queue<IMolangParticleInstance>> groupedParticles;
	@Shadow
	@Final
	private Queue<IMolangParticleInstance> particlesToAdd;

	@Override
	public Map<ParticleRenderType, Queue<IMolangParticleInstance>> asyncparticles$getGroupedParticles() {
		return groupedParticles;
	}

	@Override
	public Queue<IMolangParticleInstance> asyncparticles$getParticlesToAdd() {
		return particlesToAdd;
	}


	@WrapOperation(method = "renderParticles", at = @At(value = "INVOKE", target =
		"Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
	private Iterator<IMolangParticleInstance> renderLayer(Queue<IMolangParticleInstance> queue,
														  Operation<Iterator<IMolangParticleInstance>> original,
														  @Local ParticleRenderType type,
														  @Local(argsOnly = true) Camera camera,
														  @Local(argsOnly = true) float partialTick) {
		if (!ConfigHelper.isParticleStormGpuRendering()) {
			return original.call(queue);
		}
		Iterator<IMolangParticleInstance> cpuParticles = ParticleStormCompat.render(type, camera, partialTick);
		return cpuParticles != null ? cpuParticles : original.call(queue);
	}
}
