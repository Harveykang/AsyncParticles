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
		particle$spinLockRequired: comma-separated list of particle classes that require a spin lock.""";
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
		Set<String> noCulling;

		{
			Set<String> noCulling = new LinkedHashSet<>();
			noCulling.add("pigcart.particlerain.particle.GroundFogParticle");
			this.noCulling = Collections.unmodifiableSet(noCulling);
		}

		@Unmodifiable
		Set<String> noLightCache;

		{
			Set<String> noLightCache = new LinkedHashSet<>();
			noLightCache.add("dev.shadowsoffire.gateways.client.GatewayParticle");
			noLightCache.add("com.chailotl.particular.particles.FireflyParticle");
			noLightCache.add("com.lowdragmc.photon.client.gameobject.FXObject");
			this.noLightCache = Collections.unmodifiableSet(noLightCache);
		}

		@Unmodifiable
		Set<String> spinLockRequired;

		{
			Set<String> spinLockRequired = new LinkedHashSet<>();
			spinLockRequired.add("yesman.epicfight.client.particle.TrailParticle");
			spinLockRequired.add("comcom.dfdyz.epicacg.client.particle.BloomTrailParticle");
			spinLockRequired.add("com.brandon3055.draconicevolution.client.render.effect.ExplosionFX");
			spinLockRequired.add("com.brandon3055.draconicevolution.client.render.effect.CrystalFXWireless");
			this.spinLockRequired = Collections.unmodifiableSet(spinLockRequired);
		}

		private void flat() {
			config = this;
		}

		private void fold() {
			if (this == config) {
				throw new IllegalStateException("Cannot modify global config object");
			}
			noCulling = config.noCulling;
			noLightCache = config.noLightCache;
			spinLockRequired = config.spinLockRequired;
		}

		private void read(Properties properties) {
			if (this == config) {
				throw new IllegalStateException("Cannot modify global config object");
			}
			Mixin$Particle defaultConfig = new Mixin$Particle();
			noCulling = read(properties, "particle$noCulling", defaultConfig.noCulling);
			noLightCache = read(properties, "particle$noLightCache",defaultConfig.noLightCache);
			spinLockRequired = read(properties, "particle$spinLockRequired", defaultConfig.spinLockRequired);
		}

		private void write(Properties properties) {
			properties.setProperty("particle$noCulling", String.join(",", noCulling));
			properties.setProperty("particle$noLightCache", String.join(",", noLightCache));
			properties.setProperty("particle$spinLockRequired", String.join(",", spinLockRequired));
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
	}
}
