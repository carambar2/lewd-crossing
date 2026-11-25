package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * L'excitation du joueur, envoyée à lui seul pour sa barre.
 *
 * @param niveau de 0 à 100, arrondi
 */
public record ExcitationPayload(int niveau) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ExcitationPayload> ID = new CustomPacketPayload.Type<>(JN1.id("excitation"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ExcitationPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ExcitationPayload::niveau,
			ExcitationPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
