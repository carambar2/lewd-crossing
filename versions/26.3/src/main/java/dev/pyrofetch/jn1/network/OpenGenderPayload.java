package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le serveur demande au joueur, à sa première venue, s'il est un homme ou une femme. */
public record OpenGenderPayload() implements CustomPacketPayload {
	public static final OpenGenderPayload INSTANCE = new OpenGenderPayload();
	public static final CustomPacketPayload.Type<OpenGenderPayload> ID = new CustomPacketPayload.Type<>(JN1.id("open_gender"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenGenderPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
