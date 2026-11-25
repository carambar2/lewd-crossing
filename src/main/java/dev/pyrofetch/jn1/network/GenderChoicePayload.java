package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** La réponse du joueur : vrai pour une femme. */
public record GenderChoicePayload(boolean femme) implements CustomPayload {
	public static final CustomPayload.Id<GenderChoicePayload> ID = new CustomPayload.Id<>(JN1.id("gender_choice"));
	public static final PacketCodec<RegistryByteBuf, GenderChoicePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, GenderChoicePayload::femme,
			GenderChoicePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
