package dev.pyrofetch.jn1.interaction;

import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.dialogue.DialogueTrigger;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionGesture;
import dev.pyrofetch.jn1.network.TalkPayload;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.sound.GirlSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Le bouton « Talk » de son écran (0.56.0) : chacune a sa personnalité
 * (Jenny, Ellie, FapGPT). Elle peut éconduire le joueur, jamais lui
 * refuser une scène ; pas de mariage ni d'enfants.
 *
 * <p>Six façons de lui parler ({@link Geste}). Chacune réussit plus ou moins
 * selon l'affection, son humeur, sa personnalité ({@link #affinité}) et la
 * lassitude : refaire la même chose coup sur coup marche de moins en moins
 * ({@link #LASSITUDE}). Réussi, son humeur monte et l'affection parfois ;
 * raté, elle le fait savoir et son humeur baisse — <strong>jamais
 * l'affection</strong> : c'est elle qui ouvre les Demandes, et une conversation
 * ratée ne doit pas les refermer. Ce qu'elle répond vient des dialogues
 * ({@code data/jn1/dialogue/<fille>.json}, déclencheur {@code talk}).
 *
 * <p>L'affection gagnée en parlant plafonne à {@link #AFFECTION_PAR_JOUR} par
 * jour de jeu, par fille : parler ne coûte rien, les cadeaux restent le gros
 * de la progression.
 */
public final class Conversation {
	/** Un point de lassitude retombe toutes les minutes. */
	private static final int LASSITUDE_RETOUR = 1200;
	/** Ce que coûte, en chances, chaque fois qu'on vient de refaire la même chose. */
	private static final float LASSITUDE = 0.12F;
	public static final int AFFECTION_PAR_JOUR = 3;
	/**
	 * Quatre interactions par seconde au plus : l'écran attend déjà la réponse
	 * avant de rouvrir ses boutons ; ce répit n'arrête qu'un client modifié. À
	 * une seconde, un clic juste après la réponse était ignoré sans un mot, et
	 * l'écran restait figé deux secondes.
	 */
	private static final int RÉPIT = 5;

	/** Les façons de lui parler. */
	public enum Geste {
		CHAT(0.80F, 0.010F, 1, 1, 0.20F),
		JOKE(0.50F, 0.015F, 2, 1, 0.30F),
		COMPLIMENT(0.55F, 0.015F, 2, 1, 0.30F),
		FLIRT(0.15F, 0.035F, 2, 2, 0.45F),
		TEASE(0.35F, 0.020F, 2, 2, 0.30F),
		KISS(-0.10F, 0.040F, 3, 3, 0.60F);

		/** Chance à affection nulle, et ce que chaque point d'affection y ajoute. */
		final float base;
		final float pente;
		/** Ce que gagne son humeur à la réussite, et ce qu'elle perd à l'échec. */
		final int gain;
		final int perte;
		/** Chance de gagner un point d'affection quand ça réussit. */
		final float affection;

		Geste(float base, float pente, int gain, int perte, float affection) {
			this.base = base;
			this.pente = pente;
			this.gain = gain;
			this.perte = perte;
			this.affection = affection;
		}

		public String id() {
			return this.name().toLowerCase(Locale.ROOT);
		}

		@Nullable
		public static Geste de(String id) {
			for (Geste g : values()) {
				if (g.id().equals(id)) {
					return g;
				}
			}
			return null;
		}
	}

	/**
	 * Sa personnalité, en chances ajoutées à chaque geste.
	 *
	 * <ul>
	 * <li><strong>Jenny</strong> : joyeuse, effrontée, en manque d'attention —
	 * on la flatte et on la drague facilement ; on la taquine moins bien, elle
	 * boude.</li>
	 * <li><strong>Ellie</strong> : gothique, sèche, dominante — elle respecte
	 * qu'on lui tienne tête et rit des blagues noires ; les compliments et la
	 * drague à l'eau de rose la font lever les yeux au ciel.</li>
	 * <li><strong>FapGPT</strong> : l'IA du laboratoire — elle adore qu'on lui
	 * parle (des données !) ; elle analyse les blagues au lieu d'en rire, et ne
	 * comprend pas toujours qu'on la taquine.</li>
	 * </ul>
	 */
	static float affinité(String fille, Geste geste) {
		return switch (fille) {
			case "jenny" -> switch (geste) {
				case FLIRT -> 0.15F;
				case COMPLIMENT -> 0.10F;
				case JOKE -> 0.05F;
				case TEASE -> -0.10F;
				case KISS -> 0.05F;
				default -> 0.0F;
			};
			case "ellie" -> switch (geste) {
				case TEASE -> 0.25F;
				case JOKE -> 0.10F;
				case CHAT -> -0.10F;
				case COMPLIMENT -> -0.15F;
				case FLIRT -> -0.10F;
				default -> 0.0F;
			};
			case "aura" -> switch (geste) {
				case CHAT -> 0.15F;
				case COMPLIMENT -> 0.05F;
				case JOKE -> -0.10F;
				case TEASE -> -0.15F;
				case KISS -> -0.05F;
				default -> 0.0F;
			};
			default -> 0.0F;
		};
	}

	/** Lassitude par fille puis par joueur : un compteur par geste, et le tick du dernier retour. */
	private static final Map<UUID, Map<UUID, Lassitude>> LASSITUDES = new HashMap<>();
	/** Affection gagnée en parlant aujourd'hui, par joueur puis par fille ({@code jour:points}). */
	private static final Map<UUID, Map<String, long[]>> GAGNÉ = new HashMap<>();
	private static final Map<UUID, Long> DERNIER = new HashMap<>();

	private static final class Lassitude {
		final int[] fois = new int[Geste.values().length];
		long vu;
	}

	private Conversation() {
	}

	/** Peut-on lui parler maintenant ? En scène ou en veille, non. */
	public static boolean disponible(CompanionEntity fille) {
		return !fille.hasScene() && !fille.estEnVeille();
	}

	/** Une interaction demandée depuis son écran ; le serveur en décide tout. */
	public static void parler(ServerPlayerEntity joueur, CompanionEntity fille, String id) {
		Geste geste = Geste.de(id);
		if (geste == null || !disponible(fille)) {
			return;
		}
		long maintenant = joueur.getServer().getTicks();
		if (maintenant < DERNIER.getOrDefault(joueur.getUuid(), 0L) + RÉPIT) {
			return;
		}
		DERNIER.put(joueur.getUuid(), maintenant);

		String qui = fille.kind().id();
		int affection = ModAttachments.get(joueur).trust(qui);
		Lassitude lassitude = lassitude(fille, joueur, maintenant);
		float chance = geste.base + geste.pente * affection + affinité(qui, geste)
				+ 0.03F * fille.getMood() - LASSITUDE * lassitude.fois[geste.ordinal()];
		chance = Math.clamp(chance, 0.05F, 0.97F);
		boolean réussi = fille.getRandom().nextFloat() < chance;
		lassitude.fois[geste.ordinal()]++;

		fille.getNavigation().stop();
		fille.getLookControl().lookAt(joueur);
		if (réussi) {
			fille.changerHumeur(geste.gain);
			if (fille.getRandom().nextFloat() < geste.affection && prendreAffection(joueur, qui, fille)) {
				DialogueEngine.grantTrust(joueur, qui, 1);
			}
			fille.setDelightedFor(40);
			fille.showHearts(geste == Geste.KISS ? 10 : geste == Geste.FLIRT ? 5 : 2);
			if (geste == Geste.KISS || geste == Geste.FLIRT) {
				GirlSounds.crier(fille, "ahh", 0.8F);
			}
		} else {
			fille.changerHumeur(-geste.perte);
			// Un baiser volé trop tôt : la gifle (le geste d'attaque, sans dégâts).
			if (geste == Geste.KISS && !qui.equals("aura")) {
				fille.playGesture(CompanionGesture.SWIPE_A);
			}
			GirlSounds.crier(fille, "huh", 0.8F);
		}

		Text réponse = DialogueEngine.parler(joueur, fille, DialogueTrigger.TALK, geste.id(),
				réussi ? "success" : "fail", null);
		if (réponse == null) {
			réponse = Text.translatable(réussi ? "talk.jn1.success" : "talk.jn1.fail", fille.getDisplayName());
		}
		ServerPlayNetworking.send(joueur, new TalkPayload(fille.getId(), geste.id(), réussi, réponse,
				ModAttachments.get(joueur).trust(qui)));
	}

	private static Lassitude lassitude(CompanionEntity fille, ServerPlayerEntity joueur, long maintenant) {
		Lassitude l = LASSITUDES.computeIfAbsent(fille.getUuid(), k -> new HashMap<>())
				.computeIfAbsent(joueur.getUuid(), k -> new Lassitude());
		if (l.vu == 0) {
			l.vu = maintenant;
		}
		long retours = (maintenant - l.vu) / LASSITUDE_RETOUR;
		if (retours > 0) {
			for (int i = 0; i < l.fois.length; i++) {
				l.fois[i] = (int) Math.max(0, l.fois[i] - retours);
			}
			l.vu += retours * LASSITUDE_RETOUR;
		}
		return l;
	}

	/** Le plafond du jour : vrai s'il reste de la place, et le point est compté. */
	private static boolean prendreAffection(ServerPlayerEntity joueur, String qui, CompanionEntity fille) {
		if (ModAttachments.get(joueur).trust(qui) >= PlayerProgress.MAX_TRUST) {
			return false;
		}
		long jour = fille.getWorld().getTimeOfDay() / 24000L;
		long[] compte = GAGNÉ.computeIfAbsent(joueur.getUuid(), k -> new HashMap<>())
				.computeIfAbsent(qui, k -> new long[] {jour, 0});
		if (compte[0] != jour) {
			compte[0] = jour;
			compte[1] = 0;
		}
		if (compte[1] >= AFFECTION_PAR_JOUR) {
			return false;
		}
		compte[1]++;
		return true;
	}

	/**
	 * Ce que lui font les événements du mod, selon qui elle est : un câlin, le
	 * fouet, les menottes, une tenue plus légère, une scène finie.
	 */
	public static void humeur(CompanionEntity fille, String événement) {
		String qui = fille.kind().id();
		int delta = switch (événement) {
			case "hug" -> qui.equals("ellie") ? 1 : 2;
			case "whip" -> qui.equals("ellie") ? 0 : 1;
			case "cuffed" -> qui.equals("ellie") ? -1 : 1;
			case "undress" -> qui.equals("jenny") ? 2 : 1;
			case "after" -> 3;
			case "demande" -> 1;
			case "left" -> qui.equals("ellie") ? -2 : -1;
			case "cum_poured" -> qui.equals("aura") ? 0 : 1;
			default -> 0;
		};
		fille.changerHumeur(delta);
	}

	/** Un événement : son humeur bouge, et elle le dit si elle a de quoi. */
	public static void événement(@Nullable ServerPlayerEntity joueur, CompanionEntity fille, String événement) {
		humeur(fille, événement);
		if (joueur != null) {
			DialogueEngine.réagir(joueur, fille, événement);
		}
	}

	public static void clear() {
		LASSITUDES.clear();
		GAGNÉ.clear();
		DERNIER.clear();
	}
}
