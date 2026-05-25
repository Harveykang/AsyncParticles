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
		MixinClassAdjusterRegistrar.register(new AdjusterParticlesLockRequired());
		MixinClassAdjusterRegistrar.register(new AdjusterReplaceRandom());
		MixinClassAdjusterRegistrar.register(new AdjusterContraptionNoParticleCollision());
		MixinMemberCancellerRegistrar.register(new AsyncParticlesMixinMemberCanceller());
		MixinCancellerRegistrar.register(new AsyncParticlesMixinCanceller());
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

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
			throw new IllegalArgumentException("Unknown mixin: " + mixinClassName);
		}
		return switch (split[0]) {
			case "core" -> !"fabric".equals(split[1]) || !IS_FORGE;
			case "conditional" -> switch (split[1]) {
				case "MixinClassInstanceMultiMap" -> MixinConfigHelper.isSafeClassInstanceMultiMap();
				case "MixinLevelChunk_BlockEntityMap", "MixinLevelChunk_BlockEntityMap_Late" ->
					MixinConfigHelper.isSafeBlockEntityMap();
				case "MixinLegacyRandomSource" -> MixinConfigHelper.isSafeLegacyRandomSource();
				case "MixinParticleEngine_SplitTick" -> MixinConfigHelper.isParticleSplitTick();
				default -> true;
			};
			case "compat" -> switch (split[1]) {
				case "particlerain" -> PARTICLERAIN_LOADED;
				case "particlerain_vs" -> PARTICLERAIN_LOADED && VS_LOADED;
				case "particlerain_create" -> PARTICLERAIN_LOADED && CREATE_LOADED;
				case "modernui" -> MODERN_UI_LOADED;
				case "lambdynlights" -> LAMBDYNLIGHTS_LOADED;
				case "vs2" -> VS_LOADED;
				case "vs2_create" -> VS_LOADED && CREATE_LOADED;
				case "create" -> CREATE_LOADED; // 0.5.1~6.0.8
				case "iris_like" -> IRIS_LIKE_LOADED;
				case "flywheel" -> FLYWHEEL_LOADED &&
					versionCheck("flywheel", "1.0", "2.0");
				case "physicsmod" -> PHYSICSMOD_LOADED;
				case "physicsmod_create" -> PHYSICSMOD_LOADED && CREATE_LOADED;
				case "physicsmod_vs" -> PHYSICSMOD_LOADED && VS_LOADED;
				case "a_good_place" -> A_GOOD_PLACE_LOADED;
				case "watut" -> WATUT_LOADED && versionCheck("watut", "1.20.1-1.2.0", null);
				case "lodestone" -> LODESTONE_LOADED;
				case "fabric_api" -> FABRIC_API_LOADED; // Includes Forge version
				case "cloth_config" -> CLOTH_CONFIG_LOADED;
				case "photon_editor" -> PHOTON_EDITOR_LOADED;
				case "shimmer" -> SHIMMER_LOADED;
				case "immediatelyfast" -> IMMEDIATELY_FAST_LOADED;
				case "figura" -> FIGURA_LOADED;
				case "fabric" -> switch (split[2]) {
					case "effective" -> FABRIC_EFFECTIVE_LOADED;
					case "effectual" -> FABRIC_EFFECTUAL_LOADED;
					case "particular" -> FABRIC_PARTICULAR_LOADED;
					case "vulkanmod" -> FABRIC_VULKAN_MOD_LOADED;
					case "iris" -> FABRIC_IRIS_LOADED;
					case "iris_else" -> !IS_FORGE && !FABRIC_IRIS_LOADED;
					case "porting_lib_base" -> FABRIC_PORTING_LIB_BASE_LOADED;
					case "loot_beams_up" -> FABRIC_LOOT_BEAMS_UP_LOADED;
					case "sodium_extra" -> FABRIC_SODIUM_EXTRA_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric compat mixin: " + mixinClassName);
				};
				default -> throw new IllegalArgumentException("Unknown compat mixin: " + mixinClassName);
			};
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
