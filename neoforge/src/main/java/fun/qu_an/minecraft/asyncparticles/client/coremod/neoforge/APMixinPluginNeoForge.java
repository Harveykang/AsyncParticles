package fun.qu_an.minecraft.asyncparticles.client.coremod.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class APMixinPluginNeoForge implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncparticlesClient.class.getPackage().getName().length() +
											  ".mixin.neoforge.".length();

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!ModListHelper.IS_CLIENT) {
			return false;
		}
		String mixinPackageName = mixinClassName.substring(PACKAGE_LENGTH);
		String[] split = mixinPackageName.split("\\.");
		if (split.length == 1) {
			return true;
		}
		return switch (split[0]) {
			case "off_thread_access" -> true;
			case "particlerain_create" -> ModListHelper.FORGE_PARTICLERAIN_LOADED && ModListHelper.FORGE_CREATE_LOADED;
			case "particlerain" -> ModListHelper.FORGE_PARTICLERAIN_LOADED;
			case "create" -> ModListHelper.FORGE_CREATE_LOADED;
			// TODO: 下面这个 mod 没有正式发布，且不确定是否是唯一的 forge 移植版
			case "effecticularity" -> ModListHelper.FORGE_EFFECTIVE_LOADED;
			case "subtle_effects" -> ModListHelper.FORGE_SUBTLE_EFFECTS_LOADED;
			case "iris_like" -> ModListHelper.FORGE_IRIS_LIKE_LOADED;
			case "simple_weather" -> ModListHelper.FORGE_SIMPLE_WEATHER_LOADED;
			case "particular" -> ModListHelper.FORGE_PARTICULAR_LOADED;
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
