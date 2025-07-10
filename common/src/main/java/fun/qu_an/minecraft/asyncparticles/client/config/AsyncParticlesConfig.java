package fun.qu_an.minecraft.asyncparticles.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElse;

public class AsyncParticlesConfig {
	public static final int VERSION = 1;
	public static final Path CONFIG_FILE = Path.of("config", "asyncparticles", "asyncparticles.json");
	static final Gson GSON = new GsonBuilder()
		.setLenient()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();
	static final Logger LOGGER = LogUtils.getLogger();
	public static int particle$particleLimit;
	public static boolean particle$removeIfMissedTick;
	public static boolean particle$particleLightCache;
	public static boolean particle$cullUnderwaterParticleType;
	public static TickMode tick$animationTickMode;
	public static TickMode tick$particleTickMode;
	public static int tick$failPerSecLimit;
	public static FailBehavior tick$failBehavior;
	public static boolean tick$suppressCME;
	public static ParticleCullingMode rendering$particleCulling;
	public static RenderingMode rendering$particleRenderingMode;
	public static int rendering$failPerSecLimit;
	public static FailBehavior rendering$failBehavior;
	public static RainEffect valkyrienSkies$rainEffect;
	public static boolean valkyrienSkies$fixParticleLights;
	public static RainEffect create$rainEffect;

