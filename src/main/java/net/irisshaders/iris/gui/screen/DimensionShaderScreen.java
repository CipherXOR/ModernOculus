package net.irisshaders.iris.gui.screen;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.element.IrisObjectSelectionList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DimensionShaderScreen extends Screen implements HudHideable {
	private static final Component TITLE = Component.translatable("options.iris.dimensionShader.title");
	private static final Component ADD_DIMENSION_LABEL = Component.translatable("options.iris.dimensionShader.addDimension");
	private static final Component NO_SHADER_PACKS = Component.translatable("options.iris.dimensionShader.noPacks").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
	private static final Component DIMENSION_HINT = Component.translatable("options.iris.dimensionHint").withStyle(ChatFormatting.GRAY);

	private final Screen parent;
	private DimensionShaderList dimensionShaderList;
	private EditBox dimensionInput;
	private CycleButton<String> shaderSelector;
	private Button addButton;
	private Button removeButton;
	private Button doneButton;
	private @Nullable String selectedDimension = null;
	private @Nullable String selectedShader = null;

	public DimensionShaderScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		this.dimensionShaderList = new DimensionShaderList(this, this.minecraft, this.width, this.height, 64, this.height - 96, 0, this.width);
		this.addRenderableWidget(this.dimensionShaderList);

		int centerX = this.width / 2;

		this.dimensionInput = new EditBox(this.font, centerX - 154, this.height - 88, 148, 20, Component.literal(""));
		this.dimensionInput.setHint(DIMENSION_HINT);
		this.addRenderableWidget(this.dimensionInput);

		List<String> packs = getAvailableShaderPacks();
		this.shaderSelector = CycleButton.<String>builder(name -> Component.literal(name))
			.withValues(packs.isEmpty() ? List.of("") : packs)
			.withInitialValue(selectedShader != null && packs.contains(selectedShader) ? selectedShader : (packs.isEmpty() ? "" : packs.get(0)))
			.create(centerX + 6, this.height - 88, 148, 20, Component.empty(), (button, value) -> {
				selectedShader = value;
			});
		this.addRenderableWidget(this.shaderSelector);

		this.addButton = Button.builder(ADD_DIMENSION_LABEL, button -> addMapping())
			.bounds(centerX - 154, this.height - 64, 148, 20)
			.build();
		this.addRenderableWidget(this.addButton);

		this.removeButton = Button.builder(Component.translatable("options.iris.dimensionShader.remove"), button -> removeMapping())
			.bounds(centerX + 6, this.height - 64, 148, 20)
			.build();
		this.removeButton.active = false;
		this.addRenderableWidget(this.removeButton);

		this.doneButton = Button.builder(CommonComponents.GUI_DONE, button -> onClose())
			.bounds(centerX - 50, this.height - 40, 100, 20)
			.build();
		this.addRenderableWidget(this.doneButton);

		refreshList();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		if (this.minecraft.level == null) {
			this.renderBackground(guiGraphics);
		} else {
			guiGraphics.fillGradient(0, 0, width, height, 0x4F232323, 0x4F232323);
		}

		this.dimensionShaderList.render(guiGraphics, mouseX, mouseY, delta);
		super.render(guiGraphics, mouseX, mouseY, delta);

		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
	}

	private void refreshList() {
		this.dimensionShaderList.refresh();
	}

	private void addMapping() {
		String dimensionId = this.dimensionInput.getValue().trim();
		if (dimensionId.isEmpty()) {
			return;
		}

		if (!dimensionId.contains(":")) {
			dimensionId = "minecraft:" + dimensionId;
		}

		List<String> packs = getAvailableShaderPacks();
		if (packs.isEmpty()) {
			return;
		}

		String shaderPack = selectedShader != null && packs.contains(selectedShader) ? selectedShader : packs.get(0);

		Iris.getDimensionShaderConfig().setShaderForDimension(dimensionId, shaderPack);
		try {
			Iris.getDimensionShaderConfig().save();
		} catch (IOException e) {
			Iris.logger.error("Failed to save dimension shader config", e);
		}

		this.dimensionInput.setValue("");
		refreshList();
	}

	private void removeMapping() {
		if (selectedDimension == null) {
			return;
		}

		Iris.getDimensionShaderConfig().removeShaderForDimension(selectedDimension);
		try {
			Iris.getDimensionShaderConfig().save();
		} catch (IOException e) {
			Iris.logger.error("Failed to save dimension shader config", e);
		}

		selectedDimension = null;
		removeButton.active = false;
		refreshList();
	}

	void onSelectDimension(String dimensionId, String shaderPack) {
		this.selectedDimension = dimensionId;
		this.selectedShader = shaderPack;
		this.removeButton.active = true;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean keyPressed(int key, int j, int k) {
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		return super.keyPressed(key, j, k);
	}

	private List<String> getAvailableShaderPacks() {
		try {
			return Iris.getShaderpacksDirectoryManager().enumerate();
		} catch (Exception e) {
			Iris.logger.error("Error reading shaderpacks directory", e);
			return new ArrayList<>();
		}
	}

	public static class DimensionShaderList extends IrisObjectSelectionList<DimensionShaderList.Entry> {
		private final DimensionShaderScreen screen;

		public DimensionShaderList(DimensionShaderScreen screen, Minecraft client, int width, int height, int top, int bottom, int left, int right) {
			super(client, width, height, top, bottom, left, right, 20);
			this.screen = screen;
		}

		public void refresh() {
			this.clearEntries();
			Map<String, String> mappings = Iris.getDimensionShaderConfig().getAllMappings();

			if (mappings.isEmpty()) {
				this.addEntry(new LabelEntry(NO_SHADER_PACKS));
			} else {
				for (Map.Entry<String, String> entry : mappings.entrySet()) {
					this.addEntry(new MappingEntry(entry.getKey(), entry.getValue()));
				}
			}
		}

		public abstract static class Entry extends IrisObjectSelectionList.Entry<Entry> {
		}

		public class LabelEntry extends Entry {
			private final Component label;

			public LabelEntry(Component label) {
				this.label = label;
			}

			@Override
			public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, (x + entryWidth / 2), y + (entryHeight - 11) / 2, 0xC2C2C2);
			}
		}

		public class MappingEntry extends Entry {
			private final String dimensionId;
			private final String shaderPack;
			private boolean hovered;

			public MappingEntry(String dimensionId, String shaderPack) {
				this.dimensionId = dimensionId;
				this.shaderPack = shaderPack;
			}

			@Override
			public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				this.hovered = hovered;
				Font font = Minecraft.getInstance().font;

				if (hovered || DimensionShaderList.this.getSelected() == this) {
					GuiUtil.bindIrisWidgetsTexture();
					GuiUtil.drawButton(guiGraphics, x - 2, y - 2, entryWidth, entryHeight + 4, hovered, false);
				}

				String dimText = dimensionId;
				if (font.width(dimText) > entryWidth / 2 - 10) {
					dimText = font.plainSubstrByWidth(dimText, entryWidth / 2 - 14) + "...";
				}

				String shaderText = shaderPack;
				if (font.width(shaderText) > entryWidth / 2 - 10) {
					shaderText = font.plainSubstrByWidth(shaderText, entryWidth / 2 - 14) + "...";
				}

				guiGraphics.drawString(font, dimText, x + 4, y + (entryHeight - 11) / 2, 0xFFFFFF);
				guiGraphics.drawString(font, shaderText, x + entryWidth / 2 + 4, y + (entryHeight - 11) / 2, 0xFFF263);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					screen.onSelectDimension(dimensionId, shaderPack);
					return true;
				}
				return false;
			}
		}
	}
}
