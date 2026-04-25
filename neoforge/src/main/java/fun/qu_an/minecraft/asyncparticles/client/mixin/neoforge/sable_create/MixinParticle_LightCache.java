package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.sable_create;

import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin.sable.MixinParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.lang.ref.WeakReference;

@Mixin(value = {AirFlowParticle.class, AirParticle.class, SteamJetParticle.class}, priority = 1500)
// Later than mixin.create.MixinParticle_LightCache
public abstract class MixinParticle_LightCache extends MixinParticle {
	@Override // inject after MixinParticle_LightCache to override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		ParticleExtension particleExtension = (ParticleExtension) this;
		BlockPos blockPos = BlockPos.containing(x, y, z);
		int light = level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		SubLevel subLevel = particleExtension.sable$getTrackingSubLevel();
		if (!ConfigHelper.fixParticleLightOnSableSublevel()) {
			asyncparticles$setLight(light);
		} else if (subLevel == null) {
			subLevel = asyncparticle$tracingSubLevel.get();
			if (subLevel == null) {
				asyncparticles$setLight(light);
			} else {
				asyncparticles$clampLight(subLevel, level, light);
			}
		} else {
			if (asyncparticle$tracingSubLevel.get() != subLevel){
				asyncparticle$tracingSubLevel = new WeakReference<>(subLevel);
			}
			asyncparticles$clampLight(subLevel, level, light);
		}
	}

	@Unique
	private void asyncparticles$clampLight(SubLevel subLevel, ClientLevel level, int light) {
		Vector3d world = new Vector3d(x, y, z);
		Vector3d transformed = subLevel.logicalPose().transformPositionInverse(world, world);
		BlockPos pos = BlockPos.containing(transformed.x, transformed.y, transformed.z);
		int shipLight = level.isLoaded(pos) ? LevelRenderer.getLightColor(level, pos) : 0;
		int finalLight = Math.max(light & 0xFFFF, shipLight & 0xFFFF) | // max for block, min for sky
			Math.min(light & 0xFFFF0000, shipLight & 0xFFFF0000);
		asyncparticles$setLight(finalLight);
	}
}
