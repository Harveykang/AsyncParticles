package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FallbackScreen extends Screen {
	@Nullable
	public final Screen parent;
	public Component message;
	public final Component buttonTextLeft;
	public Consumer<FallbackScreen> buttonLeftCallback;
	public final Component buttonTextRight;
	public Consumer<FallbackScreen> buttonRightCallback;
	private final GridLayout layout = new GridLayout();
	private MultiLineTextWidget reasonWidget;
	public Button buttonLeft;
	public Button buttonRight;
	public BiConsumer<FallbackScreen, Button> buttonLeftTick;
	public BiConsumer<FallbackScreen, Button> buttonRightTick;

	public FallbackScreen(@Nullable Screen parent,
						  Component title,
						  Component message,
						  Component buttonL,
						  Consumer<FallbackScreen> buttonLeftCallback,
						  Component buttonR,
						  Consumer<FallbackScreen> buttonRightCallback) {
		super(title);
		this.parent = parent;
		this.message = message;
		this.buttonTextLeft = buttonL;
		this.buttonLeftCallback = buttonLeftCallback;
		this.buttonTextRight = buttonR;
		this.buttonRightCallback = buttonRightCallback;
	}

	@Override
	protected void init() {
		super.init();
		this.layout.defaultCellSetting().alignHorizontallyCenter().padding(10);
		GridLayout.RowHelper rowHelper = this.layout.createRowHelper(2);
		rowHelper.addChild(new StringWidget(this.title, this.font), 2);
		reasonWidget = new MultiLineTextWidget(this.message, this.font).setMaxWidth(this.width - 50).setCentered(true);
		rowHelper.addChild(reasonWidget, 2);

		buttonLeft = Button.builder(this.buttonTextLeft, button1 -> getButtonLeftCallback().accept(this))
			.bounds(this.width / 2 - 155, this.height / 6 + 96, 150, 20)
			.build();
		buttonRight = Button.builder(this.buttonTextRight, button1 -> getButtonRightCallback().accept(this))
			.bounds(this.width / 2 - 155 + 160, this.height / 6 + 96, 150, 20)
			.build();
		rowHelper.addChild(buttonLeft);
		rowHelper.addChild(buttonRight);

		this.layout.arrangeElements();
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	private Consumer<FallbackScreen> getButtonLeftCallback() {
		return buttonLeftCallback;
	}

	private Consumer<FallbackScreen> getButtonRightCallback() {
		return buttonRightCallback;
	}

	@Override
	protected void repositionElements() {
		FrameLayout.centerInRectangle(this.layout, this.getRectangle());
	}

	@Override
	public Component getNarrationMessage() {
		return CommonComponents.joinForNarration(this.title, this.message);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		if (buttonLeftTick != null) {
			buttonLeftTick.accept(this, buttonLeft);
		}
		if (buttonRightTick != null) {
			buttonRightTick.accept(this, buttonRight);
		}
		if (reasonWidget.getMessage() != message) {
			FallbackScreen guiScreen = new FallbackScreen(
				parent,
				title,
				message,
				buttonTextLeft,
				buttonLeftCallback,
				buttonTextRight,
				buttonRightCallback
			);
			guiScreen.buttonLeft = buttonLeft;
			guiScreen.buttonLeftTick = buttonLeftTick;
			guiScreen.buttonRight = buttonRight;
			guiScreen.buttonRightTick = buttonRightTick;
			minecraft.gui.setScreen(guiScreen);
			return;
		}
		super.tick();
	}
}
