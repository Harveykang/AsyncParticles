package fun.qu_an.minecraft.asyncparticles.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.BackendCaps;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElse;

public class AsyncParticlesConfig {
	public static final int MIN_PARTICLE_LIMIT = 1024;
	public static final int DEFAULT_PARTICLE_LIMIT = 16384;
	public static final int MAX_PARTICLE_LIMIT = 262144;
	public static final int VERSION = 1;
	public static final Path CONFIG_FILE = Path.of("config", AsyncParticlesClient.MOD_ID, AsyncParticlesClient.MOD_ID + ".json");
	static final Gson GSON = new GsonBuilder()
		.setLenient()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();
	static final Logger LOGGER = LogUtils.getLogger();
	public static int particle$particleLimit;
	public static boolean particle$removeIfMissedTick;
	public static boolean particle$parallelQueueRemoval;
	public static boolean particle$parallelQueueEviction;
	public static boolean particle$particleLightCache;
	public static boolean particle$cullUnderwaterParticleType;
	public static boolean tick$asyncAnimateTick;
	public static ParticleAsyncMode tick$particleAsyncMode;
	public static boolean tick$tickWeatherAsync;
	public static boolean tick$deferredTextureTick;
	public static int tick$failPerSecLimit;
//	public static FailBehavior tick$failBehavior;
	public static boolean tick$suppressCME;
	public static Set<String> tick$syncParticleClasses = new LinkedHashSet<>();
	public static RenderingMode rendering$particleRenderingMode;
	public static boolean rendering$gpuAcceleration;
	public static boolean rendering$appendNewParticlesToRenderer;
	public static int rendering$failPerSecLimit;
//	public static FailBehavior rendering$failBehavior;
//	public static Set<String> rendering$syncParticleClasses = new LinkedHashSet<>();
	public static RainEffect valkyrienSkies$rainEffect;
	public static boolean valkyrienSkies$fixParticleLights;
	public static RainEffect create$rainEffect;
	public static int create$tickRainBlockingRange;

	static {
		LOGGER.debug("AsyncParticlesConfig initialized.");
		try {
			load();
		} catch (Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static Screen newConfigScreen(Screen parent) {
		if (ModListHelper.CLOTH_CONFIG_LOADED) {
			return ClothConfigMenus.screenBuilder(parent).build();
		} else {
			return fallBackScreen(parent);
		}
	}

	private static Screen fallBackScreen(Screen parent) {
		FallbackScreen fallbackScreen = new FallbackScreen(
			parent,
			Component.translatable("gui.asyncparticles.menu-unavailable"),
			Component.translatable("gui.asyncparticles.menu-unavailable.message"),
			Component.translatable("gui.back"),
			current -> Minecraft.getInstance().gui.setScreen(current.parent),
			Component.translatable("gui.asyncparticles.reload"),
			current -> Minecraft.getInstance().gui.setScreen(
				new ConfirmScreen(b -> {
					MutableComponent msg = Component.translatable("gui.asyncparticles.menu-unavailable.message");
					if (!b) {
						current.message = msg;
						Minecraft.getInstance().gui.setScreen(current);
						return;
					}
					try {
						AsyncParticlesConfig.load();
					} catch (Exception e) {
						current.message = msg.append("\n").append(
							Component.translatable("gui.asyncparticles.failed-to-reload", e.toString())
								.withStyle(ChatFormatting.DARK_RED));
						Minecraft.getInstance().gui.setScreen(current);
						return;
					} finally {
						AsyncTickBehavior.getInstance().reloadLater();
					}
					current.message = msg.append("\n").append(
						Component.translatable("gui.asyncparticles.reload-successfully")
							.withStyle(ChatFormatting.DARK_GREEN));
					Minecraft.getInstance().gui.setScreen(current);
				},
					Component.translatable("gui.asyncparticles.menu-unavailable"),
					Component.translatable("gui.asyncparticles.reload-confirmation"))));
		@SuppressWarnings("unchecked")
		BiConsumer<FallbackScreen, Button>[] tickCallbacks = new BiConsumer[2];
		Consumer<FallbackScreen> reloadCallback = fallbackScreen.buttonRightCallback;
		tickCallbacks[0] = (fs, br) -> {
			if (!Minecraft.getInstance().hasShiftDown()) {
				return;
			}
			br.setMessage(Component.translatable("gui.asyncparticles.reset")
				.withStyle(ChatFormatting.RED));
			fs.buttonRightTick = tickCallbacks[1];
			fs.buttonRightCallback = current -> Minecraft.getInstance().gui.setScreen(
				new ConfirmScreen(b -> {
					MutableComponent msg = Component.translatable("gui.asyncparticles.menu-unavailable.message");
					if (!b) {
						current.message = msg;
						Minecraft.getInstance().gui.setScreen(current);
						return;
					}
					try {
						AsyncParticlesConfig.reset();
					} catch (Exception e) {
						current.message = msg.append("\n").append(
							Component.translatable("gui.asyncparticles.failed-to-reset", e.toString())
								.withStyle(ChatFormatting.DARK_RED));
						Minecraft.getInstance().gui.setScreen(current);
						return;
					} finally {
						AsyncTickBehavior.getInstance().reloadLater();
					}
					current.message = msg.append("\n").append(
						Component.translatable("gui.asyncparticles.reset-successfully")
							.withStyle(ChatFormatting.DARK_GREEN));
					Minecraft.getInstance().gui.setScreen(current);
				},
					Component.translatable("gui.asyncparticles.menu-unavailable"),
					Component.translatable("gui.asyncparticles.reset-confirmation")
						.withStyle(ChatFormatting.RED)));
		};
		tickCallbacks[1] = (fs, br) -> {
			if (Minecraft.getInstance().hasShiftDown()) {
				return;
			}
			br.setMessage(Component.translatable("gui.asyncparticles.reload"));
			fs.buttonRightTick = tickCallbacks[0];
			fs.buttonRightCallback = reloadCallback;
		};
		fallbackScreen.buttonRightTick = tickCallbacks[0];
		return fallbackScreen;
	}

	public static void load() throws IOException, JsonParseException {
		if (!Files.exists(CONFIG_FILE)) {
			Files.createDirectories(CONFIG_FILE.getParent());
			Files.createFile(CONFIG_FILE);
			reset();
			return;
		}

		ConfigObj configObj;
		try (BufferedReader json = Files.newBufferedReader(CONFIG_FILE)) {
			configObj = GSON.fromJson(json, ConfigObj.class);
		}
		if (configObj == null) {
			reset();
			return;
		}
		configObj = upgrade(configObj.version, configObj);

		configObj.flat();
		save(configObj);
	}

	@Contract
	private static ConfigObj upgrade(int ver, ConfigObj configObj) {
		if (VERSION != 1) {
			throw new RuntimeException("I forgot to update the upgrade method.");
		}
		return switch (ver) {
			case 1 -> configObj;
			default -> new ConfigObj();
		};
	}

	public static void save() throws IOException, JsonParseException {
		ConfigObj configObj = new ConfigObj();
		configObj.fold();
		save(configObj);
	}

	public static void reset() throws IOException {
		ConfigObj configObj = new ConfigObj();
		configObj.flat();
		save(configObj);
	}

	private static void save(ConfigObj configObj) throws IOException {
		configObj.version = VERSION;
		try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE)) {
			GSON.toJson(configObj, writer);
		}
	}

