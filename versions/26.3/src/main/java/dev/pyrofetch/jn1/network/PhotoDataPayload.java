package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.photo.PhotoImage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Une image de l'archive, envoyée à un client qui l'a demandée.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record PhotoDataPayload(int numéro, PhotoImage image) implements CustomPacketPayload {
	public static final Type<PhotoDataPayload> ID = new Type<>(JN1.id("photo_data"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PhotoDataPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, PhotoDataPayload::numéro, PhotoImage.PACKET_CODEC, PhotoDataPayload::image, PhotoDataPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
