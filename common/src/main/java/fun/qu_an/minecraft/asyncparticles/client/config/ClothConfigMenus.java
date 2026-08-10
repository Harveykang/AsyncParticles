package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.cloth_config.AbstractConfigEntryAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.cloth_config.AbstractListListEntryAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ClothConfigMixinMenus.MixinConfigBundle;
import fun.qu_an.minecraft.asyncparticles.client.config.CompatibilityTryItButton.CompatibilityTryIt;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backend;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collector;

// No more NoClassDefFoundError
class ClothConfigMenus {
	public static Screen screen(Screen parent) {
		return screen(parent, ConfigBundle.create(), MixinConfigBundle.create(), 0, null);
	}

	@SuppressWarnings("UnstableApiUsage")
	static Screen screen(Screen parent,
	                     ConfigBundle bundle,
	                     MixinConfigBundle mixinBundle,
	                     int selectedCategoryIndex,
	                     @Nullable CompatibilityTryIt compatibilityTryIt) {
		AsyncParticlesConfig.ConfigObj oldConfig = bundle.oldConfig();
		AsyncParticlesConfig.ConfigObj defaultConfig = bundle.defaultConfig();
		AsyncParticlesConfig.ConfigObj savingConfig = bundle.savingConfig();
		AsyncParticlesConfig.ConfigObj unsavedConfig = bundle.unsavedConfig();

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("gui.asyncparticles"))
			.setTransparentBackground(true);
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		// region Particle Category
		builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.particle"))
			.addEntry(modifyOriginal(entryBuilder
				.startIntField(Component.translatable("config.asyncparticles.particle.particleLimit"),
					oldConfig.particle.particleLimit)
				.setDefaultValue(defaultConfig.particle.particleLimit)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLimit.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.particle.particleLimit = newValue)
				.setMin(AsyncParticlesConfig.MIN_PARTICLE_LIMIT)
				.setMax(AsyncParticlesConfig.MAX_PARTICLE_LIMIT)
				.build(), unsavedConfig.particle.particleLimit))
			.addEntry(modifyOriginal(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.particle.cleanupStrategy"),
					ParticleCleanupStrategy.class, oldConfig.particle.cleanupStrategy)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.particle.cleanupStrategy)
				.setTooltip(Component.translatable("config.asyncparticles.particle.cleanupStrategy.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.particle.cleanupStrategy = newValue)
				.build(), unsavedConfig.particle.cleanupStrategy))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.parallelQueueRemoval"),
					oldConfig.particle.parallelQueueRemoval)
				.setDefaultValue(defaultConfig.particle.parallelQueueRemoval)
				.setTooltip(Component.translatable("config.asyncparticles.particle.parallelQueueRemoval.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.particle.parallelQueueRemoval = newValue)
				.build(), unsavedConfig.particle.parallelQueueRemoval))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.parallelQueueEviction"),
					oldConfig.particle.parallelQueueEviction)
				.setDefaultValue(defaultConfig.particle.parallelQueueEviction)
				.setTooltip(Component.translatable("config.asyncparticles.particle.parallelQueueEviction.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.particle.parallelQueueEviction = newValue)
				.build(), unsavedConfig.particle.parallelQueueEviction))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.particleLightCache"),
					oldConfig.particle.particleLightCache)
				.setDefaultValue(defaultConfig.particle.particleLightCache)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLightCache.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.particle.particleLightCache = newValue)
				.build(), unsavedConfig.particle.particleLightCache))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.cullUnderwaterParticleType"),
					oldConfig.particle.cullUnderwaterParticleType)
				.setDefaultValue(defaultConfig.particle.cullUnderwaterParticleType)
				.setTooltip(Component.translatable("config.asyncparticles.particle.cullUnderwaterParticleType.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.particle.cullUnderwaterParticleType = newValue)
				.build(), unsavedConfig.particle.cullUnderwaterParticleType));
		// endregion
		// region Tick Category
		builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.tick"))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.animationTickMode"),
					oldConfig.tick.animationTickMode)
				.setDefaultValue(defaultConfig.tick.animationTickMode)
				.setTooltip(Component.translatable("config.asyncparticles.tick.animationTickMode.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.tick.animationTickMode = newValue)
				.build(), unsavedConfig.tick.animationTickMode))
			.addEntry(modifyOriginal(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.tick.particleAsyncMode"),
					ParticleAsyncMode.class, oldConfig.tick.particleAsyncMode)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.tick.particleAsyncMode)
				.setTooltip(Component.translatable("config.asyncparticles.tick.particleAsyncMode.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.tick.particleAsyncMode = newValue)
				.build(), unsavedConfig.tick.particleAsyncMode))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.gpuOnlyAsyncParticleTick"),
					oldConfig.tick.gpuOnlyAsyncParticleTick)
				.setDefaultValue(defaultConfig.tick.gpuOnlyAsyncParticleTick)
				.setTooltip(Component.translatable("config.asyncparticles.tick.gpuOnlyAsyncParticleTick.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.tick.gpuOnlyAsyncParticleTick = newValue)
				.build(), unsavedConfig.tick.gpuOnlyAsyncParticleTick))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.tickWeatherAsync"),
					oldConfig.tick.tickWeatherAsync)
				.setDefaultValue(defaultConfig.tick.tickWeatherAsync)
				.setTooltip(Component.translatable("config.asyncparticles.tick.tickWeatherAsync.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.tick.tickWeatherAsync = newValue)
				.build(), unsavedConfig.tick.tickWeatherAsync))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.deferredTextureTick"),
					oldConfig.tick.deferredTextureTick)
				.setDefaultValue(defaultConfig.tick.deferredTextureTick)
				.setTooltipSupplier(() -> {
					if (ModListHelper.AXIOM_LOADED) {
						return incompatibilityTooltip(
							Component.translatable("config.asyncparticles.tick.deferredTextureTick.tooltip"),
							"Axiom");
					} else {
						return Optional.of(new MutableComponent[]{
							Component.translatable("config.asyncparticles.tick.deferredTextureTick.tooltip")
						});
					}
				})
				.setSaveConsumer(newValue -> savingConfig.tick.deferredTextureTick = newValue)
				.setRequirement(() -> !ModListHelper.AXIOM_LOADED)
				.build(), unsavedConfig.tick.deferredTextureTick))
			.addEntry(modifyOriginal(entryBuilder
				.startIntField(Component.translatable("config.asyncparticles.tick.failPerSecLimit"),
					oldConfig.tick.failPerSecLimit)
				.setDefaultValue(defaultConfig.tick.failPerSecLimit)
				.setTooltip(Component.translatable("config.asyncparticles.tick.failPerSecLimit.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.tick.failPerSecLimit = newValue)
				.setMin(0)
				.setMax(256)
				.build(), unsavedConfig.tick.failPerSecLimit))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.suppressCME"),
					oldConfig.tick.suppressCME)
				.setDefaultValue(defaultConfig.tick.suppressCME)
				.setTooltip(Component.translatable("config.asyncparticles.tick.suppressCME.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.tick.suppressCME = newValue)
				.build(), unsavedConfig.tick.suppressCME))
			.addEntry(modifyOriginal(entryBuilder
				.startStrList(Component.translatable("config.asyncparticles.tick.syncParticleClasses"),
					new ArrayList<>(oldConfig.tick.syncParticleClasses))
				.setDefaultValue(new ArrayList<>(defaultConfig.tick.syncParticleClasses))
				.setTooltip(Component.translatable("config.asyncparticles.tick.syncParticleClasses.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.tick.syncParticleClasses = new LinkedHashSet<>(newValue))
				.build(), unsavedConfig.tick.syncParticleClasses));
		// endregion
		// region Rendering Category
		builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.rendering"))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.gpuAcceleration"),
					oldConfig.rendering.gpuAcceleration)
				.setDefaultValue(defaultConfig.rendering.gpuAcceleration)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.gpuAcceleration.tooltip"))
				// todo add gpu acceleration requirement
				.setSaveConsumer(newValue -> savingConfig.rendering.gpuAcceleration = newValue)
				.setRequirement(Backends::supportsGpuAcceleration)
				.build(), unsavedConfig.rendering.gpuAcceleration))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.appendNewParticlesToRenderer"),
					oldConfig.rendering.appendNewParticlesToRenderer)
				.setDefaultValue(defaultConfig.rendering.appendNewParticlesToRenderer)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.appendNewParticlesToRenderer.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.rendering.appendNewParticlesToRenderer = newValue)
				.build(), unsavedConfig.rendering.appendNewParticlesToRenderer))
			.addEntry(modifyOriginal(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.rendering.computeExecutionStage"),
					ComputeExecutionStage.class, oldConfig.rendering.computeExecutionStage)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.rendering.computeExecutionStage)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.computeExecutionStage.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.rendering.computeExecutionStage = newValue)
				.build(), unsavedConfig.rendering.computeExecutionStage))
			.addEntry(modifyOriginal(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.tickRendererOnMainThread"),
					oldConfig.rendering.tickRendererOnMainThread)
				.setDefaultValue(defaultConfig.rendering.tickRendererOnMainThread)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.tickRendererOnMainThread.tooltip"))
				.setSaveConsumer(newValue -> savingConfig.rendering.tickRendererOnMainThread = newValue)
				.build(), unsavedConfig.rendering.tickRendererOnMainThread));
		// endregion

		// region Compat Category
		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> vsEntries = new ArrayList<>();
		vsEntries.add(modifyOriginal(entryBuilder
			.startSelector(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect"),
				RainEffect.values(), oldConfig.valkyrienSkies.rainEffect)
			.setNameProvider(RainEffect::getComponent)
			.setDefaultValue(defaultConfig.valkyrienSkies.rainEffect)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect.tooltip"))
			.setSaveConsumer(newValue -> savingConfig.valkyrienSkies.rainEffect = newValue)
			.setRequirement(() -> ModListHelper.VS_LOADED)
			.build(), unsavedConfig.valkyrienSkies.rainEffect));
		vsEntries.add(modifyOriginal(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights"),
				oldConfig.valkyrienSkies.fixParticleLights)
			.setDefaultValue(defaultConfig.valkyrienSkies.fixParticleLights)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights.tooltip"))
			.setSaveConsumer(newValue -> savingConfig.valkyrienSkies.fixParticleLights = newValue)
			.setRequirement(() -> ModListHelper.VS_LOADED)
			.build(), unsavedConfig.valkyrienSkies.fixParticleLights));

		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> createEntries = new ArrayList<>();
		createEntries.add(modifyOriginal(entryBuilder
			.startEnumSelector(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect"),
				RainEffect.class, oldConfig.create.rainEffect)
			.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
			.setDefaultValue(defaultConfig.create.rainEffect)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect.tooltip"))
			.setSaveConsumer(newValue -> savingConfig.create.rainEffect = newValue)
			.setRequirement(() -> ModListHelper.CREATE_LOADED)
			.build(), unsavedConfig.create.rainEffect));
		createEntries.add(modifyOriginal(entryBuilder
			.startIntField(Component.translatable("config.asyncparticles.mod-compat.create.tickRainBlockingRange"),
				oldConfig.create.tickRainBlockingRange)
			.setDefaultValue(defaultConfig.create.tickRainBlockingRange)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.create.tickRainBlockingRange.tooltip"))
			.setSaveConsumer(newValue -> savingConfig.create.tickRainBlockingRange = newValue)
			.setRequirement(() -> ModListHelper.CREATE_LOADED)
			.build(), unsavedConfig.create.tickRainBlockingRange));
		// endregion

		// region Mixin
		ConfigEntryBuilder mixinEntryBuilder = builder.entryBuilder();
		mixinEntryBuilder.setResetButtonKey(Component.translatable("gui.asyncparticles.revert"));
		ClothConfigMixinMenus.addModCompatCategory(entryBuilder, mixinEntryBuilder, vsEntries, createEntries);

		builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mod-compat"))
			.addEntry(new SubCategoryListEntryFix(entryBuilder
				// .startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
				.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
					vsEntries)
				.build()))
			.addEntry(new SubCategoryListEntryFix(entryBuilder
				// .startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.create"),
				.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.create"),
					createEntries)
				.build()));

		ConfigCategory mixinCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mixin"));
		ClothConfigMixinMenus.buildCategory(mixinBundle, mixinCategory, entryBuilder, mixinEntryBuilder);
		// endregion

		// region Mobile
		if (Backends.backend == Backend.OPENGL_ON_ES) {
			builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mobile"))
				.addEntry(modifyOriginal(entryBuilder
					.startBooleanToggle(Component.translatable("config.asyncparticles.mobile.multiDrawWorkaround"), oldConfig.mobile.multiDrawWorkaround)
					.setTooltip(Component.translatable("config.asyncparticles.mobile.multiDrawWorkaround.tooltip"))
					.setDefaultValue(defaultConfig.mobile.multiDrawWorkaround)
					.setSaveConsumer(newValue -> savingConfig.mobile.multiDrawWorkaround = newValue)
					.build(), unsavedConfig.mobile.multiDrawWorkaround));
		}
		// endregion

		// region Dev
		builder.getOrCreateCategory(Component.literal("Dev"))
			.addEntry(entryBuilder
				.startBooleanToggle(Component.literal("Debug Sync Particle Classes"), DevRuntimeDebug.syncAllParticles)
				.setTooltip(Component.literal("Never change it unless requested by the developer."))
				.setDefaultValue(false)
				.setSaveConsumer(newValue -> DevRuntimeDebug.syncAllParticles = newValue)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.literal("GL TransformFeedback Command"),
					DevRuntimeDebug.TransformFeedbackGlCommand.class, DevRuntimeDebug.transformFeedbackGlCommand)
				.setTooltip(Component.literal("Never change it unless requested by the developer."))
				.setDefaultValue(DevRuntimeDebug.TransformFeedbackGlCommand.AUTO)
				.setSaveConsumer(newValue -> DevRuntimeDebug.transformFeedbackGlCommand = newValue)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.literal("GL Direct State Access"),
					TriState.class, DevRuntimeDebug.directStateAccess)
				.setTooltip(Component.literal("Never change it unless requested by the developer."))
				.setDefaultValue(TriState.DEFAULT)
				.setSaveConsumer(newValue -> DevRuntimeDebug.directStateAccess = newValue)
				.build())
			.addEntry(entryBuilder.startTextDescription(Component.literal(Backends.debugInfo())
					.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(Backends.debugInfo()))))
				.setTooltip(Component.literal("Click the text to copy to clipboard."))
				.build());
		// endregion

		AsyncParticlesMixinConfig.MixinConfigObj savingMixinConfig = mixinBundle.savingConfig();
		builder.setSavingRunnable(() -> {
			try {
				savingConfig.flat();
				AsyncParticlesConfig.save();
				savingMixinConfig.flat();
				AsyncParticlesMixinConfig.save();
				DevRuntimeDebug.apply();
			} catch (Exception e) {
				AsyncParticlesConfig.LOGGER.error("Failed to save config", e);
				Minecraft mc = Minecraft.getInstance();
				Screen configScreen = mc.screen;
				ThreadUtil.enqueueClientTask(() -> {
					Screen prevScreen = mc.screen;
					mc.setScreen(new FallbackScreen(
						null,
						Component.translatable("gui.asyncparticles.error"),
						Component.translatable("gui.asyncparticles.failed-to-save", e.toString()),
						Component.translatable("gui.back"),
						current -> Minecraft.getInstance().setScreen(configScreen),
						Component.translatable("gui.continue"),
						current -> Minecraft.getInstance().setScreen(prevScreen)));
				});
			}
			AsyncTickBehavior.getInstance().reloadLater();
		});

		AsyncParticlesMixinConfig.MixinConfigObj unsavedMixinConfig = mixinBundle.unsavedConfig();
		builder.setAfterInitConsumer(screen -> screen.addRenderableWidget(
			new CompatibilityTryItButton((AbstractConfigScreen) screen, unsavedConfig, savingConfig, unsavedMixinConfig, savingMixinConfig, compatibilityTryIt)));

		Screen screen = builder.build();
		((AbstractConfigScreen) screen).selectedCategoryIndex = selectedCategoryIndex;
		return screen;
	}

	@SuppressWarnings({"UnstableApiUsage", "unchecked"})
	static @NotNull <T, C extends AbstractListListEntry.AbstractListCell<T, C, SELF>, SELF extends AbstractListListEntry<T, C, SELF>> AbstractListListEntry<T, C, SELF>
	modifyOriginal(@NotNull AbstractListListEntry<T, C, SELF> entry, Collection<T> list) {
		((AbstractListListEntryAddon<T>) entry).asyncparticles$setOriginal(new ArrayList<>(list));
		return entry;
	}

	@SuppressWarnings({"unchecked"})
	static <T> AbstractConfigListEntry<T> modifyOriginal(@NotNull AbstractConfigListEntry<T> entry, T original) {
		((AbstractConfigEntryAddon<T>) entry).asyncparticles$setOriginal(original);
		return entry;
	}

	@SuppressWarnings("SameParameterValue")
	private static Optional<Component[]> incompatibilityTooltip(MutableComponent description, Object... modNames) {
		Component modNamesStr = Arrays.stream(modNames)
			.filter(Objects::nonNull)
			.map(modName -> modName instanceof Component ? (Component) modName : Component.literal(String.valueOf(modName)))
			.collect(Collector.of(Component::empty, MutableComponent::append, (a, b) -> a.append(", ").append(b)));

		return Optional.of(new MutableComponent[]{
			description.withStyle(ChatFormatting.STRIKETHROUGH),
			Component.translatable("config.asyncparticles.incompatibility", modNamesStr)
				.withStyle(ChatFormatting.YELLOW)
		});
	}

	record ConfigBundle(
		AsyncParticlesConfig.ConfigObj oldConfig,
		AsyncParticlesConfig.ConfigObj defaultConfig,
		AsyncParticlesConfig.ConfigObj savingConfig,
		AsyncParticlesConfig.@Nullable ConfigObj unsavedConfig) {
		public static ConfigBundle create() {
			return new ConfigBundle(AsyncParticlesConfig.getCurrentConfig(), AsyncParticlesConfig.getDefaultConfig(), AsyncParticlesConfig.getDefaultConfig(), null);
		}

		@Override
		public AsyncParticlesConfig.ConfigObj unsavedConfig() {
			return unsavedConfig == null ? oldConfig : unsavedConfig;
		}
	}
}
