package dev.pyrofetch.jn1.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import dev.pyrofetch.jn1.block.VendingMachineBlockEntity;
import dev.pyrofetch.jn1.screen.VendingEditScreenHandler;
import dev.pyrofetch.jn1.shop.ShopEntry;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Les rayons d'un distributeur, en créatif : ce qu'il vend, et à quel prix.
 *
 * <p>Les douze cases se garnissent comme un coffre ; la pile posée est ce que
 * l'achat rendra, quantité comprise. À droite de chaque case, son prix, en
 * diodes : la molette l'ajuste (Maj : par dix), un clic permet de le taper.
 * Chaque changement part aussitôt au serveur, qui le revérifie ; il n'y a rien
 * à valider en sortant.
 *
 * <p>Même caisse rouge que la vitrine ({@link VendingScreen}), vue de
 * l'intérieur : c'est la même machine, porte ouverte.
 */
public class VendingEditScreen extends AbstractContainerScreen<VendingEditScreenHandler> {
	private static final int CAISSE = 0xFF7A1616;
	private static final int CAISSE_CLAIRE = 0xFFA82A26;
	private static final int CAISSE_SOMBRE = 0xFF4A0C0C;
	private static final int VITRE = 0xFF121A1E;
	private static final int PANNEAU = 0xFFC6C6C6;
	private static final int CASE_FOND = 0xFF8B8B8B;
	private static final int CASE_BORD = 0xFF373737;
	private static final int DIODE = 0xFF6CF08A;
	private static final int AMBRE = 0xFFE8C860;
	private static final int CRÈME = 0xFFF4E8D8;

	private static final int PRIX_DX = 21;
	private static final int PRIX_L = VendingEditScreenHandler.CASE_L - PRIX_DX - 3;
	private static final int PRIX_DY = 5;
	private static final int PRIX_H = 13;

	private static final int BOUTON_Y = VendingEditScreenHandler.RAYON_Y + 4 * VendingEditScreenHandler.CASE_H + 6;
	private static final int BOUTON_H = 14;

	/** La case dont on tape le prix, ou -1. */
	private int saisie = -1;
	private String tampon = "";

