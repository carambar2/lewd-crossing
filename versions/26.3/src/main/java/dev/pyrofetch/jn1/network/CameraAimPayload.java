package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le réglage d'un trépied, envoyé en quittant le viseur.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record CameraAimPayload(BlockPos pied, float lacet, float tangage, int retard, PhotoFormat format) implements CustomPacketPayload {
	public static final Type<CameraAimPayload> ID = new Type<>(JN1.id("camera_aim"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CameraAimPayload> CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC,
		CameraAimPayload::pied,
		ByteBufCodecs.FLOAT,
		CameraAimPayload::lacet,
		ByteBufCodecs.FLOAT,
		CameraAimPayload::tangage,
		ByteBufCodecs.VAR_INT,
		CameraAimPayload::retard,
		PhotoFormat.PACKET_CODEC,
		CameraAimPayload::format,
		CameraAimPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
