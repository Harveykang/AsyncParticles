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
				case "com.moepus.flerovium.mixins.Particle.ParticleEngineMixin",
					 "com.moepus.flerovium.mixins.Particle.ParticleMixin",
					 "net.coderbot.iris.mixin.fantastic.MixinLevelRenderer",
					 "me.jellysquid.mods.sodium.mixin.features.particle.cull.MixinParticleManagerr"-> true;
				default -> false;
			});
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return switch (mixinClassName) {
			case "fun.qu_an.minecraft.asyncparticles.client.mixin.iris.MixinLevelRenderer" -> ModListHelper.IRIS_LOADED;
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
