package dev.pyrofetch.jn1.client.agegate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pyrofetch.jn1.JN1;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * La question d'âge, posée au premier lancement avec le mod.
 *
 * <p>Le mod contient des scènes à caractère sexuel : avant d'aller plus loin,
 * le joueur déclare avoir 18 ans ou plus, ou le jeu se ferme. C'est une
 * déclaration, pas une vérification — rien ne peut en être une ici.
 *
 * <p>Pour l'instant, la question revient à chaque lancement du jeu
 * ({@link #À_CHAQUE_LANCEMENT}). Sinon, un « oui » vaut pour l'installation,
 * gardé dans {@code config/jn1-client.json}, à part de {@code jn1.json} qui sert
 * aussi au serveur. Un refus n'est jamais enregistré.
 *
 * <p>L'écran prend la place de l'écran titre tant qu'on n'a pas répondu. Un
 * lancement direct dans un monde saute l'écran titre : on le montre alors à
 * l'arrivée dans le monde, avec la même issue.
 */
public final class AgeGate {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FICHIER = FabricLoader.getInstance().getConfigDir().resolve("jn1-client.json");
	private static final String CLÉ = "ageConfirmed";
	/**
	 * Pour l'instant, la question revient à
	 * chaque lancement du jeu : la réponse ne vaut que pour la session, rien
	 * n'est lu ni écrit. Repasser à {@code false} pour qu'un « oui » soit
	 * retenu dans {@code config/jn1-client.json}.
	 */
	private static final boolean À_CHAQUE_LANCEMENT = true;

	private static Boolean confirmé;

	private AgeGate() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, écran, largeur, hauteur) -> {
			if (écran instanceof TitleScreen titre && !confirmed()) {
				// Pas pendant l'initialisation de l'écran titre : au tour suivant.
				client.execute(() -> client.gui.setScreen(new AgeGateScreen(titre)));
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!confirmed()) {
				client.execute(() -> client.gui.setScreen(new AgeGateScreen(null)));
			}
		});
	}

	public static boolean confirmed() {
		if (confirmé == null) {
			confirmé = lire();
		}
		return confirmé;
	}

	static void confirm() {
		confirmé = true;
		if (À_CHAQUE_LANCEMENT) {
			return;
		}
		JsonObject racine = new JsonObject();
		racine.addProperty(CLÉ, true);
		try (Writer écrivain = Files.newBufferedWriter(FICHIER)) {
			GSON.toJson(racine, écrivain);
		} catch (IOException exception) {
			// La réponse vaut au moins pour cette session.
			JN1.LOGGER.warn("Impossible d'écrire {}", FICHIER, exception);
		}
	}

	/** Refus : le jeu se ferme, proprement. */
	static void refuse() {
		Minecraft.getInstance().stop();
	}

	private static boolean lire() {
		if (À_CHAQUE_LANCEMENT || !Files.exists(FICHIER)) {
			return false;
		}
		try (Reader lecteur = Files.newBufferedReader(FICHIER)) {
			JsonObject racine = JsonParser.parseReader(lecteur).getAsJsonObject();
			return racine.has(CLÉ) && racine.get(CLÉ).getAsBoolean();
		} catch (Exception exception) {
			JN1.LOGGER.warn("Illisible : {} — la question sera reposée", FICHIER, exception);
			return false;
		}
	}
}
