package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.screen.JennyScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Le sac de la compagne, ouvert depuis son écran (bouton « Inventaire »).
 *
 * <p>Jusqu'en 0.52 il portait aussi la jauge d'affection et les actions ; elles
 * sont passées à {@link CompanionScreen}. Reste le coffre : sa rangée, et l'inventaire
 * du joueur. Tout est dessiné au rectangle, faute de texture.
 */
public class JennyMenuScreen extends AbstractContainerScreen<JennyScreenHandler> {
	private static final int PANEL = 0xFFC6C6C6;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int PANEL_SHADE = 0xFF565656;
	private static final int SLOT_BACK = 0xFF8B8B8B;
	private static final int SLOT_EDGE = 0xFF373737;
	private static final int TEXT = 0xFF404040;

	public JennyMenuScreen(JennyScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title, JennyScreenHandler.SLOTS_LEFT + 9 * 18 + 8, 147);
		this.inventoryLabelY = 51;
		this.inventoryLabelX = JennyScreenHandler.SLOTS_LEFT;
		this.titleLabelX = JennyScreenHandler.SLOTS_LEFT;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		// Le voile du jeu d'abord (un écran à cases n'est pas flouté), puis le panneau.
		super.extractBackground(context, mouseX, mouseY, delta);
		int left = (this.width - this.imageWidth) / 2;
		int top = (this.height - this.imageHeight) / 2;

		context.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL);
		context.fill(left, top, left + this.imageWidth, top + 1, PANEL_LIGHT);
		context.fill(left, top, left + 1, top + this.imageHeight, PANEL_LIGHT);
		context.fill(left, top + this.imageHeight - 1,
				left + this.imageWidth, top + this.imageHeight, PANEL_SHADE);
		context.fill(left + this.imageWidth - 1, top,
				left + this.imageWidth, top + this.imageHeight, PANEL_SHADE);

		// Les cases sont creusées à la main, faute de texture pour les porter.
		this.drawSlots(context, left, top, JennyScreenHandler.SLOTS_LEFT, 22, 1);
		this.drawSlots(context, left, top, JennyScreenHandler.SLOTS_LEFT, 61, 3);
		this.drawSlots(context, left, top, JennyScreenHandler.SLOTS_LEFT, 123, 1);
	}

	private void drawSlots(GuiGraphicsExtractor context, int left, int top, int x, int y, int rows) {
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < 9; column++) {
				int sx = left + x + column * 18 - 1;
				int sy = top + y + row * 18 - 1;
				context.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
				context.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BACK);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
		context.text(this.font, this.playerInventoryTitle,
				this.inventoryLabelX, this.inventoryLabelY, TEXT, false);
	}
}
