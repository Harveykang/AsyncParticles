package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.coremod.ClothConfigMixinMenus;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

// No more NoClassDefFoundError
class ClothConfigMenus {
	@SuppressWarnings("UnstableApiUsage")
	static ConfigBuilder screenBuilder(Screen screen) {
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(screen)
			.setTitle(Component.translatable("gui.asyncparticles"))
			.setTransparentBackground(true);
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		// region Particle Category
		AsyncParticlesConfig.ConfigObj defaultConfig = new AsyncParticlesConfig.ConfigObj();
		AsyncParticlesConfig.ConfigObj newConfig = new AsyncParticlesConfig.ConfigObj();
		AsyncParticlesConfig.ConfigObj globalConfig = AsyncParticlesConfig.getCurrentConfig();
		builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.particle"))
			.addEntry(entryBuilder
				.startIntField(Component.translatable("config.asyncparticles.particle.particleLimit"),
					globalConfig.particle.particleLimit)
				.setDefaultValue(defaultConfig.particle.particleLimit)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLimit.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.particleLimit = newValue)
				.setMin(AsyncParticlesConfig.MIN_PARTICLE_LIMIT)
				.setMax(AsyncParticlesConfig.MAX_PARTICLE_LIMIT)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.removeIfMissedTick"),
					globalConfig.particle.removeIfMissedTick)
				.setDefaultValue(defaultConfig.particle.removeIfMissedTick)
				.setTooltip(Component.translatable("config.asyncparticles.particle.removeIfMissedTick.tooltip"))
				.setSaveConsumer(newValue -> newConfig.particle.removeIfMissedTick = newValue)
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
		builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.tick"))
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.animationTickMode"),
					globalConfig.tick.animationTickMode)
				.setDefaultValue(defaultConfig.tick.animationTickMode)
				.setTooltip(Component.translatable("config.asyncparticles.tick.animationTickMode.tooltip"))
				.setSaveConsumer(newValue -> newConfig.tick.animationTickMode = newValue)
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
					if (ModListHelper.AXIOM_LOADED) {
						return Optional.of(new MutableComponent[]{
							Component.translatable("config.asyncparticles.tick.deferredTextureTick.tooltip")
								.withStyle(ChatFormatting.STRIKETHROUGH),
							Component.translatable("config.asyncparticles.incompatibility", "Axiom")
								.withStyle(ChatFormatting.YELLOW)
						});
					} else {
						return Optional.of(new MutableComponent[]{
							Component.translatable("config.asyncparticles.tick.deferredTextureTick.tooltip")
						});
					}
				})
				.setSaveConsumer(newValue -> newConfig.tick.deferredTextureTick = newValue)
				.setRequirement(() -> !ModListHelper.AXIOM_LOADED)
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
//			.addEntry(entryBuilder
//				.startEnumSelector(Component.translatable("config.asyncparticles.tick.failBehavior"),
//					FailBehavior.class, globalConfig.tick.failBehavior)
//				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
//				.setDefaultValue(defaultConfig.tick.failBehavior)
//				.setTooltip(
//					Component.translatable("config.asyncparticles.tick.failBehavior.tooltip")
//						.withStyle(ChatFormatting.STRIKETHROUGH),
//					Component.translatable("config.asyncparticles.not-implemented")
//						.withStyle(ChatFormatting.DARK_RED))
//				.setSaveConsumer(newValue -> newConfig.tick.failBehavior = newValue)
//				.setRequirement(() -> false)
//				.build())
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
		builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.rendering"))
//			.addEntry(entryBuilder
//				.startEnumSelector(Component.translatable("config.asyncparticles.rendering.particleRenderingMode"),
//					RenderingMode.class, globalConfig.rendering.particleRenderingMode)
//				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
//				.setDefaultValue(defaultConfig.rendering.particleRenderingMode)
//				.setTooltip(Component.translatable("config.asyncparticles.rendering.particleRenderingMode.tooltip"))
//				.setSaveConsumer(newValue -> newConfig.rendering.particleRenderingMode = newValue)
//				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.gpuAcceleration"),
					globalConfig.rendering.gpuAcceleration)
				.setDefaultValue(defaultConfig.rendering.gpuAcceleration)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.gpuAcceleration.tooltip"))
				// todo add gpu acceleration requirement
				.setSaveConsumer(newValue -> newConfig.rendering.gpuAcceleration = newValue)
				.setRequirement(GLCaps::supportsGpuAcceleration)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.appendNewParticlesToRenderer"),
					globalConfig.rendering.appendNewParticlesToRenderer)
				.setDefaultValue(defaultConfig.rendering.appendNewParticlesToRenderer)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.appendNewParticlesToRenderer.tooltip"))
				.setSaveConsumer(newValue -> newConfig.rendering.appendNewParticlesToRenderer = newValue)
				.build())
