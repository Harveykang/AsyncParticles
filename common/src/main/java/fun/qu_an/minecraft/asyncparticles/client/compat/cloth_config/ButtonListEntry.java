package fun.qu_an.minecraft.asyncparticles.client.compat.cloth_config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public class ButtonListEntry extends AbstractConfigListEntry<Void> {
	private final Button button;
	private final List<Button> widgets;

	public ButtonListEntry(Component fieldName, Component buttonText, Runnable onPress) {
		super(fieldName, false);
		this.button = Button.builder(buttonText, _ -> onPress.run())
			.bounds(0, 0, 150, 20)
			.createNarration(message -> Component.empty().append(fieldName).append(": ").append(message.get()))
			.build();
		this.widgets = List.of(button);
	}

	@Override
	public Void getValue() {
		return null;
	}

	@Override
	public Optional<Void> getDefaultValue() {
		return Optional.empty();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight,
	                               int mouseX, int mouseY, boolean hovered, float delta) {
		super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
		Font font = Minecraft.getInstance().font;
		Component fieldName = getDisplayedFieldName();
		boolean rightToLeft = font.isBidirectional();
		int textX = rightToLeft ? x + entryWidth - font.width(fieldName) : x;
		graphics.text(font, fieldName, textX, y + 6, getPreferredTextColor());
		button.setX(rightToLeft ? x : x + entryWidth - button.getWidth());
		button.setY(y);
		button.active = isEditable();
		button.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public List<Button> children() {
		return widgets;
	}

	@Override
	public List<Button> narratables() {
		return widgets;
	}
}
