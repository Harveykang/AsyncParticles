package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FallbackScreen extends Screen {
	@Nullable
	public final Screen parent;
	public Component reason;
	private final Component buttonTextLeft;
	private final Consumer<FallbackScreen> buttonLeftCallback;
	private final Component buttonTextRight;
	private final Consumer<FallbackScreen> buttonRightCallback;
	private final GridLayout layout = new GridLayout();
	private MultiLineTextWidget reasonWidget;

	public FallbackScreen(@Nullable Screen parent,
						  Component title,
						  Component reason,
						  Component buttonL,
						  Consumer<FallbackScreen> buttonLeftCallback,
						  Component buttonR,
						  Consumer<FallbackScreen> buttonRightCallback) {
		super(title);
		this.parent = parent;
		this.reason = reason;
		this.buttonTextLeft = buttonL;
		this.buttonLeftCallback = buttonLeftCallback;
		this.buttonTextRight = buttonR;
		this.buttonRightCallback = buttonRightCallback;
	}

	@Override
	protected void init() {
		this.layout.defaultCellSetting().alignHorizontallyCenter().padding(10);
		GridLayout.RowHelper rowHelper = this.layout.createRowHelper(2);
		rowHelper.addChild(new StringWidget(this.title, this.font), 2);
		reasonWidget = new MultiLineTextWidget(this.reason, this.font).setMaxWidth(this.width - 50).setCentered(true);
		rowHelper.addChild(reasonWidget, 2);

		Button buttonL = Button.builder(this.buttonTextLeft, button1 -> buttonLeftCallback.accept(this))
			.bounds(this.width / 2 - 155, this.height / 6 + 96, 150, 20)
			.build();
		Button buttonR = Button.builder(this.buttonTextRight, button1 -> buttonRightCallback.accept(this))
			.bounds(this.width / 2 - 155 + 160, this.height / 6 + 96, 150, 20)
			.build();
		rowHelper.addChild(buttonL);
		rowHelper.addChild(buttonR);

		this.layout.arrangeElements();
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		FrameLayout.centerInRectangle(this.layout, this.getRectangle());
	}

	@Override
	public Component getNarrationMessage() {
		return CommonComponents.joinForNarration(this.title, this.reason);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		if (reasonWidget.getMessage() != reason) {
			minecraft.setScreen(new FallbackScreen(
				parent,
				title,
				reason,
				buttonTextLeft,
				buttonLeftCallback,
				buttonTextRight,
				buttonRightCallback
			));
		}
		super.tick();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}
}
