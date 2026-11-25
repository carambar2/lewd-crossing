package dev.pyrofetch.jn1.entity;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ce qu'on peut lui demander, dans le menu de la fille (« Demande »).
 *
 * <p>Suivant le niveau d'affection, plus ou moins de scènes sont débloquées ;
 * mais des scènes entières — branlette, fellation, branlette espagnole,
 * levrette —, pas chaque morceau. Le choix morceau par
 * morceau reste à la télécommande, pour les essais.
 *
 * <p>Une demande, c'est une introduction jouée une fois (elle s'approche, elle
 * se met en place), puis trois rythmes que le joueur choisit lui-même pendant la
 * scène (avancer : plus vite, reculer : plus lentement). Un rythme est une
 * animation et une vitesse de lecture : la branlette n'a qu'une animation, ses
 * rythmes ne diffèrent que par la vitesse. La barre d'excitation pleine amène la
 * fin « cu » de la scène, comme partout.
 *
 * <p>À l'affection maximale, chaque demande se fait aussi « brutalement » : les
 * versions brutales des animations (`_r`, tools/gen_brutal.py), ou, faute de
 * version brutale, la même plus vite.
 *
 * @param id     le nom envoyé par l'écran ({@code demande.jn1.<id>} pour son libellé)
 * @param seuil  l'affection qu'il faut
 * @param intro  jouée une fois au début, ou rien
 * @param rythmes du plus lent au plus rapide
 */
public record Demande(String id, int seuil, @Nullable String intro, List<Rythme> rythmes) {
	/** Un rythme : l'animation (sans préfixe) et sa vitesse de lecture. */
	public record Rythme(String scène, float vitesse) {
	}

	/** Plus vite, faute de version brutale de l'animation. */
	public static final float BRUTAL_VITESSE = 1.35F;

	public static final Demande BRANLETTE = new Demande("branlette", 12, null, List.of(
			new Rythme("b_hj", 0.7F), new Rythme("b_hj", 1.0F), new Rythme("b_hj", 1.45F)));
	public static final Demande FELLATION = new Demande("fellation", 14, "b_in", List.of(
			new Rythme("b_su", 0.8F), new Rythme("b_su", 1.0F), new Rythme("b_th", 1.0F)));
	public static final Demande ESPAGNOLE = new Demande("espagnole", 16, null, List.of(
			new Rythme("p_sl", 1.0F), new Rythme("p_st", 1.0F), new Rythme("p_fa", 1.0F)));
	public static final Demande LEVRETTE = new Demande("levrette", 18, "d_go", List.of(
			new Rythme("d_sl", 1.0F), new Rythme("d_st", 1.0F), new Rythme("d_fa", 1.0F)));

	/** Toutes, dans l'ordre où elles se débloquent. */
	public static final List<Demande> TOUTES = List.of(BRANLETTE, FELLATION, ESPAGNOLE, LEVRETTE);

	@Nullable
	public static Demande byId(String id) {
		for (Demande demande : TOUTES) {
			if (demande.id.equals(id)) {
				return demande;
			}
		}
		return null;
	}

	/** Le rythme de départ : le plus lent. */
	public static final int DÉPART = 0;

	/**
	 * Vrai si cette fille sait jouer toute la demande : chaque animation dans
	 * son fichier, et un partenaire (`steve`) à qui la jouer.
	 */
	public boolean possible(CompanionKind kind) {
		if (!kind.hasSceneProp()) {
			return false;
		}
		if (this.intro != null && CompanionScenes.ticks(kind, this.intro) <= 0) {
			return false;
		}
		for (Rythme rythme : this.rythmes) {
			if (CompanionScenes.ticks(kind, rythme.scène()) <= 0) {
				return false;
			}
		}
		return true;
	}

	/** Ce rythme, brutal ou non : la version `_r` si elle existe, sinon la même plus vite. */
	public Rythme rythme(CompanionKind kind, int niveau, boolean brutal) {
		Rythme rythme = this.rythmes.get(Math.clamp(niveau, 0, this.rythmes.size() - 1));
		if (!brutal) {
			return rythme;
		}
		String brutale = rythme.scène() + "_r";
		return CompanionScenes.ticks(kind, brutale) > 0
				? new Rythme(brutale, rythme.vitesse())
				: new Rythme(rythme.scène(), rythme.vitesse() * BRUTAL_VITESSE);
	}
}
