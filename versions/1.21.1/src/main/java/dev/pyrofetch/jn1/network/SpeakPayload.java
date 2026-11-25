package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

/**
 * Une réplique choisie par le serveur, envoyée au seul joueur concerné : deux
 * joueurs peuvent être à des actes différents, leurs bulles ne doivent pas se
 * mélanger.
 *
 * @param entityId      l'entité au-dessus de laquelle afficher la bulle
 * @param text          la réplique déjà résolue
 * @param durationTicks combien de temps la laisser à l'écran
 */
public record SpeakPayload(int entityId, Text text, int durationTicks) implements CustomPayload {
	public static final CustomPayload.Id<SpeakPayload> ID = new CustomPayload.Id<>(JN1.id("speak"));

	public static final PacketCodec<RegistryByteBuf, SpeakPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, SpeakPayload::entityId,
			TextCodecs.REGISTRY_PACKET_CODEC, SpeakPayload::text,
			PacketCodecs.VAR_INT, SpeakPayload::durationTicks,
			SpeakPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
