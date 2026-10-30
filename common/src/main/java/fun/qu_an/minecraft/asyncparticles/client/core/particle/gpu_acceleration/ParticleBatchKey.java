package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals.ImmersivePortalsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** A contiguous GPU draw range belongs to both a render layer and a world. */
public record ParticleBatchKey(ParticleRenderType layer, ClientLevel world) {
	public static ParticleBatchKey of(TextureSheetParticle particle) {
		return new ParticleBatchKey(particle.getRenderType(), ModListHelper.IMMERSIVE_PORTALS_LOADED
			? ImmersivePortalsCompat.getParticleWorld(particle) : null);
	}

	public static ParticleBatchKey forRender(ParticleRenderType layer) {
		// Immersive Portals switches Minecraft.level for each nested portal view.
		return new ParticleBatchKey(layer, ModListHelper.IMMERSIVE_PORTALS_LOADED
			? Minecraft.getInstance().level : null);
	}

	public static <T extends Collection<TextureSheetParticle>> Map<ParticleBatchKey, Collection<TextureSheetParticle>> groupParticles(
		Map<ParticleRenderType, T> particles) {
		Map<ParticleBatchKey, Collection<TextureSheetParticle>> result = new LinkedHashMap<>();
		for (Map.Entry<ParticleRenderType, T> entry : particles.entrySet()) {
			if (!ModListHelper.IMMERSIVE_PORTALS_LOADED) {
				result.put(new ParticleBatchKey(entry.getKey(), null), entry.getValue());
				continue;
			}
			for (TextureSheetParticle particle : entry.getValue()) {
				result.computeIfAbsent(of(particle), ignored -> new ArrayList<>()).add(particle);
			}
		}
		return result;
	}
}
