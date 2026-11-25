package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.block.VendingMachineBlockEntity;
import dev.pyrofetch.jn1.screen.VendingEditScreenHandler;
import dev.pyrofetch.jn1.shop.ShopEntry;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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
public class VendingEditScreen extends HandledScreen<VendingEditScreenHandler> {
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

	public VendingEditScreen(VendingEditScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = VendingEditScreenHandler.LARGEUR;
		this.backgroundHeight = VendingEditScreenHandler.INVENTAIRE_Y + 82;
		this.titleX = 8;
		this.titleY = 8;
		this.playerInventoryTitleX = 8;
		this.playerInventoryTitleY = VendingEditScreenHandler.INVENTAIRE_Y - 11;
	}

	private int caseX(int rayon) {
		return this.x + VendingEditScreenHandler.RAYON_X + (rayon % VendingEditScreenHandler.COLONNES)
				* VendingEditScreenHandler.CASE_L;
	}

	private int caseY(int rayon) {
		return this.y + VendingEditScreenHandler.RAYON_Y + (rayon / VendingEditScreenHandler.COLONNES)
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
		return this.textRenderer.getWidth(Text.translatable("screen.jn1.vending.reset")) + 12;
	}

	private boolean surBouton(double mouseX, double mouseY) {
		int bx = this.x + 8;
		int by = this.y + BOUTON_Y;
		return mouseX >= bx && mouseX < bx + this.largeurBouton() && mouseY >= by && mouseY < by + BOUTON_H;
	}

	/** Le code de la case, comme sur la vitrine : A1, A2, A3, B1… */
	private static String code(int rayon) {
		return (char) ('A' + rayon / VendingEditScreenHandler.COLONNES)
				+ String.valueOf(rayon % VendingEditScreenHandler.COLONNES + 1);
	}

	private void fixer(int rayon, int prix) {
		int borné = Math.clamp(prix, 0, VendingMachineBlockEntity.PRIX_MAX);
		if (this.client == null || this.client.interactionManager == null) {
			return;
		}
		// Affiché tout de suite ; le serveur renverra le même chiffre.
		this.handler.setProperty(rayon, borné);
		this.client.interactionManager.clickButton(this.handler.syncId,
				VendingEditScreenHandler.boutonPrix(rayon, borné));
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int gauche = this.x;
		int haut = this.y;
		int largeur = this.backgroundWidth;
		int hauteur = this.backgroundHeight;

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
				chiffres = String.valueOf(this.handler.prix(rayon));
			}
			boolean vide = this.handler.getSlot(rayon).getStack().isEmpty();
			int couleur = saisi ? AMBRE : vide ? 0xFF2C5A38 : DIODE;
			context.drawText(this.textRenderer, chiffres,
					px + PRIX_L - 3 - this.textRenderer.getWidth(chiffres), py + 3, couleur, false);
		}

		// Rétablir le catalogue.
		boolean dessus = this.surBouton(mouseX, mouseY);
		int bx = gauche + 8;
		int by = haut + BOUTON_Y;
		int bl = this.largeurBouton();
		context.fill(bx - 1, by - 1, bx + bl + 1, by + BOUTON_H + 1, CAISSE_SOMBRE);
		context.fill(bx, by, bx + bl, by + BOUTON_H, dessus ? 0xFF3A4046 : 0xFF23282C);
		Text libellé = Text.translatable("screen.jn1.vending.reset");
		context.drawText(this.textRenderer, libellé,
				bx + (bl - this.textRenderer.getWidth(libellé)) / 2, by + 3, CRÈME, false);

		// L'état de la machine, en face du bouton.
		Text état = Text.translatable(this.handler.garnie()
				? "screen.jn1.vending.custom" : "screen.jn1.vending.catalogue");
		context.drawText(this.textRenderer, état,
				gauche + largeur - 8 - this.textRenderer.getWidth(état), by + 3,
				this.handler.garnie() ? AMBRE : 0xFFD8B0A8, false);

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
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, CRÈME, false);
		context.drawItem(new ItemStack(ShopEntry.CURRENCY), this.backgroundWidth - 24, 4);
		context.drawText(this.textRenderer, this.playerInventoryTitle,
				this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF404040, false);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// HandledScreen.render floute le fond lui-même, avant le panneau.
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
		if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
			return;
		}
		int rayon = this.prixSous(mouseX, mouseY);
		if (rayon >= 0 && rayon != this.saisie) {
			context.drawTooltip(this.textRenderer, List.of(
					Text.translatable("screen.jn1.vending.price_of", code(rayon)),
					Text.translatable("screen.jn1.vending.price_help").withColor(0xA0A0A0)), mouseX, mouseY);
		} else if (this.surBouton(mouseX, mouseY)) {
			context.drawTooltip(this.textRenderer,
					Text.translatable("screen.jn1.vending.reset_help"), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		int rayon = this.prixSous(mouseX, mouseY);
		if (rayon >= 0 && vertical != 0) {
			this.saisie = -1;
			int pas = hasShiftDown() ? 10 : 1;
			this.fixer(rayon, this.handler.prix(rayon) + (vertical > 0 ? pas : -pas));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int rayon = this.prixSous(mouseX, mouseY);
		if (rayon >= 0) {
			this.saisie = rayon;
			this.tampon = "";
			this.clic();
			return true;
		}
		this.saisie = -1;
		if (button == 0 && this.surBouton(mouseX, mouseY) && this.client != null
				&& this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, VendingEditScreenHandler.BOUTON_RÉTABLIR);
			this.clic();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/**
	 * La saisie d'un prix passe avant tout : les chiffres, sinon, échangeraient
	 * la case survolée avec la barre d'action, et Échap fermerait l'écran.
	 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.saisie >= 0) {
			int chiffre = -1;
			if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
				chiffre = keyCode - GLFW.GLFW_KEY_0;
			} else if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
				chiffre = keyCode - GLFW.GLFW_KEY_KP_0;
			}
			if (chiffre >= 0) {
				if (this.tampon.length() < String.valueOf(VendingMachineBlockEntity.PRIX_MAX).length()) {
					this.tampon = (this.tampon + chiffre).replaceFirst("^0+(?=\\d)", "");
					this.fixer(this.saisie, Integer.parseInt(this.tampon));
				}
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				if (!this.tampon.isEmpty()) {
					this.tampon = this.tampon.substring(0, this.tampon.length() - 1);
				}
				this.fixer(this.saisie, this.tampon.isEmpty() ? 0 : Integer.parseInt(this.tampon));
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
					|| keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_ESCAPE) {
				this.saisie = -1;
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void clic() {
		if (this.client != null) {
			this.client.getSoundManager().play(
					PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, 0.3F));
		}
	}
}
