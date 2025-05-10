package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.bawnorton.mixinsquared.ext.ExtensionRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.ExtensionMemberCancelApplication;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.MixinMemberCanceller;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.MixinMemberCancellerRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier.MixinClassAdjuster;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier.MixinClassAdjusterRegistrar;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

@PreLaunch
public class AsyncParticlesMixinPlugin implements IMixinConfigPlugin {
	static final ILogger LOGGER = MixinService.getService().getLogger("asyncparticles:plugin");

	@Override
	public void onLoad(String mixinPackage) {
		if (!IS_CLIENT) {
			return;
		}
		try {
			AsyncParticlesMixinConfig.load();
		} catch (IOException e) {
			throw new MixinError(e);
		}
		ExtensionRegistrar.register(new ExtensionMemberCancelApplication());

		AsyncParticlesMixinConfig.Mixin$Particle config = AsyncParticlesMixinConfig.config;
		MixinClassAdjusterRegistrar.register(new MixinClassAdjuster() {
			@Override
			public String getMixinClassName() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge." : "fabric.") +
					   "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticles_NoCulling";
			}

			@Override
			public List<String> getTargets(List<String> originalTargets) {
				return List.copyOf(config.getNoCulling());
			}

			@Override
			public String getRefMapperConfig() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge-" : "fabric-") +
					   "asyncparticles-common-refmap.json";
			}
		});
		MixinClassAdjusterRegistrar.register(new MixinClassAdjuster() {
			@Override
			public String getMixinClassName() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge." : "fabric.") +
					   "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticles_LightCacheNoRefresh";
			}

			@Override
			public List<String> getTargets(List<String> originalTargets) {
				ArrayList<String> list = new ArrayList<>(originalTargets);
				list.addAll(config.getNoLightCache());
				return list;
			}

			@Override
			public String getRefMapperConfig() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge-" : "fabric-") +
					   "asyncparticles-common-refmap.json";
			}
		});
		MixinClassAdjusterRegistrar.register(new MixinClassAdjuster() {
			@Override
			public String getMixinClassName() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge." : "fabric.") +
					   "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticles_LockProvider";
			}

			@Override
			public List<String> getTargets(List<String> originalTargets) {
				return List.copyOf(config.getLockProvider());
			}

			@Override
			public String getRefMapperConfig() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge-" : "fabric-") +
					   "asyncparticles-common-refmap.json";
			}
		});
		MixinClassAdjusterRegistrar.register(new MixinClassAdjuster() {
			@Override
			public String getMixinClassName() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge." : "fabric.") +
					   "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticles_LockRequired";
			}

			@Override
			public List<String> getTargets(List<String> originalTargets) {
				return List.copyOf(config.getLockRequired());
			}

			@Override
			public String getRefMapperConfig() {
				return (isDevelopmentEnvironment() ? "" : IS_FORGE ? "forge-" : "fabric-") +
					   "asyncparticles-common-refmap.json";
			}
		});
		MixinMemberCancellerRegistrar.register(new MixinMemberCanceller() {
			@Override
			public boolean preCancel(List<String> targetClassNames, String mixinClassName) {
				return switch (mixinClassName) {
					case "einstein.subtle_effects.mixin.client.particle.FabricParticleEngineMixin",
						 "einstein.subtle_effects.mixin.client.particle.ForgeParticleEngineMixin",
						 "team.teampotato.ruok.mixin.minecraft.ParticleManagerMixin",
						 "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
						 "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin" -> true;
					default -> false;
				};
			}

			@Override
			public boolean shouldCancelMethod(List<String> targetClassNames, String mixinClassName, List<String> targetMethodDescs, String mixinMethodName, String mixinMethodDesc) {
				return switch (mixinClassName) {
					case "einstein.subtle_effects.mixin.client.particle.FabricParticleEngineMixin",
						 "einstein.subtle_effects.mixin.client.particle.ForgeParticleEngineMixin" ->
						"shouldRenderParticle".equals(mixinMethodName);
					case "team.teampotato.ruok.mixin.minecraft.ParticleManagerMixin" -> "tick".equals(mixinMethodName);
					case "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin" ->
						"flerovium$getLightColorCached".equals(mixinMethodName);
					case "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin" ->
						"port_lib$addCustomRenderTypes".equals(mixinMethodName);
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
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncParticlesClient.class.getPackage().getName().length() +
											  ".mixin.".length();

	/// - mixin/fabric 包下位于根目录的mixin只在fabric环境下生效。除非另有说明，位于其他子目录的mixin在fabric或信雅互联环境下均生效
	/// - mixin/<mod_id>/fabric 包下的mixin只在fabric环境下生效，其他mixin在任何环境下生效
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
			case "fabric" -> {
				if (split.length == 2) {
					yield !IS_FORGE;
				}
				yield switch (split[1]) {
					case "particlerain_vs" -> FABRIC_PARTICLERAIN_LOADED && VS_LOADED;
					case "particlerain_create" -> FABRIC_PARTICLERAIN_LOADED && CREATE_LOADED;
					case "particlerain" -> FABRIC_PARTICLERAIN_LOADED;
					case "create_5" -> FABRIC_CREATE_LOADED && IS_LEGACY_CREATE;
					case "create_6" -> FABRIC_CREATE_LOADED && !IS_LEGACY_CREATE;
					case "sodium" -> FABRIC_SODIUM_LOADED;
					case "effective" -> FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> FABRIC_EFFECTUAL_LOADED;
					case "particular" -> FABRIC_PARTICULAR_LOADED;
					case "vulkanmod" -> FABRIC_VULKAN_MOD_LOADED;
					case "iris" -> FABRIC_IRIS_LOADED;
					case "iris_else" -> !IS_FORGE && !FABRIC_IRIS_LOADED;
					case "porting_lib_base" -> FABRIC_PORTING_LIB_BASE_LOADED;
					case "loot_beams_up" -> FABRIC_LOOT_BEAMS_UP_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "legacy" -> {
				if (split.length == 2) {
					throw new IllegalArgumentException("Unknown legacy mixin: " + mixinClassName);
				}
				yield switch (split[1]) {
					case "flywheel" -> FLYWHEEL_LOADED &&
									   versionCheck("flywheel", "0.6", "1.0");
					default -> throw new IllegalArgumentException("Unknown legacy mod mixin: " + mixinClassName);
				};
			}
			case "fake_renders",
				 "off_thread_access",
				 "tick",
				 "render" -> true;
			case "modernui" -> MODERN_UI_LOADED;
			case "vs2" -> VS_LOADED;
			case "vs2_create" -> VS_LOADED && CREATE_LOADED;
			case "create" -> CREATE_LOADED;
			case "iris_like" -> IRIS_LIKE_LOADED;
			case "flywheel" -> FLYWHEEL_LOADED &&
							   versionCheck("flywheel", "1.0", "2.0");
			case "particle_core" -> PARTICLE_CORE_LOADED;
			case "physicsmod" -> PHYSICSMOD_LOADED;
			case "physicsmod_create" -> PHYSICSMOD_LOADED && CREATE_LOADED;
			case "physicsmod_vs" -> PHYSICSMOD_LOADED && VS_LOADED;
			case "a_good_place" -> A_GOOD_PLACE_LOADED;
			case "subtle_effects" -> {
				if (split.length == 2) {
					yield SUBTLE_EFFECTS_LOADED;
				}
				yield switch (split[1]) {
					case "fabric" -> !IS_FORGE && FABRIC_SUBTLE_EFFECTS_LOADED;
					default -> SUBTLE_EFFECTS_LOADED;
				};
			}
			case "watut" -> WATUT_LOADED;
			case "lodestone" -> LODESTONE_LOADED;
			case "fabric_api" -> FABRIC_API_LOADED; // Includes Forge version
			case "cloth_config" -> CLOTH_CONFIG_LOADED;
			case "photon_editor" -> PHOTON_EDITOR_LOADED;
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
