package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.bawnorton.mixinsquared.ext.ExtensionRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters.AdjusterParticlesLockProvider;
import fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters.AdjusterParticlesLockRequired;
import fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters.AdjusterParticlesNoCulling;
import fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters.AdjusterParticlesNoLightCache;
import fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers.AsyncParticlesMixinMemberCanceller;
import fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers.AsyncParticlesMixinCanceller;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.ExtensionMemberCancelApplication;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.MixinMemberCancellerRegistrar;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier.MixinClassAdjusterRegistrar;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.FABRIC_LOOT_BEAMS_UP_LOADED;

public class AsyncParticlesMixinPlugin implements IMixinConfigPlugin {
	static final ILogger LOGGER = MixinService.getService().getLogger("asyncparticles:plugin");

	@Override
	public void onLoad(String mixinPackage) {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		ExtensionRegistrar.register(new ExtensionMemberCancelApplication());

		MixinClassAdjusterRegistrar.register(new AdjusterParticlesNoCulling());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesNoLightCache());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesLockProvider());
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesLockRequired());
		MixinMemberCancellerRegistrar.register(new AsyncParticlesMixinMemberCanceller());
		MixinCancellerRegistrar.register(new AsyncParticlesMixinCanceller());
	}

	@Override
	public String getRefMapperConfig() {
		// this fixes the useless refmap (crash) on neoforge
		return ModListHelper.IS_FORGE ? null : "fabric-asyncparticles-common-refmap.json";
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncParticlesClient.class.getPackage().getName().length() +
											  ".mixin.".length();

	/// - mixins located in `mixin/fabric` or `mixin/<mod_id>/fabric` package only take effect in fabric.
	/// - mixins located in `mixin/fabric/<mod_id>` take effect in fabric or Sinytra Connector.
	/// - others take effect in any environment.
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
					case "particlerain_create" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED &&
												  ModListHelper.CREATE_LOADED;
					case "particlerain" -> ModListHelper.FABRIC_PARTICLERAIN_LOADED;
					case "create" -> ModListHelper.FABRIC_CREATE_LOADED;
					case "effective" -> ModListHelper.FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> ModListHelper.FABRIC_EFFECTUAL_LOADED;
					case "particular" -> ModListHelper.FABRIC_PARTICULAR_LOADED;
					case "vulkanmod" -> ModListHelper.FABRIC_VULKAN_MOD_LOADED;
					case "iris" -> FABRIC_IRIS_LOADED;
					case "iris_else" -> !IS_FORGE && !FABRIC_IRIS_LOADED;
					case "porting_lib_base" -> FABRIC_PORTING_LIB_BASE_LOADED;
					case "loot_beams_up" -> FABRIC_LOOT_BEAMS_UP_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "fake_renders",
				 "off_thread_access",
				 "tick",
				 "render" -> true;
			case "modernui" -> ModListHelper.MODERN_UI_LOADED;
			case "create" -> ModListHelper.CREATE_LOADED;
//			case "sodium_0_6" -> ModListHelper.SODIUM_LOADED
//								 && ModListHelper.versionCheck("sodium", "0.6", "0.7");
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
			case "physicsmod" -> ModListHelper.PHYSICSMOD_LOADED;
			case "physicsmod_create" -> ModListHelper.PHYSICSMOD_LOADED && ModListHelper.CREATE_LOADED;
			case "lodestone" -> ModListHelper.LODESTONE_LOADED;
			case "cloth_config" -> ModListHelper.CLOTH_CONFIG_LOADED;
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
