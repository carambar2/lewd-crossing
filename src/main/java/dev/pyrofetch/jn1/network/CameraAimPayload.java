package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.math.BlockPos;

/**
 * Le réglage d'un trépied, envoyé en quittant le viseur.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record CameraAimPayload(BlockPos pied, float lacet, float tangage, int retard, PhotoFormat format) implements CustomPayload {
	public static final Id<CameraAimPayload> ID = new Id<>(JN1.id("camera_aim"));
	public static final PacketCodec<RegistryByteBuf, CameraAimPayload> CODEC = PacketCodec.tuple(
		BlockPos.PACKET_CODEC,
		CameraAimPayload::pied,
		PacketCodecs.FLOAT,
		CameraAimPayload::lacet,
		PacketCodecs.FLOAT,
		CameraAimPayload::tangage,
		PacketCodecs.VAR_INT,
		CameraAimPayload::retard,
		PhotoFormat.PACKET_CODEC,
		CameraAimPayload::format,
		CameraAimPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
