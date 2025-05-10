package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ClassAdjusterMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		MixinClassAdjusterApplication.init(mixinPackage, this);
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return MixinClassAdjusterApplication.getInstance().shouldApplyMixin(targetClassName, mixinClassName);
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return MixinClassAdjusterApplication.getInstance().apply();
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		MixinClassAdjusterApplication.getInstance().preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		MixinClassAdjusterApplication.getInstance().postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
	}
}