	static ConfigObj getCurrentConfig() {
		ConfigObj configObj = new ConfigObj();
		configObj.fold();
		return configObj;
	}

	static class ConfigObj {
		int version = 0; // 0 means no version, will reset to default values.
		Particle particle = new Particle();
		Tick tick = new Tick();
		Rendering rendering = new Rendering();
		ValkyrienSkies valkyrienSkies = new ValkyrienSkies();
		Create create = new Create();

		void flat() {
			particle.flat();
			tick.flat();
			rendering.flat();
			valkyrienSkies.flat();
			create.flat();
		}

		void fold() {
			particle.fold();
			tick.fold();
			rendering.fold();
			valkyrienSkies.fold();
			create.fold();
		}

		static class Particle {
			int particleLimit = DEFAULT_PARTICLE_LIMIT;
			boolean removeIfMissedTick = false;
			boolean parallelQueueRemoval = true;
			boolean parallelQueueEviction = true;
			boolean particleLightCache = true;
			boolean cullUnderwaterParticleType = true;

			private void flat() {
				particle$particleLimit = Mth.clamp(particleLimit, MIN_PARTICLE_LIMIT, MAX_PARTICLE_LIMIT);
				particle$removeIfMissedTick = removeIfMissedTick;
				particle$parallelQueueRemoval = parallelQueueRemoval;
				particle$parallelQueueEviction = parallelQueueEviction;
				particle$particleLightCache = particleLightCache;
				particle$cullUnderwaterParticleType = cullUnderwaterParticleType;
			}

			private void fold() {
				particleLimit = particle$particleLimit;
				removeIfMissedTick = particle$removeIfMissedTick;
				parallelQueueRemoval = particle$parallelQueueRemoval;
				parallelQueueEviction = particle$parallelQueueEviction;
				particleLightCache = particle$particleLightCache;
				cullUnderwaterParticleType = particle$cullUnderwaterParticleType;
			}
		}

		static class Tick {
			boolean animationTickMode = true;
			ParticleAsyncMode particleAsyncMode = ParticleAsyncMode.SEQUENTIAL;
			boolean tickWeatherAsync = true;
			boolean deferredTextureTick = !ModListHelper.AXIOM_LOADED;
			int failPerSecLimit = 5;
			FailBehavior failBehavior = FailBehavior.RAISE_CRASH;
			boolean suppressCME = false;
			Set<String> syncParticleClasses = new LinkedHashSet<>();
			{
			}

