package fun.qu_an.minecraft.asyncparticles.client.forge;

import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
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

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncparticlesClient.class.getPackage().getName().length() +
											  ".mixin.forge.".length();

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
			case "particlerain_vs" -> ModListHelper.FORGE_PARTICLERAIN_LOADED && ModListHelper.VS_LOADED;
			case "particlerain_create" -> ModListHelper.FORGE_PARTICLERAIN_LOADED && ModListHelper.CREATE_LOADED;
			case "particlerain" -> {
				if (!ModListHelper.FORGE_PARTICLERAIN_LOADED) {
					yield false;
				}
				if (split.length == 2) {
					yield true;
				}
				yield switch (split[1]) {
					case "v1_1_2" -> ModListHelper.versionCheck("particlerain", null, "1.1.3");
					case "v1_1_3" -> ModListHelper.versionCheck("particlerain", "1.1.3", null);
					default -> true;
				};
			}
			case "create" -> ModListHelper.FORGE_CREATE_LOADED && !ModListHelper.IS_LEGACY_CREATE;
			// TODO: 下面这个 mod 没有正式发布，且不确定是否是唯一的 forge 移植版
			case "effecticularity_v1_0_2" -> ModListHelper.FORGE_EFFECTICULARITY_LOADED &&
											 ModListHelper.versionCheck("effective", "1.0.0", "1.0.3");
			case "flerovium" -> ModListHelper.FORGE_FLEROVIUM_LOADED;
			case "embeddium" -> ModListHelper.FORGE_EMBEDDIUM_LOADED;
			case "epicfight" -> ModListHelper.FORGE_EPICFIGHT_LOADED;
			case "epicacg" -> ModListHelper.FORGE_EPICACG_LOADED;
			case "gateways" -> ModListHelper.FORGE_GATEWAYS_LOADED;
			case "subtle_effects" -> ModListHelper.FORGE_SUBTLE_EFFECTS_LOADED;
			case "weather2" -> ModListHelper.FORGE_WEATHER2_LOADED;
			case "weather2_vs" -> ModListHelper.FORGE_WEATHER2_LOADED && ModListHelper.VS_LOADED;
			case "weather2_create" -> ModListHelper.FORGE_WEATHER2_LOADED && ModListHelper.CREATE_LOADED;
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
