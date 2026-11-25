package dev.pyrofetch.jn1.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pyrofetch.jn1.JN1;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Les scènes qu'une compagne sait jouer à la télécommande : toutes les
 * animations de son fichier dont le nom court commence par l'un de ses
 * préfixes ({@link CompanionKind#scenePrefixes()}).
 *
 * <p>La liste est lue dans le fichier plutôt qu'écrite ici : les animations
 * se refont à part, et une nouvelle {@code d_…} doit paraître à
 * la télécommande sans toucher au code. Le serveur la lit dans le jar du mod —
 * les ressources du client y sont aussi — et c'est elle qu'il envoie à l'écran
 * et qui borne ce qu'il accepte : réclamer à GeckoLib une animation absente du
 * fichier ne jouerait rien.
 *
 * <p>L'ordre est celui d'une scène, préfixe par préfixe : ce qui l'ouvre, puis
 * de plus en plus vite, puis ce qui la clôt. Un suffixe inconnu passe après,
 * dans l'ordre alphabétique.
 */
public final class CompanionScenes {
	/** Du début à la fin d'une scène. */
	private static final List<String> STAGES = List.of("in", "go", "wa", "st", "su", "sl", "th", "fa", "cu");

	private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, Bounds>> BOÎTES = new ConcurrentHashMap<>();

	/**
	 * La place qu'occupe le corps pendant une scène, mesurée hors du jeu par
	 * {@code tools/gen_scene_bounds.py} : en pixels, repère des fichiers du
	 * modèle (l'avant vers les z négatifs, x non inversé).
	 */
	public record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
	}

	/** La boîte de cette scène, ou {@code null} si elle n'a pas été mesurée. */
	@org.jetbrains.annotations.Nullable
	public static Bounds bounds(CompanionKind kind, String scene) {
		return BOÎTES.computeIfAbsent(kind.id(), id -> loadBounds(kind)).get(scene);
	}

	private static Map<String, Bounds> loadBounds(CompanionKind kind) {
		Optional<Path> chemin = FabricLoader.getInstance().getModContainer(JN1.MOD_ID)
				.flatMap(mod -> mod.findPath("assets/" + JN1.MOD_ID + "/scene_bounds/" + kind.id() + ".json"));
		if (chemin.isEmpty()) {
			return Map.of();
		}
		Map<String, Bounds> boîtes = new java.util.HashMap<>();
		try (Reader lecteur = Files.newBufferedReader(chemin.get())) {
			JsonObject racine = JsonParser.parseReader(lecteur).getAsJsonObject();
			for (String scène : racine.keySet()) {
				var v = racine.getAsJsonArray(scène);
				boîtes.put(scène, new Bounds(v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat(),
						v.get(3).getAsFloat(), v.get(4).getAsFloat(), v.get(5).getAsFloat()));
			}
		} catch (IOException | RuntimeException exception) {
			JN1.LOGGER.warn("Scènes : boîtes illisibles pour {}", kind.id(), exception);
		}
		return Map.copyOf(boîtes);
	}

	private CompanionScenes() {
	}

	/** Les scènes de cette compagne, dans l'ordre où les proposer. */
	public static List<String> of(CompanionKind kind) {
		if (!kind.hasScenes()) {
			return List.of();
		}
		return CACHE.computeIfAbsent(kind.id(), id -> load(kind));
	}

	/** Vrai si cette scène figure bien dans le fichier de la compagne. */
	private static final Map<String, Map<String, Integer>> DURÉES = new ConcurrentHashMap<>();

	/**
	 * La durée d'une boucle de cette scène, en ticks ; zéro si elle est inconnue.
	 * C'est ce qui rythme les traces des scènes « cu » ({@code Cum}).
	 */
	public static int ticks(CompanionKind kind, String scene) {
		return DURÉES.computeIfAbsent(kind.id(), id -> loadDurations(kind)).getOrDefault(scene, 0);
	}

	/**
	 * Le fichier d'animations dans le jar du mod. 26.3 : GeckoLib 5 les lit sous
	 * {@code geckolib/animations/}, où le build les range
	 * ({@code animations/entity/jenny.animation.json} →
	 * {@code geckolib/animations/entity/jenny.animation.json}) ; l'ancien
	 * chemin reste essayé, au cas où.
	 */
	private static Optional<Path> fichierAnimations(CompanionKind kind) {
		String chemin = kind.animationPath();
		String rangé = chemin.startsWith("animations/") ? "geckolib/" + chemin : chemin;
		return FabricLoader.getInstance().getModContainer(JN1.MOD_ID)
				.flatMap(mod -> mod.findPath("assets/" + JN1.MOD_ID + "/" + rangé)
						.or(() -> mod.findPath("assets/" + JN1.MOD_ID + "/" + chemin)));
	}

	private static Map<String, Integer> loadDurations(CompanionKind kind) {
		Optional<Path> chemin = fichierAnimations(kind);
		if (chemin.isEmpty()) {
			return Map.of();
		}
		Map<String, Integer> durées = new java.util.HashMap<>();
		try (Reader lecteur = Files.newBufferedReader(chemin.get())) {
			JsonObject animations = JsonParser.parseReader(lecteur).getAsJsonObject().getAsJsonObject("animations");
			String préfixe = "animation." + kind.id() + ".";
			for (String nom : animations.keySet()) {
				JsonObject animation = animations.getAsJsonObject(nom);
				if (nom.startsWith(préfixe) && animation.has("animation_length")) {
					durées.put(nom.substring(préfixe.length()),
							Math.round(animation.get("animation_length").getAsFloat() * 20.0F));
				}
			}
		} catch (IOException | RuntimeException exception) {
			JN1.LOGGER.warn("Scènes : durées illisibles pour {}", kind.id(), exception);
		}
		return Map.copyOf(durées);
	}

	/** Suffixe d'une animation jouée une fois avant la scène de ce nom ({@code p_sp_intro}). */
	public static final String INTRO = "_intro";

	/**
	 * Vrai si la scène a une introduction : jouée une fois, elle amène le corps
	 * à la première image de la boucle — sans elle, le contrôleur n'a que ses
	 * quatre ticks de fondu pour passer de la posture à la scène, et une scène
	 * qui commence de dos ferait faire un demi-tour en un éclair.
	 */
	public static boolean hasIntro(CompanionKind kind, String scene) {
		return ticks(kind, scene + INTRO) > 0;
	}

	/**
	 * Les scènes qu'on choisit, à la télécommande ou au menu : toutes sauf les
	 * fins « cu », qui viennent d'elles-mêmes quand la barre d'excitation est
	 * pleine ({@link #fin}).
	 */
	public static List<String> choisissables(CompanionKind kind) {
		return of(kind).stream().filter(scène -> !scène.endsWith("cu")).toList();
	}

	/**
	 * La fin « cu » qui suit cette scène : la sienne propre d'abord
	 * ({@code d_hj} → {@code d_hjcu}), sinon celle de son préfixe
	 * ({@code d_st} → {@code d_cu}) ; vide si elle n'en a pas.
	 */
	public static String fin(CompanionKind kind, String scene) {
		if (knows(kind, scene + "cu")) {
			return scene + "cu";
		}
		int trait = scene.indexOf('_');
		String fin = (trait < 0 ? "" : scene.substring(0, trait + 1)) + "cu";
		return knows(kind, fin) ? fin : "";
	}

	public static boolean knows(CompanionKind kind, String scene) {
		return of(kind).contains(scene);
	}

	private static List<String> load(CompanionKind kind) {
		Optional<Path> chemin = fichierAnimations(kind);
		if (chemin.isEmpty()) {
			JN1.LOGGER.warn("Scènes : fichier d'animations introuvable pour {}", kind.id());
			return List.of();
		}
		JsonObject animations;
		try (Reader lecteur = Files.newBufferedReader(chemin.get())) {
			animations = JsonParser.parseReader(lecteur).getAsJsonObject().getAsJsonObject("animations");
		} catch (IOException | RuntimeException exception) {
			JN1.LOGGER.warn("Scènes : lecture impossible pour {}", kind.id(), exception);
			return List.of();
		}

		String préfixe = "animation." + kind.id() + ".";
		List<String> scènes = new ArrayList<>();
		for (String nom : animations.keySet()) {
			if (!nom.startsWith(préfixe)) {
				continue;
			}
			String court = nom.substring(préfixe.length());
			// Une introduction n'est pas une scène à part : elle se joue d'elle-même
			// avant celle dont elle porte le nom (voir intro).
			if (court.endsWith(INTRO)) {
				continue;
			}
			if (kind.scenePrefixes().stream().anyMatch(court::startsWith)) {
				scènes.add(court);
			}
		}
		scènes.sort(Comparator
				.comparingInt((String scène) -> rangDuPréfixe(kind, scène))
				.thenComparingInt(CompanionScenes::rangDuSuffixe)
				.thenComparing(Comparator.naturalOrder()));
		JN1.LOGGER.debug("Scènes de {} : {}", kind.id(), scènes);
		return List.copyOf(scènes);
	}

	private static int rangDuPréfixe(CompanionKind kind, String scène) {
		for (int i = 0; i < kind.scenePrefixes().size(); i++) {
			if (scène.startsWith(kind.scenePrefixes().get(i))) {
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}

	private static int rangDuSuffixe(String scène) {
		int rang = STAGES.indexOf(scène.substring(scène.indexOf('_') + 1));
		return rang < 0 ? STAGES.size() : rang;
	}
}
