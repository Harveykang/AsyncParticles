package fun.qu_an.minecraft.asyncparticles.client;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class APMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		MixinCancellerRegistrar.register((targetClassNames, mixinClassName)
			-> switch (mixinClassName) {
			case
				"com.moepus.flerovium.mixins.Particle.ParticleEngineMixin",
				"com.moepus.flerovium.mixins.Particle.ParticleMixin",
				"net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer" -> true;
			default -> false;
		});
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String mixinPackageName = mixinClassName.substring("fun.qu_an.minecraft.asyncparticles.client.mixin.".length());
		String[] split = mixinPackageName.split("\\.");
		return switch (split[0]) {
			case "fabric" -> {
				if (split.length <= 2) {
					yield !ModListHelper.IS_FORGE;
				}
				yield switch (split[1]) {
					case "particlerain_vs" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED && ModListHelper.VS_LOADED;
					case "particlerain" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED;
					case "create" -> ModListHelper.FABRIC_CREATE_LOADED;
					case "effective" -> ModListHelper.FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> ModListHelper.FABRIC_EFFECTUAL_LOADED;
					default -> false;
				};
			}
			case "forge" -> {
				if (split.length <= 2) {
					yield ModListHelper.IS_FORGE;
				}
				yield switch (split[1]) {
					case "particlerain_vs" -> ModListHelper.FORGE_PARTICLERAIN_LOADED && ModListHelper.VS_LOADED;
					case "particlerain" -> ModListHelper.FORGE_PARTICLERAIN_LOADED;
					case "create" -> ModListHelper.FORGE_CREATE_LOADED;
					// TODO: 下面这个 mod 没有正式发布，且不确定是否是唯一的 forge 移植版
					case "effecticularity" -> ModListHelper.FORGE_EFFECTIVE_LOADED;
					case "flerovium" -> ModListHelper.FORGE_FLEROVIUM_LOADED;
					default -> false;
				};
			}
			case "vs2" -> ModListHelper.VS_LOADED;
			case "iris" -> ModListHelper.IRIS_LOADED;
			case "sodium" -> ModListHelper.SODIUM_LOADED;
			case "flerovium" -> ModListHelper.FORGE_FLEROVIUM_LOADED;
			case "lodestone" -> ModListHelper.LODESTONE_LOADED;
			case "hexcasting" -> ModListHelper.HEXCASTING_LOADED;
			default -> true;
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
