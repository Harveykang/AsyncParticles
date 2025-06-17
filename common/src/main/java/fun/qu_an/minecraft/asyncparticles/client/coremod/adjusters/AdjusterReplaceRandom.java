package fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters;

import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjuster;

import java.util.ArrayList;
import java.util.List;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.IS_FORGE;
import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.isDevelopmentEnvironment;

public class AdjusterReplaceRandom implements MixinClassAdjuster {
	@Override
	public String getMixinClassName() {
		return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "neoforge." : "fabric.") +
			   "fun.qu_an.minecraft.asyncparticles.client.mixin.conditional.Mixin_ReplaceRandom";
	}

	@Override
	public List<String> getTargets(List<String> originalTargets) {
		ArrayList<String> list = new ArrayList<>(originalTargets);
		list.addAll(MixinConfigHelper.getReplaceRandom());
		return list;
	}

	@Override
	public String getRefMapperConfig() {
		return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "neoforge-" : "fabric-") +
			   "asyncparticles-common-refmap.json";
	}
}
