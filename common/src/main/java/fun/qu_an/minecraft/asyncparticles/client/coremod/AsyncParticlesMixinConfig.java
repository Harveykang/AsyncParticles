package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.spongepowered.asm.mixin.throwables.MixinError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinPlugin.LOGGER;

public class AsyncParticlesMixinConfig {
	public static final Path MIXIN_CONFIG_FILE = Path.of("config", AsyncParticlesClient.MOD_ID, AsyncParticlesClient.MOD_ID + "-mixin.properties");
	public static final int VERSION = 2;
	static String COMMENTS = """
		safeBlockEntityMap: Boolean. Make 'LevelChunk#blockEntities' thread-safe.
		safeClassInstanceMultiMap: Boolean. Make 'ClassInstanceMultiMap' thread-safe.
		safeLegacyRandomSource: Boolean. Make LegacyRandomSource thread-safe.
		particle$splitTick: Boolean. Enable recursive particle tick.
		particle$noCulling: A comma-separated list of classes extending 'Particle' that should not be culled.
		particle$noLightCache: A comma-separated list of classes extending 'Particle' that should not use the light cache.
		particle$lockRequired: A comma-separated list of classes extending 'Particle' that require a spin lock.
		particle$lockProvider: A comma-separated list of classes extending 'Particle' that provide a spin lock.
		replaceRandom: A comma-separated list of classes that require multithreaded random sources.
		create$contraptionNoParticleCollision: A comma-separated list of classes extending 'AbstractContraptionEntity' that should not collide with particles.
		""";
	static final MixinConfigObj CONFIG;
	private static MixinConfigObj toSaveConfig;

	static {
		LOGGER.debug("AsyncParticlesMixinConfig initialized.");
		try {
			load();
		} catch (Throwable e) {
			throw new MixinError(e);
		}
		CONFIG = toSaveConfig;
	}

	static void load() throws IOException {
		if (!Files.exists(MIXIN_CONFIG_FILE)) {
			Files.createDirectories(MIXIN_CONFIG_FILE.getParent());
			Files.createFile(MIXIN_CONFIG_FILE);
			reset();
			return;
		}

		Properties properties = new Properties();
		try (InputStream is = Files.newInputStream(MIXIN_CONFIG_FILE)) {
			properties.load(is);
		}

		MixinConfigObj configObj = new MixinConfigObj();
		configObj.read(properties);
		configObj = upgrade(configObj.version, configObj);

		configObj.flat();
		save(configObj);
	}

	@Contract
	private static MixinConfigObj upgrade(int ver, MixinConfigObj configObj) {
		if (VERSION != 2) {
			throw new RuntimeException("I forgot to update the upgrade method.");
		}
		return switch (ver) {
			case 2 -> configObj;
			default -> new MixinConfigObj();
		};
	}

	static void save() throws IOException {
		MixinConfigObj configObj = new MixinConfigObj();
		configObj.fold();
		save(configObj);
	}

	static void reset() throws IOException {
		MixinConfigObj configObj = new MixinConfigObj();
		configObj.flat();
		save(configObj);
	}

	static void save(MixinConfigObj configObj) throws IOException {
		configObj.version = VERSION;
		Properties properties = new Properties();
		configObj.write(properties);
		try (OutputStream os = Files.newOutputStream(MIXIN_CONFIG_FILE)) {
			properties.store(os, COMMENTS);
		}
	}

	static MixinConfigObj getToSaveConfig() {
		return toSaveConfig;
	}

	static class MixinConfigObj {
		private int version = 0;
		private boolean particle$splitTick = false;
		private boolean safeClassInstanceMultiMap = (IRONS_SPELLBOOKS_LOADED && IRONS_SPELLBOOKS_LESS_THAN_3_13_0) ||
													MAKE_BUBBLES_POP_LOADED;
		private boolean safeBlockEntityMap = false;
		private boolean safeLegacyRandomSource = false;
		private Set<String> particle$noCulling = new LinkedHashSet<>();

		{
			particle$noCulling.add("com.lowdragmc.photon.client.gameobject.FXObject");
		}

		private Set<String> particle$noLightCache = new LinkedHashSet<>();

		{
			particle$noLightCache.add("dev.shadowsoffire.gateways.client.GatewayParticle");
			particle$noLightCache.add("com.chailotl.particular.particles.FireflyParticle");
			particle$noLightCache.add("com.lowdragmc.photon.client.gameobject.FXObject");
			particle$noLightCache.add("net.diebuddies.minecraft.weather.WeatherParticle");
		}

