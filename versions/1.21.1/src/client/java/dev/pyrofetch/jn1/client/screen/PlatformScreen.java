package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.lift.PlatformScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * L'interface d'un bloc de plateforme : la case d'apparence (on y dépose un
 * bloc, il reste dans la main), ce que la plateforme réunit, et trois
 * boutons — assembler aux voisines, désassembler, habiller toute la
 * plateforme comme ce bloc. Dessinée à plat, aux couleurs des écrans du jeu.
 */
public class PlatformScreen extends HandledScreen<PlatformScreenHandler> {
	private static final int FOND = 0xFFC6C6C6;
	private static final int CLAIR = 0xFFFFFFFF;
	private static final int SOMBRE = 0xFF555555;
	private static final int ENCRE = 0xFF404040;

	private ButtonWidget assembler;
	private ButtonWidget désassembler;
	private ButtonWidget habiller;

	public PlatformScreen(PlatformScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 166;
		this.playerInventoryTitleY = PlatformScreenHandler.INVENTAIRE_Y - 11;
	}

	@Override
	protected void init() {
		super.init();
		int y = this.y + 52;
		this.assembler = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.lift_platform.assemble"),
				b -> this.cliquer(PlatformScreenHandler.BOUTON_ASSEMBLER)).dimensions(this.x + 7, y, 52, 18)
				.tooltip(Tooltip.of(Text.translatable("screen.jn1.lift_platform.assemble.tip"))).build());
		this.désassembler = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.lift_platform.split"),
				b -> this.cliquer(PlatformScreenHandler.BOUTON_DÉSASSEMBLER)).dimensions(this.x + 62, y, 52, 18)
				.tooltip(Tooltip.of(Text.translatable("screen.jn1.lift_platform.split.tip"))).build());
		this.habiller = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.lift_platform.dress"),
				b -> this.cliquer(PlatformScreenHandler.BOUTON_HABILLER_TOUT)).dimensions(this.x + 117, y, 52, 18)
				.tooltip(Tooltip.of(Text.translatable("screen.jn1.lift_platform.dress.tip"))).build());
	}

	private void cliquer(int bouton) {
		if (this.client != null && this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, bouton);
		}
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		boolean assemblée = this.handler.assemblée();
		this.assembler.active = this.handler.voisines() > 1
				&& (!assemblée || this.handler.voisines() > this.handler.taille());
		this.désassembler.active = assemblée;
		this.habiller.active = this.handler.taille() > 1;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
		int x = PlatformScreenHandler.CASE_X + 24;
		context.drawText(this.textRenderer, Text.translatable("screen.jn1.lift_platform.look"),
				PlatformScreenHandler.CASE_X - 4 - this.textRenderer.getWidth(Text.translatable("screen.jn1.lift_platform.look")),
				PlatformScreenHandler.CASE_Y + 5, ENCRE, false);
		Text état = this.handler.assemblée()
				? Text.translatable("screen.jn1.lift_platform.assembled", this.handler.taille())
				: Text.translatable("screen.jn1.lift_platform.single");
		context.drawText(this.textRenderer, état, x, PlatformScreenHandler.CASE_Y, ENCRE, false);
		if (this.handler.voisines() > this.handler.taille()) {
			context.drawText(this.textRenderer, Text.translatable("screen.jn1.lift_platform.neighbours",
					this.handler.voisines()), x, PlatformScreenHandler.CASE_Y + 10, 0xFF7A5A10, false);
		}
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int g = this.x;
		int h = this.y;
		int l = this.backgroundWidth;
		int t = this.backgroundHeight;
		context.fill(g, h, g + l, h + t, FOND);
		context.fill(g, h, g + l, h + 2, CLAIR);
		context.fill(g, h, g + 2, h + t, CLAIR);
		context.fill(g, h + t - 2, g + l, h + t, SOMBRE);
		context.fill(g + l - 2, h, g + l, h + t, SOMBRE);
		for (Slot slot : this.handler.slots) {
			int x = g + slot.x - 1;
			int y = h + slot.y - 1;
			context.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
			context.fill(x, y, x + 17, y + 1, 0xFF373737);
			context.fill(x, y, x + 1, y + 17, 0xFF373737);
			context.fill(x + 1, y + 17, x + 18, y + 18, CLAIR);
			context.fill(x + 17, y + 1, x + 18, y + 18, CLAIR);
		}
		// La case d'apparence, cerclée d'ambre : on y dépose, on ne prend pas.
		Slot modèle = this.handler.slots.getFirst();
		int x = g + modèle.x - 2;
		int y = h + modèle.y - 2;
		context.drawBorder(x, y, 20, 20, 0xFFE0A030);
	}
}
