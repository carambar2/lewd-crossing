package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Ordre d'aller lire une note.
 *
 * @param pos    la feuille visée
 * @param face   son appui : 0 au sol, 1 au mur, 2 au plafond — l'ordre de
 *               {@code BlockFace}, qui décide de la façon dont la vue s'y pose
 * @param facing son orientation, qui donne le sens de lecture
 */
public record OpenNotePayload(BlockPos pos, int face, int facing) implements CustomPayload {
	public static final CustomPayload.Id<OpenNotePayload> ID =
			new CustomPayload.Id<>(JN1.id("open_note"));

	public static final PacketCodec<RegistryByteBuf, OpenNotePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenNotePayload::pos,
			PacketCodecs.VAR_INT, OpenNotePayload::face,
			PacketCodecs.VAR_INT, OpenNotePayload::facing,
			OpenNotePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
