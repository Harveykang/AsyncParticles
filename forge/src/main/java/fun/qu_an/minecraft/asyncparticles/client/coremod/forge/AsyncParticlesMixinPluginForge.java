package fun.qu_an.minecraft.asyncparticles.client.coremod.forge;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

public class AsyncParticlesMixinPluginForge implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncParticlesClient.class.getPackage().getName().length() +
											  ".mixin.forge.".length();

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
			case "prettyrain_vs" -> FORGE_PRETTY_RAIN_LOADED && VS_LOADED;
			case "prettyrain_create" -> FORGE_PRETTY_RAIN_LOADED && CREATE_LOADED;
			case "prettyrain" -> {
				if (!FORGE_PRETTY_RAIN_LOADED) {
					yield false;
				}
				if (split.length == 2) {
					yield true;
				}
				yield switch (split[1]) {
					case "v1_1_2" -> versionCheck("particlerain", null, "1.1.3");
					case "v1_1_3" -> versionCheck("particlerain", "1.1.3", null);
					default -> true;
				};
			}
			case "create" -> FORGE_CREATE_LOADED && !IS_LEGACY_CREATE;
			// TODO: 下面这个 mod 没有正式发布，且不确定是否是唯一的 forge 移植版
			case "effecticularity_v1_0_2" -> FORGE_EFFECTICULARITY_LOADED &&
											 versionCheck("effective", "1.0.0", "1.0.3");
			case "flerovium" -> FORGE_FLEROVIUM_LOADED;
			case "embeddium" -> FORGE_EMBEDDIUM_LOADED;
			case "epicfight" -> FORGE_EPICFIGHT_LOADED;
			case "epicacg" -> FORGE_EPICACG_LOADED;
			case "gateways" -> FORGE_GATEWAYS_LOADED;
			case "subtle_effects" -> FORGE_SUBTLE_EFFECTS_LOADED;
			case "weather2" -> FORGE_WEATHER2_LOADED;
			case "weather2_vs" -> FORGE_WEATHER2_LOADED && VS_LOADED;
			case "weather2_create" -> FORGE_WEATHER2_LOADED && CREATE_LOADED;
			case "particular" -> FORGE_PARTICULAR_LOADED;
			case "iris_like" -> FORGE_IRIS_LIKE_LOADED;
			case "iris_like_else" -> !FORGE_IRIS_LIKE_LOADED;
			case "fluffy_fur" -> FORGE_FLUFFY_FUR_LOADED;
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