	static {
		try {
			load();
			LOGGER.debug("AsyncParticlesConfig initialized.");
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
			current -> Minecraft.getInstance().setScreen(current.parent),
			Component.translatable("gui.asyncparticles.reload"),
			current -> Minecraft.getInstance().setScreen(
				new ConfirmScreen(b -> {
					MutableComponent msg = Component.translatable("gui.asyncparticles.menu-unavailable.message");
					if (!b) {
						current.message = msg;
						Minecraft.getInstance().setScreen(current);
						return;
					}
					try {
						load();
					} catch (Exception e) {
						current.message = msg.append("\n").append(
							Component.translatable("gui.asyncparticles.failed-to-reload", e.toString())
								.withStyle(ChatFormatting.DARK_RED));
						Minecraft.getInstance().setScreen(current);
						return;
					} finally {
						AsyncTicker.reloadLater();
					}
					current.message = msg.append("\n").append(
						Component.translatable("gui.asyncparticles.reload-successfully")
							.withStyle(ChatFormatting.DARK_GREEN));
					Minecraft.getInstance().setScreen(current);
				},
					Component.translatable("gui.asyncparticles.menu-unavailable"),
					Component.translatable("gui.asyncparticles.reload-confirmation"))));
		@SuppressWarnings("unchecked")
		BiConsumer<FallbackScreen, Button>[] tickCallbacks = new BiConsumer[2];
		Consumer<FallbackScreen> reloadCallback = fallbackScreen.buttonRightCallback;
		tickCallbacks[0] = (fs, br) -> {
			if (!Screen.hasShiftDown()) {
				return;
			}
			br.setMessage(Component.translatable("gui.asyncparticles.reset")
				.withStyle(ChatFormatting.RED));
			fs.buttonRightTick = tickCallbacks[1];
			fs.buttonRightCallback = current -> Minecraft.getInstance().setScreen(
				new ConfirmScreen(b -> {
					MutableComponent msg = Component.translatable("gui.asyncparticles.menu-unavailable.message");
					if (!b) {
						current.message = msg;
						Minecraft.getInstance().setScreen(current);
						return;
					}
					try {
						AsyncParticlesConfig.reset();
					} catch (Exception e) {
						current.message = msg.append("\n").append(
							Component.translatable("gui.asyncparticles.failed-to-reset", e.toString())
								.withStyle(ChatFormatting.DARK_RED));
						Minecraft.getInstance().setScreen(current);
						return;
					} finally {
						AsyncTicker.reloadLater();
					}
					current.message = msg.append("\n").append(
						Component.translatable("gui.asyncparticles.reset-successfully")
							.withStyle(ChatFormatting.DARK_GREEN));
					Minecraft.getInstance().setScreen(current);
				},
					Component.translatable("gui.asyncparticles.menu-unavailable"),
					Component.translatable("gui.asyncparticles.reset-confirmation")
						.withStyle(ChatFormatting.RED)));
		};
		tickCallbacks[1] = (fs, br) -> {
			if (Screen.hasShiftDown()) {
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
			new ConfigObj().flat();
			if (LegacyConfigMigrator.migrate()) {
				save();
			} else {
				reset();
			}
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
		LOGGER.debug("asyncparticles.json loaded.");
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

	static class ConfigObj {
		int version = 0; // 0 means no version, will reset to default values.
		Particle particle = new Particle();
		Tick tick = new Tick();
		Rendering rendering = new Rendering();
		ValkyrienSkies valkyrienSkies = new ValkyrienSkies();
		Create create = new Create();

		private void flat() {
			particle.flat();
			tick.flat();
			rendering.flat();
			valkyrienSkies.flat();
			create.flat();
		}

		private void fold() {
			particle.fold();
			tick.fold();
			rendering.fold();
			valkyrienSkies.fold();
			create.fold();
		}

		static class Particle {
			int particleLimit = 16384;
			boolean removeIfMissedTick = false;
			boolean particleLightCache = true;
			boolean cullUnderwaterParticleType = true;

			private void flat() {
				particle$particleLimit = Mth.clamp(particleLimit, 1024, 262144);
				particle$removeIfMissedTick = removeIfMissedTick;
				particle$particleLightCache = particleLightCache;
				particle$cullUnderwaterParticleType = cullUnderwaterParticleType;
			}

			private void fold() {
				particleLimit = particle$particleLimit;
				removeIfMissedTick = particle$removeIfMissedTick;
				particleLightCache = particle$particleLightCache;
				cullUnderwaterParticleType = particle$cullUnderwaterParticleType;
			}
		}

		static class Tick {
			TickMode animationTickMode = TickMode.INTERRUPTIBLE;
			TickMode particleTickMode = TickMode.INTERRUPTIBLE;
			int failPerSecLimit = 5;
			FailBehavior failBehavior = FailBehavior.RAISE_CRASH;
			boolean suppressCME = false;

			private void flat() {
				tick$animationTickMode = requireNonNullElse(animationTickMode, TickMode.INTERRUPTIBLE);
				tick$particleTickMode = requireNonNullElse(particleTickMode, TickMode.INTERRUPTIBLE);
				tick$failPerSecLimit = Mth.clamp(failPerSecLimit, 0, 256);
				tick$failBehavior = requireNonNullElse(failBehavior, FailBehavior.RAISE_CRASH);
				tick$suppressCME = suppressCME;
			}

			private void fold() {
				animationTickMode = tick$animationTickMode;
				particleTickMode = tick$particleTickMode;
				failPerSecLimit = tick$failPerSecLimit;
				failBehavior = tick$failBehavior;
				suppressCME = tick$suppressCME;
			}
		}

		static class Rendering {
			ParticleCullingMode particleCulling = ParticleCullingMode.AABB;
			RenderingMode particleRenderingMode = RenderingMode.DELAYED;
			int failPerSecLimit = 20;
			FailBehavior failBehavior = FailBehavior.MARK_AS_SYNC;

			private void flat() {
				rendering$particleCulling = particleCulling;
				rendering$particleRenderingMode = requireNonNullElse(particleRenderingMode, RenderingMode.DELAYED);
				rendering$failPerSecLimit = Mth.clamp(failPerSecLimit, 0, 256);
				rendering$failBehavior = requireNonNullElse(failBehavior, FailBehavior.MARK_AS_SYNC);
			}

			private void fold() {
				particleCulling = rendering$particleCulling;
				particleRenderingMode = rendering$particleRenderingMode;
				failPerSecLimit = rendering$failPerSecLimit;
				failBehavior = rendering$failBehavior;
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

			private void flat() {
				create$rainEffect = requireNonNullElse(rainEffect, RainEffect.ALWAYS);
			}

			private void fold() {
				rainEffect = create$rainEffect;
			}
		}
	}

}
