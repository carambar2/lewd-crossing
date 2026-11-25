package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;

/**
 * Une tranche d'une photo qui monte vers le serveur : une image entière ne
 * tiendrait pas dans un seul paquet.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record PhotoUploadPayload(int session, PhotoFormat format, int décalage, byte[] tranche, boolean dernière,
		dev.pyrofetch.jn1.photo.PhotoPose pose) implements CustomPayload {
	public static final int TRANCHE = 24000;
	public static final Id<PhotoUploadPayload> ID = new Id<>(JN1.id("photo_upload"));
	public static final PacketCodec<RegistryByteBuf, PhotoUploadPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT,
		PhotoUploadPayload::session,
		PhotoFormat.PACKET_CODEC,
		PhotoUploadPayload::format,
		PacketCodecs.VAR_INT,
		PhotoUploadPayload::décalage,
		PacketCodecs.BYTE_ARRAY,
		PhotoUploadPayload::tranche,
		PacketCodecs.BOOL,
		PhotoUploadPayload::dernière,
		dev.pyrofetch.jn1.photo.PhotoPose.PACKET_CODEC,
		PhotoUploadPayload::pose,
		PhotoUploadPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
