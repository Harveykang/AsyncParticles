package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon, LightCachedParticleAddon {
	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Shadow
	@Final
	protected ClientLevel level;

	@Shadow
	protected abstract int getLightCoords(float a);

	@Unique
	private volatile boolean asyncparticles$ticked = true; // always true at first tick
	@Unique
	private boolean asyncparticles$enableLightCache = false;

	@Override
	public void asyncparticles$setTicked() {
		this.asyncparticles$ticked = true;
	}

	@Override
	public void asyncparticles$resetTicked() {
		this.asyncparticles$ticked = false;
	}

	@Override
	public boolean asyncparticles$isTicked() {
		return this.asyncparticles$ticked;
	}

	@Override
	public void asyncparticles$enableLightCache(boolean enable) {
		asyncparticles$enableLightCache = enable;
	}

	public boolean asyncparticles$isEnabledLightCache() {
		return asyncparticles$enableLightCache;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return (Class) this.getClass();
	}

	@Override
	public int asyncparticles$invoke_getLightCoords(float partialTickTime) {
		try {
			return getLightCoords(partialTickTime);
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			return 0;
		}
	}
}
