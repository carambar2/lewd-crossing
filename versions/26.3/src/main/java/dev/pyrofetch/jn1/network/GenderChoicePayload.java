package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** La réponse du joueur : vrai pour une femme. */
public record GenderChoicePayload(boolean femme) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GenderChoicePayload> ID = new CustomPacketPayload.Type<>(JN1.id("gender_choice"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GenderChoicePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, GenderChoicePayload::femme,
			GenderChoicePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
