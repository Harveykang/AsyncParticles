package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.ConfigObj;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesMixinConfig.MixinConfigObj;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config.AccessorAbstractConfigScreen;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import it.unimi.dsi.fastutil.Pair;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class CompatibilityTryItButton extends Button {
	private final AbstractConfigScreen owner;
	private final ConfigObj modified;
	private final MixinConfigObj mixinModified;
	private CompatibilityTryIt select = CompatibilityTryIt.DEFAULT;
	private boolean lastHoveredOrFocused;

	CompatibilityTryItButton(AbstractConfigScreen owner, ConfigObj modified, MixinConfigObj mixinModified, @Nullable CompatibilityTryIt select) {
		super(owner.width - 5 - 120, 13, 120, 20, Component.translatable("config.asyncparticles.enum.CompatibilityTryIt"), button -> {
		}, DEFAULT_NARRATION);
		this.owner = owner;
		this.modified = modified; // Reference to the modified config object
		this.mixinModified = mixinModified;
		if (select != null) {
			this.select = select;
		} else {
			this.select = getSelect();
		}
	}

	@Override
	public void onPress(InputWithModifiers inputWithModifiers) {
		super.onPress(inputWithModifiers);

		CompatibilityTryIt select = getSelect(); // getSelect() will trigger a captured save
		CompatibilityTryIt[] values = CompatibilityTryIt.values();
		Minecraft mc = Minecraft.getInstance();
		do {
			select = values[(select.ordinal() + (mc.hasShiftDown() ? -1 : 1) + values.length) % values.length];
		} while (select == CompatibilityTryIt.CUSTOM);
		Screen parent = ((AccessorAbstractConfigScreen) owner).getParent();

		Pair<ConfigObj, MixinConfigObj> pair = select.getConfig(modified, mixinModified);
		Screen screen = ClothConfigMenus.screen(parent,
			new ClothConfigMenus.ConfigBundle(pair.first(),
				AsyncParticlesConfig.getDefaultConfig(),
				AsyncParticlesConfig.getCurrentConfig()),
			new ClothConfigMixinMenus.MixinConfigBundle(
				pair.second(),
				AsyncParticlesMixinConfig.getDefaultConfig(),
				AsyncParticlesMixinConfig.getCurrentConfig()
			),
			owner.selectedCategoryIndex,
			select);

		mc.setScreenAndShow(screen);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.setMessage(this.getComponent());
		this.extractDefaultSprite(graphics);
		this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
		if (isHovered()) {
			List<FormattedCharSequence> lines = owner.getFont().split(select.getTooltipComponent(), owner.width / 2);
			graphics.setTooltipForNextFrame(owner.getFont(), lines, mouseX, mouseY);
		}
	}

	private Component getComponent() {
		if (!this.isHoveredOrFocused()) {
			lastHoveredOrFocused = false;
			return Component.translatable("config.asyncparticles.enum.CompatibilityTryIt");
		} else {
			if (!lastHoveredOrFocused) {
				lastHoveredOrFocused = true;
				select = getSelect();
			}
			CompatibilityTryIt select = this.select;
			if (select == CompatibilityTryIt.CUSTOM) {
				return CompatibilityTryIt.CUSTOM.getComponent();
			} else {
				return select.getComponent();
			}
		}
	}

	private CompatibilityTryIt getSelect() {
		for (Collection<AbstractConfigEntry<?>> entries : owner.getCategorizedEntries().values()) {
			for (AbstractConfigEntry<?> entry : entries) {
				entry.save();
			}
		}
		CompatibilityTryIt[] values = CompatibilityTryIt.values();
		for (int i = select.ordinal(); i < values.length; i++) {
			CompatibilityTryIt compatibilityTryIt = values[i];
			if (compatibilityTryIt == CompatibilityTryIt.CUSTOM) {
				continue;
			}
			if (compatibilityTryIt.getConfig(modified, mixinModified).equals(Pair.of(modified, mixinModified))) {
				return compatibilityTryIt;
			}
		}
		for (int i = 0; i < select.ordinal(); i++) {
			CompatibilityTryIt compatibilityTryIt = values[i];
			if (compatibilityTryIt == CompatibilityTryIt.CUSTOM) {
				continue;
			}
			if (compatibilityTryIt.getConfig(modified, mixinModified).equals(Pair.of(modified, mixinModified))) {
				return compatibilityTryIt;
			}
		}
		return CompatibilityTryIt.CUSTOM;
	}

	enum CompatibilityTryIt implements TranslatableEnum {
		CUSTOM(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.CUSTOM"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.CUSTOM.tooltip").withStyle(ChatFormatting.YELLOW)) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig(ConfigObj modified, MixinConfigObj mixinModified) {
				ConfigObj config = AsyncParticlesConfig.getCurrentConfig();
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getCurrentConfig();
				preserve(modified, mixinModified, config, mixinConfig);
				return Pair.of(config, mixinConfig);
			}
		},
		DEFAULT(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.DEFAULT"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.DEFAULT.tooltip").withStyle(ChatFormatting.YELLOW)) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig(ConfigObj modified, MixinConfigObj mixinModified) {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getDefaultConfigExceptCollections();
				preserve(modified, mixinModified, config, mixinConfig);
				return Pair.of(config, mixinConfig);
			}
		},
		PARTICLE_ASYNC_ONLY(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig(ConfigObj modified, MixinConfigObj mixinModified) {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				preserve(modified, mixinModified, config, mixinConfig);
				return Pair.of(config, mixinConfig);
			}
		},
		THREAD_SAFE(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.THREAD_SAFE"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.THREAD_SAFE.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig(ConfigObj modified, MixinConfigObj mixinModified) {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getDefaultConfigExceptCollections();
				mixinConfig.setSafeClassInstanceMultiMap(true);
				mixinConfig.setSafeBlockEntityMap(true);
				preserve(modified, mixinModified, config, mixinConfig);
				return Pair.of(config, mixinConfig);
			}
		},
		PARTICLE_ASYNC_ONLY_AND_THREAD_SAFE(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY_AND_THREAD_SAFE"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY_AND_THREAD_SAFE.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig(ConfigObj modified, MixinConfigObj mixinModified) {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				mixinConfig.setSafeClassInstanceMultiMap(true);
				mixinConfig.setSafeBlockEntityMap(true);
				preserve(modified, mixinModified, config, mixinConfig);
				return Pair.of(config, mixinConfig);
			}
		},
		MAIN_THREAD_EVERYTHING(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.MAIN_THREAD_EVERYTHING"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.MAIN_THREAD_EVERYTHING.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig(ConfigObj modified, MixinConfigObj mixinModified) {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				config.tick.particleAsyncMode = ParticleAsyncMode.DISABLE;
				config.particle.particleLightCache = false;
				config.particle.cleanupStrategy = ParticleCleanupStrategy.MAIN_THREAD;
				config.rendering.tickRendererOnMainThread = true;
				preserve(modified, mixinModified, config, mixinConfig);
				return Pair.of(config, mixinConfig);
			}
		};

		private static void preserve(ConfigObj modified, MixinConfigObj mixinModified, ConfigObj config, MixinConfigObj mixinConfig) {
			// preserve modified collections
			config.tick.syncParticleClasses = modified.tick.syncParticleClasses;
			config.particle.particleLimit = modified.particle.particleLimit;
			mixinConfig.setAsyncTickableParticleGroups(mixinModified.getAsyncTickableParticleGroups());
			mixinConfig.setLockProvider(mixinModified.getLockProvider());
			mixinConfig.setLockRequired(mixinModified.getLockRequired());
			mixinConfig.setReplaceRandom(mixinModified.getReplaceRandom());
			mixinConfig.setNoLightCache(mixinModified.getNoLightCache());
		}

		private final Supplier<Component> componentSupplier;
		private final Supplier<Component> tooltipSupplier;

		CompatibilityTryIt(Supplier<Component> componentSupplier, Supplier<Component> tooltipSupplier) {
			this.componentSupplier = componentSupplier;
			this.tooltipSupplier = tooltipSupplier;
		}

		@Override
		public Component getComponent() {
			return componentSupplier.get();
		}

		abstract Pair<ConfigObj, MixinConfigObj> getConfig(ConfigObj modified, MixinConfigObj mixinModified);

		public Component getTooltipComponent() {
			return tooltipSupplier.get();
		}
	}
}
