package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Une tranche d'une photo qui monte vers le serveur : une image entière ne
 * tiendrait pas dans un seul paquet.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record PhotoUploadPayload(int session, PhotoFormat format, int décalage, byte[] tranche, boolean dernière,
		dev.pyrofetch.jn1.photo.PhotoPose pose) implements CustomPacketPayload {
	public static final int TRANCHE = 24000;
	public static final Type<PhotoUploadPayload> ID = new Type<>(JN1.id("photo_upload"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PhotoUploadPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		PhotoUploadPayload::session,
		PhotoFormat.PACKET_CODEC,
		PhotoUploadPayload::format,
		ByteBufCodecs.VAR_INT,
		PhotoUploadPayload::décalage,
		ByteBufCodecs.BYTE_ARRAY,
		PhotoUploadPayload::tranche,
		ByteBufCodecs.BOOL,
		PhotoUploadPayload::dernière,
		dev.pyrofetch.jn1.photo.PhotoPose.PACKET_CODEC,
		PhotoUploadPayload::pose,
		PhotoUploadPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
