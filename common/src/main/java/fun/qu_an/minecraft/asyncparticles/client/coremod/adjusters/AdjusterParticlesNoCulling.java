package fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters;

import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjuster;

import java.util.List;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.IS_FORGE;
import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.isDevelopmentEnvironment;

public class AdjusterParticlesNoCulling implements MixinClassAdjuster {
	@Override
	public String getMixinClassName() {
		return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge." : "fabric.") +
			   "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticles_NoCulling";
	}

	@Override
	public List<String> getTargets(List<String> originalTargets) {
		return List.copyOf(MixinConfigHelper.getNoCulling());
	}

	@Override
	public String getRefMapperConfig() {
		return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge-" : "fabric-") +
			   "asyncparticles-common-refmap.json";
	}
}
