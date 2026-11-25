package dev.pyrofetch.jn1.client.onlyfaps;

import dev.pyrofetch.jn1.network.OnlyFapsEtatPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Ce que le serveur a dit d'OnlyFaps en dernier : OnlyFaps.exe le dessine.
 * Le client ne calcule rien — prix et abonnés viennent du serveur.
 */
public final class OnlyFapsClient {
	private static OnlyFapsEtatPayload état;
	/** Le dernier commentaire et son gain restent affichés jusqu'à la publication suivante. */
	private static String commentaire = "";
	private static int gain;

	private OnlyFapsClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(OnlyFapsEtatPayload.ID, (payload, context) -> {
			état = payload;
			if (!payload.commentaire().isEmpty()) {
				commentaire = payload.commentaire();
				gain = payload.gain();
			}
		});
	}

	/** Null tant que le serveur n'a pas répondu. */
	public static OnlyFapsEtatPayload état() {
		return état;
	}

	public static String commentaire() {
		return commentaire;
	}

	public static int gain() {
		return gain;
	}

	/** À l'ouverture de la fenêtre : l'ancien commentaire n'a plus lieu d'être. */
	public static void oublier() {
		état = null;
		commentaire = "";
		gain = 0;
	}
}
