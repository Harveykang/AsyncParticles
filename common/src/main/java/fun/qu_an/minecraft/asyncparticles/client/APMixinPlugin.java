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
			case "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer",
				 // o(≧口≦)o particle_core: These mixins not support async rendering
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerFrustumMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerRotationMixin",
				 "me.fzzyhmstrs.particle_core.mixins.WorldRendererFrustumMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerCachedLightMixin",
				 "me.fzzyhmstrs.particle_core.mixins.BillboardParticleMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleMixin",
				 "com.moepus.flerovium.mixins.Particle.ParticleEngineMixin",
				 "com.moepus.flerovium.mixins.Particle.ParticleMixin"
//				 , "net.diebuddies.mixins.ocean.MixinParticleEngine"
				-> true;
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
		if (split.length == 1) {
			return true;
		}
		return switch (split[0]) {
			case "fabric" -> {
				if (split.length == 2) {
					yield !ModListHelper.IS_FORGE;
				}
				yield switch (split[1]) {
					case "particlerain_vs" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED && ModListHelper.FABRIC_VS_LOADED;
					case "particlerain_create" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED && ModListHelper.FABRIC_CREATE_LOADED;
					case "particlerain" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED;
					case "create_5" -> ModListHelper.FABRIC_CREATE_LOADED && ModListHelper.CREATE_MAJOR_VERSION < 6;
					case "create_6" -> ModListHelper.FABRIC_CREATE_LOADED && ModListHelper.CREATE_MAJOR_VERSION == 6;
					case "sodium" -> ModListHelper.FABRIC_SODIUM_LOADED;
					case "effective" -> ModListHelper.FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> ModListHelper.FABRIC_EFFECTUAL_LOADED;
					case "particular" -> ModListHelper.FABRIC_PARTICULAR_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "legacy" -> {
				if (split.length == 2) {
					yield true;
				}
				yield switch (split[1]) {
					case "flywheel" -> ModListHelper.FLYWHEEL_LOADED && ModListHelper.FLYWHEEL_MAJOR_VERSION < 1;
					default -> throw new IllegalArgumentException("Unknown legacy mod mixin: " + mixinClassName);
				};
			}
			case "fake_renders" -> true;
			case "vs2" -> ModListHelper.VS_LOADED;
			case "create" -> ModListHelper.CREATE_LOADED;
			case "iris_like" -> ModListHelper.IRIS_LIKE_LOADED;
			case "sodium_like" -> ModListHelper.SODIUM_LIKE_LOADED;
			case "lodestone" -> ModListHelper.LODESTONE_LOADED;
			case "hexcasting" -> ModListHelper.HEXCASTING_LOADED;
			case "flywheel" -> ModListHelper.FLYWHEEL_LOADED && ModListHelper.FLYWHEEL_MAJOR_VERSION == 1;
			case "particle_core" -> ModListHelper.PARTICLE_CORE_LOADED;
			case "physicsmod" -> ModListHelper.PHYSICSMOD_LOADED;
//			case "enhancedblockentities" -> ModListHelper.ENHANCEDBLOCKENTITIES_LOADED;
			default -> throw new IllegalArgumentException("Unknown mixin: " + mixinClassName);
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
