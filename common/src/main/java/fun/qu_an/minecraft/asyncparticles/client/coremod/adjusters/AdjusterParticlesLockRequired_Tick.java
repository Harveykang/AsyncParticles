package fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters;

import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinUtil;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjuster;

import java.util.List;

public class AdjusterParticlesLockRequired_Tick implements MixinClassAdjuster {
	private static final String CLASS_NAME = "fun.qu_an.minecraft.asyncparticles.client.mixin.conditional.MixinParticles_LockRequired_Tick";
	@Override
	public String getMixinClassName() {
		return CLASS_NAME;
	}

	@Override
	public List<String> getTargets(List<String> originalTargets) {
		return List.copyOf(MixinConfigHelper.getLockRequired());
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}
}
