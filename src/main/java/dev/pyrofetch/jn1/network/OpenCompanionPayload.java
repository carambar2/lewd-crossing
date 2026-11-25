package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

/**
 * Ouvre l'écran d'une compagne (clic main vide) : le client n'a pas l'affection
 * du joueur, le serveur la joint.
 *
 * @param entityId  la compagne
 * @param affection l'affection du joueur pour elle
 * @param salut     ce qu'elle dit en le voyant venir (vide si rien)
 */
public record OpenCompanionPayload(int entityId, int affection, Text salut) implements CustomPayload {
	public static final CustomPayload.Id<OpenCompanionPayload> ID = new CustomPayload.Id<>(JN1.id("open_companion"));

	public static final PacketCodec<RegistryByteBuf, OpenCompanionPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, OpenCompanionPayload::entityId,
			PacketCodecs.VAR_INT, OpenCompanionPayload::affection,
			TextCodecs.REGISTRY_PACKET_CODEC, OpenCompanionPayload::salut,
			OpenCompanionPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
