package dev.pyrofetch.jn1.excitation;

import dev.pyrofetch.jn1.network.ExcitationPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * L'excitation du joueur, de 0 à {@link #MAX}.
 *
 * <p>Elle monte pendant une scène qu'il a lancée (plus vite s'il y participe,
 * et d'autant plus que le rythme est rapide — {@code CompanionEntity}), et
 * retombe lentement quand plus rien ne l'entretient. Pleine, la scène passe à
 * sa fin « cu » et elle retombe à zéro.
 *
 * <p>Elle ne vit que côté serveur, en mémoire : ni sauvegardée ni gardée à la
 * reconnexion — c'est un état du moment. Le client n'en reçoit que l'arrondi,
 * pour sa barre, et seulement quand il change.
 */
public final class Excitation {
	public static final float MAX = 100.0F;
	/** Un plein en une minute, au rythme de base, en participant. */
	public static final float PAR_TICK = MAX / 1200.0F;
	/** Retombe de tout en une minute et demie. */
	private static final float DÉCRUE = MAX / 1800.0F;
	/** Ce qui l'a nourrie il y a moins de deux secondes la retient. */
	private static final int RÉPIT = 40;

	private static final Map<UUID, Float> NIVEAUX = new HashMap<>();
	private static final Map<UUID, Integer> ENVOYÉS = new HashMap<>();
	private static final Map<UUID, Long> ÉLANS = new HashMap<>();

	private Excitation() {
	}

	public static void register() {
		// Le partenaire qui se déconnecte en scène descend d'abord : le jeu
		// emporte sinon sa monture — la fille — dans le fichier du joueur, et
		// elle disparaît du monde jusqu'à son retour.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer joueur = handler.getPlayer();
			if (joueur.getVehicle() instanceof dev.pyrofetch.jn1.entity.CompanionEntity fille) {
				// La scène s'arrête avec lui (0.56.5), sans un mot : il n'est plus là.
				if (fille.isScenePartner(joueur)) {
					fille.stopScene();
				}
				joueur.stopRiding();
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long maintenant = server.getTickCount();
			for (ServerPlayer joueur : server.getPlayerList().getPlayers()) {
				UUID id = joueur.getUUID();
				if (maintenant - ÉLANS.getOrDefault(id, Long.MIN_VALUE / 2) > RÉPIT) {
					float niveau = get(joueur);
					if (niveau > 0) {
						NIVEAUX.put(id, Math.max(0, niveau - DÉCRUE));
					}
				}
				synchroniser(joueur);
			}
		});
	}

	public static float get(ServerPlayer joueur) {
		return NIVEAUX.getOrDefault(joueur.getUUID(), 0.0F);
	}

	public static void set(ServerPlayer joueur, float niveau) {
		NIVEAUX.put(joueur.getUUID(), Math.clamp(niveau, 0.0F, MAX));
	}

	/** Ajoute, retient la décrue un moment ; rend vrai si c'est plein. */
	public static boolean ajouter(ServerPlayer joueur, float quantité) {
		ÉLANS.put(joueur.getUUID(), (long) joueur.level().getServer().getTickCount());
		set(joueur, get(joueur) + quantité);
		return get(joueur) >= MAX;
	}

	public static boolean pleine(ServerPlayer joueur) {
		return get(joueur) >= MAX;
	}

	private static void synchroniser(ServerPlayer joueur) {
		int arrondi = Math.round(get(joueur));
		Integer envoyé = ENVOYÉS.get(joueur.getUUID());
		if (envoyé == null || envoyé != arrondi) {
			ENVOYÉS.put(joueur.getUUID(), arrondi);
			ServerPlayNetworking.send(joueur, new ExcitationPayload(arrondi));
		}
	}

	/** Il jouit : l'écran blanchit chez lui ({@code OrgasmeEcran}). */
	public static void orgasme(ServerPlayer joueur) {
		ServerPlayNetworking.send(joueur, dev.pyrofetch.jn1.network.OrgasmePayload.INSTANCE);
	}

	/** À la déconnexion : rien ne reste. */
	public static void oublier(UUID joueur) {
		NIVEAUX.remove(joueur);
		ENVOYÉS.remove(joueur);
		ÉLANS.remove(joueur);
	}
}