		private Set<String> particle$lockProvider = new LinkedHashSet<>();

		{
			particle$lockProvider.add("yesman.epicfight.client.particle.TrailParticle");
			particle$lockProvider.add("yesman.epicfight.client.particle.AbstractTrailParticle");
			particle$lockProvider.add("com.lowdragmc.photon.client.gameobject.FXObject");
		}

		private Set<String> particle$lockRequired = new LinkedHashSet<>();

		{
			particle$lockRequired.add("yesman.epicfight.client.particle.TrailParticle");
			particle$lockRequired.add("yesman.epicfight.client.particle.AbstractTrailParticle");
			particle$lockRequired.add("com.lowdragmc.photon.client.gameobject.FXObject");
		}

		private Set<String> replaceRandom = new LinkedHashSet<>();

		{
			replaceRandom.add("appeng.client.render.effects.LightningArcFX");
			replaceRandom.add("appeng.client.render.effects.LightningFX");
			replaceRandom.add("de.cheaterpaul.fallingleaves.util.LeafUtil");
		}

		private Set<String> create$contraptionNoParticleCollision = new LinkedHashSet<>();

		{
			create$contraptionNoParticleCollision.add("rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity");
			create$contraptionNoParticleCollision.add("rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption");
		}

		private void fold() {
			assertNotGlobal();
			particle$splitTick = toSaveConfig.particle$splitTick;
			safeClassInstanceMultiMap = toSaveConfig.safeClassInstanceMultiMap;
			safeBlockEntityMap = toSaveConfig.safeBlockEntityMap;
			safeLegacyRandomSource = toSaveConfig.safeLegacyRandomSource;
			particle$noCulling = toSaveConfig.particle$noCulling;
			particle$noLightCache = toSaveConfig.particle$noLightCache;
			particle$lockProvider = toSaveConfig.particle$lockProvider;
			particle$lockRequired = toSaveConfig.particle$lockRequired;
			replaceRandom = toSaveConfig.replaceRandom;
			create$contraptionNoParticleCollision = toSaveConfig.create$contraptionNoParticleCollision;
		}

		private void read(Properties properties) {
			assertNotGlobal();
			try {
				version = Integer.parseInt(properties.getProperty("version"));
			} catch (NumberFormatException ignored) {
			}
			MixinConfigObj defaultConfig = new MixinConfigObj();
			particle$splitTick = getBoolean(properties, "particle$splitTick", defaultConfig.particle$splitTick);
			safeClassInstanceMultiMap = (IRONS_SPELLBOOKS_LOADED && IRONS_SPELLBOOKS_LESS_THAN_3_13_0) ||
										MAKE_BUBBLES_POP_LOADED ||
										getBoolean(properties, "safeClassInstanceMultiMap", defaultConfig.safeClassInstanceMultiMap);
			safeBlockEntityMap = getBoolean(properties, "safeBlockEntityMap", defaultConfig.safeBlockEntityMap);
			safeLegacyRandomSource = getBoolean(properties, "safeLegacyRandomSource", defaultConfig.safeLegacyRandomSource);
			particle$noCulling = getSet(properties, "particle$noCulling", defaultConfig.particle$noCulling);
			particle$noLightCache = getSet(properties, "particle$noLightCache", defaultConfig.particle$noLightCache);
			particle$lockProvider = getSet(properties, "particle$lockProvider", defaultConfig.particle$lockProvider);
			particle$lockRequired = getSet(properties, "particle$lockRequired", defaultConfig.particle$lockRequired);
			replaceRandom = getSet(properties, "replaceRandom", defaultConfig.replaceRandom);
			create$contraptionNoParticleCollision = getSet(properties, "create$contraptionNoParticleCollision", defaultConfig.create$contraptionNoParticleCollision);
		}

		void flat() {
			toSaveConfig = this;
		}

		private void write(Properties properties) {
			properties.setProperty("version", Integer.toString(version));
			properties.setProperty("particle$splitTick", Boolean.toString(particle$splitTick));
			properties.setProperty("safeClassInstanceMultiMap", Boolean.toString(safeClassInstanceMultiMap));
			properties.setProperty("safeBlockEntityMap", Boolean.toString(safeBlockEntityMap));
			properties.setProperty("safeLegacyRandomSource", Boolean.toString(safeLegacyRandomSource));
			properties.setProperty("particle$noCulling", String.join(",", particle$noCulling));
			properties.setProperty("particle$noLightCache", String.join(",", particle$noLightCache));
			properties.setProperty("particle$lockProvider", String.join(",", particle$lockProvider));
			properties.setProperty("particle$lockRequired", String.join(",", particle$lockRequired));
			properties.setProperty("replaceRandom", String.join(",", replaceRandom));
			properties.setProperty("create$contraptionNoParticleCollision", String.join(",", create$contraptionNoParticleCollision));
		}

