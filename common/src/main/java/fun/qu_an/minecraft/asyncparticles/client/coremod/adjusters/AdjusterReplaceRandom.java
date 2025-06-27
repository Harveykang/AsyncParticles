package fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters;

import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinUtil;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjuster;

import java.util.ArrayList;
import java.util.List;

public class AdjusterReplaceRandom implements MixinClassAdjuster {
	private static final String CLASS_NAME = "fun.qu_an.minecraft.asyncparticles.client.mixin.conditional.Mixin_ReplaceRandom";
	@Override
	public String getMixinClassName() {
		return MixinUtil.getMixinClassName(CLASS_NAME);
	}

	@Override
	public List<String> getTargets(List<String> originalTargets) {
		ArrayList<String> list = new ArrayList<>(originalTargets);
		list.addAll(MixinConfigHelper.getReplaceRandom());
		return list;
	}

	@Override
	public String getRefMapperConfig() {
		return MixinUtil.getRefMapperName(CLASS_NAME,"asyncparticles-common-refmap.json");
	}
}
