package fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters;

import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjuster;

import java.util.List;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.IS_FORGE;
import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.isDevelopmentEnvironment;

public class AdjusterParticlesLockProvider implements MixinClassAdjuster {
	@Override
	public String getMixinClassName() {
		return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "neoforge." : "fabric.") +
			   "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticles_LockProvider";
	}

	@Override
	public List<String> getTargets(List<String> originalTargets) {
		return List.copyOf(MixinConfigHelper.getLockProvider());
	}

	@Override
	public String getRefMapperConfig() {
		return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "neoforge-" : "fabric-") +
			   "asyncparticles-common-refmap.json";
	}
}
