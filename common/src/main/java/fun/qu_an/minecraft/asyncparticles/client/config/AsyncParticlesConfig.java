package fun.qu_an.minecraft.asyncparticles.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElse;

public class AsyncParticlesConfig {
	public static final Path CONFIG_FILE = Path.of("config", "asyncparticles", "asyncparticles.json");
	static final Gson GSON = new GsonBuilder()
		.setVersion(1.0)
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();
	static final Logger LOGGER = LogUtils.getLogger();
	public static int particle$particleLimit;
	public static boolean particle$particleLightCache;
	public static AsyncTickBehavior tick$asyncAnimationTickBehavior;
	public static AsyncTickBehavior tick$asyncParticleTickBehavior;
	public static int tick$failPerSecLimit;
	public static FailBehavior tick$failBehavior;
	public static boolean tick$suppressCME;
	public static boolean rendering$cullParticles;
	public static boolean rendering$asyncParticleRendering;
	public static int rendering$failPerSecLimit;
	public static FailBehavior rendering$failBehavior;
	public static RainEffect valkyrienSkies$rainEffect;
	public static boolean valkyrienSkies$fixParticleLights;
	public static RainEffect create$rainEffect;

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
						AsyncParticlesConfig.load();
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
			if (LegacyConfigTransitions.migrate()) {
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

		configObj.flat();
		save(configObj);
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
		try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE)) {
			GSON.toJson(configObj, writer);
		}
	}

	static class ConfigObj {
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
			boolean particleLightCache = true;

			private void flat() {
				particle$particleLimit = Mth.clamp(particleLimit, 1024, 262144);
				particle$particleLightCache = particleLightCache;
			}

			private void fold() {
				particleLimit = particle$particleLimit;
				particleLightCache = particle$particleLightCache;
			}
		}

		static class Tick {
			AsyncTickBehavior asyncAnimationTickBehavior = AsyncTickBehavior.INTERRUPTIBLE;
			AsyncTickBehavior asyncParticleTickBehavior = AsyncTickBehavior.INTERRUPTIBLE;
			int failPerSecLimit = 5;
			FailBehavior failBehavior = FailBehavior.RAISE_CRASH;
			boolean suppressCME = false;

			private void flat() {
				tick$asyncAnimationTickBehavior = requireNonNullElse(asyncAnimationTickBehavior, AsyncTickBehavior.INTERRUPTIBLE);
				tick$asyncParticleTickBehavior = requireNonNullElse(asyncParticleTickBehavior, AsyncTickBehavior.INTERRUPTIBLE);
				tick$failPerSecLimit = Mth.clamp(failPerSecLimit, 0, 256);
				tick$failBehavior = requireNonNullElse(failBehavior, FailBehavior.RAISE_CRASH);
				tick$suppressCME = suppressCME;
			}

			private void fold() {
				asyncAnimationTickBehavior = tick$asyncAnimationTickBehavior;
				asyncParticleTickBehavior = tick$asyncParticleTickBehavior;
				failPerSecLimit = tick$failPerSecLimit;
				failBehavior = tick$failBehavior;
				suppressCME = tick$suppressCME;
			}
		}

		static class Rendering {
			boolean cullParticles = true;
			boolean asyncParticleRendering = true;
			int failPerSecLimit = 20;
			FailBehavior failBehavior = FailBehavior.MARK_AS_SYNC;

			private void flat() {
				rendering$cullParticles = cullParticles;
				rendering$asyncParticleRendering = !ModListHelper.PHOTON_EDITOR_LOADED && asyncParticleRendering;
				rendering$failPerSecLimit = Mth.clamp(failPerSecLimit, 0, 256);
				rendering$failBehavior = requireNonNullElse(failBehavior, FailBehavior.MARK_AS_SYNC);
			}

			private void fold() {
				cullParticles = rendering$cullParticles;
				asyncParticleRendering = rendering$asyncParticleRendering;
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

	// No more NoClassDefFoundError
	private static class ClothConfigMenus {
		private static ConfigBuilder screenBuilder(Screen screen) {
			ConfigObj defaultConfig = new ConfigObj();
			ConfigBuilder builder = ConfigBuilder.create();
			builder.setParentScreen(screen);
			builder.setTitle(Component.translatable("gui.asyncparticles"));
			ConfigEntryBuilder entryBuilder = builder.entryBuilder();

			// Tick Category
			ConfigCategory particleCategory = builder
				.getOrCreateCategory(Component.translatable("config.asyncparticles.category.particle"));
			particleCategory
				.addEntry(entryBuilder
					.startIntField(Component.translatable("config.asyncparticles.particle.particleLimit"),
						particle$particleLimit)
					.setDefaultValue(defaultConfig.particle.particleLimit)
					.setTooltip(Component.translatable("config.asyncparticles.particle.particleLimit.tooltip"))
					.setSaveConsumer(newValue -> {
						particle$particleLimit = newValue;
						AsyncTicker.reloadLater();
					})
					.setMin(1024)
					.setMax(262144)
					.build())
				.addEntry(entryBuilder
					.startBooleanToggle(Component.translatable("config.asyncparticles.particle.particleLightCache"),
						particle$particleLightCache)
					.setDefaultValue(defaultConfig.particle.particleLightCache)
					.setTooltip(Component.translatable("config.asyncparticles.particle.particleLightCache.tooltip"))
					.setSaveConsumer(newValue -> particle$particleLightCache = newValue)
					.build());

			ConfigCategory tickCategory = builder
				.getOrCreateCategory(Component.translatable("config.asyncparticles.category.tick"));
			tickCategory
				.addEntry(entryBuilder
					.startEnumSelector(Component.translatable("config.asyncparticles.tick.asyncAnimationTickBehavior"),
						AsyncTickBehavior.class, tick$asyncAnimationTickBehavior)
					.setEnumNameProvider(value -> ((TranslatableEnum) value).getTranslatable())
					.setDefaultValue(defaultConfig.tick.asyncAnimationTickBehavior)
					.setTooltip(Component.translatable("config.asyncparticles.tick.asyncAnimationTickBehavior.tooltip"))
					.setSaveConsumer(newValue -> tick$asyncAnimationTickBehavior = newValue)
					.build())
				.addEntry(entryBuilder
					.startEnumSelector(Component.translatable("config.asyncparticles.tick.asyncParticleTickBehavior"),
						AsyncTickBehavior.class, tick$asyncParticleTickBehavior)
					.setEnumNameProvider(value -> ((TranslatableEnum) value).getTranslatable())
					.setDefaultValue(defaultConfig.tick.asyncParticleTickBehavior)
					.setTooltip(Component.translatable("config.asyncparticles.tick.asyncParticleTickBehavior.tooltip"))
					.setSaveConsumer(newValue -> tick$asyncParticleTickBehavior = newValue)
					.build())
				.addEntry(entryBuilder
					.startIntField(Component.translatable("config.asyncparticles.tick.failPerSecLimit"),
						tick$failPerSecLimit)
					.setDefaultValue(defaultConfig.tick.failPerSecLimit)
					.setTooltip(Component.translatable("config.asyncparticles.tick.failPerSecLimit.tooltip"))
					.setSaveConsumer(newValue -> tick$failPerSecLimit = newValue)
					.setMin(0)
					.setMax(256)
					.build())
				.addEntry(entryBuilder
					.startEnumSelector(Component.translatable("config.asyncparticles.tick.failBehavior"),
						FailBehavior.class, tick$failBehavior)
					.setEnumNameProvider(value -> ((TranslatableEnum) value).getTranslatable())
					.setDefaultValue(defaultConfig.tick.failBehavior)
					.setTooltip(Component.empty()
						.append(Component.translatable("config.asyncparticles.tick.failBehavior.tooltip")
							.withStyle(ChatFormatting.STRIKETHROUGH))
						.append("\n")
						.append(Component.translatable("config.asyncparticles.not-implemented")
							.withStyle(ChatFormatting.DARK_RED)))
					.setSaveConsumer(newValue -> tick$failBehavior = newValue)
					.setRequirement(() -> false)
					.build())
				.addEntry(entryBuilder
					.startBooleanToggle(Component.translatable("config.asyncparticles.tick.suppressCME"),
						tick$suppressCME)
					.setDefaultValue(defaultConfig.tick.suppressCME)
					.setTooltip(Component.translatable("config.asyncparticles.tick.suppressCME.tooltip"))
					.setSaveConsumer(newValue -> tick$suppressCME = newValue)
					.build());

			// Rendering Category
			ConfigCategory renderingCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.rendering"));
			renderingCategory
				.addEntry(entryBuilder
					.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.cullParticles"),
						rendering$cullParticles)
					.setDefaultValue(defaultConfig.rendering.cullParticles)
					.setTooltip(Component.translatable("config.asyncparticles.rendering.cullParticles.tooltip"))
					.setSaveConsumer(newValue -> rendering$cullParticles = newValue)
					.build())
				.addEntry(entryBuilder
					.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.asyncParticleRendering"),
						rendering$asyncParticleRendering)
					.setDefaultValue(defaultConfig.rendering.asyncParticleRendering)
					.setTooltipSupplier(() -> {
						Component[] components;
						if (!ModListHelper.PHOTON_EDITOR_LOADED) {
							components = new Component[]{
								Component.translatable("config.asyncparticles.rendering.asyncParticleRendering.tooltip")};
						} else {
							components = new Component[]{
								Component.translatable("config.asyncparticles.rendering.asyncParticleRendering.tooltip")
									.withStyle(ChatFormatting.STRIKETHROUGH),
								Component.translatable("config.asyncparticles.incompatibility", "Photon Editor")
									.withStyle(ChatFormatting.DARK_RED)};
						}
						return Optional.of(components);
					})
					.setSaveConsumer(newValue -> rendering$asyncParticleRendering = newValue)
					.setRequirement(() -> !ModListHelper.PHOTON_EDITOR_LOADED)
					.build())
				.addEntry(entryBuilder
					.startIntField(Component.translatable("config.asyncparticles.rendering.failPerSecLimit"),
						rendering$failPerSecLimit)
					.setDefaultValue(defaultConfig.rendering.failPerSecLimit)
					.setTooltip(Component.translatable("config.asyncparticles.rendering.failPerSecLimit.tooltip"))
					.setSaveConsumer(newValue -> rendering$failPerSecLimit = newValue)
					.setMin(0)
					.setMax(256)
					.build())
				.addEntry(entryBuilder
					.startEnumSelector(Component.translatable("config.asyncparticles.rendering.failBehavior"),
						FailBehavior.class, rendering$failBehavior)
					.setEnumNameProvider(value -> ((TranslatableEnum) value).getTranslatable())
					.setDefaultValue(defaultConfig.rendering.failBehavior)
					.setTooltip(Component.empty()
						.append(Component.translatable("config.asyncparticles.rendering.failBehavior.tooltip"))
						.append("\n")
						.append(Component.translatable("config.asyncparticles.not-implemented")
							.withStyle(ChatFormatting.DARK_RED)))
					.setSaveConsumer(newValue -> rendering$failBehavior = newValue)
					.setRequirement(() -> false)
					.build());

			// Mod Compat Category
			ConfigCategory modCompatCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mod-compat"));
			modCompatCategory
				.addEntry(entryBuilder
					// .startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
					.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
						List.of(entryBuilder
							// .startEnumSelector(Component.translatable("config.asyncparticles.valkyrienskies.rainEffect"),
							// 	RainEffect.class, valkyrienSkies$rainEffect)
							.startSelector(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect"),
								new RainEffect[]{RainEffect.ALWAYS, RainEffect.STATIONARY}, valkyrienSkies$rainEffect)
							.setNameProvider(value -> ((TranslatableEnum) value).getTranslatable())
							.setDefaultValue(defaultConfig.valkyrienSkies.rainEffect)
							.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect.tooltip"))
							.setSaveConsumer(newValue -> valkyrienSkies$rainEffect = newValue)
							.setRequirement(() -> ModListHelper.VS_LOADED)
							.build(), entryBuilder
							.startBooleanToggle(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights"),
								valkyrienSkies$fixParticleLights)
							.setDefaultValue(defaultConfig.valkyrienSkies.fixParticleLights)
							.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights.tooltip"))
							.setSaveConsumer(newValue -> valkyrienSkies$fixParticleLights = newValue)
							.setRequirement(() -> ModListHelper.VS_LOADED)
							.build()
						))
					.build())
				.addEntry(entryBuilder
					// .startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.create"),
					.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.create"),
						List.of(entryBuilder
							.startEnumSelector(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect"),
								RainEffect.class, create$rainEffect)
							.setEnumNameProvider(value -> ((TranslatableEnum) value).getTranslatable())
							.setDefaultValue(defaultConfig.create.rainEffect)
							.setTooltip(Component.empty()
								.append(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect.tooltip")
									.withStyle(ChatFormatting.STRIKETHROUGH))
								.append("\n")
								.append(Component.translatable("config.asyncparticles.not-implemented")
									.withStyle(ChatFormatting.DARK_RED)))
							.setSaveConsumer(newValue -> create$rainEffect = newValue)
							// .setRequirement(() -> ModListHelper.CREATE_LOADED)
							.setRequirement(() -> false)
							.build()))
					.build());

			builder.setSavingRunnable(() -> {
				try {
					save();
				} catch (Exception e) {
					LOGGER.error("Failed to save config", e);
					Minecraft mc = Minecraft.getInstance();
					Screen configScreen = mc.screen;
					ThreadUtil.enqueueClientTask(() -> {
						Screen prevScreen = mc.screen;
						mc.setScreen(new FallbackScreen(
							null,
							Component.translatable("gui.asyncparticles.error"),
							Component.translatable("gui.asyncparticles.failed-to-save", e.toString()),
							Component.translatable("gui.back"),
							current -> Minecraft.getInstance().setScreen(prevScreen),
							Component.translatable("gui.continue"),
							current -> Minecraft.getInstance().setScreen(configScreen)));
					});
				}
			});
			return builder;
		}
	}
}
