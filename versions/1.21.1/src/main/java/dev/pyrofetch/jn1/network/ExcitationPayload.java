package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * L'excitation du joueur, envoyée à lui seul pour sa barre.
 *
 * @param niveau de 0 à 100, arrondi
 */
public record ExcitationPayload(int niveau) implements CustomPayload {
	public static final CustomPayload.Id<ExcitationPayload> ID = new CustomPayload.Id<>(JN1.id("excitation"));

	public static final PacketCodec<RegistryByteBuf, ExcitationPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ExcitationPayload::niveau,
			ExcitationPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
