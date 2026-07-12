package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.flerovium;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SingleQuadParticle.class, priority = 1010)
public abstract class MixinSingleQuadParticleMixin implements LightCachedParticleAddon {
	@Dynamic
	@TargetHandler(
		mixin = "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
		// The two fields added by the mixin will be cancelled by APMixinPlugin.
		name = "renderFast"
	)
	@Redirect(method = "@MixinSquared:Handler",
		at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/client/particle/SingleQuadParticle;flerovium$getLightColorCached(FLnet/minecraft/client/Camera;)I"))
	private int redirectGetLightColor(SingleQuadParticle particle, float pt, Camera camera) {
		try {
			return ((Particle) (Object) this).getLightColor(pt);
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			return 0;
		}
	}

	/**
	 * @reason JDK ThreadLocal is slower in multithreaded environment.
	 */
	@TargetHandler(
		mixin = "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
		name = "renderFast"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", remap = false,
		target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;"))
	private MemoryStack redirectStackPush() {
		return MemStackUtil.stackPush();
	}
}
