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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class CompatibilityTryItButton extends Button {
	private final AbstractConfigScreen owner;
	private final ConfigObj modified;
	private final MixinConfigObj mixinModified;
	private CompatibilityTryIt select = CompatibilityTryIt.DEFAULT;
	private boolean lastHoveredOrFocused;

	CompatibilityTryItButton(AbstractConfigScreen screen, ConfigObj modified, MixinConfigObj mixinModified, @Nullable CompatibilityTryIt select) {
		super(5, 12, 120, 20, Component.translatable("config.asyncparticles.enum.CompatibilityTryIt"), button -> {
		}, DEFAULT_NARRATION);
		this.owner = screen;
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

		CompatibilityTryIt select = this.select;
		CompatibilityTryIt[] values = CompatibilityTryIt.values();
		Minecraft mc = Minecraft.getInstance();
		do {
			select = values[(select.ordinal() + (mc.hasShiftDown() ? -1 : 1) + values.length) % values.length];
		} while (select == CompatibilityTryIt.CUSTOM);
		Screen parent = ((AccessorAbstractConfigScreen) owner).getParent();

		Pair<ConfigObj, MixinConfigObj> pair = select.getConfig();
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

		mc.setScreen(screen);
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
		this.setMessage(this.getComponent());
		this.renderDefaultSprite(guiGraphics);
		this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
		if (isHovered()) {
			List<FormattedCharSequence> lines = owner.getFont().split(select.getTooltipComponent(), owner.width / 2);
			guiGraphics.setTooltipForNextFrame(owner.getFont(), lines, i, j);
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
			if (compatibilityTryIt.getConfig().equals(Pair.of(modified, mixinModified))) {
				return compatibilityTryIt;
			}
		}
		for (int i = 0; i < select.ordinal(); i++) {
			CompatibilityTryIt compatibilityTryIt = values[i];
			if (compatibilityTryIt == CompatibilityTryIt.CUSTOM) {
				continue;
			}
			if (compatibilityTryIt.getConfig().equals(Pair.of(modified, mixinModified))) {
				return compatibilityTryIt;
			}
		}
		return CompatibilityTryIt.CUSTOM;
	}

	enum CompatibilityTryIt implements TranslatableEnum {
		CUSTOM(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.CUSTOM"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.CUSTOM.tooltip").withStyle(ChatFormatting.YELLOW)) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig() {
				return Pair.of(AsyncParticlesConfig.getCurrentConfig(), AsyncParticlesMixinConfig.getCurrentConfig());
			}
		},
		DEFAULT(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.DEFAULT"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.DEFAULT.tooltip").withStyle(ChatFormatting.YELLOW)) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig() {
				return Pair.of(AsyncParticlesConfig.getDefaultConfigExceptCollections(), AsyncParticlesMixinConfig.getDefaultConfigExceptCollections());
			}
		},
		PARTICLE_ASYNC_ONLY(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig() {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				return Pair.of(config, AsyncParticlesMixinConfig.getDefaultConfigExceptCollections());
			}
		},
		THREAD_SAFE(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.THREAD_SAFE"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.THREAD_SAFE.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig() {
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getDefaultConfigExceptCollections();
				mixinConfig.setSafeClassInstanceMultiMap(true);
				mixinConfig.setSafeBlockEntityMap(true);
				return Pair.of(AsyncParticlesConfig.getDefaultConfigExceptCollections(), mixinConfig);
			}
		},
		PARTICLE_ASYNC_ONLY_AND_THREAD_SAFE(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY_AND_THREAD_SAFE"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY_AND_THREAD_SAFE.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig() {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				MixinConfigObj mixinConfig = AsyncParticlesMixinConfig.getDefaultConfigExceptCollections();
				mixinConfig.setSafeClassInstanceMultiMap(true);
				mixinConfig.setSafeBlockEntityMap(true);
				return Pair.of(config, mixinConfig);
			}
		},
		MAIN_THREAD_EVERYTHING(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.MAIN_THREAD_EVERYTHING"),
			() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.MAIN_THREAD_EVERYTHING.tooltip")) {
			@Override
			Pair<ConfigObj, MixinConfigObj> getConfig() {
				ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				config.tick.particleAsyncMode = ParticleAsyncMode.DISABLE;
				config.particle.particleLightCache = false;
				config.particle.cleanupStrategy = ParticleCleanupStrategy.MAIN_THREAD;
				config.rendering.tickRendererOnMainThread = true;
				return Pair.of(config, AsyncParticlesMixinConfig.getDefaultConfigExceptCollections());
			}
		};

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

		abstract Pair<ConfigObj, MixinConfigObj> getConfig();

		public Component getTooltipComponent() {
			return tooltipSupplier.get();
		}
	}
}
