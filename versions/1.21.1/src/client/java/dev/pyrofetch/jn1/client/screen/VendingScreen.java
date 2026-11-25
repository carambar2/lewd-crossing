package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.block.VendingMachineBlockEntity;
import dev.pyrofetch.jn1.network.ShopPurchasePayload;
import dev.pyrofetch.jn1.shop.ShopEntry;
import dev.pyrofetch.jn1.shop.VendingOffer;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * La façade du distributeur : une vitrine, des rayons, des codes, et un
 * afficheur à diodes pour la monnaie.
 *
 * <p>Même esprit que le terminal — tout est dessiné au rectangle, rien n'est
 * emprunté aux boutons du jeu — mais pas le même objet : c'est une machine
 * qu'on a devant soi, pas un système d'exploitation. Un clic sur une case
 * achète ; le serveur revérifie tout.
 *
 * <p>Ce qui est en vitrine est relu dans la machine à chaque image : une machine
 * garnie à la main montre ses rayons à leur case, les cases vides restant
 * vides, et un changement fait par quelqu'un d'autre paraît aussitôt.
 */
public class VendingScreen extends Screen {
	private static final int CAISSE = 0xFF7A1616;
	private static final int CAISSE_CLAIRE = 0xFFA82A26;
	private static final int CAISSE_SOMBRE = 0xFF4A0C0C;
	private static final int VITRE = 0xFF121A1E;
	private static final int RAYON = 0xFF4C545C;
	private static final int DIODE = 0xFF6CF08A;
	private static final int DIODE_ÉTEINTE = 0xFF1C3A24;
	private static final int ROUGE_DIODE = 0xFFF06060;

	private static final int COLONNES = 3;
	private static final int CASE_L = 64;
	private static final int CASE_H = 50;

	private final BlockPos distributeur;
	private int survol = -1;

	public VendingScreen(BlockPos distributeur) {
		super(Text.translatable("screen.jn1.vending.title"));
		this.distributeur = distributeur;
	}

	private List<VendingOffer> catalogue() {
		if (this.client == null || this.client.world == null) {
			return List.of();
		}
		return VendingMachineBlockEntity.vitrine(
				this.client.world.getBlockEntity(this.distributeur) instanceof VendingMachineBlockEntity machine
						? machine : null,
				this.client.world);
	}

	/**
	 * Le fond flouté une seule fois, avant tout le reste.
	 *
	 * <p>{@code Screen.render} relance lui-même {@code renderBackground}, et le
	 * flou du jeu s'applique à tout ce qui est déjà dessiné : appelé en fin de
	 * rendu, il brouillait le distributeur lui-même. On ne passe donc plus par
	 * lui ; il n'y a d'ailleurs aucun widget à dessiner.
	 */
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		List<VendingOffer> catalogue = this.catalogue();
		int dernière = catalogue.stream().mapToInt(VendingOffer::place).max().orElse(0);
		int rangées = dernière / COLONNES + 1;
		int largeur = COLONNES * CASE_L + 36;
		int hauteur = 34 + rangées * CASE_H + 40;
		int gauche = (this.width - largeur) / 2;
		int haut = (this.height - hauteur) / 2;
		int bourse = this.bourse();

		// La caisse, en relief.
		context.fill(gauche, haut, gauche + largeur, haut + hauteur, CAISSE);
		context.fill(gauche, haut, gauche + largeur, haut + 2, CAISSE_CLAIRE);
		context.fill(gauche, haut, gauche + 2, haut + hauteur, CAISSE_CLAIRE);
		context.fill(gauche, haut + hauteur - 2, gauche + largeur, haut + hauteur, CAISSE_SOMBRE);
		context.fill(gauche + largeur - 2, haut, gauche + largeur, haut + hauteur, CAISSE_SOMBRE);

		// Le fronton lumineux.
		context.fill(gauche + 8, haut + 7, gauche + largeur - 8, haut + 23, 0xFFF4E8D8);
		Text titre = this.title;
		context.drawText(this.textRenderer, titre,
				gauche + (largeur - this.textRenderer.getWidth(titre)) / 2, haut + 11, CAISSE, false);

		// La vitrine et ses rayons.
		int vitreX = gauche + 12;
		int vitreY = haut + 30;
		int vitreL = COLONNES * CASE_L + 12;
		int vitreH = rangées * CASE_H + 4;
		context.fill(vitreX - 2, vitreY - 2, vitreX + vitreL + 2, vitreY + vitreH + 2, CAISSE_SOMBRE);
		context.fill(vitreX, vitreY, vitreX + vitreL, vitreY + vitreH, VITRE);

