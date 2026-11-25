package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ordre d'aller lire une note.
 *
 * @param pos    la feuille visée
 * @param face   son appui : 0 au sol, 1 au mur, 2 au plafond — l'ordre de
 *               {@code BlockFace}, qui décide de la façon dont la vue s'y pose
 * @param facing son orientation, qui donne le sens de lecture
 */
public record OpenNotePayload(BlockPos pos, int face, int facing) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenNotePayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("open_note"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenNotePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenNotePayload::pos,
			ByteBufCodecs.VAR_INT, OpenNotePayload::face,
			ByteBufCodecs.VAR_INT, OpenNotePayload::facing,
			OpenNotePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
