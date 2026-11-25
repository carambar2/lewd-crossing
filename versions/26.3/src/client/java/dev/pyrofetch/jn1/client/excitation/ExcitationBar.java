package dev.pyrofetch.jn1.client.excitation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * La barre d'excitation : verticale, à gauche de la barre d'objets et de la main gauche, du rose
 * pâle au rose vif. Elle n'est dessinée que lorsqu'elle n'est pas vide, et
 * bat quand elle approche du plein.
 */
public final class ExcitationBar {
	private static final int LARGEUR = 5;
	private static final int HAUTEUR = 40;
	private static final int CADRE = 0xFF20101A;
	private static final int FOND = 0xC03A2A34;
	private static final int PÂLE = 0xFFE8A0C0;
	private static final int VIF = 0xFFFF3C8C;

	/** Ce que le serveur a envoyé, de 0 à 100 ; ce que la barre montre, lissé. */
	private static int reçu;
	private static float affiché;

	private ExcitationBar() {
	}

	public static void recevoir(int niveau) {
		reçu = niveau;
	}

	public static void render(GuiGraphicsExtractor contexte, float tickDelta) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) {
			reçu = client.player == null ? 0 : reçu;
			return;
		}
		affiché += (reçu - affiché) * 0.15F;
		if (affiché < 0.5F && reçu == 0) {
			affiché = 0;
			return;
		}
		// À gauche de l'emplacement de la main gauche (qui va jusqu'à -120) : il
		// la recouvrait, là même où l'on tient le flacon.
		int x = contexte.guiWidth() / 2 - 124 - LARGEUR;
		int bas = contexte.guiHeight() - 3;
		int haut = bas - HAUTEUR;
		contexte.fill(x - 1, haut - 1, x + LARGEUR + 1, bas + 1, CADRE);
		contexte.fill(x, haut, x + LARGEUR, bas, FOND);
		float part = Math.clamp(affiché / 100.0F, 0.0F, 1.0F);
		int plein = Math.round(HAUTEUR * part);
		int couleur = mélange(PÂLE, VIF, part);
		if (part > 0.85F) {
			// Elle bat : l'éclat suit le temps du monde.
			float battement = 0.5F + 0.5F * (float) Math.sin((client.player.tickCount + tickDelta) * 0.6F);
			couleur = mélange(couleur, 0xFFFFFFFF, 0.35F * battement);
		}
		contexte.fill(x, bas - plein, x + LARGEUR, bas, couleur);
		contexte.text(client.font, "♥", x - 1, haut - 10, couleur, true);
	}

	private static int mélange(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}
}