	public VendingEditScreen(VendingEditScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title, VendingEditScreenHandler.LARGEUR, VendingEditScreenHandler.INVENTAIRE_Y + 82);
		this.titleLabelX = 8;
		this.titleLabelY = 8;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = VendingEditScreenHandler.INVENTAIRE_Y - 11;
	}

	private int caseX(int rayon) {
		return this.leftPos + VendingEditScreenHandler.RAYON_X + (rayon % VendingEditScreenHandler.COLONNES)
				* VendingEditScreenHandler.CASE_L;
	}

	private int caseY(int rayon) {
		return this.topPos + VendingEditScreenHandler.RAYON_Y + (rayon / VendingEditScreenHandler.COLONNES)
				* VendingEditScreenHandler.CASE_H;
	}

	/** Le prix sous la souris, ou -1. */
	private int prixSous(double mouseX, double mouseY) {
		for (int rayon = 0; rayon < VendingEditScreenHandler.RAYONS; rayon++) {
			int px = this.caseX(rayon) + PRIX_DX;
			int py = this.caseY(rayon) + PRIX_DY;
			if (mouseX >= px && mouseX < px + PRIX_L && mouseY >= py && mouseY < py + PRIX_H) {
				return rayon;
			}
		}
		return -1;
	}

	/** Le bouton se taille sur son libellé, qui n'a pas la même longueur partout. */
	private int largeurBouton() {
		return this.font.width(Component.translatable("screen.jn1.vending.reset")) + 12;
	}

	private boolean surBouton(double mouseX, double mouseY) {
		int bx = this.leftPos + 8;
		int by = this.topPos + BOUTON_Y;
		return mouseX >= bx && mouseX < bx + this.largeurBouton() && mouseY >= by && mouseY < by + BOUTON_H;
	}

	/** Le code de la case, comme sur la vitrine : A1, A2, A3, B1… */
	private static String code(int rayon) {
		return (char) ('A' + rayon / VendingEditScreenHandler.COLONNES)
				+ String.valueOf(rayon % VendingEditScreenHandler.COLONNES + 1);
	}

	private void fixer(int rayon, int prix) {
		int borné = Math.clamp(prix, 0, VendingMachineBlockEntity.PRIX_MAX);
		if (this.minecraft == null || this.minecraft.gameMode == null) {
			return;
		}
		// Affiché tout de suite ; le serveur renverra le même chiffre.
		this.menu.setData(rayon, borné);
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
				VendingEditScreenHandler.boutonPrix(rayon, borné));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractBackground(context, mouseX, mouseY, delta);
		int gauche = this.leftPos;
		int haut = this.topPos;
		int largeur = this.imageWidth;
		int hauteur = this.imageHeight;

		context.fill(gauche, haut, gauche + largeur, haut + hauteur, CAISSE);
		context.fill(gauche, haut, gauche + largeur, haut + 2, CAISSE_CLAIRE);
		context.fill(gauche, haut, gauche + 2, haut + hauteur, CAISSE_CLAIRE);
		context.fill(gauche, haut + hauteur - 2, gauche + largeur, haut + hauteur, CAISSE_SOMBRE);
		context.fill(gauche + largeur - 2, haut, gauche + largeur, haut + hauteur, CAISSE_SOMBRE);

		// Les rayons, derrière la vitre.
		int vx = gauche + VendingEditScreenHandler.RAYON_X - 3;
		int vy = haut + VendingEditScreenHandler.RAYON_Y - 3;
		int vl = VendingEditScreenHandler.COLONNES * VendingEditScreenHandler.CASE_L + 4;
		int vh = 4 * VendingEditScreenHandler.CASE_H + 4;
		context.fill(vx - 2, vy - 2, vx + vl + 2, vy + vh + 2, CAISSE_SOMBRE);
		context.fill(vx, vy, vx + vl, vy + vh, VITRE);

		for (int rayon = 0; rayon < VendingEditScreenHandler.RAYONS; rayon++) {
			int cx = this.caseX(rayon);
			int cy = this.caseY(rayon);
			context.fill(cx, cy + 2, cx + 18, cy + 20, CASE_BORD);
			context.fill(cx + 1, cy + 3, cx + 17, cy + 19, 0xFF2A3238);

			int px = cx + PRIX_DX;
			int py = cy + PRIX_DY;
			boolean saisi = rayon == this.saisie;
			context.fill(px - 1, py - 1, px + PRIX_L + 1, py + PRIX_H + 1, saisi ? AMBRE : 0xFF30383E);
			context.fill(px, py, px + PRIX_L, py + PRIX_H, 0xFF050805);
			String chiffres;
			if (saisi) {
				boolean curseur = (System.currentTimeMillis() / 400) % 2 == 0;
				chiffres = this.tampon + (curseur ? "_" : " ");
			} else {
				chiffres = String.valueOf(this.menu.prix(rayon));
			}
			boolean vide = this.menu.getSlot(rayon).getItem().isEmpty();
			int couleur = saisi ? AMBRE : vide ? 0xFF2C5A38 : DIODE;
			context.text(this.font, chiffres,
					px + PRIX_L - 3 - this.font.width(chiffres), py + 3, couleur, false);
		}

		// Rétablir le catalogue.
		boolean dessus = this.surBouton(mouseX, mouseY);
		int bx = gauche + 8;
		int by = haut + BOUTON_Y;
		int bl = this.largeurBouton();
		context.fill(bx - 1, by - 1, bx + bl + 1, by + BOUTON_H + 1, CAISSE_SOMBRE);
		context.fill(bx, by, bx + bl, by + BOUTON_H, dessus ? 0xFF3A4046 : 0xFF23282C);
		Component libellé = Component.translatable("screen.jn1.vending.reset");
		context.text(this.font, libellé,
				bx + (bl - this.font.width(libellé)) / 2, by + 3, CRÈME, false);

		// L'état de la machine, en face du bouton.
		Component état = Component.translatable(this.menu.garnie()
				? "screen.jn1.vending.custom" : "screen.jn1.vending.catalogue");
		context.text(this.font, état,
				gauche + largeur - 8 - this.font.width(état), by + 3,
				this.menu.garnie() ? AMBRE : 0xFFD8B0A8, false);

		// L'inventaire du joueur, sur un panneau ordinaire.
		int iy = haut + VendingEditScreenHandler.INVENTAIRE_Y - 14;
		context.fill(gauche + 4, iy, gauche + largeur - 4, haut + hauteur - 4, PANNEAU);
		for (int ligne = 0; ligne < 4; ligne++) {
			int sy = haut + VendingEditScreenHandler.INVENTAIRE_Y + ligne * 18 + (ligne == 3 ? 4 : 0) - 1;
			for (int colonne = 0; colonne < 9; colonne++) {
				int sx = gauche + 8 + colonne * 18 - 1;
				context.fill(sx, sy, sx + 18, sy + 18, CASE_BORD);
				context.fill(sx + 1, sy + 1, sx + 17, sy + 17, CASE_FOND);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, CRÈME, false);
		context.item(new ItemStack(ShopEntry.CURRENCY), this.imageWidth - 24, 4);
		context.text(this.font, this.playerInventoryTitle,
				this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		// Le fond et le panneau sont dans leur strate ; l'info-bulle d'une case vient avec le reste.
		super.extractRenderState(context, mouseX, mouseY, delta);
		if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
			return;
		}
		int rayon = this.prixSous(mouseX, mouseY);
		if (rayon >= 0 && rayon != this.saisie) {
			context.setComponentTooltipForNextFrame(this.font, List.of(
					Component.translatable("screen.jn1.vending.price_of", code(rayon)),
					Component.translatable("screen.jn1.vending.price_help").withColor(0xA0A0A0)), mouseX, mouseY);
		} else if (this.surBouton(mouseX, mouseY)) {
			context.setTooltipForNextFrame(this.font,
					Component.translatable("screen.jn1.vending.reset_help"), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		int rayon = this.prixSous(mouseX, mouseY);
		if (rayon >= 0 && vertical != 0) {
			this.saisie = -1;
			int pas = this.minecraft.hasShiftDown() ? 10 : 1;
			this.fixer(rayon, this.menu.prix(rayon) + (vertical > 0 ? pas : -pas));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent événement, boolean doubleClic) {
		double mouseX = événement.x();
		double mouseY = événement.y();
		int button = événement.button();
		int rayon = this.prixSous(mouseX, mouseY);
		if (rayon >= 0) {
			this.saisie = rayon;
			this.tampon = "";
			this.clic();
			return true;
		}
		this.saisie = -1;
		if (button == InputConstants.MOUSE_BUTTON_LEFT && this.surBouton(mouseX, mouseY) && this.minecraft != null
				&& this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, VendingEditScreenHandler.BOUTON_RÉTABLIR);
			this.clic();
			return true;
		}
		return super.mouseClicked(événement, doubleClic);
	}

	/**
	 * La saisie d'un prix passe avant tout : les chiffres, sinon, échangeraient
	 * la case survolée avec la barre d'action, et Échap fermerait l'écran.
	 */
	@Override
	public boolean keyPressed(KeyEvent événement) {
		int keyCode = événement.key();
		if (this.saisie >= 0) {
			int chiffre = chiffre(keyCode);
			if (chiffre >= 0) {
				if (this.tampon.length() < String.valueOf(VendingMachineBlockEntity.PRIX_MAX).length()) {
					this.tampon = (this.tampon + chiffre).replaceFirst("^0+(?=\\d)", "");
					this.fixer(this.saisie, Integer.parseInt(this.tampon));
				}
				return true;
			}
			if (keyCode == InputConstants.KEY_BACKSPACE) {
				if (!this.tampon.isEmpty()) {
					this.tampon = this.tampon.substring(0, this.tampon.length() - 1);
				}
				this.fixer(this.saisie, this.tampon.isEmpty() ? 0 : Integer.parseInt(this.tampon));
				return true;
			}
			if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER
					|| keyCode == InputConstants.KEY_TAB || keyCode == InputConstants.KEY_ESCAPE) {
				this.saisie = -1;
				return true;
			}
		}
		return super.keyPressed(événement);
	}

	/**
	 * Le chiffre d'une touche, rangée du haut ou pavé numérique ; -1 sinon. Les
	 * codes de touche de SDL (26.3) ne se suivent pas de 0 à 9 comme ceux de GLFW :
	 * le 0 vient après le 9, et le pavé de même.
	 */
	private static int chiffre(int touche) {
		return switch (touche) {
			case InputConstants.KEY_0, InputConstants.KEY_NUMPAD0 -> 0;
			case InputConstants.KEY_1, InputConstants.KEY_NUMPAD1 -> 1;
			case InputConstants.KEY_2, InputConstants.KEY_NUMPAD2 -> 2;
			case InputConstants.KEY_3, InputConstants.KEY_NUMPAD3 -> 3;
			case InputConstants.KEY_4, InputConstants.KEY_NUMPAD4 -> 4;
			case InputConstants.KEY_5, InputConstants.KEY_NUMPAD5 -> 5;
			case InputConstants.KEY_6, InputConstants.KEY_NUMPAD6 -> 6;
			case InputConstants.KEY_7, InputConstants.KEY_NUMPAD7 -> 7;
			case InputConstants.KEY_8, InputConstants.KEY_NUMPAD8 -> 8;
			case InputConstants.KEY_9, InputConstants.KEY_NUMPAD9 -> 9;
			default -> -1;
		};
	}

	private void clic() {
		if (this.minecraft != null) {
			this.minecraft.getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, 0.3F));
		}
	}
}
