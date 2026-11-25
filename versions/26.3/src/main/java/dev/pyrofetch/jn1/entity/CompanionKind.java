package dev.pyrofetch.jn1.entity;

import java.util.List;
import java.util.Map;

/**
 * Ce qui distingue une compagne d'une autre : son modèle, et ce qu'elle sait faire.
 *
 * <p>Les modèles ne partagent pas le même répertoire de mouvements : Ellie a un
 * vrai câlin là où Jenny doit emprunter un geste écrit pour elle. Plutôt que d'écrire trois fois la même
 * logique de comportement, chaque compagne déclare ici la correspondance entre
 * un rôle et l'animation qui le tient, et le rôle qu'elle ne sait pas tenir
 * reste vide : le contrôleur rend alors la main au lieu de réclamer une
 * animation absente, ce que GeckoLib refuserait.
 */
public record CompanionKind(String id, List<String> outfits, Map<Role, String> animations,
                            String sceneProp, List<String> scenePrefixes) {
	/** Les rôles que le comportement sait demander. */
	public enum Role {
		IDLE, WALK, FAST_WALK, RUN, WALK_BACK, SIT, FALL, SWIM, DOWNED, WAITING, HAPPY,
		WAVE, GREET, RECEIVE, SWIPE_A, SWIPE_B, SWIPE_C, DANCE, HUG
	}

	public static final CompanionKind JENNY = new CompanionKind("jenny",
			// Du plus habillé au moins habillé. jenny3 (sous-vêtements) et jenny4
			// (culotte) sont le modèle nu, peint par tools/gen_dessous.py.
			List.of("jenny", "jenny3", "jenny4", "jenny2"), Map.ofEntries(
			Map.entry(Role.IDLE, "idle"),
			Map.entry(Role.WALK, "walk"),
			Map.entry(Role.FAST_WALK, "fastwalk"),
			Map.entry(Role.RUN, "run"),
			Map.entry(Role.WALK_BACK, "backwards_walk"),
			Map.entry(Role.SIT, "sit"),
			Map.entry(Role.FALL, "fly"),
			Map.entry(Role.SWIM, "fly2"),
			Map.entry(Role.DOWNED, "downed"),
			Map.entry(Role.WAITING, "bowcharge"),
			Map.entry(Role.HAPPY, "fhappy"),
			Map.entry(Role.WAVE, "wave"),
			Map.entry(Role.GREET, "wave_idle"),
			Map.entry(Role.RECEIVE, "jn1_gift"),
			Map.entry(Role.SWIPE_A, "attack0"),
			Map.entry(Role.SWIPE_B, "attack1"),
			Map.entry(Role.SWIPE_C, "attack2"),
			Map.entry(Role.DANCE, "fortnite"),
			// « payment » servait de câlin faute de mieux : les bras y restent
			// immobiles quatre secondes avant qu'un seul se lève de côté, la
			// tête détournée. C'est une remise d'objet. Le geste est désormais
			// écrit par tools/gen_hug.py.
			Map.entry(Role.HUG, "jn1_hug")),
			// La pièce que la télécommande peut faire paraître pendant une scène :
			// un os du sous-arbre masqué « steve », dessiné avec sa propre planche.
			"co",
			// Les scènes que la télécommande propose : toute animation du
			// fichier dont le nom commence ainsi (voir CompanionScenes).
			List.of("b_", "d_", "p_"));

	/**
	 * Aura, l'IA holographique du laboratoire : un personnage original bâti sur
	 * le squelette de Jenny, dont elle reprend tout le répertoire — tenues,
	 * gestes, scènes. Voir {@link AuraEntity}.
	 */
	public static final CompanionKind AURA = new CompanionKind("aura",
			// Les tenues de Jenny, sous sa planche à elle (tools/gen_aura.py).
			List.of("aura", "aura3", "aura4", "aura2"), Map.ofEntries(
			Map.entry(Role.IDLE, "idle"),
			Map.entry(Role.WALK, "walk"),
			Map.entry(Role.FAST_WALK, "fastwalk"),
			Map.entry(Role.RUN, "run"),
			Map.entry(Role.WALK_BACK, "backwards_walk"),
			Map.entry(Role.SIT, "sit"),
			Map.entry(Role.FALL, "fly"),
			Map.entry(Role.SWIM, "fly2"),
			Map.entry(Role.DOWNED, "downed"),
			Map.entry(Role.WAITING, "bowcharge"),
			Map.entry(Role.HAPPY, "fhappy"),
			Map.entry(Role.WAVE, "wave"),
			Map.entry(Role.GREET, "wave_idle"),
			Map.entry(Role.RECEIVE, "jn1_gift"),
			Map.entry(Role.SWIPE_A, "attack0"),
			Map.entry(Role.SWIPE_B, "attack1"),
			Map.entry(Role.SWIPE_C, "attack2"),
			Map.entry(Role.DANCE, "fortnite"),
			Map.entry(Role.HUG, "jn1_hug")),
			"co",
			// Celles de Jenny, transposées avec le reste par tools/gen_aura.py.
			List.of("b_", "d_", "p_"));

	public static final CompanionKind ELLIE = new CompanionKind("ellie", List.of("ellie", "ellie2"), Map.ofEntries(
			Map.entry(Role.IDLE, "idle"),
			Map.entry(Role.WALK, "walk"),
			Map.entry(Role.FAST_WALK, "fastwalk"),
			Map.entry(Role.RUN, "run"),
			Map.entry(Role.WALK_BACK, "backwards_walk"),
			Map.entry(Role.SIT, "sit"),
			Map.entry(Role.FALL, "fly"),
			Map.entry(Role.SWIM, "fly2"),
			Map.entry(Role.DOWNED, "downed"),
			Map.entry(Role.WAITING, "bowcharge"),
			// « eyes » n'anime que le regard : c'est l'équivalent du sourire.
			Map.entry(Role.HAPPY, "fhappy"),
			Map.entry(Role.GREET, "hugselected"),
			Map.entry(Role.RECEIVE, "jn1_gift"),
			Map.entry(Role.SWIPE_A, "attack0"),
			Map.entry(Role.SWIPE_B, "attack1"),
			Map.entry(Role.SWIPE_C, "attack2"),
			Map.entry(Role.DANCE, "dash"),
			// Le câlin côte à côte de tools/gen_hug.py (0.56.5), comme les autres.
			Map.entry(Role.HUG, "jn1_hug")),
			// Son « co » s'appelait encore « cock » (renommé en 0.46.1) : mêmes
			// cubes, mêmes UV que celui de Jenny, d'où la même planche (ellie_co).
			"co",
			// Celles de Jenny, recopiées par tools/copy_scenes.py : même
			// squelette, et les poses tombent à un pixel près.
			List.of("b_", "d_", "p_"));

	/** Toutes les compagnes, dans l'ordre du catalogue. */
	public static List<CompanionKind> all() {
		return List.of(JENNY, ELLIE, AURA);
	}

	/** Vrai si elle sait faire un câlin : sans animation, le bouton n'a pas lieu d'être. */
	public boolean saitCâliner() {
		return this.animations.containsKey(Role.HUG);
	}

	/** La compagne de cet identifiant, ou {@code null}. */
	@org.jetbrains.annotations.Nullable
	public static CompanionKind byId(String id) {
		for (CompanionKind kind : all()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}

	/**
	 * Le nom complet de l'animation qui tient ce rôle, ou {@code null} si cette
	 * compagne ne sait pas le tenir.
	 */
	public String animation(Role role) {
		String court = this.animations.get(role);
		return court == null ? null : "animation." + this.id + "." + court;
	}

	/** Vrai si elle a plus d'une tenue : sans quoi le choix est absent, pas grisé. */
	public boolean hasOutfits() {
		return this.outfits.size() > 1;
	}

	/** Sa tenue par défaut, la première de la liste. */
	public String defaultOutfit() {
		return this.outfits.getFirst();
	}

	/**
	 * La tenue demandée si elle est à elle, sinon celle par défaut : un nom
	 * venu d'une vieille sauvegarde ou d'un client modifié ne doit pas désigner
	 * un fichier absent.
	 */
	public String outfit(String demandée) {
		return this.outfits.contains(demandée) ? demandée : this.defaultOutfit();
	}

	/**
	 * La tenue qu'une sauvegarde d'avant 0.40 désignait par « seconde
	 * apparence » : le modèle suffixé 2, s'il existe.
	 */
	public String legacyAlternate() {
		return this.outfit(this.id + "2");
	}

	/** Vrai si la télécommande a quelque chose à lui faire jouer. */
	public boolean hasScenes() {
		return !this.scenePrefixes.isEmpty();
	}

	/** Vrai si la télécommande peut faire paraître une pièce pendant ses scènes. */
	public boolean hasSceneProp() {
		return this.sceneProp != null;
	}

	/**
	 * La planche de cette pièce. Ses UV visent le coin d'une grille de 64 — une
	 * poignée de pixels de couleur — que la planche de la compagne laisse vide.
	 */
	public String scenePropTexturePath() {
		return "textures/entity/" + this.id + "_" + this.sceneProp + ".png";
	}

	/** Le nom complet d'une scène, donnée par son nom court ({@code b_su}). */
	public String sceneAnimation(String scene) {
		return "animation." + this.id + "." + scene;
	}

	public String modelPath() {
		return this.modelPath(this.defaultOutfit());
	}

	/**
	 * Le modèle à afficher.
	 *
	 * <p>Une tenue n'est qu'un fichier de géométrie : toutes partagent la planche
	 * et le fichier d'animations de la compagne, bâties sur le même squelette.
	 * Une tenue inconnue retombe sur celle par défaut plutôt que de pointer vers
	 * un fichier absent — GeckoLib laisserait alors le jeu sur l'écran de
	 * chargement, sans message.
	 */
	public String modelPath(String outfit) {
		return "geo/entity/" + this.outfit(outfit) + ".geo.json";
	}

	public String texturePath() {
		return "textures/entity/" + this.id + ".png";
	}

	public String animationPath() {
		return "animations/entity/" + this.id + ".animation.json";
	}
}