			private void flat() {
				tick$asyncAnimateTick = animationTickMode;
				tick$particleAsyncMode = requireNonNullElse(particleAsyncMode, ParticleAsyncMode.SEQUENTIAL);
				tick$tickWeatherAsync = tickWeatherAsync;
				tick$deferredTextureTick = deferredTextureTick && !ModListHelper.AXIOM_LOADED;
				tick$failPerSecLimit = Mth.clamp(failPerSecLimit, 0, 256);
//				tick$failBehavior = requireNonNullElse(failBehavior, FailBehavior.RAISE_CRASH);
				tick$suppressCME = suppressCME;
				tick$syncParticleClasses = new LinkedHashSet<>(syncParticleClasses);
			}

			private void fold() {
				animationTickMode = tick$asyncAnimateTick;
				particleAsyncMode = tick$particleAsyncMode;
				tickWeatherAsync = tick$tickWeatherAsync;
				deferredTextureTick = tick$deferredTextureTick;
				failPerSecLimit = tick$failPerSecLimit;
//				failBehavior = tick$failBehavior;
				suppressCME = tick$suppressCME;
				syncParticleClasses = new LinkedHashSet<>(tick$syncParticleClasses);
			}
		}

		static class Rendering {
			RenderingMode particleRenderingMode = RenderingMode.SYNCHRONOUSLY;
			boolean gpuAcceleration = BackendCaps.supportsGpuAcceleration();
			boolean appendNewParticlesToRenderer = true;
			int failPerSecLimit = 20;
//			FailBehavior failBehavior = FailBehavior.MARK_AS_SYNC;
//			Set<String> syncParticleClasses = new LinkedHashSet<>();
//			{
//				syncParticleClasses.add("com.lootbeams.VFXParticle");
//				syncParticleClasses.add("ovh.corail.tombstone.particle.ParticleCasting");
//				syncParticleClasses.add("ovh.corail.tombstone.particle.ParticleGhost");
//				syncParticleClasses.add("ovh.corail.tombstone.particle.ParticleGraveSoul");
//				syncParticleClasses.add("ovh.corail.tombstone.particle.ParticleMagicCircle");
//				syncParticleClasses.add("ovh.corail.tombstone.particle.ParticleMarker");
//				syncParticleClasses.add("ovh.corail.tombstone.particle.ParticleRounding");
//				syncParticleClasses.add("concerrox.effective.particle.SplashParticle");
//				syncParticleClasses.add("org.ladysnake.effective.particle.SplashParticle");
//				syncParticleClasses.add("net.mehvahdjukaar.dummmmmmy.client.DamageNumberParticle");
//			}

			private void flat() {
				rendering$particleRenderingMode = requireNonNullElse(particleRenderingMode, RenderingMode.DELAYED);
				rendering$gpuAcceleration = gpuAcceleration && BackendCaps.supportsGpuAcceleration();
				rendering$appendNewParticlesToRenderer = appendNewParticlesToRenderer;
				rendering$failPerSecLimit = Mth.clamp(failPerSecLimit, 0, 256);
//				rendering$failBehavior = requireNonNullElse(failBehavior, FailBehavior.MARK_AS_SYNC);
//				rendering$syncParticleClasses = new LinkedHashSet<>(syncParticleClasses);
			}

			private void fold() {
				particleRenderingMode = rendering$particleRenderingMode;
				gpuAcceleration = rendering$gpuAcceleration;
				appendNewParticlesToRenderer = rendering$appendNewParticlesToRenderer;
				failPerSecLimit = rendering$failPerSecLimit;
//				failBehavior = rendering$failBehavior;
//				syncParticleClasses = new LinkedHashSet<>(rendering$syncParticleClasses);
			}
		}

		static class ValkyrienSkies {
			RainEffect rainEffect = RainEffect.STATIONARY;
			boolean fixParticleLights = true;

			private void flat() {
				valkyrienSkies$rainEffect = requireNonNullElse(rainEffect, RainEffect.STATIONARY);
				valkyrienSkies$fixParticleLights = fixParticleLights;
			}

			private void fold() {
				rainEffect = valkyrienSkies$rainEffect;
				fixParticleLights = valkyrienSkies$fixParticleLights;
			}
		}

		static class Create {
			RainEffect rainEffect = RainEffect.ALWAYS;
			int tickRainBlockingRange = ModListHelper.PARTICLERAIN_LOADED ? 32 : 16;

			private void flat() {
				create$rainEffect = requireNonNullElse(rainEffect, RainEffect.ALWAYS);
				create$tickRainBlockingRange = Mth.clamp(tickRainBlockingRange, 10, 512);
			}

			private void fold() {
				rainEffect = create$rainEffect;
				tickRainBlockingRange = create$tickRainBlockingRange;
			}
		}
	}
}
