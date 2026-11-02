package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public abstract class MixinParticle_LightCache implements LightCachedParticleAddon {
	@Shadow @Final public ClientLevel level;
	@Shadow public double x;
	@Shadow public double y;
	@Shadow public double z;

	@Override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = GameUtil.SHARED_POS.get().set(x, y, z);
		int light;
		try {
			light = level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			light = 0;
		}
		asyncparticles$setLight(light);
	}

	@Override
	public boolean asyncparticles$isStaticLight() {
		return false;
	}
}
