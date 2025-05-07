package fun.qu_an.minecraft.asyncparticles.client.coremod;

import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

@PreLaunch
public class AsyncParticlesMixinConfig {
	public static final Path MIXIN_CONFIG_FILE = Path.of("config", "asyncparticles", "asyncparticles-mixin.properties");
	static String COMMENTS = """
		particle$noCulling: comma-separated list of particle classes that should not be culled.
		particle$noLightCache: comma-separated list of particle classes that should not use the light cache.
		particle$lockRequired: comma-separated list of particle classes that require a spin lock.""";
	static Mixin$Particle config = new Mixin$Particle();

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
		save(configObj);
		AsyncParticlesMixinConfig.config = configObj;
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

	private static void save(Mixin$Particle configObj) throws IOException {
		Properties properties = new Properties();
		configObj.write(properties);
		try (OutputStream os = Files.newOutputStream(MIXIN_CONFIG_FILE)) {
			properties.store(os, COMMENTS);
		}
	}

	static class Mixin$Particle {
		@Unmodifiable
		private Set<String> noCulling;

		{
			Set<String> noCulling = new LinkedHashSet<>();
			noCulling.add("pigcart.particlerain.particle.GroundFogParticle");
			this.noCulling = Collections.unmodifiableSet(noCulling);
		}

		@Unmodifiable
		private Set<String> noLightCache;

		{
			Set<String> noLightCache = new LinkedHashSet<>();
			noLightCache.add("dev.shadowsoffire.gateways.client.GatewayParticle");
			noLightCache.add("com.chailotl.particular.particles.FireflyParticle");
			noLightCache.add("com.lowdragmc.photon.client.gameobject.FXObject");
			noLightCache.add("net.diebuddies.minecraft.weather.WeatherParticle");
			this.noLightCache = Collections.unmodifiableSet(noLightCache);
		}

		@Unmodifiable
		private Set<String> lockProvider;

		{
			Set<String> lockProvider = new LinkedHashSet<>();
			lockProvider.add("yesman.epicfight.client.particle.TrailParticle");
			lockProvider.add("com.dfdyz.epicacg.client.particle.BloomTrailParticle");
			lockProvider.add("com.brandon3055.draconicevolution.client.render.effect.ExplosionFX");
			lockProvider.add("com.brandon3055.draconicevolution.client.render.effect.CrystalFXWireless");
			lockProvider.add("com.lowdragmc.photon.client.gameobject.emitter.Emitter");
			this.lockProvider = Collections.unmodifiableSet(lockProvider);
		}

		@Unmodifiable
		private Set<String> lockRequired;

		{
			Set<String> lockRequired = new LinkedHashSet<>();
			lockRequired.add("yesman.epicfight.client.particle.TrailParticle");
			lockRequired.add("com.dfdyz.epicacg.client.particle.BloomTrailParticle");
			lockRequired.add("com.brandon3055.draconicevolution.client.render.effect.ExplosionFX");
			lockRequired.add("com.brandon3055.draconicevolution.client.render.effect.CrystalFXWireless");
			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.Emitter");
			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter");
			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.beam.BeamEmitter");
			lockRequired.add("com.lowdragmc.photon.client.gameobject.emitter.trail.TrailEmitter");
			this.lockRequired = Collections.unmodifiableSet(lockRequired);
		}

		private void fold() {
			assertNotGlobal();
			noCulling = config.noCulling;
			noLightCache = config.noLightCache;
			lockProvider = config.lockProvider;
			lockRequired = config.lockRequired;
		}

		private void read(Properties properties) {
			assertNotGlobal();
			Mixin$Particle defaultConfig = new Mixin$Particle();
			noCulling = read(properties, "particle$noCulling", defaultConfig.noCulling);
			noLightCache = read(properties, "particle$noLightCache",defaultConfig.noLightCache);
			lockProvider = read(properties, "particle$lockRequired", defaultConfig.lockProvider);
			lockRequired = read(properties, "particle$lockProvider", defaultConfig.lockRequired);
		}

		private void flat() {
			config = this;
		}

		private void write(Properties properties) {
			properties.setProperty("particle$noCulling", String.join(",", noCulling));
			properties.setProperty("particle$noLightCache", String.join(",", noLightCache));
			properties.setProperty("particle$lockProvider", String.join(",", lockProvider));
			properties.setProperty("particle$lockRequired", String.join(",", lockRequired));
		}

		private static Set<String> read(Properties properties, String key, Set<String> defaultValue) {
			String value = properties.getProperty(key);
			if (value == null) {
				return defaultValue;
			}
			value = value.replace("[\\s\\u0085\\u2028\\u2029]", "");
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

		Set<String> getNoCulling() {
			return noCulling;
		}

		void setNoCulling(Set<String> noCulling) {
			assertNotGlobal();
			this.noCulling = noCulling;
		}

		Set<String> getNoLightCache() {
			return noLightCache;
		}

		void setNoLightCache(Set<String> noLightCache) {
			assertNotGlobal();
			this.noLightCache = noLightCache;
		}

		Set<String> getLockProvider() {
			return lockProvider;
		}

		void setLockProvider(Set<String> lockProvider) {
			assertNotGlobal();
			this.lockProvider = lockProvider;
		}

		Set<String> getLockRequired() {
			return lockRequired;
		}

		void setLockRequired(Set<String> lockRequired) {
			assertNotGlobal();
			this.lockRequired = lockRequired;
		}

		private void assertNotGlobal() {
			if (this == config) {
				throw new IllegalStateException("Cannot modify global config object");
			}
		}
	}
}
