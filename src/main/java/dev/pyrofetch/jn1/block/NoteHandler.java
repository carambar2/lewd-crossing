package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.network.SaveNotePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Reçoit ce que le joueur a écrit sur une note ou un écran mural.
 *
 * <p>Rien n'est cru sur parole : la position doit désigner une note, elle doit
 * être à portée, et une note déjà verrouillée ne se réécrit plus. Ces trois
 * contrôles ne coûtent rien et ferment la porte à un client bricolé qui
 * réécrirait à distance les notes des autres.
 */
public final class NoteHandler {
	/** Au-delà, on n'a pas pu ouvrir la note qu'on prétend modifier. */
	private static final double PORTÉE_CARRÉE = 8.0 * 8.0;
	/**
	 * Plus large pour un écran : un grand écran mural garde son texte dans son
	 * coin supérieur gauche, qui peut être loin du bloc qu'on a visé.
	 */
	private static final double PORTÉE_ÉCRAN_CARRÉE = 24.0 * 24.0;

	private NoteHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(SaveNotePayload.ID,
				(payload, context) -> context.player().server.execute(
						() -> appliquer(context.player(), payload)));
	}

	private static void appliquer(ServerPlayerEntity joueur, SaveNotePayload payload) {
		BlockPos pos = payload.pos();
		// La plus large des deux portées d'abord : lire l'entité de bloc d'une
		// position lointaine chargerait son tronçon, à la demande du client.
		double distance = joueur.squaredDistanceTo(pos.toCenterPos());
		if (distance > PORTÉE_ÉCRAN_CARRÉE) {
			return;
		}
		BlockEntity entité = joueur.getWorld().getBlockEntity(pos);
		if (!(entité instanceof WallScreenBlockEntity) && distance > PORTÉE_CARRÉE) {
			return;
		}
		if (!(entité instanceof TextSupport support) || support.verrouillée()) {
			return;
		}
		support.setLignes(payload.lignes(), payload.verrou());
	}
}
