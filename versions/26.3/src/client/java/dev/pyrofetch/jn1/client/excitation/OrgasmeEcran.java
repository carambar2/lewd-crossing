package dev.pyrofetch.jn1.client.excitation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * L'orgasme à l'écran : un éclair blanc qui retombe en deux secondes et demie,
 * un voile rose qui bat sur les bords, trois battements de cœur. Dessiné
 * par-dessus tout le reste de l'interface.
 */
public final class OrgasmeEcran {
	private static final int DURÉE = 50;
	private static int début = -1;

	private OrgasmeEcran() {
	}

	public static void déclencher() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		début = client.player.tickCount;
		client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.2F));
	}

	public static void render(GuiGraphicsExtractor contexte, float tickDelta) {
		Minecraft client = Minecraft.getInstance();
		if (début < 0 || client.player == null) {
			return;
		}
		float t = (client.player.tickCount - début + tickDelta) / DURÉE;
		if (t >= 1.0F || t < 0.0F) {
			début = -1;
			return;
		}
		int largeur = contexte.guiWidth();
		int hauteur = contexte.guiHeight();
		// L'éclair : plein d'abord, puis il s'efface en courbe.
		float éclair = (float) Math.pow(1.0F - t, 2.2F);
		int blanc = Math.round(235 * éclair);
		if (blanc > 0) {
			contexte.fill(0, 0, largeur, hauteur, (blanc << 24) | 0xFFF4F8);
		}
		// Le voile rose sur les bords, qui bat trois fois.
		float battement = 0.5F + 0.5F * (float) Math.cos(t * Math.PI * 6);
		int alpha = Math.round(150 * (1.0F - t) * battement);
		int bord = Math.max(12, largeur / 9);
		for (int i = 0; i < bord; i += 2) {
			int a = Math.round(alpha * (1.0F - (float) i / bord));
			if (a <= 0) {
				continue;
			}
			int couleur = (a << 24) | 0xFF4F9A;
			contexte.fill(i, i, largeur - i, i + 2, couleur);
			contexte.fill(i, hauteur - i - 2, largeur - i, hauteur - i, couleur);
			contexte.fill(i, i + 2, i + 2, hauteur - i - 2, couleur);
			contexte.fill(largeur - i - 2, i + 2, largeur - i, hauteur - i - 2, couleur);
		}
	}
}
