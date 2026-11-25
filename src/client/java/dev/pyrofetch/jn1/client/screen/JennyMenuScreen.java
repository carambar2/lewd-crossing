package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.screen.JennyScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Le sac de la compagne, ouvert depuis son écran (bouton « Inventaire »).
 *
 * <p>Jusqu'en 0.52 il portait aussi la jauge d'affection et les actions ; elles
 * sont passées à {@link CompanionScreen}. Reste le coffre : sa rangée, et l'inventaire
 * du joueur. Tout est dessiné au rectangle, faute de texture.
 */
public class JennyMenuScreen extends HandledScreen<JennyScreenHandler> {
	private static final int PANEL = 0xFFC6C6C6;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int PANEL_SHADE = 0xFF565656;
	private static final int SLOT_BACK = 0xFF8B8B8B;
	private static final int SLOT_EDGE = 0xFF373737;
	private static final int TEXT = 0xFF404040;

	public JennyMenuScreen(JennyScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = JennyScreenHandler.SLOTS_LEFT + 9 * 18 + 8;
		this.backgroundHeight = 147;
		this.playerInventoryTitleY = 51;
		this.playerInventoryTitleX = JennyScreenHandler.SLOTS_LEFT;
		this.titleX = JennyScreenHandler.SLOTS_LEFT;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = (this.width - this.backgroundWidth) / 2;
		int top = (this.height - this.backgroundHeight) / 2;

		context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, PANEL);
		context.fill(left, top, left + this.backgroundWidth, top + 1, PANEL_LIGHT);
		context.fill(left, top, left + 1, top + this.backgroundHeight, PANEL_LIGHT);
		context.fill(left, top + this.backgroundHeight - 1,
				left + this.backgroundWidth, top + this.backgroundHeight, PANEL_SHADE);
		context.fill(left + this.backgroundWidth - 1, top,
				left + this.backgroundWidth, top + this.backgroundHeight, PANEL_SHADE);

		// Les cases sont creusées à la main, faute de texture pour les porter.
		this.drawSlots(context, left, top, JennyScreenHandler.SLOTS_LEFT, 22, 1);
		this.drawSlots(context, left, top, JennyScreenHandler.SLOTS_LEFT, 61, 3);
		this.drawSlots(context, left, top, JennyScreenHandler.SLOTS_LEFT, 123, 1);
	}

	private void drawSlots(DrawContext context, int left, int top, int x, int y, int rows) {
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
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, TEXT, false);
		context.drawText(this.textRenderer, this.playerInventoryTitle,
				this.playerInventoryTitleX, this.playerInventoryTitleY, TEXT, false);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Pas de renderBackground ici : HandledScreen.render le fait déjà, et un
		// second passage floutait le panneau qu'il venait de dessiner.
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}
}
