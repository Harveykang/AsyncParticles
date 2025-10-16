package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.vs2_create;

import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.MixinParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {AirFlowParticle.class, AirParticle.class, SteamJetParticle.class}, priority = 1500)
// Later than mixin.create.MixinParticle_LightCache
public abstract class MixinParticle_LightCache extends MixinParticle {
	@Override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos.MutableBlockPos blockPos = SHARED_POS.get().set(x, y, z);
		int light;
		try {
			light = level.isLoaded(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			light = 0;
		}
		if (asyncparticles$vsShip == null || !ConfigHelper.fixParticleLightOnVsShips()) {
			asyncparticles$setLight(light);
		} else {
			Vector3d transformed = asyncparticles$vsShip.getWorldToShip().transformPosition(x, y, z, new Vector3d());
			blockPos.set(transformed.x, transformed.y, transformed.z);
			int shipLight;
			try {
				shipLight = level.isLoaded(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
			} catch (MissingPaletteEntryException ignore) {
				// chunk not loaded yet maybe, ignore
				shipLight = 0;
			}
			int finalLight = Math.max(light & 0xFFFF, shipLight & 0xFFFF) | // max for block, min for sky
				Math.min(light & 0xFFFF0000, shipLight & 0xFFFF0000);
			asyncparticles$setLight(finalLight);
		}
	}
}
