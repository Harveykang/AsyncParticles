package fun.qu_an.minecraft.asyncparticles.client.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;

public class ModListHelper {
	public static final boolean IS_FORGE = isForge();
	public static final boolean IS_CLIENT = isClient();
	public static final boolean FABRIC_API_LOADED = isModLoaded("fabric-api") || isModLoaded("fabric_api");
	public static final boolean CONNECTORMOD_LOADED = isModLoaded("connectormod");
	/* Valkyrien Skies */
	public static final boolean VS_LOADED = isModLoaded("valkyrienskies");
	public static final boolean FABRIC_VS_LOADED = isFabricModLoaded("valkyrienskies");
	public static final boolean FORGE_VS_LOADED = isForgeModLoaded("valkyrienskies");
	/* Sodium */
	public static final boolean SODIUM_LOADED = isModLoaded("sodium");
	public static final boolean FABRIC_SODIUM_LOADED = isFabricModLoaded("sodium");
	public static final boolean FORGE_SODIUM_LOADED = isForgeModLoaded("sodium");
	public static final boolean FORGE_EMBEDDIUM_LOADED = isForgeModLoaded("embeddium");
	/* IRIS */
	// What the hell of compatibility is this!
	public static final boolean IRIS_LIKE_LOADED = isModLoaded("iris") || isModLoaded("oculus");
	public static final boolean FABRIC_IRIS_LOADED = isFabricModLoaded("iris");
	public static final boolean FORGE_IRIS_LIKE_LOADED = isForgeModLoaded("iris") || isForgeModLoaded("oculus");
	public static final boolean FORGE_IRIS_LOADED = isForgeModLoaded("iris");
	public static final boolean FORGE_OCULUS_LOADED = isForgeModLoaded("oculus");
	/* Dummmmmmy */
	public static final boolean DUMMMMMMY_LOADED = isModLoaded("dummmmmmy");
	/* Effectual */
	public static final boolean FABRIC_EFFECTUAL_LOADED = isFabricModLoaded("effectual");
	/* Flerovium */
	public static final boolean FORGE_FLEROVIUM_LOADED = isForgeModLoaded("flerovium");
	/* Effective */
	public static final boolean FORGE_EFFECTIVE_LOADED = isForgeModLoaded("effective");
	public static final boolean FABRIC_EFFECTIVE_LOADED = isFabricModLoaded("effective");
	/* Particle Rain */
	public static final boolean PARTICLERAIN_LOADED = isModLoaded("particlerain");
	public static final boolean FABRIC_PARTICLERAIN_LOADED = isFabricModLoaded("particlerain");
	public static final boolean FORGE_PRETTY_RAIN_LOADED = isForgeModLoaded("particlerain") &&
														   versionCheck("particlerain", null, "1.999999");
	public static final boolean FORGE_PARTICLERAIN_LOADED = isForgeModLoaded("particlerain") &&
														   versionCheck("particlerain", "3.999999", null);
	/* Flywheel */
	public static final boolean FLYWHEEL_LOADED = isModLoaded("flywheel");
	/* Create */
	public static final boolean CREATE_LOADED = isModLoaded("create");
	public static final boolean FABRIC_CREATE_LOADED = isFabricModLoaded("create");
	public static final boolean FORGE_CREATE_LOADED = isForgeModLoaded("create");
	/* Tombstone */
	public static final boolean TOMBSTONE_LOADED = isModLoaded("tombstone");
	/* Hexcasting */
	public static final boolean HEXCASTING_LOADED = isModLoaded("hexcasting");
	/* Enhanced Block Entities */
//	public static final boolean ENHANCEDBLOCKENTITIES_LOADED = isModLoaded("enhancedblockentities");
	/* Particular */
	public static final boolean FABRIC_PARTICULAR_LOADED = isFabricModLoaded("particular");
	public static final boolean FORGE_PARTICULAR_LOADED = isForgeModLoaded("particular");
	/* Particle Core */
	public static final boolean PARTICLE_CORE_LOADED = isModLoaded("particle_core");
	/* Physics Mod */
	public static final boolean PHYSICSMOD_LOADED = isModLoaded("physicsmod");
	/* A Good Place */
	public static final boolean A_GOOD_PLACE_LOADED = isModLoaded("a_good_place");
	/* Modern UI */
	public static final boolean MODERN_UI_LOADED = isModLoaded("modernui");
	/* Subtle Effects */
	public static final boolean SUBTLE_EFFECTS_LOADED = isModLoaded("subtle_effects");
	public static final boolean FABRIC_SUBTLE_EFFECTS_LOADED = isFabricModLoaded("subtle_effects");
	public static final boolean FORGE_SUBTLE_EFFECTS_LOADED = isForgeModLoaded("subtle_effects");
	/* What Are They Up To */
	public static final boolean WATUT_LOADED = isModLoaded("watut");
	/* Simple Weather */
	public static final boolean FORGE_SIMPLE_WEATHER_LOADED = isForgeModLoaded("simple_weather");
	/* Vulkan Mod */
	public static final boolean VULKAN_MOD_LOADED = isModLoaded("vulkanmod");
	public static final boolean FABRIC_VULKAN_MOD_LOADED = isFabricModLoaded("vulkanmod");
	/* Lodestone */
	public static final boolean LODESTONE_LOADED = isModLoaded("lodestone");
	/* Cloth Config */
	public static final boolean CLOTH_CONFIG_LOADED = isModLoaded("cloth_config") || isModLoaded("cloth-config");
	/* Photon Editor */
	public static final boolean PHOTON_EDITOR_LOADED = isModLoaded("photon");
	/* Fluffy Fur */
	public static final boolean FLUFFY_FUR_LOADED = isModLoaded("fluffy_fur");
	public static final boolean FORGE_FLUFFY_FUR_LOADED = isForgeModLoaded("fluffy_fur");
	/* Wizards Reborn */
	public static final boolean FORGE_WIZARDS_REBORN_LOADED = isForgeModLoaded("wizards_reborn");
	/* Porting Lib Base */
	public static final boolean FABRIC_PORTING_LIB_BASE_LOADED = isFabricModLoaded("porting_lib_base");
	/* Loot Beams Up */
	public static final boolean FABRIC_LOOT_BEAMS_UP_LOADED = isFabricModLoaded("lootbeams");
	/* Coo Particles API */
	public static final boolean FABRIC_COO_PARTICLES_API_LOADED = isModLoaded("cooparticlesapi");
	/* Immediately Fast */
	public static final boolean IMMEDIATELY_FAST_LOADED = isModLoaded("immediatelyfast");
	/* More Culling */
	public static final boolean MORE_CULLING_LOADED = isModLoaded("moreculling");

	@ExpectPlatform
	private static boolean isForge() {
		throw new AssertionError();
	}

	@ExpectPlatform
	private static boolean isClient() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean isModLoaded(String modId) {
		ExceptionUtil.throwAssertionError();
		return false;
	}

	@ExpectPlatform
	public static boolean versionCheck(String modId, String minInclusive, String maxExclusive) {
		// Suppressing the ConstantValue check because this is a generated method.
		ExceptionUtil.throwAssertionError();
		return true;
	}

	@ExpectPlatform
	public static String versionToString(String modId) {
		throw new AssertionError();
	}

	public static boolean isForgeModLoaded(String modId) {
		return IS_FORGE && isModLoaded(modId);
	}

	public static boolean isFabricModLoaded(String modId) {
		return !IS_FORGE && isModLoaded(modId);
	}

	@ExpectPlatform
	public static boolean isDevelopmentEnvironment() {
		throw new AssertionError();
	}

	public static boolean classExists(String className) {
		return ModListHelper.class.getClassLoader().getResource(className.replace(".", "/") + ".class") != null;
	}
}
