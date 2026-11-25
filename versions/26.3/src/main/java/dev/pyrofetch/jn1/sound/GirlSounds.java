package dev.pyrofetch.jn1.sound;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionKind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * Les sons des filles : deux voix — celles du mod d'origine, Jenny et Ellie —
 * et des bruitages (`tools/import_sounds.py`, depuis `fichiers_en_plus/`).
 *
 * <p>Chaque fille parle d'une des deux voix ({@link #voixDe}). Pendant les
 * scènes, ce sont les animations elles-mêmes qui portent les sons, en images
 * clés (`sound_effects`, écrites par `tools/gen_scene_sounds.py`) : GeckoLib
 * les joue côté client au bon instant ({@link #jouer}). Une image clé nomme un
 * ou plusieurs effets séparés par {@code |} : {@code voice:moan} (la voix de la
 * fille), {@code misc:pounding} (un bruitage), {@code bell} (la clochette de
 * son collier, chez celles qui en portent un).
 *
 * <p>Les listes ci-dessous doivent suivre `sounds.json` : `validate_assets.py`
 * le vérifie.
 */
public final class GirlSounds {
	public static final List<String> VOIX = List.of("jenny", "ellie");
	public static final List<String> CRIS = List.of("aftersessionmoan", "ahh", "bjmoan", "giggle", "happyoh",
			"heavybreathing", "hmph", "huh", "lightbreathing", "lipsound", "mmm", "moan", "sadoh", "sigh",
			"cometomommy", "goodboy", "mommyhorny");
	/** Les cris qu'Ellie seule enregistre. */
	public static final Set<String> ELLIE_SEULE = Set.of("cometomommy", "goodboy", "mommyhorny");
	public static final List<String> BRUITS = List.of("bedrustle", "belljingle", "cuminflation", "eat", "fart",
			"inserts", "jump", "plob", "pounding", "scream", "slap", "slide", "smallinserts", "touch");

	private static final Map<String, SoundEvent> ÉVÉNEMENTS = new HashMap<>();

	private GirlSounds() {
	}

	public static void register() {
		for (String voix : VOIX) {
			for (String cri : CRIS) {
				if (voix.equals("ellie") || !ELLIE_SEULE.contains(cri)) {
					enregistrer("voice." + voix + "." + cri);
				}
			}
		}
		for (String bruit : BRUITS) {
			enregistrer("misc." + bruit);
		}
	}

	private static void enregistrer(String nom) {
		Identifier id = JN1.id(nom);
		ÉVÉNEMENTS.put(nom, Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
	}

	/**
	 * La voix d'une fille. Jenny et Ellie ont la leur ; les autres empruntent :
	 * la voix d'Ellie, plus grave, va à Gardevoir ; celle de Jenny à FapGPT.
	 */
	public static String voixDe(CompanionKind kind) {
		return switch (kind.id()) {
			case "ellie" -> "ellie";
			default -> "jenny";
		};
	}

	/** Le cri de cette fille, ou {@code null} si sa voix ne l'a pas. */
	@Nullable
	public static SoundEvent voix(CompanionEntity fille, String cri) {
		return ÉVÉNEMENTS.get("voice." + voixDe(fille.kind()) + "." + cri);
	}

	@Nullable
	public static SoundEvent bruit(String nom) {
		return ÉVÉNEMENTS.get("misc." + nom);
	}

	/** La clochette du collier : Jenny et FapGPT. Les mob girls n'ont pas son collier. */
	public static boolean porteClochette(CompanionKind kind) {
		return kind == CompanionKind.JENNY || kind == CompanionKind.AURA;
	}

	/**
	 * Une image clé sonore, côté client. Les cris un peu plus forts que les
	 * bruitages, chacun avec un léger écart de hauteur pour que la répétition
	 * ne s'entende pas.
	 */
	public static void jouer(CompanionEntity fille, String effets) {
		for (String effet : effets.split("\\|")) {
			effet = effet.trim();
			SoundEvent son;
			float volume;
			if (effet.startsWith("voice:")) {
				son = voix(fille, alias(fille, effet.substring(6)));
				volume = 0.9F;
			} else if (effet.startsWith("misc:")) {
				son = bruit(effet.substring(5));
				volume = 0.65F;
			} else if (effet.equals("bell") && porteClochette(fille.kind())) {
				son = bruit("belljingle");
				volume = 0.45F;
			} else {
				continue;
			}
			if (son == null) {
				continue;
			}
			float hauteur = 0.95F + fille.getRandom().nextFloat() * 0.1F;
			fille.level().playLocalSound(fille.getX(), fille.getY() + 1.0, fille.getZ(), son, SoundSource.NEUTRAL,
					volume, hauteur, false);
		}
	}

	/**
	 * Les repères des animations reprises du mod d'origine (repos, attaques,
	 * perle) : son code les lisait pour agir au bon moment. Ceux qui ont un sens
	 * ici deviennent un bruit ; les autres (paiement, dash, messages) se taisent.
	 * Les effets propres au mod ({@code voice:}, {@code misc:}, {@code bell})
	 * passent par {@link #jouer}.
	 */
	public static void repère(CompanionEntity fille, String effets) {
		for (String effet : effets.split("\\|")) {
			effet = effet.trim();
			SoundEvent son = null;
			float volume = 0.6F;
			switch (effet) {
				case "attackSound" -> son = net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP;
				case "pearl" -> son = net.minecraft.sounds.SoundEvents.ENDER_PEARL_THROW;
				// Au repos, la tête bouge : la clochette tinte une fois sur quatre.
				case "idle" -> {
					if (porteClochette(fille.kind()) && fille.getRandom().nextInt(4) == 0) {
						son = bruit("belljingle");
						volume = 0.3F;
					}
				}
				default -> {
					if (effet.startsWith("voice:") || effet.startsWith("misc:") || effet.equals("bell")) {
						jouer(fille, effet);
					}
				}
			}
			if (son != null) {
				float hauteur = 0.95F + fille.getRandom().nextFloat() * 0.1F;
				fille.level().playLocalSound(fille.getX(), fille.getY() + 1.0, fille.getZ(), son,
						SoundSource.NEUTRAL, volume, hauteur, false);
			}
		}
	}

	/**
	 * Deux cris qui dépendent de la voix : {@code tease}, l'invite du début (un
	 * rire chez Jenny ; chez Ellie, « come to mommy », « mommy's horny » ou un
	 * rire), et {@code after}, l'après (un gémissement ; chez Ellie, parfois
	 * « good boy »).
	 */
	private static String alias(CompanionEntity fille, String cri) {
		boolean ellie = voixDe(fille.kind()).equals("ellie");
		return switch (cri) {
			case "tease" -> ellie ? new String[] {"cometomommy", "mommyhorny", "giggle"}[fille.getRandom().nextInt(3)] : "giggle";
			case "after" -> ellie && fille.getRandom().nextBoolean() ? "goodboy" : "aftersessionmoan";
			default -> cri;
		};
	}

	/** Un cri joué par le serveur, entendu de tous (gestes, coups, humeur). */
	public static void crier(CompanionEntity fille, String cri, float volume) {
		SoundEvent son = voix(fille, cri);
		if (son != null && !fille.level().isClientSide()) {
			fille.level().playSound(null, fille.getX(), fille.getY() + 1.0, fille.getZ(), son,
					SoundSource.NEUTRAL, volume, 0.95F + fille.getRandom().nextFloat() * 0.1F);
		}
	}
}
