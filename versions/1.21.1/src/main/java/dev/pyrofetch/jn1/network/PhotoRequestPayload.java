package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;

/**
 * Demande d'une image de l'archive, par son numéro.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record PhotoRequestPayload(int numéro) implements CustomPayload {
	public static final Id<PhotoRequestPayload> ID = new Id<>(JN1.id("photo_request"));
	public static final PacketCodec<RegistryByteBuf, PhotoRequestPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT, PhotoRequestPayload::numéro, PhotoRequestPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
