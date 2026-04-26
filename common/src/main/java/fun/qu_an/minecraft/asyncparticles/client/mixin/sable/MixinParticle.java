package fun.qu_an.minecraft.asyncparticles.client.mixin.sable;

import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.ref.WeakReference;

@Mixin(value = Particle.class, priority = 1500)
public abstract class MixinParticle implements LightCachedParticleAddon {
	@Shadow
	@Final
	public ClientLevel level;

	@Shadow
	public double x;

	@Shadow
	public double y;

	@Shadow
	public double z;

	@Shadow
	public double xo;
	@Shadow
	public double yo;
	@Shadow
	public double zo;
	@Unique
	protected WeakReference<SubLevel> asyncparticle$tracingSubLevel = new WeakReference<>(null);

	@Unique
	protected BlockPos asyncparticle$lossSublevelPos;

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
		} else if (subLevel != null) {
			if (asyncparticle$tracingSubLevel.get() != subLevel) {
				asyncparticle$tracingSubLevel = new WeakReference<>(subLevel);
			}
			asyncparticles$clampLight(subLevel, level, light);
		} else if ((subLevel = asyncparticle$tracingSubLevel.get()) == null) {
			asyncparticles$setLight(light);
			asyncparticle$lossSublevelPos = null;
		} else if (asyncparticle$lossSublevelPos == null) {
			asyncparticles$clampLight(subLevel, level, light);
			asyncparticle$lossSublevelPos = BlockPos.containing(xo, yo, zo);
		} else if (asyncparticle$lossSublevelPos.distManhattan(blockPos) > 15) {
			asyncparticles$setLight(light);
		} else {
			asyncparticles$clampLight(subLevel, level, light);
		}
	}

	@Unique
	private void asyncparticles$clampLight(SubLevel subLevel, ClientLevel level, int light) {
		Vector3d world = new Vector3d(x, y, z);
		Vector3d transformed = subLevel.logicalPose().transformPositionInverse(world, world);
		BlockPos pos = BlockPos.containing(transformed.x, transformed.y, transformed.z);
		int shipLight = level.hasChunkAt(pos) ? LevelRenderer.getLightColor(level, pos) : 0;
		int finalLight = Math.max(light & 0xFFFF, shipLight & 0xFFFF) | // max for block, min for sky
			Math.min(light & 0xFFFF0000, shipLight & 0xFFFF0000);
		asyncparticles$setLight(finalLight);
	}
}
