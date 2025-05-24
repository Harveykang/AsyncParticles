package fun.qu_an.minecraft.asyncparticles.client.coremod.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

public class APMixinPluginNeoForge implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncParticlesClient.class.getPackage().getName().length() +
											  ".mixin.neoforge.".length();

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!IS_CLIENT) {
			return false;
		}
		String mixinPackageName = mixinClassName.substring(PACKAGE_LENGTH);
		String[] split = mixinPackageName.split("\\.");
		if (split.length == 1) {
			return true;
		}
		return switch (split[0]) {
			case "off_thread_access" -> true;
			case "particlerain_create" -> FORGE_PARTICLERAIN_LOADED && CREATE_LOADED;
			case "particlerain" -> FORGE_PARTICLERAIN_LOADED;
			case "create" -> FORGE_CREATE_LOADED;
			// TODO: 下面这个 mod 没有正式发布，且不确定是否是唯一的 forge 移植版
			case "effecticularity" -> FORGE_EFFECTIVE_LOADED;
			case "subtle_effects" -> FORGE_SUBTLE_EFFECTS_LOADED;
			case "simple_weather" -> FORGE_SIMPLE_WEATHER_LOADED;
			case "simple_weather_create" -> FORGE_SIMPLE_WEATHER_LOADED && CREATE_LOADED;
			case "particular" -> FORGE_PARTICULAR_LOADED;
			case "iris_like" -> FORGE_IRIS_LIKE_LOADED;
			case "iris_like_else" -> !FORGE_IRIS_LIKE_LOADED;
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
