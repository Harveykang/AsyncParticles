package fun.qu_an.minecraft.asyncparticles.client.forge;

import fun.qu_an.minecraft.asyncparticles.client.ModListHelper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class APMixinPluginForge implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String mixinPackageName = mixinClassName.substring("fun.qu_an.minecraft.asyncparticles.client.mixin.forge.".length());
		String[] split = mixinPackageName.split("\\.");
		if (split.length == 1) {
			return true;
		}
		return switch (split[0]) {
			case "particlerain_vs" -> ModListHelper.FORGE_PARTICLERAIN_LOADED && ModListHelper.FORGE_VS_LOADED;
			case "particlerain_create" -> ModListHelper.FORGE_PARTICLERAIN_LOADED && ModListHelper.FORGE_CREATE_LOADED;
			case "particlerain" -> ModListHelper.FORGE_PARTICLERAIN_LOADED;
			case "create" -> ModListHelper.FORGE_CREATE_LOADED && ModListHelper.CREATE_MAJOR_VERSION == 6;
			// TODO: 下面这个 mod 没有正式发布，且不确定是否是唯一的 forge 移植版
			case "effecticularity" -> ModListHelper.FORGE_EFFECTIVE_LOADED;
			case "flerovium" -> ModListHelper.FORGE_FLEROVIUM_LOADED;
			case "embeddium" -> ModListHelper.FORGE_EMBEDDIUM_LOADED;
			default -> throw new IllegalArgumentException("Unknown forge mixin: " + mixinClassName);
		};
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}
}
