package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.cooparticlesapi.CooTickMode;
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
import java.util.List;
import java.util.Optional;

import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.*;

// No more NoClassDefFoundError
class ClothConfigMenus {
	static ConfigBuilder screenBuilder(Screen screen) {
		ConfigObj defaultConfig = new ConfigObj();
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
					particle$particleLimit)
				.setDefaultValue(defaultConfig.particle.particleLimit)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLimit.tooltip"))
				.setSaveConsumer(newValue -> particle$particleLimit = newValue)
				.setMin(1024)
				.setMax(262144)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.removeIfMissedTick"),
					particle$removeIfMissedTick)
				.setDefaultValue(defaultConfig.particle.removeIfMissedTick)
				.setTooltip(Component.translatable("config.asyncparticles.particle.removeIfMissedTick.tooltip"))
				.setSaveConsumer(newValue -> particle$removeIfMissedTick = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.particleLightCache"),
					particle$particleLightCache)
				.setDefaultValue(defaultConfig.particle.particleLightCache)
				.setTooltip(Component.translatable("config.asyncparticles.particle.particleLightCache.tooltip"))
				.setSaveConsumer(newValue -> particle$particleLightCache = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.particle.cullUnderwaterParticleType"),
					particle$cullUnderwaterParticleType)
				.setDefaultValue(defaultConfig.particle.cullUnderwaterParticleType)
				.setTooltip(Component.translatable("config.asyncparticles.particle.cullUnderwaterParticleType.tooltip"))
				.setSaveConsumer(newValue -> particle$cullUnderwaterParticleType = newValue)
				.build());
		// endregion
		// region Tick Category
		ConfigCategory tickCategory = builder
			.getOrCreateCategory(Component.translatable("config.asyncparticles.category.tick"));
		tickCategory
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.tick.animationTickMode"),
					TickMode.class, tick$animationTickMode)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.tick.animationTickMode)
				.setTooltip(Component.translatable("config.asyncparticles.tick.animationTickMode.tooltip"))
				.setSaveConsumer(newValue -> tick$animationTickMode = newValue)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.tick.particleTickMode"),
					TickMode.class, tick$particleTickMode)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.tick.particleTickMode)
				.setTooltip(Component.translatable("config.asyncparticles.tick.particleTickMode.tooltip"))
				.setSaveConsumer(newValue -> tick$particleTickMode = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.tickWeatherAsync"),
					tick$tickWeatherAsync)
				.setDefaultValue(defaultConfig.tick.tickWeatherAsync)
				.setTooltip(Component.translatable("config.asyncparticles.tick.tickWeatherAsync.tooltip"))
				.setSaveConsumer(newValue -> tick$tickWeatherAsync = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.tick.deferredTextureTick"),
					tick$deferredTextureTick)
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
				.setSaveConsumer(newValue -> tick$deferredTextureTick = newValue)
				.setRequirement(() -> !ModListHelper.AXIOM_LOADED)
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
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.tick.failBehavior)
				.setTooltip(
					Component.translatable("config.asyncparticles.tick.failBehavior.tooltip")
						.withStyle(ChatFormatting.STRIKETHROUGH),
					Component.translatable("config.asyncparticles.not-implemented")
						.withStyle(ChatFormatting.DARK_RED))
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
		// endregion
		// region Rendering Category
		ConfigCategory renderingCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.rendering"));
		renderingCategory
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.rendering.particleRenderingMode"),
					RenderingMode.class, rendering$particleRenderingMode)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.rendering.particleRenderingMode)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.particleRenderingMode.tooltip"))
				.setSaveConsumer(newValue -> rendering$particleRenderingMode = newValue)
				.build())
			.addEntry(entryBuilder
				.startSelector(Component.translatable("config.asyncparticles.rendering.particleCulling"),
					ParticleCullingMode.values(), rendering$particleCulling)
				.setNameProvider(ParticleCullingMode::getComponent)
				.setDefaultValue(defaultConfig.rendering.particleCulling)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.particleCulling.tooltip"))
				.setSaveConsumer(newValue -> rendering$particleCulling = newValue)
				.build())
			.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.cullWeathers"),
					rendering$cullWeathers)
				.setDefaultValue(defaultConfig.rendering.cullWeathers)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.cullWeathers.tooltip"))
				.setSaveConsumer(newValue -> rendering$cullWeathers = newValue)
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
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.rendering.failBehavior)
				.setTooltip(
					Component.translatable("config.asyncparticles.rendering.failBehavior.tooltip")
						.withStyle(ChatFormatting.STRIKETHROUGH),
					Component.translatable("config.asyncparticles.not-implemented")
						.withStyle(ChatFormatting.DARK_RED))
				.setSaveConsumer(newValue -> rendering$failBehavior = newValue)
				.setRequirement(() -> false)
				.build());
		// endregion
		// region Compat Category
		// Mixin
		ConfigEntryBuilder mixinEntryBuilder = builder.entryBuilder();
		mixinEntryBuilder.setResetButtonKey(Component.translatable("gui.asyncparticles.revert"));

		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> vsEntries = new ArrayList<>();
		vsEntries.add(entryBuilder
			.startSelector(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect"),
				RainEffect.values(), valkyrienSkies$rainEffect)
			.setNameProvider(RainEffect::getComponent)
			.setDefaultValue(defaultConfig.valkyrienSkies.rainEffect)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.rainEffect.tooltip"))
			.setSaveConsumer(newValue -> valkyrienSkies$rainEffect = newValue)
			.setRequirement(() -> ModListHelper.VS_LOADED)
			.build());
		vsEntries.add(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights"),
				valkyrienSkies$fixParticleLights)
			.setDefaultValue(defaultConfig.valkyrienSkies.fixParticleLights)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.valkyrienskies.fixParticleLights.tooltip"))
			.setSaveConsumer(newValue -> valkyrienSkies$fixParticleLights = newValue)
			.setRequirement(() -> ModListHelper.VS_LOADED)
			.build());

		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> createEntries = new ArrayList<>();
		createEntries.add(entryBuilder
			.startEnumSelector(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect"),
				RainEffect.class, create$rainEffect)
			.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
			.setDefaultValue(defaultConfig.create.rainEffect)
			.setTooltip(Component.translatable("config.asyncparticles.mod-compat.create.rainEffect.tooltip"))
			.setSaveConsumer(newValue -> create$rainEffect = newValue)
			.setRequirement(() -> ModListHelper.CREATE_LOADED)
			.build());

		// Mixin
		ClothConfigMixinMenus.addModCompatCategory(entryBuilder, mixinEntryBuilder, vsEntries, createEntries);

		ConfigCategory modCompatCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mod-compat"));
		modCompatCategory
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
				save();
				mixinSaveRunnable.run();
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
						current -> Minecraft.getInstance().setScreen(configScreen),
						Component.translatable("gui.continue"),
						current -> Minecraft.getInstance().setScreen(prevScreen)));
				});
			}
			AsyncTicker.reloadLater();
		});

		return builder;
	}
}
