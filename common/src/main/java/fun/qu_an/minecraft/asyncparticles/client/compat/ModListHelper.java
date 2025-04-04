package fun.qu_an.minecraft.asyncparticles.client.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.jetbrains.annotations.Nullable;
import org.sinytra.connector.ConnectorEarlyLoader;

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
	public static final boolean EMBEDDIUM_LOADED = isModLoaded("embeddium");
	/* IRIS */
	public static final boolean IRIS_LIKE_LOADED = isModLoaded("iris") || isModLoaded("oculus");
	public static final boolean FABRIC_IRIS_LOADED = isFabricModLoaded("iris");
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
	public static final boolean FORGE_PARTICLERAIN_LOADED = isForgeModLoaded("particlerain");
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
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean versionCheck(String modId, String minInclusive, String maxExclusive) {
		// Suppressing the ConstantValue check because this is a generated method.
		throwAssertionError();
		return true;
	}

	private static void throwAssertionError() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static String versionToString(String modId) {
		throw new AssertionError();
	}

	@SuppressWarnings("ConstantValue")
	public static boolean isForgeModLoaded(String modId) {
		return IS_FORGE && isModLoaded(modId) && (!CONNECTORMOD_LOADED || !ConnectorEarlyLoader.isConnectorMod(modId));
	}

	public static boolean isFabricModLoaded(String modId) {
		return IS_FORGE ? CONNECTORMOD_LOADED && ConnectorEarlyLoader.isConnectorMod(modId) : isModLoaded(modId);
	}

	@Nullable
	public static Class<?> getClass(String className) {
		try {
			return Class.forName(className, false, ModListHelper.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
