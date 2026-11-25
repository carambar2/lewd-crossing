package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Demande d'une image de l'archive, par son numéro.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record PhotoRequestPayload(int numéro) implements CustomPacketPayload {
	public static final Type<PhotoRequestPayload> ID = new Type<>(JN1.id("photo_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PhotoRequestPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, PhotoRequestPayload::numéro, PhotoRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
