package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Son humeur et ses répliques, dessinées dans le panneau de ses écrans (0.56.0). */
final class Humeur {
	private static final int RÉPLIQUE = 0xFFF2D6E2;

	private Humeur() {
	}

	/** De « Furious » à « Elated », en couleur, aligné à droite sur {@code droite}. */
	static void dessiner(GuiGraphicsExtractor contexte, Font police, CompanionEntity fille, int droite, int y) {
		int humeur = fille.getMood();
		String clé;
		int couleur;
		if (humeur <= -7) {
			clé = "furious";
			couleur = 0xFFE04848;
		} else if (humeur <= -3) {
			clé = "annoyed";
			couleur = 0xFFE09048;
		} else if (humeur < 3) {
			clé = "calm";
			couleur = 0xFFC8C8D0;
		} else if (humeur < 7) {
			clé = "happy";
			couleur = 0xFF8EE07A;
		} else {
			clé = "elated";
			couleur = 0xFFF27AAE;
		}
		Component texte = Component.translatable("screen.jn1.humeur." + clé);
		contexte.text(police, texte, droite - police.width(texte), y, couleur);
	}

	/** Une réplique entre guillemets, coupée à la largeur, sur {@code lignes} lignes au plus. */
	static void réplique(GuiGraphicsExtractor contexte, Font police, Component texte, int x, int y, int largeur, int lignes) {
		if (texte.getString().isEmpty()) {
			return;
		}
		List<FormattedCharSequence> coupé = police.split(Component.literal("“").append(texte).append("”"), largeur);
		for (int i = 0; i < Math.min(lignes, coupé.size()); i++) {
			contexte.text(police, coupé.get(i), x, y + i * 10, RÉPLIQUE);
		}
	}
}
