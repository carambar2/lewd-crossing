package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Le serveur demande au joueur, à sa première venue, s'il est un homme ou une femme. */
public record OpenGenderPayload() implements CustomPayload {
	public static final OpenGenderPayload INSTANCE = new OpenGenderPayload();
	public static final CustomPayload.Id<OpenGenderPayload> ID = new CustomPayload.Id<>(JN1.id("open_gender"));
	public static final PacketCodec<RegistryByteBuf, OpenGenderPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
