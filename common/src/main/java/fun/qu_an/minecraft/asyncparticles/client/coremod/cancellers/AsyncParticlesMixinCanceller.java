package fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinConfigHelper;

import java.util.List;

public class AsyncParticlesMixinCanceller implements MixinCanceller {
	@Override
	public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
		if (mixinClassName.startsWith("net.caffeinemc.mods.lithium.mixin.collections.entity_by_type")) {
			return MixinConfigHelper.isSafeClassInstanceMultiMap();
		}
		return switch (mixinClassName) {
			case "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer",
				 "net.irisshaders.iris.mixin.fabric.MixinLevelRenderer",
				 // disable this because our implementation is better
				 "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
//				 "net.diebuddies.mixins.ocean.MixinParticleEngine", // Physics mod
				 "ca.fxco.moreculling.mixin.LevelRenderer_rainMixin" -> true;
			// why? org.spongepowered.asm.mixin.transformer.throwables.IllegalClassLoadError: Illegal classload request for dev.engine_room.flywheel.backend.mixin.light.SkyDataLayerStorageMapAccessor. Mixin is defined in flywheel.backend.mixins.json and cannot be referenced directly
			case "dev.engine_room.flywheel.backend.mixin.light.SkyLightSectionStorageMixin" -> ModListHelper.isDevelopmentEnvironment();
			default -> false;
		};
	}
}
