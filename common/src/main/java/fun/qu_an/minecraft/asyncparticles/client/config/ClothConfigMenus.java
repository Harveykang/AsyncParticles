package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.coremod.ClothConfigMixinMenus;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

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
				.setSaveConsumer(newValue -> {
					particle$particleLimit = newValue;
					AsyncTicker.reloadLater();
				})
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
				.startBooleanToggle(Component.translatable("config.asyncparticles.rendering.cullParticles"),
					rendering$cullParticles)
				.setDefaultValue(defaultConfig.rendering.cullParticles)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.cullParticles.tooltip"))
				.setSaveConsumer(newValue -> rendering$cullParticles = newValue)
				.build())
			.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("config.asyncparticles.rendering.particleRenderingMode"),
					RenderingMode.class, rendering$particleRenderingMode)
				.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
				.setDefaultValue(defaultConfig.rendering.particleRenderingMode)
				.setTooltip(Component.translatable("config.asyncparticles.rendering.particleRenderingMode.tooltip"))
				.setSaveConsumer(newValue -> rendering$particleRenderingMode = newValue)
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
		// region Mod Compat Category
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
						.setNameProvider(value -> value.getComponent())
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
						.setEnumNameProvider(value -> ((TranslatableEnum) value).getComponent())
						.setDefaultValue(defaultConfig.create.rainEffect)
						.setTooltip(
							Component.translatable("config.asyncparticles.mod-compat.create.rainEffect.tooltip")
								.withStyle(ChatFormatting.STRIKETHROUGH),
							Component.translatable("config.asyncparticles.not-implemented")
								.withStyle(ChatFormatting.DARK_RED))
						.setSaveConsumer(newValue -> create$rainEffect = newValue)
						// .setRequirement(() -> ModListHelper.CREATE_LOADED)
						.setRequirement(() -> false)
						.build()))
				.build());
		// endregion
		// region Mixin Category
		ConfigCategory mixinCategory = builder.getOrCreateCategory(Component.translatable("config.asyncparticles.category.mixin"));
		ConfigEntryBuilder mixinEntryBuilder = builder.entryBuilder();
		mixinEntryBuilder.setResetButtonKey(Component.translatable("gui.asyncparticles.revert"));
		Object newConfig = ClothConfigMixinMenus.buildCategory(mixinCategory, entryBuilder, mixinEntryBuilder);
		// endregion

		builder.setSavingRunnable(() -> {
			try {
				save();
				ClothConfigMixinMenus.onSave(newConfig);
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
		});

		return builder;
	}
}