		// Chaque case a son rayon, même vide : une machine garnie à la main peut
		// laisser des trous, et une vitrine sans étagère ne se lit pas.
		for (int place = 0; place < rangées * COLONNES; place++) {
			this.renderRayon(context, vitreX + 6 + (place % COLONNES) * CASE_L,
					vitreY + 2 + (place / COLONNES) * CASE_H);
		}
		this.survol = -1;
		for (int i = 0; i < catalogue.size(); i++) {
			int place = catalogue.get(i).place();
			int x = vitreX + 6 + (place % COLONNES) * CASE_L;
			int y = vitreY + 2 + (place / COLONNES) * CASE_H;
			boolean dessus = mouseX >= x && mouseX < x + CASE_L - 4 && mouseY >= y && mouseY < y + CASE_H - 4;
			if (dessus) {
				this.survol = i;
			}
			this.renderCase(context, catalogue.get(i), place, x, y, dessus, bourse);
		}
		// Un reflet en travers de la vitre : c'est ce qui la fait lire comme du verre.
		for (int k = 0; k < 6; k++) {
			int x = vitreX + vitreL - 40 + k;
			context.fill(x, vitreY, x + 1, vitreY + vitreH, 0x0CFFFFFF);
		}

		// L'afficheur de monnaie, en bas.
		int afficheurY = vitreY + vitreH + 10;
		context.fill(gauche + 12, afficheurY, gauche + 112, afficheurY + 16, 0xFF050805);
		String chiffres = String.format("%04d", Math.min(bourse, 9999));
		context.drawText(this.textRenderer, "8888", gauche + 18, afficheurY + 4, DIODE_ÉTEINTE, false);
		context.drawText(this.textRenderer, chiffres, gauche + 18, afficheurY + 4, DIODE, false);
		context.drawItem(new ItemStack(ShopEntry.CURRENCY), gauche + 90, afficheurY);

		// La fente et le bac, pour qu'on sache où tombe ce qu'on achète.
		context.fill(gauche + largeur - 70, afficheurY + 2, gauche + largeur - 40, afficheurY + 5, 0xFF1A1A1A);
		context.fill(gauche + largeur - 32, afficheurY - 2, gauche + largeur - 12, afficheurY + 18, 0xFF1A1A1A);
		context.fill(gauche + largeur - 30, afficheurY, gauche + largeur - 14, afficheurY + 16, 0xFF2C2C2C);

		if (this.survol >= 0) {
			context.drawItemTooltip(this.textRenderer, catalogue.get(this.survol).stack(), mouseX, mouseY);
		}
	}

	private void renderCase(DrawContext context, VendingOffer article, int index, int x, int y,
	                        boolean dessus, int bourse) {
		int l = CASE_L - 4;
		int h = CASE_H - 4;
		if (dessus) {
			context.fill(x, y, x + l, y + h, 0x22FFFFFF);
		}
		context.drawItem(article.stack(), x + l / 2 - 8, y + 6);
		context.drawItemInSlot(this.textRenderer, article.stack(), x + l / 2 - 8, y + 6);

		// Le code de la case, puis le prix, en diodes.
		String code = (char) ('A' + index / COLONNES) + String.valueOf(index % COLONNES + 1);
		context.fill(x + 4, y + 31, x + l - 4, y + 42, 0xFF050805);
		context.drawText(this.textRenderer, code, x + 7, y + 33, 0xFFE8C860, false);
		Text prix = Text.translatable("screen.jn1.shop.price", article.price());
		context.drawText(this.textRenderer, prix, x + l - 7 - this.textRenderer.getWidth(prix), y + 33,
				bourse >= article.price() ? DIODE : ROUGE_DIODE, false);
	}

	/** Le rayon d'une case, et la spirale qui tient l'objet. */
	private void renderRayon(DrawContext context, int x, int y) {
		int l = CASE_L - 4;
		context.fill(x + 4, y + 26, x + l - 4, y + 28, RAYON);
		for (int k = 0; k < 5; k++) {
			context.fill(x + 12 + k * 7, y + 20, x + 14 + k * 7, y + 26, 0xFF8C949C);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.survol >= 0) {
			List<VendingOffer> catalogue = this.catalogue();
			if (this.survol < catalogue.size()) {
				ClientPlayNetworking.send(new ShopPurchasePayload(this.distributeur, catalogue.get(this.survol).id()));
				if (this.client != null) {
					this.client.getSoundManager().play(
							PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, 0.3F));
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private int bourse() {
		if (this.client == null || this.client.player == null) {
			return 0;
		}
		int total = 0;
		var inventaire = this.client.player.getInventory();
		for (int emplacement = 0; emplacement < inventaire.size(); emplacement++) {
			ItemStack pile = inventaire.getStack(emplacement);
			if (pile.isOf(ShopEntry.CURRENCY)) {
				total += pile.getCount();
			}
		}
		return total;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