		private static Set<String> getSet(Properties properties, String key, Set<String> defaultValue) {
			String value = properties.getProperty(key);
			if (value == null) {
				return defaultValue;
			}
			value = value.replaceAll("[\\s\\u0085\\u2028\\u2029]", "");
			if (value.endsWith(",")) {
				value = value.substring(0, value.length() - 1);
			}
			String[] split = value.split(",");
			Set<String> set = new LinkedHashSet<>(defaultValue);
			for (String s : split) {
				if (s.isEmpty()) {
					continue;
				}
				set.add(s);
			}
			return Collections.unmodifiableSet(set);
		}

		private static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
			String bool = properties.getProperty(key);
			if (bool == null) {
				return defaultValue;
			}
			return Boolean.parseBoolean(bool);
		}

		private void assertNotGlobal() {
			if (this == toSaveConfig || this == CONFIG) {
				throw new IllegalStateException("Cannot modify global config object");
			}
		}

		@Unmodifiable
		Set<String> getNoCulling() {
			return Collections.unmodifiableSet(particle$noCulling);
		}

		void setNoCulling(Set<String> noCulling) {
			assertNotGlobal();
			this.particle$noCulling = noCulling;
		}

		@Unmodifiable
		Set<String> getNoLightCache() {
			return Collections.unmodifiableSet(particle$noLightCache);
		}

		void setNoLightCache(Set<String> noLightCache) {
			assertNotGlobal();
			this.particle$noLightCache = noLightCache;
		}

		@Unmodifiable
		Set<String> getLockProvider() {
			return Collections.unmodifiableSet(particle$lockProvider);
		}

		void setLockProvider(Set<String> lockProvider) {
			assertNotGlobal();
			this.particle$lockProvider = lockProvider;
		}

		@Unmodifiable
		Set<String> getLockRequired() {
			return Collections.unmodifiableSet(particle$lockRequired);
		}

		void setLockRequired(Set<String> lockRequired) {
			assertNotGlobal();
			this.particle$lockRequired = lockRequired;
		}

		@Unmodifiable
		Set<String> getReplaceRandom() {
			return Collections.unmodifiableSet(replaceRandom);
		}

		void setReplaceRandom(Set<String> replaceRandom) {
			assertNotGlobal();
			this.replaceRandom = replaceRandom;
		}

		void setSafeLegacyRandomSource(boolean safeLegacyRandomSource) {
			assertNotGlobal();
			this.safeLegacyRandomSource = safeLegacyRandomSource;
		}

		public boolean isSafeLegacyRandomSource() {
			return safeLegacyRandomSource;
		}

		public boolean isSafeClassInstanceMultiMap() {
			return safeClassInstanceMultiMap;
		}

		public void setSafeClassInstanceMultiMap(boolean safeClassInstanceMultiMap) {
			assertNotGlobal();
			this.safeClassInstanceMultiMap = IRONS_SPELLBOOKS_LOADED || MAKE_BUBBLES_POP_LOADED ||
											 safeClassInstanceMultiMap;
		}

		public boolean isSafeBlockEntityMap() {
			return safeBlockEntityMap;
		}

		public void setSafeBlockEntityMap(boolean safeBlockEntityMap) {
			assertNotGlobal();
			this.safeBlockEntityMap = safeBlockEntityMap;
		}

		@Unmodifiable
		Set<String> getContraptionNoParticleCollision() {
			return Collections.unmodifiableSet(create$contraptionNoParticleCollision);
		}

		void setContraptionNoParticleCollision(Collection<String> contraptionNoParticleCollision) {
			assertNotGlobal();
			this.create$contraptionNoParticleCollision = new LinkedHashSet<>(contraptionNoParticleCollision);
		}

		public boolean isParticleSplitTick() {
			return particle$splitTick;
		}

		void setParticleSplitTick(boolean splitTick) {
			assertNotGlobal();
			this.particle$splitTick = splitTick;
		}
	}
}
