package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.photo.PhotoImage;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;

/**
 * Une image de l'archive, envoyée à un client qui l'a demandée.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record PhotoDataPayload(int numéro, PhotoImage image) implements CustomPayload {
	public static final Id<PhotoDataPayload> ID = new Id<>(JN1.id("photo_data"));
	public static final PacketCodec<RegistryByteBuf, PhotoDataPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT, PhotoDataPayload::numéro, PhotoImage.PACKET_CODEC, PhotoDataPayload::image, PhotoDataPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
