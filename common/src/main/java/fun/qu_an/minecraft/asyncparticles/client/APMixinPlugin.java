package fun.qu_an.minecraft.asyncparticles.client;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.bawnorton.mixinsquared.ext.ExtensionRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin_extension.ExtensionMemberCancelApplication;
import fun.qu_an.minecraft.asyncparticles.client.mixin_extension.MixinMemberCanceller;
import fun.qu_an.minecraft.asyncparticles.client.mixin_extension.MixinMemberCancellerRegistrar;
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
		ExtensionRegistrar.register(new ExtensionMemberCancelApplication());
		MixinMemberCancellerRegistrar.register(new MixinMemberCanceller() {
			@Override
			public boolean preCancel(List<String> targetClassNames, String mixinClassName) {
				return switch (mixinClassName) {
					case "einstein.subtle_effects.mixin.client.particle.FabricParticleEngineMixin",
						 "einstein.subtle_effects.mixin.client.particle.ForgeParticleEngineMixin",
						 "team.teampotato.ruok.mixin.minecraft.ParticleManagerMixin",
						 "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin" -> true;
					default -> false;
				};
			}

			@Override
			public boolean shouldCancelMethod(List<String> targetClassNames, String mixinClassName, List<String> targetMethodDescs, String mixinMethodName, String mixinMethodDesc) {
				return switch (mixinClassName) {
					case "einstein.subtle_effects.mixin.client.particle.FabricParticleEngineMixin",
						 "einstein.subtle_effects.mixin.client.particle.ForgeParticleEngineMixin" ->
						"shouldRenderParticle".equals(mixinMethodName);
					case "team.teampotato.ruok.mixin.minecraft.ParticleManagerMixin" ->
						"tick".equals(mixinMethodName);
					case "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin" ->
						"flerovium$getLightColorCached".equals(mixinMethodName);
					default -> false;
				};
			}

			@Override
			public boolean shouldCancelField(List<String> targetClassNames, String mixinClassName, String mixinFieldName, String mixinFieldDesc) {
				return switch (mixinClassName) {
					case "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin" ->
						"flerovium$lastTick".equals(mixinFieldName) ||
						"flerovium$cachedLight".equals(mixinFieldName);
					default -> false;
				};
			}
		});
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
//			, TODO: 这里处理一下
//				 "net.diebuddies.mixins.ocean.MixinParticleEngine"
				-> true;
			default -> false;
		});
//		MixinAnnotationAdjusterRegistrar.register((List<String> targetClassNames,
//												   String mixinClassName,
//												   MethodNode handlerNode,
//												   AdjustableAnnotationNode annotationNode) -> {
//			return annotationNode;
//		});
	}

	@Override
	public String getRefMapperConfig() {
		return null;
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
					case "particlerain_vs" ->
						ModListHelper.FABRIC_PARTICLERAIN_LOADED && ModListHelper.FABRIC_VS_LOADED;
					case "particlerain_create" ->
						ModListHelper.FABRIC_PARTICLERAIN_LOADED && ModListHelper.FABRIC_CREATE_LOADED;
					case "particlerain" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED;
					case "create_5" -> ModListHelper.FABRIC_CREATE_LOADED && ModListHelper.IS_LEGACY_CREATE;
					case "create_6" -> ModListHelper.FABRIC_CREATE_LOADED && !ModListHelper.IS_LEGACY_CREATE;
					case "sodium" -> ModListHelper.FABRIC_SODIUM_LOADED;
					case "effective" -> ModListHelper.FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> ModListHelper.FABRIC_EFFECTUAL_LOADED;
					case "particular" -> ModListHelper.FABRIC_PARTICULAR_LOADED;
					case "vulkanmod" -> ModListHelper.FABRIC_VULKAN_MOD_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "legacy" -> {
				if (split.length == 2) {
					throw new IllegalArgumentException("Unknown legacy mixin: " + mixinClassName);
				}
				yield switch (split[1]) {
					case "flywheel" -> ModListHelper.FLYWHEEL_LOADED &&
									   ModListHelper.versionCheck("flywheel", "0.6", "1.0");
					default -> throw new IllegalArgumentException("Unknown legacy mod mixin: " + mixinClassName);
				};
			}
			case "fake_renders",
				 "off_thread_access" -> true;
			case "modernui" -> ModListHelper.MODERN_UI_LOADED;
			case "vs2" -> ModListHelper.VS_LOADED;
			case "create" -> ModListHelper.CREATE_LOADED;
			case "iris_like" -> ModListHelper.IRIS_LIKE_LOADED;
			case "flywheel" -> ModListHelper.FLYWHEEL_LOADED &&
							   ModListHelper.versionCheck("flywheel", "1.0", "2.0");
			case "particle_core" -> ModListHelper.PARTICLE_CORE_LOADED;
			case "physicsmod" -> ModListHelper.PHYSICSMOD_LOADED;
			case "physicsmod_create" -> ModListHelper.PHYSICSMOD_LOADED && ModListHelper.CREATE_LOADED;
			case "physicsmod_vs" -> ModListHelper.PHYSICSMOD_LOADED && ModListHelper.VS_LOADED;
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
