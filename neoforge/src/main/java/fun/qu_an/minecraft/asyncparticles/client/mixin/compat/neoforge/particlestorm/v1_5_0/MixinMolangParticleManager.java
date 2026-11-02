package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.particlestorm.v1_5_0;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormEngineAddon;
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

@Pseudo
@Mixin(targets = "org.mesdag.particlestorm.particle.MolangParticleManager", remap = false)
public abstract class MixinMolangParticleManager implements ParticleStormEngineAddon {
	@Shadow @Final
	protected Reference2ObjectOpenHashMap<ParticleRenderType, Queue<IMolangParticleInstance>> groupedParticles;
	@Shadow @Final
	protected Queue<IMolangParticleInstance> particlesToAdd;

	@Override
	public Map<ParticleRenderType, Queue<IMolangParticleInstance>> asyncparticles$getGroupedParticles() {
		return groupedParticles;
	}

	@Override
	public Queue<IMolangParticleInstance> asyncparticles$getParticlesToAdd() {
		return particlesToAdd;
	}

	@Inject(method = "orderForRender", at = @At("HEAD"), cancellable = true)
	private void renderLayer(ParticleRenderType type, Queue<IMolangParticleInstance> queue, Camera camera,
	                         float partialTick, CallbackInfoReturnable<Iterator<IMolangParticleInstance>> cir) {
		if (!ConfigHelper.isParticleStormGpuRendering()) {
			return;
		}
		if ((Object) this == MolangParticleEngine.INSTANCE) {
			Iterator<IMolangParticleInstance> cpuParticles = ParticleStormCompat.render(type, camera, partialTick);
			if (cpuParticles != null) {
				cir.setReturnValue(cpuParticles);
			}
		}
	}
}
