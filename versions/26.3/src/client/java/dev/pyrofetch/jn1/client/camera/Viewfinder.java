package dev.pyrofetch.jn1.client.camera;

import dev.pyrofetch.jn1.onlyfaps.Sujet;
import dev.pyrofetch.jn1.onlyfaps.Sujets;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Le viseur : un masque autour du cadrage, des repères (coins, tiers, mise au
 * point), qui est dans le cadre, les réglages en pastilles et le compte à
 * rebours. Il remplace l'interface du jeu. La fenêtre est exactement ce que la
 * photo garde ({@link PhotoCapture}).
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public final class Viewfinder {
	private static final int BOÎTIER = -435812856;
	private static final int LISERÉ = -14736341;
	private static final int REPÈRE = 1728053247;
	private static final int PASTILLE = -1072688104;
	private static final int PASTILLE_BORD = -13024692;
	private static final int MARGE = 10;
	private static final int PASTILLE_HAUTEUR = 15;
	private static final int TIERS = 0x30FFFFFF;
	private static final int NETTE = 0xFF6EE07A;
	/** Le relevé des filles dans le cadre, refait tous les cinq ticks. */
	private static List<Sujet> sujets = List.of();
	private static long relevéLe = -1000;

	private Viewfinder() {
	}

	public static void render(GuiGraphicsExtractor contexte, float tickDelta) {
		if (CameraSession.isActive() || CameraSession.compteÀRebours() >= 0 || !(CameraSession.éclair() <= 0.0F)) {
			Minecraft client = Minecraft.getInstance();
			int largeur = contexte.guiWidth();
			int hauteur = contexte.guiHeight();
			float éclair = CameraSession.éclair();
			if (éclair > 0.0F) {
				int alpha = (int)(éclair * 200.0F) << 24;
				contexte.fill(0, 0, largeur, hauteur, alpha | 16777215);
			}

			if (!CameraSession.isActive()) {
				if (CameraSession.compteÀRebours() >= 0) {
					compteÀRebours(contexte, client.font, largeur, hauteur);
				}
			} else {
				Viewfinder.Fenêtre fenêtre = cadrer(largeur, hauteur, CameraSession.format().rapport());
				masque(contexte, largeur, hauteur, fenêtre);
				relever(client);
				repères(contexte, fenêtre, !sujets.isEmpty());
				sujets(contexte, client.font, fenêtre);
				pastilles(contexte, client.font, fenêtre);
				if (CameraSession.compteÀRebours() >= 0) {
					compteÀRebours(contexte, client.font, largeur, hauteur);
				}
			}
		}
	}

	static Viewfinder.Fenêtre cadrer(int largeur, int hauteur, float rapport) {
		int disponibleL = largeur - 20;
		int disponibleH = hauteur - 20 - 15 - 6;
		int l = disponibleL;
		int h = Math.round(l / rapport);
		if (h > disponibleH) {
			h = disponibleH;
			l = Math.round(h * rapport);
		}

		int x = (largeur - l) / 2;
		int y = (hauteur - h) / 2;
		return new Viewfinder.Fenêtre(x, y, x + l, y + h);
	}

	private static void masque(GuiGraphicsExtractor contexte, int largeur, int hauteur, Viewfinder.Fenêtre f) {
		contexte.fill(0, 0, largeur, f.y1(), -435812856);
		contexte.fill(0, f.y2(), largeur, hauteur, -435812856);
		contexte.fill(0, f.y1(), f.x1(), f.y2(), -435812856);
		contexte.fill(f.x2(), f.y1(), largeur, f.y2(), -435812856);
		contexte.outline(f.x1() - 1, f.y1() - 1, f.largeur() + 2, f.hauteur() + 2, -14736341);
	}

	private static void repères(GuiGraphicsExtractor contexte, Viewfinder.Fenêtre f, boolean net) {
		// Les tiers, à peine marqués.
		for (int i = 1; i <= 2; i++) {
			int x = f.x1() + f.largeur() * i / 3;
			int y = f.y1() + f.hauteur() * i / 3;
			contexte.fill(x, f.y1(), x + 1, f.y2(), TIERS);
			contexte.fill(f.x1(), y, f.x2(), y + 1, TIERS);
		}

		// Les coins, en équerres de deux pixels.
		int longueur = Math.max(8, Math.min(f.largeur(), f.hauteur()) / 12);
		for (int côté = 0; côté < 4; côté++) {
			boolean droite = (côté & 1) != 0;
			boolean bas = (côté & 2) != 0;
			int x = droite ? f.x2() - longueur : f.x1();
			int y = bas ? f.y2() - 2 : f.y1();
			contexte.fill(x, y, x + longueur, y + 2, REPÈRE);
			x = droite ? f.x2() - 2 : f.x1();
			y = bas ? f.y2() - longueur : f.y1();
			contexte.fill(x, y, x + 2, y + longueur, REPÈRE);
		}

		// La mise au point : quatre coins autour du centre, verts quand une fille
		// est dans le cadre.
		int couleur = net ? NETTE : REPÈRE;
		int cx = (f.x1() + f.x2()) / 2;
		int cy = (f.y1() + f.y2()) / 2;
		int r = 9;
		int b = 4;
		for (int côté = 0; côté < 4; côté++) {
			int sx = (côté & 1) == 0 ? -1 : 1;
			int sy = (côté & 2) == 0 ? -1 : 1;
			int x = cx + sx * r;
			int y = cy + sy * r;
			contexte.fill(Math.min(x, x - sx * b), y, Math.max(x, x - sx * b) + 1, y + 1, couleur);
			contexte.fill(x, Math.min(y, y - sy * b), x + 1, Math.max(y, y - sy * b) + 1, couleur);
		}
		contexte.fill(cx, cy, cx + 1, cy + 1, couleur);
	}

	/** Refait le relevé des filles cadrées, avec le calcul du serveur, tous les cinq ticks. */
	private static void relever(Minecraft client) {
		if (client.level == null) {
			sujets = List.of();
			return;
		}
		long maintenant = client.level.getGameTime();
		if (maintenant >= relevéLe && maintenant - relevéLe < 5) {
			return;
		}
		relevéLe = maintenant;
		var fenêtre = client.getWindow();
		sujets = Sujets.relever(client.level, PhotoCapture.pose(client, fenêtre.getWidth(),
				fenêtre.getHeight(), CameraSession.format()));
	}

	/** En haut à gauche du cadre : qui sera sur la photo — ce qu'OnlyFaps paiera. */
	private static void sujets(GuiGraphicsExtractor contexte, Font police, Viewfinder.Fenêtre f) {
		Component texte;
		if (sujets.isEmpty()) {
			texte = Component.translatable("camera.jn1.subjects.none").withStyle(ChatFormatting.GRAY);
		} else {
			Component nom = Component.translatable("entity.jn1." + sujets.get(0).fille());
			texte = (sujets.size() == 1
					? Component.translatable("camera.jn1.subjects.one", nom)
					: Component.translatable("camera.jn1.subjects.many", nom, sujets.size() - 1))
					.withStyle(ChatFormatting.GREEN);
		}
		contexte.text(police, texte, f.x1() + 6, f.y1() + 6, -1);
	}

	private static void pastilles(GuiGraphicsExtractor contexte, Font police, Viewfinder.Fenêtre f) {
		int y = f.y2() + 4;
		int x = f.x1();
		x = pastille(contexte, police, x, y, Component.translatable("camera.jn1.button.format",
				touche("key.jn1.photo_format"), Component.translatable(CameraSession.format().translationKey())));
		if (CameraSession.surPied()) {
			int retard = CameraSession.retard();
			x = pastille(contexte, police, x, y, Component.translatable("camera.jn1.button.timer", touche("key.jn1.photo_timer"),
					retard == 0 ? Component.translatable("camera.jn1.timer.off") : Component.literal(retard + " s")));
		}

		x = pastille(contexte, police, x, y, Component.translatable("camera.jn1.button.shutter", touche("key.attack")));
		pastille(contexte, police, x, y, Component.translatable("camera.jn1.button.close", touche("key.jn1.photo_close")));
	}

	/** La touche telle que le joueur l'a réglée, et non celle d'origine écrite en dur. */
	private static MutableComponent touche(String clé) {
		return Component.keybind(clé);
	}

	private static int pastille(GuiGraphicsExtractor contexte, Font police, int x, int y, Component libellé) {
		int largeur = police.width(libellé) + 10;
		contexte.fill(x, y, x + largeur, y + 15, -1072688104);
		contexte.outline(x, y, largeur, 15, -13024692);
		contexte.text(police, libellé, x + 5, y + 4, -2696223, false);
		return x + largeur + 4;
	}

	private static void compteÀRebours(GuiGraphicsExtractor contexte, Font police, int largeur, int hauteur) {
		int restant = CameraSession.compteÀRebours();
		if (restant >= 0) {
			int secondes = (restant + 19) / 20;
			Component texte = Component.literal(String.valueOf(Math.max(1, secondes))).withStyle(secondes <= 1 ? ChatFormatting.RED : ChatFormatting.WHITE);
			contexte.pose().pushMatrix();
			contexte.pose().translate(largeur / 2.0F, hauteur / 3.0F);
			contexte.pose().scale(4.0F, 4.0F);
			contexte.centeredText(police, texte, 0, 0, -1);
			contexte.pose().popMatrix();
		}
	}

	record Fenêtre(int x1, int y1, int x2, int y2) {
		int largeur() {
			return this.x2 - this.x1;
		}

		int hauteur() {
			return this.y2 - this.y1;
		}
	}
}
