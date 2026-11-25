package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Ce que le joueur vient d'écrire sur une note.
 *
 * @param pos    la feuille
 * @param lignes son texte, dans l'ordre
 * @param verrou vrai si le joueur a choisi de la figer
 */
public record SaveNotePayload(BlockPos pos, List<String> lignes, boolean verrou)
		implements CustomPayload {
	public static final CustomPayload.Id<SaveNotePayload> ID =
			new CustomPayload.Id<>(JN1.id("save_note"));

	/**
	 * Les bornes sont là pour le serveur, pas pour le client.
	 *
	 * <p>Rien n'oblige un paquet à venir de notre écran : sans limite de
	 * longueur ni de nombre, un client bricolé pourrait écrire un roman dans
	 * chaque feuille et le faire porter par la sauvegarde du monde.
	 */
	public static final int LIGNES_MAX = 32;
	/** En caractères : un grand écran mural tient des lignes bien plus longues qu'une feuille. */
	public static final int LARGEUR_MAX = 256;

	public static final PacketCodec<RegistryByteBuf, SaveNotePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, SaveNotePayload::pos,
			PacketCodecs.string(LARGEUR_MAX).collect(PacketCodecs.toList(LIGNES_MAX)),
			SaveNotePayload::lignes,
			PacketCodecs.BOOL, SaveNotePayload::verrou,
			SaveNotePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
