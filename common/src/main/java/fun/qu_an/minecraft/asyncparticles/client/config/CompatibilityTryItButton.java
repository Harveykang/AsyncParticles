package fun.qu_an.minecraft.asyncparticles.client.config;

import com.google.common.collect.Lists;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config.AccessorAbstractConfigScreen;
import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public class CompatibilityTryItButton extends Button {
	private static final int SIZE = CompatibilityTryIt.values().length;
	private final AbstractConfigScreen owner;
	private final AsyncParticlesConfig.ConfigObj saving;
	private CompatibilityTryIt select;
	private final AsyncParticlesConfig.ConfigObj original;

	CompatibilityTryItButton(AbstractConfigScreen screen, AsyncParticlesConfig.ConfigObj original, AsyncParticlesConfig.ConfigObj saving) {
		super(5, 12, 120, 20, Component.translatable("config.asyncparticles.enum.CompatibilityTryIt"), button -> {
		}, DEFAULT_NARRATION);
		this.owner = screen;
		this.original = original;
		this.saving = saving;
		select = getSelect();
	}

	@Override
	public void onPress(InputWithModifiers inputWithModifiers) {
		super.onPress(inputWithModifiers);
		CompatibilityTryIt select = getSelect();
		select = CompatibilityTryIt.values()[(select.ordinal() + 1) % SIZE];
		if (select == CompatibilityTryIt.CUSTOM) {
			select = CompatibilityTryIt.values()[(CompatibilityTryIt.CUSTOM.ordinal() + 1) % SIZE];
		}
		Screen parent = ((AccessorAbstractConfigScreen) owner).getParent();

		Screen screen = ClothConfigMenus.screen(parent,
			select.getConfig(),
			AsyncParticlesConfig.getDefaultConfig(),
			AsyncParticlesConfig.getDefaultConfig(),
			this.original,
			owner.selectedCategoryIndex);

		Minecraft.getInstance().setScreen(screen);
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
		this.setMessage(this.getComponent());
		this.renderDefaultSprite(guiGraphics);
		this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
	}

	private Component getComponent() {
		if (!this.isHoveredOrFocused()) {
			return Component.literal("Compatibility Try It!");
		} else {
			CompatibilityTryIt select = this.select != null ? this.select : (this.select = getSelect());
			if (select == CompatibilityTryIt.CUSTOM) {
				return CompatibilityTryIt.CUSTOM.getComponent();
			} else {
				return select.getComponent();
			}
		}
	}

	private CompatibilityTryIt getSelect() {
		for (List<AbstractConfigEntry<?>> entries : owner.getCategorizedEntries().values()) {
			for (AbstractConfigEntry<?> entry : entries) {
				entry.save();
			}
		}

		for (CompatibilityTryIt compatibilityTryIt : CompatibilityTryIt.values()) {
			if (compatibilityTryIt == CompatibilityTryIt.CUSTOM) {
				continue;
			}
			if (compatibilityTryIt.getConfig().equals(saving)) {
				return compatibilityTryIt;
			}
		}
		return CompatibilityTryIt.CUSTOM;
	}

	private enum CompatibilityTryIt implements TranslatableEnum {
		CUSTOM(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.CUSTOM")) {
			@Override
			AsyncParticlesConfig.ConfigObj getConfig() {
				return AsyncParticlesConfig.getCurrentConfig();
			}
		},
		DEFAULT(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.DEFAULT")) {
			@Override
			AsyncParticlesConfig.ConfigObj getConfig() {
				return AsyncParticlesConfig.getDefaultConfigExceptCollections();
			}
		},
		PARTICLE_ASYNC_ONLY(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PARTICLE_ASYNC_ONLY")) {
			@Override
			AsyncParticlesConfig.ConfigObj getConfig() {
				AsyncParticlesConfig.ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				return config;
			}
		},
		PREVENT_OFF_THREAD(() -> Component.translatable("config.asyncparticles.enum.CompatibilityTryIt.PREVENT_OFF_THREAD")) {
			@Override
			AsyncParticlesConfig.ConfigObj getConfig() {
				AsyncParticlesConfig.ConfigObj config = AsyncParticlesConfig.getDefaultConfigExceptCollections();
				config.tick.animationTickMode = false;
				config.tick.deferredTextureTick = false;
				config.tick.tickWeatherAsync = false;
				config.tick.particleAsyncMode = ParticleAsyncMode.DISABLE;
				config.particle.particleLightCache = false;
				config.particle.cleanupStrategy = ParticleCleanupStrategy.MAIN_THREAD;
				return config;
			}
		};

		private final Supplier<Component> componentSupplier;

		CompatibilityTryIt(Supplier<Component> componentSupplier) {
			this.componentSupplier = componentSupplier;
		}

		@Override
		public Component getComponent() {
			return componentSupplier.get();
		}

		abstract AsyncParticlesConfig.ConfigObj getConfig();
	}
}
