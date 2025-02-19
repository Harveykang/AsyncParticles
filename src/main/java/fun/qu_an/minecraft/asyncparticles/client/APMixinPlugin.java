package fun.qu_an.minecraft.asyncparticles.client;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.moepus.flerovium.mixins.Particle.ParticleEngineMixin;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
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
//				"com.moepus.flerovium.mixins.Particle.ParticleEngineMixin",
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
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.vs2")) {
			return ModListHelper.VS_LOADED;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain_vs")) {
			return ModListHelper.PARTICLERAIN_LOADED && ModListHelper.VS_LOADED && !ModListHelper.IS_FORGE;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.forge_particlerain_vs")) {
			return ModListHelper.PARTICLERAIN_LOADED && ModListHelper.VS_LOADED && ModListHelper.IS_FORGE;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain")) {
			return ModListHelper.PARTICLERAIN_LOADED && !ModListHelper.IS_FORGE;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.forge_particlerain")) {
			return ModListHelper.PARTICLERAIN_LOADED && ModListHelper.IS_FORGE;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.create")) {
			return ModListHelper.CREATE_LOADED && !ModListHelper.IS_FORGE;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.forge_create")) {
			return ModListHelper.CREATE_LOADED && ModListHelper.IS_FORGE;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.iris")) {
			return ModListHelper.IRIS_LOADED;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.effectual")) {
			return ModListHelper.EFFECTUAL_LOADED;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.sodium")) {
			return ModListHelper.SODIUM_LOADED;
		}
		if (mixinClassName.startsWith("fun.qu_an.minecraft.asyncparticles.client.mixin.flerovium")) {
			return ModListHelper.FLEROVIUM_LOADED;
		}
		return switch (mixinClassName) {
			case "fun.qu_an.minecraft.asyncparticles.client.mixin.ForgeMixinMinecraft" -> ModListHelper.IS_FORGE;
			case "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinMinecraft" -> !ModListHelper.IS_FORGE;
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
