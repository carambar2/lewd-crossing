package dev.pyrofetch.jn1.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Le panneau commun aux écrans d'une compagne — le sien, ses tenues, ses
 * demandes (0.53.3) : un cadre rose sur fond sombre, elle à gauche dans sa
 * case, le reste à droite. Les trois écrans s'ouvrent l'un depuis l'autre ;
 * de mêmes dimensions, on passe de l'un à l'autre sans que rien ne saute.
 */
final class Panneau {
	static final int LARGEUR = 360;
	static final int HAUTEUR = 190;
	static final int PORTRAIT = 110;
	static final int BOUTON = 170;
	static final int BOUTON_H = 20;
	static final int PAS = 23;
	/** Où commence la colonne de droite, depuis le bord gauche du panneau. */
	static final int DROITE = PORTRAIT + 24;

	static final int FOND = 0xE0181018;
	static final int CADRE = 0xFFB8506F;
	static final int CADRE_CLAIR = 0xFFE894AE;
	static final int CASE = 0xFF3A2433;
	static final int TITRE = 0xFFFFFFFF;
	static final int NOTE = 0xFFC8A0B4;

	private Panneau() {
	}

	static int gauche(int largeurÉcran) {
		return (largeurÉcran - LARGEUR) / 2;
	}

	static int haut(int hauteurÉcran) {
		return (hauteurÉcran - HAUTEUR) / 2;
	}

	/** Le cadre, puis elle dans sa case. À appeler après le flou du jeu. */
	static void dessiner(GuiGraphicsExtractor contexte, int x, int y, Portrait portrait, int sourisX, int sourisY) {
		contexte.fill(x - 1, y - 1, x + LARGEUR + 1, y + HAUTEUR + 1, CADRE);
		contexte.fill(x, y, x + LARGEUR, y + HAUTEUR, FOND);
		contexte.fill(x, y, x + LARGEUR, y + 1, CADRE_CLAIR);
		int px = x + 8;
		int py = y + 8;
		int bas = py + HAUTEUR - 16;
		contexte.fill(px, py, px + PORTRAIT, bas, CASE);
		// Elle regarde devant elle, et ne suit la souris que de loin : les boutons
		// étant tous à sa droite, le regard plein lui tordait la tête à fond
		// (0.53.0).
		float cx = px + PORTRAIT / 2.0F;
		float cy = py + (HAUTEUR - 16) * 0.3F;
		// 70 pixels par bloc : Ellie et Gardevoir, plus hautes que Jenny, y
		// tiennent la tête entière (à 78, leur crâne passait sous le cadre).
		portrait.dessiner(contexte, px, py, px + PORTRAIT, bas, 70, 0.0625F,
				cx + (sourisX - cx) * 0.15F, cy + (sourisY - cy) * 0.15F);
	}
}
