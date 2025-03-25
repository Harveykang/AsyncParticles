package fun.qu_an.minecraft.asyncparticles.client;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin_extension.ExtensionMixinMethodCancellation;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class APMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		ExtensionMixinMethodCancellation.init();
		ExtensionMixinMethodCancellation.register(new ExtensionMixinMethodCancellation.Canceller() {
			@Override
			public boolean preTest(String mixinClassName) {
				return switch (mixinClassName) {
					case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin" -> true;
					default -> false;
				};
			}

			@Override
			public boolean test(String mixinClassName,
								String mixinMethodName,
								String mixinMethodDesc,
								List<String> mixinParameterNames) {
				return switch (mixinClassName) {
					case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin" ->
						mixinMethodName.equals("shouldRenderParticle");
					default -> false;
				};
			}
		});
		MixinCancellerRegistrar.register((targetClassNames, mixinClassName)
			-> switch (mixinClassName) {
			case "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer",
				 "net.irisshaders.iris.mixin.fabric.MixinLevelRenderer",
				 // disable this because our implementation is better
				 "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
				 // o(≧口≦)o particle_core: These mixins not support async rendering
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerFrustumMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerRotationMixin",
				 "me.fzzyhmstrs.particle_core.mixins.WorldRendererFrustumMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerCachedLightMixin",
				 "me.fzzyhmstrs.particle_core.mixins.BillboardParticleMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleMixin"
//				 , "net.diebuddies.mixins.ocean.MixinParticleEngine" // Physics mod
				-> true;
			default -> false;
		});
	}

	@Override
	public String getRefMapperConfig() {
		// this fixes the useless refmap (crash) on neoforge
		return ModListHelper.IS_FORGE ? null : "fabric-asyncparticles-common-refmap.json";
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
					case "off_thread_access" -> !ModListHelper.IS_FORGE;
					case "particlerain_create" ->
						ModListHelper.FABRIC_PARTICLERAIN_LOADED && ModListHelper.FABRIC_CREATE_LOADED;
					case "particlerain" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED;
					case "create" -> ModListHelper.FABRIC_CREATE_LOADED;
					case "effective" -> ModListHelper.FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> ModListHelper.FABRIC_EFFECTUAL_LOADED;
					case "particular" -> ModListHelper.FABRIC_PARTICULAR_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "fake_renders",
				 "off_thread_access" -> true;
			case "modernui" -> ModListHelper.MODERN_UI_LOADED;
			case "create" -> ModListHelper.CREATE_LOADED;
			case "sodium_0_6" -> ModListHelper.SODIUM_LOADED
								 && ModListHelper.versionCheck("sodium", "0.6", "0.7");
			case "sodium_0_7" -> ModListHelper.SODIUM_LOADED
								 && ModListHelper.versionCheck("sodium", "0.7", "0.8");
			case "iris_like" -> ModListHelper.IRIS_LIKE_LOADED;
			case "a_good_place" -> ModListHelper.A_GOOD_PLACE_LOADED;
			case "subtle_effects" -> {
				if (split.length == 2) {
					yield ModListHelper.SUBTLE_EFFECTS_LOADED;
				}
				yield switch (split[1]) {
					case "fabric" -> !ModListHelper.IS_FORGE && ModListHelper.FABRIC_SUBTLE_EFFECTS_LOADED;
					default -> ModListHelper.SUBTLE_EFFECTS_LOADED;
				};
			}
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
