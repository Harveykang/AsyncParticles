package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.ExtensionCancelMixinMethod;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

public class APMixinPlugin implements IMixinConfigPlugin {
	static final ILogger LOGGER = MixinService.getService().getLogger("asyncparticles:plugin");

	@Override
	public void onLoad(String mixinPackage) {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		// init extensions
		ExtensionCancelMixinMethod.init();
//		ExtensionRegistrar.register(new ExtensionMemberCancelApplication());
//		MixinTargetsModifierApplication.init(GeneratedImplDummy.LOOKUP, this);

		ExtensionCancelMixinMethod.register(new ExtensionCancelMixinMethod.Canceller() {
			@Override
			public boolean preTest(String mixinClassName) {
				return switch (mixinClassName) {
					case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin",
						 "net.irisshaders.iris.mixin.fabric.MixinParticleEngine" -> true;
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
					case "net.irisshaders.iris.mixin.fabric.MixinParticleEngine" ->
						mixinMethodName.equals("iris$cancel");
					default -> false;
				};
			}
		});
		MixinCancellerRegistrar.register((targetClassNames, mixinClassName)
			-> switch (mixinClassName) {
			case "net.irisshaders.iris.mixin.forge.MixinLevelRenderer",
				 "net.irisshaders.iris.mixin.fabric.MixinLevelRenderer",
				 // disable this because our implementation is better
//				 "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
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

//			@Override
//			public boolean preTest(List<String> targetClassNames, String mixinClassName) {
//				return switch (mixinClassName) {
//					case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin",
//						 "net.irisshaders.iris.mixin.fabric.MixinParticleEngine",
//						 "net.irisshaders.iris.mixin.fabric.MixinLevelRenderer" -> true;
//					default -> false;
//				};
//			}

//			@Override
//			public boolean shouldCancelMethod(List<String> targetClassNames,
//											  String mixinClassName,
//											  List<String> targetMethodDescs,
//											  String mixinMethodName,
//											  String mixinMethodDesc) {
//				return switch (mixinClassName) {
//					case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin" ->
//						mixinMethodName.equals("shouldRenderParticle");
//					case "net.irisshaders.iris.mixin.fabric.MixinParticleEngine" ->
//						mixinMethodName.equals("iris$cancel");
//					default -> false;
//				};
//			}
//		MixinTargetsModifierRegistrar.register(new MixinTargetModifier() {
//			@Override
//			public String getMixinClassName() {
//				return "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinConcurrencyUnsafeParticles";
//			}
//
//			@Override
//			public List<String> getTargets(List<String> list) {
//				System.out.println("TargetModifier.apply");
//				for (String s : list) {
//					System.out.println(s);
//				}
//				List<String> collect = list.stream()
//					.filter(mixinClassName -> !"net.minecraft.client.particle.Particle".equals(mixinClassName))
//					.collect(Util.toMutableList());
//				collect.add("net.minecraft.client.particle.ItemPickupParticle");
//				System.out.println("TargetModifier.apply.collect");
//				for (String s : collect) {
//					System.out.println(s);
//				}
//				return collect;
//			}
//
//			@Override
//			public boolean shouldApplyMixin(String s) {
//				return true;
//			}
//		});
	}

	@Override
	public String getRefMapperConfig() {
		// this fixes the useless refmap (crash) on neoforge
		return ModListHelper.IS_FORGE ? null : "fabric-asyncparticles-common-refmap.json";
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncparticlesClient.class.getPackage().getName().length() +
											  ".mixin.".length();

	/// - mixin/fabric 包下位于根目录的mixin只在fabric环境下生效。除非另有说明，位于其他子目录的mixin在fabric或信雅互联环境下均生效
	/// - mixin/<mod_id>/fabric 包下的mixin只在fabric环境下生效，其他mixin在任何环境下生效
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
			case "fabric" -> {
				if (split.length == 2) {
					yield !ModListHelper.IS_FORGE;
				}
				yield switch (split[1]) {
					case "off_thread_access" -> !ModListHelper.IS_FORGE;
					case "particlerain_create" ->
						ModListHelper.FABRIC_PARTICLERAIN_LOADED && ModListHelper.CREATE_LOADED;
					case "particlerain" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED;
					case "effective" -> ModListHelper.FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> ModListHelper.FABRIC_EFFECTUAL_LOADED;
					case "particular" -> ModListHelper.FABRIC_PARTICULAR_LOADED;
					case "iris" -> ModListHelper.FABRIC_IRIS_LOADED;
					case "vulkanmod" -> ModListHelper.FABRIC_VULKAN_MOD_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "off_thread_access",
				 "tick",
				 "render" -> true;
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
			case "watut" -> ModListHelper.WATUT_LOADED;
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
