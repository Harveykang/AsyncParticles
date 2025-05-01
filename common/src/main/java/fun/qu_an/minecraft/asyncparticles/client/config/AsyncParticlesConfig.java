package fun.qu_an.minecraft.asyncparticles.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNullElseGet;

public class AsyncParticlesConfig {
	public static final Path CONFIG_FILE = Path.of("config", "asyncparticles.json");
	static final Gson GSON = new GsonBuilder()
		.setVersion(1.0)
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();
	private static final Logger LOGGER = LogUtils.getLogger();
	public static int tick$particleLimit;
	public static boolean tick$particleLightCache;
	public static AsyncTickBehavior tick$asyncAnimationTickBehavior;
	public static AsyncTickBehavior tick$asyncParticleTickBehavior;
	public static int tick$failPerSecLimit;
	public static FailBehavior tick$failBehavior;
	public static boolean tick$suppressCME;
	public static boolean rendering$asyncParticleRendering;
	public static int rendering$failPerSecLimit;
	public static FailBehavior rendering$failBehavior;
	public static RainEffect valkyrienSkies$rainEffect;
	public static boolean valkyrienSkies$fixParticleLights;
	public static RainEffect create$rainEffect;

	public static ConfigBuilder screenBuilder(Screen screen) {
		ConfigObj defaultConfig = new ConfigObj();
		ConfigBuilder builder = ConfigBuilder.create();
		builder.setParentScreen(screen);
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		// Tick Category
		ConfigCategory particleCategory = builder
			.getOrCreateCategory(Component.translatable("config.asyncparticles.category.particle"));
		particleCategory
			.addEntry(entryBuilder
				.startIntField(Component.translatable("config.asyncparticles.particle.particleLimit"),
					tick$particleLimit)
				.setDefaultValue(defaultConfig.tick.particleLimit)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLimit.tooltip"))
				.setSaveConsumer(newValue -> {
					tick$particleLimit = newValue;
					AsyncTicker.reloadLater();
				})
				.setMin(1024)
				.setMax(262144)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.particleLightCache"),
					tick$particleLightCache)
				.setDefaultValue(defaultConfig.tick.particleLightCache)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLightCache.tooltip"))
				.setSaveConsumer(newValue -> tick$particleLightCache = newValue)
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
						.setStyle(Style.EMPTY.withStrikethrough(true)))
					.append("\n")
					.append(Component.translatable("config.asyncparticles.not-implemented")))
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
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.asyncParticleRendering"),
					rendering$asyncParticleRendering)
				.setDefaultValue(defaultConfig.rendering.asyncParticleRendering)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.asyncParticleRendering.tooltip"))
				.setSaveConsumer(newValue -> rendering$asyncParticleRendering = newValue)
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
					.append(Component.translatable("config.asyncparticles.not-implemented")))
				.setSaveConsumer(newValue -> rendering$failBehavior = newValue)
				.setRequirement(() -> false)
				.build());

		// Mod Compat Category
		ConfigCategory modCompatCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mod-compat"));
		modCompatCategory
			.addEntry(entryBuilder
//				.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
				.startSubCategory(Component.translatable("itemGroup.valkyrienSkies"),
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
//				.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.create"),
				.startSubCategory(Component.translatable("itemGroup.create.base"),
					List.of(entryBuilder
						.startEnumSelector(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect"),
							RainEffect.class, create$rainEffect)
						.setEnumNameProvider(value -> ((TranslatableEnum) value).getTranslatable())
						.setDefaultValue(defaultConfig.create.rainEffect)
						.setTooltip(Component.empty()
							.append(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect.tooltip")
								.setStyle(Style.EMPTY.withStrikethrough(true)))
							.append("\n")
							.append(Component.translatable("config.asyncparticles.not-implemented")))
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
				Screen prevScreen = mc.screen;
				mc.setScreen(new FallbackScreen(
					prevScreen,
					Component.translatable("config.asyncparticles.error"),
					Component.translatable("config.asyncparticles.error.save-failed", e.getMessage()),
					Component.translatable("gui.back"),
					current -> Minecraft.getInstance().setScreen(current.parent),
					Component.translatable("gui.continue"),
					current -> Minecraft.getInstance().setScreen(current.parent)));
			}
		});
		return builder;
	}

	public static Screen fallBackScreen(Screen parent) {
		return new FallbackScreen(
			parent,
			Component.translatable("config.asyncparticles.error.menu-unavailable"),
			Component.translatable("config.asyncparticles.error.cloth-config-required"),
			Component.translatable("gui.cancel"),
			current -> {
				try {
					AsyncParticlesConfig.save();
				} catch (Exception e) {
					current.reason = Component.translatable("config.asyncparticles.error.save-failed", e.getMessage());
					return;
				}
				Minecraft.getInstance().setScreen(current.parent);
			},
			Component.translatable("gui.done"),
			current -> {
				try {
					AsyncParticlesConfig.reload();
				} catch (Exception e) {
					current.reason = Component.translatable("config.asyncparticles.error.reload-failed", e.getMessage());
					return;
				}
				AsyncTicker.reloadLater();
				Minecraft.getInstance().setScreen(current.parent);
			});
	}

	public static void reload() throws IOException, JsonParseException {
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

		configObj.flat();
		save(configObj);
	}

	public static void save() throws IOException, JsonParseException {
		ConfigObj configObj = new ConfigObj();
		configObj.fold();
		save(configObj);
	}

	private static void reset() throws IOException {
		ConfigObj configObj = new ConfigObj();
		configObj.flat();
		save(configObj);
	}

	private static void save(ConfigObj configObj) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE)) {
			GSON.toJson(configObj, writer);
		}
	}

	private static class ConfigObj {
		Tick tick = new Tick();
		Rendering rendering = new Rendering();
		ValkyrienSkies valkyrienSkies = new ValkyrienSkies();
		Create create = new Create();

		private void flat() {
			tick.flat();
			rendering.flat();
			valkyrienSkies.flat();
			create.flat();
		}

		private void fold() {
			tick.fold();
			rendering.fold();
			valkyrienSkies.fold();
			create.fold();
		}

		private static class Tick {
			int particleLimit = 32768;
			boolean particleLightCache = true;
			AsyncTickBehavior asyncAnimationTickBehavior = AsyncTickBehavior.INTERRUPTIBLE;
			AsyncTickBehavior asyncParticleTickBehavior = AsyncTickBehavior.INTERRUPTIBLE;
			int failPerSecLimit = 5;
			FailBehavior failBehavior = FailBehavior.RAISE_CRASH;
			boolean suppressCME = false;

			private void flat() {
				tick$particleLimit = particleLimit;
				tick$particleLightCache = particleLightCache;
				tick$asyncAnimationTickBehavior = requireNonNullElseGet(asyncAnimationTickBehavior, () -> new Tick().asyncAnimationTickBehavior);
				tick$asyncParticleTickBehavior = requireNonNullElseGet(asyncParticleTickBehavior, () -> new Tick().asyncParticleTickBehavior);
				tick$failPerSecLimit = failPerSecLimit;
				tick$failBehavior = requireNonNullElseGet(failBehavior, () -> new Tick().failBehavior);
				tick$suppressCME = suppressCME;
			}

			private void fold() {
				particleLimit = tick$particleLimit;
				particleLightCache = tick$particleLightCache;
				asyncAnimationTickBehavior = tick$asyncAnimationTickBehavior;
				asyncParticleTickBehavior = tick$asyncParticleTickBehavior;
				failPerSecLimit = tick$failPerSecLimit;
				failBehavior = tick$failBehavior;
				suppressCME = tick$suppressCME;
			}
		}

		private static class Rendering {
			boolean asyncParticleRendering = true;
			int failPerSecLimit = 20;
			FailBehavior failBehavior = FailBehavior.MARK_AS_SYNC;

			private void flat() {
				rendering$asyncParticleRendering = asyncParticleRendering;
				rendering$failPerSecLimit = failPerSecLimit;
				rendering$failBehavior = requireNonNullElseGet(failBehavior, () -> new Rendering().failBehavior);
			}

			private void fold() {
				asyncParticleRendering = rendering$asyncParticleRendering;
				failPerSecLimit = rendering$failPerSecLimit;
				failBehavior = rendering$failBehavior;
			}
		}

		private static class ValkyrienSkies {
			RainEffect rainEffect = RainEffect.STATIONARY;
			boolean fixParticleLights = true;

			private void flat() {
				valkyrienSkies$rainEffect = requireNonNullElseGet(rainEffect, () -> new ValkyrienSkies().rainEffect);
				valkyrienSkies$fixParticleLights = fixParticleLights;
			}

			private void fold() {
				rainEffect = valkyrienSkies$rainEffect;
				fixParticleLights = valkyrienSkies$fixParticleLights;
			}
		}

		private static class Create {
			RainEffect rainEffect = RainEffect.ALWAYS;

			private void flat() {
				create$rainEffect = requireNonNullElseGet(rainEffect, () -> new Create().rainEffect);
			}

			private void fold() {
				rainEffect = create$rainEffect;
			}
		}
	}
}
