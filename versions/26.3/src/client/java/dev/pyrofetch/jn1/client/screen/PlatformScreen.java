package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.lift.PlatformScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * L'interface d'un bloc de plateforme : la case d'apparence (on y dépose un
 * bloc, il reste dans la main), ce que la plateforme réunit, et trois
 * boutons — assembler aux voisines, désassembler, habiller toute la
 * plateforme comme ce bloc. Dessinée à plat, aux couleurs des écrans du jeu.
 */
public class PlatformScreen extends AbstractContainerScreen<PlatformScreenHandler> {
	private static final int FOND = 0xFFC6C6C6;
	private static final int CLAIR = 0xFFFFFFFF;
	private static final int SOMBRE = 0xFF555555;
	private static final int ENCRE = 0xFF404040;

	private Button assembler;
	private Button désassembler;
	private Button habiller;

	public PlatformScreen(PlatformScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title, 176, 166);
		this.inventoryLabelY = PlatformScreenHandler.INVENTAIRE_Y - 11;
	}

	@Override
	protected void init() {
		super.init();
		int y = this.topPos + 52;
		this.assembler = this.addRenderableWidget(Button.builder(Component.translatable("screen.jn1.lift_platform.assemble"),
				b -> this.cliquer(PlatformScreenHandler.BOUTON_ASSEMBLER)).bounds(this.leftPos + 7, y, 52, 18)
				.tooltip(Tooltip.create(Component.translatable("screen.jn1.lift_platform.assemble.tip"))).build());
		this.désassembler = this.addRenderableWidget(Button.builder(Component.translatable("screen.jn1.lift_platform.split"),
				b -> this.cliquer(PlatformScreenHandler.BOUTON_DÉSASSEMBLER)).bounds(this.leftPos + 62, y, 52, 18)
				.tooltip(Tooltip.create(Component.translatable("screen.jn1.lift_platform.split.tip"))).build());
		this.habiller = this.addRenderableWidget(Button.builder(Component.translatable("screen.jn1.lift_platform.dress"),
				b -> this.cliquer(PlatformScreenHandler.BOUTON_HABILLER_TOUT)).bounds(this.leftPos + 117, y, 52, 18)
				.tooltip(Tooltip.create(Component.translatable("screen.jn1.lift_platform.dress.tip"))).build());
	}

	private void cliquer(int bouton) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, bouton);
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		boolean assemblée = this.menu.assemblée();
		this.assembler.active = this.menu.voisines() > 1
				&& (!assemblée || this.menu.voisines() > this.menu.taille());
		this.désassembler.active = assemblée;
		this.habiller.active = this.menu.taille() > 1;
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		super.extractLabels(context, mouseX, mouseY);
		int x = PlatformScreenHandler.CASE_X + 24;
		context.text(this.font, Component.translatable("screen.jn1.lift_platform.look"),
				PlatformScreenHandler.CASE_X - 4 - this.font.width(Component.translatable("screen.jn1.lift_platform.look")),
				PlatformScreenHandler.CASE_Y + 5, ENCRE, false);
		Component état = this.menu.assemblée()
				? Component.translatable("screen.jn1.lift_platform.assembled", this.menu.taille())
				: Component.translatable("screen.jn1.lift_platform.single");
		context.text(this.font, état, x, PlatformScreenHandler.CASE_Y, ENCRE, false);
		if (this.menu.voisines() > this.menu.taille()) {
			context.text(this.font, Component.translatable("screen.jn1.lift_platform.neighbours",
					this.menu.voisines()), x, PlatformScreenHandler.CASE_Y + 10, 0xFF7A5A10, false);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractBackground(context, mouseX, mouseY, delta);
		int g = this.leftPos;
		int h = this.topPos;
		int l = this.imageWidth;
		int t = this.imageHeight;
		context.fill(g, h, g + l, h + t, FOND);
		context.fill(g, h, g + l, h + 2, CLAIR);
		context.fill(g, h, g + 2, h + t, CLAIR);
		context.fill(g, h + t - 2, g + l, h + t, SOMBRE);
		context.fill(g + l - 2, h, g + l, h + t, SOMBRE);
		for (Slot slot : this.menu.slots) {
			int x = g + slot.x - 1;
			int y = h + slot.y - 1;
			context.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
			context.fill(x, y, x + 17, y + 1, 0xFF373737);
			context.fill(x, y, x + 1, y + 17, 0xFF373737);
			context.fill(x + 1, y + 17, x + 18, y + 18, CLAIR);
			context.fill(x + 17, y + 1, x + 18, y + 18, CLAIR);
		}
		// La case d'apparence, cerclée d'ambre : on y dépose, on ne prend pas.
		Slot modèle = this.menu.slots.getFirst();
		int x = g + modèle.x - 2;
		int y = h + modèle.y - 2;
		context.outline(x, y, 20, 20, 0xFFE0A030);
	}
}
