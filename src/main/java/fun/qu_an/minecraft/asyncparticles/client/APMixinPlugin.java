package fun.qu_an.minecraft.asyncparticles.client;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class APMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {

	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.vs2")) {
			return ModListHelper.VS_LOADED;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain_vs")) {
			return ModListHelper.PARTICLERAIN_LOADED && ModListHelper.VS_LOADED && !ModListHelper.IS_FORGE;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.forge_particlerain_vs")) {
			return ModListHelper.PARTICLERAIN_LOADED && ModListHelper.VS_LOADED && ModListHelper.IS_FORGE;
		}
		return switch (mixinClassName) {
			case "fun.qu_an.minecraft.asyncparticles.client.mixin.ForgeMixinMinecraft"-> ModListHelper.IS_FORGE;
			case "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinMinecraft"-> !ModListHelper.IS_FORGE;
			case "fun.qu_an.minecraft.asyncparticles.client.mixin.sodium.MixinThreadLocalBufferBuilder" -> ModListHelper.SODIUM_LOADED;
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