//			.addEntry(entryBuilder
//				.startSelector(Component.translatable("config.asyncparticles.rendering.particleCulling"),
//					ParticleCullingMode.values(), globalConfig.rendering.particleCulling)
//				.setNameProvider(ParticleCullingMode::getComponent)
//				.setDefaultValue(defaultConfig.rendering.particleCulling)
//				.setTooltip(Component.translatable("config.asyncparticles.rendering.particleCulling.tooltip"))
//				.setSaveConsumer(newValue -> newConfig.rendering.particleCulling = newValue)
//				.build())
//			.addEntry(entryBuilder
//				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.cullWeathers"),
//					globalConfig.rendering.cullWeathers)
//				.setDefaultValue(defaultConfig.rendering.cullWeathers)
//				.setTooltip(Component.translatable("config.asyncparticles.rendering.cullWeathers.tooltip"))
//				.setSaveConsumer(newValue -> newConfig.rendering.cullWeathers = newValue)
//				.build())
//			.addEntry(entryBuilder
//				.startIntField(Component.translatable("config.asyncparticles.rendering.failPerSecLimit"),
//					globalConfig.rendering.failPerSecLimit)
//				.setDefaultValue(defaultConfig.rendering.failPerSecLimit)
//				.setTooltip(Component.translatable("config.asyncparticles.rendering.failPerSecLimit.tooltip"))
//				.setSaveConsumer(newValue -> newConfig.rendering.failPerSecLimit = newValue)
//				.setMin(0)
//				.setMax(256)
//				.build())
//			.addEntry(entryBuilder
//				.startEnumSelector(Component.translatable("config.asyncparticles.rendering.failBehavior"),
//					FailBehavior.class, globalConfig.rendering.failBehavior)
//				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
//				.setDefaultValue(defaultConfig.rendering.failBehavior)
//				.setTooltip(
//					Component.translatable("config.asyncparticles.rendering.failBehavior.tooltip")
//						.withStyle(ChatFormatting.STRIKETHROUGH),
//					Component.translatable("config.asyncparticles.not-implemented")
//						.withStyle(ChatFormatting.DARK_RED))
//				.setSaveConsumer(newValue -> newConfig.rendering.failBehavior = newValue)
//				.setRequirement(() -> false)
//				.build())
			.addEntry(entryBuilder
				.startStrList(Component.translatable("config.asyncparticles.rendering.syncParticleClasses"),
					new ArrayList<>(globalConfig.rendering.syncParticleClasses))
				.setDefaultValue(new ArrayList<>(defaultConfig.rendering.syncParticleClasses))
				.setTooltip(Component.translatable("config.asyncparticles.rendering.syncParticleClasses.tooltip"))
				.setSaveConsumer(newValue -> newConfig.rendering.syncParticleClasses = new LinkedHashSet<>(newValue))
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
			.setRequirement(() -> ModListHelper.VS_LOADED)
			.build());
		vsEntries.add(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights"),
				globalConfig.valkyrienSkies.fixParticleLights)
			.setDefaultValue(defaultConfig.valkyrienSkies.fixParticleLights)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights.tooltip"))
			.setSaveConsumer(newValue -> newConfig.valkyrienSkies.fixParticleLights = newValue)
			.setRequirement(() -> ModListHelper.VS_LOADED)
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
			.setRequirement(() -> ModListHelper.CREATE_LOADED)
			.build());
		createEntries.add(entryBuilder
			.startIntField(Component.translatable("config.asyncparticles.mod-compat.create.tickRainBlockingRange"),
				globalConfig.create.tickRainBlockingRange)
			.setDefaultValue(defaultConfig.create.tickRainBlockingRange)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.create.tickRainBlockingRange.tooltip"))
			.setSaveConsumer(newValue -> newConfig.create.tickRainBlockingRange = newValue)
			.setRequirement(() -> ModListHelper.CREATE_LOADED)
			.build());

		// Mixin
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
		Runnable mixinSaveRunnable = ClothConfigMixinMenus.buildCategory(mixinCategory, entryBuilder, mixinEntryBuilder);
		// endregion

		builder.setSavingRunnable(() -> {
			try {
				newConfig.flat();
				AsyncParticlesConfig.save();
				mixinSaveRunnable.run();
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
}
