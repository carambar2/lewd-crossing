package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Une réplique choisie par le serveur, envoyée au seul joueur concerné : deux
 * joueurs peuvent être à des actes différents, leurs bulles ne doivent pas se
 * mélanger.
 *
 * @param entityId      l'entité au-dessus de laquelle afficher la bulle
 * @param text          la réplique déjà résolue
 * @param durationTicks combien de temps la laisser à l'écran
 */
public record SpeakPayload(int entityId, Component text, int durationTicks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpeakPayload> ID = new CustomPacketPayload.Type<>(JN1.id("speak"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SpeakPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SpeakPayload::entityId,
			ComponentSerialization.STREAM_CODEC, SpeakPayload::text,
			ByteBufCodecs.VAR_INT, SpeakPayload::durationTicks,
			SpeakPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
