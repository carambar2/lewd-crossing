package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ouvre l'écran d'une compagne (clic main vide) : le client n'a pas l'affection
 * du joueur, le serveur la joint.
 *
 * @param entityId  la compagne
 * @param affection l'affection du joueur pour elle
 * @param salut     ce qu'elle dit en le voyant venir (vide si rien)
 */
public record OpenCompanionPayload(int entityId, int affection, Component salut) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenCompanionPayload> ID = new CustomPacketPayload.Type<>(JN1.id("open_companion"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OpenCompanionPayload::entityId,
			ByteBufCodecs.VAR_INT, OpenCompanionPayload::affection,
			ComponentSerialization.STREAM_CODEC, OpenCompanionPayload::salut,
			OpenCompanionPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
