package fun.qu_an.minecraft.asyncparticles.client.coremod;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.spongepowered.asm.mixin.throwables.MixinError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class AsyncParticlesMixinConfig {
	public static final Path MIXIN_CONFIG_FILE = Path.of("config", "asyncparticles", "asyncparticles-mixin.properties");
	public static final int VERSION = 2;
	static String COMMENTS = """
		particle$noCulling: A comma-separated list of particle classes that should not be culled.
		particle$noLightCache: A comma-separated list of particle classes that should not use the light cache.
		particle$lockRequired: A comma-separated list of particle classes that require a spin lock.
		particle$lockProvider: A comma-separated list of particle classes that provide a spin lock.
		replaceRandom: A comma-separated list of classes that require multithreaded random sources.""";
	static final Mixin$Particle CONFIG;
	private static Mixin$Particle toSaveConfig;

	static {
		try {
			load();
		} catch (IOException e) {
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

		Mixin$Particle configObj = new Mixin$Particle();
		configObj.read(properties);
		configObj = upgrade(configObj.version, configObj);

		toSaveConfig = configObj;
		save(configObj);
	}

	@Contract
	private static Mixin$Particle upgrade(int ver, Mixin$Particle configObj) {
		if (VERSION != 2) {
			throw new RuntimeException("I forgot to update the upgrade method.");
		}
		return switch (ver) {
			case 2 -> configObj;
			default -> new Mixin$Particle();
		};
	}

	static void save() throws IOException {
		Mixin$Particle configObj = new Mixin$Particle();
		configObj.fold();
		save(configObj);
	}

	static void reset() throws IOException {
		Mixin$Particle configObj = new Mixin$Particle();
		configObj.flat();
		save(configObj);
	}

	static void save(Mixin$Particle configObj) throws IOException {
		configObj.version = VERSION;
		Properties properties = new Properties();
		configObj.write(properties);
		try (OutputStream os = Files.newOutputStream(MIXIN_CONFIG_FILE)) {
			properties.store(os, COMMENTS);
		}
	}

	static Mixin$Particle getToSaveConfig() {
		return toSaveConfig;
	}

	static class Mixin$Particle {
		private int version = 0;
		private boolean safeLegacyRandomSource = false;
		private Set<String> noCulling = new LinkedHashSet<>();

		{
//			noCulling.add("com.lowdragmc.photon.client.gameobject.FXObject");
		}

		private Set<String> noLightCache = new LinkedHashSet<>();

		{
//			noLightCache.add("dev.shadowsoffire.gateways.client.GatewayParticle");
			noLightCache.add("com.chailotl.particular.particles.FireflyParticle");
//			noLightCache.add("com.lowdragmc.photon.client.gameobject.FXObject");
			noLightCache.add("net.diebuddies.minecraft.weather.WeatherParticle");
//			noLightCache.add("cn.coostack.cooparticlesapi.particles.ControlableParticle");
		}

		private Set<String> lockProvider = new LinkedHashSet<>();

		{
//			lockProvider.add("yesman.epicfight.client.particle.TrailParticle");
//			lockProvider.add("com.dfdyz.epicacg.client.particle.BloomTrailParticle");
//			lockProvider.add("com.brandon3055.draconicevolution.client.render.effect.ExplosionFX");
//			lockProvider.add("com.brandon3055.draconicevolution.client.render.effect.CrystalFXWireless");
//			lockProvider.add("com.lowdragmc.photon.client.gameobject.FXObject");
		}

		private Set<String> lockRequired = new LinkedHashSet<>();

		{
//			lockRequired.add("yesman.epicfight.client.particle.TrailParticle");
//			lockRequired.add("com.dfdyz.epicacg.client.particle.BloomTrailParticle");
//			lockRequired.add("com.brandon3055.draconicevolution.client.render.effect.ExplosionFX");
//			lockRequired.add("com.brandon3055.draconicevolution.client.render.effect.CrystalFXWireless");
//			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.Emitter");
//			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter");
//			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.beam.BeamEmitter");
//			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.trail.TrailEmitter");
		}

		private Set<String> replaceRandom = new LinkedHashSet<>();

		{
//			replaceRandom.add("appeng.client.render.effects.LightningArcFX");
//			replaceRandom.add("appeng.client.render.effects.LightningFX");
		}

		private void fold() {
			assertNotGlobal();
			safeLegacyRandomSource = toSaveConfig.safeLegacyRandomSource;
			noCulling = toSaveConfig.noCulling;
			noLightCache = toSaveConfig.noLightCache;
			lockProvider = toSaveConfig.lockProvider;
			lockRequired = toSaveConfig.lockRequired;
			replaceRandom = toSaveConfig.replaceRandom;
		}

		private void read(Properties properties) {
			assertNotGlobal();
			try {
				version = Integer.parseInt(properties.getProperty("version"));
			} catch (NumberFormatException ignored) {
			}
			Mixin$Particle defaultConfig = new Mixin$Particle();
			safeLegacyRandomSource =
				getBoolean(properties, "safeLegacyRandomSource", defaultConfig.safeLegacyRandomSource);
			noCulling = getSet(properties, "particle$noCulling", defaultConfig.noCulling);
			noLightCache = getSet(properties, "particle$noLightCache", defaultConfig.noLightCache);
			lockProvider = getSet(properties, "particle$lockProvider", defaultConfig.lockProvider);
			lockRequired = getSet(properties, "particle$lockRequired", defaultConfig.lockRequired);
			replaceRandom = getSet(properties, "replaceRandom", defaultConfig.replaceRandom);
		}

		void flat() {
			toSaveConfig = this;
		}

		private void write(Properties properties) {
			properties.setProperty("version", Integer.toString(version));
			properties.setProperty("safeLegacyRandomSource", Boolean.toString(safeLegacyRandomSource));
			properties.setProperty("particle$noCulling", String.join(",", noCulling));
			properties.setProperty("particle$noLightCache", String.join(",", noLightCache));
			properties.setProperty("particle$lockProvider", String.join(",", lockProvider));
			properties.setProperty("particle$lockRequired", String.join(",", lockRequired));
			properties.setProperty("replaceRandom", String.join(",", replaceRandom));
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
			return Collections.unmodifiableSet(noCulling);
		}

		void setNoCulling(Set<String> noCulling) {
			assertNotGlobal();
			this.noCulling = noCulling;
		}

		@Unmodifiable
		Set<String> getNoLightCache() {
			return Collections.unmodifiableSet(noLightCache);
		}

		void setNoLightCache(Set<String> noLightCache) {
			assertNotGlobal();
			this.noLightCache = noLightCache;
		}

		@Unmodifiable
		Set<String> getLockProvider() {
			return Collections.unmodifiableSet(lockProvider);
		}

		void setLockProvider(Set<String> lockProvider) {
			assertNotGlobal();
			this.lockProvider = lockProvider;
		}

		@Unmodifiable
		Set<String> getLockRequired() {
			return Collections.unmodifiableSet(lockRequired);
		}

		void setLockRequired(Set<String> lockRequired) {
			assertNotGlobal();
			this.lockRequired = lockRequired;
		}

		@Unmodifiable
		Set<String>  getReplaceRandom() {
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
	}
}
