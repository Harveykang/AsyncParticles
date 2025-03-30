package fun.qu_an.minecraft.asyncparticles.client.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.sinytra.connector.loader.ConnectorEarlyLoader;

public class ModListHelper {
	public static final boolean IS_FORGE = isForge();
	public static final boolean IS_CLIENT = isClient();
	public static final boolean FABRIC_API_LOADED = isModLoaded("fabric");
	public static final boolean CONNECTORMOD_LOADED = isModLoaded("connectormod");
	/* Valkyrien Skies */
	public static final boolean VS_LOADED = isModLoaded("valkyrienskies");
	public static final boolean FABRIC_VS_LOADED = isFabricModLoaded("valkyrienskies");
	public static final boolean FORGE_VS_LOADED = isForgeModLoaded("valkyrienskies");
	/* Sodium */
	public static final boolean SODIUM_LIKE_LOADED = isModLoaded("sodium") || isModLoaded("embeddium");
	public static final boolean SODIUM_LOADED = isModLoaded("sodium");
	public static final boolean FABRIC_SODIUM_LOADED = isFabricModLoaded("sodium");
	public static final boolean FORGE_EMBEDDIUM_LOADED = isForgeModLoaded("embeddium");
	public static final boolean FORGE_SODIUM_LOADED = isForgeModLoaded("sodium");
	/* IRIS */
	public static final boolean IRIS_LIKE_LOADED = isModLoaded("iris") || isModLoaded("oculus");
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
	public static final int FLYWHEEL_MAJOR_VERSION = versionMajor("flywheel");
	/* Create */
	public static final boolean CREATE_LOADED = isModLoaded("create");
	public static final int CREATE_MAJOR_VERSION = versionMajor("create");
	public static final boolean FABRIC_CREATE_LOADED = isFabricModLoaded("create");
	public static final boolean FORGE_CREATE_LOADED = isForgeModLoaded("create");
	/* Epic Fight */
	public static final boolean EPICFIGHT_LOADED = isModLoaded("epicfight");
	public static final boolean FORGE_EPICFIGHT_LOADED = isForgeModLoaded("epicfight");
	/* Epic ACG */
	public static final boolean FORGE_EPICACG_LOADED = isForgeModLoaded("epicacg");
	/* A Good Place */
	public static final boolean A_GOOD_PLACE_LOADED = isModLoaded("a_good_place");
	/* Gateways to Eternity */
	public static final boolean FORGE_GATEWAYS_LOADED = isForgeModLoaded("gateways");
	/* Tombstone */
	public static final boolean TOMBSTONE_LOADED = isModLoaded("tombstone");
	/* Particular */
	public static final boolean FABRIC_PARTICULAR_LOADED = isFabricModLoaded("particular");
	/* Particle Core */
	public static final boolean PARTICLE_CORE_LOADED = isModLoaded("particle_core");
	/* Physics Mod */
	public static final boolean PHYSICSMOD_LOADED = isModLoaded("physicsmod");
	/* Draconic Evolution */
	public static final boolean FORGE_DRACONIC_EVOLUTION_LOADED = isForgeModLoaded("draconicevolution");
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
	public static int versionMajor(String modId) {
		throw new AssertionError();
	}

	@SuppressWarnings("ConstantValue")
	public static boolean isForgeModLoaded(String modId) {
		return IS_FORGE && isModLoaded(modId) && (!CONNECTORMOD_LOADED || !ConnectorEarlyLoader.isConnectorMod(modId));
	}

	public static boolean isFabricModLoaded(String modId) {
		return IS_FORGE ? CONNECTORMOD_LOADED && ConnectorEarlyLoader.isConnectorMod(modId) : isModLoaded(modId);
	}

	@ExpectPlatform
	public static boolean isDevelopmentEnvironment() {
		throw new AssertionError();
	}
}
