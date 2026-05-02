package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.bawnorton.mixinsquared.ext.ExtensionRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters.*;
import fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers.AsyncParticlesMixinCanceller;
import fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers.AsyncParticlesMixinMemberCanceller;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjusterRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.ExtensionMemberCancelApplication;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.MixinMemberCancellerRegistrar;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

public class AsyncParticlesMixinPlugin implements IMixinConfigPlugin {
	static final ILogger LOGGER = MixinService.getService().getLogger("asyncparticles:plugin");

	@Override
	public void onLoad(String mixinPackage) {
		if (!IS_CLIENT) {
			return;
		}
		ExtensionRegistrar.register(new ExtensionMemberCancelApplication());

		MixinClassAdjusterRegistrar.register(new AdjusterParticlesNoCulling());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesNoLightCache());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesLockProvider());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesLockRequired_Tick());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesLockRequired_Extract());
		MixinClassAdjusterRegistrar.register(new AdjusterReplaceRandom());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesAsyncTickableGroup());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesModifyTheFromParticleMethod());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesTestAliveBeforeRender());
		MixinMemberCancellerRegistrar.register(new AsyncParticlesMixinMemberCanceller());
		MixinCancellerRegistrar.register(new AsyncParticlesMixinCanceller());
	}

	@Override
	public String getRefMapperConfig() {
		return null; // see neoforge/build.gradle:processResources:filesMatching('asyncparticles-common.mixins.json')
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncParticlesClient.class.getPackage().getName().length() +
											  ".mixin.".length();

	/// - mixins located in `mixin/fabric` or `mixin/<mod_id>/fabric` package only take effect on fabric.
	/// - mixins located in `mixin/fabric/<mod_id>` take effect on fabric or Sinytra Connector.
	/// - others take effect on any platform.
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
			case "conditional" -> switch (split[1]) {
				case "MixinClassInstanceMultiMap" -> MixinConfigHelper.isSafeClassInstanceMultiMap();
				case "MixinLevelChunk_BlockEntityMap" -> MixinConfigHelper.isSafeBlockEntityMap();
				case "MixinLegacyRandomSource" -> MixinConfigHelper.isSafeLegacyRandomSource();
				default -> true;
			};
			case "fabric" -> {
				if (split.length == 2) {
					yield !IS_FORGE;
				}
				yield switch (split[1]) {
					case "particlerain_3" -> FABRIC_PARTICLERAIN_LOADED &&
											 versionCheck("particlerain", null, "3.999999");
					case "effectual" -> FABRIC_EFFECTUAL_LOADED;
					case "particular" -> FABRIC_PARTICULAR_LOADED;
					case "vulkanmod" -> FABRIC_VULKAN_MOD_LOADED;
					case "iris" -> FABRIC_IRIS_LOADED;
					case "porting_lib_base" -> FABRIC_PORTING_LIB_BASE_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "core", "off_thread_access" -> true;
			case "compat" -> {
				if (split.length == 2) {
					throw new IllegalArgumentException("Unknown compat mixin: " + mixinClassName);
				}
				yield switch (split[1]) {
					case "modernui" -> MODERN_UI_LOADED;
					case "sodium" -> SODIUM_LOADED;
					case "sodium_0_6" -> SODIUM_LOADED && versionCheck("sodium", "0.6", "0.7");
					case "sodium_0_7" -> SODIUM_LOADED && versionCheck("sodium", "0.6.999999", "0.8");
					case "sodium_extra" -> SODIUM_EXTRA_LOADED;
					case "iris_like" -> IRIS_LIKE_LOADED;
					case "a_good_place" -> A_GOOD_PLACE_LOADED;
					case "watut" -> WATUT_LOADED;
					case "physicsmod" -> PHYSICSMOD_LOADED;
					case "cloth_config" -> CLOTH_CONFIG_LOADED;
					case "immediatelyfast" -> IMMEDIATELY_FAST_LOADED;
					case "figura" -> FIGURA_LOADED;
					default -> throw new IllegalStateException("Unknown compat mixin: " + mixinClassName);
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
