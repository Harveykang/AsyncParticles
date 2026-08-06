package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backend;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.coremod.ClothConfigMixinMenus;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.*;
import java.util.stream.Collector;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.MAX_PARTICLE_LIMIT;
import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.MIN_PARTICLE_LIMIT;

// No more NoClassDefFoundError
class ClothConfigMenus {
	@SuppressWarnings("UnstableApiUsage")
	static ConfigBuilder screenBuilder(Screen screen) {
		AsyncParticlesConfig.ConfigObj defaultConfig = new AsyncParticlesConfig.ConfigObj();
		AsyncParticlesConfig.ConfigObj newConfig = new AsyncParticlesConfig.ConfigObj();
		AsyncParticlesConfig.ConfigObj globalConfig = AsyncParticlesConfig.getCurrentConfig();
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(screen)
			.setTitle(Component.translatable("gui.asyncparticles"))
			.setTransparentBackground(true);
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		// region Particle Category
		ConfigCategory particleCategory = builder
			.getOrCreateCategory(Component.translatable("config.asyncparticles.category.particle"));
		particleCategory
			.addEntry(entryBuilder
				.startIntField(Component.translatable("config.asyncparticles.particle.particleLimit"),
					globalConfig.particle.particleLimit)
				.setDefaultValue(defaultConfig.particle.particleLimit)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLimit.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.particleLimit = newValue)
				.setMin(MIN_PARTICLE_LIMIT)
				.setMax(MAX_PARTICLE_LIMIT)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.particle.cleanupStrategy"),
					ParticleCleanupStrategy.class, globalConfig.particle.cleanupStrategy)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.particle.cleanupStrategy)
				.setTooltip(Component.translatable("config.asyncparticles.particle.cleanupStrategy.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.cleanupStrategy = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.parallelQueueRemoval"),
					globalConfig.particle.parallelQueueRemoval)
				.setDefaultValue(defaultConfig.particle.parallelQueueRemoval)
				.setTooltip(Component.translatable("config.asyncparticles.particle.parallelQueueRemoval.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.parallelQueueRemoval = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.parallelQueueEviction"),
					globalConfig.particle.parallelQueueEviction)
				.setDefaultValue(defaultConfig.particle.parallelQueueEviction)
				.setTooltip(Component.translatable("config.asyncparticles.particle.parallelQueueEviction.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.parallelQueueEviction = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.particleLightCache"),
					globalConfig.particle.particleLightCache)
				.setDefaultValue(defaultConfig.particle.particleLightCache)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLightCache.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.particleLightCache = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.cullUnderwaterParticleType"),
					globalConfig.particle.cullUnderwaterParticleType)
				.setDefaultValue(defaultConfig.particle.cullUnderwaterParticleType)
				.setTooltip(Component.translatable("config.asyncparticles.particle.cullUnderwaterParticleType.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.cullUnderwaterParticleType = newValue)
				.build());
		// endregion
		// region Tick Category
		ConfigCategory tickCategory = builder
			.getOrCreateCategory(Component.translatable("config.asyncparticles.category.tick"));
		tickCategory
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.animationTickMode"),
					globalConfig.tick.animationTickMode)
				.setDefaultValue(defaultConfig.tick.animationTickMode)
				.setTooltip(Component.translatable("config.asyncparticles.tick.animationTickMode.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.animationTickMode = newValue)
				.setTooltipSupplier(() -> {
					if (REIGNOFNETHER_LOADED || IMMERSIVE_PORTALS_LOADED) {
						return incompatibilityTooltip(
							Component.translatable("config.asyncparticles.tick.animationTickMode.tooltip"),
							REIGNOFNETHER_LOADED ? "Reign of Nether" : null,
							IMMERSIVE_PORTALS_LOADED ? "Immersive Portals" : null);
					} else {
						return Optional.of(new MutableComponent[]{
							Component.translatable("config.asyncparticles.tick.animationTickMode.tooltip")
						});
					}
				})
				.setRequirement(() -> !REIGNOFNETHER_LOADED && !IMMERSIVE_PORTALS_LOADED)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.tick.particleAsyncMode"),
					ParticleAsyncMode.class, globalConfig.tick.particleAsyncMode)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.tick.particleAsyncMode)
				.setTooltip(Component.translatable("config.asyncparticles.tick.particleAsyncMode.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.particleAsyncMode = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.gpuOnlyAsyncParticleTick"),
					globalConfig.tick.gpuOnlyAsyncParticleTick)
				.setDefaultValue(defaultConfig.tick.gpuOnlyAsyncParticleTick)
				.setTooltip(Component.translatable("config.asyncparticles.tick.gpuOnlyAsyncParticleTick.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.gpuOnlyAsyncParticleTick = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.tickWeatherAsync"),
					globalConfig.tick.tickWeatherAsync)
				.setDefaultValue(defaultConfig.tick.tickWeatherAsync)
				.setTooltip(Component.translatable("config.asyncparticles.tick.tickWeatherAsync.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.tickWeatherAsync = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.deferredTextureTick"),
					globalConfig.tick.deferredTextureTick)
				.setDefaultValue(defaultConfig.tick.deferredTextureTick)
				.setTooltipSupplier(() -> {
					if (AXIOM_LOADED) {
						return incompatibilityTooltip(
							Component.translatable("config.asyncparticles.tick.deferredTextureTick.tooltip"),
							"Axiom");
					} else {
						return Optional.of(new MutableComponent[]{
							Component.translatable("config.asyncparticles.tick.deferredTextureTick.tooltip")
						});
					}
				})
				.setSaveConsumer(newValue -> newConfig.tick.deferredTextureTick = newValue)
				.setRequirement(() -> !AXIOM_LOADED)
				.build())
			.addEntry(entryBuilder
				.startIntField(Component.translatable("config.asyncparticles.tick.failPerSecLimit"),
					globalConfig.tick.failPerSecLimit)
				.setDefaultValue(defaultConfig.tick.failPerSecLimit)
				.setTooltip(Component.translatable("config.asyncparticles.tick.failPerSecLimit.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.failPerSecLimit = newValue)
				.setMin(0)
				.setMax(256)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.tick.failBehavior"),
					FailBehavior.class, globalConfig.tick.failBehavior)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.tick.failBehavior)
				.setTooltip(
					Component.translatable("config.asyncparticles.tick.failBehavior.tooltip")
						.withStyle(ChatFormatting.STRIKETHROUGH),
					Component.translatable("config.asyncparticles.not-implemented")
						.withStyle(ChatFormatting.DARK_RED))
				.setSaveConsumer(newValue -> newConfig.tick.failBehavior = newValue)
				.setRequirement(() -> false)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.suppressCME"),
					globalConfig.tick.suppressCME)
				.setDefaultValue(defaultConfig.tick.suppressCME)
				.setTooltip(Component.translatable("config.asyncparticles.tick.suppressCME.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.suppressCME = newValue)
				.build())
			.addEntry(entryBuilder
				.startStrList(Component.translatable("config.asyncparticles.tick.syncParticleClasses"),
					new ArrayList<>(globalConfig.tick.syncParticleClasses))
				.setDefaultValue(new ArrayList<>(defaultConfig.tick.syncParticleClasses))
				.setTooltip(Component.translatable("config.asyncparticles.tick.syncParticleClasses.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.syncParticleClasses = new LinkedHashSet<>(newValue))
				.build());
		// endregion
		// region Rendering Category
		ConfigCategory renderingCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.rendering"));
		renderingCategory
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.gpuAcceleration"),
					globalConfig.rendering.gpuAcceleration)
				.setDefaultValue(defaultConfig.rendering.gpuAcceleration)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.gpuAcceleration.tooltip"))
				.setSaveConsumer(newValue -> newConfig.rendering.gpuAcceleration = newValue)
				.setRequirement(Backends::supportsGpuAcceleration)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.appendNewParticlesToRenderer"),
					globalConfig.rendering.appendNewParticlesToRenderer)
				.setDefaultValue(defaultConfig.rendering.appendNewParticlesToRenderer)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.appendNewParticlesToRenderer.tooltip"))
				.setSaveConsumer(newValue -> newConfig.rendering.appendNewParticlesToRenderer = newValue)
				.build())
			.addEntry(entryBuilder
				.startSelector(Component.translatable("config.asyncparticles.rendering.particleCulling"),
					ParticleCullingMode.values(), globalConfig.rendering.particleCulling)
				.setNameProvider(ParticleCullingMode::getComponent)
				.setDefaultValue(defaultConfig.rendering.particleCulling)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.particleCulling.tooltip"))
				.setSaveConsumer(newValue -> newConfig.rendering.particleCulling = newValue)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.rendering.computeExecutionStage"),
					ComputeExecutionStage.class, globalConfig.rendering.computeExecutionStage)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.rendering.computeExecutionStage)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.computeExecutionStage.tooltip"))
				.setSaveConsumer(newValue -> newConfig.rendering.computeExecutionStage = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.cullWeathers"),
					globalConfig.rendering.cullWeathers)
				.setDefaultValue(defaultConfig.rendering.cullWeathers)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.cullWeathers.tooltip"))
				.setSaveConsumer(newValue -> newConfig.rendering.cullWeathers = newValue)
				.build());
		// endregion
		// region Compat Category

		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> vsEntries = new ArrayList<>();
		vsEntries.add(entryBuilder
			.startSelector(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect"),
				RainEffect.values(), globalConfig.valkyrienSkies.rainEffect)
			.setNameProvider(RainEffect::getComponent)
			.setDefaultValue(defaultConfig.valkyrienSkies.rainEffect)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect.tooltip"))
			.setSaveConsumer(newValue -> newConfig.valkyrienSkies.rainEffect = newValue)
			.setRequirement(() -> VS_LOADED)
			.build());
		vsEntries.add(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights"),
				globalConfig.valkyrienSkies.fixParticleLights)
			.setDefaultValue(defaultConfig.valkyrienSkies.fixParticleLights)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights.tooltip"))
			.setSaveConsumer(newValue -> newConfig.valkyrienSkies.fixParticleLights = newValue)
			.setRequirement(() -> VS_LOADED)
			.build());

		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> sableEntries = new ArrayList<>();
		sableEntries.add(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mod-compat.sable.fixParticleLights"),
				globalConfig.sable.fixParticleLights)
			.setDefaultValue(defaultConfig.sable.fixParticleLights)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.sable.fixParticleLights.tooltip"))
			.setSaveConsumer(newValue -> newConfig.sable.fixParticleLights = newValue)
			.setRequirement(() -> SABLE_LOADED)
			.build());

		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> createEntries = new ArrayList<>();
		createEntries.add(entryBuilder
			.startEnumSelector(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect"),
				RainEffect.class, globalConfig.create.rainEffect)
			.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
			.setDefaultValue(defaultConfig.create.rainEffect)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect.tooltip"))
			.setSaveConsumer(newValue -> newConfig.create.rainEffect = newValue)
			.setRequirement(() -> CREATE_LOADED)
			.build());
		createEntries.add(entryBuilder
			.startIntField(Component.translatable("config.asyncparticles.mod-compat.create.tickRainBlockingRange"),
				globalConfig.create.tickRainBlockingRange)
			.setDefaultValue(defaultConfig.create.tickRainBlockingRange)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.create.tickRainBlockingRange.tooltip"))
			.setSaveConsumer(newValue -> newConfig.create.tickRainBlockingRange = newValue)
			.setRequirement(() -> CREATE_LOADED)
			.build());
		// endregion

		// region Mixin
		ConfigEntryBuilder mixinEntryBuilder = builder.entryBuilder();
		mixinEntryBuilder.setResetButtonKey(Component.translatable("gui.asyncparticles.revert"));
		ClothConfigMixinMenus.addModCompatCategory(entryBuilder, mixinEntryBuilder, vsEntries, sableEntries, createEntries);

		ConfigCategory modCompatCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mod-compat"));
		modCompatCategory
			.addEntry(new SubCategoryListEntryFix(entryBuilder
				// .startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
				.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
					vsEntries)
				.build()))
			.addEntry(new SubCategoryListEntryFix(entryBuilder
				// .startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.valkyrienskies"),
				.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.sable"),
					sableEntries)
				.build()))
			.addEntry(new SubCategoryListEntryFix(entryBuilder
				// .startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.create"),
				.startSubCategory(Component.translatable("config.asyncparticles.category.mod-compat.create"),
					createEntries)
				.build()));

		ConfigCategory mixinCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mixin"));
		Runnable mixinSaveRunnable = ClothConfigMixinMenus.buildCategory(mixinCategory, entryBuilder, mixinEntryBuilder);
		// endregion

		// region Mobile
		if (Backends.backend == Backend.OPENGL_ON_ES) {
			builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mobile"))
				.addEntry(entryBuilder
					.startBooleanToggle(Component.translatable("config.asyncparticles.mobile.multiDrawWorkaround"), globalConfig.mobile.multiDrawWorkaround)
					.setTooltip(Component.translatable("config.asyncparticles.mobile.multiDrawWorkaround.tooltip"))
					.setDefaultValue(defaultConfig.mobile.multiDrawWorkaround)
					.setSaveConsumer(newValue -> newConfig.mobile.multiDrawWorkaround = newValue)
					.build());
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
					DevRuntimeDebug.TriState.class, DevRuntimeDebug.directStateAccess)
				.setTooltip(Component.literal("Never change it unless requested by the developer."))
				.setDefaultValue(DevRuntimeDebug.TriState.DEFAULT)
				.setSaveConsumer(newValue -> DevRuntimeDebug.directStateAccess = newValue)
				.build())
			.addEntry(entryBuilder.startTextDescription(Component.literal(Backends.debugInfo())
					.withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, Backends.debugInfo()))))
				.setTooltip(Component.literal("Click the text to copy to clipboard."))
				.build());
		// endregion

		builder.setSavingRunnable(() -> {
			try {
				newConfig.flat();
				AsyncParticlesConfig.save();
				mixinSaveRunnable.run();
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

		return builder;
	}

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
}
