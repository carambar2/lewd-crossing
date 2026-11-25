package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce que le joueur vient d'écrire sur une note.
 *
 * @param pos    la feuille
 * @param lignes son texte, dans l'ordre
 * @param verrou vrai si le joueur a choisi de la figer
 */
public record SaveNotePayload(BlockPos pos, List<String> lignes, boolean verrou)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SaveNotePayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("save_note"));

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

	public static final StreamCodec<RegistryFriendlyByteBuf, SaveNotePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, SaveNotePayload::pos,
			ByteBufCodecs.stringUtf8(LARGEUR_MAX).apply(ByteBufCodecs.list(LIGNES_MAX)),
			SaveNotePayload::lignes,
			ByteBufCodecs.BOOL, SaveNotePayload::verrou,
			SaveNotePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
